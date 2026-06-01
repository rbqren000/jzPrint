package com.org.jzprinter.database.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.org.jzprinter.database.entity.PrintTaskEntity;

import java.util.List;

@Dao
public interface PrintTaskDao {
    @Insert
    long insert(PrintTaskEntity task);

    @Update
    void update(PrintTaskEntity task);

    @Query("SELECT * FROM print_task WHERE taskId = :taskId")
    PrintTaskEntity getById(long taskId);

    @Query("SELECT * FROM print_task WHERE schoolId = :schoolId AND targetId = :targetId AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinished(@NonNull String schoolId, @NonNull String targetId);

    @Query("SELECT * FROM print_task WHERE schoolId = :schoolId AND targetId = :targetId AND editionId = :editionId AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinishedByEdition(@NonNull String schoolId, @NonNull String targetId, @NonNull String editionId);

    @Query("SELECT * FROM print_task WHERE targetId = :targetId AND editionId = :editionId AND printMode = :printMode AND status IN (0, 4, 5) ORDER BY createdAt DESC LIMIT 1")
    PrintTaskEntity findUnfinishedByEditionAndMode(@NonNull String targetId, @NonNull String editionId, int printMode);

    @Query("SELECT * FROM print_task WHERE status IN (0, 4, 5) ORDER BY updatedAt DESC")
    List<PrintTaskEntity> findAllResumable();

    @Query("SELECT * FROM print_task WHERE targetId = :targetId AND status IN (0, 4, 5) ORDER BY updatedAt DESC")
    List<PrintTaskEntity> findResumableByTargetId(@NonNull String targetId);

    @Query("SELECT * FROM print_task WHERE status = :status ORDER BY createdAt DESC")
    List<PrintTaskEntity> findByStatus(int status);

    @Query("SELECT * FROM print_task ORDER BY createdAt DESC")
    List<PrintTaskEntity> getAll();

    @Query("SELECT * FROM print_task ORDER BY updatedAt DESC LIMIT :limit")
    List<PrintTaskEntity> findRecent(int limit);

    @Query("SELECT * FROM print_task WHERE targetId = :targetId ORDER BY updatedAt DESC")
    List<PrintTaskEntity> findByTargetId(@NonNull String targetId);

    @Query("DELETE FROM print_task WHERE status = 2 AND completedAt < :beforeTime")
    int deleteCompletedBefore(long beforeTime);
}
