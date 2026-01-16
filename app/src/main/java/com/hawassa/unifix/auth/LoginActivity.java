package com.hawassa.unifix.auth;

import android.content.Intent;
import android.os.Bundle;
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
import com.hawassa.unifix.admin.AdminDashboardActivity;
import com.hawassa.unifix.technician.TechnicianDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginId, etPassword;
    private RadioGroup rgLoginType;
    private RadioButton rbUserId, rbEmail;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupClickListeners();

        // Check if user is already logged in
        checkExistingLogin();
    }

    private void initializeViews() {
        // IMPORTANT: Your XML should have etLoginId instead of etUniversityId
        etLoginId = findViewById(R.id.etLoginId); // Change from etUniversityId
        etPassword = findViewById(R.id.etPassword);

        // Optional: Add login type selector
        rgLoginType = findViewById(R.id.rgLoginType);
        rbUserId = findViewById(R.id.rbUserId);
        rbEmail = findViewById(R.id.rbEmail);

        // If you don't have login type selector, default to User ID
        if (rgLoginType != null) {
            rgLoginType.check(R.id.rbUserId); // Default to User ID
        }

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            // Create a temporary progress bar
            progressBar = new ProgressBar(this);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void checkExistingLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, fetch their data and redirect
            usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                User user = dataSnapshot.getValue(User.class);
                                redirectToDashboard(user);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Continue to login screen
                        }
                    }
            );
        }
    }

    private void loginUser() {
        String loginId = etLoginId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(loginId)) {
            etLoginId.setError("User ID or Email is required");
            etLoginId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Determine login type and prepare email for Firebase Auth
        String authEmail;
        boolean isEmailLogin = isEmailLogin(loginId);

        if (isEmailLogin) {
            // User entered an email directly
            authEmail = loginId;
        } else {
            // User entered a User ID, we need to find their email first
            findUserByUserId(loginId, password);
            return; // Exit early, will continue in callback
        }

        // Direct Firebase Authentication with email
        authenticateWithFirebase(authEmail, password);
    }

    private boolean isEmailLogin(String loginId) {
        // Check if input looks like an email
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(loginId).matches()) {
            return true;
        }

        // Check radio button selection if available
        if (rgLoginType != null) {
            return rgLoginType.getCheckedRadioButtonId() == R.id.rbEmail;
        }

        // Default: assume User ID if not an email pattern
        return false;
    }

    private void findUserByUserId(String userId, String password) {
        // Search for user by custom userId field in database
        Query query = usersRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getEmail() != null) {
                            // Found user, now authenticate with their email
                            authenticateWithFirebase(user.getEmail(), password);
                            return;
                        }
                    }
                    // User found but no email (shouldn't happen)
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "User account error: No email associated", Toast.LENGTH_LONG).show();
                } else {
                    // User ID not found
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "User ID not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase Authentication successful
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            // Fetch user data from database
                            fetchUserData(firebaseUser.getUid());
                        }
                    } else {
                        // Authentication failed
                        showLoading(false);

                        String errorMessage = "Login failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();

                            // User-friendly error messages
                            if (errorMessage.contains("invalid login credentials") ||
                                    errorMessage.contains("password is invalid")) {
                                errorMessage = "Invalid email/User ID or password";
                            } else if (errorMessage.contains("user not found")) {
                                errorMessage = "Account not found";
                            } else if (errorMessage.contains("network error")) {
                                errorMessage = "Network error. Check your connection";
                            }
                        }

                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserData(String firebaseUid) {
        usersRef.child(firebaseUid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        showLoading(false);

                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                // Check if user is active
                                if ("active".equals(user.getStatus())) {
                                    redirectToDashboard(user);
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Account is inactive. Contact administrator.",
                                            Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // Sign out inactive user
                                }
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "User data corrupted", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "User data not found in database", Toast.LENGTH_SHORT).show();
                            mAuth.signOut(); // Clean up orphaned auth session
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Failed to load user data: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void redirectToDashboard(User user) {
        Intent intent;

        // Your RegisterActivity creates "student" or "staff" roles
        switch (user.getRole()) {
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "staff": // Changed from "technician" to match RegisterActivity
                intent = new Intent(this, TechnicianDashboardActivity.class);
                break;
            case "student":
            default:
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
        }

        // Pass both Firebase UID and custom User ID
        intent.putExtra("firebaseUid", user.getFirebaseUid());
        intent.putExtra("userId", user.getUserId());
        intent.putExtra("userName", user.getName());
        intent.putExtra("userRole", user.getRole());

        // Clear activity stack and start new
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");
            if (progressBar.getVisibility() != View.VISIBLE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            progressBar.setVisibility(View.GONE);
        }
    }
}