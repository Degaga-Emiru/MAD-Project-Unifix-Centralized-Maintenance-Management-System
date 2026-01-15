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
