# 简作 (jzPrint) 数据库设计文档

> 版本：v1.0  
> 日期：2026-05-26  
> 数据库方案：Room (androidx.room)  
> 数据库名称：jz_print_db  
> 版本号：1

## 1. 技术选型

### 1.1 选择 Room 的理由

| 维度 | 说明 |
|------|------|
| 现有方案 | 项目仅使用 SharedPreferences + Parcelable 序列化，无 SQLite |
| 需求复杂度 | 打印任务、进度追踪、素材管理需要结构化查询 |
| 断点续打 | 需要精确记录已打印页码，SP 无法满足 |
| 数据关联 | 学生→素材→打印任务→进度，存在关联关系 |
| Room 优势 | 编译期 SQL 校验、LiveData 支持、类型安全、迁移支持 |

### 1.2 依赖添加

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"
}
```

## 2. ER 关系图

```
┌──────────────────┐     ┌──────────────┐     ┌──────────────────┐
│     Student      │     │   Material   │     │    PrintTask     │
├──────────────────┤     ├──────────────┤     ├──────────────────┤
│ studentId PK(复合)│     │ id PK        │     │ taskId PK        │
│ schoolId  PK(复合)│     │ schoolId     │◄───►│ schoolId         │
│ editionId PK(复合)│     │ editionId    │     │ editionId        │
│ studentName      │     │ targetId     │     │ targetId         │
│ classId          │     │ targetMode   │     │ targetMode       │
│ className        │     │ remoteUrl    │     │ materialPath     │
│ cachedAt         │     │ localZipPath │     │ totalPages       │
│ materialPath     │     │ localExtract │     │ printMode        │
│ materialReady    │     │ fileSize     │     │ targetPages      │
└──────────────────┘     │ downloadedAt │     │ printedPages     │
                         │ extractedAt  │     │ status           │
                         │ status       │     │ createdAt        │
                         │ lastError    │     │ updatedAt        │
                         └──────────────┘     │ completedAt     │
                                              │ lastError       │
                                              └──────┬───────────┘
                                                 │
                                                 │ 1:N
                                                 ▼
                                          ┌──────────────────┐
                                          │  PrintProgress   │
                                          ├──────────────────┤
                                          │ id PK            │
                                          │ taskId FK        │
                                          │ pageIndex        │
                                          │ puzzleIndex      │
                                          │ totalPuzzles     │
                                          │ status           │
                                          │ cartridgeId      │
                                          │ timestamp        │
                                          └──────────────────┘
```

## 3. 表结构详细设计

### 3.1 student（学生信息缓存）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| studentId | String | PK(复合) | 学生唯一标识 |
| schoolId | String | PK(复合), NOT NULL | 学校标识 |
| editionId | String | PK(复合), NOT NULL | 校本作业标识 |
| studentName | String | NOT NULL | 学生姓名 |
| classId | String | NOT NULL | 班级唯一标识 |
| className | String | NOT NULL | 班级名称 |
| cachedAt | long | NOT NULL | 缓存时间戳 |
| materialPath | String | | 素材本地路径 |
| materialReady | boolean | DEFAULT false | 素材是否已就绪 |

> **主键改为复合主键** `(studentId, schoolId, editionId)`：同一学生在不同学校/校本作业下是独立记录，避免 `INSERT(REPLACE)` 覆盖。

**Entity 定义**：

```java
@Entity(tableName = "student",
        primaryKeys = {"studentId", "schoolId", "editionId"},
        indices = {@Index(value = {"schoolId", "editionId"})})
