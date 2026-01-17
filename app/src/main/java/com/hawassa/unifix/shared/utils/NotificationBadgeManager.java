package com.hawassa.unifix.shared.utils;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.firebase.database.*;

public class NotificationBadgeManager {

    private DatabaseReference notificationsRef;
    private String userId;
    private TextView badgeTextView;
    private ValueEventListener notificationListener;

    public NotificationBadgeManager(String userId, TextView badgeTextView) {
        this.userId = userId;
        this.badgeTextView = badgeTextView;
        this.notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }

    public void startListening() {
        if (userId == null || userId.isEmpty() || badgeTextView == null) {
            Log.e("NotificationBadge", "Cannot start listening: missing userId or badgeTextView");
            return;
        }

        Log.d("NotificationBadge", "Starting notification listener for user: " + userId);

        // Remove old listener if exists
        if (notificationListener != null) {
            notificationsRef.removeEventListener(notificationListener);
        }

        // Listen for unread notifications
        notificationListener = notificationsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;

                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                            Boolean isRead = notificationSnapshot.child("read").getValue(Boolean.class);
                            if (isRead == null || !isRead) {
                                unreadCount++;
                            }
                        }

                        updateBadge(unreadCount);
                        Log.d("NotificationBadge", "Unread count for " + userId + ": " + unreadCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationBadge", "Error loading notifications: " + error.getMessage());
                        updateBadge(0);
                    }
                });
    }

    public void stopListening() {
        if (notificationListener != null) {
            notificationsRef.removeEventListener(notificationListener);
            notificationListener = null;
            Log.d("NotificationBadge", "Stopped notification listener");
        }
    }

    private void updateBadge(int count) {
        if (badgeTextView == null) return;

        // Run on UI thread
        badgeTextView.post(() -> {
            try {
                if (count > 0) {
                    badgeTextView.setText(String.valueOf(count > 99 ? "99+" : count));
                    badgeTextView.setVisibility(View.VISIBLE);
                    Log.d("NotificationBadge", "Badge updated: " + count);
                } else {
                    badgeTextView.setVisibility(View.GONE);
                    Log.d("NotificationBadge", "Badge hidden: 0 notifications");
                }
            } catch (Exception e) {
                Log.e("NotificationBadge", "Error updating badge: " + e.getMessage());
            }
        });
    }

    // ADD THIS STATIC METHOD - This is what you're calling from dashboards
    public static void getUnreadCount(String userId, OnUnreadCountListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onCountReceived(0);
            }
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications");
        ref.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                            Boolean isRead = notificationSnapshot.child("read").getValue(Boolean.class);
                            if (isRead == null || !isRead) {
                                unreadCount++;
                            }
                        }

                        if (listener != null) {
                            listener.onCountReceived(unreadCount);
                        }
                        Log.d("NotificationBadge", "Static getUnreadCount for " + userId + ": " + unreadCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationBadge", "Error getting unread count: " + error.getMessage());
                        if (listener != null) {
                            listener.onCountReceived(0);
                        }
                    }
                });
    }

    // ADD THIS INTERFACE
    public interface OnUnreadCountListener {
        void onCountReceived(int count);
    }
}