package com.silverpe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.provider.Settings;

import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

public class CmsActivity extends AppCompatActivity {

    private String TOKEN = "";
    private double latitude = 0.0, longitude = 0.0;

    private FusedLocationProviderClient fusedLocationClient;
    private WebView webView;

    private boolean isDialogOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cms);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        webView = findViewById(R.id.webView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        setupWebView();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 🔥 AUTO START
        checkLocationAndStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🔁 retry after coming from settings
        if (latitude == 0.0 && longitude == 0.0) {
            checkLocationAndStart();
        }
    }

    // ================= WEBVIEW =================
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
    }

    // ================= LOCATION =================
    private void checkLocationAndStart() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        getCurrentLocation();
    }

    private void getCurrentLocation() {

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(this, location -> {

            if (location != null) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                // 🔥 API CALL
                callCmsApi();

            } else {
                showLocationDialog();
            }
        }).addOnFailureListener(e -> showLocationDialog());
    }

    private void showLocationDialog() {

        if (isDialogOpen) return;

        isDialogOpen = true;

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("Please enable GPS to continue CMS service")
                .setPositiveButton("Settings", (d, w) -> {
                    isDialogOpen = false;
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setOnDismissListener(dialog -> isDialogOpen = false)
                .setCancelable(false)
                .show();
    }

    // ================= API =================
    private void callCmsApi() {

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(this, "fino/generate-url", TOKEN, body, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                if (response == null) {
                    UiUtils.showToast(this, "Server not responding", false);
                    return;
                }

                int status = response.optInt("status");
                JSONObject data = response.optJSONObject("data");
                String redirectUrl = data != null ? data.optString("redirect_url") : "";



                if (status == 1 && redirectUrl != null && !redirectUrl.isEmpty()) {

                    // 🔥 LOAD URL IN WEBVIEW
                    webView.loadUrl(redirectUrl);

                } else {

                    new AlertDialog.Builder(CmsActivity.this)
                            .setTitle("Failed")
                            .setMessage(response.optString("message"))
                            .setPositiveButton("OK", null)
                            .show();
                }

            });

        }, error -> {

            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                new AlertDialog.Builder(CmsActivity.this)
                        .setTitle("API Error")
                        .setMessage(error)
                        .setPositiveButton("OK", null)
                        .show();
            });

        });
    }

    // 🔙 WebView Back Control
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}