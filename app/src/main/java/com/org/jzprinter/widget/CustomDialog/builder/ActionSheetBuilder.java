package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class ActionSheetBuilder {

    private final Context context;
    private String title;
    private List<String> items;
    private boolean cancelable = true;
    private OnActionSheetListener listener;

    public interface OnActionSheetListener {
        void onItemClick(Dialog dialog, int position, String item);
    }

    public ActionSheetBuilder(Context context) {
        this.context = context;
    }

    public ActionSheetBuilder title(String title) { this.title = title; return this; }
    public ActionSheetBuilder items(List<String> items) { this.items = items; return this; }
    public ActionSheetBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public ActionSheetBuilder onItemClick(OnActionSheetListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 16, 0, 16);

        if (!TextUtils.isEmpty(title)) {
            TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextSize(16);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(24, 16, 24, 16);
            container.addView(titleView);
        }

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                final int position = i;
                final String item = items.get(i);
                TextView itemView = new TextView(context);
                itemView.setText(item);
                itemView.setTextSize(16);
                itemView.setGravity(Gravity.CENTER);
                itemView.setPadding(24, 20, 24, 20);
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(dialog, position, item);
                    dialog.dismiss();
                });
                container.addView(itemView);
            }
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(container);
        dialog.setContentView(scrollView);
        dialog.setCancelable(cancelable);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }

        dialog.show();
        return dialog;
    }
}
