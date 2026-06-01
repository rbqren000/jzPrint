package com.org.jzprinter.manager;

import static com.mx.mxSdk.Device.defaultCirculationTime;
import static com.mx.mxSdk.Device.defaultDirection;
import static com.mx.mxSdk.Device.defaultDistance;
import static com.mx.mxSdk.Device.defaultLandscapePix;
import static com.mx.mxSdk.Device.defaultPortraitPix;
import static com.mx.mxSdk.Device.defaultPrinterHead;
import static com.mx.mxSdk.Device.defaultRepeatTime;
import android.content.Context;
import android.text.TextUtils;

import com.org.jzprinter.utils.Storage.ParcelableStorageUtil;
import com.org.jzprinter.utils.Storage.PreferencesUtils;
import com.mx.mxSdk.ConnModel;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.Utils.RBQLog;

import androidx.annotation.Nullable;

public class ParameterManager {

	public static final String noMoreTipApInstructions = "noMoreTipApInstructions";//不再提示ap说明

	//不再提醒打开自动关机
	public static final String autoPowerOffNotReminderKey = "autoPowerOffNotReminderKey";

	// AI打印默认拼数
	public static final String aiPrintJointsKey = "aiPrintTilesKey";  // 保持key不变以兼容旧数据

	public static final String ConnModelKey = "ConnModelKey";
	public static final String DeviceKey = "DeviceKey";

	public static boolean flipHorizontally(@Nullable Device device) {
		if (!versionNumberOver1_7_2(device)) {
			return false;
		}
		int direction = direction(device);
		return direction != defaultDirection;
	}

	public static int printerHead(@Nullable Device device) {
		if (device == null) {
			return defaultPrinterHead;
		}
		return device.printer_head;
	}

	public static int landscapePix(@Nullable Device device) {
		if (device == null) return defaultLandscapePix;
		return device.l_pix;
	}

	public static int portraitPix(@Nullable Device device) {
		if (device == null) return defaultPortraitPix;
		return device.p_pix;
	}

	public static int distance(@Nullable Device device) {
		if (device == null) return defaultDistance;
		return device.distance;
	}

	public static int circulationTime(@Nullable Device device) {
		if (device == null) return defaultCirculationTime;
		return device.circulation;
	}

	public static int repeatTime(@Nullable Device device) {

		if (!versionNumberOver1_7_2(device)) return defaultRepeatTime;
		return device.repeat_time;
	}

	public static int direction(@Nullable Device device) {
		if (!versionNumberOver1_7_2(device)) return defaultDirection;
		return device.horizontalDirection;
	}

	public static int temperature(Device device) {
		if (device != null)
			return (int) device.temperature;
		return 0;
	}

	public static String directionText(@Nullable Device device) {
		if (!versionNumberOver1_7_2(device)) {
			return String.valueOf(defaultDirection);
		}
		int direction = device.horizontalDirection;
		return String.valueOf(direction);
	}

	public static boolean versionNumberOver1_7_2(@Nullable Device device) {

		if (device == null) return false;
		String mcu_version = device.mcuVersion;
		if (mcu_version == null) return false;
		String[] vs = mcu_version.split("\\.");
		if (vs.length > 3) {
			int v0 = Integer.parseInt(vs[0]);
			int v1 = Integer.parseInt(vs[1]);
			int v2 = Integer.parseInt(vs[2]);
			return v0 >= 1 && v1 >= 7 && v2 > 2;
		}
		return false;
	}

	public static boolean versionNumberOver1_7_2(String mcu_version) {

		if (TextUtils.isEmpty(mcu_version)) return false;
		String[] vs = mcu_version.split("\\.");
		if (vs.length > 3) {
			int v0 = Integer.parseInt(vs[0]);
			int v1 = Integer.parseInt(vs[1]);
			int v2 = Integer.parseInt(vs[2]);
			return v0 >= 1 && v1 >= 7 && v2 > 2;
		}
		return false;
	}

	public static boolean autoPowerOffNotReminder(Context context){
		return PreferencesUtils.getBoolean(context,autoPowerOffNotReminderKey,false);
	}

	public static void saveAutoPowerOffNotReminder(Context context,boolean notReminder){
		PreferencesUtils.putBoolean(context,autoPowerOffNotReminderKey,notReminder);
	}

    // 获取AI打印拼数，默认2
	public static int getAIPrintJoints(Context context) {
		int joints = PreferencesUtils.getInt(context, aiPrintJointsKey, 1);
		if (joints < 1 || joints > 6) {
			joints = 2;
		}
		return joints;
	}

	public static void saveAIPrintJoints(Context context, int joints) {
		if (joints >= 1 && joints <= 6) {
			PreferencesUtils.putInt(context, aiPrintJointsKey, joints);
		}
	}

	public static ConnModel loadConnModel(Context context){
		return ParcelableStorageUtil.loadFromPreferences(context,ConnModelKey,ConnModel.CREATOR);
	}

	public static void saveConnModel(Context context,ConnModel connModel){
		ParcelableStorageUtil.saveToPreferences(context,ConnModelKey,connModel);
	}

	public static Device loadDevice(Context context){
		return ParcelableStorageUtil.loadFromPreferences(context,DeviceKey,Device.CREATOR);
	}

	public static void saveDevice(Context context,Device device){
		RBQLog.i("保存本地设备-->device:"+device);
		ParcelableStorageUtil.saveToPreferences(context,DeviceKey,device);
	}
}
