package com.org.jzprinter.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.org.jzprinter.utils.SignUtil;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class AuthManager {

    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "token";

    private static volatile AuthManager INSTANCE;

    public static AuthManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AuthManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private final Context appContext;
    private final OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AuthManager(Context appContext) {
        this.appContext = appContext;
        this.httpClient = new OkHttpClient();
    }

    public String getToken() {
        return getPrefs().getString(KEY_TOKEN, null);
    }

    public boolean isTokenValid() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void login(LoginCallback callback) {
        long time = System.currentTimeMillis();
        String sign = SignUtil.generateSign(
            AuthConfig.CREDIT_CODE, time,
            AuthConfig.ACCESS_KEY, AuthConfig.ACCESS_SECRET,
            AuthConfig.REGIONALISM_CODE, AuthConfig.FUNCTION_CODE);

        try {
            JSONObject body = new JSONObject();
            body.put("creditCode", AuthConfig.CREDIT_CODE);
            body.put("time", time);
            body.put("functionCode", AuthConfig.FUNCTION_CODE);
            body.put("sign", sign);

            String url = AuthConfig.BASE_URL + ApiConfig.PATH_LOGIN;
            Log.d(TAG, "login url=" + url);
            Log.d(TAG, "login body=" + body);

            RequestBody requestBody = RequestBody.create(
                body.toString(), MediaType.get("application/json"));

            Request request = new Request.Builder()
                .url(AuthConfig.BASE_URL + ApiConfig.PATH_LOGIN)
                .post(requestBody)
                .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "login onFailure: " + e.getMessage());
                    mainHandler.post(() -> callback.onError("网络错误: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String respStr = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "login response: code=" + response.code() + " body=" + respStr);
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                "登录失败: HTTP " + response.code() + " " + respStr));
                            return;
                        }
                        JSONObject respJson = new JSONObject(respStr);
                        String code = respJson.optString("code", "");
                        if (!"'200'".equals(code) && !"200".equals(code)) {
                            String msg = respJson.optString("message", "未知错误");
                            mainHandler.post(() -> callback.onError("登录失败: " + msg));
                            return;
                        }
                        JSONObject data = respJson.getJSONObject("data");
                        String token = data.getString("token");
                        saveToken(token);
                        mainHandler.post(() -> callback.onSuccess());
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("解析错误: " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("构建请求失败: " + e.getMessage());
        }
    }

    private void saveToken(String token) {
        getPrefs().edit().putString(KEY_TOKEN, token).apply();
    }

    private SharedPreferences getPrefs() {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(String error);
    }
}
