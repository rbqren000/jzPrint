package com.org.jzprinter.database.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.org.jzprinter.database.entity.MaterialEntity;

import java.util.List;

@Dao
public interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(MaterialEntity material);

    @Update
    void update(MaterialEntity material);

    @Query("SELECT * FROM material WHERE schoolId = :schoolId AND editionId = :editionId AND targetId = :targetId AND targetMode = :editionType")
    MaterialEntity find(@NonNull String schoolId, @NonNull String editionId,
                        @NonNull String targetId, int editionType);

    @Query("SELECT * FROM material WHERE status = :status")
    List<MaterialEntity> findByStatus(int status);

    @Query("SELECT SUM(fileSize) FROM material")
    long getTotalSize();

    default void upsert(MaterialEntity material) {
        MaterialEntity existing = find(material.getSchoolId(), material.getEditionId(),
                                        material.getTargetId(), material.getEditionType());
        if (existing != null) {
            material.setId(existing.getId());
            update(material);
        } else {
            insert(material);
        }
    }
}
