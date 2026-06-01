// Created by RBQ on 2025/10/17.
package com.org.jzprinter.widget.CustomDialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.org.jzprinter.R;
import com.org.jzprinter.databinding.DialogMessageLayoutBinding;
import com.org.jzprinter.databinding.DialogMessageLayoutSingleBinding;
import com.org.jzprinter.databinding.DialogSingleChoiceLayoutBinding;
import com.org.jzprinter.databinding.DialogMultiChoiceLayoutBinding;
import com.org.jzprinter.databinding.DialogProgressLayoutBinding;
import com.org.jzprinter.databinding.DialogInputLayoutBinding;
import com.org.jzprinter.databinding.DialogIndeterminateProgressLayoutBinding;
import com.org.jzprinter.widget.CustomDialog.adapter.MultiChoiceAdapter;
import com.org.jzprinter.widget.CustomDialog.adapter.SingleChoiceAdapter;
import com.org.jzprinter.widget.CustomDialog.builder.*;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import androidx.recyclerview.widget.LinearLayoutManager;


public class DialogFactory {

    private static DialogFactory instance;

    // 私有构造函数，防止外部实例化
    private DialogFactory() {}

    // 获取单例实例
    public static synchronized DialogFactory share() {
        if (instance == null) {
            instance = new DialogFactory();
        }
        return instance;
    }

    /********************************************************************************
     * Builder API - 推荐使用
     ********************************************************************************/

    /**
     * 创建单按钮消息对话框 Builder
     * <pre>
     * DialogFactory.share().singleMessage(context)
     *     .title("提示")
     *     .message("操作成功")
     *     .okText("确定")
     *     .show();
     * </pre>
     */
    public SingleMessageDialogBuilder singleMessage(Context context) {
        return new SingleMessageDialogBuilder(context);
    }

    /**
     * 创建双按钮消息对话框 Builder
     * <pre>
     * DialogFactory.share().message(context)
     *     .title("确认")
     *     .message("确定要删除吗？")
     *     .okText("删除")
     *     .cancelText("取消")
     *     .onOk((dialog, checked) -> dialog.dismiss())
     *     .show();
     * </pre>
     */
    public MessageDialogBuilder message(Context context) {
        return new MessageDialogBuilder(context);
    }

    /**
     * 创建进度对话框 Builder
     * <pre>
     * Dialog dialog = DialogFactory.share().progress(context)
     *     .message("加载中...")
     *     .show();
     * </pre>
     */
    public ProgressDialogBuilder progress(Context context) {
        return new ProgressDialogBuilder(context);
    }

    /**
     * 创建输入对话框 Builder
     * <pre>
     * DialogFactory.share().input(context)
     *     .title("输入名称")
     *     .hint("请输入...")
     *     .onOk((dialog, text) -> { })
     *     .show();
     * </pre>
     */
    public InputDialogBuilder input(Context context) {
        return new InputDialogBuilder(context);
    }

    /**
     * 创建不确定进度对话框 Builder
     * <pre>
     * DialogFactory.share().indeterminateProgress(context)
     *     .title("请稍候")
     *     .message("正在处理...")
     *     .show();
     * </pre>
     */
    public IndeterminateProgressDialogBuilder indeterminateProgress(Context context) {
        return new IndeterminateProgressDialogBuilder(context);
    }

    /**
     * 创建单选对话框 Builder
     * <pre>
     * DialogFactory.share().singleChoice(context)
     *     .title("选择选项")
     *     .items(Arrays.asList("选项1", "选项2"))
     *     .defaultSelection(0)
     *     .onOk((dialog, pos, item) -> { })
     *     .show();
     * </pre>
     */
    public SingleChoiceDialogBuilder singleChoice(Context context) {
        return new SingleChoiceDialogBuilder(context);
    }