public class StudentEntity {
    @NonNull
    private String studentId;
    @NonNull
    private String schoolId;
    @NonNull
    private String editionId;
    @NonNull
    private String studentName;
    @NonNull
    private String classId;
    @NonNull
    private String className;
    private long cachedAt;
    private String materialPath;
    private boolean materialReady;
}
```

**DAO 定义**：

```java
@Dao
public interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StudentEntity student);

    @Update
    void update(StudentEntity student);

    @Query("SELECT * FROM student WHERE schoolId = :schoolId AND editionId = :editionId")
    List<StudentEntity> getByEdition(String schoolId, String editionId);

    @Query("SELECT * FROM student WHERE studentId = :studentId AND schoolId = :schoolId AND editionId = :editionId")
    StudentEntity getById(String studentId, String schoolId, String editionId);

    @Query("SELECT * FROM student WHERE schoolId = :schoolId AND editionId = :editionId AND materialReady = 1")
    List<StudentEntity> getMaterialReady(String schoolId, String editionId);
}
```

### 3.2 material（素材管理）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | long | PK, AUTO | 自增主键 |
| schoolId | String | NOT NULL | 学校标识 |
| editionId | String | NOT NULL | 校本作业标识 |
| targetId | String | NOT NULL | 学生ID 或 预铺码 |
| targetMode | int | NOT NULL | 1=学生 2=预铺码 |
| remoteUrl | String | | 远程下载地址 |
| remoteMd5 | String | | 远程文件 MD5 |
| localZipPath | String | | 压缩包本地路径 |
| localExtractPath | String | | 解压后本地路径 |
| fileSize | long | DEFAULT 0 | 文件大小（字节） |
| downloadedAt | long | DEFAULT 0 | 下载完成时间 |
| extractedAt | long | DEFAULT 0 | 解压完成时间 |
| status | int | NOT NULL | 0=未下载 1=下载中 2=已下载 3=已解压 4=异常 |
| lastError | String | | 最近错误信息 |

**唯一约束**：`(schoolId, editionId, targetId, targetMode)`

**Entity 定义**：

```java
@Entity(tableName = "material",
        indices = {@Index(value = {"schoolId", "editionId", "targetId", "targetMode"},
                           unique = true)})
public class MaterialEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull
    private String schoolId;
    @NonNull
    private String editionId;
    @NonNull
    private String targetId;
    private int targetMode;
    private String remoteUrl;
    private String remoteMd5;
    private String localZipPath;
    private String localExtractPath;
    private long fileSize;
    private long downloadedAt;
    private long extractedAt;
    private int status;
    private String lastError;
}
```

**DAO 定义**：

```java
@Dao
public interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(MaterialEntity material);

    @Update
    void update(MaterialEntity material);

    @Query("SELECT * FROM material WHERE schoolId = :schoolId AND editionId = :editionId AND targetId = :targetId AND targetMode = :targetMode")
    MaterialEntity find(String schoolId, String editionId, String targetId, int targetMode);

    @Query("SELECT * FROM material WHERE status = :status")
    List<MaterialEntity> findByStatus(int status);

    @Query("SELECT SUM(fileSize) FROM material")
    long getTotalSize();

    /**
     * Upsert：先查后插/更新（避免 REPLACE 导致自增ID变化）
     */
    default void upsert(MaterialEntity material) {
        MaterialEntity existing = find(material.getSchoolId(), material.getEditionId(),
                                        material.getTargetId(), material.getTargetMode());
        if (existing != null) {
            material.setId(existing.getId());
            update(material);
        } else {
            insert(material);
        }
    }
}
```

### 3.3 print_task（打印任务）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| taskId | long | PK, AUTO | 自增主键 |
| schoolId | String | NOT NULL | 学校标识 |
| editionId | String | NOT NULL | 校本作业标识 |
| targetId | String | NOT NULL | 学生ID 或 预铺码 |
| targetMode | int | NOT NULL | 1=学生 2=预铺码 |
| materialPath | String | NOT NULL | 素材 pages 目录路径 |
| totalPages | int | NOT NULL | 总页数 |
| printMode | int | NOT NULL | 1=全部 2=奇数 3=偶数 |
| targetPages | String | NOT NULL | 目标页码 JSON，如 "[85,87,89]" |
| printedPages | String | NOT NULL | 已打印页码 JSON，如 "[85,87]" |
| status | int | NOT NULL | 0=待打印 1=进行中 2=已完成 3=已取消 4=异常中断 5=已暂停 |
| createdAt | long | NOT NULL | 创建时间 |
| updatedAt | long | NOT NULL | 更新时间 |
| completedAt | long | DEFAULT 0 | 完成时间 |
| lastError | String | | 最近错误信息 |

**索引**：`targetId`、`(targetId, editionId)`、`status`（用于查找未完成任务）

**Entity 定义**：

```java
@Entity(tableName = "print_task",
        indices = {
            @Index(value = {"targetId"}),
            @Index(value = {"targetId", "editionId"}),
            @Index(value = {"status"})
        })
