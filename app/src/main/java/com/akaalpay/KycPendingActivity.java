package com.akaalpay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

public class KycPendingActivity extends AppCompatActivity {

    private Button btnCheckStatus;
    private String TOKEN = "";
    private String careNo = "", careEmail = "", careWhatsApp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc_pending);
        StatusBarUtils.setupStatusBar(this, R.id.header);
        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        initViews();

        // Backend se Support Details fetch karein
        fetchSupportData();
    }

    private void initViews() {
        btnCheckStatus = findViewById(R.id.btnCheckStatus);

        // Refresh Status Logic
        btnCheckStatus.setOnClickListener(v -> checkKycStatus());

        // Header Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // WhatsApp Support Click
        findViewById(R.id.btnWhatsappSupport).setOnClickListener(v -> {
            if (!careWhatsApp.isEmpty()) {
                openWhatsApp(careWhatsApp, "Hello Bharat Pay Support, my KYC is pending. Please help.");
            } else {
                UiUtils.showToast(this, "WhatsApp not available", false);
            }
        });

        findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            if (!careNo.isEmpty()) makeCall(careNo);
            else UiUtils.showToast(this, "Not Available", false);
        });

        // Email Support Click
        findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            if (!careEmail.isEmpty()) {
                sendEmail(careEmail);
            } else {
                UiUtils.showToast(this, "Email support not available", false);
            }
        });
    }

    // ================= FETCH SUPPORT FROM BACKEND =================
    private void fetchSupportData() {
        // Aapke SupportActivity wala same endpoint
        ApiClient.get(this, "support_contact_details", TOKEN, response -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = response.optJSONObject("data");
                    if (data != null) {
                        careEmail = data.optString("customer_care_email", "");
                        careNo = data.optString("customer_care_no", "");
                        careWhatsApp = data.optString("customer_whatapp_support", "");
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }, error -> {});
    }

    // ================= CHECK KYC STATUS =================
    private void checkKycStatus() {
        if (TOKEN.isEmpty()) {
            UiUtils.showToast(this, "Session expired", false);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        UiUtils.showLoading(this); // Start Loader

        ApiClient.get(this, "user/kyc-status", TOKEN, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading(); // Stop Loader
                handleResponse(response);
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Network error. Try again.", false);
        }));
    }

    private void handleResponse(JSONObject obj) {
        try {
            if (obj == null || obj.optInt("status") != 1) {
                logoutUser();
                return;
            }

            JSONObject data = obj.getJSONObject("data");
            int kycStatus = data.optInt("account_kyc_status");
            String remark = data.optString("kyc_remark", "Invalid documents.");

            if (kycStatus == 1) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
         else if (kycStatus == 2) {
            checkVideoKycStatus();

        } else if (kycStatus == 3) {
                Intent intent = new Intent(this, KycRejectedActivity.class);
                intent.putExtra("REMARK", remark);
                startActivity(intent);
                finish();
            }
            else {
                startActivity(new Intent(this, KycUploadActivity.class));
                finish();
            }

        } catch (Exception e) {
            logoutUser();
        }
    }

    private void checkVideoKycStatus() {

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        ApiClient.get(this, "user/vkyc-status", token, response -> {
            runOnUiThread(() -> {

                // ✅ FINAL DISMISS HERE
                UiUtils.dismissLoading();

                handleVideoKycResponse(response);
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            Toast.makeText(this, "Video KYC error", Toast.LENGTH_SHORT).show();
        }));
    }

    private void handleVideoKycResponse(JSONObject response) {
        try {

            if (response == null || response.optInt("status") != 1) {
                logout();
                return;
            }

            String videoStatus = response
                    .getJSONObject("data")
                    .optString("video_kyc_status", "");

            Intent i;

            if (videoStatus.equalsIgnoreCase("Pending") || videoStatus.equalsIgnoreCase("Rejected")) {

                // 🔥 VIDEO KYC SCREEN
                i = new Intent(this, VideoKycActivity.class);

            } else {

                // 🔥 NORMAL PENDING SCREEN
                i = new Intent(this, KycPendingActivity.class);
            }

            startActivity(i);
            finish();

        } catch (Exception e) {
            logout();
        }
    }

    private void logout() {

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        pref.edit().clear().apply();

        UiUtils.dismissLoading();

        Toast.makeText(
                this,
                "Session expired. Please login again.",
                Toast.LENGTH_SHORT
        ).show();

        goLogin();
    }
    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("BP_PREF", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    // ================= UTILS (WhatsApp & Email) =================
    private void openWhatsApp(String number, String message) {
        String cleanNumber = number.replaceAll("[^0-9]", "");
        if (cleanNumber.length() == 10) cleanNumber = "91" + cleanNumber;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + cleanNumber + "?text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }
    private void makeCall(String number) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    private void sendEmail(String email) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "KYC Pending Query");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Email app not found", Toast.LENGTH_SHORT).show();
        }
    }
}