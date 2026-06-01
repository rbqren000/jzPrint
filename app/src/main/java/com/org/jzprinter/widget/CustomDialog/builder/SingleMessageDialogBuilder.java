package com.org.jzprinter.widget.CustomDialog.builder;

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

import com.org.jzprinter.R;
import com.org.jzprinter.databinding.DialogMessageLayoutSingleBinding;

public class SingleMessageDialogBuilder {

    private final Context context;
    private String title;
    private int titleGravity = Gravity.CENTER;
    private String message;
    private String okText;
    private boolean cancelable = false;
    private boolean showCheckBox = false;
    private boolean checked = false;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;
    private OnSingleMessageDialogListener listener;

    public interface OnSingleMessageDialogListener {
        void onOkClicked(Dialog dialog, boolean isChecked);
        void onCheckChange(Dialog dialog, CompoundButton buttonView, boolean isChecked);
    }

    public SingleMessageDialogBuilder(Context context) {
        this.context = context;
        this.okText = context.getString(R.string.dialog_ok);
    }

    public SingleMessageDialogBuilder title(String title) { this.title = title; return this; }
    public SingleMessageDialogBuilder titleGravity(int gravity) { this.titleGravity = gravity; return this; }
    public SingleMessageDialogBuilder message(String message) { this.message = message; return this; }
    public SingleMessageDialogBuilder okText(String text) { this.okText = text; return this; }
    public SingleMessageDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public SingleMessageDialogBuilder showCheckBox(boolean show) { this.showCheckBox = show; return this; }
    public SingleMessageDialogBuilder checked(boolean checked) { this.checked = checked; return this; }
    public SingleMessageDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public SingleMessageDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }
    public SingleMessageDialogBuilder onOk(OnSingleMessageDialogListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogMessageLayoutSingleBinding binding = DialogMessageLayoutSingleBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
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

        binding.titleTextView.setGravity(titleGravity);
        if (TextUtils.isEmpty(title)) {
            binding.titleTextView.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.messageTextView.getLayoutParams();
            lp.topMargin = (int) (15 * context.getResources().getDisplayMetrics().density);
            binding.messageTextView.setLayoutParams(lp);
        } else {
            binding.titleTextView.setText(title);
        }

        binding.checkBox.setVisibility(showCheckBox ? View.VISIBLE : View.GONE);
        binding.checkBox.setChecked(checked);
        binding.messageTextView.setText(message);
        binding.okButton.setText(okText);

        binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onCheckChange(dialog, buttonView, isChecked);
        });
        binding.okButton.setOnClickListener(v -> {
            if (listener != null) listener.onOkClicked(dialog, binding.checkBox.isChecked());
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }
}
