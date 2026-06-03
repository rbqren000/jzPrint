# 简作 (jzPrint) 技术设计文档

> 版本：v1.0  
> 日期：2026-05-26

## 1. 系统架构

### 1.1 模块结构

| 模块 | 类型 | 说明 |
|------|------|------|
| app | application | 主应用模块，业务逻辑与 UI |
| mxSdk | library | 打印 SDK（蓝牙BLE/SPP/WiFi通信、OpenCV、串口等），已有 |
| serialport | library | 串口通信库，已有 |
| opencv | library | OpenCV 库，已有 |
| NanoHttpd | library | 嵌入式 Web 服务器，已有 |

### 1.2 新增代码组织

```
com.org.jzprinter
├── data/                               # 数据层
│   ├── db/                             # Room 数据库
│   │   ├── AppDatabase.java            # 数据库定义
│   │   ├── entity/                     # 实体类
│   │   │   ├── StudentEntity.java
│   │   │   ├── MaterialEntity.java
│   │   │   ├── PrintTaskEntity.java
│   │   │   └── PrintProgressEntity.java
│   │   ├── dao/                        # 数据访问对象
│   │   │   ├── StudentDao.java
│   │   │   ├── MaterialDao.java
│   │   │   ├── PrintTaskDao.java
│   │   │   └── PrintProgressDao.java
│   │   └── converter/                 # 类型转换器
│   │       └── IntegerListConverter.java
│   ├── repository/                     # 数据仓库
│   │   ├── StudentRepository.java
│   │   ├── MaterialRepository.java
│   │   └── PrintTaskRepository.java
│   └── model/                          # 业务模型（非数据库实体）
│       ├── PrintMode.java              # 打印模式枚举
│       └── TaskStatus.java             # 任务状态枚举
│
├── print/                              # 打印核心
│   ├── PrintEngine.java                # 打印引擎（协调打印流程）
│   ├── PageSelector.java               # 奇偶页筛选
│   ├── MaterialLoader.java             # 素材加载（两种模式）
│   ├── ImageMerger.java                # 单点阵纵向拼接
│   ├── PrintProgressManager.java       # 打印进度管理（监听SDK回调）
│   ├── PrintImagePreparer.java         # 旋转90°+补白到552px（根据页面侧+方向自动决策旋转/对齐）
│   ├── PrintConfig.java                # 打印设置中心（奇数页位置+左右侧滑动方向+设置摘要）
│   └── StorageManager.java             # 存储空间管理
│
├── service/                            # 后台服务
│   ├── DownloadService.java            # 素材下载前台服务
│   └── PrintService.java               # 打印前台服务
│
├── network/                            # 网络层
│   ├── ApiClient.java                  # API 客户端（BaseOkHttpV3）
│   └── ApiResponse.java                # 通用响应模型
│
└── ui/                                 # 界面层
    ├── activity/
    │   ├── MainActivity.java           # 主界面 + 任务卡片列表
    │   ├── DeviceSelectActivity.java   # 设备选择
    │   ├── DeepLinkActivity.java       # 小程序分享跳转入口
    │   ├── SchoolHomeworkListActivity.java  # 校本作业列表
    │   ├── StudentListActivity.java    # 学生列表
    │   ├── PrintModeSelectActivity.java# 打印模式选择
    │   ├── PrintProgressActivity.java  # 打印进度展示
    │   ├── PrintSettingsActivity.java  # 打印设置（奇偶页方向）
    │   └── TaskDetailActivity.java     # 任务详情
    └── adapter/
        ├── TaskCardAdapter.java        # 首页任务卡片适配器
        └── ...
```

### 1.3 依赖关系

```
UI 层 → Repository 层 → DAO 层 → Room Database
UI 层 → PrintEngine → ConnectManager → mxSdk
UI 层 → DownloadService → ApiClient → 远程 API
PrintEngine → PrintProgressManager → PrintTaskRepository + PrintProgressRepository
```

## 2. 打印模式设计

### 2.1 PrintMode 枚举

```java
public enum PrintMode {
    ALL(1, "全部页"),
    ODD(2, "奇数页"),
    EVEN(3, "偶数页");

    private final int code;
    private final String label;
}
```

### 2.2 PageSelector 奇偶页筛选

```java
public class PageSelector {
    /**
     * 基于实际存在的页码列表筛选（解决非连续页码问题）
     * @param availablePages 素材中实际存在的页码列表，如 [85,86,88,89,90]
     * @param mode           打印模式
     * @return 目标页码列表（有序）
     */
    public static List<Integer> select(List<Integer> availablePages, PrintMode mode) {
        if (mode == ALL) return new ArrayList<>(availablePages);
        List<Integer> pages = new ArrayList<>();
        for (int page : availablePages) {
            if ((mode == ODD && page % 2 == 1) ||
                (mode == EVEN && page % 2 == 0)) {
                pages.add(page);
            }
        }
        return pages;
    }
}
```

## 3. 素材管理设计

### 3.1 素材存储路径规则

```
存储根目录：{context.getFilesDir()}/materials/

路径结构：
materials/
└── school_{schoolId}/
    └── edition_{editionId}/
        ├── student_{studentId}/         # editionType=1
        │   ├── info.json                # 学生信息快照
        │   ├── package.zip              # 下载的压缩包（解压后可删除）
        │   └── pages/                   # 解压后的素材
        │       ├── page_85.png
        │       ├── page_85/
        │       │   ├── 1.png
        │       │   └── ...
        │       └── ...
        └── prepare_{prepareCode}/       # editionType=2
            ├── info.json
            ├── package.zip
            └── pages/
                └── ...
```

### 3.2 路径生成

```java
public class MaterialPathBuilder {
    private static final String BASE_DIR = "materials";

    public static String build(Context context, String schoolId, String editionId,
                               String targetId, int targetMode) {
        String base = context.getFilesDir() + "/" + BASE_DIR;
        String targetDir = targetMode == 1
            ? "student_" + targetId
            : "prepare_" + targetId;
        return base + "/school_" + schoolId
            + "/edition_" + editionId
            + "/" + targetDir;
    }

    public static String getPagesPath(String basePath) {
        return basePath + "/pages";
    }

    public static String getZipPath(String basePath) {
        return basePath + "/package.zip";
    }
}
```

### 3.3 MaterialLoader 素材加载

```java
public class MaterialLoader {
    private final boolean useCustomMerge;

    public MaterialLoader(boolean useCustomMerge) {
        this.useCustomMerge = useCustomMerge;
    }

    /**
     * 加载指定页的 Bitmap
     * @param pagesPath 素材解压后的 pages 目录路径
     * @param pageIndex 页码
     * @return 该页的 Bitmap（纵向长图）
     */
    public Bitmap loadPage(String pagesPath, int pageIndex) {
        if (useCustomMerge) {
            return loadAndMerge(pagesPath, pageIndex);
        } else {
            return loadPreMerged(pagesPath, pageIndex);
        }
    }

    private Bitmap loadPreMerged(String pagesPath, int pageIndex) {
        String path = pagesPath + "/page_" + pageIndex + ".png";
        return BitmapFactory.decodeFile(path);
    }

    private Bitmap loadAndMerge(String pagesPath, int pageIndex) {
        String dir = pagesPath + "/page_" + pageIndex;
        File[] files = new File(dir).listFiles((d, name) ->
            name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, Comparator.comparing(File::getName));
        return ImageMerger.mergeVertically(files);
    }

    /**
     * 获取素材的总页数（基于实际存在的 page_XX.png 文件，不假设连续）
     */
    public int getTotalPages(String pagesPath) {
        return getAvailablePages(pagesPath).size();
    }

    /**
     * 获取素材中实际存在的页码列表（解决非连续页码问题）
     * 例如：page_85,86,88 → 返回 [85,86,88]（跳过87）
     */
    public List<Integer> getAvailablePages(String pagesPath) {
        File dir = new File(pagesPath);
        File[] pngFiles = dir.listFiles((d, name) ->
            name.matches("page_\\d+\\.png"));
        if (pngFiles == null) return new ArrayList<>();

        List<Integer> pages = new ArrayList<>();
        for (File f : pngFiles) {
            String name = f.getName();  // page_85.png
            int page = Integer.parseInt(name.replaceAll("[^\\d]", ""));
            pages.add(page);
        }
        Collections.sort(pages);
        return pages;
    }
}
```

