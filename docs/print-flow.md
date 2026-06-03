# 简作 (jzPrint) 打印流程设计文档

> 版本：v1.0  
> 日期：2026-05-26

## 1. 完整业务流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 小程序分享   │───►│ 查询校本列表 │───►│ 选择学生/   │───►│ 下载素材    │
│ schoolId    │    │             │    │ 预铺码      │    │ (如已缓存   │
└─────────────┘    └─────────────┘    └─────────────┘    │  则跳过)    │
                                                         └──────┬──────┘
                                                                ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 打印完成     │◄───│ 逐页打印    │◄───│ 创建打印任务 │◄───│ 选择打印模式│
│ (更新状态)   │    │ (1页=1拼)   │    │             │    │ 奇/偶/全部  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

## 2. 打印流程详细时序

### 2.1 正常打印流程（MultiRow 一次发送）

```
用户                    PrintEngine             mxSdk              数据库
 │                         │                       │                  │
 │──选择奇数页模式────────►│                       │                  │
 │                         │──创建PrintTask───────│                  │──INSERT
 │                         │  targetPages=[85,87,89]│                  │
 │                         │                       │                  │
 │                         │──加载所有目标页Bitmap─│                  │
 │                         │  loadPage(85,87,89)   │                  │
 │                         │                       │                  │
 │                         │──★旋转90°(纵向→横向  │                  │
 │                         │  根据页面侧+方向自动决策│                  │
 │                         │  360×1186 → 1186×360  │                  │
 │                         │──★补白到打印头高度552  │                  │
 │                         │  1186×360 → 1186×552  │                  │
 │                         │  按对齐方式(顶/底)放置 │                  │
 │                         │  补白后每页1个RowData  │                  │
 │                         │                       │                  │
 │                         │──逐页生成MultiRowImage│                  │
 │                         │  合并为1个MultiRowImage│                  │
 │                         │                       │                  │
 │                         │──生成MultiRowData─────│                  │
 │                         │  bitmap2MultiRowData()│                  │
 │                         │                       │                  │
 │◄─发送数据中────────────│──sendMultiRowData()──►│                  │
 │◄─发送进度100%──────────│◄──onDataProgressFinish│                  │
 │                         │                       │                  │
 │                         │                       │──onPrintStart───►│
 │◄─"打印page_85"─────────│  (0,2,0)             │  →记录Progress   │
 │                         │                       │                  │
 │                         │                       │──onPrintComplete►│
 │◄─"page_85完成"─────────│  (0,2,0)             │  →★记录page_85  │──UPDATE
 │                         │                       │                  │  printedPages
 │                         │                       │                  │  =[85]
 │                         │                       │──onPrintStart───►│
 │◄─"打印page_87"─────────│  (0,2,1)             │  →记录Progress   │
 │                         │                       │                  │
 │                         │                       │──onPrintComplete►│
 │◄─"page_87完成"─────────│  (0,2,1)             │  →★记录page_87  │──UPDATE
 │                         │                       │                  │  printedPages
 │                         │                       │                  │  =[85,87]
 │                         │                       │──onPrintStart───►│
 │◄─"打印page_89"─────────│  (0,2,2)             │                  │
 │                         │                       │──onPrintComplete►│
 │◄─"page_89完成"─────────│  (0,2,2)             │  →★记录page_89  │──UPDATE
 │                         │  currentIndex==endIndex│  status=COMPLETED│
 │                         │                       │                  │  =[85,87,89]
```

**核心**：所有目标页合并为**一个** MultiRowData **一次**发送，打印机连续打印无停顿。1页=1拼，`onPrintComplete` 每回调1次=完成1页。

### 2.2 多学生切换流程（高频场景）

```
用户                    PrintEngine             数据库
 │                         │                       │
 │──张三-奇数页-开始打印──►│                       │
 │                         │──创建Task_张三────────►│──INSERT
 │                         │                       │
 │◄─page_85,87,89完成────│                       │──UPDATE
 │                         │  printedPages=[85,87,89]│
 │                         │                       │
 │──切换到李四────────────►│                       │
 │                         │──★暂停Task_张三──────►│──UPDATE
 │                         │  status=PAUSED        │  张三进度不丢失
 │                         │                       │
 │                         │──创建Task_李四────────►│──INSERT
 │◄─李四page_85,86完成────│                       │──UPDATE
 │                         │  printedPages=[85,86] │
 │                         │                       │
 │──想回来继续张三────────►│                       │
 │                         │──★暂停Task_李四──────►│──UPDATE
 │                         │  status=PAUSED        │  李四进度不丢失
 │                         │                       │
 │                         │──查询可续打任务───────►│
 │                         │◄──[Task_张三,Task_李四]│
 │                         │                       │
 │◄─列出：张三(3/12) 李四(2/8)│                    │
 │                         │                       │
 │──选择张三继续──────────►│                       │
 │                         │──resumeTask(张三)────►│──UPDATE
 │                         │  remaining=[91,93,...]│  status=IN_PROGRESS
 │◄─从page_91继续打印────│                       │
```

