package com.org.jzprinter.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.org.jzprinter.database.entity.PrintProgressEntity;

import java.util.List;

@Dao
public interface PrintProgressDao {
    @Insert
    void insert(PrintProgressEntity progress);

    @Query("SELECT * FROM print_progress WHERE taskId = :taskId ORDER BY timestamp ASC")
    List<PrintProgressEntity> getByTaskId(long taskId);

    @Query("SELECT * FROM print_progress WHERE taskId = :taskId AND status = :completedStatus ORDER BY timestamp DESC")
    List<PrintProgressEntity> getCompletedByTaskId(long taskId, int completedStatus);

    @Query("DELETE FROM print_progress WHERE taskId = :taskId")
    void deleteByTaskId(long taskId);
}
