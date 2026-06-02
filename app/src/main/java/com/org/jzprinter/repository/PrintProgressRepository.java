package com.org.jzprinter.repository;

import com.org.jzprinter.database.dao.PrintProgressDao;
import com.org.jzprinter.database.entity.PrintProgressEntity;

public class PrintProgressRepository {
    private final PrintProgressDao progressDao;

    public PrintProgressRepository(PrintProgressDao progressDao) {
        this.progressDao = progressDao;
    }

    public void insert(PrintProgressEntity progress) {
        progressDao.insert(progress);
    }
}
