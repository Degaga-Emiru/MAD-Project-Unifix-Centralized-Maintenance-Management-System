package com.hawassa.unifix.technician.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<MaintenanceReport> taskList;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(MaintenanceReport task);
        void onUpdateStatusClick(MaintenanceReport task);
    }

    public TaskAdapter(List<MaintenanceReport> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceReport task = taskList.get(position);

        holder.tvCategory.setText(task.getCategory());
        holder.tvLocation.setText(task.getBuildingBlock() + ", Room " + task.getRoomNumber());
        holder.tvStatus.setText(task.getStatus());
        holder.tvReporter.setText("Reported by: " +
                (task.getReporterName() != null ? task.getReporterName() : "Unknown"));

        // Format date
        if (task.getTimestamp() > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(task.getTimestamp())));
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        holder.btnUpdateStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateStatusClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvLocation, tvStatus, tvReporter, tvDate;
        Button btnUpdateStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReporter = itemView.findViewById(R.id.tvReporter);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}