**关键点**：
- 切换学生时自动暂停当前任务，**进度零丢失**
- 每个学生的任务完全独立，互不影响
- 回来时从任务列表选择即可，无需重新走下载/选择流程

### 2.3 断点续打流程

```
用户                    PrintEngine             数据库
 │                         │                       │
 │──打开App───────────────│                       │
 │                         │──findResumable()─────►│
 │                         │◄──PrintTask──────────│
 │                         │  (status=INTERRUPTED, │
 │                         │   printedPages=[85])  │
 │                         │                       │
 │◄─提示"有未完成任务"────│                       │
 │  "已打印1/3页，是否继续？"│                     │
 │                         │                       │
 │──确认继续──────────────►│                       │
 │                         │──计算剩余页──────────│
 │                         │  target=[85,87,89]    │
 │                         │  printed=[85]         │
 │                         │  remaining=[87,89]    │
 │                         │                       │
 │                         │──只将[87,89]合并─────│
 │                         │  为MultiRowData发送   │
 │◄─打印机连续打印87,89──│  (不再重发已完成的85) │
```

### 2.4 重打指定页流程

#### 指令重打模式（0x030A — 进度页触发）

```
用户                    PrintProgressActivity    PrintEngine              打印机
 │                         │                       │                        │
 │──点击「重打指定页」────►│                       │                        │
 │                         │──弹出BottomSheet─────│                        │
 │                         │  列出所有targetPages  │                        │
 │                         │  ✅/⬜ 标记已打印/   │                        │
 │                         │  未打印               │                        │
 │──选择 page_87──────────►│                       │                        │
 │──点击「确认重打」──────►│                       │                        │
 │                         │──reprintSpecifiedPage(puzzleIndex)───────────►│
 │                         │                       │──sendCommand(0x030A,  │
 │                         │                       │    [puzzleIndex])    │
 │                         │                       │──isReprintMode=true  │
 │◄──Toast: 请按按钮──────│                       │                        │
 │                         │                       │                        │
 │──按打印机按钮───────────│                       │◄──打印机打印指定拼────│
 │                         │                       │──onPrintComplete────►│
 │                         │                       │  检测重打模式→跳过    │
 │                         │                       │  onPageComplete       │
 │                         │                       │  恢复正常进度          │
```

**关键**：指令重打不推进 `printedPages`（页面已记录），`onPhysicalPrintStart/Complete` 检测 `isReprintMode` 标志跳过正常进度记录。

#### 重新发送模式（任务详情页触发）

```
用户                    TaskDetailActivity         PrintEngine             数据库
 │                         │                       │                   │
 │──查看已完成任务────────►│                       │                   │
 │──勾选87,91，确认重打──►│──reprintPages(task,    │                   │
 │                         │    [87,91])──────────►│                   │
 │                         │                       │──移除printedPages│──UPDATE
 │                         │                       │  →[85,89]         │
 │                         │                       │──重新发送数据────│
 │◄─打印87,91────────────│                       │                   │
 │◄─全部完成──────────────│                       │──记入printedPages│──UPDATE
```

### 2.5 打印异常流程

```
打印过程中异常（断电/崩溃/蓝牙断开）：

1. 异常发生前：
   - PrintTask.printedPages = [85,87]  ← 已实时保存
   - PrintTask.status = 1 (进行中)

2. 异常发生后：
   - 数据库中 printedPages 仍为 [85,87]
   - status 仍为 1（或可能为 4，取决于异常类型）

3. App 重新启动：
   - findUnfinished() 查到该任务
   - remaining = [89]（自动跳过已打印的85、87）
   - 从 page_89 继续打印
```

## 3. SDK 回调与进度记录

### 3.1 回调链路（MultiRow 一次发送模式）

本项目中，将所有目标页合并为一个 MultiRowData 一次发送。1页=1拼，每拼恰好1个RowData。

