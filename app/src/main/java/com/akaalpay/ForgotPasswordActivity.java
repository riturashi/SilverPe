package com.akaalpay;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

import okhttp3.*;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etUser, etPan, etOtp, etPassword, etConfirm;
    Button btnAction;
    LinearLayout hiddenSection;

    boolean isOtpSent = false;

    String user = "", pan = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnAction.setOnClickListener(v -> {
            if (!isOtpSent) {
                sendOtp();
            } else {
                resetPassword();
            }
        });
    }

    private void initViews() {
        etUser = findViewById(R.id.etUser);
        etPan = findViewById(R.id.etPan);
        etOtp = findViewById(R.id.etOtp);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);

        btnAction = findViewById(R.id.btnAction);
        hiddenSection = findViewById(R.id.hiddenSection);
    }


    private void sendOtp() {

        user = etUser.getText().toString().trim();
        pan = etPan.getText().toString().trim().toUpperCase();

        if (user.isEmpty()) {
            showDialog("Enter Mobile No or User ID");
            return;
        }

        if (pan.length() != 10) {
            showDialog("Enter valid PAN Number");
            return;
        }

        UiUtils.showLoading(ForgotPasswordActivity.this);

        new Thread(() -> {
            try {

                OkHttpClient client = new OkHttpClient();

                RequestBody body = new FormBody.Builder()
                        .add("username", user)
                        .add("pan", pan)
                        .build();

                Request req = new Request.Builder()
                        .url("https://akaalpay.com/app/index.php/api/v1/forgot-password/newrequest-otp")
                        .post(body)
                        .build();

                Response res = client.newCall(req).execute();
                JSONObject json = new JSONObject(res.body().string());

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (json.optInt("status") == 1) {

                        isOtpSent = true;
                        hiddenSection.setVisibility(View.VISIBLE);
                        btnAction.setText("Submit");

                        showDialog(json.optString("message"));

                    } else {
                        showDialog(json.optString("message"));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    showDialog(e.toString()); // 🔥 REAL ERROR
                });
            }
        }).start();
    }

    // 🔥 STEP 2 → RESET PASSWORD
    private void resetPassword() {

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

        UiUtils.showLoading(ForgotPasswordActivity.this);

        new Thread(() -> {
            try {

                OkHttpClient client = new OkHttpClient();

                RequestBody body = new FormBody.Builder()
                        .add("username", user)
                        .add("pan", pan)
                        .add("otp", otp)
                        .add("password", pass)
                        .add("confirm_password", confirm)
                        .build();

                Request req = new Request.Builder()
                        .url("https://akaalpay.com/app/index.php/api/v1/forgot-password/newvalidate-otp")
                        .post(body)
                        .build();

                Response res = client.newCall(req).execute();
                JSONObject json = new JSONObject(res.body().string());

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (json.optInt("status") == 1) {

                        showSuccessDialog(json.optString("message"));



                    } else {
                        showDialog(json.optString("message"));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    showDialog(e.toString()); // 🔥 REAL ERROR
                });
            }
        }).start();
    }

    // 🔥 COMMON DIALOG
    private void showDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
    private void showSuccessDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {

                    Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                })
                .show();
    }
}