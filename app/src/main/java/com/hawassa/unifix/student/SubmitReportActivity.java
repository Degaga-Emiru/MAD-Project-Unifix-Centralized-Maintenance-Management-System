package com.hawassa.unifix.student;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.MaintenanceReport;
import com.hawassa.unifix.shared.utils.NotificationUtils;

import java.text.SimpleDateFormat;
import java.util.*;

public class SubmitReportActivity extends AppCompatActivity {

    // UI Elements
    private EditText etBuildingBlock, etRoomNumber, etDescription;
    private AutoCompleteTextView spCategory;
    private Button btnSubmit, btnAttach;
    private ImageView ivAttachment;
    private ProgressBar progressBar;

    // NEW: Location UI Elements
    private TextView tvLocationStatus;
    private Button btnGetLocation;

    // Data
    private String firebaseUid, customUserId, userName;
    private String selectedCategory = "";
    private Uri imageUri = null;

    // NEW: Location Data
    private Double currentLatitude = null;
    private Double currentLongitude = null;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;

    // Firebase
    private DatabaseReference reportsRef, usersRef;
    private StorageReference storageRef;

    // Constants
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String[] CATEGORIES = {
            "Electrical", "Plumbing", "HVAC", "Carpentry",
            "Painting", "Furniture", "Computer", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_report);

        // Get user data
        Intent intent = getIntent();
        firebaseUid = intent.getStringExtra("firebaseUid");
        customUserId = intent.getStringExtra("userId");
        userName = intent.getStringExtra("userName");