### 3.4 ImageMerger 图片纵向拼接

```java
public class ImageMerger {
    /**
     * 将多张图片纵向拼接为一张长图
     * @param imageFiles 图片文件数组（已排序）
     * @return 拼接后的 Bitmap
     */
    public static Bitmap mergeVertically(File[] imageFiles) {
        // ★ 自然排序：确保 "1" < "2" < "2-1" < "2-2" < "3" < "10" 的正确顺序
        // 字符串排序会导致 "10" < "2" 的错误
        Arrays.sort(imageFiles, ImageMerger::naturalCompare);

        List<Bitmap> bitmaps = new ArrayList<>();
        int width = 0;
        int totalHeight = 0;

        for (File file : imageFiles) {
            Bitmap bm = BitmapFactory.decodeFile(file.getPath());
            if (bm == null) continue;
            bitmaps.add(bm);
            width = Math.max(width, bm.getWidth());
            totalHeight += bm.getHeight();
        }

        if (bitmaps.isEmpty() || width == 0) return null;

        Bitmap result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        int y = 0;
        for (Bitmap bm : bitmaps) {
            canvas.drawBitmap(bm, 0, y, null);
            y += bm.getHeight();
            bm.recycle();
        }

        return result;
    }

    /**
     * ★ 自然排序比较器（Natural Sort）
     * 支持素材子图命名：1.png, 2.png, 2-1.png, 2-2.png, 3.png, 10.png, 提交码.png, 订正码.png
     * 数字部分按数值比较，非数字部分按字符串比较
     * 中文命名的文件（提交码.png、订正码.png）排在数字命名之后
     */
    private static int naturalCompare(File f1, File f2) {
        String s1 = f1.getName();
        String s2 = f2.getName();
        int i1 = 0, i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int num1 = 0;
                while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) {
                    num1 = num1 * 10 + (s1.charAt(i1) - '0');
                    i1++;
                }
                int num2 = 0;
                while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) {
                    num2 = num2 * 10 + (s2.charAt(i2) - '0');
                    i2++;
                }
                if (num1 != num2) return num1 - num2;
            } else {
                if (c1 != c2) return c1 - c2;
                i1++;
                i2++;
            }
        }
        return (s1.length() - i1) - (s2.length() - i2);
    }
}
```

## 4. 打印引擎设计

### 4.1 核心流程：MultiRow 一次发送

**关键设计**：将所有目标页（如所有奇数页）组织成一个 `MultiRowData`，一次发送给打印机。打印机连续逐拼打印，SDK 通过 `OnPrintListener` 逐拼回调进度。

**★ 1 页 = 1 拼**。素材原图是纵向窄长条（如 360×1186），**打印前旋转 90° 变为横向**（1186×360），旋转后高度=360px < 打印头高度 552px，**补白到 552px**（创建白色画布，按顶部/底部对齐放置），补白后每页恰好 1 个 RowData = 1 拼。`onPrintComplete` 每回调 1 次 = 完成 1 页。

```
用户选择"奇数页"模式
    ↓
筛选目标页：[85, 87, 89, 91, 93, 95]  (6页=6拼)
    ↓
逐页加载 Bitmap → 旋转90°(纵向→横向) → 补白到552px(按对齐方式) → 生成 MultiRowImage → 合并
    ↓
例：page_85 旋转后 1186×360 → 补白 1186×552 → 1个RowData=1拼
    ↓
合并后的 MultiRowImage → MultiRowDataFactory.bitmap2MultiRowData()
    ↓
生成一个 MultiRowData（包含6拼数据，每拼1个RowData）
    ↓
ConnectManager.setWithSendMultiRowDataPacket(data)  ← 一次发送
    ↓
打印机逐拼打印：
  onPrintStart(0, 5, 0)    ← 第0拼(page_85)开始
  onPrintComplete(0, 5, 0)  ← 第0拼完成 → 记录 page_85
  onPrintStart(0, 5, 1)    ← 第1拼(page_87)开始
  onPrintComplete(0, 5, 1)  ← 第1拼完成 → 记录 page_87
  ... 直到第5拼完成
```

### 4.2 PrintEngine 核心实现

