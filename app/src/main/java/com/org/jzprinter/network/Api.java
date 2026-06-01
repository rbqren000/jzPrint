package com.org.jzprinter.network;

import com.org.jzprinter.network.model.Edition;
import com.org.jzprinter.network.model.RosterGroup;
import com.org.jzprinter.network.model.Semester;

import java.util.List;

public interface Api {

    interface SemesterCallback {
        void onSuccess(List<Semester> semesters);
        void onError(String error);
    }

    interface StudentCallback {
        void onSuccess(List<RosterGroup> students);
        void onError(String error);
    }

    interface DownloadCallback {
        void onSuccess(String localPath);
        void onProgress(int percentage);
        void onError(String error);
    }

    void fetchSemesters(String schoolId, SemesterCallback callback);

    void fetchStudents(String schoolId, String editionId, int editionType,
                       StudentCallback callback);

    void downloadMaterial(String schoolId, String businessId, int editionType,
                          String savePath, DownloadCallback callback);
}
