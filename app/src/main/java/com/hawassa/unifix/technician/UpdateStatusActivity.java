package com.hawassa.unifix.technician;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.shared.utils.NotificationUtils;
import java.text.SimpleDateFormat;
import java.util.*;

public class UpdateStatusActivity extends AppCompatActivity {
    private TextView tvReportTitle, tvLocation, tvDescription, tvReporter, tvReportDate, tvCurrentStatus;
    private RadioGroup rgStatus;
    private EditText etNotes, etEstimatedCompletion, etRequiredTools;
    private Button btnUpdateStatus, btnCancel, btnNavigate;  // Added btnNavigate
    private DatabaseReference reportsRef;
    private String reportId, technicianId, technicianName, customUserId;
    private MaintenanceReport currentReport;

    // Radio buttons
    private RadioButton rbAcknowledged, rbInProgress, rbOnHold, rbCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_status);

        // Get data from intent
        reportId = getIntent().getStringExtra("reportId");
        technicianId = getIntent().getStringExtra("technicianId");
        technicianName = getIntent().getStringExtra("technicianName");
        customUserId = getIntent().getStringExtra("customUserId");

        // Debug log
        Log.d("UPDATE_STATUS", "Received - reportId: " + reportId +
                ", technicianId: " + technicianId +
                ", technicianName: " + technicianName);

        // CRITICAL FIX: If coming from dashboard, we need to get reportId from somewhere else
        // Let's check if reportId is null and load the first assigned report
        if (reportId == null && technicianId != null) {
            // We need to get a report assigned to this technician
            loadFirstAssignedReport();
            return;
        }

        if (reportId == null || technicianId == null) {
            Toast.makeText(this, "Missing required information. Please select a task first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // If technicianName is not provided in intent, try to get it
        if (technicianName == null || technicianName.isEmpty()) {
            technicianName = getIntent().getStringExtra("userName");
            if (technicianName == null) {
                technicianName = "Technician";
            }
        }

        initializeViews();
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");

        loadReportDetails();
        setupClickListeners();
    }

    private void loadFirstAssignedReport() {
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");

        reportsRef.orderByChild("assignedTechnicianId").equalTo(technicianId)
                .limitToFirst(1) // Get first assigned report
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                reportId = snapshot.getKey();
                                Log.d("UPDATE_STATUS", "Found assigned report: " + reportId);

                                // Now initialize with this report
                                initializeViews();
                                reportsRef = FirebaseDatabase.getInstance().getReference("reports");
                                loadReportDetails();
                                setupClickListeners();
                                return;
                            }
                        }

                        // No assigned reports found
                        Toast.makeText(UpdateStatusActivity.this,
                                "No tasks assigned to you yet.", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(UpdateStatusActivity.this,
                                "Failed to load tasks: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void initializeViews() {
        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvDescription = findViewById(R.id.tvDescription);
        tvReporter = findViewById(R.id.tvReporter);
        tvReportDate = findViewById(R.id.tvReportDate);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        rgStatus = findViewById(R.id.rgStatus);
        etNotes = findViewById(R.id.etNotes);
        etEstimatedCompletion = findViewById(R.id.etEstimatedCompletion);
        etRequiredTools = findViewById(R.id.etRequiredTools);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnCancel = findViewById(R.id.btnCancel);
        btnNavigate = findViewById(R.id.btnNavigate);  // Initialize navigation button

        // Initialize radio buttons
        rbAcknowledged = findViewById(R.id.rbAcknowledged);
        rbInProgress = findViewById(R.id.rbInProgress);
        rbOnHold = findViewById(R.id.rbOnHold);
        rbCompleted = findViewById(R.id.rbCompleted);

        // Set current date as default estimated completion
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etEstimatedCompletion.setText(sdf.format(new Date()));

        // Disable fields initially
        etEstimatedCompletion.setEnabled(false);
        etRequiredTools.setEnabled(false);
    }

    private void loadReportDetails() {
        if (reportId == null) {
            Toast.makeText(this, "No report selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reportsRef.child(reportId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentReport = dataSnapshot.getValue(MaintenanceReport.class);
                    if (currentReport != null) {
                        // Verify this report is assigned to the current technician
                        if (!technicianId.equals(currentReport.getAssignedTechnicianId())) {
                            Toast.makeText(UpdateStatusActivity.this,
                                    "This task is not assigned to you", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        displayReportDetails(currentReport);
                        preSelectStatus(currentReport.getStatus());

                        // Setup navigation button if location exists
                        setupNavigationButton();
                    }
                } else {
                    Toast.makeText(UpdateStatusActivity.this,
                            "Report not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(UpdateStatusActivity.this,
                        "Failed to load report: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReportDetails(MaintenanceReport report) {
        tvReportTitle.setText(report.getCategory() + " - " + report.getBuildingBlock());

        // Show location with coordinates if available
        String locationText = "Location: " + report.getBuildingBlock() + ", Room " + report.getRoomNumber();
        if (report.getReportLatitude() != null && report.getReportLongitude() != null) {
            locationText += "\nCoordinates: " +
                    String.format("%.6f", report.getReportLatitude()) + ", " +
                    String.format("%.6f", report.getReportLongitude());
        }
        tvLocation.setText(locationText);

        tvDescription.setText(report.getDescription());

        if (report.getReporterName() != null && !report.getReporterName().isEmpty()) {
            tvReporter.setText("Reported by: " + report.getReporterName());
        } else {
            tvReporter.setText("Reported by: Unknown");
        }

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(report.getTimestamp()));
        tvReportDate.setText("Submitted: " + dateStr);

        tvCurrentStatus.setText("Current Status: " + report.getStatus());

        // Set status color
        int statusColor = getStatusColor(report.getStatus());
        tvCurrentStatus.setTextColor(statusColor);

        // Pre-fill notes if exists
        if (report.getTechnicianNotes() != null && !report.getTechnicianNotes().isEmpty()) {
            etNotes.setText(report.getTechnicianNotes());
        }

        // Pre-fill technician name if not already set
        if (technicianName == null || technicianName.isEmpty()) {
            if (report.getAssignedTechnicianName() != null && !report.getAssignedTechnicianName().isEmpty()) {
                technicianName = report.getAssignedTechnicianName();
            } else {
                technicianName = "Technician";
            }
        }
    }

    private void setupNavigationButton() {
        if (currentReport != null &&
                currentReport.getReportLatitude() != null &&
                currentReport.getReportLongitude() != null) {

            // Show the navigation button
            btnNavigate.setVisibility(View.VISIBLE);

            btnNavigate.setOnClickListener(v -> {
                Double latitude = currentReport.getReportLatitude();
                Double longitude = currentReport.getReportLongitude();

                // Create Google Maps navigation URI
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);

                // Create intent to launch Google Maps
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");  // Force open with Google Maps

                // Check if Google Maps is installed
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                    Log.d("NAVIGATION", "Launching Google Maps to: " + latitude + ", " + longitude);
                } else {
                    // Google Maps not installed, show alternatives
                    showNavigationAlternatives(latitude, longitude);
                }
            });

        } else {
            // Hide navigation button if no location data
            btnNavigate.setVisibility(View.GONE);
            Log.d("NAVIGATION", "No location data available for navigation");
        }
    }

    private void showNavigationAlternatives(Double latitude, Double longitude) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Google Maps Not Found");
        builder.setMessage("The Google Maps app is not installed. Choose an alternative:");

        builder.setPositiveButton("Install Google Maps", (dialog, which) -> {
            // Open Play Store to install Google Maps
            try {
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                playStoreIntent.setData(Uri.parse("market://details?id=com.google.android.apps.maps"));
                startActivity(playStoreIntent);
            } catch (Exception e) {
                // If Play Store fails, open browser to Play Store website
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps"));
                startActivity(browserIntent);
            }
        });

        builder.setNegativeButton("Open in Browser", (dialog, which) -> {
            // Open Google Maps in browser
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                    latitude + "," + longitude + "&travelmode=driving");
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            startActivity(webIntent);
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void preSelectStatus(String currentStatus) {
        switch (currentStatus) {
            case "Acknowledged":
                rbAcknowledged.setChecked(true);
                break;
            case "In Progress":
                rbInProgress.setChecked(true);
                etEstimatedCompletion.setEnabled(true);
                etRequiredTools.setEnabled(true);
                break;
            case "On Hold":
                rbOnHold.setChecked(true);
                etEstimatedCompletion.setEnabled(true);
                etRequiredTools.setEnabled(true);
                break;
            case "Completed":
                rbCompleted.setChecked(true);
                break;
            case "Assigned":
            case "Submitted":
                // No radio button selected for initial statuses
                break;
            default:
                Log.d("STATUS", "Unknown status: " + currentStatus);
                break;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Submitted":
                return getResources().getColor(R.color.status_submitted);
            case "Acknowledged":
                return getResources().getColor(R.color.primary_blue);
            case "Assigned":
                return getResources().getColor(R.color.status_assigned);
            case "In Progress":
                return getResources().getColor(R.color.status_in_progress);
            case "On Hold":
                return getResources().getColor(R.color.orange);
            case "Completed":
                return getResources().getColor(R.color.status_completed);
            case "Cancelled":
                return getResources().getColor(R.color.red);
            default:
                return getResources().getColor(R.color.text_primary);
        }
    }

    private void setupClickListeners() {
        btnUpdateStatus.setOnClickListener(v -> updateReportStatus());
        btnCancel.setOnClickListener(v -> finish());

        // Set up radio button change listener
        rgStatus.setOnCheckedChangeListener((group, checkedId) -> {
            // Enable/disable additional fields based on selection
            if (checkedId == R.id.rbInProgress || checkedId == R.id.rbOnHold) {
                etEstimatedCompletion.setEnabled(true);
                etRequiredTools.setEnabled(true);
            } else {
                etEstimatedCompletion.setEnabled(false);
                etRequiredTools.setEnabled(false);
            }
        });
    }

    private void updateReportStatus() {
        String newStatus = getSelectedStatus();
        String notes = etNotes.getText().toString().trim();
        String estimatedCompletion = etEstimatedCompletion.getText().toString().trim();
        String requiredTools = etRequiredTools.getText().toString().trim();

        if (newStatus == null) {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }

        // If status is In Progress or On Hold, estimated completion is required
        if (("In Progress".equals(newStatus) || "On Hold".equals(newStatus)) && estimatedCompletion.isEmpty()) {
            Toast.makeText(this, "Please provide estimated completion date", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("technicianNotes", notes);

        // Ensure technician info is set
        if (currentReport.getAssignedTechnicianId() == null || currentReport.getAssignedTechnicianId().isEmpty()) {
            updates.put("assignedTechnicianId", technicianId);
        }

        if (currentReport.getAssignedTechnicianName() == null || currentReport.getAssignedTechnicianName().isEmpty()) {
            updates.put("assignedTechnicianName", technicianName);
        }

        // Add to status history
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("status", newStatus);
        statusUpdate.put("timestamp", System.currentTimeMillis());
        statusUpdate.put("changedBy", technicianName);
        statusUpdate.put("notes", notes);

        updates.put("statusHistory/" + System.currentTimeMillis(), statusUpdate);

        // Update timestamp based on status
        if ("Completed".equals(newStatus)) {
            updates.put("completedTimestamp", System.currentTimeMillis());
        }

        reportsRef.child(reportId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Send notification to reporter
                    sendStatusUpdateNotification(newStatus);

                    Toast.makeText(this, "Status updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("UPDATE_STATUS", "Failed to update: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedStatus() {
        int selectedId = rgStatus.getCheckedRadioButtonId();

        if (selectedId == R.id.rbAcknowledged) {
            return "Acknowledged";
        } else if (selectedId == R.id.rbInProgress) {
            return "In Progress";
        } else if (selectedId == R.id.rbOnHold) {
            return "On Hold";
        } else if (selectedId == R.id.rbCompleted) {
            return "Completed";
        }

        return null;
    }

    private void sendStatusUpdateNotification(String newStatus) {
        if (currentReport != null) {
            // Send notification to reporter
            String message = "Your maintenance report for " + currentReport.getCategory() +
                    " has been updated to: " + newStatus;

            // Using NotificationUtils - make sure senderId and senderName are correct
            NotificationUtils.sendNotification(
                    currentReport.getReporterId(),
                    "Status Updated: " + currentReport.getCategory(),
                    message,
                    NotificationUtils.Types.STATUS_UPDATE,
                    reportId,
                    technicianId,
                    technicianName,
                    UpdateStatusActivity.this
            );

            // Also notify admin
            NotificationUtils.sendNotificationToRole(
                    "admin",
                    "Report Status Updated",
                    technicianName + " updated Report #" +
                            (reportId.length() > 8 ? reportId.substring(0, 8) : reportId) +
                            " to: " + newStatus,
                    NotificationUtils.Types.STATUS_UPDATE,
                    reportId,
                    technicianId,
                    technicianName,
                    UpdateStatusActivity.this
            );

            // Also notify the technician (optional)
            NotificationUtils.sendNotification(
                    technicianId,
                    "Status Update Confirmation",
                    "You updated report #" + (reportId.length() > 8 ? reportId.substring(0, 8) : reportId) +
                            " to: " + newStatus,
                    NotificationUtils.Types.STATUS_UPDATE,
                    reportId,
                    "system",
                    "System",
                    UpdateStatusActivity.this
            );
        }
    }
}