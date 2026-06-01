package com.org.jzprinter.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    public int getPageIndex() { return pageIndex; }
    public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }

    public int getPuzzleIndex() { return puzzleIndex; }
    public void setPuzzleIndex(int puzzleIndex) { this.puzzleIndex = puzzleIndex; }

    public int getTotalPuzzles() { return totalPuzzles; }
    public void setTotalPuzzles(int totalPuzzles) { this.totalPuzzles = totalPuzzles; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCartridgeId() { return cartridgeId; }
    public void setCartridgeId(String cartridgeId) { this.cartridgeId = cartridgeId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
