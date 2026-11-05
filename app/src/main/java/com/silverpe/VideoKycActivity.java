package com.silverpe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class VideoKycActivity extends AppCompatActivity {

    private CheckBox chkAccept;
    private Button btnStartKyc;
    private ImageView btnBack;
    private String TOKEN = "";
    private static final int REQ_CODE = 101;

    private final String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_kyc);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        chkAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnStartKyc.setEnabled(isChecked);
            btnStartKyc.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        btnStartKyc.setOnClickListener(v -> handleStartFlow());
    }

    private void initViews() {
        chkAccept = findViewById(R.id.chkAccept);
        btnStartKyc = findViewById(R.id.btnStartKyc);
        btnBack = findViewById(R.id.btnBack);

        btnStartKyc.setEnabled(false);
        btnStartKyc.setAlpha(0.5f);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goToSplashDecision());
        }
    }

    @Override
    public void onBackPressed() {
        goToSplashDecision();
    }

    private void goToSplashDecision() {
        Intent intent = new Intent(this, SplashDecisionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ================= MAIN FLOW =================
    private void handleStartFlow() {
        if (!checkPermissions()) {
            requestPermissions();
        } else if (!isLocationEnabled()) {
            showLocationSettingsDialog();
        } else {
            processStartVkyc();
        }
    }

    // ================= API CALL =================
    private void processStartVkyc() {
        if (TOKEN.isEmpty()) {
            UiUtils.showToast(this, "Session Expired. Please login again.", false);
            return;
        }

        UiUtils.showLoading(this);

        JSONObject emptyBody = new JSONObject();

        ApiClient.post(this, "user/start-vkyc", TOKEN, emptyBody, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                if (response != null && response.optInt("status") == 1) {
                    JSONObject data = response.optJSONObject("data");
                    String link = (data != null) ? data.optString("kyc_link", "") : "";

                    if (!link.isEmpty()) {
                        // Seedha Chrome mein open karega
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            i.setPackage("com.android.chrome");
                            startActivity(i);
                        } catch (Exception e) {
                            // Chrome nahi hai toh default browser mein open hoga
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            startActivity(i);
                        }
                    } else {
                        showErrorDialog("KYC link not found in server response.");
                    }
                } else {
                    String msg = (response != null) ? response.optString("message", "Failed to initiate KYC") : "Failed to initiate KYC";
                    showErrorDialog(msg);
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showErrorDialog("Server Connection Failed. Please try again.");
        }));
    }

    // ================= PERMISSION HANDLING =================
    private boolean checkPermissions() {
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);

        if (requestCode == REQ_CODE) {

            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                handleStartFlow();
            } else {
                boolean isPermanentlyDenied = true;
                for (String p : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                        isPermanentlyDenied = false;
                        break;
                    }
                }

                if (isPermanentlyDenied) {
                    showGoToSettingsDialog();
                } else {
                    UiUtils.showToast(this,
                            "Camera, Microphone & Location are required for Video KYC.", false);
                }
            }
        }
    }

    // ================= LOCATION CHECK =================
    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // ================= DIALOGS =================
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Status")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("GPS must be ON for Video KYC. Please enable it in Settings.")
                .setPositiveButton("Open Settings", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGoToSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Camera, Microphone & Location permission is permanently denied. " +
                        "Please enable from App Settings to continue.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}