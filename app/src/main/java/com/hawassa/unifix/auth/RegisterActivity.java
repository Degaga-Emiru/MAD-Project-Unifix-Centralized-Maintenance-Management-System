package com.hawassa.unifix.auth;

public class RegisterActivity {
}
package com.hawassa.unifix.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;  // ADD THIS IMPORT
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
        import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
        import com.hawassa.unifix.R;
import com.hawassa.unifix.models.User;
import com.hawassa.unifix.student.StudentDashboardActivity;
import com.hawassa.unifix.technician.TechnicianDashboardActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etUserId, etEmail, etPhone, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etUserId = findViewById(R.id.etUserId);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar == null) {
            progressBar = new ProgressBar(this);
        }
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String userId = etUserId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation (same as before)
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(password)) {
            showError("Please fill required fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        String role = getSelectedRole();
        String authEmail = TextUtils.isEmpty(email) ?
                userId.toLowerCase() + "@hawassa.unifix.edu" : email;

        showLoading(true);

        // Step 1: Create Firebase Auth user
        mAuth.createUserWithEmailAndPassword(authEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // ✅ SIMPLIFIED: Save user data WITHOUT checking duplicate user ID first
                            saveUserDirectly(firebaseUser.getUid(), name, userId, email, phone, role);
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, "Auth Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void saveUserDirectly(String firebaseUid, String name, String userId,
                                  String email, String phone, String role) {

        Log.d("REGISTER_DEBUG", "Saving user to database...");
        Log.d("REGISTER_DEBUG", "Firebase UID: " + firebaseUid);
        Log.d("REGISTER_DEBUG", "User ID: " + userId);

        // Create User object
        User user = new User(firebaseUid, userId, name, email, phone,
                role, System.currentTimeMillis(), "active");

        Log.d("REGISTER_DEBUG", "User object created: " + user.toString());

        // Save to database
        usersRef.child(firebaseUid).setValue(user)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d("REGISTER_DEBUG", "✅ User saved successfully to database!");
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                        // ✅ CHANGED: Redirect to LOGIN page instead of dashboard
                        redirectToLogin(role);
                        finish();
                    } else {
                        Log.e("REGISTER_DEBUG", "❌ Save failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown"));

                        // Delete the Firebase Auth user since database save failed
                        mAuth.getCurrentUser().delete().addOnCompleteListener(deleteTask -> {
                            Toast.makeText(this, "Failed to save user data. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("REGISTER_DEBUG", "❌ Save failed with exception: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ✅ NEW METHOD: Redirect to Login with role information
    private void redirectToLogin(String role) {
        Intent intent = new Intent(this, LoginActivity.class);

        // Optional: Pass role information to pre-fill radio button in login
        intent.putExtra("registeredRole", role);

        // Clear any existing tasks and start fresh
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Optional: Show a message about successful registration
        Toast.makeText(this, "Please login with your credentials", Toast.LENGTH_SHORT).show();
    }

    private String getSelectedRole() {
        return rgRole.getCheckedRadioButtonId() == R.id.rbStaff ? "staff" : "student";
    }

    private void redirectToDashboard(String role) {
        Intent intent = "staff".equals(role) ?
                new Intent(this, TechnicianDashboardActivity.class) :
                new Intent(this, StudentDashboardActivity.class);

        intent.putExtra("firebaseUid", mAuth.getCurrentUser().getUid());
        intent.putExtra("userId", etUserId.getText().toString().trim());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Registering..." : "Register");
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}