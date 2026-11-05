package com.akaalpay.aeps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

public class FinoEntryActivity extends AppCompatActivity {

    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        if (TOKEN == null || TOKEN.isEmpty()) {
            showExitDialog("Error", "Session expired. Please login again.");
            return;
        }

        loadProfileAndDecide();
    }

    private void loadProfileAndDecide() {

        UiUtils.showLoading(this);

        ApiClient.get(
                this,
                "user/my-profile",
                TOKEN,
                response -> {

                    runOnUiThread(() -> UiUtils.dismissLoading());

                    try {
                        JSONObject d = response.getJSONObject("data");

                        int onboarding = d.optInt("onboarding_status", 0);
                        int twoFaReg   = d.optInt("2fa_reg", 0);
                        int twoFaAuth  = d.optInt("2fa_auth", 0);
                        String userId  = d.optString("username", "");

                        if (userId == null || userId.isEmpty()) {
                            showExitDialog("Error", "Invalid user. Please login again.");
                            return;
                        }

                        if (onboarding == 1) {
                            handleActiveFlow(twoFaReg, twoFaAuth);
                        } else {
                            checkOnboardingStatus(userId, twoFaReg, twoFaAuth);
                        }

                    } catch (Exception e) {
                        showExitDialog("Error", "Something went wrong.");
                    }
                },
                err -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    showExitDialog("Error", "Unable to fetch profile.");
                })
        );
    }

    /**
     * ✅ ACTIVE FLOW
     */
    private void handleActiveFlow(int twoFaReg, int twoFaAuth) {

        if (twoFaReg != 1) {
            startActivity(new Intent(this, FinoTwoFaRegisterActivity.class));
            return;
        }

        if (twoFaAuth != 1) {
            startActivity(new Intent(this, FinoTwoFaAuthActivity.class));
            return;
        }

        startActivity(new Intent(this, FinoAepsDashboardActivity.class));
        finish();
    }

    /**
     * 🌐 STATUS CHECK API
     */
    private void checkOnboardingStatus(String userId, int twoFaReg, int twoFaAuth) {

        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("id", userId);

            ApiClient.post(
                    this,
                    "aeps/onboarding/status-check",
                    TOKEN,
                    body,
                    response -> {

                        runOnUiThread(() -> UiUtils.dismissLoading());

                        try {
                            JSONObject data = response.getJSONObject("data");
                            int code = data.optInt("response_code", 0);

                            runOnUiThread(() -> handleApiResponse(code, twoFaReg, twoFaAuth));

                        } catch (Exception e) {
                            showExitDialog("Error", "Invalid server response.");
                        }

                    },
                    err -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        showExitDialog("Error", "Unable to verify status.");
                    })
            );

        } catch (Exception e) {
            showExitDialog("Error", "Request failed.");
        }
    }

    /**
     * 🎯 FINAL ROUTING
     */
    private void handleApiResponse(int code, int twoFaReg, int twoFaAuth) {

        switch (code) {

            case 1:
                new AlertDialog.Builder(this)
                        .setTitle("Success")
                        .setMessage("Your AEPS Is Activated")
                        .setCancelable(false)
                        .setPositiveButton("Continue", (d, w) -> {
                            handleActiveFlow(twoFaReg, twoFaAuth);
                        })
                        .show();
                break;

            case 2:
                startActivity(new Intent(this, FinoOnboardingActivity.class));
                break;

            case 3:
                showExitDialog("Activation Failed",
                        "Your Fino AEPS is rejected. Please contact support.");
                break;

            default:
                showExitDialog("Activation Required",
                        "Please complete onboarding from profile.");
                break;
        }
    }

    /**
     * ❌ EXIT
     */
    private void showExitDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }
}