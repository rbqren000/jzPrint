package com.mx.mxSdk.Utils;

import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RBQLog {

    private final static String TAG = "RBQLogger";

    private static final boolean ENABLE = true;
    private static final boolean LOG2FILE_ENABLE = false;
    private static BufferedWriter mWriter;

    public static boolean isLoggable(String tag, int level) {
        return ENABLE && Log.isLoggable(tag, level);
    }

    private static String getStackTraceString(Throwable th) {
        return ENABLE ? Log.getStackTraceString(th) : th.getMessage();
    }

    public static int println(String tag, int level, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.println(level, tag, msg) : 0;
    }

    public static int v(String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.v(TAG, msg) : 0;
    }

    public static int v(String tag, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.v(tag, msg) : 0;
    }

    public static int v(String tag, String msg, Throwable th) {
        if (msg == null || th == null) {
            return 0;
        }
        write2File(msg);
        write2File(getStackTraceString(th));
        return ENABLE ? Log.v(tag, msg, th) : 0;
    }

    public static int d(String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.d(TAG, msg) : 0;
    }

    public static int d(String tag, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.d(tag, msg) : 0;
    }

    public static int d(String tag, String msg, Throwable th) {
        if (msg == null || th == null) {
            return 0;
        }
        write2File(msg);
        write2File(getStackTraceString(th));
        return ENABLE ? Log.d(tag, msg, th) : 0;
    }

    public static int i(String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.i(TAG, msg) : 0;
    }

    public static int i(String tag, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.i(tag, msg) : 0;
    }

    public static int i(String tag1, String tag2, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.i(tag1 + " [ " + tag2 + " ] ", msg) : 0;
    }

    public static int i(String tag, String msg, Throwable th) {
        if (msg == null || th == null) {
            return 0;
        }
        write2File(msg);
        write2File(getStackTraceString(th));
        return ENABLE ? Log.i(tag, msg, th) : 0;
    }

    public static int w(String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.w(TAG, msg) : 0;
    }

    public static int w(String tag, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.w(tag, msg) : 0;
    }

    public static int w(String tag, String msg, Throwable th) {
        if (msg == null || th == null) {
            return 0;
        }
        write2File(msg);
        write2File(getStackTraceString(th));
        return ENABLE ? Log.w(tag, msg, th) : 0;
    }

    public static int w(String tag, Throwable th) {
        if (th == null) {
            return 0;
        }
        write2File(getStackTraceString(th));
        return ENABLE ? Log.w(tag, th) : 0;
    }

    public static int e(String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.e(TAG, msg) : 0;
    }

    public static int e(String tag, String msg) {
        if (msg == null) {
            return 0;
        }
        write2File(msg);
        return ENABLE ? Log.e(tag, msg) : 0;
    }

    public static int e(String tag, String msg, Throwable th) {
        if (msg == null || th == null) {
            return 0;
        }
        write2File(msg);
        write2File(getStackTraceString(th));
        return ENABLE ? Log.e(tag, msg, th) : 0;
    }

    private synchronized static void write2File(String log) {
        if (LOG2FILE_ENABLE) {
            try {
                if (mWriter == null) {
                    return;
                }
                mWriter.write(log);
                mWriter.newLine();
                mWriter.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error writing log to file", e);
            }
        }
    }

    public static void onCreate(String fileName) {
        // 已废弃：请使用 onCreate(Context, String)
        Log.e(TAG, "onCreate(String) is deprecated, use onCreate(Context, String) instead");
    }
    
    public static void onCreate(android.content.Context context, String fileName) {
        File dir = new File(context.getFilesDir(), "RBQLog");

        try {
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }

            File file = new File(dir, fileName);
            if (!file.exists() && !file.createNewFile()) {
                Log.e(TAG, "Failed to create log file");
                return;
            }

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            mWriter = new BufferedWriter(osw, 1024);
            mWriter.write(TAG + " begin : ");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing log file", e);
        }
    }

    public static void onDestroy() {
        try {
            if (mWriter != null) {
                mWriter.write(TAG + " end : ");
                mWriter.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing log file", e);
        }
    }
}
