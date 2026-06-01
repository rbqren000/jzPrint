package com.org.jzprinter.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.database.entity.StudentEntity;

import java.util.ArrayList;
import java.util.List;

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
    private OnStudentClickListener listener;
    private OnDownloadClickListener downloadListener;

    public interface OnStudentClickListener {
        void onStudentClick(StudentEntity student);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(StudentEntity student);
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public void setItems(List<ListItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isSection() ? TYPE_SECTION : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            return new SectionViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_student, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (item.isSection()) {
            ((SectionViewHolder) holder).tvSectionTitle.setText(item.sectionTitle);
        } else {
            StudentEntity student = item.student;
            ItemViewHolder vh = (ItemViewHolder) holder;

            vh.tvStudentName.setText(student.getStudentName());
            vh.tvClassName.setText(student.getClassName());

            if (student.isMaterialReady()) {
                vh.tvMaterialStatus.setText("素材已就绪");
                vh.tvMaterialStatus.setTextColor(
                    ContextCompat.getColor(vh.itemView.getContext(), R.color.status_success));
                vh.tvDownload.setVisibility(View.GONE);
            } else {
                vh.tvMaterialStatus.setText("素材未下载");
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
        return items.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;
        SectionViewHolder(View itemView) {
            super(itemView);
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
