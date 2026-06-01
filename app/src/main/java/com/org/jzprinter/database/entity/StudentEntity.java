package com.org.jzprinter.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

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
    private String businessId;
    private long cachedAt;
    private String materialPath;
    private boolean materialReady;

    @NonNull public String getStudentId() { return studentId; }
    public void setStudentId(@NonNull String studentId) { this.studentId = studentId; }

    @NonNull public String getSchoolId() { return schoolId; }
    public void setSchoolId(@NonNull String schoolId) { this.schoolId = schoolId; }

    @NonNull public String getEditionId() { return editionId; }
    public void setEditionId(@NonNull String editionId) { this.editionId = editionId; }

    @NonNull public String getStudentName() { return studentName; }
    public void setStudentName(@NonNull String studentName) { this.studentName = studentName; }

    @NonNull public String getClassId() { return classId; }
    public void setClassId(@NonNull String classId) { this.classId = classId; }

    @NonNull public String getClassName() { return className; }
    public void setClassName(@NonNull String className) { this.className = className; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public long getCachedAt() { return cachedAt; }
    public void setCachedAt(long cachedAt) { this.cachedAt = cachedAt; }

    public String getMaterialPath() { return materialPath; }
    public void setMaterialPath(String materialPath) { this.materialPath = materialPath; }

    public boolean isMaterialReady() { return materialReady; }
    public void setMaterialReady(boolean materialReady) { this.materialReady = materialReady; }
}
