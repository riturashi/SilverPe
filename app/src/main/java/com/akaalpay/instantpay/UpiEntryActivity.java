package com.akaalpay.instantpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.aeps.AirtelOnboardingActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.UiUtils;
import org.json.JSONObject;

public class UpiEntryActivity extends AppCompatActivity {

    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new View(this)); // Blank view for safety

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        if (TOKEN == null || TOKEN.isEmpty()) {
            UiUtils.showToast(this, "Login session expired", false);
            finish();
            return;
        }

        loadAirtelStatus();
    }

    private void loadAirtelStatus() {
        UiUtils.showLoading(this);

        // 🔄 Ab ye tumhare PHP backend ke GET method se connect hoga
        // Yahan se humne JSONObject() body hata di hai kyunki GET method hai
        ApiClient.get(
                this,
                //"user/instant_pay_details",
                "user/bharatdigicash_aeps_status",
                TOKEN,
                response -> {
                    runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        try {
                            if (response != null && response.optInt("status") == 1) {
                                JSONObject d = response.optJSONObject("data");

                                if (d != null) {
                                    // API ke exact keys use kar rahe hain
                                    int onboarding = d.optInt("ip_onboarding_status", 0);
                                    int ekyc       = d.optInt("ip_jio_ekyc_status", 0);
                                    int auth2fa    = d.optInt("ipay_2fa_auth", 0);
                                    String outletId = d.optString("ip_outlet_id", "");

                                    Log.d("AIRTEL_FLOW", "Onb: " + onboarding + ", eKYC: " + ekyc + ", 2FA: " + auth2fa);

                                    // Conditions check karke sahi screen par bhejo
                                    routeUser(onboarding, ekyc, auth2fa, outletId);
                                } else {
                                    UiUtils.showToast(this, "Data parsing failed", false);
                                    finish();
                                }
                            } else {
                                String msg = (response != null) ? response.optString("message") : "Server error";
                                UiUtils.showToast(this, msg, false);
                                finish();
                            }
                        } catch (Exception e) {

                            finish();
                        }
                    });
                },
                err -> {
                    runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        UiUtils.showToast(this, "Network error", false);
                        finish();
                    });
                }
        );
    }

    private void routeUser(int onb, int ekyc, int auth, String oid) {
        Intent intent;

        if (onb != 1) {
            // Agar Agent onboarding nahi hui
            intent = new Intent(this, AirtelOnboardingActivity.class);
        } else if (ekyc != 1) {
            // Agar Fingerprint KYC baki hai
            intent = new Intent(this, JioEkycActivity.class);
        } else {
            // Sab complete hai -> Main AEPS Dashboard
            intent = new Intent(this, UatmActivity.class);
            intent.putExtra("OUTLET_ID", oid);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}