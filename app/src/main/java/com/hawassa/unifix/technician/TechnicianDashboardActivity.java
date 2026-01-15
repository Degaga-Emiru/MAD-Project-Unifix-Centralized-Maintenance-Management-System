package com.hawassa.unifix.technician;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.auth.LoginActivity;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.shared.NotificationsActivity;
import com.hawassa.unifix.shared.ProfileActivity;
import com.hawassa.unifix.shared.SettingsActivity;
import com.hawassa.unifix.shared.utils.NotificationBadgeManager;
import com.hawassa.unifix.technician.adapters.RecentTasksAdapter;

import java.util.ArrayList;
import java.util.List;

public class TechnicianDashboardActivity extends AppCompatActivity {
    private TextView tvAssignedTasks, tvCompletedTasks, tvInProgressTasks, tvWelcome;
    private DatabaseReference reportsRef, usersRef;
    private String firebaseUid, customUserId, userRole, userName;

    // Add notification badge manager like Admin/Student dashboards
    private ImageView ivNotifications;
    private TextView tvNotificationCount;
    private NotificationBadgeManager notificationBadgeManager;

    // Add recent tasks RecyclerView
    private RecyclerView rvRecentTasks;
    private RecentTasksAdapter recentTasksAdapter;
    private List<MaintenanceReport> recentTasksList = new ArrayList<>();

    // Add these constants at the top for consistency
    private static final String EXTRA_FIREBASE_UID = "firebaseUid";
    private static final String EXTRA_USER_ID = "userId";
    private static final String EXTRA_USER_ROLE = "userRole";
    private static final String EXTRA_USER_NAME = "userName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        // Get user data from Intent (matching LoginActivity)
        Intent intent = getIntent();
        firebaseUid = intent.getStringExtra(EXTRA_FIREBASE_UID);
        customUserId = intent.getStringExtra(EXTRA_USER_ID);
        userRole = intent.getStringExtra(EXTRA_USER_ROLE);
        userName = intent.getStringExtra(EXTRA_USER_NAME);

        // Debug log to see what we received
        Log.d("TECH_DASHBOARD", "Received - firebaseUid: " + firebaseUid +
                ", userId: " + customUserId + ", name: " + userName + ", role: " + userRole);

