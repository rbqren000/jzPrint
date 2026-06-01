package com.org.jzprinter.widget.CustomDialog.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;

import java.util.List;

public class SingleChoiceAdapter extends RecyclerView.Adapter<SingleChoiceAdapter.ViewHolder> {

    private final List<String> items;
    private int selectedPosition;

    public SingleChoiceAdapter(List<String> items, int defaultSelection) {
        this.items = items;
        this.selectedPosition = defaultSelection;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_single_choice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(items.get(position));
        holder.radioButton.setChecked(position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public String getSelectedItem() {
        if (items != null && selectedPosition >= 0 && selectedPosition < items.size()) {
            return items.get(selectedPosition);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        RadioButton radioButton;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            // For simple_list_item_single_choice, the root is a CheckedTextView
            // We'll use a RadioButton approach instead
            if (itemView instanceof android.widget.CheckedTextView) {
                // no radioButton separate
                radioButton = null;
            } else {
                radioButton = null;
            }
        }
    }
}
