package com.org.jzprinter.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;

import java.util.ArrayList;
import java.util.List;

public class PageSelectAdapter extends RecyclerView.Adapter<PageSelectAdapter.ViewHolder> {

    public static class PageItem {
        public final int pageCode;
        public final int codeCount;
        public boolean selected;

        public PageItem(int pageCode, int codeCount, boolean selected) {
            this.pageCode = pageCode;
            this.codeCount = codeCount;
            this.selected = selected;
        }
    }

    private final List<PageItem> items = new ArrayList<>();
    private OnPageClickListener listener;

    public interface OnPageClickListener {
        void onPageClick(PageItem item, int position);
        void onPageCheckChanged(PageItem item, int position, boolean checked);
    }

    public void setOnPageClickListener(OnPageClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PageItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        notifyDataSetChanged();
    }

    public List<PageItem> getItems() {
        return items;
    }

    public List<Integer> getSelectedPageCodes() {
        List<Integer> selected = new ArrayList<>();
        for (PageItem item : items) {
            if (item.selected) selected.add(item.pageCode);
        }
        return selected;
    }

    public void selectAll() {
        for (PageItem item : items) item.selected = true;
        notifyDataSetChanged();
    }

    public void selectOdd() {
        for (PageItem item : items) item.selected = (item.pageCode % 2 == 1);
        notifyDataSetChanged();
    }

    public void selectEven() {
        for (PageItem item : items) item.selected = (item.pageCode % 2 == 0);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_page_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PageItem item = items.get(position);
        holder.tvPageCode.setText("page_" + item.pageCode);
        holder.tvCodeCount.setText(item.codeCount + "码");
        holder.cbPage.setOnCheckedChangeListener(null);
        holder.cbPage.setChecked(item.selected);
        holder.cbPage.setOnCheckedChangeListener((btn, checked) -> {
            item.selected = checked;
            if (listener != null) listener.onPageCheckChanged(item, holder.getAdapterPosition(), checked);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPageClick(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPage;
        TextView tvPageCode;
        TextView tvCodeCount;

        ViewHolder(View itemView) {
            super(itemView);
            cbPage = itemView.findViewById(R.id.cbPage);
            tvPageCode = itemView.findViewById(R.id.tvPageCode);
            tvCodeCount = itemView.findViewById(R.id.tvCodeCount);
        }
    }
}
