package com.org.jzprinter.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.databinding.ItemTaskCardBinding;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.ViewHolder> {

    private final List<PrintTaskEntity> tasks = new ArrayList<>();
    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onContinue(PrintTaskEntity task);
        void onViewDetail(PrintTaskEntity task);
        void onCancel(PrintTaskEntity task);
        void onDelete(PrintTaskEntity task);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<PrintTaskEntity> tasks) {
        this.tasks.clear();
        if (tasks != null) {
            this.tasks.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskCardBinding binding = ItemTaskCardBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrintTaskEntity task = tasks.get(position);
        ItemTaskCardBinding b = holder.binding;

        b.tvStudentName.setText(task.getTargetName() != null ? task.getTargetName() : task.getTargetId());
        String editionDisplay = task.getEditionName() != null && !task.getEditionName().isEmpty()
            ? task.getEditionName() : task.getEditionId();
        b.tvEditionName.setText(editionDisplay);

        Context ctx = b.getRoot().getContext();
        PrintMode mode = PrintMode.fromCode(task.getPrintMode());
        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        b.tvPrintMode.setText(mode.getLabel(ctx));

        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        int total = target.size();
        int done = 0;
        for (int p : printed) {
            if (target.contains(p)) done++;
        }
        int percent = total > 0 ? (done * 100 / total) : 0;

        b.pbProgress.setProgress(percent);

        String statusLabel;
        int statusColor;
        switch (status) {
            case COMPLETED:
                statusLabel = ctx.getString(R.string.task_status_completed);
                statusColor = ContextCompat.getColor(ctx, R.color.status_success);
                break;
            case CANCELLED:
                statusLabel = ctx.getString(R.string.task_status_cancelled);
                statusColor = ContextCompat.getColor(ctx, R.color.text_disabled);
                break;
            default:
                statusLabel = status.getLabel(ctx);
                statusColor = ContextCompat.getColor(ctx, R.color.text_secondary);
                break;
        }
        b.tvProgressText.setText(ctx.getString(R.string.task_card_progress, done, total, statusLabel));
        b.tvProgressText.setTextColor(statusColor);

        long elapsedMs = System.currentTimeMillis() - task.getUpdatedAt();
        b.tvTimeAgo.setText(formatTimeAgo(ctx, elapsedMs));

        boolean resumable = TaskStatus.isResumable(task.getStatus());
        b.btnContinue.setVisibility(resumable ? View.VISIBLE : View.GONE);
        b.btnContinue.setOnClickListener(v -> {
            if (listener != null) listener.onContinue(task);
        });

        boolean showViewDetail = true;
        b.btnViewDetail.setVisibility(showViewDetail ? View.VISIBLE : View.GONE);
        b.btnViewDetail.setOnClickListener(v -> {
            if (listener != null) listener.onViewDetail(task);
        });

        b.btnCancelTask.setVisibility(resumable ? View.VISIBLE : View.GONE);
        b.btnCancelTask.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(task);
        });

        b.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(task);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private static String formatTimeAgo(Context ctx, long elapsedMs) {
        long minutes = elapsedMs / 60000;
        if (minutes < 1) return ctx.getString(R.string.time_just_now);
        if (minutes < 60) return ctx.getString(R.string.time_minutes_ago, minutes);
        long hours = minutes / 60;
        if (hours < 24) return ctx.getString(R.string.time_hours_ago, hours);
        long days = hours / 24;
        return ctx.getString(R.string.time_days_ago, days);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTaskCardBinding binding;

        ViewHolder(ItemTaskCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
