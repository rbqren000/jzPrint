package com.org.jzprinter.database.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.org.jzprinter.database.entity.StudentEntity;

import java.util.List;

@Dao
public interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StudentEntity student);

    @Update
    void update(StudentEntity student);

    @Query("SELECT * FROM student WHERE schoolId = :schoolId AND editionId = :editionId")
    List<StudentEntity> getByEdition(@NonNull String schoolId, @NonNull String editionId);

    @Query("SELECT * FROM student WHERE studentId = :studentId AND schoolId = :schoolId AND editionId = :editionId")
    StudentEntity getById(@NonNull String studentId, @NonNull String schoolId, @NonNull String editionId);

    @Query("SELECT * FROM student WHERE schoolId = :schoolId AND editionId = :editionId AND materialReady = 1")
    List<StudentEntity> getMaterialReady(@NonNull String schoolId, @NonNull String editionId);
}
