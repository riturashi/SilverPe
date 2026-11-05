package com.silverpe;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText etOtp, etPassword, etConfirm;
    Button btnSubmit;

    String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);

        etOtp = findViewById(R.id.etOtp);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnSubmit = findViewById(R.id.btnSubmit);

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 🔥 AUTO OTP SEND
        sendOtp();

        btnSubmit.setOnClickListener(v -> changePassword());
    }

    // 🔥 SEND OTP
    private void sendOtp() {

        if (TOKEN.isEmpty()) {
            showDialog("Session Expired, Please Login Again");
            return;
        }

        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("type", "account_password");

            ApiClient.post(
                    this,
                    "user/password-change-otp",
                    TOKEN,
                    body,
                    response -> {

                        runOnUiThread(() -> {

                            UiUtils.dismissLoading();

                            if (response != null && response.optInt("status") == 1) {

                                Toast.makeText(this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();

                            } else {

                                String msg = response != null ? response.optString("message") : "Error";

                                if (msg != null && msg.contains("already sent")) {
                                    Toast.makeText(this, "OTP already sent. Please try after 2 mint", Toast.LENGTH_SHORT).show();
                                } else {
                                    showDialog(msg);
                                }
                            }
                        });

                    },
                    error -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();
                        showDialog("Server Error");

                    })
            );

        } catch (Exception e) {
            UiUtils.dismissLoading();
            showDialog("Request Failed");
        }
    }

    // 🔥 CHANGE PASSWORD
    private void changePassword() {

        String otp = etOtp.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (otp.length() != 6) {
            showDialog("Enter valid 6 digit OTP");
            return;
        }

        if (pass.length() < 6) {
            showDialog("Password must be at least 6 characters");
            return;
        }

        if (!pass.equals(confirm)) {
            showDialog("Password does not match");
            return;
        }

        if (TOKEN.isEmpty()) {
            showDialog("Session Expired, Please Login Again");
            return;
        }

        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("otp", otp);
            body.put("password", pass);
            body.put("cpassword", confirm);

            ApiClient.post(
                    this,
                    "user/change-password",
                    TOKEN,
                    body,
                    response -> {

                        runOnUiThread(() -> {

                            UiUtils.dismissLoading();

                            if (response != null && response.optInt("status") == 1) {

                                showSuccessDialog(response.optString("message"));

                            } else {

                                showDialog(response != null ? response.optString("message") : "Error");
                            }
                        });

                    },
                    error -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();
                        showDialog("Server Error");

                    })
            );

        } catch (Exception e) {
            UiUtils.dismissLoading();
            showDialog("Request Failed");
        }
    }

    // 🔥 SUCCESS → LOGOUT
    private void showSuccessDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> logoutUser())
                .show();
    }

    // 🔥 LOGOUT
    private void logoutUser() {

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        pref.edit().clear().apply();

        Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}