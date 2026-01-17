package com.hawassa.unifix.shared;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.User;
import com.hawassa.unifix.shared.utils.ImageUploader;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private ImageView ivProfile;
    private TextView tvName, tvUniversityId, tvEmail, tvPhone, tvRole;
    private EditText etName, etEmail, etPhone;
    private Button btnEdit, btnSave, btnCancel, btnChangePhoto;
    private DatabaseReference usersRef;
    private String userId;
    private Uri imageUri;
    private ImageUploader imageUploader;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get the correct user ID - Firebase UID is likely what you need
        String firebaseUid = getIntent().getStringExtra("firebaseUid");
        String customUserId = getIntent().getStringExtra("userId");

        Log.d("ProfileActivity", "Received - firebaseUid: " + firebaseUid + ", userId: " + customUserId);

        // Use Firebase UID if available, otherwise use custom ID
        if (firebaseUid != null && !firebaseUid.isEmpty()) {
            userId = firebaseUid;
            Log.d("ProfileActivity", "Using Firebase UID: " + userId);
        } else if (customUserId != null && !customUserId.isEmpty()) {
            userId = customUserId;
            Log.d("ProfileActivity", "Using custom userId: " + userId);
        } else {
            // Fallback to current logged-in user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
                Log.d("ProfileActivity", "Using current Firebase Auth UID: " + userId);
            }
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        imageUploader = new ImageUploader(this);

        setupImageUploaderListener();
        loadUserProfile();
        setupClickListeners();
    }

    private void initializeViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvUniversityId = findViewById(R.id.tvUniversityId);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRole = findViewById(R.id.tvRole);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
    }

    private void setupImageUploaderListener() {
        imageUploader.setUploadListener(new ImageUploader.OnUploadListener() {
            @Override
            public void onUploadSuccess(String downloadUrl) {
                if (downloadUrl != null) {
                    // Update user profile with new image URL
                    updateProfileImage(downloadUrl);
                    Glide.with(ProfileActivity.this)
                            .load(downloadUrl)
                            .placeholder(R.drawable.ic_profile)
                            .into(ivProfile);
                }
            }

            @Override
            public void onUploadFailure(String errorMessage) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to upload image: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadProgress(double percentage) {
                // Show progress if needed
            }
        });
    }

    private void loadUserProfile() {
        usersRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        displayUserProfile(user);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserProfile(User user) {
        // Display mode
        tvName.setText(user.getName());
        tvUniversityId.setText(user.getUserId());
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Not set");
        tvPhone.setText(user.getPhone() != null ? user.getPhone() : "Not set");
        tvRole.setText(user.getRole());

        // Edit mode
        etName.setText(user.getName());
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        etPhone.setText(user.getPhone() != null ? user.getPhone() : "");

//        // Load profile image if exists
//        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
//            Glide.with(this)
//                    .load(user.getProfileImageUrl())
//                    .placeholder(R.drawable.ic_profile)
//                    .into(ivProfile);
//        }
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> enableEditing(true));
        btnSave.setOnClickListener(v -> saveProfileChanges());
        btnCancel.setOnClickListener(v -> enableEditing(false));

        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                // Upload new profile image
                imageUploader.uploadProfileImage(imageUri, userId);
            }
        }
    }

    private void enableEditing(boolean enable) {
        isEditing = enable;

        if (enable) {
            // Show edit fields, hide display fields
            tvName.setVisibility(View.GONE);
            tvEmail.setVisibility(View.GONE);
            tvPhone.setVisibility(View.GONE);

            etName.setVisibility(View.VISIBLE);
            etEmail.setVisibility(View.VISIBLE);
            etPhone.setVisibility(View.VISIBLE);

            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnChangePhoto.setVisibility(View.VISIBLE);
        } else {
            // Show display fields, hide edit fields
            tvName.setVisibility(View.VISIBLE);
            tvEmail.setVisibility(View.VISIBLE);
            tvPhone.setVisibility(View.VISIBLE);

            etName.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
            etPhone.setVisibility(View.GONE);

            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            btnChangePhoto.setVisibility(View.GONE);

            // Reload original data
            loadUserProfile();
        }
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);

        usersRef.child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    enableEditing(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateProfileImage(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl);

        usersRef.child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Image URL saved to profile
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to save profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
    }
}