package com.hawassa.unifix.shared;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.hawassa.unifix.R;

public class HelpSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        setupToolbar();
        setupFAQSection();
        setupContactInfo();
        setupTroubleshooting();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Help & Support");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFAQSection() {
        LinearLayout faqContainer = findViewById(R.id.faqContainer);
        faqContainer.removeAllViews(); // Clear any existing views

        // Hardcoded FAQ questions and answers
        String[][] faqs = {
                // Report Submission FAQs
                {"How do I submit a maintenance report?",
                        "Go to Dashboard → Click 'New Report' → Fill in all required fields → Add photos if needed → Click 'Submit Report'"},

                {"What information do I need to provide when submitting a report?",
                        "You need to provide: Category, Location (Building & Room), Description, and optional photos"},

                {"Can I edit or cancel a report after submission?",
                        "You cannot edit submitted reports. Contact support to request changes or cancellation."},

                // Account & Login FAQs
                {"How do I reset my password?",
                        "On the login screen, click 'Forgot Password' → Enter your email → Check email for reset link"},

                {"Why can't I log in?",
                        "1. Check your internet connection\n" +
                                "2. Verify your email and password\n" +
                                "3. Make sure your account is active\n" +
                                "4. Contact support if issue persists"},

                {"How do I update my profile information?",
                        "Go to Dashboard → Click 'Profile' → Edit your details → Click 'Save Changes'"},

                // Report Status FAQs
                {"What do the different report statuses mean?",
                        "• Submitted: Report received, awaiting assignment\n" +
                                "• Assigned: Technician assigned, not started yet\n" +
                                "• In Progress: Technician working on the issue\n" +
                                "• Completed: Issue resolved, report closed"},

                {"How long does it take for reports to be resolved?",
                        "Response time depends on issue severity:\n" +
                                "• Emergency: 1-2 hours\n" +
                                "• High priority: 4-6 hours\n" +
                                "• Normal: 24-48 hours"},

                {"How do I check the status of my reports?",
                        "Go to Dashboard → Click 'Report History' → View all your reports with current status"},

                // Technical Issues
                {"The app is not loading or crashing",
                        "1. Close and restart the app\n" +
                                "2. Clear app cache in phone settings\n" +
                                "3. Update to latest version\n" +
                                "4. Reinstall the app"},

                {"Photos are not uploading",
                        "1. Check internet connection\n" +
                                "2. Ensure photos are under 5MB\n" +
                                "3. Try smaller image size\n" +
                                "4. Restart the app"},

                {"Notifications not showing",
                        "1. Check app notification permissions\n" +
                                "2. Restart your phone\n" +
                                "3. Update app to latest version"}
        };

        // Create FAQ items dynamically
        for (String[] faq : faqs) {
            View faqItemView = getLayoutInflater().inflate(R.layout.item_faq, null);

            TextView tvQuestion = faqItemView.findViewById(R.id.tvQuestion);
            TextView tvAnswer = faqItemView.findViewById(R.id.tvAnswer);
            TextView tvExpandIndicator = faqItemView.findViewById(R.id.tvExpandIndicator);

            tvQuestion.setText(faq[0]);
            tvAnswer.setText(faq[1]);

            // Initially hide the answer
            tvAnswer.setVisibility(View.GONE);
            tvExpandIndicator.setText("Tap to expand ▼");

            // Set click listener to toggle answer visibility
            faqItemView.setOnClickListener(v -> {
                if (tvAnswer.getVisibility() == View.VISIBLE) {
                    tvAnswer.setVisibility(View.GONE);
                    tvExpandIndicator.setText("Tap to expand ▼");
                } else {
                    tvAnswer.setVisibility(View.VISIBLE);
                    tvExpandIndicator.setText("Tap to collapse ▲");
                }
            });

            faqContainer.addView(faqItemView);
        }
    }

    private void setupContactInfo() {
        TextView tvSupportHours = findViewById(R.id.tvSupportHours);
        TextView tvSupportEmail = findViewById(R.id.tvSupportEmail);
        TextView tvSupportPhone = findViewById(R.id.tvSupportPhone);
        TextView tvSupportAddress = findViewById(R.id.tvSupportAddress);

        // Set contact information
        tvSupportHours.setText("Monday - Friday: 8:00 AM - 5:00 PM\n" +
                "Saturday: 9:00 AM - 1:00 PM\n" +
                "Sunday: Closed");

        tvSupportEmail.setText("support@hawassauniversity.edu.et\n" +
                "maintenance@hawassauniversity.edu.et");

        tvSupportPhone.setText("+251-46-210-0000 (Ext: 1234)\n" +
                "Emergency: +251-91-234-5678");

        tvSupportAddress.setText("Hawassa University\n" +
                "Main Campus, Main Building\n" +
                "Room 205, IT Support Office\n" +
                "Hawassa, Ethiopia");
    }

    private void setupTroubleshooting() {
        TextView tvTroubleshooting = findViewById(R.id.tvTroubleshooting);

        String troubleshooting =
                "🔧 Common Troubleshooting Guide\n\n" +
                        "1. Login Issues:\n" +
                        "   • Clear app data and cache\n" +
                        "   • Check internet connection\n" +
                        "   • Verify login credentials\n" +
                        "   • Ensure account is not locked\n\n" +

                        "2. App Not Loading:\n" +
                        "   • Force stop and restart app\n" +
                        "   • Check for app updates\n" +
                        "   • Reinstall the application\n" +
                        "   • Check device storage space\n\n" +

                        "3. Data Not Showing:\n" +
                        "   • Check internet connection\n" +
                        "   • Pull down to refresh\n" +
                        "   • Restart the app\n" +
                        "   • Check if server is down\n\n" +

                        "4. Permission Errors:\n" +
                        "   • Grant all required permissions\n" +
                        "   • Check app settings\n" +
                        "   • Update Android version\n" +
                        "   • Reinstall the app\n\n" +

                        "5. Camera/Photo Issues:\n" +
                        "   • Allow camera permissions\n" +
                        "   • Clear app cache\n" +
                        "   • Check storage permissions\n" +
                        "   • Restart device camera\n\n" +

                        "6. Slow Performance:\n" +
                        "   • Clear cache regularly\n" +
                        "   • Close background apps\n" +
                        "   • Update to latest version\n" +
                        "   • Check device memory";

        tvTroubleshooting.setText(troubleshooting);
    }
}