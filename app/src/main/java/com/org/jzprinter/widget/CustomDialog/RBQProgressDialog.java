package com.org.jzprinter.widget.CustomDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.org.jzprinter.R;

import java.lang.ref.WeakReference;

public class RBQProgressDialog {

    private Dialog dialog;
    private TextView titleTextView;
    private TextView messageView;
    private WeakReference<Context> contextRef;

    public void show(Context context, String title, String msg) {
        contextRef = new WeakReference<>(context);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showDialog(context, title, msg);
        } else {
            runOnUiThread(() -> showDialog(context, title, msg));
        }
    }

    private void showDialog(Context context, String title, String msg) {
        // Activity 已销毁，不能显示 Dialog，否则 BadTokenException
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            return;
        }
        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            return;
        }

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(context, R.style.ProgressDialogStyle);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_rbq_progress, null);

        LinearLayout container = view.findViewById(R.id.container);

        titleTextView = view.findViewById(R.id.titleTextView);
        messageView = view.findViewById(R.id.progress_message);

        if (!TextUtils.isEmpty(title)) {
            titleTextView.setVisibility(View.VISIBLE);
            titleTextView.setText(title);
        } else {
            titleTextView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(msg)) {
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(msg);
        } else {
            messageView.setVisibility(View.GONE);
        }

        int minWidthDp = (title != null && !title.isEmpty() && msg != null && !msg.isEmpty()) ? 125 :
                ((title != null && !title.isEmpty()) || (msg != null && !msg.isEmpty()) ? 85 : 45);
        int minWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp, context.getResources().getDisplayMetrics());
        container.setMinimumWidth(minWidthPx);

        dialog.setContentView(view);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        dialog.show();
    }

    //设定对话框是否允许cancel
    public void setCancelable(boolean cancelable) {
        if (dialog != null) {
            dialog.setCancelable(cancelable);
        }
    }

    public void dismiss() {
        Context context = contextRef != null ? contextRef.get() : null;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissDialog(context);
        } else {
            runOnUiThread(() -> dismissDialog(context));
        }
    }

    private void dismissDialog(Context context) {
        if (dialog != null && dialog.isShowing() && context instanceof Activity && !((Activity) context).isFinishing()) {
            dialog.dismiss();
        }
    }

    public void updateTitle(String title) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateTitleUI(title);
        } else {
            runOnUiThread(() -> updateTitleUI(title));
        }
    }

    private void updateTitleUI(String title) {
        if (dialog != null && dialog.isShowing() && titleTextView != null) {
            if (title != null && !title.isEmpty()) {
                titleTextView.setText(title);
                titleTextView.setVisibility(View.VISIBLE);
            } else {
                titleTextView.setVisibility(View.GONE);
            }
        }
    }

    public void updateMessage(String msg) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateMessageUI(msg);
        } else {
            runOnUiThread(() -> updateMessageUI(msg));
        }
    }

    private void updateMessageUI(String msg) {
        if (dialog != null && dialog.isShowing() && messageView != null) {
            if (msg != null && !msg.isEmpty()) {
                messageView.setText(msg);
                messageView.setVisibility(View.VISIBLE);
            } else {
                messageView.setVisibility(View.GONE);
            }
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    private void runOnUiThread(Runnable action) {
        Context safeContext = contextRef != null ? contextRef.get() : null;
        if (safeContext instanceof Activity && !((Activity) safeContext).isFinishing()) {
            ((Activity) safeContext).runOnUiThread(action);
        }
    }
}






