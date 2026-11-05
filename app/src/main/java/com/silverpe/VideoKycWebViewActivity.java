package com.silverpe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.silverpe.utils.StatusBarUtils;

public class VideoKycWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ImageView btnBack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_kyc_webview);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        String url = getIntent().getStringExtra("url");

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "KYC URL is missing. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> goToSplashDecision());

        setupWebView();

        webView.loadUrl(url);
    }

    // ================= WEBVIEW SETUP =================
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // ✅ Basic Settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // ✅ Geolocation Enable
        webSettings.setGeolocationEnabled(true);

        // ✅ Chrome User Agent - Unsupported Browser error fix
        webSettings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36"
        );

        // ✅ WebViewClient - Page load progress
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(VideoKycWebViewActivity.this,
                        "Page Error: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ WebChromeClient - Camera + Mic + Location permission
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {

                    // ✅ Native permission check before granting to WebView
                    boolean cameraGranted = ContextCompat.checkSelfPermission(
                            VideoKycWebViewActivity.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

                    boolean micGranted = ContextCompat.checkSelfPermission(
                            VideoKycWebViewActivity.this,
                            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

                    if (cameraGranted && micGranted) {
                        // ✅ Both granted - allow WebView
                        request.grant(request.getResources());
                    } else {
                        // ❌ Not granted - deny and inform user
                        request.deny();
                        Toast.makeText(VideoKycWebViewActivity.this,
                                "Camera/Microphone permission missing. Please restart the app.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                // ✅ Native location check before granting to WebView
                boolean locationGranted = ContextCompat.checkSelfPermission(
                        VideoKycWebViewActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                if (locationGranted) {
                    callback.invoke(origin, true, false);
                } else {
                    callback.invoke(origin, false, false);
                    Toast.makeText(VideoKycWebViewActivity.this,
                            "Location permission missing. Please restart the app.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // ================= NAVIGATION =================
    private void goToSplashDecision() {
        Intent intent = new Intent(this, SplashDecisionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            goToSplashDecision();
        }
    }
}