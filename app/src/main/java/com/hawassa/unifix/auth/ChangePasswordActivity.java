package com.hawassa.unifix.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hawassa.unifix.R;
import com.hawassa.unifix.student.StudentDashboardActivity;
import com.hawassa.unifix.technician.TechnicianDashboardActivity;
import com.hawassa.unifix.admin.AdminDashboardActivity;

public class ChangePasswordActivity extends AppCompatActivity {
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userRole;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Get user info
        Intent intent = getIntent();
        userRole = intent.getStringExtra("userRole");
        String userId = intent.getStringExtra("userId");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Enter current password");
            etCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Enter new password");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirm new password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (currentPassword.equals(newPassword)) {
            etNewPassword.setError("New password must be different from current password");
            etNewPassword.requestFocus();
            return;
        }

        // Show loading
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Updating...");

        // Step 1: Re-authenticate user with current password (Firebase Auth)
        reauthenticateAndChangePassword(currentPassword, newPassword);
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        // Create credential with current password
        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), currentPassword);

        // Re-authenticate user
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        // Step 2: Current password is correct, now update to new password
                        updatePasswordInFirebaseAuth(newPassword);
                    } else {
                        resetButton();
                        String error = reauthTask.getException() != null ?
                                reauthTask.getException().getMessage() : "Authentication failed";

                        if (error.contains("wrong-password") || error.contains("invalid-credential")) {
                            etCurrentPassword.setError("Current password is incorrect");
                            etCurrentPassword.requestFocus();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updatePasswordInFirebaseAuth(String newPassword) {
        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        // Password updated successfully in Firebase Auth
                        Toast.makeText(ChangePasswordActivity.this,
                                "Password changed successfully!", Toast.LENGTH_SHORT).show();

                        // Update firstLogin flag in database (for technicians)
                        updateFirstLoginFlag();

                        // Redirect to appropriate dashboard
                        redirectToDashboard();
                    } else {
                        resetButton();
                        String error = updateTask.getException() != null ?
                                updateTask.getException().getMessage() : "Password update failed";

                        if (error.contains("requires-recent-login")) {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "For security, please login again before changing password",
                                    Toast.LENGTH_LONG).show();
                            // Optional: Redirect to login
                            mAuth.signOut();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Failed to update password: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateFirstLoginFlag() {
        // For technicians who have firstLogin flag in database
        if (currentUser != null && "staff".equals(userRole)) {
            // Check if this is a technician (has technicianData node)
            usersRef.child(currentUser.getUid()).child("technicianData")
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Update firstLogin to false
                                dataSnapshot.getRef().child("firstLogin").setValue(false);
                            }
                        }

                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                            // Silent fail - not critical
                        }
                    });
        }
    }

    private void redirectToDashboard() {
        Intent intent;

        if (userRole == null) {
            // If no role provided, get from Firebase user
            getUserRoleAndRedirect();
            return;
        }

        switch (userRole) {
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "staff":
                intent = new Intent(this, TechnicianDashboardActivity.class);
                break;
            case "student":
            default:
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
        }

        // Pass user data
        intent.putExtra("firebaseUid", currentUser.getUid());
        intent.putExtra("userRole", userRole);

        // Clear stack and redirect
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void getUserRoleAndRedirect() {
        if (currentUser == null) return;

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(
                new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            com.hawassa.unifix.models.User user =
                                    dataSnapshot.getValue(com.hawassa.unifix.models.User.class);
                            if (user != null) {
                                userRole = user.getRole();
                                redirectToDashboard();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        resetButton();
                        Toast.makeText(ChangePasswordActivity.this,
                                "Failed to get user role", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetButton() {
        btnChangePassword.setEnabled(true);
        btnChangePassword.setText("Change Password");
    }
}