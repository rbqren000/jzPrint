package com.org.jzprinter.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印进度页码 Chip 适配器。
 * 每个页码有四种状态：已打印（绿）、即将打印（蓝）、待打印（灰）、重打中（橙）。
 */
public class PageChipAdapter extends RecyclerView.Adapter<PageChipAdapter.ViewHolder> {

    public enum State { PRINTED, NEXT, PENDING, REPRINTING }

    public static class ChipItem {
        public final int pageNumber;
        public State state = State.PENDING;

        public ChipItem(int pageNumber) {
            this.pageNumber = pageNumber;
        }
    }

    private final List<ChipItem> items = new ArrayList<>();
    private final int colorPrinted;
    private final int colorNext;
    private final int colorNextStroke;
    private final int colorPending;
    private final int colorPendingText;
    private final int colorReprinting;
    private final float density;

    public PageChipAdapter(ViewGroup parent) {
        colorPrinted = ContextCompat.getColor(parent.getContext(), R.color.status_success);
        colorNext = ContextCompat.getColor(parent.getContext(), R.color.accent_blue);
        colorNextStroke = ContextCompat.getColor(parent.getContext(), R.color.primary_blue);
        colorPending = ContextCompat.getColor(parent.getContext(), R.color.grey300);
        colorPendingText = ContextCompat.getColor(parent.getContext(), R.color.text_secondary);
        colorReprinting = ContextCompat.getColor(parent.getContext(), R.color.md_orange_500);
        density = parent.getContext().getResources().getDisplayMetrics().density;
    }

    public void setItems(List<ChipItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_page_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChipItem item = items.get(position);
        holder.tvChip.setText(String.valueOf(item.pageNumber));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(density * 8);

        switch (item.state) {
            case PRINTED:
                bg.setColor(colorPrinted);
                holder.tvChip.setBackground(bg);
                holder.tvChip.setTextColor(0xFFFFFFFF);
                break;
            case NEXT:
                bg.setColor(colorNext);
                holder.tvChip.setBackground(bg);
                holder.tvChip.setTextColor(0xFFFFFFFF);
                break;
            case REPRINTING:
                bg.setColor(colorReprinting);
                holder.tvChip.setBackground(bg);
                holder.tvChip.setTextColor(0xFFFFFFFF);
                break;
            case PENDING:
            default:
                bg.setColor(colorPending);
                holder.tvChip.setBackground(bg);
                holder.tvChip.setTextColor(colorPendingText);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvChip;

        ViewHolder(View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tvChip);
        }
    }
}
