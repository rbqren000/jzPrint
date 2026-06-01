package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;

import com.org.jzprinter.databinding.DialogSingleChoiceLayoutBinding;
import com.org.jzprinter.widget.CustomDialog.adapter.SingleChoiceAdapter;

import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

public class SingleChoiceDialogBuilder {

    private final Context context;
    private String title;
    private List<String> items = Collections.emptyList();
    private int defaultSelection = -1;
    private boolean cancelable = true;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;
    private boolean showCheckBox = false;
    private boolean checked = false;
    private String checkBoxText;
    private OnSingleChoiceDialogListener listener;

    public interface OnSingleChoiceDialogListener {
        void onOkClicked(Dialog dialog, int selectedPosition, String selectedItem);
        void onCancelClicked(Dialog dialog);
    }

    public SingleChoiceDialogBuilder(Context context) {
        this.context = context;
    }

    public SingleChoiceDialogBuilder title(String title) { this.title = title; return this; }
    public SingleChoiceDialogBuilder items(List<String> items) { this.items = items; return this; }
    public SingleChoiceDialogBuilder defaultSelection(int position) { this.defaultSelection = position; return this; }
    public SingleChoiceDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public SingleChoiceDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public SingleChoiceDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }
    public SingleChoiceDialogBuilder showCheckBox(boolean show) { this.showCheckBox = show; return this; }
    public SingleChoiceDialogBuilder checked(boolean checked) { this.checked = checked; return this; }
    public SingleChoiceDialogBuilder checkBoxText(String text) { this.checkBoxText = text; return this; }
    public SingleChoiceDialogBuilder onOk(OnSingleChoiceDialogListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogSingleChoiceLayoutBinding binding = DialogSingleChoiceLayoutBinding.inflate(LayoutInflater.from(context));
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
            binding.titleTextView.setVisibility(View.GONE);
        } else {
            binding.titleTextView.setText(title);
        }

        binding.checkBox.setVisibility(showCheckBox ? View.VISIBLE : View.GONE);
        binding.checkBox.setChecked(checked);
        if (!TextUtils.isEmpty(checkBoxText)) {
            binding.checkBox.setText(checkBoxText);
        }

        List<String> safeItems = items != null ? items : Collections.emptyList();
        SingleChoiceAdapter adapter = new SingleChoiceAdapter(safeItems, defaultSelection);
        binding.optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.optionsRecyclerView.setAdapter(adapter);

        binding.okButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOkClicked(dialog, adapter.getSelectedPosition(), adapter.getSelectedItem());
            }
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
