package com.hawassa.unifix;
import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class UnifixApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Initialize other app-wide components
        initializeAppComponents();
    }

    private void initializeAppComponents() {
        // Initialize shared preferences, analytics, etc.
    }
}