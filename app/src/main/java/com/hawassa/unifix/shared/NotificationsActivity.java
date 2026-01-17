package com.hawassa.unifix.shared;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.Notification;
import com.hawassa.unifix.shared.adapters.NotificationAdapter;
import com.hawassa.unifix.shared.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView tvNoNotifications;
    private MaterialButton btnMarkAllRead, btnClearAll;
    private SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseReference notificationsRef;
    private List<Notification> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;
    private String userId;
    private ValueEventListener notificationsListener;
    private boolean isActivityActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Debug log
        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_notifications);
            Log.d(TAG, "Layout inflated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            finish();
            return;
        }

        userId = getIntent().getStringExtra("userId");
        Log.d(TAG, "User ID received: " + (userId != null ? userId : "NULL"));

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "No user ID provided, finishing activity");
            finish();
            return;
        }

        try {
            initializeViews();
            setupFirebase();
            loadNotifications();
            setupClickListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.e(TAG, "Toolbar not found!");
            throw new RuntimeException("Toolbar not found in layout");
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Back pressed");
            onBackPressed();
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAll = findViewById(R.id.btnClearAll);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Verify all views are found
        if (rvNotifications == null) Log.e(TAG, "rvNotifications not found!");
        if (progressBar == null) Log.e(TAG, "progressBar not found!");
        if (tvNoNotifications == null) Log.e(TAG, "tvNoNotifications not found!");
        if (btnMarkAllRead == null) Log.e(TAG, "btnMarkAllRead not found!");
        if (btnClearAll == null) Log.e(TAG, "btnClearAll not found!");
        if (swipeRefreshLayout == null) Log.e(TAG, "swipeRefreshLayout not found!");

        // Initialize adapter with safe click listener
        adapter = new NotificationAdapter(notificationList, notification -> {
            Log.d(TAG, "Notification clicked: " +
                    (notification != null ? notification.getTitle() : "null"));
            if (notification != null) {
                handleNotificationClick(notification);
            }
        });

        // Setup RecyclerView
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
            rvNotifications.setAdapter(adapter);
        } else {
            Log.e(TAG, "Cannot setup RecyclerView - rvNotifications is null");
        }

        // Initialize buttons state
        if (btnMarkAllRead != null) btnMarkAllRead.setEnabled(false);
        if (btnClearAll != null) btnClearAll.setEnabled(false);

        Log.d(TAG, "Views initialized successfully");
    }

    private void setupFirebase() {
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
        Log.d(TAG, "Firebase setup complete");
    }

    private void loadNotifications() {
        Log.d(TAG, "Loading notifications for user: " + userId);

        if (!isActivityActive) {
            Log.d(TAG, "Activity not active, skipping load");
            return;
        }

        showLoading(true);

        // Remove old listener if exists
        removeNotificationListener();

        if (notificationsRef == null) {
            Log.e(TAG, "notificationsRef is null!");
            showLoading(false);
            return;
        }

        try {
            notificationsListener = notificationsRef.orderByChild("userId").equalTo(userId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Log.d(TAG, "onDataChange called, snapshot exists: " + dataSnapshot.exists());

                            if (!isActivityActive) {
                                Log.d(TAG, "Activity not active, ignoring data change");
                                return;
                            }

                            final List<Notification> tempList = new ArrayList<>();

                            try {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    Notification notification = snapshot.getValue(Notification.class);
                                    if (notification != null) {
                                        tempList.add(notification);
                                    }
                                }

                                // Calculate hasUnread after building the list
                                boolean hasUnreadValue = false;
                                for (Notification notification : tempList) {
                                    if (notification != null && !notification.isRead()) {
                                        hasUnreadValue = true;
                                        break;
                                    }
                                }

                                // Sort by timestamp (newest first)
                                if (!tempList.isEmpty()) {
                                    Collections.sort(tempList, (n1, n2) ->
                                            Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                                }

                                // Create final variables for lambda
                                final boolean finalHasUnread = hasUnreadValue;
                                final List<Notification> finalTempList = new ArrayList<>(tempList);

                                // Update the main list and UI on main thread
                                runOnUiThread(() -> {
                                    notificationList.clear();
                                    notificationList.addAll(finalTempList);
                                    updateUI(finalHasUnread);
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                    showLoading(false);

                                    Log.d(TAG, "Notifications loaded: " + notificationList.size() +
                                            " items, hasUnread: " + finalHasUnread);
                                });

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing notifications: " + e.getMessage(), e);
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Firebase onCancelled: " + databaseError.getMessage() +
                                    ", code: " + databaseError.getCode());

                            if (!isActivityActive) return;

                            runOnUiThread(() -> {
                                showLoading(false);
                                swipeRefreshLayout.setRefreshing(false);

                                // Show error message
                                if (tvNoNotifications != null) {
                                    tvNoNotifications.setText("Error loading notifications");
                                    tvNoNotifications.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase listener: " + e.getMessage(), e);
            showLoading(false);
        }
    }

    private void updateUI(boolean hasUnread) {
        Log.d(TAG, "updateUI called, list size: " + notificationList.size() + ", hasUnread: " + hasUnread);

        if (notificationList.isEmpty()) {
            Log.d(TAG, "No notifications to show");
            if (tvNoNotifications != null) {
                tvNoNotifications.setText("No notifications yet");
                tvNoNotifications.setVisibility(View.VISIBLE);
            }
            if (rvNotifications != null) {
                rvNotifications.setVisibility(View.GONE);
            }
            if (btnClearAll != null) {
                btnClearAll.setEnabled(false);
            }
            if (btnMarkAllRead != null) {
                btnMarkAllRead.setEnabled(false);
            }
        } else {
            Log.d(TAG, "Showing " + notificationList.size() + " notifications");
            if (tvNoNotifications != null) {
                tvNoNotifications.setVisibility(View.GONE);
            }
            if (rvNotifications != null) {
                rvNotifications.setVisibility(View.VISIBLE);
            }
            if (btnClearAll != null) {
                btnClearAll.setEnabled(true);
            }
            if (btnMarkAllRead != null) {
                btnMarkAllRead.setEnabled(hasUnread);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void removeNotificationListener() {
        if (notificationsListener != null && notificationsRef != null) {
            try {
                notificationsRef.removeEventListener(notificationsListener);
                Log.d(TAG, "Notification listener removed");
            } catch (Exception e) {
                Log.e(TAG, "Error removing listener: " + e.getMessage(), e);
            }
            notificationsListener = null;
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                Log.d(TAG, "Mark all as read clicked");
                if (userId != null && !userId.isEmpty()) {
                    btnMarkAllRead.setEnabled(false);
                    NotificationUtils.markAllAsRead(userId);
                    // Show toast or snackbar
                }
            });
        }

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                Log.d(TAG, "Clear all clicked");
                if (userId != null && !userId.isEmpty()) {
                    NotificationUtils.clearAllNotifications(userId);
                    notificationList.clear();
                    updateUI(false);
                    // Show toast or snackbar
                }
            });
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d(TAG, "Pull to refresh triggered");
                if (userId != null && !userId.isEmpty()) {
                    loadNotifications();
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void handleNotificationClick(Notification notification) {
        Log.d(TAG, "Handling notification click: " + notification.getTitle());

        if (notification == null) {
            Log.w(TAG, "Null notification in handleNotificationClick");
            return;
        }

        // Mark notification as read when clicked
        if (!notification.isRead()) {
            String notificationId = notification.getNotificationId();
            if (notificationId != null && !notificationId.isEmpty()) {
                Log.d(TAG, "Marking notification as read: " + notificationId);
                NotificationUtils.markAsRead(notificationId);
            }
        }

        // Handle navigation based on notification type
        String reportId = notification.getReportId();
        if (reportId != null && !reportId.isEmpty()) {
            navigateToReport(reportId, notification.getType());
        } else {
            Log.d(TAG, "No report ID in notification");
        }
    }

    private void navigateToReport(String reportId, String type) {
        Log.d(TAG, "Navigating to report: " + reportId + ", type: " + type);

        try {
            // TODO: Implement navigation to report details
            // For now, just log the action
            switch (type) {
                case "new_report":
                case "task_assigned":
                case "status_update":
                case "feedback":
                    Log.d(TAG, "Should navigate to ReportDetailActivity with reportId: " + reportId);
                    break;
                default:
                    Log.d(TAG, "No navigation for type: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in navigateToReport: " + e.getMessage(), e);
        }
    }

    private void showLoading(boolean show) {
        Log.d(TAG, "showLoading: " + show);

        runOnUiThread(() -> {
            try {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (show && rvNotifications != null) {
                    rvNotifications.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in showLoading: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        isActivityActive = true;

        // Refresh notifications when returning to activity
        if (userId != null && !userId.isEmpty()) {
            loadNotifications();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        isActivityActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        isActivityActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        isActivityActive = false;

        // Remove Firebase listeners
        removeNotificationListener();

        // Clear references to prevent memory leaks
        notificationList.clear();
    }
}