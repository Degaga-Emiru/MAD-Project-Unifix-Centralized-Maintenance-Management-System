package com.hawassa.unifix.technician;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.technician.adapters.TaskAdapter;

import java.util.ArrayList;
import java.util.List;

public class AssignedTasksActivity extends AppCompatActivity {
    private static final String TAG = "AssignedTasksActivity";

    private RecyclerView rvTasks;
    private ProgressBar progressBar;
    private TextView tvNoTasks;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup chipGroupStatus;
    private Chip chipAll, chipAssigned, chipInProgress, chipCompleted;

    private DatabaseReference reportsRef;
    private List<MaintenanceReport> allTaskList = new ArrayList<>();
    private List<MaintenanceReport> filteredTaskList = new ArrayList<>();
    private TaskAdapter adapter;
    private String technicianId, technicianName, customUserId;
    private ValueEventListener tasksListener;
    private String selectedStatus = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_tasks);

        // Get data from intent
        technicianId = getIntent().getStringExtra("technicianId");
        technicianName = getIntent().getStringExtra("userName");
        customUserId = getIntent().getStringExtra("customUserId");

        Log.d(TAG, "Received - technicianId: " + technicianId +
                ", name: " + technicianName + ", customId: " + customUserId);

        if (technicianId == null || technicianId.isEmpty()) {
            Toast.makeText(this, "Technician ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupFirebase();
        loadAssignedTasks();
        setupRefreshListener();
        setupChipListeners();
    }

    private void initializeViews() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Assigned Tasks");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize views
        rvTasks = findViewById(R.id.rvTasks);
        progressBar = findViewById(R.id.progressBar);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipAll = findViewById(R.id.chipAll);
        chipAssigned = findViewById(R.id.chipAssigned);
        chipInProgress = findViewById(R.id.chipInProgress);
        chipCompleted = findViewById(R.id.chipCompleted);

        // Setup RecyclerView
        adapter = new TaskAdapter(filteredTaskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(MaintenanceReport task) {
                viewTaskDetails(task);
            }

            @Override
            public void onUpdateStatusClick(MaintenanceReport task) {
                updateTaskStatus(task);
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);
    }

    private void setupChipListeners() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If nothing selected, select "All"
                chipAll.setChecked(true);
                selectedStatus = "All";
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                selectedStatus = chip.getText().toString();
            }
            filterTasksByStatus();
        });
    }

    private void filterTasksByStatus() {
        filteredTaskList.clear();

        if (selectedStatus.equals("All")) {
            filteredTaskList.addAll(allTaskList);
        } else {
            for (MaintenanceReport task : allTaskList) {
                if (task.getStatus().equals(selectedStatus)) {
                    filteredTaskList.add(task);
                }
            }
        }

        updateUI();
    }

    private void setupFirebase() {
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
    }

    private void loadAssignedTasks() {
        showLoading(true);

        // Remove old listener if exists
        if (tasksListener != null) {
            reportsRef.removeEventListener(tasksListener);
        }

        // Query for tasks assigned to this technician
        tasksListener = reportsRef.orderByChild("assignedTechnicianId").equalTo(technicianId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        allTaskList.clear();

                        if (!dataSnapshot.exists()) {
                            Log.d(TAG, "No tasks found for technician: " + technicianId);
                            runOnUiThread(() -> {
                                updateUI();
                                swipeRefreshLayout.setRefreshing(false);
                                showLoading(false);
                            });
                            return;
                        }

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MaintenanceReport report = snapshot.getValue(MaintenanceReport.class);
                            if (report != null) {
                                allTaskList.add(report);
                            }
                        }

                        runOnUiThread(() -> {
                            filterTasksByStatus();
                            swipeRefreshLayout.setRefreshing(false);
                            showLoading(false);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Firebase error: " + databaseError.getMessage());
                        runOnUiThread(() -> {
                            showLoading(false);
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(AssignedTasksActivity.this,
                                    "Failed to load tasks", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateUI() {
        if (filteredTaskList.isEmpty()) {
            tvNoTasks.setText("No " + selectedStatus.toLowerCase() + " tasks");
            tvNoTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvTasks.setVisibility(View.GONE);
            tvNoTasks.setVisibility(View.GONE);
        }
    }

    private void setupRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener(this::loadAssignedTasks);
    }

    private void viewTaskDetails(MaintenanceReport task) {
        try {
            Intent intent = new Intent(this, UpdateStatusActivity.class);
            intent.putExtra("reportId", task.getReportId());
            intent.putExtra("technicianId", technicianId);
            intent.putExtra("technicianName", technicianName != null ? technicianName : "Technician");
            intent.putExtra("customUserId", customUserId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening task details: " + e.getMessage());
            Toast.makeText(this, "Error opening task", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTaskStatus(MaintenanceReport task) {
        try {
            Intent intent = new Intent(this, UpdateStatusActivity.class);
            intent.putExtra("reportId", task.getReportId());
            intent.putExtra("technicianId", technicianId);
            intent.putExtra("technicianName", technicianName != null ? technicianName : "Technician");
            intent.putExtra("customUserId", customUserId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening update status: " + e.getMessage());
            Toast.makeText(this, "Error updating task", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignedTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            reportsRef.removeEventListener(tasksListener);
        }
    }
}