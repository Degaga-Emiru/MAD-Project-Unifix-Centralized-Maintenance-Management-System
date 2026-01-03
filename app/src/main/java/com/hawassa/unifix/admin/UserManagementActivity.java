package com.hawassa.unifix.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.admin.adapters.UserListAdapter;
import com.hawassa.unifix.models.User;
import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalStudents, tvTotalTechnicians, tvListHeader;
    private RecyclerView rvUserList;
    private MaterialToolbar toolbar;
    private ChipGroup chipGroup;
    private Chip chipAll, chipStudents, chipTechnicians;
    private TextInputEditText etSearch;
    private TextInputLayout searchLayout;

    private DatabaseReference usersRef;
    private UserListAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private List<User> originalUserList = new ArrayList<>(); // Store all loaded users

    // Filter variables
    private String currentFilter = "all"; // all, student, staff
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        setupFirebase();
        loadUsers();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvTotalTechnicians = findViewById(R.id.tvTotalTechnicians);
        tvListHeader = findViewById(R.id.tvListHeader);
        rvUserList = findViewById(R.id.rvUserList);
        chipGroup = findViewById(R.id.chipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipStudents = findViewById(R.id.chipStudents);
        chipTechnicians = findViewById(R.id.chipTechnicians);
        etSearch = findViewById(R.id.etSearch);
        searchLayout = findViewById(R.id.searchLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("User Management");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Initialize adapter with empty list
        userAdapter = new UserListAdapter(new ArrayList<>(), new UserListAdapter.OnUserActionListener() {
            @Override
            public void onDeleteUser(User user) {
                deleteUser(user);
            }

            @Override
            public void onEditUser(User user) {
                editUser(user);
            }
        });
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(userAdapter);
    }

    private void setupFilterChips() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (checkedId == R.id.chipStudents) {
                currentFilter = "student";
            } else if (checkedId == R.id.chipTechnicians) {
                currentFilter = "staff"; // Assuming technicians have role "staff"
            }
            applyFilters();
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search when clear icon is clicked
        searchLayout.setEndIconOnClickListener(v -> {
            etSearch.setText("");
            currentSearchQuery = "";
            applyFilters();
        });
    }

    private void setupFirebase() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    private void loadUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalUserList.clear(); // Clear the original list
                int total = 0, students = 0, technicians = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && "active".equals(user.getStatus())) {
                        originalUserList.add(user);
                        total++;

                        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
                        if ("student".equals(role)) {
                            students++;
                        } else if ("staff".equals(role) || "technician".equals(role)) {
                            technicians++;
                        }
                    }
                }

                tvTotalUsers.setText(String.valueOf(total));
                tvTotalStudents.setText(String.valueOf(students));
                tvTotalTechnicians.setText(String.valueOf(technicians));

                // Apply filters after loading original data
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserManagementActivity.this,
                        "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        List<User> filteredList = new ArrayList<>();

        // Always filter from the originalUserList
        for (User user : originalUserList) {
            // Apply role filter
            boolean matchesRole = false;
            String userRole = user.getRole() != null ? user.getRole().toLowerCase() : "";

            switch (currentFilter) {
                case "all":
                    matchesRole = true;
                    break;
                case "student":
                    matchesRole = "student".equals(userRole);
                    break;
                case "staff":
                    matchesRole = "staff".equals(userRole) || "technician".equals(userRole);
                    break;
            }

            // Apply search filter
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String userName = user.getName() != null ? user.getName().toLowerCase() : "";
                String userEmail = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                String userId = user.getUserId() != null ? user.getUserId().toLowerCase() : "";
                matchesSearch = userName.contains(currentSearchQuery) ||
                        userEmail.contains(currentSearchQuery) ||
                        userId.contains(currentSearchQuery);
            }

            // Add to filtered list if both filters match
            if (matchesRole && matchesSearch) {
                filteredList.add(user);
            }
        }

        // Update adapter with filtered list
        userAdapter.updateData(filteredList);

        // Update list header with count
        updateListHeader(filteredList.size());
    }

    private void updateListHeader(int filteredCount) {
        String header = "";
        switch (currentFilter) {
            case "all":
                header = "👤 All Users";
                break;
            case "student":
                header = "🎓 Students";
                break;
            case "staff":
                header = "🔧 Technicians";
                break;
        }

        header += " (" + filteredCount + ")";
        tvListHeader.setText(header);
    }

    private void deleteUser(User user) {
        if (user == null) {
            Toast.makeText(this, "User object is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String firebaseUid = user.getFirebaseUid();
        String userId = user.getUserId();
        String userName = user.getName();

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Toast.makeText(this, "Firebase UID is missing for user: " + userName, Toast.LENGTH_LONG).show();
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Deactivate User")
                .setMessage("Are you sure you want to deactivate:\n" +
                        userName + " (" + user.getEmail() + ")?\n\n" +
                        "Firebase UID: " + firebaseUid.substring(0, Math.min(8, firebaseUid.length())) + "...")
                .setPositiveButton("Deactivate", (dialog, which) -> {
                    performUserDeactivation(firebaseUid, userName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUserDeactivation(String firebaseUid, String userName) {
        usersRef.child(firebaseUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update status to inactive
                    snapshot.getRef().child("status").setValue("inactive")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(UserManagementActivity.this,
                                        userName + " has been deactivated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(UserManagementActivity.this,
                                        "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } else {
                    Toast.makeText(UserManagementActivity.this,
                            "User not found in database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserManagementActivity.this,
                        "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editUser(User user) {
        // Open edit user activity
        Toast.makeText(this, "Edit user: " + user.getName(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, EditUserActivity.class);
        // intent.putExtra("userId", user.getFirebaseUid());
        // startActivity(intent);
    }
}