    /**
     * 创建多选对话框 Builder
     * <pre>
     * DialogFactory.share().multiChoice(context)
     *     .title("选择多个")
     *     .items(Arrays.asList("选项1", "选项2"))
     *     .showSelectAllClear(true)
     *     .onOk((dialog, positions, items) -> { })
     *     .show();
     * </pre>
     */
    public MultiChoiceDialogBuilder multiChoice(Context context) {
        return new MultiChoiceDialogBuilder(context);
    }

    /**
     * 创建自定义视图对话框 Builder
     * <pre>
     * DialogFactory.share().custom(context)
     *     .view(customView)
     *     .cancelable(true)
     *     .show();
     * </pre>
     */
    public CustomDialogBuilder custom(Context context) {
        return new CustomDialogBuilder(context);
    }

    /**
     * 创建 ActionSheet 样式对话框 Builder
     * 类似 iOS ActionSheet，点击选项直接执行回调
     * <pre>
     * DialogFactory.share().actionSheet(context)
     *     .title("选择操作")
     *     .items(Arrays.asList("选项1", "选项2"))
     *     .onItemClick((dialog, position, item) -> {
     *         // 处理点击
     *     })
     *     .show();
     * </pre>
     */
    public ActionSheetBuilder actionSheet(Context context) {
        return new ActionSheetBuilder(context);
    }

    /**
     * 创建国家代码选择对话框 Builder
     * 支持搜索和字母索引快速定位
     * <pre>
     * DialogFactory.share().countryCode(context)
     *     .onItemClick((dialog, item) -> {
     *         String countryName = item.getCountryName();
     *         String region = item.getRegion();
     *     })
     *     .show();
     * </pre>
     */
    public CountryCodeDialogBuilder countryCode(Context context) {
        return new CountryCodeDialogBuilder(context);
    }

    /********************************************************************************
     * Single Message Dialog (Legacy API - 建议使用 singleMessage() Builder)
     ********************************************************************************/

