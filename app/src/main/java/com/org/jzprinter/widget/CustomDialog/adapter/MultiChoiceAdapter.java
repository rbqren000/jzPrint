package com.org.jzprinter.widget.CustomDialog.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiChoiceAdapter extends RecyclerView.Adapter<MultiChoiceAdapter.ViewHolder> {

    private final List<String> items;
    private final Set<Integer> selectedIndices;

    public MultiChoiceAdapter(List<String> items, Set<Integer> defaultSelectedIndices) {
        this.items = items != null ? items : new ArrayList<>();
        this.selectedIndices = defaultSelectedIndices != null ? new HashSet<>(defaultSelectedIndices) : new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.checkBox.setText(items.get(position));
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIndices.contains(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (isChecked) {
                selectedIndices.add(pos);
            } else {
                selectedIndices.remove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Set<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    public List<String> getSelectedItems() {
        List<String> result = new ArrayList<>();
        for (int index : selectedIndices) {
            if (index >= 0 && index < items.size()) {
                result.add(items.get(index));
            }
        }
        return result;
    }

    public void selectAll() {
        selectedIndices.clear();
        for (int i = 0; i < items.size(); i++) {
            selectedIndices.add(i);
        }
        notifyDataSetChanged();
    }

    public void clearAll() {
        selectedIndices.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            if (itemView instanceof CheckBox) {
                checkBox = (CheckBox) itemView;
            } else {
                checkBox = null;
            }
        }
    }
}