        // If firebaseUid is null, try to get it from FirebaseAuth
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                firebaseUid = currentUser.getUid();
                Log.d("TECH_DASHBOARD", "Got firebaseUid from Auth: " + firebaseUid);
            }
        }

        // Fallback: if still null, go back to login
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.e("TECH_DASHBOARD", "No user ID found, redirecting to login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupNotificationSystem();
        loadTechnicianStats();
        loadRecentTasks();
        updateWelcomeMessage();
    }

    private void initializeViews() {
        tvAssignedTasks = findViewById(R.id.tvAssignedTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvInProgressTasks = findViewById(R.id.tvInProgressTasks);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Setup recent tasks RecyclerView (you need to add this to XML)
        rvRecentTasks = findViewById(R.id.rvRecentTasks);
        if (rvRecentTasks != null) {
            recentTasksAdapter = new RecentTasksAdapter(recentTasksList);
            rvRecentTasks.setLayoutManager(new LinearLayoutManager(this));
            rvRecentTasks.setAdapter(recentTasksAdapter);
        }

        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Setup click listeners - use firebaseUid for technicianId
        findViewById(R.id.btnAssignedTasks).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, AssignedTasksActivity.class);
                intent.putExtra("technicianId", firebaseUid);
                intent.putExtra("customUserId", customUserId);
                intent.putExtra("userName", userName);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("TECH_DASHBOARD", "Error opening AssignedTasks: " + e.getMessage());
                Toast.makeText(this, "Error opening tasks", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnUpdateStatus).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, UpdateStatusActivity.class);
                intent.putExtra("technicianId", firebaseUid);
                intent.putExtra("customUserId", customUserId);
                intent.putExtra("userName", userName);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("TECH_DASHBOARD", "Error opening UpdateStatus: " + e.getMessage());
                Toast.makeText(this, "Error opening update status", Toast.LENGTH_SHORT).show();
            }
        });

        // Profile button
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", firebaseUid);
            intent.putExtra("customUserId", customUserId);
            startActivity(intent);
        });

        // Settings button
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("userId", firebaseUid);
            intent.putExtra("userRole", "technician");
            startActivity(intent);
        });

        // View All Tasks button
        MaterialButton btnViewAllTasks = findViewById(R.id.btnViewAllTasks);
        if (btnViewAllTasks != null) {
            btnViewAllTasks.setOnClickListener(v -> {
                Intent intent = new Intent(this, AssignedTasksActivity.class);
                intent.putExtra("technicianId", firebaseUid);
                intent.putExtra("customUserId", customUserId);
                intent.putExtra("userName", userName);
                startActivity(intent);
            });
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set title with user's custom ID if available
        if (customUserId != null && !customUserId.isEmpty()) {
            toolbar.setTitle("Technician - " + customUserId);
        } else {
            toolbar.setTitle("Technician Dashboard");
        }
    }

    private void setupNotificationSystem() {
        // Find notification views (you need to add these to your layout)
        ivNotifications = findViewById(R.id.ivNotifications);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);

        if (ivNotifications == null || tvNotificationCount == null) {
            Log.e("TECH_DASHBOARD", "Notification views not found in layout!");
            return;
        }

        Log.d("TECH_DASHBOARD", "Setting up notification system for: " + firebaseUid);

        // Setup notification badge manager
        notificationBadgeManager = new NotificationBadgeManager(firebaseUid, tvNotificationCount);
        notificationBadgeManager.startListening();

        // Setup notification icon click
        ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("userId", firebaseUid);
            startActivity(intent);
        });
    }

    private void updateWelcomeMessage() {
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText("Welcome, " + userName);
        } else if (customUserId != null && !customUserId.isEmpty()) {
            tvWelcome.setText("Welcome, Tech " + customUserId);
        } else {
            tvWelcome.setText("Welcome, Technician");
        }
    }

    private void loadTechnicianStats() {
        // Use firebaseUid to filter reports
        reportsRef.orderByChild("assignedTechnicianId").equalTo(firebaseUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int assigned = 0;
                        int completed = 0;
                        int inProgress = 0;

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MaintenanceReport report = snapshot.getValue(MaintenanceReport.class);
                            if (report != null) {
                                assigned++;
                                if ("Completed".equals(report.getStatus())) {
                                    completed++;
                                } else if ("In Progress".equals(report.getStatus())) {
                                    inProgress++;
                                }
                            }
                        }

                        tvAssignedTasks.setText(String.valueOf(assigned));
                        tvCompletedTasks.setText(String.valueOf(completed));
                        tvInProgressTasks.setText(String.valueOf(inProgress));

                        Log.d("TECH_STATS", "Assigned: " + assigned +
                                ", Completed: " + completed + ", In Progress: " + inProgress);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("TECH_STATS", "Error loading stats: " + databaseError.getMessage());
                        Toast.makeText(TechnicianDashboardActivity.this,
                                "Failed to load statistics", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRecentTasks() {
        reportsRef.orderByChild("assignedTechnicianId").equalTo(firebaseUid)
                .limitToLast(5)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        recentTasksList.clear();

                        boolean hasTasks = false;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                            if (report != null) {
                                recentTasksList.add(0, report); // newest first
                                hasTasks = true;
                            }
                        }

                        if (recentTasksAdapter != null) {
                            recentTasksAdapter.notifyDataSetChanged();
                        }

                        // Show/hide empty state message
                        TextView tvNoTasks = findViewById(R.id.tvNoTasks);
                        if (tvNoTasks != null) {
                            if (hasTasks) {
                                tvNoTasks.setVisibility(View.GONE);
                            } else {
                                tvNoTasks.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("RECENT_TASKS", "Error: " + error.getMessage());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.technician_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_notifications) {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("userId", firebaseUid);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("userId", firebaseUid);
            intent.putExtra("userRole", "technician");
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_logout) {
            logoutUser();
            return true;
        } else if (id == R.id.menu_refresh) {
            refreshDashboard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void refreshDashboard() {
        loadTechnicianStats();
        loadRecentTasks();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationBadgeManager != null) {
            notificationBadgeManager.startListening();
        }
        loadTechnicianStats();
        loadRecentTasks();
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