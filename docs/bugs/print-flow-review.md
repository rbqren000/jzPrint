# 打印全流程深度审查

> 审查日期：2026-05-29
> 审查范围：PrintEngine + PrintProgressActivity 全链路

---

## 【1/8】PrintEngine 状态变量 — isPrinting/cancelled/currentTask 一致性

### 状态变量矩阵

| 变量 | 类型 | 线程安全 | 设置 true 的位置 | 设置 false 的位置 |
|------|------|----------|-----------------|-------------------|
| `isPrinting` | `AtomicBoolean` | ✅ | `execute()` / `fallbackResend()` | `cancelTransfer()` / `pause()` / `switchToNewTarget()` / `onDeviceDisconnected()` / `onPhysicalPrintComplete()` / `onDataProgressError()` / `buildAndSendMultiRow` error+cancel 路径 |
| `cancelled` | `AtomicBoolean` | ✅ | `cancelTransfer()` / `pause()` / `switchToNewTarget()` / `onDeviceDisconnected()` | `execute()` / `fallbackResend()` |
| `currentTask` | 普通对象 | ⚠️ | `execute()` / `fallbackResend()` / `PrintProgressActivity.restartPrint()` | 不会被置 null |
| `phaseCallback` | 普通对象 | ⚠️ | `onCreate` 中 set | `onDestroy` 中 set null |
| `currentDataProgressListener` | 普通对象 | ⚠️ | `sendToPrinter()` | `sendToPrinter()` 入口 unregister，`onDeviceDisconnected` 中 unregister+null |

### 多线程写 currentTask.status 分析

| 线程 | 写入位置 | 写入值 |
|------|---------|--------|
| 主线程 | `cancelTransfer()` | `PAUSED` |
| SDK 回调线程 | `onPhysicalPrintComplete()` | `COMPLETED` |
| SDK 回调线程 | `onDataProgressError()` | `INTERRUPTED` |
| dbExecutor | `execute()` | `IN_PROGRESS` |
| dbExecutor | `buildAndSendMultiRow` 错误路径 | `INTERRUPTED` |

`isPrinting` + `cancelled` 两个 AtomicBoolean 形成松散互斥保护：
- `cancelTransfer`（主线程）和 `onPhysicalPrintComplete`（SDK 回调）不会同时触发：前者在 TRANSFER 阶段，后者在 PRINT 阶段
- `cancelTransfer` 和 `onDataProgressError` 通过 `cancelled` 标志互斥

### 结论

✅ 所有 AtomicBoolean 状态的读写配对正确。非线程安全的 `currentTask.status` 受 `isPrinting`/`cancelled` 松散保护，实际场景不冲突。**无 bug。**

---

## 【2/8】execute() + buildAndSendMultiRow() 全流程

### 正常流程 ✅

```
execute(task)
├─ getRemainingPages → remaining.isEmpty? → COMPLETED 返回 ✅
├─ isPrinting guard ✅
├─ isPrinting=true, cancelled=false, PrintService.start ✅
├─ progressManager.setup ✅
└─ buildAndSendMultiRow()
    ├─ onPhaseChanged(PREPARE) + onPrepareStart ✅
    ├─ for each page: load → prepare → recycle ✅
    ├─ onPhaseChanged(TRANSFER) + onPrepareComplete ✅
    ├─ cancelled check → 跳过发送 或 继续 ✅
    └─ bitmap2MultiRowData(async) → callback → sendToPrinter()
```

### Bug 2A 🔴

**sendToPrinter 缺少 cancelled 检查，与 cancelTransfer 存在竞态**

时序：

```
T1: buildAndSendMultiRow 完成 PREPARE
T2: cancelled=false → 通过检查 → 调用 bitmap2MultiRowData(async)
T3: buildAndSendMultiRow 返回，dbExecutor 空闲
T4: 用户点击"停止发送" → cancelTransfer() → cancelled=true → STOPPED 阶段
T5: bitmap2MultiRowData 异步回调触发 → sendToPrinter(data)
    ↓ 此时 cancelled=true，但 sendToPrinter 没有检查！
    ↓ 注册新 listener，调用 setWithSendMultiRowDataPacket(data)
    ↓ 数据在后台偷偷开始发送
    ↓ onDataProgressStart → 更新传输进度 UI，与 STOPPED 界面不一致
    ↓ onDataProgressFinish → onPhaseChanged(PRINT) → 覆盖 STOPPED 阶段
```

**影响**：用户点击"停止发送"看到 STOPPED 界面，但旧的回调还在后台启动了一次新的数据发送，UI 出现瞬时不一致。

**建议修复**：在 `sendToPrinter()` 入口加 `if (cancelled.get()) return;`

---

## 【3/8】sendToPrinter() — listener 生命周期 & SDK 暗桩

### 方法结构