```java
public class PrintEngine {
    private final PrintTaskRepository taskRepo;
    private final MaterialLoader materialLoader;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();  // ★ DB操作专用线程
    private PrintProgressManager progressManager;
    private PrintTaskEntity currentTask;
    private final AtomicBoolean isPrinting = new AtomicBoolean(false);  // ★ 线程安全

    // ★ 打印设置（从 PrintConfig 读取，重打/续打时也在 PrintProgressActivity 中重新设置）
    private boolean oddPageOnRight = true;
    private boolean leftBottomToTop = false;
    private boolean rightBottomToTop = false;

    public void setOddPageOnRight(boolean v) { this.oddPageOnRight = v; }
    public void setLeftBottomToTop(boolean v) { this.leftBottomToTop = v; }
    public void setRightBottomToTop(boolean v) { this.rightBottomToTop = v; }

    /**
     * 开始新任务
     */
    public PrintTaskEntity startNewTask(String schoolId, String editionId,
                                   String targetId, int targetMode,
                                   PrintMode printMode, String pagesPath) {
        PrintTaskEntity existing = taskRepo.findUnfinishedByEditionAndMode(
            targetId, editionId, printMode.getCode());
        if (existing != null) {
            throw new IllegalStateException("该学生此校本此模式已有未完成的打印任务");
        }

        List<Integer> availablePages = materialLoader.getAvailablePages(pagesPath);
        int totalPages = availablePages.size();
        List<Integer> targetPages = PageSelector.select(availablePages, printMode);

        PrintTaskEntity task = new PrintTaskEntity();
        task.setSchoolId(schoolId);
        task.setEditionId(editionId);
        task.setTargetId(targetId);
        task.setTargetMode(targetMode);
        task.setMaterialPath(pagesPath);
        task.setTotalPages(totalPages);
        task.setPrintMode(printMode.getCode());
        task.setTargetPages(IntegerListConverter.fromList(targetPages));
        task.setPrintedPages("[]");
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setCreatedAt(System.currentTimeMillis());
        dbExecutor.execute(() -> taskRepo.insert(task));  // ★ 子线程DB写

        return task;
    }

    /**
     * 执行打印：将所有剩余页组织成一个 MultiRowData 一次发送
     */
    public void execute(PrintTaskEntity task) {
        List<Integer> remaining = getRemainingPages(task);
        if (remaining.isEmpty()) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写
            return;
        }

        // ★ 并发发送保护
        // isDataSending() 在 cancelSendMultiRowDataPacket 后会正确返回 false（已修复SDK Bug）
        // 另外 SDK 内部 setWithSendMultiRowDataPacket 也会检查 isConnected/isDataSynchronize/commandQueue，
        // 此处检查作为 App 层面的二次保护。
        if (isPrinting.get()) {
            throw new IllegalStateException("当前有打印任务正在进行中");
        }

        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写
        currentTask = task;
        isPrinting = true;

        progressManager.setCurrentTask(task);
        progressManager.setTargetPages(remaining); // 拼索引→页码映射（1页=1拼，直接对应）

        // 启动前台服务
        PrintService.start();

        // 逐页加载 Bitmap 并合并为 MultiRowImage
        buildAndSendMultiRow(task.getMaterialPath(), remaining);
    }

    /**
     * 逐页加载 Bitmap → 旋转90°+补白552px → 生成 MultiRowImage → 合并发送
     * ★ 补白后高度=552px，每页恰好1个RowData=1拼，SDK不再做缩放
     */
    private void buildAndSendMultiRow(String pagesPath, List<Integer> remainingPages) {
        List<RowImage> allRowImages = new ArrayList<>();

        for (int pageIndex : remainingPages) {
            Bitmap page = materialLoader.loadPage(pagesPath, pageIndex);
            if (page == null) {
                currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                currentTask.setLastError("页面 " + pageIndex + " 加载失败");
                dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
                isPrinting = false;
                return;
            }

            // ★ 旋转 + 补白到552px（按旋转方向和对齐方式放置）
            Bitmap prepared = PrintImagePreparer.prepare(page, rotationDirection, verticalAlignment);
            page.recycle();

            // 补白后 Bitmap → MultiRowImage（高度=552，只产生1个RowImage=1拼）
            MultiRowImage pageImage;
            try {
                pageImage = MultiRowImageFactory.image2MultiRowImage(
                    context, prepared,
                    RowLayoutDirection.RowLayoutDirectionVertical, 0);
            } catch (IOException e) {
                currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                currentTask.setLastError("页面 " + pageIndex + " 图片拆行失败: " + e.getMessage());
                dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
                isPrinting = false;
                prepared.recycle();
                return;
            }
            prepared.recycle();

            allRowImages.addAll(pageImage.getRowImages());
        }

        // 合并所有 RowImage 为一个 MultiRowImage（使用工厂方法）
        MultiRowImage combinedImage = MultiRowImage.createInstance(
            allRowImages, null,
            RowLayoutDirection.RowLayoutDirectionVertical, false);

        // MultiRowImage → MultiRowData
        MultiRowDataFactory.bitmap2MultiRowData(
            context, combinedImage,
            127, false, true, false, false, false, false,
            new MultiRowDataFactory.OnCreateMultiRowDataListener() {
                @Override
                public void onCreateMultiRowDataStart() {}

                @Override
                public void onCreateMultiRowDataComplete(MultiRowData multiRowData) {
                    sendToPrinter(multiRowData);
                }

                @Override
                public void onCreateMultiRowDataError(int code) {
                    currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                    currentTask.setLastError("生成打印数据失败: " + code);
                    dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
                    isPrinting = false;
                }
            });
    }

    /**
     * 发送 MultiRowData 给打印机
     * ★ 修复：发送前先注销旧的 DataProgressListener，避免累积
     */
    private ConnectManager.OnDataProgressListener currentDataProgressListener;

    private void sendToPrinter(MultiRowData data) {
        ConnectManager cm = ConnectManager.share();

        // ★ 先注销旧监听器，避免累积
        if (currentDataProgressListener != null) {
            cm.unregisterDataProgressListener(currentDataProgressListener);
        }

        currentDataProgressListener = new ConnectManager.OnDataProgressListener() {
            @Override
            public void onDataProgressStart(float size, int progress, long startTime) {
                progressManager.onDataTransferStart(size);
            }

            @Override
            public void onDataProgress(float size, int progress, long startTime, long currentTime) {
                progressManager.onDataTransferProgress(progress);
            }

            @Override
            public void onDataProgressFinish(float size, long startTime, long currentTime) {
                progressManager.onDataTransferComplete();
            }

            @Override
            public void onDataProgressError(String error, int code) {
                currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                currentTask.setLastError("数据发送失败: " + error);
                dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
                isPrinting = false;
            }
        };

        cm.registerDataProgressListener(currentDataProgressListener);
        cm.setWithSendMultiRowDataPacket(data);
    }

    /**
     * SDK OnPrintListener.onPrintComplete 回调
     * ★ 1页=1拼，currentIndex 直接对应 targetPages[currentIndex]
     * ★ 暂停/切换学生后，打印机仍在物理输出当前批次数据，onPrintComplete 会继续触发，
     *   这是手持移动打印机的正常行为（协议无 StopPrint 指令）。
     *   回调推进 printedPages 是正确的——这些页确实已物理打印完成。
     *   暂停仅表示"当前批次打完后，不再发起新打印任务"。
     */
    public void onPhysicalPrintComplete(int beginIndex, int endIndex,
                                         int currentIndex, String cartridgeId) {
        if (currentTask == null) return;

        // 通过拼索引映射到实际页码（1页=1拼，直接对应）
        int pageIndex = progressManager.getPageByPuzzleIndex(currentIndex);

        // ★ 物理打印完成，实时保存进度
        progressManager.onPageComplete(currentTask, pageIndex);

        // 记录进度明细
        progressManager.onSdkPrintComplete(beginIndex, endIndex, currentIndex, cartridgeId);

        // 检查是否全部完成
        if (currentIndex == endIndex) {
            currentTask.setStatus(TaskStatus.COMPLETED.getCode());
            currentTask.setCompletedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
            isPrinting = false;
            PrintService.notifyComplete();
        }
    }

    /**
     * SDK OnPrintListener.onPrintStart 回调
     */
    public void onPhysicalPrintStart(int beginIndex, int endIndex, int currentIndex) {
        if (currentTask == null) return;
        progressManager.onSdkPrintStart(beginIndex, endIndex, currentIndex);
    }

    /**
     * 断点续打
     */
    public void resumeFromBreakpoint(PrintTaskEntity task) {
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写
        execute(task);  // execute 内部会自动跳过已打印页
    }

    private List<Integer> getRemainingPages(PrintTaskEntity task) {
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        List<Integer> remaining = new ArrayList<>();
        for (int page : target) {
            if (!printed.contains(page)) {
                remaining.add(page);
            }
        }
        return remaining;
    }
}
```

### 4.3 PrintImagePreparer — 旋转 + 补白到打印头高度

**★ 关键流程**：
1. 原图纵向窄长条 (360×H)，旋转 90° 变为横向 (H×360)
2. 旋转后高度=360px < 打印头 552px，**需补白到 552px**（不是缩放！）
3. 创建 552px 高度的白色画布，将旋转后的图放上去
4. 旋转方向和对齐方式由页面侧 + 滑动方向自动决策

**为什么不能缩放**：SDK 的 `MultiRowDataFactory.bitmap2MultiRowData` 内部会自动缩放非 552px 高度的图片，但缩放会变形导致 Anoto 点阵失真。必须手动补白到恰好 552px，让 SDK 不再做缩放。

