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