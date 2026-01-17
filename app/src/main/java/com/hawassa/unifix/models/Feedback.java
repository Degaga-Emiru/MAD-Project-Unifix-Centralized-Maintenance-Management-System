package com.hawassa.unifix.student;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.Feedback;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.shared.utils.NotificationUtils;
import java.text.SimpleDateFormat;
import java.util.*;

public class FeedbackActivity extends AppCompatActivity {

    // UI Components
    private AutoCompleteTextView actvReports;
    private TextView tvReportInfo, tvRatingText;
    private ChipGroup chipGroupFeedbackType;
    private Chip chipAcknowledgement, chipComplaint, chipSuggestion;
    private RatingBar ratingBar;
    private EditText etComments;
    private Button btnSubmit;
    private ProgressBar progressBar;

    // Data
    private String userId, userName, firebaseUid;
    private String selectedReportId = "";
    private String selectedFeedbackType = "";
    private MaintenanceReport selectedReport = null;

    // Firebase
    private DatabaseReference reportsRef, feedbackRef, usersRef;
    private List<MaintenanceReport> userReports = new ArrayList<>();
    private List<String> reportTitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Get user data from intent
        firebaseUid = getIntent().getStringExtra("firebaseUid");
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        feedbackRef = FirebaseDatabase.getInstance().getReference("feedback");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupListeners();
        loadUserReports();