```
ConnectManager.setWithSendMultiRowDataPacket(data)  ← 包含N拼数据
    │
    ├── OnDataProgressListener.onDataProgressStart()     ← 全部数据开始发送
    │   → UI: "发送数据 0%"
    │
    ├── OnDataProgressListener.onDataProgress()           ← 数据发送进度
    │   → UI: "发送数据 N%"
    │
    ├── OnDataProgressListener.onDataProgressFinish()     ← 全部数据发送完成
    │   → UI: "数据已发送，等待打印"
    │
    ├── OnPrintListener.onPrintStart(0, N-1, 0)          ← 第0拼开始打印
    │   → 通过 targetPages[0] 映射为 page_85
    │   → UI: "正在打印 page_85"
    │
    ├── OnPrintListener.onPrintComplete(0, N-1, 0, "墨盒ID") ← 第0拼打印完成
    │   → ★ 通过 targetPages[0] 映射为 page_85
    │   → ★ 更新 printedPages，追加 85
    │   → UI: "page_85 打印完成"
    │
    ├── OnPrintListener.onPrintStart(0, N-1, 1)          ← 第1拼开始打印
    │   → UI: "正在打印 page_87"
    │
    └── ... 直到最后一拼完成
```

**拼索引→页码映射**：`targetPages` 按发送顺序排列，`targetPages[currentIndex]` = 当前完成的页码。

### 3.2 回调注册

```java
// 在 PrintEngine 或 PrintProgressManager 初始化时注册
ConnectManager.share().registerPrintListener(new ConnectManager.OnPrintListener() {
    @Override
    public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
        progressManager.onSdkPrintStart(beginIndex, endIndex, currentIndex);
    }

    @Override
    public void onPrintComplete(int beginIndex, int endIndex,
                                int currentIndex, String cartridgeId) {
        progressManager.onSdkPrintComplete(beginIndex, endIndex, currentIndex, cartridgeId);
    }
});

// 在合适时机注销
// ConnectManager.share().unregisterPrintListener(printListener);
```

### 3.3 OnDataProgressListener

```java
// PrintEngine.sendToPrinter() 中注册
connectManager.registerDataProgressListener(new ConnectManager.OnDataProgressListener() {
    @Override
    public void onDataProgressStart(float size, int progress, long startTime) {
        callback.onProgress(0);
    }

    @Override
    public void onDataProgress(float size, int progress, long startTime, long currentTime) {
        callback.onProgress(progress);
    }

    @Override
    public void onDataProgressFinish(float size, long startTime, long currentTime) {
        callback.onComplete();
    }

    @Override
    public void onDataProgressError(String error, int code) {
        callback.onError(error);
    }
});
```

## 4. 打印模式与页面映射

### 4.1 页码计算示例

假设素材包含 page_85 到 page_108，共 24 页：

| 打印模式 | 目标页码 | 页数 |
|----------|---------|------|
| 全部 | [85,86,87,...,108] | 24 |
| 奇数 | [85,87,89,91,93,95,97,99,101,103,105,107] | 12 |
| 偶数 | [86,88,90,92,94,96,98,100,102,104,106,108] | 12 |

### 4.2 双人协作映射

```
设备A（奇数页模式）：                设备B（偶数页模式）：
  page_85 ← 本子左侧第1张           page_86 ← 本子右侧第1张
  page_87 ← 本子左侧第2张           page_88 ← 本子右侧第2张
  page_89 ← 本子左侧第3张           page_90 ← 本子右侧第3张
  ...                                ...

奇数页 + 偶数页 = 全部页（无重复、无遗漏）
```

## 5. 打印数据生成链路

### 5.1 打印数据生成链路

```
原图 Bitmap (360 x N)        ← 纵向窄长条，如 360×1186
    │
    ▼ ★ 旋转90° (根据设置自动决策 CW_90 或 CCW_90)
    │  纵向(360×1186) → 横向(1186×360)
    │
    ▼ ★ 补白到打印头高度 552px (PrintImagePreparer.prepare)
    │  创建 1186×552 白色画布
    │  将旋转后的图放到画布上（按对齐方式：顶部/底部）
    │  顶部对齐：y=0，底部留白 552-360=192px
    │  底部对齐：y=192，顶部留白 192px
    │
    ▼ MultiRowImageFactory.image2MultiRowImage()
    │  参数：RowLayoutDirectionVertical
    │  高度=552，只拆出1个RowImage=1拼
    │  输出：MultiRowImage（包含1个RowImage）
    │
    ▼ MultiRowDataFactory.bitmap2MultiRowData()
    │  高度已=552，SDK不再缩放
    │  输出：MultiRowData（包含1个RowData = 1拼 = 1页）
    │
    ▼ ConnectManager.setWithSendMultiRowDataPacket()
    │  回调：OnDataProgressListener（发送进度）
    │        OnPrintListener（打印状态，1次回调=1页完成）
    │
    ▼ 打印机输出
```

