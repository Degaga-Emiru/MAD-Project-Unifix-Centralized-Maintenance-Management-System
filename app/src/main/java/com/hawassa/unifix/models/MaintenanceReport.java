package com.hawassa.unifix.models;

import java.io.Serializable;

public class MaintenanceReport implements Serializable {
    private String reportId;
    private String reporterId;
    private String reporterName;
    private String buildingBlock;
    private String roomNumber;
    private String category;
    private String description;
    private String status;
    private long timestamp;
    private String assignedTechnicianId;
    private String assignedTechnicianName;
    private Long completedTimestamp;
    private String technicianNotes;
    private String imageUrl;
    // --- ADD THESE TWO NEW FIELDS ---
    private Double reportLatitude;  // Latitude coordinate
    private Double reportLongitude; // Longitude coordinate

    // Required empty constructor for Firebase
    public MaintenanceReport() {}

    // Constructor
    public MaintenanceReport(String reportId, String reporterId, String reporterName,
                             String buildingBlock, String roomNumber, String category,
                             String description, String status, long timestamp,
                             String assignedTechnicianId, String assignedTechnicianName,
                             Long completedTimestamp, String technicianNotes, String imageUrl, Double reportLatitude, Double reportLongitude) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.buildingBlock = buildingBlock;
        this.roomNumber = roomNumber;
        this.category = category;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
        this.assignedTechnicianId = assignedTechnicianId;
        this.assignedTechnicianName = assignedTechnicianName;
        this.completedTimestamp = completedTimestamp;
        this.technicianNotes = technicianNotes;
        this.imageUrl = imageUrl;
        this.reportLatitude = reportLatitude;    // <-- Add this
        this.reportLongitude = reportLongitude;  // <-- Add this
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getBuildingBlock() { return buildingBlock; }
    public void setBuildingBlock(String buildingBlock) { this.buildingBlock = buildingBlock; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAssignedTechnicianId() { return assignedTechnicianId; }
    public void setAssignedTechnicianId(String assignedTechnicianId) { this.assignedTechnicianId = assignedTechnicianId; }

    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }

    public Long getCompletedTimestamp() { return completedTimestamp; }
    public void setCompletedTimestamp(Long completedTimestamp) { this.completedTimestamp = completedTimestamp; }

    public String getTechnicianNotes() { return technicianNotes; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Double getReportLatitude() {
        return reportLatitude;
    }

    public void setReportLatitude(Double reportLatitude) {
        this.reportLatitude = reportLatitude;
    }

    public Double getReportLongitude() {
        return reportLongitude;
    }

    public void setReportLongitude(Double reportLongitude) {
        this.reportLongitude = reportLongitude;
    }
}