        if (firebaseUid == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) firebaseUid = currentUser.getUid();
        }

        // Initialize Firebase
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageRef = FirebaseStorage.getInstance().getReference("report_images");

        // NEW: Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupCategoryDropdown();
        setupClickListeners();

        // NEW: Request location permission and get location
        checkLocationPermission();
    }

    private void initializeViews() {
        etBuildingBlock = findViewById(R.id.etBuildingBlock);
        etRoomNumber = findViewById(R.id.etRoomNumber);
        etDescription = findViewById(R.id.etDescription);
        spCategory = findViewById(R.id.spCategory);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAttach = findViewById(R.id.btnAttach);
        ivAttachment = findViewById(R.id.ivAttachment);
        progressBar = findViewById(R.id.progressBar);

        // NEW: Initialize location views
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnGetLocation = findViewById(R.id.btnGetLocation);

        if (progressBar == null) {
            progressBar = new ProgressBar(this);
        }
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CATEGORIES
        );
        spCategory.setAdapter(adapter);

        spCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = CATEGORIES[position];
                Log.d("REPORT", "Selected category: " + selectedCategory);
            }
        });
    }

    private void setupClickListeners() {
        btnAttach.setOnClickListener(v -> openImageChooser());
        btnSubmit.setOnClickListener(v -> submitReport());

        // NEW: Location refresh button
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());

        // Clear attachment
        ivAttachment.setOnClickListener(v -> {
            imageUri = null;
            ivAttachment.setVisibility(View.GONE);
            btnAttach.setText("📷 Attach Image");
        });
    }

    // NEW: Location Permission Methods
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get location
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getCurrentLocation();
            } else {
                // Permission denied
                tvLocationStatus.setText("Location permission denied");
                tvLocationStatus.setTextColor(getResources().getColor(R.color.red));
                btnGetLocation.setVisibility(View.GONE);
                Toast.makeText(this,
                        "Location permission is needed for technician navigation",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // NEW: Get Current Location
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }

        // Show loading
        tvLocationStatus.setText("Getting location...");
        btnGetLocation.setEnabled(false);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    btnGetLocation.setEnabled(true);

                    if (location != null) {
                        // Got location!
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        // Update UI
                        String locationText = String.format("%.6f, %.6f", currentLatitude, currentLongitude);
                        tvLocationStatus.setText(locationText);
                        tvLocationStatus.setTextColor(getResources().getColor(R.color.green));

                        Log.d("LOCATION", "Got location: " + currentLatitude + ", " + currentLongitude);

                        // Optional: Show success message
                        Toast.makeText(this,
                                "Location captured successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Location is null (GPS might be off)
                        tvLocationStatus.setText("Location unavailable (GPS might be off)");
                        tvLocationStatus.setTextColor(getResources().getColor(R.color.orange));
                        btnGetLocation.setText("Try Again");

                        Log.w("LOCATION", "Location is null. GPS might be off.");
                        Toast.makeText(this,
                                "Could not get current location. Please ensure GPS is on and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnGetLocation.setEnabled(true);
                    tvLocationStatus.setText("Error getting location");
                    tvLocationStatus.setTextColor(getResources().getColor(R.color.red));
                    btnGetLocation.setText("Retry");

                    Log.e("LOCATION", "Error getting location: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed to get location: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivAttachment.setImageURI(imageUri);
            ivAttachment.setVisibility(View.VISIBLE);
            btnAttach.setText("🔄 Change Image");
            Log.d("REPORT", "Image selected: " + imageUri.toString());
        }
    }

    private void submitReport() {
        // Get form data
        String buildingBlock = etBuildingBlock.getText().toString().trim();
        String roomNumber = etRoomNumber.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(buildingBlock)) {
            etBuildingBlock.setError("Building block is required");
            etBuildingBlock.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(roomNumber)) {
            etRoomNumber.setError("Room number is required");
            etRoomNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedCategory)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < 10) {
            etDescription.setError("Please provide more details (minimum 10 characters)");
            etDescription.requestFocus();
            return;
        }

        // NEW: Location validation (optional but recommended)
        if (currentLatitude == null || currentLongitude == null) {
            // Ask user if they want to proceed without location
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Not Available");
            builder.setMessage("Your location could not be captured. Technicians won't be able to navigate directly to your location. Do you want to submit anyway?");

            builder.setPositiveButton("Submit Anyway", (dialog, which) -> {
                // Proceed without location
                proceedWithReportSubmission(buildingBlock, roomNumber, description);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });

            builder.setNeutralButton("Try Again", (dialog, which) -> {
                getCurrentLocation();
                dialog.dismiss();
            });

            builder.show();
            return;
        }

        // Show loading
        showLoading(true);

        // Proceed with submission (with location)
        proceedWithReportSubmission(buildingBlock, roomNumber, description);
    }

    private void proceedWithReportSubmission(String buildingBlock, String roomNumber, String description) {
        // Generate report ID
        String reportId = generateReportId();

        // Create report object WITH LOCATION
        MaintenanceReport report = new MaintenanceReport(
                reportId,
                firebaseUid,
                userName != null ? userName : "Unknown",
                buildingBlock,
                roomNumber,
                selectedCategory,
                description,
                "Submitted", // Initial status
                System.currentTimeMillis(),
                null, // assignedTechnicianId
                null, // assignedTechnicianName
                null, // completedTimestamp
                null, // technicianNotes
                null, // imageUrl (will be set after upload)
                currentLatitude,  // <-- ADD LOCATION
                currentLongitude   // <-- ADD LOCATION
        );

        // If image is attached, upload it first
        if (imageUri != null) {
            uploadImageAndSaveReport(report, reportId);
        } else {
            saveReportToDatabase(report, reportId, null);
        }
    }

    private void uploadImageAndSaveReport(MaintenanceReport report, String reportId) {
        String imageName = "report_" + reportId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(imageName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get download URL
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                String imageUrl = downloadUri.toString();
                                Log.d("REPORT", "Image uploaded: " + imageUrl);
                                saveReportToDatabase(report, reportId, imageUrl);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("REPORT", "Failed to get download URL: " + e.getMessage());
                                saveReportToDatabase(report, reportId, null);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("REPORT", "Image upload failed: " + e.getMessage());
                        Toast.makeText(SubmitReportActivity.this,
                                "Image upload failed, saving report without image",
                                Toast.LENGTH_SHORT).show();
                        saveReportToDatabase(report, reportId, null);
                    }
                });
    }

    private void saveReportToDatabase(MaintenanceReport report, String reportId, String imageUrl) {
        if (imageUrl != null) {
            report.setImageUrl(imageUrl);
        }

        reportsRef.child(reportId).setValue(report)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showLoading(false);

                        if (task.isSuccessful()) {
                            Log.d("REPORT", "✅ Report saved successfully: " + reportId);

                            // Log location data for debugging
                            if (currentLatitude != null && currentLongitude != null) {
                                Log.d("REPORT", "Location saved: " + currentLatitude + ", " + currentLongitude);
                            }

                            // ✅ UPDATED: Send notifications using NotificationUtils
                            sendAdminNotification(report);

                            // Show success message
                            showSuccessDialog(reportId);
                        } else {
                            Toast.makeText(SubmitReportActivity.this,
                                    "❌ Failed to submit report: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("REPORT", "Save failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // ✅ UPDATED: Using NotificationUtils
    private void sendAdminNotification(MaintenanceReport report) {
        String reporterName = userName != null ? userName : "A Student";
        String title = "📋 New " + report.getCategory() + " Report";
        String message = reporterName + " reported an issue in " +
                report.getBuildingBlock() + ", Room " + report.getRoomNumber();

        // Add location info to notification if available
        if (currentLatitude != null && currentLongitude != null) {
            message += "\n📍 Location coordinates available for navigation";
        }
        // Send to all admin users
        NotificationUtils.sendNotificationToRole(
                "admin",
                title,
                message,
                "new_report",
                report.getReportId(),
                firebaseUid,
                reporterName,
                SubmitReportActivity.this
        );

        // Also send confirmation to student
        NotificationUtils.sendNotification(
                firebaseUid,
                "✅ Report Submitted",
                "Your " + report.getCategory() + " report has been submitted successfully." +
                        (currentLatitude != null ? "\n📍 Your location was captured for technician navigation." : ""),
                "report_confirmation",
                report.getReportId(),
                "system",
                "System",
                SubmitReportActivity.this
        );
    }

    private void showSuccessDialog(String reportId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Report Submitted Successfully!");

        String message = "Your report has been submitted.\n\nReport ID: " + reportId +
                "\n\nAdministrators have been notified and will assign a technician soon.";

        if (currentLatitude != null && currentLongitude != null) {
            message += "\n\n📍 Your location was captured for navigation.";
        } else {
            message += "\n\n⚠️ Location was not captured. Technician will use building/room information.";
        }

        builder.setMessage(message);
        builder.setPositiveButton("View Report", (dialog, which) -> {
            // Go to report details or history
            Intent intent = new Intent(SubmitReportActivity.this, ReportHistoryActivity.class);
            intent.putExtra("firebaseUid", firebaseUid);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Submit Another", (dialog, which) -> {
            // Clear form but keep location if available
            resetForm();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void resetForm() {
        etBuildingBlock.setText("");
        etRoomNumber.setText("");
        etDescription.setText("");
        spCategory.setText("");
        selectedCategory = "";
        imageUri = null;
        ivAttachment.setVisibility(View.GONE);
        btnAttach.setText("📷 Attach Image");
        // DON'T reset location - keep it for next report
        etBuildingBlock.requestFocus();
    }

    private String generateReportId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String random = String.valueOf(new Random().nextInt(9000) + 1000);
        return "REP-" + timestamp + "-" + random;
    }

    private void showLoading(boolean isLoading) {
        btnSubmit.setEnabled(!isLoading);
        btnAttach.setEnabled(!isLoading);
        btnGetLocation.setEnabled(!isLoading);
        btnSubmit.setText(isLoading ? "Submitting..." : "✅ Submit Report");
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}