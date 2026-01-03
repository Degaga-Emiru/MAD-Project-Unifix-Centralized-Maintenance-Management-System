package com.hawassa.unifix.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.admin.adapters.AdminReportAdapter;
import com.hawassa.unifix.auth.LoginActivity;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.shared.SettingsActivity;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;
import com.google.android.material.snackbar.Snackbar;
import com.hawassa.unifix.shared.NotificationsActivity;
import com.hawassa.unifix.shared.utils.NotificationBadgeManager;

public class AdminDashboardActivity extends AppCompatActivity
        implements AdminReportAdapter.OnAssignClickListener,
        AdminReportAdapter.OnDeleteClickListener,
        AdminReportAdapter.OnViewDetailsClickListener

{

    // UI Components
    private TextView tvWelcome, tvAdminName;
    private TextView tvTotalReports, tvPendingReports, tvInProgressReports, tvCompletedReports;
    private RecyclerView rvRecentReports;

    // Card Views
    private MaterialCardView cardTaskAssignment, cardViewReports, cardAnalytics,
            cardCreateTechnician, cardUserManagement, cardSettings;

    // Firebase
    private DatabaseReference reportsRef, usersRef;

    // User data
    private String firebaseUid, customUserId, userName, userRole;
    // Add these with other UI Components variables
    private ImageView ivNotifications;
    private TextView tvNotificationCount;

    // Add this with other variables
    private NotificationBadgeManager notificationBadgeManager;
    // Adapter
    private AdminReportAdapter adminReportAdapter;
    private List<MaintenanceReport> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Get user data from Intent
        Intent intent = getIntent();
        firebaseUid = intent.getStringExtra("firebaseUid");
        customUserId = intent.getStringExtra("userId");
        userName = intent.getStringExtra("userName");
        userRole = intent.getStringExtra("userRole");

        // Fallback to FirebaseAuth
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                firebaseUid = currentUser.getUid();
            }
        }

        // Redirect to login if no user ID
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFirebase();
        setupCardClickListeners();
        loadDashboardStats();
        loadRecentReports();
        updateWelcomeMessage();
        setupNotificationSystem();

    }

    private void initializeViews() {
        // Welcome and Admin Name
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAdminName = findViewById(R.id.tvAdminName);

        // Statistics TextViews
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        tvInProgressReports = findViewById(R.id.tvInProgressReports);
        tvCompletedReports = findViewById(R.id.tvCompletedReports);
        // Add these lines AFTER the Statistics TextViews
        // Notification icon and badge
        ivNotifications = findViewById(R.id.ivNotifications);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);

        if (ivNotifications == null) {
            Log.e("AdminDashboard", "ivNotifications is NULL - check your layout!");
        }
        if (tvNotificationCount == null) {
            Log.e("AdminDashboard", "tvNotificationCount is NULL - check your layout!");
        }
        // RecyclerView
        rvRecentReports = findViewById(R.id.rvRecentReports);

        // Card Views
        cardTaskAssignment = findViewById(R.id.cardTaskAssignment);
        cardViewReports = findViewById(R.id.cardViewReports);
        cardAnalytics = findViewById(R.id.cardAnalytics);
        cardCreateTechnician = findViewById(R.id.cardCreateTechnician);
        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardSettings = findViewById(R.id.cardSettings);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Unifix Admin Panel");
        // Set overflow icon color to white
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert));
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));
        }}
    private void setupNotificationSystem() {
        if (ivNotifications == null || tvNotificationCount == null) {
            Log.e("AdminDashboard", "Notification views not found in layout!");
            return;
        }

        Log.d("AdminDashboard", "Setting up notification system for: " + firebaseUid);

        // Setup notification badge manager
        notificationBadgeManager = new NotificationBadgeManager(firebaseUid, tvNotificationCount);
        notificationBadgeManager.startListening();

        // Setup notification icon click
        ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("userId", firebaseUid);
            startActivity(intent);
        });

        // Test: Check current notification count
        NotificationBadgeManager.getUnreadCount(firebaseUid, count -> {
            Log.d("AdminDashboard", "Initial unread count: " + count);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        adminReportAdapter = new AdminReportAdapter(
                reportList,
                this,  // OnAssignClickListener
                this,  // OnDeleteClickListener
                this   // OnViewDetailsClickListener
        );
        rvRecentReports.setLayoutManager(new LinearLayoutManager(this));
        rvRecentReports.setAdapter(adminReportAdapter);
    }

    private void setupFirebase() {
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    private void updateWelcomeMessage() {
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText("Welcome Back,");
            tvAdminName.setText(userName);
        } else {
            tvWelcome.setText("Welcome Back,");
            tvAdminName.setText("Administrator");
        }
    }

    private void setupCardClickListeners() {
        // Task Assignment Card
        cardTaskAssignment.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskAssignmentActivity.class);
            intent.putExtra("adminUid", firebaseUid);
            startActivity(intent);
        });

        // View Reports Card
        cardViewReports.setOnClickListener(v -> {
            // Create new AllReportsActivity or use existing
            Toast.makeText(this, "All Reports View coming soon", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AllReportsActivity.class);
            startActivity(intent);
        });

        // Analytics Card
        cardAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            //intent.putExtra("adminUid", firebaseUid);
            startActivity(intent);
        });

        // Create Technician Card
        cardCreateTechnician.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTechnicianActivity.class);
            intent.putExtra("adminUid", firebaseUid);
            startActivity(intent);
        });

        // User Management Card
        cardUserManagement.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserManagementActivity.class);
            intent.putExtra("adminUid", firebaseUid);
            startActivity(intent);
        });

        // Settings Card
        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("userRole", "admin");
            startActivity(intent);
        });
    }

    private void loadDashboardStats() {
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0, pending = 0, inProgress = 0, completed = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                    if (report == null) continue;

                    total++;
                    switch (report.getStatus()) {
                        case "Submitted":
                            pending++;
                            break;
                        case "Assigned":
                        case "In Progress":
                            inProgress++;
                            break;
                        case "Completed":
                            completed++;
                            break;
                    }
                }

                tvTotalReports.setText(String.valueOf(total));
                tvPendingReports.setText(String.valueOf(pending));
                tvInProgressReports.setText(String.valueOf(inProgress));
                tvCompletedReports.setText(String.valueOf(completed));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ADMIN_STATS", "Error loading stats: " + error.getMessage());
            }
        });
    }

    private void loadRecentReports() {
        reportsRef.limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                    if (report != null) {
                        reportList.add(0, report); // newest first
                    }
                }
                adminReportAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ADMIN_REPORTS", "Error loading reports: " + error.getMessage());
            }
        });
    }

    @Override
    public void onAssignClick(MaintenanceReport report) {
        Intent intent = new Intent(this, TaskAssignmentActivity.class);
        intent.putExtra("reportId", report.getReportId());
        intent.putExtra("adminUid", firebaseUid);
        startActivity(intent);
    }
    @Override
    public void onDeleteClick(MaintenanceReport report) {
        // Show confirmation dialog for deletion
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Report");
        builder.setMessage("Are you sure you want to delete this completed report?\n\n" +
                "Report ID: " + report.getReportId() + "\n" +
                "Category: " + report.getCategory() + "\n" +
                "Location: " + report.getBuildingBlock() + ", Room " + report.getRoomNumber() + "\n\n" +
                "This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Delete from Firebase
            DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
            if (report.getReportId() != null) {
                reportsRef.child(report.getReportId()).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Report deleted successfully",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to delete: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onViewDetailsClick(MaintenanceReport report) {
        // Navigate to report details
        // For now, show a dialog with details
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
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
                                        "Technician Notes: " + report.getTechnicianNotes() + "\n" : "")
                )
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationBadgeManager != null) {
            notificationBadgeManager.startListening();
        }

        // Check for new notifications when returning
        NotificationBadgeManager.getUnreadCount(firebaseUid, count -> {
            if (count > 0) {
                Log.d("AdminDashboard", "Resumed with " + count + " unread notifications");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
        }
    }
}