```
sendToPrinter(data)
├─ isConnected 检查（仅日志用途）
├─ 若 currentDataProgressListener != null → unregister 旧 listener
├─ 创建新 listener（onDataProgressStart/Finish/Error）
├─ registerDataProgressListener → SDK 注册
└─ setWithSendMultiRowDataPacket(data)
    ├─ SDK 内部 !isConnected? → 静默 return（无回调）
    ├─ SDK 内部 isDataSynchronize? → notifyDataProgressError
    └─ SDK 内部 !commandQueue.isEmpty? → notifyDataProgressError
```

### Listener 生命周期 ✅

正常 → 取消 → 重新发送 的完整序列中，每次 `sendToPrinter` 都先 unregister 旧 listener 再注册新的。逻辑正确。

### SDK 暗桩分析 ✅

`setWithSendMultiRowDataPacket` 的 3 个静默/错误返回点都已被理解，且 `onDeviceDisconnected` 作为兜底处理。

### 结论

**与 Bug 2A 相同**：`sendToPrinter` 入口缺少 `cancelled` 检查。已记录。

---

## 【4/8】cancelTransfer / pause / switchToNewTarget 一致性

### 三者对比

| 操作 | cancelTransfer | pause | switchToNewTarget |
|------|:---:|:---:|:---:|
| `cancelled.set(true)` | ✅ | ✅ | ✅ |
| `cancelSendMultiRowDataPacket()` | ✅（先） | ✅（先） | ✅（在 isPrinting=false 之后） |
| `currentTask → PAUSED` | ✅ | ✅ | ✅ |
| DB update 入队 | ✅ | ✅ | ✅ |
| `isPrinting.set(false)` | ✅ | ✅ | ✅ |
| `onPhaseChanged(STOPPED)` | ✅ | ❌ | ❌ |
| 入口守卫 `isPrinting && currentTask` | ❌ | ✅ | ✅ |

### 发现

**🟡 发现 4A**：`switchToNewTarget` 中 `cancelSendMultiRowDataPacket` 调用在 `isPrinting=false` 之后，与其他两方法顺序不一致。功能上无影响（SDK 内部检查的是自己的 `isStartSendingData`，不是我们的 `isPrinting`），仅风格不一致。

**🟡 发现 4B**：`pause()` 被 `btnCancel`（取消打印）和 `onDestroy` 调用后，又立即被调用方覆盖为 `CANCELLED` / `INTERRUPTED`。DB 写了两次（PAUSED → CANCELLED/INTERRUPTED），浪费但不影响正确性。

### 结论

✅ 三个方法的职责分工明确、功能正确。无功能性 bug。两个小瑕疵不紧急。

---

## 【5/8】onPhysicalPrintComplete/Start & onDeviceDisconnected

### onPhysicalPrintComplete ✅

```
onPhysicalPrintComplete(begin, end, current, cartridgeId)
├─ currentTask==null? → return ✅
├─ progressManager==null? → return ✅
├─ getPageByPuzzleIndex → onPageComplete → 更新 printedPages ✅
├─ onSdkPrintComplete ✅
├─ phaseCallback?.onPhysicalPrintPageProgress ✅
└─ currentIndex==endIndex?
    ├─ COMPLETED + isPrinting=false + PrintService.notifyComplete ✅
    └─ phaseCallback?.onPhysicalPrintComplete ✅
```

### onPhysicalPrintStart ✅

简单初始化回调，无问题。

### onDeviceDisconnected

```
onDeviceDisconnected()
├─ cancelled=true
├─ unregister+null currentDataProgressListener
├─ currentTask → INTERRUPTED
├─ isPrinting=false
└─ cancelSendMultiRowDataPacket
```

### 发现

**🟡 发现 5A**：`onDeviceDisconnected` 不调用 `phaseCallback.onPhaseError`。polling 循环在 `isPrinting=false` 之后立即 break（不读 DB），导致 UI 可能停留在断开前的旧状态（如"发送数据中..."），不会显示"中断"错误提示。用户需去任务详情页才能看到任务状态。

### 结论

✅ 物理打印回调逻辑正确。设备断开的 UI 更新有一个小延迟/遗漏，但不影响功能。

---

## 【6/8】PrintProgressActivity 生命周期 & UI 状态一致性

### 按钮状态控制（两个来源）

| 来源 | 触发时机 | 控制内容 |
|------|---------|---------|
| `updateRestartButton()` | `onPhaseChanged(PREPARE/TRANSFER/STOPPED/PRINT)` | btnRestart 文案+可点 |
| polling 循环 | DB 读到 COMPLETED/PAUSED/INTERRUPTED/CANCELLED | btnRestart/btnCancel/btnViewDetail |

### 双重控制冲突检查 ✅

```
cancelTransfer → STOPPED 阶段：
  ① isPrinting=false
  ② polling 循环下轮检测 isPrinting=false → break（不读 DB）
  ③ polling 的 PAUSED 处理不会触发 → 不与 STOPPED 冲突
```

正常完成流程同理。**两套控制不会同时触发，无冲突。**

### onDestroy 安全性 ✅

