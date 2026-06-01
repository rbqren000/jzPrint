package com.mx.mxSdk.SppCenter;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BluetoothUtils {

	private static final String TAG = BluetoothUtils.class.getSimpleName();

	/**
	 *
	 * @param device 要配对的设备
	 * @return 返回创建bond是否成功
	 */
	public static boolean createBond(BluetoothDevice device){

		try{

			Log.i(TAG, "pair: 调用配对");

			Method createBondMethod = device.getClass().getMethod("createBond");
			Boolean returnValue = (Boolean) createBondMethod.invoke(device);
			if (returnValue==null){
				return false;
			}
			return returnValue;

		}catch (Exception e){

			e.printStackTrace();
		}

		return false;

	}

	/**
	 *
	 * @param device 要移除的配对的设备
	 * @return
     */
	public static boolean removeBond(BluetoothDevice device){

		try{

			Method removeBondMethod = device.getClass().getMethod("removeBond");
			Boolean returnValue = (Boolean) removeBondMethod.invoke(device);
			if (returnValue==null){
				return false;
			}
			return returnValue;

		}catch (Exception e){

			e.printStackTrace();
		}

		return false;
	}

	/**
	 *
	 * @param device 设置配对密码
	 * @param str 密码
	 * @return
	 */
	public static boolean setPin(BluetoothDevice device, String str) {
		try {

			Method removeBondMethod = device.getClass().getDeclaredMethod("setPin",
					new Class<?>[] { byte[].class });
			Boolean returnValue = (Boolean) removeBondMethod.invoke(device,
					new Object[] { str.getBytes() });
			if (returnValue==null){
				return false;
			}
			return returnValue;

		} catch (Exception e) {

			e.printStackTrace();
		}

		return false;

	}

	/**
	 *
	 * @param device
	 * @return
     */
	public static boolean cancelPairingUserInput(BluetoothDevice device) {

		try {

			Method createBondMethod = device.getClass().getMethod("cancelPairingUserInput");
			// cancelBondProcess()
			Boolean returnValue = (Boolean) createBondMethod.invoke(device);
			if (returnValue==null){
				return false;
			}
			return returnValue;

		}catch (Exception e){

			e.printStackTrace();
		}

		return false;
	}

	/**
	 *
	 * @param device
	 * @return
     */
	public static boolean cancelBondProcess(BluetoothDevice device){

		try {

			Method createBondMethod = device.getClass().getMethod("cancelBondProcess");
			Boolean returnValue = (Boolean) createBondMethod.invoke(device);
			if (returnValue==null){
				return false;
			}
			return returnValue;

		}catch (Exception e){

			e.printStackTrace();
		}

		return false;
	}

	/**
	 *
	 * @param device
	 * @return
     */
	public static boolean isConnected(BluetoothDevice device){

		try {

			Method isConnectedMethod = device.getClass().getMethod("isConnected");
			Boolean returnValue = (Boolean) isConnectedMethod.invoke(device);
			if (returnValue==null){
				return false;
			}
			return returnValue;

		}catch (Exception e){

			e.printStackTrace();
		}

		return false;
	}

	public static boolean isBonded(BluetoothDevice device){

		return device.getBondState()== BluetoothDevice.BOND_BONDED;
	}

	public static boolean isBonding(BluetoothDevice device){

		return device.getBondState()== BluetoothDevice.BOND_BONDING;
	}

	public static boolean isBondNone(BluetoothDevice device){

		return device.getBondState()== BluetoothDevice.BOND_NONE;
	}

	/**
	 *
	 * @param clsShow
	 */
	public static void printAllInform(Class<?> clsShow) {
		try {
			// 取得所有方法
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("method name", hideMethod[i].getName() + ";and the i is:"
						+ i);
			}
			// 取得所有常量
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("Field name", allFields[i].getName());
			}
		} catch (Exception e) {

			e.printStackTrace();

		}
	}

}