```java
public class PrintImagePreparer {
    public static final int PRINT_HEAD_HEIGHT = 552;

    public enum RotationDirection { CW_90, CCW_90 }
    public enum VerticalAlignment { TOP, BOTTOM }

    /**
     * 根据配置决策旋转方向。
     *
     * @param pageCode         页码
     * @param oddPageOnRight   奇数页是否在右侧
     * @param leftBottomToTop  左侧页滑动方向是否为下→上
     * @param rightBottomToTop 右侧页滑动方向是否为下→上
     * @return 旋转方向
     *
     * 决策逻辑：
     *   基准旋转：两侧均为 CCW_90（实测确认）
     *   基准对齐：右侧=TOP，左侧=BOTTOM
     *   当该侧方向为「下→上」时，旋转与对齐同时取反
     */
    public static RotationDirection getRotation(int pageCode, boolean oddPageOnRight,
                                                 boolean leftBottomToTop, boolean rightBottomToTop) {
        boolean isRightPage = (pageCode % 2 == 1) == oddPageOnRight;
        boolean btoT = isRightPage ? rightBottomToTop : leftBottomToTop;
        return btoT ? RotationDirection.CW_90 : RotationDirection.CCW_90;
    }

    /** 对齐方式的决策逻辑同 getRotation */
    public static VerticalAlignment getAlignment(...);

    public static Bitmap prepare(Bitmap source, RotationDirection rotation,
                                  VerticalAlignment alignment) {
        // Step 1: 旋转（CW_90→postRotate(90), CCW_90→postRotate(-90)）
        // Step 2: 高度=552 直接返回，>552 抛异常，<552 创建白色画布补白
        // Step 3: TOP→y=0, BOTTOM→y=552-height
    }
}
```

**旋转+对齐决策表**（以 oddPageOnRight=true 为例）：

| # | 页面侧 | 滑动方向 | 旋转 | 对齐 |
|---|--------|---------|------|------|
| 1 | 左侧 | 上→下 | CCW_90 | BOTTOM |
| 2 | 左侧 | 下→上 | CW_90 | TOP |
| 3 | 右侧 | 上→下 | CCW_90 | TOP |
| 4 | 右侧 | 下→上 | CW_90 | BOTTOM |

**用户配置项**（PrintSettingActivity + PrintConfig 管理）：

| 配置项 | Key | 默认值 | 说明 |
|--------|-----|--------|------|
| 奇数页位置 | `odd_page_on_right` | true | true=奇数在右，false=奇数在左 |
| 左侧打印方向 | `left_print_direction_btt` | false | false=上→下，true=下→上 |
| 右侧打印方向 | `right_print_direction_btt` | false | false=上→下，true=下→上 |

### 4.4 PrintProgressManager 进度管理

```java
public class PrintProgressManager {
    private final PrintTaskRepository taskRepo;
    private final PrintProgressDao progressDao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();  // ★ DB操作专用线程
    private PrintTaskEntity currentTask;
    private List<Integer> targetPages;  // 拼索引→页码映射（按发送顺序，1页=1拼直接对应）

    /**
     * 设置当前打印任务的拼→页映射
     * @param targetPages 按发送顺序排列的页码列表
     *                    索引0=第0拼对应的页码，索引1=第1拼对应的页码，...
     */
    public void setTargetPages(List<Integer> targetPages) {
        this.targetPages = targetPages;
    }

    /**
     * 根据拼索引获取实际页码（1页=1拼，直接对应）
     */
    public int getPageByPuzzleIndex(int puzzleIndex) {
        if (targetPages != null && puzzleIndex >= 0 && puzzleIndex < targetPages.size()) {
            return targetPages.get(puzzleIndex);
        }
        return -1;
    }

    public void setCurrentTask(PrintTaskEntity task) {
        this.currentTask = task;
    }

    /**
     * SDK OnPrintListener 回调处理
     */
    public void onSdkPrintStart(int beginIndex, int endIndex, int currentIndex) {
        if (currentTask == null) return;
        int pageIndex = getPageByPuzzleIndex(currentIndex);

        PrintProgressEntity progress = new PrintProgressEntity();
        progress.setTaskId(currentTask.getTaskId());
        progress.setPageIndex(pageIndex);
        progress.setPuzzleIndex(currentIndex);
        progress.setTotalPuzzles(endIndex + 1);
        progress.setStatus(0); // 开始
        progress.setTimestamp(System.currentTimeMillis());
        dbExecutor.execute(() -> progressDao.insert(progress));  // ★ 子线程DB写
    }

    public void onSdkPrintComplete(int beginIndex, int endIndex,
                                    int currentIndex, String cartridgeId) {
        if (currentTask == null) return;
        int pageIndex = getPageByPuzzleIndex(currentIndex);

        PrintProgressEntity progress = new PrintProgressEntity();
        progress.setTaskId(currentTask.getTaskId());
        progress.setPageIndex(pageIndex);
        progress.setPuzzleIndex(currentIndex);
        progress.setTotalPuzzles(endIndex + 1);
        progress.setStatus(2); // 完成
        progress.setCartridgeId(cartridgeId);
        progress.setTimestamp(System.currentTimeMillis());
        dbExecutor.execute(() -> progressDao.insert(progress));  // ★ 子线程DB写
    }

    /**
     * 物理打印完成，更新 printedPages
     * ★ OnPrintListener 回调在主线程，DB 写操作必须在子线程执行
     */
    public void onPageComplete(PrintTaskEntity task, int pageIndex) {
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        if (!printed.contains(pageIndex)) {
            printed.add(pageIndex);
            task.setPrintedPages(IntegerListConverter.fromList(printed));
        }
        task.setUpdatedAt(System.currentTimeMillis());

        // 检查是否全部完成
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        if (printed.containsAll(target)) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(System.currentTimeMillis());
        }

        dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写，避免主线程I/O阻塞ANR
    }

    public void onPageError(PrintTaskEntity task, int pageIndex, String error) {
        task.setStatus(TaskStatus.INTERRUPTED.getCode());
        task.setLastError("页面 " + pageIndex + ": " + error);
        task.setUpdatedAt(System.currentTimeMillis());
        dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写
    }

    // 数据传输进度回调
    public void onDataTransferStart(float size) { /* UI: "开始发送数据" */ }
    public void onDataTransferProgress(int percentage) { /* UI: "发送数据 N%" */ }
    public void onDataTransferComplete() { /* UI: "数据发送完成，等待打印" */ }
}
```

## 5. SDK 回调集成

### 5.1 OnPrintListener 注册（★ 必须在 Application.onCreate 中注册）

**关键**：`OnPrintListener` 必须在 App 启动时就注册到 `ConnectManager`，否则 `onPrintComplete` 回调不会触发，`printedPages` 永远为空，断点续打完全失效。

```java
// 在 Application.onCreate 或 PrintEngine 初始化时注册
public class PrintEngine {
    private static PrintEngine instance;

    public static PrintEngine init(Context context, PrintTaskRepository taskRepo,
                                   MaterialLoader materialLoader) {
        if (instance == null) {
            instance = new PrintEngine(taskRepo, materialLoader);
            // ★ 注册 OnPrintListener
            ConnectManager.share().registerPrintListener(new ConnectManager.OnPrintListener() {
                @Override
                public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
                    instance.onPhysicalPrintStart(beginIndex, endIndex, currentIndex);
                }

                @Override
                public void onPrintComplete(int beginIndex, int endIndex,
                                            int currentIndex, String cartridgeId) {
                    instance.onPhysicalPrintComplete(beginIndex, endIndex, currentIndex, cartridgeId);
                }
            });

            // ★ 注册 OnDeviceConnectListener，监听断连事件
            ConnectManager.share().registerDeviceConnectListener(new ConnectManager.OnDeviceConnectListener() {
                @Override
                public void onDeviceConnectStart(Device device) {}

                @Override
                public void onDeviceConnectSucceed(Device device) {}

                @Override
                public void onDeviceDisconnect(Device device) {
                    instance.onDeviceDisconnected();
                }

                @Override
                public void onDeviceConnectFail(Device device, String error) {}
            });
        }
        return instance;
    }
}
```

### 5.2 OnPrintListener 接口定义

**参数说明**：
- `beginIndex`：本次打印任务的起始索引（从 0 开始）
- `endIndex`：本次打印任务的结束索引（`endIndex = 总拼数 - 1`）
- `currentIndex`：当前正在打印/刚完成打印的拼索引
- `cartridgeId`：墨盒 ID（仅 `onPrintComplete` 提供）

