package com.org.jzprinter.utils.Storage;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ParcelableStorageUtil {

    private static final String PREFS_NAME = "parcelable_storage";

    // -------- SharedPreferences 存储 --------
    public static <T extends Parcelable> void saveToPreferences(Context context, String key, T value) {
        if (context == null || key == null) return;

        if (value == null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(key).apply();
            return;
        }

        Parcel parcel = Parcel.obtain();
        value.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();

        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, encoded).apply();
    }

    public static <T extends Parcelable> T loadFromPreferences(Context context, String key, Parcelable.Creator<T> creator) {
        if (context == null || key == null || creator == null) return null;

        String encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, null);
        if (encoded == null) {
            return null;
        }

        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);

        T value = creator.createFromParcel(parcel);
        parcel.recycle();

        return value;
    }

    public static void removeFromPreferences(Context context, String key) {
        if (context == null || key == null) return;

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(key).apply();
    }

    public static void clearPreferences(Context context) {
        if (context == null) return;

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    // -------- 文件存储 --------

    public static <T extends Parcelable> void saveToFile(Context context, String fileName, T value) {
        if (context == null || fileName == null) return;

        if (value == null) {
            deleteFile(context, fileName);
            return;
        }

        File file = new File(context.getFilesDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Parcel parcel = Parcel.obtain();
            value.writeToParcel(parcel, 0);
            fos.write(parcel.marshall());
            parcel.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T extends Parcelable> T loadFromFile(Context context, String fileName, Parcelable.Creator<T> creator) {
        if (context == null || fileName == null || creator == null) return null;

        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);

            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);

            T value = creator.createFromParcel(parcel);
            parcel.recycle();

            return value;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteFile(Context context, String fileName) {
        if (context == null || fileName == null) return;

        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}

