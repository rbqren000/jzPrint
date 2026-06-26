package com.org.jzprinter.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.database.entity.StudentEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ITEM = 1;

    public static class ListItem {
        public final String sectionTitle;
        public final StudentEntity student;

        private ListItem(String sectionTitle, StudentEntity student) {
            this.sectionTitle = sectionTitle;
            this.student = student;
        }

        public static ListItem section(String title) {
            return new ListItem(title, null);
        }

        public static ListItem item(StudentEntity student) {
            return new ListItem(null, student);
        }

        public boolean isSection() {
            return sectionTitle != null;
        }
    }

    private final List<ListItem> items = new ArrayList<>();
    private final List<ListItem> displayItems = new ArrayList<>();
    private final Set<String> collapsedClasses = new HashSet<>();
    private final Map<String, Integer> classStudentCount = new HashMap<>();
    private OnStudentClickListener listener;
    private OnDownloadClickListener downloadListener;
    private OnClassClickListener classClickListener;

    public interface OnStudentClickListener {
        void onStudentClick(StudentEntity student);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(StudentEntity student);
    }

    public interface OnClassClickListener {
        void onClassClick(String className);
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public void setOnClassClickListener(OnClassClickListener listener) {
        this.classClickListener = listener;
    }

    public void setItems(List<ListItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        calculateClassStudentCount();
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public boolean isClassCollapsed(String className) {
        return collapsedClasses.contains(className);
    }

    public void toggleClass(String className) {
        boolean wasCollapsed = collapsedClasses.contains(className);
        if (wasCollapsed) {
            collapsedClasses.remove(className);
        } else {
            collapsedClasses.add(className);
        }

        int sectionPosition = -1;
        for (int i = 0; i < displayItems.size(); i++) {
            if (displayItems.get(i).isSection()
                && className.equals(displayItems.get(i).sectionTitle)) {
                sectionPosition = i;
                break;
            }
        }

        if (sectionPosition >= 0) {
            int count = classStudentCount.getOrDefault(className, 0);
            if (wasCollapsed) {
                rebuildDisplayItems();
                notifyItemRangeInserted(sectionPosition + 1, count);
            } else {
                rebuildDisplayItems();
                notifyItemRangeRemoved(sectionPosition + 1, count);
            }
            notifyItemChanged(sectionPosition);
        }
    }

    private void calculateClassStudentCount() {
        classStudentCount.clear();
        String currentClass = null;
        int count = 0;
        for (ListItem item : items) {
            if (item.isSection()) {
                if (currentClass != null) {
                    classStudentCount.put(currentClass, count);
                }
                currentClass = item.sectionTitle;
                count = 0;
            } else {
                count++;
            }
        }
        if (currentClass != null) {
            classStudentCount.put(currentClass, count);
        }
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        String currentClass = null;
        boolean skip = false;
        for (ListItem item : items) {
            if (item.isSection()) {
                currentClass = item.sectionTitle;
                skip = collapsedClasses.contains(currentClass);
                displayItems.add(item);
            } else if (!skip) {
                displayItems.add(item);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).isSection() ? TYPE_SECTION : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            SectionViewHolder vh = new SectionViewHolder(view);
            vh.itemView.setOnClickListener(v -> {
                int pos = vh.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && classClickListener != null) {
                    ListItem item = displayItems.get(pos);
                    if (item.isSection()) {
                        classClickListener.onClassClick(item.sectionTitle);
                    }
                }
            });
            return vh;
        }
        View view = inflater.inflate(R.layout.item_student, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = displayItems.get(position);
        if (item.isSection()) {
            SectionViewHolder vh = (SectionViewHolder) holder;
            String className = item.sectionTitle;
            int count = classStudentCount.getOrDefault(className, 0);
            vh.tvSectionTitle.setText(className + " (" + count + ")");
            boolean collapsed = collapsedClasses.contains(className);
            vh.ivCollapseIcon.setRotation(collapsed ? 0f : 180f);
        } else {
            StudentEntity student = item.student;
            ItemViewHolder vh = (ItemViewHolder) holder;

            vh.tvStudentName.setText(student.getStudentName());
            vh.tvClassName.setText(student.getClassName());

            if (student.isMaterialReady()) {
                vh.tvMaterialStatus.setText(R.string.material_ready);
                vh.tvMaterialStatus.setTextColor(
                    ContextCompat.getColor(vh.itemView.getContext(), R.color.status_success));
                vh.tvDownload.setVisibility(View.GONE);
            } else {
                vh.tvMaterialStatus.setText(R.string.material_not_downloaded);
                vh.tvMaterialStatus.setTextColor(
                    ContextCompat.getColor(vh.itemView.getContext(), R.color.status_error));
                vh.tvDownload.setVisibility(View.VISIBLE);
            }

            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStudentClick(student);
            });
            vh.tvDownload.setOnClickListener(v -> {
                if (downloadListener != null) downloadListener.onDownloadClick(student);
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCollapseIcon;
        TextView tvSectionTitle;
        SectionViewHolder(View itemView) {
            super(itemView);
            ivCollapseIcon = itemView.findViewById(R.id.ivCollapseIcon);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvClassName;
        TextView tvMaterialStatus;
        TextView tvDownload;
        ItemViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvMaterialStatus = itemView.findViewById(R.id.tvMaterialStatus);
            tvDownload = itemView.findViewById(R.id.tvDownload);
        }
    }
}
