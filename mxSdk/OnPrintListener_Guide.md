Created by RBQ on 2025-02-04

# OnPrintListener 事件使用指南

## 概述

`OnPrintListener` 是 MX SDK 中用于监听打印机打印状态的核心接口。当打印机开始打印或完成打印时，SDK 会通过该接口回调给应用程序，使开发者能够实时获取打印进度、当前打印索引以及墨盒信息。

该接口主要应用于多拼打印场景（例如连续打印多份相同或不同的内容），通过 `beginIndex`、`endIndex` 和 `currentIndex` 三个参数清晰地标识打印任务的起止范围和当前进度。

## 接口定义

`OnPrintListener` 是 `ConnectManager` 的内部接口，定义如下：

```java
public interface OnPrintListener {
    /**
     * 打印开始回调
     *
     * @param beginIndex   本次打印任务的起始索引（从0开始）
     * @param endIndex     本次打印任务的结束索引
     * @param currentIndex 当前正在打印的索引
     */
    void onPrintStart(int beginIndex, int endIndex, int currentIndex);

    /**
     * 打印完成回调
     *
     * @param beginIndex   本次打印任务的起始索引（从0开始）
     * @param endIndex     本次打印任务的结束索引
     * @param currentIndex 当前打印完成的索引
     * @param cartridgeId  墨盒ID（可能为空字符串）
     */
    void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId);
}
```

## 注册与注销

### 获取 ConnectManager 实例

`ConnectManager` 采用单例模式，可以通过 `share()` 方法获取实例。您可以选择以下两种方式之一：

**方式一：直接使用单例（推荐）**
```java
// 直接使用 ConnectManager.share() 进行链式调用
ConnectManager.share().registerPrintListener(listener);
```

**方式二：存储局部引用**
```java
// 如果需要多次使用，可以存储为局部变量
ConnectManager connectManager = ConnectManager.share();
connectManager.registerPrintListener(listener);
```

### 注册监听器

调用 `registerPrintListener()` 方法注册 `OnPrintListener`：

```java
ConnectManager.share().registerPrintListener(new ConnectManager.OnPrintListener() {
    @Override
    public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
        // 处理打印开始事件
    }

    @Override
    public void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId) {
        // 处理打印完成事件
    }
});
```

### 注销监听器

当不再需要接收打印事件时，调用 `unregisterPrintListener()` 方法注销监听器：

```java
// 需要传入与注册时相同的监听器实例
ConnectManager.share().unregisterPrintListener(listener);
```

**注意**：为了避免内存泄漏，建议在 Activity/Fragment 的 `onDestroy()` 或适当生命周期中注销监听器。

## 事件触发时机

### onPrintStart 触发时机

当打印机开始打印**每一拼**数据时触发。例如：
- 发送了3拼数据（索引 0、1、2）
- 开始打印第0拼时：`onPrintStart(0, 2, 0)`
- 开始打印第1拼时：`onPrintStart(0, 2, 1)`
- 开始打印第2拼时：`onPrintStart(0, 2, 2)`

### onPrintComplete 触发时机

当打印机完成**每一拼**数据的打印时触发。例如：
- 第0拼打印完成：`onPrintComplete(0, 2, 0, "墨盒ID")`
- 第1拼打印完成：`onPrintComplete(0, 2, 1, "墨盒ID")`
- 第2拼打印完成：`onPrintComplete(0, 2, 2, "墨盒ID")`

## 参数详解

### beginIndex（起始索引）

- **类型**：`int`
- **说明**：本次打印任务的起始索引，从0开始计数。
- **示例**：如果发送了3拼数据，则 `beginIndex` 始终为0。

### endIndex（结束索引）

- **类型**：`int`
- **说明**：本次打印任务的结束索引。
- **示例**：如果发送了3拼数据，则 `endIndex` 为2。
- **计算方式**：`endIndex = 总拼数 - 1`

### currentIndex（当前索引）

- **类型**：`int`
- **说明**：当前正在打印（或刚完成打印）的索引。
- **在 onPrintStart 中**：表示即将开始打印的拼索引
- **在 onPrintComplete 中**：表示刚刚完成打印的拼索引

### cartridgeId（墨盒ID）

- **类型**：`String`
- **说明**：当前打印所使用的墨盒唯一标识。
- **可能值**：
  - 打印机返回的墨盒标识字符串
  - 空字符串（当打印机未返回墨盒ID时）
- **注意**：该参数仅在 `onPrintComplete` 中提供，`onPrintStart` 不包含此参数。

## 示例代码

以下是一个完整的使用示例：

```java
public class PrintActivity extends AppCompatActivity {
    private ConnectManager.OnPrintListener printListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        
        // 初始化 SDK（需先调用 init 方法）
        ConnectManager.share().init(getApplication());
        
        // 创建打印监听器
        printListener = new ConnectManager.OnPrintListener() {
            @Override
            public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
                String message = String.format("开始打印：第 %d/%d 拼", 
                    currentIndex + 1, endIndex + 1);
                Toast.makeText(PrintActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // 更新UI显示打印进度
                updatePrintProgress(currentIndex, endIndex);
            }

            @Override
            public void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId) {
                String message = String.format("完成打印：第 %d/%d 拼，墨盒：%s", 
                    currentIndex + 1, endIndex + 1, 
                    TextUtils.isEmpty(cartridgeId) ? "未知" : cartridgeId);
                Toast.makeText(PrintActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // 如果全部打印完成
                if (currentIndex == endIndex) {
                    showPrintCompleteDialog();
                }
            }
        };
        
        // 注册监听器 - 直接使用单例模式
        ConnectManager.share().registerPrintListener(printListener);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销监听器 - 直接使用单例模式
        if (printListener != null) {
            ConnectManager.share().unregisterPrintListener(printListener);
        }
    }
    
    private void updatePrintProgress(int current, int total) {
        // 实现UI进度更新逻辑
    }
    
    private void showPrintCompleteDialog() {
        // 实现打印完成提示
    }
}
```

## 注意事项

### 1. 线程安全性
- SDK 确保所有 `OnPrintListener` 回调都在主线程执行
- 您可以直接在回调中安全地更新UI，无需额外切换线程

### 2. 多拼打印概念
- **拼（Piece）**：在 SDK 中用于表示一次打印任务中的单个打印单元
- 例如发送3拼数据（索引 0、1、2），表示连续打印3份内容

### 3. 墨盒ID处理
- 某些打印机型号可能不返回墨盒ID
- 建议对 `cartridgeId` 进行空值检查：`TextUtils.isEmpty(cartridgeId)`

### 4. 生命周期管理
- 确保在适当的时机注册和注销监听器
- 避免在 Activity 被销毁后仍然持有监听器导致内存泄漏

### 5. 事件顺序
- 正常打印流程：`onPrintStart` →（打印过程）→ `onPrintComplete`
- 对于每一拼数据，都会先触发 `onPrintStart`，再触发 `onPrintComplete`

**最后更新**：2025-02-04  
**适用机型**：所有支持多拼打印的 Inksi/MX 系列打印机