    public interface OnSingleMessageDialogListener {
        void onOkClicked(Dialog dialog, boolean isChecked);
        void onCheckChange(Dialog dialog, CompoundButton buttonView, boolean isChecked);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId),Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId, boolean cancelable, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f,onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, false, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText, boolean cancelable, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, cancelable, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText, boolean cancelable, boolean showCheckBox, boolean checked, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId),Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId),Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, boolean cancelable, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, boolean cancelable, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), cancelable,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, String message, String okText, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, Gravity.CENTER, message, okText, false, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, int titleGravity, String message, String okText, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, titleGravity, message, okText, false, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, String message, String okText, boolean cancelable,OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, Gravity.CENTER, message, okText, cancelable, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, int titleGravity, String message, String okText, boolean cancelable, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, titleGravity, message, okText, cancelable, false, false,Gravity.CENTER,0.85f, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId),dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth,onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, false, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, cancelable, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String message, String okText, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, "", Gravity.CENTER, message, okText, cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId),dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId),dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), cancelable,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, String message, String okText,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, Gravity.CENTER, message, okText, false, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, int titleGravity, String message, String okText,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, titleGravity, message, okText, false, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, String message, String okText, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, Gravity.CENTER, message, okText, cancelable, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    @Deprecated
    public Dialog showSingleMessageDialog(Context context, String title, int titleGravity, String message, String okText, boolean cancelable,int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        return showSingleMessageDialog(context, title, titleGravity, message, okText, cancelable, false, false,dialogGravity,dialogWidth, onSingleMessageDialogListener);
    }

    public Dialog showSingleMessageDialog(Context context, String title, int titleGravity, String message, String okText, boolean cancelable, boolean showCheckBox, boolean checked, int dialogGravity, float dialogWidth, OnSingleMessageDialogListener onSingleMessageDialogListener) {
        Dialog dialog = new Dialog(context);
        DialogMessageLayoutSingleBinding dialogMessageLayout0Binding = DialogMessageLayoutSingleBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(dialogMessageLayout0Binding.getRoot());
        dialog.setCancelable(cancelable);

        // 设置对话框位置和宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            layoutParams.gravity = dialogGravity;

            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }

            window.setAttributes(layoutParams);
        }

        dialogMessageLayout0Binding.titleTextView.setGravity(titleGravity);
        if (TextUtils.isEmpty(title)) {

            dialogMessageLayout0Binding.titleTextView.setVisibility(View.GONE);

            // 动态设置 messageTextView 的 marginTop 为 15dp
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) dialogMessageLayout0Binding.messageTextView.getLayoutParams();
            layoutParams.topMargin = (int) (15 * context.getResources().getDisplayMetrics().density); // 转换 dp 为像素
            dialogMessageLayout0Binding.messageTextView.setLayoutParams(layoutParams);
        }

        int visible = showCheckBox ? View.VISIBLE : View.GONE;
        dialogMessageLayout0Binding.checkBox.setVisibility(visible);
        dialogMessageLayout0Binding.checkBox.setChecked(checked);

        dialogMessageLayout0Binding.titleTextView.setText(title);
        dialogMessageLayout0Binding.messageTextView.setText(message);
        dialogMessageLayout0Binding.okButton.setText(okText);

        dialogMessageLayout0Binding.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (onSingleMessageDialogListener != null) {
                    onSingleMessageDialogListener.onCheckChange(dialog, buttonView, isChecked);
                }
            }
        });
        dialogMessageLayout0Binding.okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = dialogMessageLayout0Binding.checkBox.isChecked();
                if (onSingleMessageDialogListener != null) {
                    onSingleMessageDialogListener.onOkClicked(dialog, checked);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
        return dialog;
    }

    /********************************************************************************
     * Message Dialog (Legacy API - 建议使用 message() Builder)
     ********************************************************************************/

    public interface OnMessageDialogListener {
        void onOkClicked(Dialog dialog, boolean isChecked);
        void onCancelClicked(Dialog dialog);
        void onCheckChange(Dialog dialog, CompoundButton buttonView, boolean isChecked);
    }

    //下边的缺省dialog的位置和宽度信息
    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, false, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, cancelable, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, boolean cancelable, boolean showCheckBox, boolean checked, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, String message, String okText, String cancelText, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, Gravity.CENTER, message, okText, cancelText, false, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, int titleGravity, String message, String okText, String cancelText, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, titleGravity, message, okText, cancelText, false, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, String message, String okText, String cancelText, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, Gravity.CENTER, message, okText, cancelText, cancelable, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, int titleGravity, String message, String okText, String cancelText, boolean cancelable, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, titleGravity, message, okText, cancelText, cancelable, false, false,Gravity.CENTER,0.85f, onMessageDialogListener);
    }

    // 需要输入dialog的位置和宽度信息
    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, false, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, boolean cancelable, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, cancelable, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String message, String okText, String cancelText, boolean cancelable, boolean showCheckBox, boolean checked, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, "", Gravity.CENTER, message, okText, cancelText, cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId),dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), Gravity.CENTER, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, int titleResId, int titleGravity, int messageResId, int okTextResId, int cancelTextResId, boolean cancelable, boolean showCheckBox, boolean checked,int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, context.getString(titleResId), titleGravity, context.getString(messageResId), context.getString(okTextResId), context.getString(cancelTextResId), cancelable, showCheckBox, checked,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, String message, String okText, String cancelText,int dialogGravity, float dialogWidth,  OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, Gravity.CENTER, message, okText, cancelText, false, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, int titleGravity, String message, String okText, String cancelText,int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, titleGravity, message, okText, cancelText, false, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, String message, String okText, String cancelText, boolean cancelable,int dialogGravity, float dialogWidth,  OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, Gravity.CENTER, message, okText, cancelText, cancelable, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    @Deprecated
    public Dialog showMessageDialog(Context context, String title, int titleGravity, String message, String okText, String cancelText, boolean cancelable,int dialogGravity, float dialogWidth,  OnMessageDialogListener onMessageDialogListener) {
        return showMessageDialog(context, title, titleGravity, message, okText, cancelText, cancelable, false, false,dialogGravity,dialogWidth, onMessageDialogListener);
    }

    public Dialog showMessageDialog(Context context, String title, int titleGravity, String message, String okText, String cancelText, boolean cancelable, boolean showCheckBox, boolean checked, int dialogGravity, float dialogWidth, OnMessageDialogListener onMessageDialogListener) {
        Dialog dialog = new Dialog(context);
        DialogMessageLayoutBinding messageLayoutBinding = DialogMessageLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(messageLayoutBinding.getRoot());
        dialog.setCancelable(cancelable);

        // 设置对话框位置和宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            layoutParams.gravity = dialogGravity;

            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }

            window.setAttributes(layoutParams);
        }

        messageLayoutBinding.titleTextView.setGravity(titleGravity);
        if (TextUtils.isEmpty(title)) {

            messageLayoutBinding.titleTextView.setVisibility(View.GONE);

            // 动态设置 messageTextView 的 marginTop 为 15dp
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) messageLayoutBinding.messageTextView.getLayoutParams();
            layoutParams.topMargin = (int) (15 * context.getResources().getDisplayMetrics().density); // 转换 dp 为像素
            messageLayoutBinding.messageTextView.setLayoutParams(layoutParams);

        }

        int visible = showCheckBox ? View.VISIBLE : View.GONE;
        messageLayoutBinding.checkBox.setVisibility(visible);
        messageLayoutBinding.checkBox.setChecked(checked);

        messageLayoutBinding.titleTextView.setText(title);
        messageLayoutBinding.messageTextView.setText(message);
        messageLayoutBinding.okButton.setText(okText);
        messageLayoutBinding.cancelButton.setText(cancelText);

        messageLayoutBinding.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (onMessageDialogListener != null) {
                    onMessageDialogListener.onCheckChange(dialog, buttonView, isChecked);
                }
            }
        });
        messageLayoutBinding.okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = messageLayoutBinding.checkBox.isChecked();
                if (onMessageDialogListener != null) {
                    onMessageDialogListener.onOkClicked(dialog, checked);
                }
                dialog.dismiss();
            }
        });
        messageLayoutBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onMessageDialogListener != null) {
                    onMessageDialogListener.onCancelClicked(dialog);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
        return dialog;
    }

    /********************************************************************************
     * Custom Dialog (Legacy API - 建议使用 custom() Builder)
     ********************************************************************************/

    @Deprecated
    public Dialog showCustomDialog(Context context, View customView) {
        return showCustomDialog(context,customView,false,Gravity.CENTER,0.85f);
    }

    @Deprecated
    public Dialog showCustomDialog(Context context, View customView, boolean cancelable) {
        return showCustomDialog(context,customView,cancelable,Gravity.CENTER,0.85f);
    }

    @Deprecated
    public Dialog showCustomDialog(Context context, View customView, float dialogWidth) {
        return showCustomDialog(context,customView,false,Gravity.CENTER,dialogWidth);
    }

    public Dialog showCustomDialog(Context context, View customView, boolean cancelable, int dialogGravity, float dialogWidth) {
        // 创建自定义对话框
        Dialog dialog = new Dialog(context);
        dialog.setContentView(customView);
        dialog.setCancelable(cancelable);

        // 设置对话框位置和宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            layoutParams.gravity = dialogGravity;

            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }

            window.setAttributes(layoutParams);
        }
        // 显示对话框
        dialog.show();
        //返回对话框
        return dialog;
    }

    /********************************************************************************
     * Progress Dialog (Legacy API - 建议使用 progress() Builder)
     ********************************************************************************/

    @Deprecated
    public Dialog showProgressDialog(Context context) {
        return showProgressDialog(context, "", false, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showProgressDialog(Context context, boolean cancelable) {
        return showProgressDialog(context, "", cancelable, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showProgressDialog(Context context, String message) {
        return showProgressDialog(context, message, false, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showProgressDialog(Context context, String message, boolean cancelable) {
        return showProgressDialog(context, message, cancelable, Gravity.CENTER, 0.6f);
    }

    public Dialog showProgressDialog(Context context, String message, boolean cancelable, int dialogGravity, float dialogWidth) {
        Dialog dialog = new Dialog(context);
        DialogProgressLayoutBinding binding = DialogProgressLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(false);

        // 设置对话框位置和宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.gravity = dialogGravity;
            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            window.setAttributes(layoutParams);
        }

        if (TextUtils.isEmpty(message)) {
            binding.progressMessage.setVisibility(View.GONE);
        } else {
            binding.progressMessage.setVisibility(View.VISIBLE);
            binding.progressMessage.setText(message);
        }

        dialog.show();
        return dialog;
    }

    // 动态更新加载消息
    public void updateProgressMessage(Dialog dialog, String message) {
        if (dialog == null) return;
        View v = dialog.findViewById(R.id.progress_message);
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (TextUtils.isEmpty(message)) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(message);
                tv.setVisibility(View.VISIBLE);
            }
        }
    }

    /********************************************************************************
     * Input Dialog (Legacy API - 建议使用 input() Builder)
     ********************************************************************************/

    public interface OnInputDialogListener {
        void onOkClicked(Dialog dialog, String inputText);
        void onCancelClicked(Dialog dialog);
    }

    @Deprecated
    public Dialog showInputDialog(Context context, String title, String hint, String defaultText,
                                  boolean cancelable, int dialogGravity, float dialogWidth,
                                  OnInputDialogListener listener) {
        Dialog dialog = new Dialog(context);
        DialogInputLayoutBinding binding = DialogInputLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);

        // 设置位置与宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = dialogGravity;
            lp.width = dialogWidth > 0 ? (int)(context.getResources().getDisplayMetrics().widthPixels * dialogWidth)
                                       : WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        // 标题
        if (TextUtils.isEmpty(title)) {
            binding.titleText.setVisibility(View.GONE);
        } else {
            binding.titleText.setText(title);
        }
        // 提示与默认值
        if (!TextUtils.isEmpty(hint)) binding.editText.setHint(hint);
        if (!TextUtils.isEmpty(defaultText)) binding.editText.setText(defaultText);

        // 按钮
        binding.okButton.setOnClickListener(v -> {
            String text = binding.editText.getText() != null ? binding.editText.getText().toString().trim() : "";
            if (listener != null) listener.onOkClicked(dialog, text);
            dialog.dismiss();
        });
        binding.cancelButton.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClicked(dialog);
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }

    // 便捷重载
    @Deprecated
    public Dialog showInputDialog(Context context, String title, OnInputDialogListener listener) {
        return showInputDialog(context, title, "", "", true, Gravity.CENTER, 0.85f, listener);
    }
    @Deprecated
    public Dialog showInputDialog(Context context, String title, String hint, OnInputDialogListener listener) {
        return showInputDialog(context, title, hint, "", true, Gravity.CENTER, 0.85f, listener);
    }
    @Deprecated
    public Dialog showInputDialog(Context context, String title, String hint, String defaultText, OnInputDialogListener listener) {
        return showInputDialog(context, title, hint, defaultText, true, Gravity.CENTER, 0.85f, listener);
    }
    @Deprecated
    public Dialog showInputDialog(Context context, String title, String hint, String defaultText, boolean cancelable, OnInputDialogListener listener) {
        return showInputDialog(context, title, hint, defaultText, cancelable, Gravity.CENTER, 0.85f, listener);
    }

    /********************************************************************************
     * Indeterminate Progress Dialog (Legacy API - 建议使用 indeterminateProgress() Builder)
     ********************************************************************************/

    @Deprecated
    public Dialog showIndeterminateProgressDialog(Context context) {
        return showIndeterminateProgressDialog(context, "", "", false, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showIndeterminateProgressDialog(Context context, boolean cancelable) {
        return showIndeterminateProgressDialog(context, "", "", cancelable, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showIndeterminateProgressDialog(Context context, String title, String message) {
        return showIndeterminateProgressDialog(context, title, message, false, Gravity.CENTER, 0.6f);
    }

    @Deprecated
    public Dialog showIndeterminateProgressDialog(Context context, String title, String message, boolean cancelable) {
        return showIndeterminateProgressDialog(context, title, message, cancelable, Gravity.CENTER, 0.6f);
    }

    public Dialog showIndeterminateProgressDialog(Context context, String title, String message, boolean cancelable, int dialogGravity, float dialogWidth) {
        Dialog dialog = new Dialog(context);
        DialogIndeterminateProgressLayoutBinding binding = DialogIndeterminateProgressLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(false);

        // 设置位置与宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = dialogGravity;
            lp.width = dialogWidth > 0 ? (int)(context.getResources().getDisplayMetrics().widthPixels * dialogWidth)
                                        : WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        // 标题与消息
        if (TextUtils.isEmpty(title)) {
            binding.titleText.setVisibility(View.GONE);
        } else {
            binding.titleText.setText(title);
        }
        if (TextUtils.isEmpty(message)) {
            binding.messageText.setVisibility(View.GONE);
        } else {
            binding.messageText.setText(message);
        }

        dialog.show();
        return dialog;
    }

    // 动态更新标题与消息
    public void updateIndeterminateProgress(Dialog dialog, String title, String message) {
        if (dialog == null) return;
        View t = dialog.findViewById(R.id.title_text);
        View m = dialog.findViewById(R.id.message_text);
        if (t instanceof TextView tv) {
            if (TextUtils.isEmpty(title)) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(title);
                tv.setVisibility(View.VISIBLE);
            }
        }
        if (m instanceof TextView mv) {
            if (TextUtils.isEmpty(message)) {
                mv.setVisibility(View.GONE);
            } else {
                mv.setText(message);
                mv.setVisibility(View.VISIBLE);
            }
        }
    }

    /********************************************************************************
     * Single Choice Dialog (Legacy API - 建议使用 singleChoice() Builder)
     ********************************************************************************/

    public interface OnSingleChoiceDialogListener {
        void onOkClicked(Dialog dialog, int selectedPosition, String selectedItem);
        void onCancelClicked(Dialog dialog);
    }

    @Deprecated
    public Dialog showSingleChoiceDialog(
            Context context,
            String title,
            List<String> items,
            int defaultSelection,
            boolean cancelable,
            int dialogGravity,
            float dialogWidth,
            boolean showCheckBox,
            boolean checked,
            String checkBoxText,
            OnSingleChoiceDialogListener listener) {

        Dialog dialog = new Dialog(context);
        DialogSingleChoiceLayoutBinding binding = DialogSingleChoiceLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);

        // Set dialog window attributes
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.gravity = dialogGravity;
            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            window.setAttributes(layoutParams);
        }

        // Set title
        if (TextUtils.isEmpty(title)) {
            binding.titleTextView.setVisibility(View.GONE);
        } else {
            binding.titleTextView.setText(title);
        }

        // Optional checkbox
        binding.checkBox.setVisibility(showCheckBox ? View.VISIBLE : View.GONE);
        binding.checkBox.setChecked(checked);
        if (!TextUtils.isEmpty(checkBoxText)) {
            binding.checkBox.setText(checkBoxText);
        }

        // Setup RecyclerView
        List<String> safeItems = (items != null) ? items : Collections.emptyList();
        SingleChoiceAdapter adapter = new SingleChoiceAdapter(safeItems, defaultSelection);
        binding.optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.optionsRecyclerView.setAdapter(adapter);

        // Set button listeners
        binding.okButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOkClicked(dialog, adapter.getSelectedPosition(), adapter.getSelectedItem());
            }
            dialog.dismiss();
        });

        binding.cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelClicked(dialog);
            }
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }

    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items, int defaultSelection, OnSingleChoiceDialogListener listener) {
        return showSingleChoiceDialog(context, title, items, defaultSelection, true, Gravity.CENTER, 0.85f, false, false, "", listener);
    }

    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items, OnSingleChoiceDialogListener listener) {
        return showSingleChoiceDialog(context, title, items, -1, true, Gravity.CENTER, 0.85f, false, false, "", listener);
    }

    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items, String defaultSelectionItem, OnSingleChoiceDialogListener listener) {
        int defaultSelectionIndex = (items != null) ? items.indexOf(defaultSelectionItem) : -1;
        return showSingleChoiceDialog(context, title, items, defaultSelectionIndex, true, Gravity.CENTER, 0.85f, false, false, "", listener);
    }

    // 新增便捷重载：支持复选框控制（按索引默认选中）
    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items, int defaultSelection,
                                         boolean showCheckBox, boolean checked, String checkBoxText,
                                         OnSingleChoiceDialogListener listener) {
        return showSingleChoiceDialog(context, title, items, defaultSelection, true, Gravity.CENTER, 0.85f,
                showCheckBox, checked, checkBoxText, listener);
    }

    // 新增便捷重载：支持复选框控制（不指定默认选中）
    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items,
                                         boolean showCheckBox, boolean checked, String checkBoxText,
                                         OnSingleChoiceDialogListener listener) {
        return showSingleChoiceDialog(context, title, items, -1, true, Gravity.CENTER, 0.85f,
                showCheckBox, checked, checkBoxText, listener);
    }

    // 新增便捷重载：支持复选框控制（按字符串默认选中）
    @Deprecated
    public Dialog showSingleChoiceDialog(Context context, String title, List<String> items, String defaultSelectionItem,
                                         boolean showCheckBox, boolean checked, String checkBoxText,
                                         OnSingleChoiceDialogListener listener) {
        int defaultSelectionIndex = (items != null) ? items.indexOf(defaultSelectionItem) : -1;
        return showSingleChoiceDialog(context, title, items, defaultSelectionIndex, true, Gravity.CENTER, 0.85f,
                showCheckBox, checked, checkBoxText, listener);
    }

    /********************************************************************************
     * Multi Choice Dialog (Legacy API - 建议使用 multiChoice() Builder)
     ********************************************************************************/

    public interface OnMultiChoiceDialogListener {
        void onOkClicked(Dialog dialog, List<Integer> selectedPositions, List<String> selectedItems);
        void onCancelClicked(Dialog dialog);
    }

    @Deprecated
    public Dialog showMultiChoiceDialog(
            Context context,
            String title,
            List<String> items,
            Set<Integer> defaultSelectedIndices,
            boolean cancelable,
            int dialogGravity,
            float dialogWidth,
            boolean showSelectAllClear,
            boolean showCheckBox,
            boolean checked,
            String checkBoxText,
            OnMultiChoiceDialogListener listener) {

        Dialog dialog = new Dialog(context);
        DialogMultiChoiceLayoutBinding binding = DialogMultiChoiceLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);

        // 设置对话框位置和宽度
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.gravity = dialogGravity;
            if (dialogWidth > 0) {
                layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth);
            } else {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            window.setAttributes(layoutParams);
        }

        // 标题
        if (TextUtils.isEmpty(title)) {
            binding.titleTextView.setVisibility(View.GONE);
        } else {
            binding.titleTextView.setText(title);
        }

        // 可选复选框
        binding.checkBox.setVisibility(showCheckBox ? View.VISIBLE : View.GONE);
        binding.checkBox.setChecked(checked);
        if (!TextUtils.isEmpty(checkBoxText)) {
            binding.checkBox.setText(checkBoxText);
        }

        // 操作区显示控制
        binding.actionContainer.setVisibility(showSelectAllClear ? View.VISIBLE : View.GONE);

        // 列表
        MultiChoiceAdapter adapter = new MultiChoiceAdapter(items, defaultSelectedIndices);
        binding.optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.optionsRecyclerView.setAdapter(adapter);

        // 全选/清除
        binding.selectAllButton.setOnClickListener(v -> adapter.selectAll());
        binding.clearButton.setOnClickListener(v -> adapter.clearAll());

        // 确定/取消
        binding.okButton.setOnClickListener(v -> {
            if (listener != null) {
                List<Integer> positions = new ArrayList<>(adapter.getSelectedIndices());
                List<String> selectedItems = adapter.getSelectedItems();
                listener.onOkClicked(dialog, positions, selectedItems);
            }
            dialog.dismiss();
        });
        binding.cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelClicked(dialog);
            }
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }

    // 便捷重载：默认参数
    @Deprecated
    public Dialog showMultiChoiceDialog(Context context, String title, List<String> items, Set<Integer> defaultSelectedIndices, boolean showSelectAllClear, OnMultiChoiceDialogListener listener) {
        return showMultiChoiceDialog(context, title, items, defaultSelectedIndices, true, Gravity.CENTER, 0.85f, showSelectAllClear, false, false, "", listener);
    }

    @Deprecated
    public Dialog showMultiChoiceDialog(Context context, String title, List<String> items, boolean showSelectAllClear, OnMultiChoiceDialogListener listener) {
        return showMultiChoiceDialog(context, title, items, null, true, Gravity.CENTER, 0.85f, showSelectAllClear, false, false, "", listener);
    }

    // 通过字符串集合推导默认选中
    @Deprecated
    public Dialog showMultiChoiceDialogFromItems(Context context, String title, List<String> items, Set<String> defaultSelectedItems, boolean showSelectAllClear, OnMultiChoiceDialogListener listener) {
        Set<Integer> indices = new java.util.HashSet<>();
        if (defaultSelectedItems != null && items != null) {
            for (String s : defaultSelectedItems) {
                int idx = items.indexOf(s);
                if (idx >= 0) indices.add(idx);
            }
        }
        return showMultiChoiceDialog(context, title, items, indices, true, Gravity.CENTER, 0.85f, showSelectAllClear, false, false, "", listener);
    }

    // 新增便捷重载：支持复选框控制（索引集合默认选中）
    @Deprecated
    public Dialog showMultiChoiceDialog(Context context, String title, List<String> items, Set<Integer> defaultSelectedIndices,
                                        boolean showSelectAllClear, boolean showCheckBox, boolean checked, String checkBoxText,
                                        OnMultiChoiceDialogListener listener) {
        return showMultiChoiceDialog(context, title, items, defaultSelectedIndices, true, Gravity.CENTER, 0.85f,
                showSelectAllClear, showCheckBox, checked, checkBoxText, listener);
    }

    // 新增便捷重载：支持复选框控制（不指定默认选中）
    @Deprecated
    public Dialog showMultiChoiceDialog(Context context, String title, List<String> items,
                                        boolean showSelectAllClear, boolean showCheckBox, boolean checked, String checkBoxText,
                                        OnMultiChoiceDialogListener listener) {
        return showMultiChoiceDialog(context, title, items, null, true, Gravity.CENTER, 0.85f,
                showSelectAllClear, showCheckBox, checked, checkBoxText, listener);
    }

    // 新增便捷重载：支持复选框控制（字符串集合默认选中）
    @Deprecated
    public Dialog showMultiChoiceDialogFromItems(Context context, String title, List<String> items, Set<String> defaultSelectedItems,
                                                 boolean showSelectAllClear, boolean showCheckBox, boolean checked, String checkBoxText,
                                                 OnMultiChoiceDialogListener listener) {
        Set<Integer> indices = new java.util.HashSet<>();
        if (defaultSelectedItems != null && items != null) {
            for (String s : defaultSelectedItems) {
                int idx = items.indexOf(s);
                if (idx >= 0) indices.add(idx);
            }
        }
        return showMultiChoiceDialog(context, title, items, indices, true, Gravity.CENTER, 0.85f,
                showSelectAllClear, showCheckBox, checked, checkBoxText, listener);
    }

}