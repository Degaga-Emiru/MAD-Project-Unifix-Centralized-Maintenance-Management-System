package com.hawassa.unifix.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type; // new_report, task_assigned, status_update, feedback, admin_alert
    private String reportId;
    private String senderId;
    private String senderName;
    private boolean read;
    private long timestamp;

    // Required empty constructor for Firebase
    public Notification() {
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for creating notifications programmatically
    public Notification(String userId, String title, String message,
                        String type, String reportId) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.reportId = reportId;
        this.notificationId = generateNotificationId();
    }

    // Full constructor
    public Notification(String notificationId, String userId, String title,
                        String message, String type, String reportId,
                        String senderId, String senderName, boolean read, long timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.reportId = reportId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.read = read;
        this.timestamp = timestamp;
    }

    // Helper method to generate notification ID (doesn't depend on Firebase in constructor)
    @Exclude
    private String generateNotificationId() {
        return "NOTIF-" + System.currentTimeMillis() + "-" +
                (int)(Math.random() * 10000);
    }

    // --- Getters ---
    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getReportId() {
        return reportId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public boolean isRead() {
        return read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // --- Setters ---
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper methods
    @Exclude
    public String getFormattedTime() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            return (diff / 60000) + " minutes ago";
        } else if (diff < 86400000) { // Less than 1 day
            return (diff / 3600000) + " hours ago";
        } else {
            return (diff / 86400000) + " days ago";
        }
    }

    @Exclude
    public boolean isAdminNotification() {
        return "new_report".equals(type) || "admin_alert".equals(type);
    }

    @Exclude
    public boolean isStudentNotification() {
        return "task_assigned".equals(type) || "status_update".equals(type);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", reportId='" + reportId + '\'' +
                ", read=" + read +
                ", timestamp=" + timestamp +
                '}';
    }
}