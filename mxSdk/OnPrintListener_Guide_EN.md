Created by RBQ on 2026-02-04

# OnPrintListener Event Usage Guide

## Overview

`OnPrintListener` is the core interface in MX SDK for monitoring printer printing status. When the printer starts or completes printing, the SDK notifies the application via this interface, allowing developers to obtain real-time printing progress, current print index, and cartridge information.

This interface is primarily used in multi-join printing scenarios (e.g., printing multiple copies of the same or different content continuously). The three parameters `beginIndex`, `endIndex`, and `currentIndex` clearly identify the start and end range of a print job and its current progress.

## Interface Definition

`OnPrintListener` is an inner interface of `ConnectManager`, defined as follows:

```java
public interface OnPrintListener {
    /**
     * Printing start callback
     *
     * @param beginIndex   The starting index of this print job (zero-based)
     * @param endIndex     The ending index of this print job
     * @param currentIndex The index currently being printed
     */
    void onPrintStart(int beginIndex, int endIndex, int currentIndex);

    /**
     * Printing completion callback
     *
     * @param beginIndex   The starting index of this print job (zero-based)
     * @param endIndex     The ending index of this print job
     * @param currentIndex The index that just completed printing
     * @param cartridgeId  Cartridge ID (may be an empty string)
     */
    void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId);
}
```

## Registration and Unregistration

### Obtaining ConnectManager Instance

`ConnectManager` uses the singleton pattern. You can obtain the instance via the `share()` method. Choose one of the following two approaches:

**Method 1: Directly use the singleton (recommended)**
```java
// Directly use ConnectManager.share() for chained calls
ConnectManager.share().registerPrintListener(listener);
```

**Method 2: Store a local reference**
```java
// If you need to use it multiple times, store it as a local variable
ConnectManager connectManager = ConnectManager.share();
connectManager.registerPrintListener(listener);
```

### Registering the Listener

Call the `registerPrintListener()` method to register an `OnPrintListener`:

```java
ConnectManager.share().registerPrintListener(new ConnectManager.OnPrintListener() {
    @Override
    public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
        // Handle print start event
    }

    @Override
    public void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId) {
        // Handle print completion event
    }
});
```

### Unregistering the Listener

When you no longer need to receive print events, call `unregisterPrintListener()` to unregister the listener:

```java
// You must pass the same listener instance as registered
ConnectManager.share().unregisterPrintListener(listener);
```

**Note**: To avoid memory leaks, it is recommended to unregister the listener in the Activity/Fragment's `onDestroy()` or appropriate lifecycle method.

## Event Trigger Timing

### onPrintStart Trigger Timing

Triggered when the printer starts printing **each join** of data. For example:
- 3 joins of data sent (indices 0, 1, 2)
- When starting to print join 0: `onPrintStart(0, 2, 0)`
- When starting to print join 1: `onPrintStart(0, 2, 1)`
- When starting to print join 2: `onPrintStart(0, 2, 2)`

### onPrintComplete Trigger Timing

Triggered when the printer completes printing **each join** of data. For example:
- Join 0 completed: `onPrintComplete(0, 2, 0, "CartridgeID")`
- Join 1 completed: `onPrintComplete(0, 2, 1, "CartridgeID")`
- Join 2 completed: `onPrintComplete(0, 2, 2, "CartridgeID")`

## Parameter Details

### beginIndex (Starting Index)

- **Type**: `int`
- **Description**: The starting index of this print job, counting from zero.
- **Example**: If 3 joins of data are sent, `beginIndex` is always 0.

### endIndex (Ending Index)

- **Type**: `int`
- **Description**: The ending index of this print job.
- **Example**: If 3 joins of data are sent, `endIndex` is 2.
- **Calculation**: `endIndex = total joins - 1`

### currentIndex (Current Index)

- **Type**: `int`
- **Description**: The index currently being printed (or just completed).
- **In onPrintStart**: Indicates the join index about to start printing.
- **In onPrintComplete**: Indicates the join index that just finished printing.

### cartridgeId (Cartridge ID)

- **Type**: `String`
- **Description**: Unique identifier of the cartridge used for the current print.
- **Possible values**:
  - Cartridge identification string returned by the printer
  - Empty string (when the printer does not return a cartridge ID)
- **Note**: This parameter is only provided in `onPrintComplete`; `onPrintStart` does not include it.

## Example Code

Below is a complete usage example:

```java
public class PrintActivity extends AppCompatActivity {
    private ConnectManager.OnPrintListener printListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        
        // Initialize SDK (must call init first)
        ConnectManager.share().init(getApplication());
        
        // Create print listener
        printListener = new ConnectManager.OnPrintListener() {
            @Override
            public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
                String message = String.format("Start printing: join %d/%d", 
                    currentIndex + 1, endIndex + 1);
                Toast.makeText(PrintActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // Update UI to show print progress
                updatePrintProgress(currentIndex, endIndex);
            }

            @Override
            public void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId) {
                String message = String.format("Print completed: join %d/%d, Cartridge: %s", 
                    currentIndex + 1, endIndex + 1, 
                    TextUtils.isEmpty(cartridgeId) ? "Unknown" : cartridgeId);
                Toast.makeText(PrintActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // If all joins are printed
                if (currentIndex == endIndex) {
                    showPrintCompleteDialog();
                }
            }
        };
        
        // Register listener - directly use singleton pattern
        ConnectManager.share().registerPrintListener(printListener);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister listener - directly use singleton pattern
        if (printListener != null) {
            ConnectManager.share().unregisterPrintListener(printListener);
        }
    }
    
    private void updatePrintProgress(int current, int total) {
        // Implement UI progress update logic
    }
    
    private void showPrintCompleteDialog() {
        // Implement print completion prompt
    }
}
```

## Notes

### 1. Thread Safety
- SDK ensures all `OnPrintListener` callbacks are executed on the main thread
- You can safely update UI directly in callbacks without additional thread switching

### 2. Multi‑Join Printing Concept
- **Join**: In SDK, represents a single printing unit within a print job
- For example, sending 3 joins of data (indices 0, 1, 2) means printing 3 copies continuously

### 3. Cartridge ID Handling
- Some printer models may not return a cartridge ID
- It is recommended to check `cartridgeId` for emptiness: `TextUtils.isEmpty(cartridgeId)`

### 4. Lifecycle Management
- Ensure registering and unregistering the listener at appropriate times
- Avoid memory leaks by not holding the listener after Activity is destroyed

### 5. Event Order
- Normal printing flow: `onPrintStart` → (printing process) → `onPrintComplete`
- For each join of data, `onPrintStart` is triggered first, followed by `onPrintComplete`

**Last Updated**: 2026-02-04  
**Applicable Models**: All Inksi/MX series printers that support multi‑join printing