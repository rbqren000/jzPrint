package com.org.jzprinter.repository;

import com.org.jzprinter.database.dao.StudentDao;
import com.org.jzprinter.database.entity.StudentEntity;

import java.util.List;

public class StudentRepository {
    private final StudentDao studentDao;

    public StudentRepository(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    public void insert(StudentEntity student) {
        studentDao.insert(student);
    }

    public void update(StudentEntity student) {
        studentDao.update(student);
    }

    public List<StudentEntity> getByEdition(String schoolId, String editionId) {
        return studentDao.getByEdition(schoolId, editionId);
    }

    public StudentEntity getById(String studentId, String schoolId, String editionId) {
        return studentDao.getById(studentId, schoolId, editionId);
    }

    public List<StudentEntity> getMaterialReady(String schoolId, String editionId) {
        return studentDao.getMaterialReady(schoolId, editionId);
    }
}
