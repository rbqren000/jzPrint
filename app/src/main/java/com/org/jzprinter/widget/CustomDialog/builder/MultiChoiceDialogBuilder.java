package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.org.jzprinter.databinding.DialogMultiChoiceLayoutBinding;
import com.org.jzprinter.widget.CustomDialog.adapter.MultiChoiceAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.widget.LinearLayoutManager;

public class MultiChoiceDialogBuilder {

    private final Context context;
    private String title;
    private List<String> items = Collections.emptyList();
    private Set<Integer> defaultSelectedIndices;
    private boolean cancelable = true;
    private int dialogGravity = Gravity.CENTER;
    private float dialogWidth = 0.85f;
    private boolean showSelectAllClear = false;
    private boolean showCheckBox = false;
    private boolean checked = false;
    private String checkBoxText;
    private OnMultiChoiceDialogListener listener;

    public interface OnMultiChoiceDialogListener {
        void onOkClicked(Dialog dialog, List<Integer> selectedPositions, List<String> selectedItems);
        void onCancelClicked(Dialog dialog);
    }

    public MultiChoiceDialogBuilder(Context context) {
        this.context = context;
    }

    public MultiChoiceDialogBuilder title(String title) { this.title = title; return this; }
    public MultiChoiceDialogBuilder items(List<String> items) { this.items = items; return this; }
    public MultiChoiceDialogBuilder defaultSelectedIndices(Set<Integer> indices) { this.defaultSelectedIndices = indices; return this; }
    public MultiChoiceDialogBuilder cancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
    public MultiChoiceDialogBuilder dialogGravity(int gravity) { this.dialogGravity = gravity; return this; }
    public MultiChoiceDialogBuilder dialogWidth(float width) { this.dialogWidth = width; return this; }
    public MultiChoiceDialogBuilder showSelectAllClear(boolean show) { this.showSelectAllClear = show; return this; }
    public MultiChoiceDialogBuilder showCheckBox(boolean show) { this.showCheckBox = show; return this; }
    public MultiChoiceDialogBuilder checked(boolean checked) { this.checked = checked; return this; }
    public MultiChoiceDialogBuilder checkBoxText(String text) { this.checkBoxText = text; return this; }
    public MultiChoiceDialogBuilder onOk(OnMultiChoiceDialogListener listener) { this.listener = listener; return this; }

    public Dialog show() {
        Dialog dialog = new Dialog(context);
        DialogMultiChoiceLayoutBinding binding = DialogMultiChoiceLayoutBinding.inflate(LayoutInflater.from(context));
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

        binding.actionContainer.setVisibility(showSelectAllClear ? View.VISIBLE : View.GONE);

        MultiChoiceAdapter adapter = new MultiChoiceAdapter(items, defaultSelectedIndices);
        binding.optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.optionsRecyclerView.setAdapter(adapter);

        binding.selectAllButton.setOnClickListener(v -> adapter.selectAll());
        binding.clearButton.setOnClickListener(v -> adapter.clearAll());

        binding.okButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOkClicked(dialog, new ArrayList<>(adapter.getSelectedIndices()), adapter.getSelectedItems());
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
