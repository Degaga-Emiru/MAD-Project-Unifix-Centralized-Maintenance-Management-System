package com.hawassa.unifix.student;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.student.adapters.ReportAdapter;
import java.util.ArrayList;
import java.util.List;

public class ReportHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private List<MaintenanceReport> reportList = new ArrayList<>();
    private List<MaintenanceReport> filteredList = new ArrayList<>();
    private DatabaseReference reportsRef;
    private String userId;

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupStatus;
    private Chip chipAll, chipPending, chipInProgress, chipCompleted;

    // Filter states
    private String currentSearchQuery = "";
    private String currentStatusFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        setupToolbar();

        // Get user ID
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            userId = getIntent().getStringExtra("firebaseUid");
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupChipListeners(); // Changed to new method
        setupSearchListener();
        loadUserReports();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView);

            if (recyclerView == null) {
                throw new RuntimeException("RecyclerView not found");
            }

            adapter = new ReportAdapter(filteredList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            reportsRef = FirebaseDatabase.getInstance().getReference("reports");

            // Initialize other views
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            etSearch = findViewById(R.id.etSearch);
            chipGroupStatus = findViewById(R.id.chipGroupStatus);
            chipAll = findViewById(R.id.chipAll);
            chipPending = findViewById(R.id.chipPending);
            chipInProgress = findViewById(R.id.chipInProgress);
            chipCompleted = findViewById(R.id.chipCompleted);

            // Set "All" as default selected
            chipAll.setChecked(true);

        } catch (Exception e) {
            Log.e("ReportHistory", "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading report history", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupChipListeners() {
        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserReports();
            swipeRefreshLayout.setRefreshing(false);
        });

        // FIXED: Use individual click listeners for each chip
        View.OnClickListener chipClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // First, uncheck ALL chips
                chipAll.setChecked(false);
                chipPending.setChecked(false);
                chipInProgress.setChecked(false);
                chipCompleted.setChecked(false);

                // Now check the clicked chip
                Chip clickedChip = (Chip) v;
                clickedChip.setChecked(true);

                // Update current filter
                int chipId = v.getId();
                if (chipId == R.id.chipAll) {
                    currentStatusFilter = "ALL";
                } else if (chipId == R.id.chipPending) {
                    currentStatusFilter = "PENDING";
                } else if (chipId == R.id.chipInProgress) {
                    currentStatusFilter = "IN_PROGRESS";
                } else if (chipId == R.id.chipCompleted) {
                    currentStatusFilter = "COMPLETED";
                }

                Log.d("CHIP_DEBUG", "Chip clicked: " + currentStatusFilter);
                applyFilters();
            }
        };

        chipAll.setOnClickListener(chipClickListener);
        chipPending.setOnClickListener(chipClickListener);
        chipInProgress.setOnClickListener(chipClickListener);
        chipCompleted.setOnClickListener(chipClickListener);
    }

    private void setupSearchListener() {
        // Setup search listener - real-time filtering
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserReports() {
        if (reportsRef == null || userId == null) return;

        reportsRef.orderByChild("reporterId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            reportList.clear();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                MaintenanceReport report = snapshot.getValue(MaintenanceReport.class);
                                if (report != null) {
                                    reportList.add(report);
                                }
                            }

                            // Sort by timestamp (newest first)
                            reportList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                            // Apply current filters
                            applyFilters();

                            Log.d("ReportHistory", "Loaded " + reportList.size() + " reports");

                        } catch (Exception e) {
                            Log.e("ReportHistory", "Error processing reports: " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("ReportHistory", "Database error: " + databaseError.getMessage());
                        Toast.makeText(ReportHistoryActivity.this,
                                "Failed to load reports: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        filteredList.clear();

        Log.d("FILTER_DEBUG", "Applying filters - Status: " + currentStatusFilter +
                ", Search: '" + currentSearchQuery + "', Total reports: " + reportList.size());

        for (MaintenanceReport report : reportList) {
            // 1. Apply status filter FIRST
            boolean statusMatches = false;
            String reportStatus = report.getStatus() != null ? report.getStatus().toLowerCase() : "";

            switch (currentStatusFilter) {
                case "ALL":
                    statusMatches = true;
                    break;
                case "PENDING":
                    statusMatches = "submitted".equals(reportStatus);
                    break;
                case "IN_PROGRESS":
                    statusMatches = "assigned".equals(reportStatus) ||
                            "in progress".equals(reportStatus) ||
                            "in_progress".equals(reportStatus);
                    break;
                case "COMPLETED":
                    statusMatches = "completed".equals(reportStatus);
                    break;
            }

            if (!statusMatches) {
                continue;
            }

            // 2. Apply search filter
            boolean searchMatches = currentSearchQuery.isEmpty();
            if (!searchMatches) {
                // Check multiple fields
                String searchLower = currentSearchQuery.toLowerCase();

                // Check category
                if (report.getCategory() != null &&
                        report.getCategory().toLowerCase().contains(searchLower)) {
                    searchMatches = true;
                }

                // Check location
                if (!searchMatches && report.getBuildingBlock() != null && report.getRoomNumber() != null) {
                    String location = (report.getBuildingBlock() + " " + report.getRoomNumber()).toLowerCase();
                    if (location.contains(searchLower)) {
                        searchMatches = true;
                    }
                }

                // Check description
                if (!searchMatches && report.getDescription() != null &&
                        report.getDescription().toLowerCase().contains(searchLower)) {
                    searchMatches = true;
                }

                // Check report ID
                if (!searchMatches && report.getReportId() != null &&
                        report.getReportId().toLowerCase().contains(searchLower)) {
                    searchMatches = true;
                }
            }

            if (searchMatches) {
                filteredList.add(report);
            }
        }

        Log.d("FILTER_DEBUG", "After filtering: " + filteredList.size() + " reports");

        // Update adapter
        adapter.notifyDataSetChanged();

        // Show/hide empty state
        updateEmptyState();
    }

    private void updateEmptyState() {
        runOnUiThread(() -> {
            TextView tvEmpty = findViewById(R.id.tvEmpty);

            if (filteredList.isEmpty()) {
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);

                    // Customize empty message based on filters
                    if (!currentSearchQuery.isEmpty()) {
                        tvEmpty.setText("No reports found for \"" + currentSearchQuery + "\"");
                    } else if (!currentStatusFilter.equals("ALL")) {
                        String statusText = currentStatusFilter.replace("_", " ").toLowerCase();
                        tvEmpty.setText("No " + statusText + " reports found");
                    } else {
                        tvEmpty.setText("No reports found");
                    }
                }
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
            } else {
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.GONE);
                }
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}