package com.hawassa.unifix.models;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsData {
    private int totalReports;
    private int pendingReports;
    private int inProgressReports;
    private int completedReports;
    private Map<String, Integer> categoryData;
    private Map<String, Integer> monthlyData;

    public AnalyticsData() {
        categoryData = new HashMap<>();
        monthlyData = new HashMap<>();
    }

    // Getters and Setters
    public int getTotalReports() { return totalReports; }
    public void setTotalReports(int totalReports) { this.totalReports = totalReports; }

    public int getPendingReports() { return pendingReports; }
    public void setPendingReports(int pendingReports) { this.pendingReports = pendingReports; }

    public int getInProgressReports() { return inProgressReports; }
    public void setInProgressReports(int inProgressReports) { this.inProgressReports = inProgressReports; }

    public int getCompletedReports() { return completedReports; }
    public void setCompletedReports(int completedReports) { this.completedReports = completedReports; }

    public Map<String, Integer> getCategoryData() { return categoryData; }
    public void setCategoryData(Map<String, Integer> categoryData) { this.categoryData = categoryData; }

    public void addCategoryData(String category, int count) {
        this.categoryData.put(category, count);
    }

    public Map<String, Integer> getMonthlyData() { return monthlyData; }
    public void setMonthlyData(Map<String, Integer> monthlyData) { this.monthlyData = monthlyData; }

    public void addMonthlyData(String month, int count) {
        this.monthlyData.put(month, count);
    }

    // Helper methods
    public double getCompletionRate() {
        if (totalReports == 0) return 0;
        return (completedReports * 100.0) / totalReports;
    }

    public double getAverageResolutionTime() {
        // Calculate average resolution time (in days)
        // Implementation depends on your data structure
        return 0;
    }
}