**★ 核心认知**：
- 原图纵向窄长条 (360×H)，旋转 90° 后横向宽矮 (H×360)
- 旋转后高度=360px < 打印头 552px，每页恰好 1 个 RowData = 1 拼
- `OnPrintListener.onPrintComplete` 每回调 1 次 = 1 页完成
- `targetPages[currentIndex]` = 当前完成的页码

### 5.2 点阵图片特性

- 宽度固定 360px（与打印机打印头宽度匹配）
- 高度不固定，取决于该页包含的点阵内容数量
- 示例尺寸对比：

| 页码 | 完整页尺寸 | 说明 |
|------|-----------|------|
| page_85 | 360 x 1186 | 5 个单点阵 (5×237=1185) |
| page_88 | 360 x 475 | 2 个单点阵 (2×237=474) |
| page_91 | 360 x 311 | 非标准高度 |
| page_100 | 360 x 2372 | 10 个单点阵 |
| page_103 | 360 x 949 | 4 个单点阵 (4×237=948) |

- 单点阵标准高度约 237px，但不同题目高度可能不同
- 拼接时各子图按文件名排序后纵向拼接，无间隔

## 6. 素材下载与解压流程

### 6.1 流程时序

```
用户                    DownloadService          API服务器           本地文件系统        数据库
 │                         │                       │                   │                │
 │──选择学生──────────────►│                       │                   │                │
 │                         │──检查本地缓存─────────│                   │──检查pages/──│
 │                         │                       │                   │                │
 │                         │──已有?───────────────│                   │◄──已解压──────│
 │                         │  是 → 直接返回        │                   │                │
 │                         │  否 → 继续下载        │                   │                │
 │                         │                       │                   │                │
 │◄─下载进度0%────────────│──getMaterialUrl()────►│                   │                │
 │                         │◄──url────────────────│                   │                │
 │                         │                       │                   │                │
 │◄─下载进度30%───────────│──downloadFile()──────►│                   │                │
 │◄─下载进度60%───────────│◄──chunk──────────────│                   │                │
 │◄─下载进度100%──────────│◄──complete───────────│──保存package.zip─►│                │
 │                         │                       │                   │                │──UPDATE
 │                         │                       │                   │                │  status=2
 │                         │                       │                   │                │
 │◄─解压中────────────────│──unzip()──────────────│                   │                │
 │                         │                       │                   │──解压到pages/►│
 │◄─解压完成──────────────│                       │                   │                │──UPDATE
 │                         │                       │                   │                │  status=3
 │                         │──可删除zip────────────│                   │──删除zip─────►│
 │                         │                       │                   │                │
 │◄─素材就绪──────────────│                       │                   │                │
```

### 6.2 压缩包预期结构

```zip
package.zip
├── page_85.png
├── page_85/
│   ├── 1.png
│   ├── 2.png
│   └── ...
├── page_86.png
├── page_86/
│   └── ...
└── ...
```

解压后直接展开到 `pages/` 目录下，保持原有结构。

## 7. 异常处理

### 7.1 打印异常

| 异常场景 | 处理方式 | 任务状态变化 |
|----------|----------|-------------|
| 蓝牙/WiFi断开 | `onDeviceDisconnected()` → 标记任务中断，当前页不记入printedPages | IN_PROGRESS → INTERRUPTED |
| 打印机缺纸/开盖 | SDK返回错误，暂停打印，提示用户处理 | IN_PROGRESS → INTERRUPTED |
| 数据发送失败 | `OnDataProgressListener.onDataProgressError()`，记录错误 | IN_PROGRESS → INTERRUPTED |
| App崩溃/被杀 | 重启后 `recoverOnStartup()` 将IN_PROGRESS改为INTERRUPTED | IN_PROGRESS → INTERRUPTED |
| 用户按返回退出 | 弹确认对话框 → 暂停任务（★当前批次数据会打印完毕，协议无StopPrint指令） | IN_PROGRESS → PAUSED |
| 用户主动取消 | 保留已打印页记录，任务标记取消 | IN_PROGRESS → CANCELLED |
| 打印机长时间断连后回来 | 检测连接状态，未连接提示先连接 | 不变，等待用户操作 |
| 切换学生发送新数据 | 旧数据被覆盖，`printedPages`记录已打印页，回来后续打重新发送未打印页 | 旧任务→PAUSED |

