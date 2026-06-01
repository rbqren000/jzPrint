package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class CustomDialogBuilder {

    private final Context context;
    private View customView;
    private boolean cancelable = true;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;

    public CustomDialogBuilder(Context context) {
        this.context = context;
    }

    public CustomDialogBuilder view(View customView) { this.customView = customView; return this; }
    public CustomDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public CustomDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public CustomDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        if (customView != null) {
            dialog.setContentView(customView);
        }
        dialog.setCancelable(cancelable);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = dialogGravity;
            lp.width = dialogWidth > 0 ? (int) (context.getResources().getDisplayMetrics().widthPixels * dialogWidth)
                    : WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        dialog.show();
        return dialog;
    }
}
