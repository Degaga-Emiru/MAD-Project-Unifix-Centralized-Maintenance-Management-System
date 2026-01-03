package com.hawassa.unifix.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import java.text.SimpleDateFormat;
import java.util.*;

public class AnalyticsActivity extends AppCompatActivity {
    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvTotalReports, tvBusiestDay, tvCompletionRate, tvAvgResolutionTime;
    private DatabaseReference reportsRef;
    private List<MaintenanceReport> allReports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // Setup toolbar FIRST
        setupToolbar();

        try {
            initializeViews();
            reportsRef = FirebaseDatabase.getInstance().getReference("reports");
            loadReportsForAnalytics();
        } catch (Exception e) {
            Log.e("AnalyticsActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading analytics", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set click listener for navigation icon
        toolbar.setNavigationOnClickListener(v -> {
            // This will handle the back navigation
            onBackPressed();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Optional: Add animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void initializeViews() {
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        tvTotalReports = findViewById(R.id.tvTotalReports); // Added this
        tvBusiestDay = findViewById(R.id.tvBusiestDay);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvAvgResolutionTime = findViewById(R.id.tvAvgResolutionTime);

        // Configure bar chart
        if (barChart != null) {
            barChart.getDescription().setEnabled(false);
            barChart.setDrawGridBackground(false);
            barChart.setDrawBarShadow(false);
            barChart.setPinchZoom(false);
            barChart.setDrawValueAboveBar(true);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
        }

        // Configure pie chart
        if (pieChart != null) {
            pieChart.getDescription().setEnabled(false);
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleColor(Color.WHITE);
            pieChart.setTransparentCircleColor(Color.WHITE);
            pieChart.setTransparentCircleAlpha(110);
            pieChart.setHoleRadius(58f);
            pieChart.setTransparentCircleRadius(61f);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterText("Reports by Status");
            pieChart.setCenterTextSize(14f);
        }
    }

    private void loadReportsForAnalytics() {
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    allReports.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        MaintenanceReport report = snapshot.getValue(MaintenanceReport.class);
                        if (report != null) {
                            allReports.add(report);
                        }
                    }
                    updateAnalyticsUI();
                    createCharts();
                } catch (Exception e) {
                    Log.e("AnalyticsActivity", "Error processing data: " + e.getMessage(), e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("AnalyticsActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(AnalyticsActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAnalyticsUI() {
        try {
            int totalReports = allReports.size();

            // Update total reports
            if (tvTotalReports != null) {
                tvTotalReports.setText("Total Reports: " + totalReports);
            }

            // Calculate completion rate
            int completedCount = 0;
            for (MaintenanceReport report : allReports) {
                if ("Completed".equals(report.getStatus())) {
                    completedCount++;
                }
            }

            float completionRate = totalReports > 0 ? (completedCount * 100f) / totalReports : 0;

            if (tvCompletionRate != null) {
                tvCompletionRate.setText(String.format("%.1f%%", completionRate));
            }

            // Calculate average resolution time
            long totalTime = 0;
            for (MaintenanceReport report : allReports) {
                if ("Completed".equals(report.getStatus())) {
                    totalTime += 24; // Placeholder: 24 hours average
                }
            }
            long avgTime = completedCount > 0 ? totalTime / completedCount : 0;

            if (tvAvgResolutionTime != null) {
                tvAvgResolutionTime.setText(avgTime + " hours");
            }

            // Find busiest day
            Map<String, Integer> dayCount = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
            for (MaintenanceReport report : allReports) {
                try {
                    Date date = new Date(report.getTimestamp());
                    String day = sdf.format(date);
                    dayCount.put(day, dayCount.getOrDefault(day, 0) + 1);
                } catch (Exception e) {
                    Log.w("AnalyticsActivity", "Error parsing date");
                }
            }

            String busiestDay = "None";
            int maxDayCount = 0;
            for (Map.Entry<String, Integer> entry : dayCount.entrySet()) {
                if (entry.getValue() > maxDayCount) {
                    maxDayCount = entry.getValue();
                    busiestDay = entry.getKey();
                }
            }

            if (tvBusiestDay != null) {
                tvBusiestDay.setText(busiestDay);
            }
        } catch (Exception e) {
            Log.e("AnalyticsActivity", "Error updating UI: " + e.getMessage(), e);
        }
    }

    private void createCharts() {
        try {
            // Bar chart: Reports by category
            Map<String, Integer> categoryMap = new HashMap<>();
            for (MaintenanceReport report : allReports) {
                String category = report.getCategory();
                if (category != null) {
                    categoryMap.put(category, categoryMap.getOrDefault(category, 0) + 1);
                }
            }

            List<BarEntry> barEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int i = 0;

            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                barEntries.add(new BarEntry(i, entry.getValue()));
                labels.add(entry.getKey());
                i++;
            }

            if (!barEntries.isEmpty() && barChart != null) {
                BarDataSet dataSet = new BarDataSet(barEntries, "Reports by Category");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.9f);
                barData.setValueTextSize(10f);
                barData.setValueTextColor(Color.BLACK);

                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                barChart.setData(barData);
                barChart.invalidate();
            }

            // Pie chart: Reports by status
            Map<String, Integer> statusMap = new HashMap<>();
            for (MaintenanceReport report : allReports) {
                String status = report.getStatus();
                if (status != null) {
                    statusMap.put(status, statusMap.getOrDefault(status, 0) + 1);
                }
            }

            List<PieEntry> pieEntries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : statusMap.entrySet()) {
                pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            if (!pieEntries.isEmpty() && pieChart != null) {
                PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
                pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                pieDataSet.setValueTextSize(12f);
                pieDataSet.setValueTextColor(Color.WHITE);

                PieData pieData = new PieData(pieDataSet);
                pieChart.setData(pieData);
                pieChart.invalidate();
            }
        } catch (Exception e) {
            Log.e("AnalyticsActivity", "Error creating charts: " + e.getMessage(), e);
        }
    }
}