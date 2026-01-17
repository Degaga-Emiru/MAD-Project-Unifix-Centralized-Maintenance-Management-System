package com.hawassa.unifix.shared.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.Notification;
import com.hawassa.unifix.models.User;
import com.hawassa.unifix.shared.NotificationsActivity;

import java.util.HashMap;
import java.util.Map;

public class NotificationUtils {

    private static final DatabaseReference notificationsRef =
            FirebaseDatabase.getInstance().getReference("notifications");

    private static final DatabaseReference usersRef =
            FirebaseDatabase.getInstance().getReference("users");

    // Notification channel constants
    private static final String CHANNEL_ID = "unifix_notifications_channel";
    private static final String CHANNEL_NAME = "Unifix Notifications";
    private static final String CHANNEL_DESC = "Maintenance report updates and notifications";

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    /**
     * Send notification to a specific user - WITH PUSH NOTIFICATION
     */
    public static void sendNotification(String userId, String title, String message,
                                        String type, String reportId,
                                        String senderId, String senderName, Context context) {

        if (userId == null || userId.isEmpty()) {
            Log.e("NOTIFICATION_UTILS", "Cannot send notification: userId is null/empty");
            return;
        }

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) {
            notificationId = "NOTIF-" + System.currentTimeMillis() + "-" +
                    (int)(Math.random() * 10000);
        }

        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReportId(reportId);
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRead(false);
        notification.setTimestamp(System.currentTimeMillis());
// Create final variables BEFORE the lambda
        final Context finalContext = context;
        final String finalTitle = title;
        final String finalMessage = message;
        final String finalType = type;
        final String finalReportId = reportId;
        final String finalSenderName = senderName;
        final String finalNotificationId = notificationId;
        // Save notification to Firebase database
        notificationsRef.child(finalNotificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NOTIFICATION_UTILS", "✅ Notification saved to Firebase: " + finalTitle);

                    // Send local push notification using final variables
                    if (finalContext != null) {
                        sendLocalPushNotification(finalContext, finalTitle, finalMessage,
                                finalType, finalReportId, finalSenderName, finalNotificationId);
                    }

                    // Also send in-app broadcast if needed
                    // NotificationBadgeManager.updateBadgeForUser(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATION_UTILS", "❌ Failed to save to Firebase: " + e.getMessage());
                });
    }

    /**
     * Send LOCAL PUSH NOTIFICATION (without FCM server)
     */
    private static void sendLocalPushNotification(Context context, String title, String message,
                                                  String type, String reportId, String senderName,
                                                  String notificationId) {
        if (context == null) {
            Log.e("NOTIFICATION_UTILS", "Context is null, cannot send push notification");
            return;
        }

        try {
            // Create notification channel for Android O and above
            createNotificationChannel(context);

            // Create intent for when notification is tapped
            Intent intent = new Intent(context, NotificationsActivity.class);
            // You need to pass actual user ID - you might need to get it from SharedPreferences
            // intent.putExtra("userId", getCurrentUserId());
            intent.putExtra("notificationId", notificationId);

            if (reportId != null && !reportId.isEmpty()) {
                intent.putExtra("reportId", reportId);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    generateNotificationId(), intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            // Get the appropriate icon
            int iconResource = getNotificationIcon(type);

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(iconResource)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            // Add sender name if available
            if (senderName != null && !senderName.isEmpty()) {
                builder.setSubText("From: " + senderName);
            }

            // Show notification with permission check
            showNotificationWithPermissionCheck(context, builder, generateNotificationId());

            Log.d("NOTIFICATION_UTILS", "📱 Local push notification prepared: " + title);

        } catch (Exception e) {
            Log.e("NOTIFICATION_UTILS", "Error sending push notification: " + e.getMessage(), e);
        }
    }

    /**
     * Generate unique notification ID
     */
    private static int generateNotificationId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    /**
     * Show notification with proper permission check
     */
    private static void showNotificationWithPermissionCheck(Context context,
                                                            NotificationCompat.Builder builder,
                                                            int notificationId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Check notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires explicit permission
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, show notification
                    notificationManager.notify(notificationId, builder.build());
                    Log.d("NOTIFICATION_UTILS", "📱 Push notification sent (Android 13+), ID: " + notificationId);
                } else {
                    // Permission not granted
                    Log.w("NOTIFICATION_UTILS", "Notification permission not granted for Android 13+");
                }
            } else {
                // For Android 12 and below
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(notificationId, builder.build());
                    Log.d("NOTIFICATION_UTILS", "📱 Push notification sent, ID: " + notificationId);
                } else {
                    Log.w("NOTIFICATION_UTILS", "Notifications not enabled for this app");
                }
            }
        } catch (Exception e) {
            Log.e("NOTIFICATION_UTILS", "Error showing notification: " + e.getMessage(), e);
        }
    }

    /**
     * Create notification channel for Android O+
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(CHANNEL_DESC);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
                channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                .build());

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d("NOTIFICATION_UTILS", "Notification channel created");
                }
            } catch (Exception e) {
                Log.e("NOTIFICATION_UTILS", "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    /**
     * Get appropriate icon for notification type using if-else
     */
    private static int getNotificationIcon(String type) {
        if (type == null) {
            return R.drawable.ic_notifications;
        }

        // Use if-else instead of switch
        if (type.equals(Types.NEW_REPORT)) {
            return R.drawable.ic_report;
        } else if (type.equals(Types.TASK_ASSIGNED)) {
            return R.drawable.ic_assign_task;
        } else if (type.equals(Types.STATUS_UPDATE)) {
            return R.drawable.ic_update;
        } else if (type.equals(Types.FEEDBACK)) {
            return R.drawable.ic_feedback;
        } else if (type.equals(Types.REPORT_CONFIRMATION)) {
            return R.drawable.ic_check_circle;
        } else if (type.equals(Types.ADMIN_ALERT)) {
            return R.drawable.ic_warning;
        } else if (type.equals(Types.SYSTEM)) {
            return R.drawable.ic_notifications; // Or create ic_system_notification
        } else {
            return R.drawable.ic_notifications;
        }
    }

    /**
     * Send notification to all users with a specific role - WITH PUSH
     */
    public static void sendNotificationToRole(String role, String title, String message,
                                              String type, String reportId,
                                              String senderId, String senderName, Context context) {

        Log.d("NOTIFICATION_UTILS", "Sending to role: " + role + " - " + title);

        usersRef.orderByChild("role").equalTo(role)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;

                        if (!dataSnapshot.exists()) {
                            Log.w("NOTIFICATION_UTILS", "No users found with role: " + role);
                            return;
                        }

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            User user = userSnapshot.getValue(User.class);

                            if (user != null && "active".equals(user.getStatus())) {
                                sendNotification(userId, title, message, type,
                                        reportId, senderId, senderName, context);
                                count++;
                            }
                        }

                        Log.d("NOTIFICATION_UTILS", "📤 Sent to " + count + " " + role + "(s)");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("NOTIFICATION_UTILS", "❌ Failed to get " + role + " users: " +
                                databaseError.getMessage());
                    }
                });
    }

    /**
     * Send system notification (from system to user) - WITH PUSH
     */
    public static void sendSystemNotification(String userId, String title, String message,
                                              String type, String reportId, Context context) {
        sendNotification(userId, title, message, type, reportId, "system", "System", context);
    }

    /**
     * Send quick notification (for testing)
     */
    public static void sendQuickNotification(Context context, String title, String message) {
        try {
            createNotificationChannel(context);

            int notificationId = generateNotificationId();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            showNotificationWithPermissionCheck(context, builder, notificationId);

            Log.d("NOTIFICATION_UTILS", "Test notification sent: " + title);
        } catch (Exception e) {
            Log.e("NOTIFICATION_UTILS", "Error sending test notification: " + e.getMessage());
        }
    }

    /**
     * Request notification permission (call this from your main activity)
     */
    public static void requestNotificationPermission(android.app.Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    /**
     * Check if notification permission is granted
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // For Android 12 and below, notifications are enabled by default
        return true;
    }

    /**
     * Get unread notification count for a user
     */
    public static void getUnreadCount(String userId, UnreadCountListener listener) {
        if (userId == null) {
            if (listener != null) {
                listener.onCountReceived(0);
            }
            return;
        }

        notificationsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Boolean isRead = snapshot.child("read").getValue(Boolean.class);
                            if (isRead == null || !isRead) {
                                count++;
                            }
                        }
                        if (listener != null) {
                            listener.onCountReceived(count);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("NOTIFICATION_UTILS", "Failed to get unread count: " +
                                databaseError.getMessage());
                        if (listener != null) {
                            listener.onCountReceived(0);
                        }
                    }
                });
    }

    /**
     * Mark notification as read
     */
    public static void markAsRead(String notificationId) {
        if (notificationId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);

        notificationsRef.child(notificationId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NOTIFICATION_UTILS", "✅ Marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATION_UTILS", "❌ Failed to mark as read: " + e.getMessage());
                });
    }

    /**
     * Mark all notifications as read for a user
     */
    public static void markAllAsRead(String userId) {
        if (userId == null) return;

        notificationsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Map<String, Object> updates = new HashMap<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String notificationId = snapshot.getKey();
                            updates.put(notificationId + "/read", true);
                        }

                        if (!updates.isEmpty()) {
                            notificationsRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("NOTIFICATION_UTILS", "✅ Marked all as read for " + userId);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("NOTIFICATION_UTILS", "Failed to mark all as read: " +
                                databaseError.getMessage());
                    }
                });
    }

    /**
     * Delete a specific notification
     */
    public static void deleteNotification(String notificationId) {
        if (notificationId == null) return;

        notificationsRef.child(notificationId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("NOTIFICATION_UTILS", "✅ Deleted notification: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATION_UTILS", "❌ Failed to delete: " + e.getMessage());
                });
    }

    /**
     * Clear all notifications for a user
     */
    public static void clearAllNotifications(String userId) {
        if (userId == null) return;

        notificationsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        Log.d("NOTIFICATION_UTILS", "✅ Cleared all notifications for " + userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("NOTIFICATION_UTILS", "❌ Failed to clear: " + databaseError.getMessage());
                    }
                });
    }

    /**
     * Predefined notification types
     */
    public static class Types {
        public static final String NEW_REPORT = "new_report";
        public static final String TASK_ASSIGNED = "task_assigned";
        public static final String STATUS_UPDATE = "status_update";
        public static final String FEEDBACK = "feedback";
        public static final String REPORT_CONFIRMATION = "report_confirmation";
        public static final String ADMIN_ALERT = "admin_alert";
        public static final String SYSTEM = "system";
    }

    /**
     * Interface for unread count callback
     */
    public interface UnreadCountListener {
        void onCountReceived(int count);
    }
}