        // Load user name if not provided
        if (userName == null || userName.isEmpty()) {
            loadUserName();
        }
    }

    private void initializeViews() {
        actvReports = findViewById(R.id.actvReports);
        tvReportInfo = findViewById(R.id.tvReportInfo);
        tvRatingText = findViewById(R.id.tvRatingText);
        chipGroupFeedbackType = findViewById(R.id.chipGroupFeedbackType);
        chipAcknowledgement = findViewById(R.id.chipAcknowledgement);
        chipComplaint = findViewById(R.id.chipComplaint);
        chipSuggestion = findViewById(R.id.chipSuggestion);
        ratingBar = findViewById(R.id.ratingBar);
        etComments = findViewById(R.id.etComments);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Report selection dropdown
        actvReports.setOnItemClickListener((parent, view, position, id) -> {
            if (position < userReports.size()) {
                selectedReport = userReports.get(position);
                selectedReportId = selectedReport.getReportId();
                updateReportInfo();
            }
        });

        // Feedback type selection
        chipGroupFeedbackType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int chipId = checkedIds.get(0);
                if (chipId == R.id.chipAcknowledgement) {
                    selectedFeedbackType = "acknowledgement";
                } else if (chipId == R.id.chipComplaint) {
                    selectedFeedbackType = "complaint";
                } else if (chipId == R.id.chipSuggestion) {
                    selectedFeedbackType = "suggestion";
                }
            }
        });

        // Rating change listener
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            updateRatingText(rating);
        });

        // Submit button
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void loadUserReports() {
        if (reportsRef == null || firebaseUid == null) return;

        showLoading(true);

        // Load user's reports for feedback
        reportsRef.orderByChild("reporterId").equalTo(firebaseUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userReports.clear();
                        reportTitles.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            MaintenanceReport report = ds.getValue(MaintenanceReport.class);
                            if (report != null) {
                                // Only show reports that are completed or in progress
                                String status = report.getStatus();
                                if ("Completed".equals(status) ||
                                        "In Progress".equals(status) ||
                                        "Assigned".equals(status)) {
                                    userReports.add(report);

                                    // Create display title
                                    String title = report.getCategory() + " - " +
                                            report.getBuildingBlock() + ", Room " +
                                            report.getRoomNumber() +
                                            " (" + report.getStatus() + ")";
                                    reportTitles.add(title);
                                }
                            }
                        }

                        if (userReports.isEmpty()) {
                            showNoReportsDialog();
                        } else {
                            setupReportDropdown();
                        }

                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FeedbackActivity.this,
                                "Failed to load reports: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void loadUserName() {
        if (firebaseUid == null) return;

        usersRef.child(firebaseUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        userName = name;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Feedback", "Error loading user name: " + error.getMessage());
            }
        });
    }

    private void setupReportDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                reportTitles
        );
        actvReports.setAdapter(adapter);

        // Enable the dropdown
        actvReports.setEnabled(true);
        actvReports.setHint("Select a report to provide feedback");
    }

    private void updateReportInfo() {
        if (selectedReport != null) {
            String info = "Report ID: " + selectedReport.getReportId() + "\n" +
                    "Category: " + selectedReport.getCategory() + "\n" +
                    "Location: " + selectedReport.getBuildingBlock() +
                    ", Room " + selectedReport.getRoomNumber() + "\n" +
                    "Status: " + selectedReport.getStatus() + "\n" +
                    "Submitted: " + getFormattedDate(selectedReport.getTimestamp());

            tvReportInfo.setText(info);
            tvReportInfo.setVisibility(View.VISIBLE);

            // Auto-select feedback type based on report status
            if ("Completed".equals(selectedReport.getStatus())) {
                chipAcknowledgement.setChecked(true);
                selectedFeedbackType = "acknowledgement";
            } else {
                chipComplaint.setChecked(true);
                selectedFeedbackType = "complaint";
            }
        }
    }

    private void updateRatingText(float rating) {
        String ratingText;
        if (rating == 0) {
            ratingText = "Select a rating";
        } else if (rating == 1) {
            ratingText = "Poor - Needs Improvement";
        } else if (rating == 2) {
            ratingText = "Fair - Below Expectations";
        } else if (rating == 3) {
            ratingText = "Good - Met Expectations";
        } else if (rating == 4) {
            ratingText = "Very Good - Exceeded Expectations";
        } else {
            ratingText = "Excellent - Outstanding Service";
        }
        tvRatingText.setText(ratingText);
    }

    private void submitFeedback() {
        // Validation
        if (selectedReport == null) {
            Toast.makeText(this, "Please select a report", Toast.LENGTH_SHORT).show();
            actvReports.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedFeedbackType)) {
            Toast.makeText(this, "Please select feedback type", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            ratingBar.requestFocus();
            return;
        }

        String comments = etComments.getText().toString().trim();

        // Check if feedback already exists for this report
        checkExistingFeedback();
    }

    private void checkExistingFeedback() {
        feedbackRef.orderByChild("reportId").equalTo(selectedReportId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Feedback already exists, ask user
                            showExistingFeedbackDialog();
                        } else {
                            // No existing feedback, proceed
                            saveFeedback();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Feedback", "Error checking existing feedback: " + error.getMessage());
                        saveFeedback();
                    }
                });
    }

    private void saveFeedback() {
        showLoading(true);

        // Generate feedback ID
        String feedbackId = "FDB-" + System.currentTimeMillis() + "-" +
                new Random().nextInt(1000);

        // Create feedback object
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(feedbackId);
        feedback.setReportId(selectedReportId);
        feedback.setUserId(firebaseUid);
        feedback.setUserName(userName != null ? userName : "Student");
        feedback.setReportTitle(selectedReport.getCategory() + " - " +
                selectedReport.getBuildingBlock() + ", Room " +
                selectedReport.getRoomNumber());
        feedback.setReportStatus(selectedReport.getStatus());
        feedback.setRating(ratingBar.getRating());
        feedback.setComments(etComments.getText().toString().trim());
        feedback.setFeedbackType(selectedFeedbackType);
        feedback.setTimestamp(System.currentTimeMillis());

        // Set requiresFollowUp for complaints with low ratings
        if ("complaint".equals(selectedFeedbackType) && ratingBar.getRating() <= 2) {
            feedback.setRequiresFollowUp(true);
        }

        // Save to Firebase
        feedbackRef.child(feedbackId).setValue(feedback)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d("Feedback", "Feedback saved successfully: " + feedbackId);

                        // Send notification to admin
                        sendAdminNotification(feedback);

                        // Show success dialog
                        showSuccessDialog(feedbackId);
                    } else {
                        Toast.makeText(FeedbackActivity.this,
                                "Failed to submit feedback: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("Feedback", "Save failed: " + task.getException().getMessage());
                    }
                });
    }

    private void sendAdminNotification(Feedback feedback) {
        String title = "";
        String message = "";

        switch (feedback.getFeedbackType()) {
            case "acknowledgement":
                title = "✅ Acknowledgement Received";
                message = userName + " acknowledged the completion of report: " +
                        feedback.getReportTitle();
                break;
            case "complaint":
                title = "⚠️ Complaint Received";
                message = userName + " submitted a complaint about report: " +
                        feedback.getReportTitle() +
                        " (Rating: " + feedback.getRating() + "/5)";
                break;
            case "suggestion":
                title = "💡 Suggestion Received";
                message = userName + " provided a suggestion for report: " +
                        feedback.getReportTitle();
                break;
        }

        // Send notification to all admin users
        NotificationUtils.sendNotificationToRole(
                "admin",
                title,
                message,
                "feedback",
                feedback.getFeedbackId(),
                firebaseUid,
                userName != null ? userName : "Student",
                FeedbackActivity.this // Add this


        );

        // Also send confirmation to student
        NotificationUtils.sendNotification(
                firebaseUid,
                "✅ Feedback Submitted",
                "Thank you for your feedback. It has been submitted successfully.",
                "feedback_confirmation",
                feedback.getFeedbackId(),
                "system",
                "System",
                FeedbackActivity.this // Add this

        );
    }

    private void showNoReportsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Reports Available");
        builder.setMessage("You don't have any completed or in-progress reports to provide feedback for.\n\n" +
                "Please submit a report first and wait for it to be processed.");
        builder.setPositiveButton("OK", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private void showExistingFeedbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Feedback Already Submitted");
        builder.setMessage("You have already submitted feedback for this report.\n\n" +
                "Would you like to update your feedback?");
        builder.setPositiveButton("Update", (dialog, which) -> {
            // Proceed with update (will overwrite existing)
            saveFeedback();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSuccessDialog(String feedbackId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Feedback Submitted Successfully!");
        builder.setMessage("Thank you for your valuable feedback.\n\n" +
                "Administrators have been notified and will review your feedback.\n\n" +
                "Feedback ID: " + feedbackId);
        builder.setPositiveButton("Done", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private void showLoading(boolean isLoading) {
        btnSubmit.setEnabled(!isLoading);
        btnSubmit.setText(isLoading ? "Submitting..." : "Submit Feedback");
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        // Disable inputs while loading
        actvReports.setEnabled(!isLoading);
        chipAcknowledgement.setEnabled(!isLoading);
        chipComplaint.setEnabled(!isLoading);
        chipSuggestion.setEnabled(!isLoading);
        ratingBar.setEnabled(!isLoading);
        etComments.setEnabled(!isLoading);
    }

    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }
}