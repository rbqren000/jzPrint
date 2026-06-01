package com.org.jzprinter.widget.Toast;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

/**
 * 自定义Toast视图工具类
 * 提供更灵活的Toast显示方式，支持自定义视图、位置和持续时间
 */
public class ToastView {
	
	private final Toast toast;
	
	/**
	 * 创建一个默认的ToastView实例
	 * @param context 上下文环境
	 */
	public ToastView(Context context) {
		toast = new Toast(context);
		toast.setGravity(Gravity.CENTER, 0, 0);
	}
	
	/**
	 * 创建一个带有自定义视图的ToastView实例
	 * @param context 上下文环境
	 * @param view 自定义视图
	 */
	public ToastView(Context context, View view) {
		this(context);
		toast.setView(view);
	}
	
	/**
	 * 创建一个带有自定义视图和持续时间的ToastView实例
	 * @param context 上下文环境
	 * @param view 自定义视图
	 * @param duration 持续时间（Toast.LENGTH_SHORT或Toast.LENGTH_LONG）
	 */
	public ToastView(Context context, View view, int duration) {
		this(context, view);
		toast.setDuration(duration);
	}
	
	/**
	 * 显示Toast
	 */
	public void show() {
		if (toast != null) {
			toast.show();
		}
	}
	
	/**
	 * 设置Toast的视图
	 * @param view 要设置的视图
	 * @return 当前ToastView实例，支持链式调用
	 */
	public ToastView setView(View view) {
		toast.setView(view);
		return this;
	}
	
	/**
	 * 设置Toast的持续时间
	 * @param duration 持续时间（Toast.LENGTH_SHORT或Toast.LENGTH_LONG）
	 * @return 当前ToastView实例，支持链式调用
	 */
	public ToastView setDuration(int duration) {
		toast.setDuration(duration);
		return this;
	}
	
	/**
	 * 设置Toast的显示位置
	 * @param gravity 重心位置
	 * @param xOffset X轴偏移量
	 * @param yOffset Y轴偏移量
	 * @return 当前ToastView实例，支持链式调用
	 */
	public ToastView setGravity(int gravity, int xOffset, int yOffset) {
		toast.setGravity(gravity, xOffset, yOffset);
		return this;
	}
	
	/**
	 * 取消Toast显示
	 */
	public void cancel() {
		if (toast != null) {
			toast.cancel();
		}
	}
	
	/**
	 * 创建并显示一个简单的Toast
	 * @param context 上下文环境
	 * @param view 自定义视图
	 * @return 创建的ToastView实例
	 */
	public static ToastView makeText(Context context, View view) {
		return new ToastView(context, view);
	}
	
	/**
	 * 创建并显示一个带有持续时间的Toast
	 * @param context 上下文环境
	 * @param view 自定义视图
	 * @param duration 持续时间
	 * @return 创建的ToastView实例
	 */
	public static ToastView makeText(Context context, View view, int duration) {
		return new ToastView(context, view, duration);
	}
}
