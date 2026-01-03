package com.hawassa.unifix.admin;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.admin.adapters.AdminReportAdapter;
import com.hawassa.unifix.models.MaintenanceReport;

import java.util.ArrayList;
import java.util.List;

public class AllReportsActivity extends AppCompatActivity
        implements AdminReportAdapter.OnAssignClickListener,
        AdminReportAdapter.OnDeleteClickListener,
        AdminReportAdapter.OnViewDetailsClickListener {

    private RecyclerView rvAllReports;
    private MaterialToolbar toolbar;
    private MaterialButtonToggleGroup filterToggleGroup;
    private DatabaseReference reportsRef;
    private AdminReportAdapter reportAdapter;
    private List<MaintenanceReport> reportList = new ArrayList<>();
    private List<MaintenanceReport> filteredList = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, ASSIGNED, IN_PROGRESS, COMPLETED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reports);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFirebase();
        loadAllReports();
        setupFilterButtons();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        rvAllReports = findViewById(R.id.rvAllReports);
        filterToggleGroup = findViewById(R.id.filterToggleGroup);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("All Reports");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        reportAdapter = new AdminReportAdapter(filteredList, this, this, this);
        rvAllReports.setLayoutManager(new LinearLayoutManager(this));
        rvAllReports.setAdapter(reportAdapter);
    }

    private void setupFirebase() {
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
    }

    private void setupFilterButtons() {
        // Set default selection
        filterToggleGroup.check(R.id.btnAll);

        filterToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnAll) {
                    currentFilter = "ALL";
                } else if (checkedId == R.id.btnAssigned) {
                    currentFilter = "ASSIGNED";
                } else if (checkedId == R.id.btnInProgress) {
                    currentFilter = "IN_PROGRESS";
                } else if (checkedId == R.id.btnCompleted) {
                    currentFilter = "COMPLETED";
                }
                applyFilter();
            }
        });
    }

    private void loadAllReports() {
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                    if (report != null) {
                        reportList.add(0, report); // newest first
                    }
                }
                applyFilter(); // Apply filter after loading
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AllReportsActivity.this,
                        "Failed to load reports: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        filteredList.clear();

        if (currentFilter.equals("ALL")) {
            filteredList.addAll(reportList);
        } else {
            for (MaintenanceReport report : reportList) {
                String status = report.getStatus() != null ? report.getStatus() : "";

                if (currentFilter.equals("ASSIGNED") &&
                        (status.equals("Assigned") || status.equals("Submitted"))) {
                    filteredList.add(report);
                } else if (currentFilter.equals("IN_PROGRESS") &&
                        (status.equals("In Progress") || status.equals("On Hold") ||
                                status.equals("Acknowledged"))) {
                    filteredList.add(report);
                } else if (currentFilter.equals("COMPLETED") &&
                        status.equals("Completed")) {
                    filteredList.add(report);
                }
            }
        }
        reportAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAssignClick(MaintenanceReport report) {
        // Only allow assignment for submitted reports
        if (!"Submitted".equals(report.getStatus())) {
            Toast.makeText(this, "Cannot assign this report. Status: " + report.getStatus(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TaskAssignmentActivity.class);
        intent.putExtra("reportId", report.getReportId());
        intent.putExtra("adminUid", getIntent().getStringExtra("adminUid"));
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(MaintenanceReport report) {
        // Show confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Report");
        builder.setMessage("Are you sure you want to delete this completed report?\n\n" +
                "Report ID: " + report.getReportId() + "\n" +
                "Category: " + report.getCategory() + "\n" +
                "Location: " + report.getBuildingBlock() + ", Room " + report.getRoomNumber() + "\n\n" +
                "This action cannot be undone.");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteReport(report);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteReport(MaintenanceReport report) {
        if (report.getReportId() != null) {
            reportsRef.child(report.getReportId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Report deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete report: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onViewDetailsClick(MaintenanceReport report) {
        // Navigate to report details activity
        // For now, show a dialog with details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Details")
                .setMessage(
                        "Report ID: " + report.getReportId() + "\n\n" +
                                "Reporter: " + report.getReporterName() + "\n" +
                                "Category: " + report.getCategory() + "\n" +
                                "Location: " + report.getBuildingBlock() + ", Room " + report.getRoomNumber() + "\n" +
                                "Status: " + report.getStatus() + "\n\n" +
                                "Description:\n" + report.getDescription() + "\n\n" +
                                (report.getAssignedTechnicianName() != null ?
                                        "Assigned to: " + report.getAssignedTechnicianName() + "\n" : "") +
                                (report.getTechnicianNotes() != null ?
                                        "Technician Notes: " + report.getTechnicianNotes() + "\n" : "") +
                                (report.getImageUrl() != null ? "Has Image: Yes" : "")
                )
                .setPositiveButton("OK", null)
                .show();
    }
}