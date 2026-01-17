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

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private List<MaintenanceReport> reportList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    public ReportAdapter(List<MaintenanceReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceReport report = reportList.get(position);

        holder.tvReportId.setText("Report #" + report.getReportId().substring(0, 8));
        holder.tvCategory.setText(report.getCategory());
        holder.tvLocation.setText(report.getBuildingBlock() + " - Room " + report.getRoomNumber());
        holder.tvDescription.setText(report.getDescription());
        holder.tvStatus.setText(report.getStatus());
        holder.tvTimestamp.setText(dateFormat.format(new Date(report.getTimestamp())));

        // Set status background color
        int statusColor = getStatusColor(report.getStatus(), holder.itemView);
        holder.tvStatus.setBackgroundColor(statusColor);
    }

    private int getStatusColor(String status, View contextView) {
        switch (status) {
            case "Submitted":
                return contextView.getContext()
                        .getResources().getColor(R.color.status_submitted);
            case "Assigned":
                return contextView.getContext()
                        .getResources().getColor(R.color.status_assigned);
            case "In Progress":
                return contextView.getContext()
                        .getResources().getColor(R.color.status_in_progress);
            case "Completed":
                return contextView.getContext()
                        .getResources().getColor(R.color.status_completed);
            default:
                return contextView.getContext()
                        .getResources().getColor(R.color.status_submitted);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportId, tvCategory, tvLocation, tvDescription, tvStatus, tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportId = itemView.findViewById(R.id.tvReportId);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
