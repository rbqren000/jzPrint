package com.org.jzprinter.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.databinding.ItemPageBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PageAdapter extends RecyclerView.Adapter<PageAdapter.ViewHolder> {

    private final List<Integer> pages = new ArrayList<>();
    private final List<Integer> printedPages = new ArrayList<>();
    private final Set<Integer> selectedPages;

    public PageAdapter(Set<Integer> selectedPages) {
        this.selectedPages = selectedPages;
    }

    public void setPages(List<Integer> allPages, List<Integer> printed) {
        pages.clear();
        printedPages.clear();
        if (allPages != null) pages.addAll(allPages);
        if (printed != null) printedPages.addAll(printed);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPageBinding binding = ItemPageBinding.inflate(
            android.view.LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int pageNum = pages.get(position);
        boolean isPrinted = printedPages.contains(pageNum);
        ItemPageBinding b = holder.binding;

        b.tvPageNumber.setText("page_" + pageNum);

        if (isPrinted) {
            b.tvPageStatus.setText("已完成");
            b.tvPageStatus.setTextColor(
                ContextCompat.getColor(b.getRoot().getContext(), R.color.status_success));
            b.cbPage.setEnabled(true);
            b.cbPage.setAlpha(1.0f);
            b.tvPageNumber.setAlpha(1.0f);
        } else {
            b.tvPageStatus.setText("待打印");
            b.tvPageStatus.setTextColor(
                ContextCompat.getColor(b.getRoot().getContext(), R.color.text_disabled));
            b.cbPage.setEnabled(false);
            b.cbPage.setAlpha(0.4f);
            b.tvPageNumber.setAlpha(0.5f);
        }

        b.cbPage.setOnCheckedChangeListener(null);
        if (isPrinted) {
            b.cbPage.setChecked(selectedPages.contains(pageNum));
        } else {
            b.cbPage.setChecked(false);
            selectedPages.remove(pageNum);
        }
        b.cbPage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPages.add(pageNum);
            } else {
                selectedPages.remove(pageNum);
            }
        });

        b.getRoot().setOnClickListener(v -> {
            if (isPrinted) {
                b.cbPage.setChecked(!b.cbPage.isChecked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPageBinding binding;

        ViewHolder(ItemPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
