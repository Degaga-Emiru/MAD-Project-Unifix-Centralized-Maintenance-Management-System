package com.hawassa.unifix.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {
    private List<MaintenanceReport> reportList;
    private OnAssignClickListener assignClickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnViewDetailsClickListener viewDetailsClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public interface OnAssignClickListener {
        void onAssignClick(MaintenanceReport report);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(MaintenanceReport report);
    }

    public interface OnViewDetailsClickListener {
        void onViewDetailsClick(MaintenanceReport report);
    }

    public AdminReportAdapter(List<MaintenanceReport> reportList,
                              OnAssignClickListener assignListener,
                              OnDeleteClickListener deleteListener,
                              OnViewDetailsClickListener viewDetailsListener) {
        this.reportList = reportList;
        this.assignClickListener = assignListener;
        this.deleteClickListener = deleteListener;
        this.viewDetailsClickListener = viewDetailsListener;
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

        holder.tvReportId.setText("REP-" + report.getReportId().substring(0, 8).toUpperCase());
        holder.tvReporterName.setText("By: " + report.getReporterName());
        holder.tvCategory.setText(report.getCategory());
        holder.tvLocation.setText(report.getBuildingBlock() + ", Room " + report.getRoomNumber());
        holder.tvDescription.setText(report.getDescription());
        holder.tvStatus.setText(report.getStatus());
        holder.tvTimestamp.setText(dateFormat.format(new Date(report.getTimestamp())));

        // Set status color
        int statusColor = getStatusColor(report.getStatus(), holder.itemView);
        holder.tvStatus.setTextColor(statusColor);

        // Show/hide buttons based on status
        if ("Submitted".equals(report.getStatus())) {
            holder.btnAssign.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnAssign.setOnClickListener(v -> {
                if (assignClickListener != null) {
                    assignClickListener.onAssignClick(report);
                }
            });
        } else if ("Completed".equals(report.getStatus())) {
            holder.btnAssign.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(report);
                }
            });
        } else {
            holder.btnAssign.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Show assigned technician if exists
        if (report.getAssignedTechnicianName() != null && !report.getAssignedTechnicianName().isEmpty()) {
            holder.tvAssignedTo.setText("Assigned to: " + report.getAssignedTechnicianName());
            holder.tvAssignedTo.setVisibility(View.VISIBLE);
        } else {
            holder.tvAssignedTo.setVisibility(View.GONE);
        }

        // Set click listener for the entire card to view details
        holder.itemView.setOnClickListener(v -> {
            if (viewDetailsClickListener != null) {
                viewDetailsClickListener.onViewDetailsClick(report);
            }
        });
    }

    private int getStatusColor(String status, View view) {
        switch (status) {
            case "Submitted":
                return view.getContext().getResources().getColor(R.color.status_submitted);
            case "Assigned":
                return view.getContext().getResources().getColor(R.color.status_assigned);
            case "In Progress":
                return view.getContext().getResources().getColor(R.color.status_in_progress);
            case "Completed":
                return view.getContext().getResources().getColor(R.color.status_completed);
            case "On Hold":
                return view.getContext().getResources().getColor(R.color.orange);
            default:
                return view.getContext().getResources().getColor(R.color.text_primary);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public void updateData(List<MaintenanceReport> newList) {
        reportList.clear();
        reportList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportId, tvReporterName, tvCategory, tvLocation, tvDescription;
        TextView tvStatus, tvTimestamp, tvAssignedTo;
        Button btnAssign;
        ImageButton btnDelete; // Change to ImageButton

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportId = itemView.findViewById(R.id.tvReportId);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            btnAssign = itemView.findViewById(R.id.btnAssign);
            btnDelete = itemView.findViewById(R.id.btnDelete); // Now ImageButton
        }
    }
}