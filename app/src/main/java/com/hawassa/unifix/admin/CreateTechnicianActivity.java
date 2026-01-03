package com.hawassa.unifix.admin;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.User;
import java.util.HashMap;
import java.util.Map;

public class CreateTechnicianActivity extends AppCompatActivity {
    private EditText etName, etUniversityId, etEmail, etPhone, etSpecialization, etPassword;
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_technician);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        Button btnCreate = findViewById(R.id.btnCreateTechnician);
        btnCreate.setOnClickListener(v -> createTechnician());
    }

    private void initializeViews() {
        etName = findViewById(R.id.etTechName);
        etUniversityId = findViewById(R.id.etTechUniversityId);
        etEmail = findViewById(R.id.etTechEmail);
        etPhone = findViewById(R.id.etTechPhone);
        etSpecialization = findViewById(R.id.etSpecialization);
        etPassword = findViewById(R.id.etPassword);
    }

    private void createTechnician() {
        String name = etName.getText().toString().trim();
        String universityId = etUniversityId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String specialization = etSpecialization.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (name.isEmpty() || universityId.isEmpty() || specialization.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Generate email SAME AS RegisterActivity
        String authEmail = email.isEmpty() ?
                universityId.toLowerCase() + "@hawassa.unifix.edu" : email;

        Log.d("CREATE_TECH", "Creating technician account...");
        Log.d("CREATE_TECH", "User ID: " + universityId);
        Log.d("CREATE_TECH", "Auth Email: " + authEmail);
        Log.d("CREATE_TECH", "Name: " + name);
        Log.d("CREATE_TECH", "Role: staff");

        // Step 1: Create Firebase Auth user (EXACTLY like RegisterActivity)
        mAuth.createUserWithEmailAndPassword(authEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Use the Firebase Auth UID (NOT a custom key!)
                            String firebaseUid = firebaseUser.getUid();

                            Log.d("CREATE_TECH", "Firebase Auth UID: " + firebaseUid);

                            // Step 2: Save user to database
                            saveTechnicianToDatabase(firebaseUid, name, universityId,
                                    email, phone, specialization, authEmail, password);
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to create account";
                        Toast.makeText(CreateTechnicianActivity.this,
                                "Auth Error: " + error, Toast.LENGTH_LONG).show();
                        Log.e("CREATE_TECH", "Firebase Auth failed: " + error);
                    }
                });
    }

    private void saveTechnicianToDatabase(String firebaseUid, String name, String universityId,
                                          String email, String phone, String specialization,
                                          String authEmail, String plainPassword) {

        // Create User object EXACTLY like RegisterActivity
        User technicianUser = new User();
        technicianUser.setFirebaseUid(firebaseUid); // This MUST be the Firebase Auth UID
        technicianUser.setUserId(universityId); // This is what LoginActivity searches for
        technicianUser.setName(name);
        technicianUser.setEmail(authEmail); // Use the auth email
        technicianUser.setPhone(phone.isEmpty() ? null : phone);
        technicianUser.setRole("staff"); // MUST BE "staff" to match RegisterActivity
        technicianUser.setCreatedAt(System.currentTimeMillis());
        technicianUser.setStatus("active");

        Log.d("CREATE_TECH", "Saving user to database with:");
        Log.d("CREATE_TECH", "FirebaseUid: " + firebaseUid);
        Log.d("CREATE_TECH", "UserId: " + universityId);
        Log.d("CREATE_TECH", "Email: " + authEmail);
        Log.d("CREATE_TECH", "Role: " + "staff");

        // Save User object to Firebase Database
        usersRef.child(firebaseUid).setValue(technicianUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CREATE_TECH", "✅ User object saved to users/" + firebaseUid);

                    // Save additional technician-specific data
                    Map<String, Object> additionalData = new HashMap<>();
                    additionalData.put("specialization", specialization);
                    additionalData.put("isTechnician", true);
                    additionalData.put("assignedTasks", 0);
                    additionalData.put("completedTasks", 0);
                    additionalData.put("originalEmail", email); // Store original email if provided

                    usersRef.child(firebaseUid).child("technicianData").setValue(additionalData)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("CREATE_TECH", "✅ Technician data saved");

                                showSuccessMessage(universityId, plainPassword, authEmail);
                                clearForm();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CREATE_TECH", "❌ Failed to save technician data: " + e.getMessage());

                                // Don't rollback - the main user account is created
                                Toast.makeText(CreateTechnicianActivity.this,
                                        "Account created but technician data failed. Contact admin.",
                                        Toast.LENGTH_SHORT).show();
                                clearForm();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("CREATE_TECH", "❌ Failed to save User object: " + e.getMessage());

                    // Rollback: Delete Firebase Auth user
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.delete();
                    }

                    Toast.makeText(CreateTechnicianActivity.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessMessage(String userId, String password, String loginEmail) {
        String message = "✅ Technician account created successfully!\n\n" +
                "Login credentials:\n" +
                "User ID: " + userId + "\n" +
                "Password: " + password + "\n" +
                "Email (for login): " + loginEmail + "\n" +
                "Role: staff";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Log.d("CREATE_TECH", "Technician created:");
        Log.d("CREATE_TECH", "User ID: " + userId);
        Log.d("CREATE_TECH", "Email: " + loginEmail);
        Log.d("CREATE_TECH", "Password: " + password);

        // Show detailed dialog
        showSuccessDialog(userId, password, loginEmail);
    }

    private void showSuccessDialog(String userId, String password, String loginEmail) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("✅ Technician Account Created");
        builder.setMessage("Technician account created successfully!\n\n" +
                "Login information:\n\n" +
                "✅ User can login with:\n" +
                "• User ID: " + userId + "\n" +
                "• Password: " + password + "\n\n" +
                "✅ Or with email:\n" +
                "• Email: " + loginEmail + "\n" +
                "• Password: " + password + "\n\n" +
                "Role: staff");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            finish(); // Go back to admin dashboard
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void clearForm() {
        etName.setText("");
        etUniversityId.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etSpecialization.setText("");
        etPassword.setText("");
        etName.requestFocus();
    }
}