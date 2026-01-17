package com.hawassa.unifix.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Feedback {
    private String feedbackId;
    private String reportId;
    private String userId;
    private String userName;
    private String reportTitle;
    private String reportStatus;
    private float rating;
    private String comments;
    private String feedbackType; // "acknowledgement", "complaint", "suggestion"
    private long timestamp;
    private boolean isAcknowledged;
    private boolean requiresFollowUp;

    // Required empty constructor for Firebase
    public Feedback() {
        this.timestamp = System.currentTimeMillis();
        this.isAcknowledged = false;
        this.requiresFollowUp = false;
    }

    // Constructor for feedback
    public Feedback(String feedbackId, String reportId, String userId, String userName,
                    String reportTitle, String reportStatus, float rating, String comments,
                    String feedbackType, long timestamp) {
        this.feedbackId = feedbackId;
        this.reportId = reportId;
        this.userId = userId;
        this.userName = userName;
        this.reportTitle = reportTitle;
        this.reportStatus = reportStatus;
        this.rating = rating;
        this.comments = comments;
        this.feedbackType = feedbackType;
        this.timestamp = timestamp;
        this.isAcknowledged = false;
        this.requiresFollowUp = false;
    }

    // Getters and Setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isAcknowledged() { return isAcknowledged; }
    public void setAcknowledged(boolean acknowledged) { isAcknowledged = acknowledged; }

    public boolean isRequiresFollowUp() { return requiresFollowUp; }
    public void setRequiresFollowUp(boolean requiresFollowUp) { this.requiresFollowUp = requiresFollowUp; }

    // Helper methods
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getRatingText() {
        if (rating >= 4.5) return "Excellent";
        else if (rating >= 3.5) return "Very Good";
        else if (rating >= 2.5) return "Good";
        else if (rating >= 1.5) return "Fair";
        else return "Poor";
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "feedbackId='" + feedbackId + '\'' +
                ", reportId='" + reportId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", rating=" + rating +
                ", feedbackType='" + feedbackType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}