**本项目中**：1 页 = 1 拼，每拼恰好 1 个 RowData（旋转后高度 360px < 552px）。
- `onPrintStart(0, 5, 0)` → 开始打印第0拼 (page_85)
- `onPrintComplete(0, 5, 0, "墨盒ID")` → 完成打印第0拼 → 记录 page_85 完成

### 5.3 OnDataProgressListener 集成

SDK 提供的 `OnDataProgressListener` 用于监听打印数据发送进度：

```java
public interface OnDataProgressListener {
    void onDataProgressStart(float size, int progress, long startTime);
    void onDataProgress(float size, int progress, long startTime, long currentTime);
    void onDataProgressFinish(float size, long startTime, long currentTime);
    void onDataProgressError(String error, int code);
}
```

**本项目中**：用于 UI 展示数据传输进度百分比。

### 5.4 打印数据发送链路

PrintEngine 直接通过 `ConnectManager` 发送打印数据：

```
Bitmap
  → PrintImagePreparer.prepare() 旋转+补白到552px
  → MultiRowImageFactory.image2MultiRowImage()
  → MultiRowDataFactory.bitmap2MultiRowData()
  → ConnectManager.setWithSendMultiRowDataPacket()
```

**重要：MultiRow 一次发送模式**：

本项目的打印方式是：将所有目标页（如所有奇数页）合并为一个 `MultiRowData`，一次发送给打印机。打印机逐拼连续打印，SDK 逐拼回调。

**★ 1 页 = 1 拼**：打印前将纵向长条图旋转 90° 变为横向，旋转后高度=360px < 打印头552px，每页恰好 1 个 RowData。

| 回调 | 含义 | 本项目处理 |
|------|------|-----------|
| `OnDataProgressListener.onDataProgressFinish()` | 全部数据传输完成 | UI: "数据已发送，等待打印" |
| `OnPrintListener.onPrintStart(begin, end, current)` | 打印机开始打印某拼 | UI: "正在打印 page_XX" |
| `OnPrintListener.onPrintComplete(begin, end, current, cartridgeId)` | **某拼物理打印完成** | ★ 通过拼索引映射到页码，记录 printedPages |

**拼索引→页码映射**：发送时 `targetPages` 列表按顺序对应拼索引 0,1,2,...，即 `targetPages[currentIndex]` = 当前完成的页码。

## 6. 素材下载设计

### 6.1 DownloadService

```java
public class DownloadService extends Service {
    // 前台服务，防止下载被系统杀死
    // 通知栏展示下载进度

    /**
     * 下载并解压素材
     */
    public void downloadAndExtract(String schoolId, String editionId,
                                    String targetId, int targetMode,
                                    String remoteUrl, DownloadCallback callback) {
        String basePath = MaterialPathBuilder.build(this, schoolId, editionId,
                                                     targetId, targetMode);
        String zipPath = MaterialPathBuilder.getZipPath(basePath);
        String pagesPath = MaterialPathBuilder.getPagesPath(basePath);

        // 1. 检查是否已解压
        if (isAlreadyExtracted(pagesPath)) {
            callback.onAlreadyExists(basePath);
            return;
        }

        // 2. 下载压缩包
        downloadFile(remoteUrl, zipPath, new DownloadCallback() {
            @Override
            public void onProgress(int percentage) {
                callback.onDownloadProgress(percentage);
            }

            @Override
            public void onComplete() {
                // 3. 解压
                unzip(zipPath, pagesPath);
                // 4. 更新数据库
                saveMaterialRecord(schoolId, editionId, targetId, targetMode, basePath);
                callback.onComplete(basePath);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
```

### 6.2 API 对接（⏳ 预留待定，客户 API 尚未提供）

> **状态**：客户 API 接口尚未提供，以下为预期接口设计，待拿到实际 API 后替换。
> **编码策略**：先定义 `ApiClient` 接口 + 本地 Mock 实现，UI 层仅依赖接口，后续切换真实实现无需改动上层代码。

使用项目已引入的 BaseOkHttpV3 库：

```java
public class ApiClient {
    private static final String BASE_URL = BuildConfig.API_BASE_URL;  // 待客户提供

    /**
     * 查询校本作业列表
     * ⏳ 待定：请求/响应格式需客户确认
     */
    public static void queryEditionList(String schoolId, ApiCallback callback) {
        BaseOkHttpV3
            .postJson(BASE_URL + "/edition/list")
            .jsonBody(new Gson().toJson(Map.of("schoolId", schoolId)))
            .setCallback(callback);
    }

    /**
     * 获取班级学生信息
     * ⏳ 待定
     */
    public static void queryStudentList(String schoolId, String editionId,
                                         ApiCallback callback) {
        // ...
    }

    /**
     * 获取预铺码列表
     * ⏳ 待定
     */
    public static void queryPrepareCodeList(String schoolId, String editionId,
                                             ApiCallback callback) {
        // ...
    }

    /**
     * 获取素材下载地址
     * ⏳ 待定
     */
    public static void getMaterialUrl(String schoolId, String editionId,
                                       String targetId, ApiCallback callback) {
        // 返回下载 URL
    }
}
```

#### Mock 实现（开发阶段使用）

```java
public class MockApiClient extends ApiClient {
    // 返回预设的校本列表/学生列表/素材URL
    // 用于开发阶段无真实 API 时的本地调试
}
```

## 7. 小程序分享入口设计

### 7.1 微信小程序跳转 App

小程序分享 schoolId 后，用户点击可跳转到 App：

```xml
<!-- AndroidManifest.xml 添加 intent-filter -->
<activity android:name=".ui.activity.DeepLinkActivity"
          android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="jzprint"
              android:host="share"/>
    </intent-filter>
</activity>
```

### 7.2 DeepLink 处理

```java
public class DeepLinkActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri != null && "jzprint".equals(uri.getScheme())) {
            String schoolId = uri.getQueryParameter("schoolId");
            if (schoolId != null) {
                // 保存 schoolId，跳转到校本作业列表
                PreferencesUtils.putString(this, "schoolId", schoolId);
                Intent intent = new Intent(this, SchoolHomeworkListActivity.class);
                intent.putExtra("schoolId", schoolId);
                startActivity(intent);
            }
        }
        finish();
    }
}
```

### 7.3 小程序端分享链接格式

```
jzprint://share?schoolId=1234567890abcd
```

## 8. 双人双机协作设计

### 8.1 设计思路

两台设备各自独立运行，无需设备间通信。通过打印模式选择实现任务分配：

```
设备 A                          设备 B
┌──────────────────┐          ┌──────────────────┐
│ 选择同一学生      │          │ 选择同一学生      │
│ 打印模式：奇数页  │          │ 打印模式：偶数页  │
│ 各自维护独立任务  │          │ 各自维护独立任务  │
│ 各自记录打印进度  │          │ 各自记录打印进度  │
└──────────────────┘          └──────────────────┘
```

### 8.2 数据隔离

- 每台设备创建独立的 `PrintTask`（不同的 `taskId`）
- `PrintTask.printMode` 区分奇偶
- `PrintTask.printedPages` 各自独立记录
- 两台设备的素材可分别下载，存储路径相同（同一学生同一校本）

### 8.3 注意事项

- 建议在打印模式选择界面明确提示"奇数页=本子左侧，偶数页=本子右侧"
- 奇数页和偶数页互补，合起来覆盖全部页
- 打印完成后可提示用户确认所有页面均已打印

## 9. 断点续打架构设计

### 9.1 场景全覆盖

