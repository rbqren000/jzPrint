package com.org.jzprinter.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import com.org.jzprinter.database.entity.PrintProgressEntity;

@Dao
public interface PrintProgressDao {
    @Insert
    void insert(PrintProgressEntity progress);
}
