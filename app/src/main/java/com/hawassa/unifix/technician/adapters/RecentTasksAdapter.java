package com.hawassa.unifix.technician.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTasksAdapter extends RecyclerView.Adapter<RecentTasksAdapter.ViewHolder> {

    private List<MaintenanceReport> taskList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public RecentTasksAdapter(List<MaintenanceReport> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceReport task = taskList.get(position);

        // Set category and location using your model's fields
        holder.tvCategory.setText(task.getCategory());

        // Create location string from buildingBlock and roomNumber
        String location = task.getBuildingBlock() + ", Room " + task.getRoomNumber();
        holder.tvLocation.setText(location);

        // Set status
        holder.tvStatus.setText(task.getStatus());

        // Set status color
        switch (task.getStatus()) {
            case "In Progress":
                holder.tvStatus.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(R.color.status_in_progress));
                break;
            case "Completed":
                holder.tvStatus.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(R.color.status_completed));
                break;
            case "Assigned":
            case "Submitted": // Your model might use "Submitted" instead of "Assigned"
                holder.tvStatus.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(R.color.status_pending));
                break;
        }

        // Format date from timestamp
        if (task.getTimestamp() > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(task.getTimestamp())));
        } else {
            holder.tvDate.setText("Recent");
        }

        // Optional: Show reporter name if available
        if (task.getReporterName() != null && !task.getReporterName().isEmpty()) {
            holder.tvReporter.setText("Reported by: " + task.getReporterName());
            holder.tvReporter.setVisibility(View.VISIBLE);
        } else {
            holder.tvReporter.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvLocation, tvStatus, tvDate, tvReporter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReporter = itemView.findViewById(R.id.tvReporter); // Optional
        }
    }
}