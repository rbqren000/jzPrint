# 入口链路审查：Main → PrintProgressActivity

> 审查日期：2026-05-29
> 范围：`startOrResumePrint()` 7 条入口 + 各调用方前置逻辑

---

## 入口全景

| # | 调用方 | Intent | 参数 | `startOrResumePrint` 分支 | Engine 操作 |
|---|-------|--------|------|--------------------------|-------------|
| 1 | `PrintModeSelectActivity.startPrint` | `newIntent(taskId=-1)` | schoolId, editionId... | **新建任务** | `startNewTask` → `execute` |
| 2 | `TaskDetailActivity.onViewProgress` | `newIntent(taskId=real)` | taskId=real | **查看进度** | 仅轮询 |
| 3 | `MainActivity.onContinue` | `newResumeIntent(taskId)` | taskId | **断点续打** | `resumeFromBreakpoint` → `execute` |
| 4 | `TaskDetailActivity.onContinuePrint` | `newResumeIntent(taskId)` | taskId | **断点续打** | `resumeFromBreakpoint` → `execute` |
| 5 | `TaskDetailActivity.onReprintAll` | `newReprintIntent(taskId)` | taskId | **全部重打** | `reprintAll` → `fallbackResend` |
| 6 | `TaskDetailActivity.onReprintSelected` | `newReprintPagesIntent(taskId, pages)` | taskId + pages | **选择性重打** | `reprintPages` → `fallbackResend` |
| 7 | `MainActivity.startupResumeDialog` | 经 `TaskDetailActivity` → `newResumeIntent` | taskId | **断续打（间接）** | 同 #4 |

---

## 逐路分析

### 路径 1：PrintModeSelectActivity → newIntent(taskId=-1)

```java
// PrintModeSelectActivity L257-278
engine.setUseCustomMerge(...);          // ① 设置 Engine 配置
engine.setRotationDirection(...);
engine.setVerticalAlignment(...);
engine.setCustomTargetPages(selectedPages);  // ② 设置自选页
Intent intent = PrintProgressActivity.newIntent(..., -1L);  // taskId=-1
startActivity(intent);
finish();
```

**到达 PrintProgressActivity 后**：
```
startOrResumePrint:
  isReprint=false, isResume=false, existingTaskId=-1
  → 穿过 3 个 if → NEW TASK 分支 (L359)
  → dbExecutor.execute:
      task = engine.startNewTask(...)  // 读取 customTargetPages
      engine.execute(task)
```

**⚠️ 发现 1A**：`customTargetPages` 是普通字段，非 volatile。写入在主线程（②），读取在 dbExecutor 线程。JMM 不保证跨线程非 volatile 写对另一个线程可见。实践中由于 `startActivity` 隐含同步和 `ExecutorService.execute` 内部同步，当前能正确执行。但代码层面不够稳健。

---

### 路径 2：TaskDetailActivity.onViewProgress

```java
// L235-241
startActivity(PrintProgressActivity.newIntent(..., taskId=real));
```

- 任务状态是 `IN_PROGRESS` 时才能看到此按钮
- `startOrResumePrint` → VIEW PROGRESS 分支
- 进度条设为"已完成"，启动 polling 只读不执行

✅ 无问题。

---

### 路径 3：MainActivity.onContinue

```java
// L130-132
PrintEngine.getInstance().switchToNewTarget();  // ① 暂停当前任务
startActivity(PrintProgressActivity.newResumeIntent(taskId));
```

**时序**：
1. `switchToNewTarget()` → isPrinting=false, cancelled=true, PAUSED
2. `startActivity` → new PrintProgressActivity created
3. `startOrResumePrint` → RESUME 分支 → `resumeFromBreakpoint(task)`
4. `execute` → `isPrinting=true`, `cancelled=false`

`switchToNewTarget` 先暂停旧任务，再启动新任务。✅

**检查**：如果当前没有打印任务（isPrinting=false），`switchToNewTarget` 的守卫使它空转。不影响。✅

**检查**：第 117-118 行有 `checkPrinterConnection()` 和 `checkMaterialExists(task)` 前置校验。✅

---

### 路径 4：TaskDetailActivity.onContinuePrint

```java
// L155-187
// PENDING: 弹窗 → newResumeIntent
// PAUSED/INTERRUPTED: 弹窗 → newResumeIntent
```

