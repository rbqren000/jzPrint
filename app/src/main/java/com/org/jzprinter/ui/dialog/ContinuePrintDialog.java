package com.org.jzprinter.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.org.jzprinter.R;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.TaskStatus;

import java.util.List;

public class ContinuePrintDialog extends Dialog {

    private final PrintTaskEntity task;
    private OnContinueListener listener;

    public interface OnContinueListener {
        void onContinue(PrintTaskEntity task);
        void onRestart(PrintTaskEntity task);
        void onCancel(PrintTaskEntity task);
    }

    public ContinuePrintDialog(@NonNull Context context, PrintTaskEntity task) {
        super(context);
        this.task = task;
    }

    public void setOnContinueListener(OnContinueListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_continue_print);

        TextView tvMessage = findViewById(R.id.tvMessage);
        Button btnContinue = findViewById(R.id.btnContinue);
        Button btnRestart = findViewById(R.id.btnRestart);
        Button btnCancel = findViewById(R.id.btnCancel);

        PrintMode printMode = PrintMode.fromCode(task.getPrintMode());
        List<Integer> printed = com.org.jzprinter.database.converter.IntegerListConverter
            .fromString(task.getPrintedPages());
        List<Integer> target = com.org.jzprinter.database.converter.IntegerListConverter
            .fromString(task.getTargetPages());

        tvMessage.setText(String.format("%s，已打印 %d/%d 页",
            printMode.getLabel(), printed.size(), target.size()));

        btnContinue.setOnClickListener(v -> {
            if (listener != null) listener.onContinue(task);
            dismiss();
        });

        btnRestart.setOnClickListener(v -> {
            if (listener != null) listener.onRestart(task);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(task);
            dismiss();
        });
    }
}
