package com.org.jzprinter.ui.adapter;

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

public class PrepareCodeAdapter extends RecyclerView.Adapter<PrepareCodeAdapter.ViewHolder> {

    private final List<String> items = new ArrayList<>();
    private final List<Boolean> readyStates = new ArrayList<>();
    private OnPrepareCodeClickListener listener;
    private OnDownloadClickListener downloadListener;

    public interface OnPrepareCodeClickListener {
        void onPrepareCodeClick(String prepareCode, boolean materialReady);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(String prepareCode);
    }

    public void setOnPrepareCodeClickListener(OnPrepareCodeClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public void setItems(List<String> codes, List<Boolean> readyStates) {
        this.items.clear();
        this.readyStates.clear();
        if (codes != null) this.items.addAll(codes);
        if (readyStates != null) this.readyStates.addAll(readyStates);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prepare_code, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String code = items.get(position);
        holder.tvPrepareCode.setText(code);

        boolean ready = position < readyStates.size() && readyStates.get(position);
        if (ready) {
            holder.tvMaterialStatus.setText("素材已就绪");
            holder.tvMaterialStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.status_success));
            holder.tvDownload.setVisibility(View.GONE);
        } else {
            holder.tvMaterialStatus.setText("素材未下载");
            holder.tvMaterialStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.status_error));
            holder.tvDownload.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrepareCodeClick(code, ready);
            }
        });
        holder.tvDownload.setOnClickListener(v -> {
            if (downloadListener != null) {
                downloadListener.onDownloadClick(code);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrepareCode;
        TextView tvMaterialStatus;
        TextView tvDownload;
        ViewHolder(View itemView) {
            super(itemView);
            tvPrepareCode = itemView.findViewById(R.id.tvPrepareCode);
            tvMaterialStatus = itemView.findViewById(R.id.tvMaterialStatus);
            tvDownload = itemView.findViewById(R.id.tvDownload);
        }
    }
}
