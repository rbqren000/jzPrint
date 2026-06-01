package com.org.jzprinter.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
    @ColumnInfo(name = "targetMode")
    private int editionType;
    private String remoteUrl;
    private String remoteMd5;
    private String localZipPath;
    private String localExtractPath;
    private long fileSize;
    private long downloadedAt;
    private long extractedAt;
    private int status;
    private String lastError;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull public String getSchoolId() { return schoolId; }
    public void setSchoolId(@NonNull String schoolId) { this.schoolId = schoolId; }

    @NonNull public String getEditionId() { return editionId; }
    public void setEditionId(@NonNull String editionId) { this.editionId = editionId; }

    @NonNull public String getTargetId() { return targetId; }
    public void setTargetId(@NonNull String targetId) { this.targetId = targetId; }

    public int getEditionType() { return editionType; }
    public void setEditionType(int editionType) { this.editionType = editionType; }

    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    public String getRemoteMd5() { return remoteMd5; }
    public void setRemoteMd5(String remoteMd5) { this.remoteMd5 = remoteMd5; }

    public String getLocalZipPath() { return localZipPath; }
    public void setLocalZipPath(String localZipPath) { this.localZipPath = localZipPath; }

    public String getLocalExtractPath() { return localExtractPath; }
    public void setLocalExtractPath(String localExtractPath) { this.localExtractPath = localExtractPath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(long downloadedAt) { this.downloadedAt = downloadedAt; }

    public long getExtractedAt() { return extractedAt; }
    public void setExtractedAt(long extractedAt) { this.extractedAt = extractedAt; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
