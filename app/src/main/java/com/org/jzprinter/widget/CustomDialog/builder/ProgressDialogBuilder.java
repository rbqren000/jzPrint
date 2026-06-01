package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.org.jzprinter.databinding.DialogProgressLayoutBinding;

public class ProgressDialogBuilder {

    private final Context context;
    private String message;
    private boolean cancelable = false;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.6f;

    public ProgressDialogBuilder(Context context) {
        this.context = context;
    }

    public ProgressDialogBuilder message(String message) { this.message = message; return this; }
    public ProgressDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public ProgressDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public ProgressDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogProgressLayoutBinding binding = DialogProgressLayoutBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(false);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = dialogGravity;
            lp.width = dialogWidth > 0 ? (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth)
                    : WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
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
}
