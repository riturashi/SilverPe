package com.silverpe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.distributer.DDashboardActivity;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class SplashDecisionActivity extends AppCompatActivity {

    private static final String PREFS = "BP_PREF";
    private static final String KEY_ONBOARDING_DONE = "ONBOARDING_DONE";
        private static final long SPLASH_DELAY = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_decision);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            SharedPreferences pref =
                    getSharedPreferences(PREFS, MODE_PRIVATE);

            // First-time user: show onboarding
            boolean onboardingDone =
                    pref.getBoolean(KEY_ONBOARDING_DONE, false);

            if (!onboardingDone) {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
                return;
            }

            // Onboarding completed: check login session
            String token = pref.getString("TOKEN", "");

            if (token == null || token.trim().isEmpty()) {
                goLogin();
            } else {
                checkKycStatus(token);
            }

        }, SPLASH_DELAY);
    }


//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.activity_splash_decision);
//
//        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
//
//        String token = pref.getString("TOKEN", "");
//
//        Log.d("token", "onCreate: "+token);
//
//        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
//
//            if (isFinishing() || isDestroyed()) {
//                return;
//            }
//
//            if (token == null || token.isEmpty()) {
//
//                goLogin();
//            } else {
//                checkKycStatus(token);
//            }
//
//        }, SPLASH_DELAY);
//    }


    /* ================= KYC STATUS ================= */

    private void checkKycStatus(String token) {
        // ✅ Loader START (Only once)
        UiUtils.showLoading(this);

        ApiClient.get(this, "user/kyc-status", token, response -> {
            Log.d("KYC_RESPONSE", "Response: " + response);

            runOnUiThread(() -> {
                handleKycResponse(response, token);
            });
        }, error -> runOnUiThread(() -> {

            Log.e("KYC_ERROR", "Error: " + error);

            if (error != null) {
                Log.e("KYC_ERROR", "Error class: " + error.getClass().getName());
                Log.e("KYC_ERROR", "Error message: " + error);
            }


            UiUtils.dismissLoading();
            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
        }));
    }


    private void checkDashboardType(String token) {

        UiUtils.showLoading(this);

        ApiClient.get(this, "user/dashboard-type", token, response -> {

            Log.d("DASHBOARD_RESPONSE", "Response: " + response);

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                JSONObject jsonObject = response;

                int status = jsonObject.optInt("status", 0);
                String message = jsonObject.optString(
                        "message", "Something went wrong"
                );


                if (status == 1) {

                    JSONObject data = jsonObject.optJSONObject("data");
                    if (data == null) {
                        Toast.makeText(
                                this,
                                "Invalid server response",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String user_type_id = data.optString("user_type_id", "");

                    // Save user type ID
                    getSharedPreferences("BP_PREF", MODE_PRIVATE)
                            .edit()
                            .putString("user_type_id", user_type_id)
                            .apply();


                    String dashboard = data.optString("dashboard", "");

                    if (dashboard.equalsIgnoreCase("distributor")) {

//                        Toast.makeText(
//                                this,
//                                "Distributor dashboard",
//                                Toast.LENGTH_SHORT
//                        ).show();

                        startActivity(new Intent(
                                this,
                                DDashboardActivity.class
                        ));
                        finish();

                    } else if (dashboard.equalsIgnoreCase("retailer")) {

//                        Toast.makeText(
//                                this,
//                                "Retailer dashboard",
//                                Toast.LENGTH_SHORT
//                        ).show();

                        startActivity(new Intent(
                                this,

                                DashboardActivity.class
                        ));
                        finish();

                    } else {

                        Toast.makeText(
                                this,
                                "Invalid dashboard type",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } else {

                    // Backend mapping incorrect / user record not found
                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            Log.e("DASHBOARD_ERROR", "API error: " + error);

            Toast.makeText(
                    this,
                    "Network error",
                    Toast.LENGTH_SHORT
            ).show();
        }));
    }


    private void handleKycResponse(JSONObject response, String token) {
        try {
            if (response == null || response.optInt("status") != 1) {
                UiUtils.dismissLoading();
                logout();
                return;
            }

            JSONObject data = response.getJSONObject("data");
            int kycStatus = data.getInt("account_kyc_status");

            if (kycStatus == 1) {
                UiUtils.dismissLoading();
                checkDashboardType(token);
//                startActivity(new Intent(this, DashboardActivity.class));
//                finish();

            } else if (kycStatus == 2) {
                checkVideoKycStatus();

            } else if (kycStatus == 3) {
                UiUtils.dismissLoading();

                String remark = data.optString("kyc_remark", "Documents are not clear.");

                Intent intent = new Intent(this, KycRejectedActivity.class);
                intent.putExtra("REMARK", remark);
                startActivity(intent);
                finish();

            } else {
                UiUtils.dismissLoading();
                startActivity(new Intent(this, KycUploadActivity.class));
                finish();
            }

        } catch (Exception e) {
            UiUtils.dismissLoading();
            logout();
        }
    }

    /* ================= VIDEO KYC ================= */

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

            String videoStatus = response.getJSONObject("data").optString("video_kyc_status", "");

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

    /* ================= LOGOUT ================= */

    private void logout() {

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        pref.edit().clear().apply();

        UiUtils.dismissLoading();

        Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();

        goLogin();
    }

    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}