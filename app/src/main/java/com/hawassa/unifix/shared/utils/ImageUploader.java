package com.hawassa.unifix.shared.utils;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUploader {
    private Context context;
    private StorageReference storageRef;
    private OnUploadListener uploadListener;

    public interface OnUploadListener {
        void onUploadSuccess(String downloadUrl);
        void onUploadFailure(String errorMessage);
        void onUploadProgress(double percentage);
    }

    public ImageUploader(Context context) {
        this.context = context;
        this.storageRef = FirebaseStorage.getInstance().getReference();
    }

    public void setUploadListener(OnUploadListener listener) {
        this.uploadListener = listener;
    }

    public void uploadReportImage(Uri imageUri, String reportId) {
        if (imageUri == null) {
            if (uploadListener != null) {
                uploadListener.onUploadSuccess(null);
            }
            return;
        }

        // Create unique filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "report_" + reportId + "_" + timeStamp + ".jpg";

        // Reference to location in Firebase Storage
        StorageReference imageRef = storageRef.child("report_images/" + fileName);

        // Upload file
        UploadTask uploadTask = imageRef.putFile(imageUri);

        // Monitor upload progress
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (uploadListener != null) {
                uploadListener.onUploadProgress(progress);
            }
        });

        // Handle successful upload
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        if (uploadListener != null) {
                            uploadListener.onUploadSuccess(downloadUri.toString());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        if (uploadListener != null) {
                            uploadListener.onUploadFailure("Failed to get download URL: " + e.getMessage());
                        }
                    }
                });
            }
        });

        // Handle failed upload
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (uploadListener != null) {
                    uploadListener.onUploadFailure("Upload failed: " + e.getMessage());
                }
                Toast.makeText(context, "Image upload failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void uploadProfileImage(Uri imageUri, String userId) {
        if (imageUri == null) {
            if (uploadListener != null) {
                uploadListener.onUploadSuccess(null);
            }
            return;
        }

        // Create unique filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "profile_" + userId + "_" + timeStamp + ".jpg";

        // Reference to location in Firebase Storage
        StorageReference imageRef = storageRef.child("profile_images/" + fileName);

        // Upload file
        UploadTask uploadTask = imageRef.putFile(imageUri);

        // Monitor upload progress
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (uploadListener != null) {
                uploadListener.onUploadProgress(progress);
            }
        });

        // Handle successful upload
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        if (uploadListener != null) {
                            uploadListener.onUploadSuccess(downloadUri.toString());
                        }
                    }
                });
            }
        });

        // Handle failed upload
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (uploadListener != null) {
                    uploadListener.onUploadFailure("Upload failed: " + e.getMessage());
                }
                Toast.makeText(context, "Profile image upload failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        // Extract path from URL
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Image deleted successfully
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed to delete image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public StorageReference getStorageReference() {
        return storageRef;
    }
}