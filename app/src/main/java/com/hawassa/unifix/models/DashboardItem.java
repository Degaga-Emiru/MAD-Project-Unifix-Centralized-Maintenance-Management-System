package com.hawassa.unifix.models;
public class DashboardItem {
    private int iconResId;
    private String title;
    private String subtitle;

    public DashboardItem(int iconResId, String title, String subtitle) {
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}