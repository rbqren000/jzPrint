package com.org.jzprinter.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.org.jzprinter.network.model.Edition;
import com.org.jzprinter.network.model.RosterGroup;
import com.org.jzprinter.network.model.Semester;
import com.org.jzprinter.network.model.Student;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient implements Api {

    private static final String TAG = "ApiClient";
    private final Context context;
    private final OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void fetchSemesters(String schoolId, SemesterCallback callback) {
        String url = ApiConfig.BASE_URL + ApiConfig.PATH_EDITION_LIST
            + "?schoolId=" + schoolId;
        Log.d(TAG, "fetchSemesters url=" + url);

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", getAuthorization())
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchSemesters onFailure: " + e.getMessage());
                callback.onError("网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "fetchSemesters HTTP=" + response.code() + " body=" + body.substring(0, Math.min(body.length(), 300)));
                    if (!response.isSuccessful()) {
                        callback.onError("请求失败: HTTP " + response.code());
                        return;
                    }
                    JSONObject main = new JSONObject(body);
                    JSONArray arr = main.optJSONArray("data");
                    List<Semester> result = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject semObj = arr.getJSONObject(i);
                            Semester semester = new Semester();
                            semester.semesterId = String.valueOf(semObj.optInt("semesterId", 0));
                            semester.semesterName = semObj.optString("semesterName", "");
                            semester.editionList = new ArrayList<>();

                            JSONArray editionsArr = semObj.optJSONArray("editionList");
                            if (editionsArr != null) {
                                for (int j = 0; j < editionsArr.length(); j++) {
                                    JSONObject edObj = editionsArr.getJSONObject(j);
                                    Edition edition = new Edition();
                                    edition.editionId = String.valueOf(edObj.optInt("editionId", 0));
                                    edition.editionName = edObj.optString("editionName", "");
                                    edition.editionType = edObj.optString("editionType", "1");
                                    semester.editionList.add(edition);
                                }
                            }
                            result.add(semester);
                        }
                    }
                    Log.d(TAG, "fetchSemesters success: " + result.size() + " semesters");
                    callback.onSuccess(result);
                } catch (Exception ex) {
                    Log.e(TAG, "fetchSemesters parse error: " + ex.getMessage());
                    callback.onError("解析失败: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void fetchStudents(String schoolId, String editionId, int editionType,
                              StudentCallback callback) {
        String url = ApiConfig.BASE_URL + ApiConfig.PATH_ROSTER_LIST
            + "?schoolId=" + schoolId
            + "&editionId=" + editionId
            + "&editionType=" + editionType;
        Log.d(TAG, "fetchStudents url=" + url);

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", getAuthorization())
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchStudents onFailure: " + e.getMessage());
                callback.onError("网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "fetchStudents HTTP=" + response.code() + " body=" + body.substring(0, Math.min(body.length(), 300)));
                    if (!response.isSuccessful()) {
                        callback.onError("请求失败: HTTP " + response.code());
                        return;
                    }
                    JSONObject main = new JSONObject(body);
                    JSONArray arr = main.optJSONArray("data");
                    List<RosterGroup> result = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject classObj = arr.getJSONObject(i);
                            RosterGroup group = new RosterGroup();
                            group.classId = classObj.optString("classId", "");
                            group.className = classObj.optString("className", "");
                            group.businessId = classObj.optString("businessId", "");
                            group.prepareCode = classObj.optString("prepareCode", "");
                            group.areaId = classObj.optInt("areaId", 0);
                            group.areaName = classObj.optString("areaName", "");
                            group.studentList = new ArrayList<>();

                            JSONArray studentsArr = classObj.optJSONArray("studentList");
                            if (studentsArr != null) {
                                for (int j = 0; j < studentsArr.length(); j++) {
                                    JSONObject sObj = studentsArr.getJSONObject(j);
                                    Student student = new Student();
                                    student.studentId = sObj.optString("studentId", "");
                                    student.studentName = sObj.optString("studentName", "");
                                    student.businessId = sObj.optString("businessId", "");
                                    group.studentList.add(student);
                                }
                            }
                            result.add(group);
                        }
                    }
                    Log.d(TAG, "fetchStudents success: " + result.size() + " groups");
                    callback.onSuccess(result);
                } catch (Exception ex) {
                    Log.e(TAG, "fetchStudents parse error: " + ex.getMessage());
                    callback.onError("解析失败: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void downloadMaterial(String schoolId, String businessId, int editionType,
                                 String savePath, DownloadCallback callback) {
        String url = ApiConfig.BASE_URL + ApiConfig.PATH_EDITION_ZIP
            + "?schoolId=" + schoolId
            + "&businessId=" + businessId
            + "&editionType=" + editionType;
        Log.d(TAG, "downloadMaterial url=" + url);

        File outFile = new File(savePath);
        outFile.getParentFile().mkdirs();

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", getAuthorization())
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "downloadMaterial onFailure: " + e.getMessage());
                callback.onError("下载失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String errorMsg = parseErrorMsg(errorBody);
                    Log.e(TAG, "downloadMaterial HTTP " + response.code()
                        + " body=" + errorBody);
                    callback.onError("下载失败: " + errorMsg);
                    return;
                }
                if (response.body() == null) {
                    callback.onError("下载失败: 响应为空");
                    return;
                }
                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(outFile)) {
                    long totalSize = response.body().contentLength();
                    byte[] buffer = new byte[8192];
                    long totalWritten = 0;
                    int lastPercent = -1;
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        totalWritten += len;
                        if (totalSize > 0) {
                            int percent = (int) (totalWritten * 100 / totalSize);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                int finalPercent = percent;
                                mainHandler.post(() -> callback.onProgress(finalPercent));
                            }
                        }
                    }
                } catch (IOException e) {
                    callback.onError("写入文件失败: " + e.getMessage());
                    return;
                }
                mainHandler.post(() -> callback.onSuccess(outFile.getAbsolutePath()));
            }
        });
    }

    private String parseErrorMsg(String errorBody) {
        try {
            JSONObject json = new JSONObject(errorBody);
            String msg = json.optString("message", "");
            if (!msg.isEmpty()) return msg;
        } catch (Exception ignored) {}
        return "HTTP " + errorBody;
    }

    private String getAuthorization() {
        String token = AuthManager.getInstance(context).getToken();
        Log.d(TAG, "getAuthorization: token=" + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "NULL"));
        return token != null ? token : "";
    }
}
