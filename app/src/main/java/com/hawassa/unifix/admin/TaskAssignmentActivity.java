package com.hawassa.unifix.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.admin.adapters.AdminReportAdapter;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.models.User;
import com.hawassa.unifix.shared.utils.NotificationUtils;
import java.util.*;

public class TaskAssignmentActivity extends AppCompatActivity {
    private RecyclerView rvReports;
    private Spinner spTechnicians;
    private Map<String, User> techniciansMap = new HashMap<>(); // Firebase UID -> User
    private List<MaintenanceReport> reportList = new ArrayList<>();
    private AdminReportAdapter adapter;
    private DatabaseReference reportsRef, usersRef;
    private String adminId, adminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_assignment);

        // Get admin info from intent
        adminId = getIntent().getStringExtra("adminUid");
        adminName = getIntent().getStringExtra("adminName");
        if (adminName == null) adminName = "Administrator";

        initializeViews();
        setupFirebase();
        loadPendingReports();
        loadTechnicians();
    }

    private void initializeViews() {
        rvReports = findViewById(R.id.rvReports);
        spTechnicians = findViewById(R.id.spinnerTechnicians);

        // Set up adapter with lambda for assignment
        adapter = new AdminReportAdapter(reportList, report -> assignTaskToTechnician(report),
                null,  // OnDeleteClickListener
                null);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);
    }

    private void setupFirebase() {
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    private void loadPendingReports() {
        reportsRef.orderByChild("status").equalTo("Submitted")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        reportList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MaintenanceReport report = snapshot.getValue(MaintenanceReport.class);
                            if (report != null) {
                                reportList.add(report);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("TASK_ASSIGN", "Loaded " + reportList.size() + " pending reports");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("TASK_ASSIGN", "Failed to load reports: " + databaseError.getMessage());
                    }
                });
    }

    private void loadTechnicians() {
        usersRef.orderByChild("role").equalTo("staff") // Your technicians have "staff" role
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        techniciansMap.clear();
                        List<String> technicianNames = new ArrayList<>();
                        technicianNames.add("Select Technician"); // Default option

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User technician = snapshot.getValue(User.class);
                            if (technician != null && "active".equals(technician.getStatus())) {
                                String firebaseUid = snapshot.getKey(); // This is the Firebase UID
                                techniciansMap.put(firebaseUid, technician);
                                technicianNames.add(technician.getName() + " (" + technician.getUserId() + ")");
                            }
                        }

                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                                TaskAssignmentActivity.this,
                                android.R.layout.simple_spinner_item,
                                technicianNames
                        );
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spTechnicians.setAdapter(spinnerAdapter);

                        Log.d("TASK_ASSIGN", "Loaded " + techniciansMap.size() + " active technicians");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("TASK_ASSIGN", "Failed to load technicians: " + databaseError.getMessage());
                    }
                });
    }

    private void assignTaskToTechnician(MaintenanceReport report) {
        int selectedPosition = spTechnicians.getSelectedItemPosition();
        if (selectedPosition <= 0) { // 0 is "Select Technician"
            Toast.makeText(this, "Please select a technician", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedText = spTechnicians.getSelectedItem().toString();
        String technicianId = getTechnicianIdFromSelection(selectedText);

        if (technicianId != null && techniciansMap.containsKey(technicianId)) {
            User technician = techniciansMap.get(technicianId);

            // Create update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "Assigned");
            updates.put("assignedTechnicianId", technicianId);
            updates.put("assignedTechnicianName", technician.getName());

            // Update the report
            reportsRef.child(report.getReportId()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TASK_ASSIGN", "✅ Report assigned: " + report.getReportId() +
                                " to " + technician.getName());

                        // Send notifications using NotificationUtils
                        sendNotifications(report, technician, technicianId);

                        Toast.makeText(this, "✅ Task assigned to " + technician.getName(),
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Assignment failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("TASK_ASSIGN", "Assignment failed: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, "Technician not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotifications(MaintenanceReport report, User technician, String technicianId) {
        // 1. Notify Technician
        String techMessage = "You've been assigned to fix a " + report.getCategory() +
                " issue in " + report.getBuildingBlock() + ", Room " + report.getRoomNumber();

        NotificationUtils.sendNotification(
                technicianId,
                "🛠️ New Task Assigned",
                techMessage,
                NotificationUtils.Types.TASK_ASSIGNED,
                report.getReportId(),
                adminId,
                adminName,
                TaskAssignmentActivity.this // Add this

        );

        Log.d("TASK_ASSIGN", "📤 Notification sent to technician: " + technician.getName());

        // 2. Notify Student (Reporter)
        String studentMessage = "Your " + report.getCategory() + " report has been assigned to " +
                technician.getName();

        NotificationUtils.sendNotification(
                report.getReporterId(),
                "👨‍🔧 Technician Assigned",
                studentMessage,
                NotificationUtils.Types.STATUS_UPDATE,
                report.getReportId(),
                adminId,
                adminName,
                TaskAssignmentActivity.this // Add this

        );

        Log.d("TASK_ASSIGN", "📤 Notification sent to student: " + report.getReporterName());

        // 3. Optional: Notify other admins
        NotificationUtils.sendNotificationToRole(
                "admin",
                "✅ Task Assigned",
                adminName + " assigned " + report.getCategory() + " to " + technician.getName(),
                NotificationUtils.Types.ADMIN_ALERT,
                report.getReportId(),
                adminId,
                adminName,
                TaskAssignmentActivity.this // Add this

        );
    }

    private String getTechnicianIdFromSelection(String selection) {
        // Selection format: "John Doe (TECH001)"
        // We need to extract the user ID from parentheses
        if (selection.contains("(") && selection.contains(")")) {
            int start = selection.indexOf("(") + 1;
            int end = selection.indexOf(")");
            String userId = selection.substring(start, end);

            // Find Firebase UID by userId
            for (Map.Entry<String, User> entry : techniciansMap.entrySet()) {
                if (entry.getValue().getUserId().equals(userId)) {
                    return entry.getKey(); // Return Firebase UID
                }
            }
        }
        return null;
    }

    // Alternative: Get technician directly by Firebase UID (simpler)
    private String getTechnicianIdByName(String name) {
        for (Map.Entry<String, User> entry : techniciansMap.entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                return entry.getKey(); // Return Firebase UID
            }
        }
        return null;
    }
}