| 场景 | 触发方式 | 风险 | 处理策略 |
|------|---------|------|----------|
| A. 打印中途App崩溃/被杀 | 系统/崩溃 | 当前页可能正在打印中 | App重启后检测 status=1 的任务，将其标记为 INTERRUPTED，提示续打 |
| B. 用户主动按Home/切到后台 | 用户操作 | 长时间后台可能被回收 | 打印期间启动前台 Service 保持存活 |
| C. 用户按返回退出打印界面 | 用户操作 | 当前页打印可能未完成 | 弹出确认对话框，确认后暂停任务并保存状态 |
| D. 打印机蓝牙/WiFi断连 | 外部原因 | 当前页数据已发但未收到完成回调 | 监听 SDK 断连事件，标记任务为 INTERRUPTED，当前页不记入 printedPages |
| E. 用户主动取消打印 | 用户操作 | 部分已打印 | 已打印页保留记录，任务标记 CANCELLED，可后续继续 |
| F. 不同学生交替打印 | 用户操作 | 张三打一半→李四打→张三回来继续 | 每个学生独立PrintTask，切换学生时暂停当前任务，回来时列出所有可续打任务。★切换后新数据覆盖打印机缓冲区，旧学生未打印页数据丢失，回来后续打需重新发送 |
| G. 同一学生有多个未完成任务 | 连续操作 | 需要选择继续哪个 | 列出所有未完成任务供用户选择 |
| H. 打印到一半离开，数小时/数天后回来 | 用户操作 | 打印机可能已关机/断连 | 检测打印机连接状态，未连接则提示先连接打印机再续打 |

**★ 暂停语义（手持移动打印机特性）**：
- 协议无 StopPrint 指令，暂停时当前批次数据会打印完毕
- `onPrintComplete` 继续触发推进 `printedPages` 是正确的（这些页确实已物理打印完成）
- 暂停 = "当前批次打完后，不再发起新打印任务"
- UI 应提示"打印机正在输出中，当前批次将打印完毕"

### 9.2 任务状态机

```
                    ┌──────────┐
          创建      │ PENDING  │
         ────────►  │  (0)     │
                    └────┬─────┘
                         │ 开始打印
                         ▼
                    ┌──────────┐
                    │IN_PROGRESS│◄─────┐
                    │  (1)     │       │
                    └────┬─────┘       │
            ┌────────────┼────────────┐│
            │            │            ││
     正常完成▼     异常中断▼     用户暂停▼│
       ┌────────┐ ┌──────────┐ ┌────────┐
       │COMPLETED│ │INTERRUPTED│ │PAUSED  │
       │  (2)   │ │  (4)      │ │ (5)    │
       └────────┘ └─────┬────┘ └───┬────┘
                        │           │
                        │  继续打印 │
                        └───────────┘
                    
       用户取消▼
       ┌────────┐
       │CANCELLED│
       │  (3)   │
       └────────┘
```

**状态定义更新**：

```java
public enum TaskStatus {
    PENDING(0, "待打印"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    INTERRUPTED(4, "异常中断"),   // 崩溃/断连等
    PAUSED(5, "已暂停");          // 用户主动暂停

    private final int code;
    private final String label;

    public boolean canResume() {
        return this == PENDING || this == INTERRUPTED || this == PAUSED;
        // 注意：IN_PROGRESS 不包含，因为 recoverOnStartup() 会将其转为 INTERRUPTED
    }
}
```

### 9.3 启动时恢复逻辑

```java
public class TaskRecoveryManager {
    private final PrintTaskRepository taskRepo;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();  // ★ DB操作专用线程

    /**
     * App 启动时调用，修复可能的不一致状态
     * 场景：App 崩溃后重启，IN_PROGRESS 状态的任务实际已中断
     */
    public void recoverOnStartup() {
        // 1. 将所有 IN_PROGRESS 的任务标记为 INTERRUPTED
        List<PrintTaskEntity> inProgress = taskDao.findByStatus(TaskStatus.IN_PROGRESS.getCode());
        for (PrintTaskEntity task : inProgress) {
            task.setStatus(TaskStatus.INTERRUPTED.getCode());
            task.setUpdatedAt(System.currentTimeMillis());
            task.setLastError("App异常退出，任务中断");
        }
        dbExecutor.execute(() -> {
            for (PrintTaskEntity task : inProgress) {
                taskDao.update(task);
            }
        });  // ★ 子线程批量DB写
    }

    /**
     * 获取所有可续打的任务列表
     */
    public List<PrintTaskEntity> getResumableTasks() {
        List<PrintTaskEntity> result = new ArrayList<>();
        result.addAll(taskDao.findByStatus(TaskStatus.INTERRUPTED.getCode()));
        result.addAll(taskDao.findByStatus(TaskStatus.PAUSED.getCode()));
        result.addAll(taskDao.findByStatus(TaskStatus.PENDING.getCode()));
        // 按更新时间倒序，最近的排前面
        result.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return result;
    }

    /**
     * 获取指定学生的可续打任务
     */
    public List<PrintTaskEntity> getResumableTasks(String targetId) {
        return taskDao.findResumableByTargetId(targetId);
    }
}
```

### 9.4 打印前台 Service

```java
/**
 * 打印期间保持 App 存活的前台服务
 * 防止系统在打印过程中回收 App
 *
 * ★ Android 13+ (API 33) 需要 POST_NOTIFICATIONS 运行时权限
 * 在启动前台服务前需检查并请求该权限
 */
public class PrintService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "print_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // 启动前台通知
        startForeground(NOTIFICATION_ID, buildNotification("正在打印..."));
    }

    /**
     * 更新打印进度通知
     */
    public void updateNotification(int current, int total, int pageIndex) {
        String text = String.format("正在打印第 %d/%d 页 (page_%d)", current, total, pageIndex);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    /**
     * 打印完成，停止服务
     */
    public void notifyComplete() {
        stopForeground(true);
        stopSelf();
    }
}
```

### 9.5 打印界面生命周期保护

```java
public class PrintProgressActivity extends BaseActivity {
    private PrintEngine printEngine;
    private PrintTaskEntity currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ★ 保持屏幕常亮，避免发送数据时黑屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ★ 使用 OnBackPressedDispatcher（替代弃用的 onBackPressed）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.btnPauseOrCancel.getVisibility() == View.VISIBLE) {
                    binding.btnPauseOrCancel.performClick();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * Activity 销毁时保存状态
     * 场景 B：App 被系统回收
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentTask != null && printEngine.isPrinting()) {
            // 标记为中断，下次可续打
            currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            printEngine.getDbExecutor().execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
        }
    }
}
```

### 9.6 打印机断连处理