```
onDestroy()
├─ phaseCallback = null          ← 所有后续 SDK 回调的 null guard 生效
├─ isPrinting? → pause()
│   └─ cancelled=true → onDataProgressError 被拦截
└─ INTERRUPTED → DB 更新
```

### setPhaseActive 一致性 ✅

3 个进度条颜色在各阶段正确切换。

### 结论

✅ 生命周期管理正确，UI 状态与 phase/DB 状态一致。无 bug。

---

## 【7/8】DB 数据一致性 — task status & printedPages

### 写入点汇总

| 位置 | 字段 | 线程 |
|------|------|------|
| `execute()` | status=IN_PROGRESS | dbExecutor |
| `cancelTransfer()` / `pause()` / `switchToNewTarget()` | status=PAUSED | dbExecutor（queue） |
| `onPhysicalPrintComplete()` | printedPages, status=COMPLETED | dbExecutor（queue） |
| `onDataProgressError()` | status=INTERRUPTED | dbExecutor（queue） |
| `onDeviceDisconnected()` | status=INTERRUPTED | dbExecutor（queue） |
| `buildAndSendMultiRow` error | status=INTERRUPTED | dbExecutor（queue） |
| `btnCancel` handler | status=CANCELLED | dbExecutor（queue） |
| `onDestroy` | status=INTERRUPTED | dbExecutor（queue） |
| `fallbackResend()` | printedPages, status=IN_PROGRESS | dbExecutor（queue） |

### 内存 vs DB 一致性

所有 DB 写都通过 `dbExecutor` 串行化，不存在并发写 DB 导致数据损坏的问题。✅

但内存对象 `currentTask` 的状态修改和 DB 落盘之间存在时间窗口。在此期间如果 App 崩溃，DB 可能落后于内存状态。这是 Room 的固有特性，可接受。

### printedPages 跟踪

- **写入**：仅 `PrintProgressManager.onPageComplete()` 和 `fallbackResend()`
- **读取**：`getRemainingPages()` 使用 `IntegerListConverter.fromString(task.getPrintedPages())` 从内存对象读取
- `onPageComplete` 先改内存 `printedPages`，再 queue DB 更新。内存和 DB 最终一致。✅

### 结论

✅ DB 写操作全部串行化，无并发写入冲突。printedPages 跟踪逻辑正确。

---

## 【8/8】边界场景

### 场景 A：快速连点"停止发送"

```
Tap1 → cancelTransfer() → STOPPED → 5s 按钮禁用
Tap2 → 按钮已禁用 → 无响应 ✅
```

### 场景 B：快速连点"重新发送"（5s 后）

```
Tap1 → restartPrint() → isPrinting=true → PREPARE → 按钮禁
Tap2 → PREPARE 阶段按钮禁用 → 无响应 ✅
```

### 场景 C：Activity 重建（屏幕旋转）

当前 manifest 未声明 `configChanges`，旋转会重建 Activity。新 Activity 的 `startOrResumePrint` 通过 `existingTaskId > 0` 分支进入"查看进度"模式。但如果此时 Engine 正在 `buildAndSendMultiRow` 中：
- 新 Activity 设置新的 `phaseCallback` → Engine 继续通过新 callback 更新 UI ✅
- 旧 Activity 的 `onDestroy` 不设置 INTERRUPTED（因为 `isPrinting` 仍为 true...）

**⚠️ 潜在问题**：旧 Activity 的 `onDestroy` 会执行：
```java
if (currentTask != null && PrintEngine.getInstance().isPrinting()) {
    PrintEngine.getInstance().pause();
    currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
}
```

如果用户在 PREPARE/TRANSFER 阶段旋转屏幕，`isPrinting` = true，`pause()` 会被调用！这会导致正在进行的打印被意外中断！

**🔴 Bug 8A**：Activity 重建（如屏幕旋转）会触发 `onDestroy` → `pause()` → 中断正在进行的打印任务。当前 manifest 是否有 `configChanges="orientation|screenSize"` 需确认。

### 场景 D：打印机在发送中突然断开

已分析 → onDeviceDisconnected → INTERRUPTED + isPrinting=false。✅

### 场景 E：App 被系统杀死后恢复

`TaskRecoveryManager.recoverOnStartup()` 将 IN_PROGRESS 任务标记为 INTERRUPTED。`startProgressPolling` 中 `isPrinting` 判断保证不会死锁。✅

---

## 问题汇总

| ID | 级别 | 位置 | 描述 | 状态 |
|----|------|------|------|------|
| **2A** | 🔴 | `sendToPrinter()` | 缺少 `cancelled` 检查 | ✅ 已修复 |
| **8A** | 🔴 | `onDestroy()` | 屏幕旋转触发 pause() 中断打印 | ✅ 已修复 |
| 4A | 🟡 | `switchToNewTarget()` | cancelSend 顺序不一致 | 低优 |
| 4B | 🟡 | `pause()` 调用方 | DB 双写 | 低优 |
| 5A | 🟡 | `onDeviceDisconnected()` | 未通知 UI 错误状态 | 低优 |
