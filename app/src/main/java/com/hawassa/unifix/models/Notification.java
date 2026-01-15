@IgnoreExtraProperties
public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type; // new_report, task_assigned, status_update, feedback, admin_alert
    private String reportId;
    private String senderId;
    private String senderName;
    private boolean read;
    private long timestamp;

    // Required empty constructor for Firebase
    public Notification() {
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for creating notifications programmatically
    public Notification(String userId, String title, String message,
                        String type, String reportId) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.reportId = reportId;
        this.notificationId = generateNotificationId();
    }