```java
public class PrintEngine {
    /**
     * 监听打印机断连事件
     * 场景 D：打印中途蓝牙/WiFi断开
     * ★ 通过 OnDeviceConnectListener.onDeviceDisconnect 回调接入
     */
    public void onDeviceDisconnected() {
        if (isPrinting && currentTask != null) {
            // 当前正在打印的页不记入 printedPages
            // （因为未收到该页的 onPrintComplete 回调）
            currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
            currentTask.setLastError("打印机断开连接");
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
            isPrinting = false;

            // ★ 尝试取消正在发送的数据（仅在数据传输阶段有效）
            // 如果数据已传输完毕（onDataProgressFinish 已触发），此方法为空操作（isStartSendingData=false）
            // 物理打印阶段断连：打印机端停止打印，onPrintComplete 不再触发，当前页不记入 printedPages
            ConnectManager.share().cancelSendMultiRowDataPacket();
        }
    }

    /**
     * 暂停打印（用户主动）
     * 场景 E/C
     *
     * ★ 暂停语义说明（手持移动打印机特性）：
     * 打印分两阶段：(1)数据传输 → (2)物理打印
     * - 数据传输阶段暂停：cancelSendMultiRowDataPacket 可中止传输，后续数据不再发送
     * - 物理打印阶段暂停：打印机协议无 StopPrint 指令，当前批次数据会打印完毕
     *   onPrintComplete 继续触发推进 printedPages 是正确的（这些页确实已物理打印完成）
     * 暂停 = "当前批次打完后，不再发起新打印任务"
     * UI 应提示"打印机正在输出中，当前批次将打印完毕"
     *
     * ★ 切换学生时，新数据发送到打印机会覆盖缓冲区中旧数据，
     *   旧学生未打印的页数据丢失，回来后需重新发送（printedPages 记录哪些已打过）
     */
    public void pause() {
        if (isPrinting && currentTask != null) {
            // ★ 取消正在发送的数据（仅在数据传输阶段有效）
            ConnectManager.share().cancelSendMultiRowDataPacket();

            currentTask.setStatus(TaskStatus.PAUSED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
            isPrinting = false;
        }
    }

    /**
     * 续打前检查打印机连接
     * 场景 G：数小时后回来，打印机可能已关机
     * ★ isConnected() 返回 Boolean（可null），必须 null-safe
     */
    public boolean checkPrinterReady() {
        ConnectManager cm = ConnectManager.share();
        if (!Boolean.TRUE.equals(cm.isConnected())) {
            return false;
        }
        return true;
    }
}
```

### 9.7 续打入口设计

```java
public class TaskRecoveryManager {
    /**
     * 续打完整流程
     */
    public void resumeTask(PrintTaskEntity task, ResumeCallback callback) {
        // 1. 检查打印机连接（★ null-safe）
        ConnectManager cm = ConnectManager.share();
        if (!Boolean.TRUE.equals(cm.isConnected())) {
            callback.onNeedReconnect();  // 提示先连接打印机
            return;
        }

        // 2. 检查素材是否还在
        File pagesDir = new File(task.getMaterialPath());
        if (!pagesDir.exists()) {
            callback.onMaterialLost(task);  // 素材被删除，需重新下载
            return;
        }

        // 3. 计算剩余页
        List<Integer> targetPages = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printedPages = IntegerListConverter.fromString(task.getPrintedPages());
        List<Integer> remaining = new ArrayList<>();
        for (int page : targetPages) {
            if (!printedPages.contains(page)) {
                remaining.add(page);
            }
        }

        if (remaining.isEmpty()) {
            // 已打印全部，标记完成
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskDao.update(task));  // ★ 子线程DB写
            callback.onAlreadyCompleted();
            return;
        }

        // 4. 开始续打
        callback.onResume(remaining);
    }
}

public interface ResumeCallback {
    void onNeedReconnect();
    void onMaterialLost(PrintTaskEntity task);
    void onAlreadyCompleted();
    void onResume(List<Integer> remainingPages);
}
```

### 9.8 续打 UI 交互

```
App 启动 → 检查可续打任务
    │
    ├─ 无未完成任务 → 正常流程
    │
    └─ 有未完成任务 → 弹出任务列表
         ┌─────────────────────────────────────┐
         │  以下任务尚未完成，是否继续打印？     │
         │                                     │
         │  ● 张三 - 奇数页 (7/12页) 3分钟前   │
         │    校本作业A                         │
         │                                     │
         │  ● 李四 - 全部页 (3/8页)  2小时前   │
         │    校本作业B                         │
         │                                     │
         │  ┌──────────┐ ┌──────────┐          │
         │  │ 继续选中 │ │ 稍后处理 │          │
         │  └──────────┘ └──────────┘          │
         └─────────────────────────────────────┘

    选择继续 → 检查打印机连接
        ├─ 未连接 → 跳转到设备连接界面
        │           连接成功后自动回到续打流程
        └─ 已连接 → 开始续打
```

### 9.9 多学生多任务切换设计

这是实际最高频场景：一个学生没打完，又去给另一个学生打印，之后回来继续打第一个学生的。

#### 核心原则

- **每个学生 + 每个校本 + 每个打印模式 = 独立的 PrintTask**
- 切换学生时，当前打印任务自动暂停（PAUSED），不丢失进度
- 回到主界面时，自动列出所有可续打任务
- 用户选择任一任务即可继续，无需重新走完整流程

#### 切换场景时序

```
张三 打印奇数页 (12页)
    │
    ├─ 打印完 page_85,87,89 → printedPages=[85,87,89]
    │
    ▼ 用户切换到李四
    │
    ├─ 张三任务自动暂停 → status=PAUSED, printedPages=[85,87,89]
    │
    ├─ 李四创建新任务 → 开始打印
    ├─ 李四打印完 page_85,86 → printedPages=[85,86]
    │
    ▼ 用户想回来继续张三的
    │
    ├─ 李四任务自动暂停 → status=PAUSED, printedPages=[85,86]
    │
    ├─ 主界面/任务列表 显示：
    │   ● 张三 - 校本作业A - 奇数页 (3/12页) ← 用户选择这个
    │   ● 李四 - 校本作业A - 全部页 (2/8页)
    │
    ├─ 张三任务恢复 → status=IN_PROGRESS
    ├─ 继续从 page_91 开始打印（自动跳过已完成的85,87,89）
    │
    ▼ 张三全部打完
    │
    ├─ 张三任务 → status=COMPLETED
    │
    ├─ 主界面/任务列表 更新：
    │   ● 李四 - 校本作业A - 全部页 (2/8页) ← 提示可继续
    │   ● 张三 - 校本作业A - 奇数页 ✓已完成
```

#### 学生切换拦截

```java
public class PrintEngine {
    private PrintTaskEntity currentTask;
    private final AtomicBoolean isPrinting = new AtomicBoolean(false);  // ★ 线程安全

    /**
     * 切换到新学生/预铺码时调用
     * 自动暂停当前任务，确保进度不丢失
     *
     * ★ 切换后新数据发送到打印机会覆盖缓冲区中旧数据，
     *   旧学生未打印的页数据丢失，回来后续打需重新发送（printedPages 记录哪些已打过）
     */
    public void switchToNewTarget() {
        if (isPrinting && currentTask != null) {
            // 暂停当前任务
            currentTask.setStatus(TaskStatus.PAUSED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));  // ★ 子线程DB写
            isPrinting = false;
            currentTask = null;

            // 停止前台服务
            PrintService.stop();
        }
    }

    /**
     * 恢复指定任务
     */
    public void resumeTask(PrintTaskEntity task) {
        // 检查打印机连接（★ null-safe）
        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            throw new PrinterNotConnectedException();
        }

        // 检查素材存在
        if (!new File(task.getMaterialPath()).exists()) {
            throw new MaterialLostException();
        }

        // 恢复任务
        currentTask = task;
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        task.setUpdatedAt(System.currentTimeMillis());
        dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写

        // 启动前台服务
        PrintService.start();

        // 从断点继续打印
        execute(task);
    }
}
```

#### 学生列表界面集成