public class PrintTaskEntity {
    @PrimaryKey(autoGenerate = true)
    private long taskId;
    @NonNull
    private String schoolId;
    @NonNull
    private String editionId;
    @NonNull
    private String targetId;
    private int targetMode;
    @NonNull
    private String materialPath;
    private int totalPages;
    private int printMode;
    @NonNull
    private String targetPages;    // JSON: [85,87,89]
    @NonNull
    private String printedPages;   // JSON: [85,87]
    private int status;
    private long createdAt;
    private long updatedAt;
    private long completedAt;
    private String lastError;
}
```

**JSON 与 List 转换**：

```java
public class IntegerListConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<Integer> fromString(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, type);
    }

    @TypeConverter
    public static String fromList(List<Integer> list) {
        if (list == null || list.isEmpty()) return "[]";
        return gson.toJson(list);
    }
}
```

**DAO 定义**：

```java
@Dao
public interface PrintTaskDao {
    @Insert
    long insert(PrintTaskEntity task);

    @Update
    void update(PrintTaskEntity task);

    @Query("SELECT * FROM print_task WHERE taskId = :taskId")
    PrintTaskEntity getById(long taskId);

    @Query("SELECT * FROM print_task WHERE schoolId = :schoolId AND targetId = :targetId AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinished(String schoolId, String targetId);

    @Query("SELECT * FROM print_task WHERE schoolId = :schoolId AND targetId = :targetId AND editionId = :editionId AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinishedByEdition(String schoolId, String targetId, String editionId);

    @Query("SELECT * FROM print_task WHERE targetId = :targetId AND editionId = :editionId AND printMode = :printMode AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinishedByEditionAndMode(String targetId, String editionId, int printMode);

    @Query("SELECT * FROM print_task WHERE status IN (0, 4, 5) ORDER BY updatedAt DESC")
    List<PrintTaskEntity> findAllResumable();

    @Query("SELECT * FROM print_task WHERE targetId = :targetId AND status IN (0, 4, 5) ORDER BY updatedAt DESC")
    List<PrintTaskEntity> findResumableByTargetId(String targetId);
    @Query("SELECT * FROM print_task WHERE status = :status ORDER BY createdAt DESC")
    List<PrintTaskEntity> findByStatus(int status);

    @Query("SELECT * FROM print_task ORDER BY createdAt DESC")
    List<PrintTaskEntity> getAll();

    @Query("DELETE FROM print_task WHERE status = 2 AND completedAt < :beforeTime")
    int deleteCompletedBefore(long beforeTime);
}
```

### 3.4 print_progress（打印进度明细）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | long | PK, AUTO | 自增主键 |
| taskId | long | NOT NULL, FK | 关联打印任务 |
| pageIndex | int | NOT NULL | 当前页码 |
| puzzleIndex | int | NOT NULL | SDK 返回的拼索引 |
| totalPuzzles | int | NOT NULL | SDK 返回的总拼数 |
| status | int | NOT NULL | 0=开始 1=打印中 2=完成 3=失败 |
| cartridgeId | String | | 墨盒 ID |
| timestamp | long | NOT NULL | 时间戳 |

**索引**：`taskId`

**Entity 定义**：

```java
@Entity(tableName = "print_progress",
        indices = {@Index(value = {"taskId"})})
public class PrintProgressEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long taskId;
    private int pageIndex;
    private int puzzleIndex;
    private int totalPuzzles;
    private int status;
    private String cartridgeId;
    private long timestamp;
}
```

**DAO 定义**：

```java
@Dao
public interface PrintProgressDao {
    @Insert
    void insert(PrintProgressEntity progress);

    @Query("SELECT * FROM print_progress WHERE taskId = :taskId ORDER BY timestamp ASC")
    List<PrintProgressEntity> getByTaskId(long taskId);

    @Query("SELECT * FROM print_progress WHERE taskId = :taskId AND status = 2 ORDER BY timestamp DESC")
    List<PrintProgressEntity> getCompletedByTaskId(long taskId);

    @Query("DELETE FROM print_progress WHERE taskId = :taskId")
    void deleteByTaskId(long taskId);
}
```

## 4. Database 定义

```java
@Database(entities = {
    StudentEntity.class,
    MaterialEntity.class,
    PrintTaskEntity.class,
    PrintProgressEntity.class
}, version = 1, exportSchema = true)
@TypeConverters({IntegerListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "jz_print_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract StudentDao studentDao();
    public abstract MaterialDao materialDao();
    public abstract PrintTaskDao printTaskDao();
    public abstract PrintProgressDao printProgressDao();
}
```

## 5. Repository 层

### 5.1 PrintTaskRepository

```java
public class PrintTaskRepository {
    private final PrintTaskDao taskDao;
    private final PrintProgressDao progressDao;

    public PrintTaskRepository(PrintTaskDao taskDao, PrintProgressDao progressDao) {
        this.taskDao = taskDao;
        this.progressDao = progressDao;
    }

    public long insert(PrintTaskEntity task) {
        return taskDao.insert(task);
    }

    public void update(PrintTaskEntity task) {
        task.setUpdatedAt(System.currentTimeMillis());
        taskDao.update(task);
    }

    public PrintTaskEntity findUnfinished(String targetId) {
        return taskDao.findUnfinished(targetId);
    }

    /**
     * 添加已打印页码并更新任务
     */
    public void addPrintedPage(long taskId, int pageIndex) {
        PrintTaskEntity task = taskDao.getById(taskId);
        if (task == null) return;

        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        if (!printed.contains(pageIndex)) {
            printed.add(pageIndex);
            task.setPrintedPages(IntegerListConverter.fromList(printed));
        }
        task.setUpdatedAt(System.currentTimeMillis());

        // 检查是否全部完成
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        if (printed.containsAll(target)) {
            task.setStatus(2); // COMPLETED
            task.setCompletedAt(System.currentTimeMillis());
        }

        taskDao.update(task);
    }
}
```

## 6. 数据生命周期

```
素材下载 → MaterialEntity(status=3 已解压)
    ↓
创建打印任务 → PrintTaskEntity(status=0 待打印)
    ↓
开始打印 → PrintTaskEntity(status=1 进行中)
    ↓         ↓
    ↓    每完成1拼 → PrintProgressEntity(status=2)
    ↓         ↓
    ↓    更新已打印页 → PrintTaskEntity.printedPages 追加
    ↓         ↓
全部完成 → PrintTaskEntity(status=2 已完成)
    ↓
可选清理 → 删除 package.zip，保留数据库记录
```

## 7. 断点续打数据流

### 7.1 App 崩溃/被杀 恢复

```
App 重启
    ↓
TaskRecoveryManager.recoverOnStartup()
    ↓
查找 status=1 (IN_PROGRESS) 的任务
    ↓
标记为 status=4 (INTERRUPTED)     ← 这些任务在崩溃时来不及改状态
    ↓
展示给用户选择续打
```

### 7.2 用户主动退出/暂停

```
用户按返回键 / 暂停按钮
    ↓
PrintEngine.pause()
    ↓
PrintTaskEntity.status = 5 (PAUSED)  ← 当前页不记入 printedPages
PrintTaskEntity.updatedAt = now
    ↓
下次启动 → getResumableTasks() 可查到
```

### 7.3 打印机断连

```
SDK onDeviceDisconnect 回调
    ↓
PrintEngine.onDeviceDisconnected()
    ↓
PrintTaskEntity.status = 4 (INTERRUPTED)
PrintTaskEntity.lastError = "打印机断开连接"
    ↓
当前打印中的页不记入 printedPages  ← 下次续打会重打该页
    ↓
下次启动 → 提示先连接打印机 → 连接后自动进入续打
```

### 7.4 printedPages 一致性保证

| 时刻 | printedPages | 说明 |
|------|-------------|------|
| 任务创建 | [] | 空 |
| page_85 打印中 | [] | 尚未完成，不记入 |
| page_85 onPrintComplete | [85] | ★ 完成回调触发，立即持久化 |
| page_87 打印中 | [85] | 当前页未完成 |
| App 崩溃 | [85] | 已持久化，不会丢失 |
| 重启后续打 | [85] | 自动跳过85，从87开始 |
| page_87 完成 | [85,87] | 继续推进 |
| 全部完成 | [85,87,89] | containsAll(targetPages) → status=2 |

**核心原则**：只有 `onPrintComplete` 回调触发后才将页码记入 `printedPages`，保证不会标记未实际完成的页面。
