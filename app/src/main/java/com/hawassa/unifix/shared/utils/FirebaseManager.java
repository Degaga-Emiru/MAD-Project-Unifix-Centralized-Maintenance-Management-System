package com.hawassa.unifix.shared.utils;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private FirebaseManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public DatabaseReference getUsersRef() {
        return databaseReference.child("users");
    }

    public DatabaseReference getReportsRef() {
        return databaseReference.child("reports");
    }

    public DatabaseReference getNotificationsRef() {
        return databaseReference.child("notifications");
    }

    public DatabaseReference getTechniciansRef() {
        return databaseReference.child("technicians");
    }

    public DatabaseReference getFeedbackRef() {
        return databaseReference.child("feedback");
    }

    public StorageReference getReportImagesRef() {
        return storageReference.child("report_images");
    }

    public StorageReference getProfileImagesRef() {
        return storageReference.child("profile_images");
    }

    // Helper method to generate unique ID
    public String generateUniqueId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 1000);
    }
}