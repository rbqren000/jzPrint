package com.org.jzprinter.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
    @NonNull
    private String targetName;
    @ColumnInfo(name = "targetMode")
    private int editionType;
    @NonNull
    private String materialPath;
    private int totalPages;
    private int printMode;
    @NonNull
    private String targetPages;
    @NonNull
    private String printedPages;
    private int status;
    private long createdAt;
    private long updatedAt;
    private long completedAt;
    private String lastError;
    private String businessId;

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    @NonNull public String getSchoolId() { return schoolId; }
    public void setSchoolId(@NonNull String schoolId) { this.schoolId = schoolId; }

    @NonNull public String getEditionId() { return editionId; }
    public void setEditionId(@NonNull String editionId) { this.editionId = editionId; }

    @NonNull public String getTargetId() { return targetId; }
    public void setTargetId(@NonNull String targetId) { this.targetId = targetId; }

    @NonNull public String getTargetName() { return targetName; }
    public void setTargetName(@NonNull String targetName) { this.targetName = targetName; }

    public int getEditionType() { return editionType; }
    public void setEditionType(int editionType) { this.editionType = editionType; }

    @NonNull public String getMaterialPath() { return materialPath; }
    public void setMaterialPath(@NonNull String materialPath) { this.materialPath = materialPath; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPrintMode() { return printMode; }
    public void setPrintMode(int printMode) { this.printMode = printMode; }

    @NonNull public String getTargetPages() { return targetPages; }
    public void setTargetPages(@NonNull String targetPages) { this.targetPages = targetPages; }

    @NonNull public String getPrintedPages() { return printedPages; }
    public void setPrintedPages(@NonNull String printedPages) { this.printedPages = printedPages; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
}
