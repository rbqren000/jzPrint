package com.org.jzprinter.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.network.model.Edition;
import com.org.jzprinter.network.model.Semester;

import java.util.ArrayList;
import java.util.List;

public class EditionAdapter extends RecyclerView.Adapter<EditionAdapter.ViewHolder> {

    private final List<ListItem> items = new ArrayList<>();
    private OnEditionClickListener listener;

    public interface OnEditionClickListener {
        void onEditionClick(Edition edition);
    }

    public void setOnEditionClickListener(OnEditionClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Semester> semesters) {
        items.clear();
        if (semesters != null) {
            for (Semester semester : semesters) {
                if (semester.editionList == null || semester.editionList.isEmpty()) {
                    continue;
                }
                items.add(ListItem.section(semester.semesterName));
                for (Edition edition : semester.editionList) {
                    items.add(ListItem.item(edition));
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SECTION
            ? R.layout.item_section_header
            : R.layout.item_edition;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (item.isSection) {
            holder.tvSectionTitle.setText(item.sectionTitle);
        } else {
            Edition edition = item.edition;
            holder.tvEditionName.setText(edition.editionName);
            String typeLabel;
            if (edition.supportsStudent() && edition.supportsPrepareCode()) {
                typeLabel = "学生 / 预铺码";
            } else if (edition.supportsPrepareCode()) {
                typeLabel = "预铺码";
            } else {
                typeLabel = "学生";
            }
            holder.tvEditionType.setText(typeLabel);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditionClick(edition);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isSection ? TYPE_SECTION : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ITEM = 1;

    static class ListItem {
        final boolean isSection;
        final String sectionTitle;
        final Edition edition;

        private ListItem(boolean isSection, String sectionTitle, Edition edition) {
            this.isSection = isSection;
            this.sectionTitle = sectionTitle;
            this.edition = edition;
        }

        static ListItem section(String title) {
            return new ListItem(true, title, null);
        }

        static ListItem item(Edition edition) {
            return new ListItem(false, null, edition);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;
        TextView tvEditionName;
        TextView tvEditionType;

        ViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_SECTION) {
                tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
            } else {
                tvEditionName = itemView.findViewById(R.id.tvEditionName);
                tvEditionType = itemView.findViewById(R.id.tvEditionType);
            }
        }
    }
}
