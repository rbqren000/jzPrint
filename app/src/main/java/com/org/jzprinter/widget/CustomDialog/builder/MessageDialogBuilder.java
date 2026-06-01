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
import com.org.jzprinter.databinding.DialogMessageLayoutBinding;

public class MessageDialogBuilder {

    private final Context context;
    private String title;
    private int titleGravity = Gravity.CENTER;
    private String message;
    private String okText;
    private String cancelText;
    private boolean cancelable = false;
    private boolean showCheckBox = false;
    private boolean checked = false;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;
    private OnMessageDialogListener listener;

    public interface OnMessageDialogListener {
        void onOkClicked(Dialog dialog, boolean isChecked);
        void onCancelClicked(Dialog dialog);
        void onCheckChange(Dialog dialog, CompoundButton buttonView, boolean isChecked);
    }

    public MessageDialogBuilder(Context context) {
        this.context = context;
        this.okText = context.getString(R.string.dialog_ok);
        this.cancelText = context.getString(R.string.dialog_cancel);
    }

    public MessageDialogBuilder title(String title) { this.title = title; return this; }
    public MessageDialogBuilder titleGravity(int gravity) { this.titleGravity = gravity; return this; }
    public MessageDialogBuilder message(String message) { this.message = message; return this; }
    public MessageDialogBuilder okText(String text) { this.okText = text; return this; }
    public MessageDialogBuilder cancelText(String text) { this.cancelText = text; return this; }
    public MessageDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public MessageDialogBuilder showCheckBox(boolean show) { this.showCheckBox = show; return this; }
    public MessageDialogBuilder checked(boolean checked) { this.checked = checked; return this; }
    public MessageDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public MessageDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }
    public MessageDialogBuilder onOk(OnMessageDialogListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogMessageLayoutBinding binding = DialogMessageLayoutBinding.inflate(LayoutInflater.from(context));
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
        binding.cancelButton.setText(cancelText);

        binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onCheckChange(dialog, buttonView, isChecked);
        });
        binding.okButton.setOnClickListener(v -> {
            if (listener != null) listener.onOkClicked(dialog, binding.checkBox.isChecked());
            dialog.dismiss();
        });
        binding.cancelButton.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClicked(dialog);
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }
}
