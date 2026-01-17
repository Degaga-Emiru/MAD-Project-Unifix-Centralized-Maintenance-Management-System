package com.hawassa.unifix.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hawassa.unifix.R;
import com.hawassa.unifix.auth.LoginActivity;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.models.User;
import com.hawassa.unifix.shared.NotificationsActivity;
import com.hawassa.unifix.shared.ProfileActivity;
import com.hawassa.unifix.shared.SettingsActivity;
import com.hawassa.unifix.shared.utils.NotificationBadgeManager;
import com.hawassa.unifix.student.adapters.RecentActivityAdapter;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    // UI Components
    private TextView tvWelcome, tvStudentName;
    private TextView tvTotalReports, tvPendingReports, tvInProgressReports, tvCompletedReports;
    private ImageView ivNotifications;
    private TextView tvNotificationCount;
    private NotificationBadgeManager notificationBadgeManager;

    // Card Views
    private MaterialCardView cardNewReport, cardReportHistory, cardFeedback,
            cardSettings, cardHelp, cardProfile;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference reportsRef, usersRef;
    private ValueEventListener statsListener;
    private ValueEventListener recentActivityListener;

    // User data
    private String firebaseUid, customUserId, userName, userRole;

    // Recent Activity
    private RecyclerView rvRecentActivity;
    private RecentActivityAdapter recentActivityAdapter;
    private List<MaintenanceReport> recentReportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Get user data from Intent
        Intent intent = getIntent();
        firebaseUid = intent.getStringExtra("firebaseUid");
        customUserId = intent.getStringExtra("userId");
        userName = intent.getStringExtra("userName");
        userRole = intent.getStringExtra("userRole");

        // Debug log
        Log.d("STUDENT_DASHBOARD", "Received - firebaseUid: " + firebaseUid +
                ", userId: " + customUserId + ", name: " + userName + ", role: " + userRole);

        // Fallback to FirebaseAuth if firebaseUid is null
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                firebaseUid = currentUser.getUid();
                Log.d("STUDENT_DASHBOARD", "Got firebaseUid from Auth: " + firebaseUid);
            }
        }

        // If still no user ID, redirect to login
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.e("STUDENT_DASHBOARD", "No user ID found, redirecting to login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupToolbar();
        setupCardClickListeners();
        updateWelcomeMessage();
        setupNotificationSystem();
    }

    private void initializeViews() {
        // Welcome and Name TextViews
        tvWelcome = findViewById(R.id.tvWelcome);
        tvStudentName = findViewById(R.id.tvStudentName);

        // Statistics TextViews
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        tvInProgressReports = findViewById(R.id.tvInProgressReports);
        tvCompletedReports = findViewById(R.id.tvCompletedReports);

        // Notification icon in toolbar
        ivNotifications = findViewById(R.id.ivNotifications);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);

        // Card Views
        cardNewReport = findViewById(R.id.cardNewReport);
        cardReportHistory = findViewById(R.id.cardReportHistory);
        cardFeedback = findViewById(R.id.cardFeedback);
        cardSettings = findViewById(R.id.cardSettings);
        cardHelp = findViewById(R.id.cardHelp);
        cardProfile = findViewById(R.id.cardProfile);

        // Recent Activity RecyclerView
        rvRecentActivity = findViewById(R.id.rvRecentActivity);

        // Initialize recent activity adapter
        recentActivityAdapter = new RecentActivityAdapter(recentReportList);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivity.setAdapter(recentActivityAdapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Unifix Student Panel");
    }

    private void updateWelcomeMessage() {
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText("Welcome Back,");
            tvStudentName.setText(userName);
        } else {
            // Try to get name from database if not in intent
            usersRef.child(firebaseUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null && user.getName() != null) {
                            userName = user.getName();
                            tvWelcome.setText("Welcome Back,");
                            tvStudentName.setText(userName);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Use defaults
                    tvWelcome.setText("Welcome Back,");
                    tvStudentName.setText("Student");
                }
            });
        }
    }

    private void setupCardClickListeners() {
        cardNewReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubmitReportActivity.class);
            intent.putExtra("firebaseUid", firebaseUid);
            intent.putExtra("userId", customUserId);
            intent.putExtra("userName", userName);
            startActivity(intent);
        });

        cardReportHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportHistoryActivity.class);
            intent.putExtra("firebaseUid", firebaseUid);
            startActivity(intent);
        });

        cardFeedback.setOnClickListener(v -> {
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.putExtra("firebaseUid", firebaseUid);
            intent.putExtra("userId", customUserId);
            intent.putExtra("userName", userName);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> openSettings());
        cardHelp.setOnClickListener(v -> openHelp());

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("firebaseUid", firebaseUid);
            intent.putExtra("userId", customUserId);
            startActivity(intent);
        });
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("userId", firebaseUid);
        intent.putExtra("userRole", "student");
        startActivity(intent);
    }

    private void openHelp() {
        Intent intent = new Intent(this, HelpSupportActivity.class);
        startActivity(intent);
    }

    private void refreshDashboard() {
        // Remove old listeners before adding new ones
        removeFirebaseListeners();

        loadStudentReportStats();
        loadRecentActivity();

        // Show toast only when manually refreshed, not in onResume
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }

    private void loadStudentReportStats() {
        if (firebaseUid == null) return;

        // Remove old listener if exists
        if (statsListener != null) {
            reportsRef.removeEventListener(statsListener);
        }

        // Query reports for this specific student
        statsListener = reportsRef.orderByChild("reporterId").equalTo(firebaseUid)
                .addValueEventListener(new ValueEventListener() {
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

                        Log.d("STUDENT_STATS", "Total: " + total + ", Pending: " + pending +
                                ", In Progress: " + inProgress + ", Completed: " + completed);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentDashboardActivity.this,
                                "Failed to load report stats: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("STUDENT_STATS", "Error: " + error.getMessage());
                    }
                });
    }

    private void setupNotificationSystem() {
        if (ivNotifications == null) {
            ivNotifications = findViewById(R.id.ivNotifications);
        }
        if (tvNotificationCount == null) {
            tvNotificationCount = findViewById(R.id.tvNotificationCount);
        }

        if (ivNotifications == null || tvNotificationCount == null) {
            Log.e("StudentDashboard", "Notification views not found!");
            return;
        }

        Log.d("StudentDashboard", "Setting up notification system for: " + firebaseUid);

        // Clean up previous instance
        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
        }

        // Create new instance
        notificationBadgeManager = new NotificationBadgeManager(firebaseUid, tvNotificationCount);
        notificationBadgeManager.startListening();

        // Setup notification icon click
        ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("userId", firebaseUid);
            startActivity(intent);
        });
    }

    private void loadRecentActivity() {
        if (firebaseUid == null) return;

        // Remove old listener if exists
        if (recentActivityListener != null) {
            reportsRef.removeEventListener(recentActivityListener);
        }

        // Query reports for this student, limit to 5 most recent
        recentActivityListener = reportsRef.orderByChild("reporterId").equalTo(firebaseUid)
                .limitToLast(5)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        recentReportList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                            if (report != null) {
                                // Add to beginning for newest first
                                recentReportList.add(0, report);
                            }
                        }
                        recentActivityAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("RECENT_ACTIVITY", "Error loading recent activity: " + error.getMessage());
                    }
                });
    }

    private void removeFirebaseListeners() {
        if (statsListener != null) {
            reportsRef.removeEventListener(statsListener);
            statsListener = null;
        }

        if (recentActivityListener != null) {
            reportsRef.removeEventListener(recentActivityListener);
            recentActivityListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // IMPORTANT: Don't call everything in onResume
        // Only restart notification badge manager
        if (notificationBadgeManager != null) {
            notificationBadgeManager.startListening();
        }

        // Refresh data only if needed
        loadStudentReportStats();
        loadRecentActivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop badge manager but keep Firebase listeners for background updates
        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Clean up Firebase listeners when activity is not visible
        removeFirebaseListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeFirebaseListeners();

        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
            notificationBadgeManager = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_logout) {
            logoutUser();
            return true;
        } else if (itemId == R.id.menu_settings) {
            openSettings();
            return true;
        } else if (itemId == R.id.menu_refresh) {
            refreshDashboard();
            return true;
        } else if (itemId == R.id.menu_help) {
            openHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        // Clean up all listeners before logout
        removeFirebaseListeners();
        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListening();
            notificationBadgeManager = null;
        }

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}