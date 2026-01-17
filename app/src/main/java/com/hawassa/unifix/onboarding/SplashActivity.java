package com.hawassa.unifix.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hawassa.unifix.R;
import com.hawassa.unifix.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 7000;

    private ImageView ivLogo;
    private TextView tvWelcome;
    private TextView tvTagline;
    private TextView tvLoading;

    private LinearLayout llError;
    private TextView tvErrorTitle;
    private TextView tvErrorDescription;
    private Button btnRetry;

    private Handler handler = new Handler();
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        setupAnimations();
        showWelcomeMessage();
        startSplashTimer();
        requestNotificationPermission();
    }

    private void initViews() {
        ivLogo = findViewById(R.id.ivLogo);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTagline = findViewById(R.id.tvTagline);
        tvLoading = findViewById(R.id.tvLoading);

        llError = findViewById(R.id.llError);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorDescription = findViewById(R.id.tvErrorDescription);
        btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> {
            hideError();
            startSplashTimer();
        });
    }

    private void setupAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);
        ivLogo.startAnimation(fadeIn);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        tvLoading.startAnimation(pulse);
    }

    private void showWelcomeMessage() {
        tvWelcome.setAlpha(0f);
        tvTagline.setAlpha(0f);

        tvWelcome.animate()
                .alpha(1f)
                .setDuration(900)
                .setStartDelay(700)
                .start();

        tvTagline.animate()
                .alpha(1f)
                .setDuration(900)
                .setStartDelay(1400)
                .start();
    }

    private void startSplashTimer() {
        if (splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }

        if (isNetworkAvailable()) {
            showLoadingState();

            splashRunnable = () -> navigateToLogin();
            handler.postDelayed(splashRunnable, SPLASH_DELAY);

            animateLoadingText();
        } else {
            showErrorState();
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void showLoadingState() {
        llError.setVisibility(View.GONE);
        tvLoading.setVisibility(View.VISIBLE);
        tvLoading.setText("Initializing Unifix...");
    }

    private void showErrorState() {
        llError.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.GONE);

        tvErrorTitle.setText("Connection Interrupted");
        tvErrorDescription.setText(
                "Please check your internet connection and try again."
        );
    }

    private void hideError() {
        llError.setVisibility(View.GONE);
        tvLoading.setVisibility(View.VISIBLE);
        tvLoading.setText("Reconnecting...");
    }

    private void animateLoadingText() {
        final String[] loadingTexts = {
                "Connecting students and technicians...",
                "Managing campus issues smartly...",
                "Unifix is ready for you..."
        };

        Handler textHandler = new Handler();
        int[] index = {0};

        Runnable textRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < loadingTexts.length) {
                    tvLoading.setText(loadingTexts[index[0]]);
                    index[0]++;
                    textHandler.postDelayed(this, 2000);
                }
            }
        };

        textHandler.postDelayed(textRunnable, 1000);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
    }
}