### 7.2 下载异常

| 异常场景 | 处理方式 |
|----------|----------|
| 网络中断 | Material.status = 4，提示用户重试 |
| 压缩包损坏 | 解压失败，删除损坏文件，提示重新下载 |
| 空间不足 | 下载前检查可用空间，不足时提示清理 |

### 7.3 断点续打边界情况

| 场景 | 处理方式 |
|------|----------|
| 已打印全部页面但 status 未更新 | 续打前检查 `printedPages.containsAll(targetPages)`，自动标记完成 |
| 原素材被删除 | 续打时检查文件存在性，提示素材丢失需重新下载 |
| 页码不连续（如 page_85,86,88 跳过87） | PageSelector 基于实际存在的文件，非连续假设 |
| 同一学生多个未完成任务 | 列出所有可续打任务，用户选择继续哪个 |
| 打印机未连接 | 续打前检查 `ConnectManager.isConnected()`，未连接则跳转设备连接页 |
| 续打时App再次崩溃 | printedPages 已保存到上次完成的页，重启后继续从断点恢复 |
| 当前页打印中崩溃（已发出但未收到完成回调） | 该页不在 printedPages 中，下次会重打该页（可接受的重复） |

### 7.4 printedPages 一致性

**核心原则**：仅在 SDK `onPrintComplete` 回调触发后才将页码写入 `printedPages`。1页=1拼，每次回调即1页完成。

```
                     printedPages
                     ┌─────┐
page_85 开始打印    │ [ ] │  ← 不记入
page_85 完成回调    │[85] │  ← ★ 立即持久化
page_87 开始打印    │[85] │  ← 不记入
  ┤ App 崩溃 ├     │[85] │  ← 已持久化，不丢失
  重启后续打        │[85] │  ← 从 page_87 重新开始
page_87 完成回调    │[85,87]│ ← ★ 继续推进
page_89 完成回调    │[85,87,89]│ → 全部完成
```

## 8. UI 交互流程

### 8.1 打印模式选择界面

```
┌─────────────────────────────┐
│      选择打印模式            │
│                             │
│  ┌─────────────────────┐    │
│  │  全部页  (24页)      │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │  奇数页  (12页)      │    │
│  │  本子左侧            │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │  偶数页  (12页)      │    │
│  │  本子右侧            │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  素材模式：          │    │
│  │  ○ 使用已拼接图片    │    │
│  │  ○ 自定义拼接        │    │
│  └─────────────────────┘    │
│                             │
│       [开始打印]            │
└─────────────────────────────┘
```

### 8.2 打印进度界面

```
┌─────────────────────────────┐
│  打印进度                    │
│                             │
│  准备数据  ████████░░  58%   │
│  3/5 页                     │
│                             │
│  发送数据  ██████████ 100%   │
│  完成                       │
│                             │
│  打印进度  ████░░░░░░  40%   │
│  2/5 页                     │
│                             │
│  正在打印...                 │
│                             │
│  ┌─────────────────────────┐│
│  │打印设置                  ││
│  │排版：奇数页在右侧 |       ││
│  │左侧：从上往下 |          ││
│  │右侧：从下往上            ││
│  └─────────────────────────┘│
│                             │
│  [85✓][87✓][89→][91][93]   │
│  [重打指定页] [暂存退出]     │
└─────────────────────────────┘
```

### 8.3 打印设置界面

```
┌─────────────────────────────┐
│  打印设置                    │
│                             │
│  书本排版                    │
│  ○ 奇数页在右侧（默认）      │
│  ○ 奇数页在左侧              │
│                             │
│  打印方向                    │
│  左侧页面                    │
│  ○ 从上往下滑动（默认）      │
│  ○ 从下往上滑动              │
│                             │
│  右侧页面                    │
│  ○ 从上往下滑动（默认）      │
│  ○ 从下往上滑动              │
└─────────────────────────────┘
```

```
┌─────────────────────────────┐
│  发现未完成的打印任务         │
│                             │
│  学生：张三                  │
│  模式：奇数页 (12页)         │
│  已打印：7/12 页             │
│  上次打印：5分钟前           │
│                             │
│  ┌──────────┐ ┌──────────┐  │
│  │ 继续打印 │ │ 放弃重打 │  │
│  └──────────┘ └──────────┘  │
└─────────────────────────────┘
```
