package com.hawassa.unifix.models;

public class User {
    private String firebaseUid;     // From Firebase Authentication
    private String userId;          // custom user id
    private String name;
    private String email;
    private String phone;
    private String role;            // "student"  by default
    private long createdAt;
    private String status;          // "active", "inactive"

    // Required empty constructor for Firebase
    public User() {
        // Initialize with default values
        this.role = "student";
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor with all fields
    public User(String firebaseUid, String userId, String name, String email,
                String phone, String role, long createdAt, String status) {
        this.firebaseUid = firebaseUid;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = (role != null) ? role : "student";
        this.createdAt = (createdAt > 0) ? createdAt : System.currentTimeMillis();
        this.status = (status != null) ? status : "active";
    }

    // Constructor for convenience (without firebaseUid)
    public User(String userId, String name, String email, String phone, String role) {
        this(null, userId, name, email, phone, role, System.currentTimeMillis(), "active");
    }

    // --- GETTERS ---
    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    // --- SETTERS ---
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRole(String role) {
        this.role = (role != null) ? role : "student";
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = (createdAt > 0) ? createdAt : System.currentTimeMillis();
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status : "active";
    }

    // --- HELPER METHODS ---

    @Override
    public String toString() {
        return "User{" +
                "firebaseUid='" + firebaseUid + '\'' +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }

    // Check if user is active
    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    // Check if user has specific role
    public boolean hasRole(String roleToCheck) {
        return role != null && role.equalsIgnoreCase(roleToCheck);
    }

    // Check if user is student
    public boolean isStudent() {
        return hasRole("student");
    }

    // Check if user is staff
    public boolean isStaff() {
        return hasRole("staff");
    }
}