```java
public class StudentListActivity extends BaseActivity {
    /**
     * 点击学生时，检查是否有未完成任务
     */
    private void onStudentClick(StudentEntity student) {
        List<PrintTaskEntity> resumable = taskRecoveryManager
            .getResumableTasks(student.getStudentId());

        if (resumable.isEmpty()) {
            // 无未完成任务，正常流程（选择校本→下载→打印）
            navigateToEditionList(student);
        } else if (resumable.size() == 1) {
            // 只有一个未完成任务，直接提示继续
            showSingleTaskResumeDialog(resumable.get(0));
        } else {
            // 多个未完成任务（不同校本/不同打印模式），列出选择
            showMultiTaskResumeDialog(resumable);
        }
    }

    private void showSingleTaskResumeDialog(PrintTaskEntity task) {
        int printed = IntegerListConverter.fromString(task.getPrintedPages()).size();
        int total = IntegerListConverter.fromString(task.getTargetPages()).size();
        String modeLabel = PrintMode.fromCode(task.getPrintMode()).getLabel();

        new AlertDialog.Builder(this)
            .setTitle(task.getTargetId() + " 有未完成的打印")
            .setMessage(String.format("%s，已打印 %d/%d 页", modeLabel, printed, total))
            .setPositiveButton("继续打印", (d, w) -> {
                // 先暂停当前打印任务（如果有）
                printEngine.switchToNewTarget();
                // 恢复该任务
                navigateToResume(task);
            })
            .setNegativeButton("重新开始", (d, w) -> {
                navigateToEditionList(student);
            })
            .setNeutralButton("取消任务", (d, w) -> {
                task.setStatus(TaskStatus.CANCELLED.getCode());
                dbExecutor.execute(() -> taskRepo.update(task));  // ★ 子线程DB写
            })
            .show();
    }
}
```

#### 任务列表全局入口

```java
// 在 MainActivity 中添加"未完成任务"入口
public class MainActivity extends BaseActivity {
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到主界面，更新未完成任务角标
        List<PrintTaskEntity> resumable = taskRecoveryManager.getResumableTasks();
        updateTaskBadge(resumable.size());
    }

    private void onTaskListClick() {
        List<PrintTaskEntity> resumable = taskRecoveryManager.getResumableTasks();
        if (resumable.isEmpty()) {
            showToast("没有未完成的打印任务");
            return;
        }
        // 跳转到任务列表界面
        navigateToTaskList(resumable);
    }
}
```

#### 任务列表界面

```
┌─────────────────────────────────────┐
│  未完成的打印任务                    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 张三  校本作业A  奇数页      │    │
│  │ ████████░░░░  3/12页        │    │
│  │ 3分钟前暂停                  │    │
│  │              [继续] [取消]   │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 李四  校本作业A  全部页      │    │
│  │ ██████░░░░░░  2/8页         │    │
│  │ 5分钟前暂停                  │    │
│  │              [继续] [取消]   │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 王五  校本作业B  偶数页      │    │
│  │ ░░░░░░░░░░░░  0/10页        │    │
│  │ 1小时前中断                  │    │
│  │              [继续] [取消]   │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

## 10. 重打指定页设计

### 10.1 重打的两种模式

| 模式 | 说明 | 状态 |
|------|------|------|
| **指令重打模式** | App 发送 jzPrint 扩展指令（0x030A），打印机从缓冲区重打指定拼 | ✅ 已实现 |
| **重新发送模式** | App 重新加载素材并完整发送数据给打印机 | ✅ 兜底方案 |

### 10.2 指令重打模式（0x030A）

jzPrint 为打印机扩展了专用指令 `opcode=0x030A`，参数 1 byte（拼索引）。打印机固件保留已接收数据的缓冲区，App 只需发送指令，打印机直接从缓冲区重打。**不修改 SDK，仅通过 `ConnectManager.sendCommand()` 通用接口发送。**

```
用户看到 Anoto 没打好 → 进度页点「重打指定页」
    ↓
弹出 BottomSheet，列出所有 targetPages（✅/⬜ 标记已打印/未打印）
    ↓
默认选中最后打印的页，用户可改选任意页
    ↓
确认 → PrintEngine.reprintSpecifiedPage(puzzleIndex)
    → sendCommand(0x030A, [(byte)puzzleIndex])
    → isReprintMode = true
    ↓
用户按打印机按钮 → 打印指定拼
    ↓
onPhysicalPrintComplete 检测到重打模式 → 跳过进度推进 → 恢复正常
```

**关键实现（PrintEngine.java）**：
```java
// 扩展指令 opcode
private static final int OPCODE_REPRINT_PAGE = 0x030A;

// 重打模式标志（volatile 保证 SDK 线程可见）
private volatile boolean isReprintMode = false;
private volatile int reprintTargetPuzzleIndex = -1;

public void reprintSpecifiedPage(int puzzleIndex) {
    // 校验 + 设置标志 + 发送指令
    isReprintMode = true;
    reprintTargetPuzzleIndex = puzzleIndex;
    ConnectManager.share().sendCommand(OPCODE_REPRINT_PAGE, new byte[]{(byte) puzzleIndex});
}

// onPhysicalPrintStart/Complete 中检测重打模式，跳过正常进度记录
```

**UI（PrintProgressActivity）**：PRINT 阶段底部显示「重打指定页」按钮 → 弹出单选页面列表（✅/⬜ 标记）→ 确认发送指令 + Toast 提示按按钮。

### 10.3 重新发送模式（兜底方案）

`PrintEngine.reprintPages(task, pages)` → 从 `printedPages` 中移除目标页 → 重新加载 Bitmap 并完整发送。

入口：任务详情页（TaskDetailActivity）支持多选重打。

### 10.4 两种模式对比

| 维度 | 指令重打模式 | 重新发送模式 |
|------|------------|------------|
| **数据传输** | 仅 3 字节指令 | 完整传输整页数据 |
| **耗时** | 几乎瞬间 | 与正常打印相同 |
| **入口** | 打印进度页 BottomSheet | 任务详情页 |
| **选择** | 单选任意 targetPages 中的页 | 多选已打印页 |
| **printedPages** | 不变 | 移除→重打→记入 |
| **UI 操作** | 进度页 → 弹窗 → 确认 | 详情页 → 多选 → 确认 |

## 11. Bitmap 内存管理

### 11.1 风险

单页 Bitmap 可达 360×2372≈3.3MB（ARGB_8888），连续打印大量页面若无回收会导致 OOM。

### 11.2 策略

```java
// PrintEngine 中管理当前页 Bitmap
private Bitmap currentPageBitmap;

private void printNextPage(PrintTask task, List<Integer> remaining, int index) {
    // 回收上一页 Bitmap
    if (currentPageBitmap != null && !currentPageBitmap.isRecycled()) {
        currentPageBitmap.recycle();
        currentPageBitmap = null;
    }

    int pageIndex = remaining.get(index);
    currentPageBitmap = materialLoader.loadPage(task.getMaterialPath(), pageIndex);
    // ...
}
```

- 每页打印完成后立即 `recycle()`
- 同时只持有一页 Bitmap
- 自定义拼接时 `ImageMerger.mergeVertically()` 内部子图已及时 recycle

## 12. 空间管理设计

### 12.1 存储策略

| 内容 | 保留策略 |
|------|----------|
| 压缩包（package.zip） | 解压成功后可删除 |
| 解压后素材（pages/） | 打印完成后可选择性删除 |
| 学生信息快照（info.json） | 长期保留（体积小） |
| 数据库记录 | 长期保留 |

### 12.2 StorageManager

```java
public class StorageManager {
    /**
     * 获取素材总占用空间
     */
    public long getMaterialTotalSize() {
        File dir = new File(context.getFilesDir(), "materials");
        return calculateDirSize(dir);
    }

    /**
     * 清理已完成任务的素材
     */
    public void cleanupCompletedMaterials() {
        List<PrintTaskEntity> completed = taskDao.findByStatus(TaskStatus.COMPLETED.getCode());
        for (PrintTaskEntity task : completed) {
            // 删除压缩包
            String zipPath = MaterialPathBuilder.getZipPath(task.getMaterialPath());
            new File(zipPath).delete();
            // 可选：删除解压文件
            // deleteDirectory(task.getMaterialPath());
        }
    }
}
```
