package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.org.jzprinter.databinding.DialogInputLayoutBinding;

public class InputDialogBuilder {

    private final Context context;
    private String title;
    private String hint;
    private String defaultText;
    private boolean cancelable = true;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;
    private OnInputDialogListener listener;

    public interface OnInputDialogListener {
        void onOkClicked(Dialog dialog, String inputText);
        void onCancelClicked(Dialog dialog);
    }

    public InputDialogBuilder(Context context) {
        this.context = context;
    }

    public InputDialogBuilder title(String title) { this.title = title; return this; }
    public InputDialogBuilder hint(String hint) { this.hint = hint; return this; }
    public InputDialogBuilder defaultText(String text) { this.defaultText = text; return this; }
    public InputDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public InputDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public InputDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }
    public InputDialogBuilder onOk(OnInputDialogListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogInputLayoutBinding binding = DialogInputLayoutBinding.inflate(LayoutInflater.from(context));
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

        if (TextUtils.isEmpty(title)) {
            binding.titleText.setVisibility(View.GONE);
        } else {
            binding.titleText.setText(title);
        }
        if (!TextUtils.isEmpty(hint)) binding.editText.setHint(hint);
        if (!TextUtils.isEmpty(defaultText)) binding.editText.setText(defaultText);

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
}