**不同于路径 3**：不调用 `switchToNewTarget()`。原因是在 TaskDetailActivity 场景下不应该有正在执行的其他任务（用户从任务详情页进入，当前只有一个任务）。✅

**PENDING 路径**：`resumeFromBreakpoint` → `execute` → `getRemainingPages` → 全部页面（printedPages 为空）。正确模拟首次打印。✅

---

### 路径 5/6：全部重打 / 选择性重打

```java
// L206-209, L227-229
PrintEngine.getInstance().switchToNewTarget();  // 先暂停
startActivity(PrintProgressActivity.newReprintIntent/PagesIntent(...));
```

→ `startOrResumePrint` → REPRINT 分支 → `reprintPages/reprintAll` → `fallbackResend`

`fallbackResend` 从 printedPages 中移除要重打的页，然后 `buildAndSendMultiRow`。✅

**检查**：`switchToNewTarget()` → cancelled=true → then `fallbackResend` → `cancelled=false` in `execute()`. 但因为 `fallbackResend` 不走 `execute()` 而是直接调用 `buildAndSendMultiRow`... 等等！

```java
// fallbackResend L541-563
cancelled.set(false);  // ← 在 fallbackResend 中重置！
buildAndSendMultiRow(...);
```

cancelled 被正确地重置为 false。✅

**检查**：`reprintPages` 走 fallbackResend 时设置 `isPrinting.set(true)`，如果有另一个打印在运行，不会冲突（因为 switchToNewTarget 先暂停了）。✅

---

### 路径 7：MainActivity 启动弹窗

```java
// L216-219
PrintTaskEntity latest = resumable.get(0);  // 取最新一条
PrintEngine.getInstance().switchToNewTarget();
startActivity(TaskDetailActivity.newIntent(this, latest.getTaskId()));
```

先到 TaskDetailActivity，用户再点"继续打印" → 进入路径 4。✅

---

## 参数传递检查

| 路径 | schoolId | editionId | targetId | targetMode | printMode | pagesPath | taskId |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 newIntent(taskId=-1) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | -1 |
| 2 newIntent(taskId=real) | ✅来自DB | ✅来自DB | ✅来自DB | ✅来自DB | ✅来自DB | ✅来自DB | real |
| 3 newResumeIntent | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | real |
| 4 newResumeIntent | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | real |
| 5 newReprintIntent | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | real |
| 6 newReprintPagesIntent | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | real |
| 7 间接 | — | — | — | — | — | — | real |

路径 3-7 不传 schoolId/editionId/pagesPath 等字段，但在 `startOrResumePrint` 中对应的 RESUME/REPRINT 分支不需要这些字段——任务已存在于 DB，从 DB 加载。

✅ 参数传递逻辑正确。

---

## startOrResumePrint 分发正确性

```java
if (isReprint && existingTaskId > 0) { ... return; }   // ① reprint
if (isResume && existingTaskId > 0)  { ... return; }   // ② resume
if (existingTaskId > 0)              { ... return; }   // ③ view progress
/* new task */                       { ... }           // ④ new
```

**路径映射**：

| 入口 | isReprint | isResume | taskId | 命中分支 |
|------|:---:|:---:|:---:|:---:|
| PrintModeSelectActivity | false | false | -1 | 跳过 ①②③ → ④ ✅ |
| TaskDetailActivity.onViewProgress | false | false | >0 | 命中 ③ ✅ |
| MainActivity.onContinue + TaskDetail.onContinuePrint | false | true | >0 | 命中 ② ✅ |
| TaskDetail.onReprintAll/Selected | true | false | >0 | 命中 ① ✅ |

**分支互斥性**：isReprint 和 isResume 不可能同时为 true（不同 Intent 方法）。分支无重叠。✅

---

## 结论

| ID | 级别 | 描述 |
|----|------|------|
| — | ✅ | 7 条入口分发全部正确 |
| — | ✅ | `switchToNewTarget()` 前置暂停逻辑正确 |
| — | ✅ | `fallbackResend` 中 cancelled/isPrinting 重置正确 |
| — | ✅ | `customTargetPages` 跨线程读写安全（ExecutorService 提供 happens-before） |

**无 bug。**
