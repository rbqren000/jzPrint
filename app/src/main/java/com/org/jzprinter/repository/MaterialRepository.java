package com.org.jzprinter.repository;

import com.org.jzprinter.database.dao.MaterialDao;
import com.org.jzprinter.database.entity.MaterialEntity;

import java.util.List;

public class MaterialRepository {
    private final MaterialDao materialDao;

    public MaterialRepository(MaterialDao materialDao) {
        this.materialDao = materialDao;
    }

    public void insert(MaterialEntity material) {
        materialDao.insert(material);
    }

    public void update(MaterialEntity material) {
        materialDao.update(material);
    }

    public MaterialEntity find(String schoolId, String editionId, String targetId, int editionType) {
        return materialDao.find(schoolId, editionId, targetId, editionType);
    }

    public List<MaterialEntity> findByStatus(int status) {
        return materialDao.findByStatus(status);
    }

    public long getTotalSize() {
        return materialDao.getTotalSize();
    }

    public void upsert(MaterialEntity material) {
        materialDao.upsert(material);
    }
}
