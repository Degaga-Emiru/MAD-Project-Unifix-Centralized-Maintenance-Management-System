package com.hawassa.unifix.shared;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.hawassa.unifix.R;
import com.hawassa.unifix.auth.ChangePasswordActivity;
import com.hawassa.unifix.auth.LoginActivity;
import com.hawassa.unifix.models.User;

public class SettingsActivity extends AppCompatActivity {
    private SwitchMaterial switchDarkMode, switchNotifications, switchSound;
    private Spinner spLanguage;
    private Button btnLogout, btnSave;
    private LinearLayout layoutChangePassword, layoutPrivacyPolicy, layoutAboutApp;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme BEFORE setting content view
        applySavedTheme();

        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        sharedPreferences = getSharedPreferences("UnifixSettings", MODE_PRIVATE);

        initializeViews();
        loadSettings();
        setupListeners();
        loadUserRole();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("UnifixSettings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initializeViews() {
        // Switches and Spinner
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchSound = findViewById(R.id.switchSound);
        spLanguage = findViewById(R.id.spLanguage);

        // Buttons
        btnLogout = findViewById(R.id.btnLogout);
        btnSave = findViewById(R.id.btnSaveSettings);

        // LinearLayouts for clickable items
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutPrivacyPolicy = findViewById(R.id.layoutPrivacyPolicy);
        layoutAboutApp = findViewById(R.id.layoutAboutApp);

        // Setup language spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLanguage.setAdapter(adapter);
    }

    private void loadSettings() {
        // Load saved settings
        boolean darkMode = sharedPreferences.getBoolean("dark_mode", false);
        boolean notifications = sharedPreferences.getBoolean("notifications", true);
        boolean sound = sharedPreferences.getBoolean("sound", true);
        String language = sharedPreferences.getString("language", "en");

        // Apply settings to UI
        switchDarkMode.setChecked(darkMode);
        switchNotifications.setChecked(notifications);
        switchSound.setChecked(sound);

        // Set spinner selection
        int position = 0; // default to English
        if (language.equals("am")) position = 1; // Amharic
        spLanguage.setSelection(position);
    }

    private void loadUserRole() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        userRole = user.getRole();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Silent fail - not critical
            }
        });
    }

    private void setupListeners() {
        // Save Settings Button
        btnSave.setOnClickListener(v -> saveSettings());

        // Logout Button
        btnLogout.setOnClickListener(v -> logoutUser());

        // Dark Mode Switch - Change theme immediately when toggled
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save immediately when user toggles
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            // Apply the theme immediately
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Restart activity to apply theme changes to all views
            recreate();
        });

        // Change Password
        layoutChangePassword.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            intent.putExtra("userRole", userRole);
            startActivity(intent);
        });

        // Privacy Policy
        layoutPrivacyPolicy.setOnClickListener(v -> {
            // You need to create PrivacyPolicyActivity
            Toast.makeText(this, "Privacy Policy", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
            // startActivity(intent);
        });

        // About App
        layoutAboutApp.setOnClickListener(v -> {
            // You need to create AboutAppActivity
            Toast.makeText(this, "About Unifix App", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, AboutAppActivity.class);
            // startActivity(intent);
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save all settings
        editor.putBoolean("dark_mode", switchDarkMode.isChecked());
        editor.putBoolean("notifications", switchNotifications.isChecked());
        editor.putBoolean("sound", switchSound.isChecked());

        String language = "en";
        if (spLanguage.getSelectedItemPosition() == 1) {
            language = "am";
        }
        editor.putString("language", language);

        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}