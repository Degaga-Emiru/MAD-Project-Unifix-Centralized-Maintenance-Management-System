package com.hawassa.unifix.student.adapters;
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

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<MaintenanceReport> reportList;

    public RecentActivityAdapter(List<MaintenanceReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceReport report = reportList.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvLocation, tvStatus, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }

        public void bind(MaintenanceReport report) {
            tvCategory.setText(report.getCategory());

            // Combine building block and room number for location
            String location = report.getBuildingBlock();
            if (report.getRoomNumber() != null && !report.getRoomNumber().isEmpty()) {
                location += ", Room " + report.getRoomNumber();
            }
            tvLocation.setText(location);

            tvStatus.setText("Status: " + report.getStatus());

            // Format timestamp to relative time
            tvDate.setText(getRelativeTime(report.getTimestamp()));

            // Set status color
            int statusColor = R.color.text_primary;
            switch (report.getStatus()) {
                case "Submitted":
                    statusColor = R.color.orange;
                    break;
                case "Assigned":
                case "In Progress":
                    statusColor = R.color.primary_blue;
                    break;
                case "Completed":
                    statusColor = R.color.green;
                    break;
            }
            tvStatus.setTextColor(itemView.getContext().getResources().getColor(statusColor));
        }

        private String getRelativeTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60000) { // Less than 1 minute
                return "Just now";
            } else if (diff < 3600000) { // Less than 1 hour
                long minutes = diff / 60000;
                return minutes + " min ago";
            } else if (diff < 86400000) { // Less than 1 day
                long hours = diff / 3600000;
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (diff < 604800000) { // Less than 1 week
                long days = diff / 86400000;
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
