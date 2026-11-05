package com.akaalpay.dmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class DmtRegisterActivity extends AppCompatActivity {

    private TextInputEditText etMobile, etAadhaar, etOtp;
    private TextInputLayout layoutOtp;
    private Button btnAction; // ✅ IMPORTANT: Button (not MaterialButton)

    private String TOKEN = "";
    private String mobile = "", referenceKey = "";

    private boolean isOtpSent = false;
    private String wadh = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmt_register);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        // 🔥 FIND VIEW
        etMobile = findViewById(R.id.etMobile);
        etAadhaar = findViewById(R.id.etAadhaar);
        etOtp = findViewById(R.id.etOtp);
        layoutOtp = findViewById(R.id.layoutOtp);
        btnAction = findViewById(R.id.btnAction);


        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        // 🔥 INTENT DATA
        mobile = getIntent().getStringExtra("mobile");
        referenceKey = getIntent().getStringExtra("referenceKey");

        wadh = getIntent().getStringExtra("wadh");
        if (wadh == null) wadh = "";

        if (mobile == null) mobile = "";
        if (referenceKey == null) referenceKey = "";

        etMobile.setText(mobile);

        // 🔙 BACK BUTTON
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 🔥 MAIN BUTTON FLOW
        btnAction.setOnClickListener(v -> {
            if (!isOtpSent) {
                sendOtp();
            } else {
                verifyOtp();
            }
        });
    }

    // ================= SEND OTP =================
//    private void sendOtp() {
//
//        String aadhaar = etAadhaar.getText() != null
//                ? etAadhaar.getText().toString().trim()
//                : "";
//
//        if (aadhaar.length() != 12) {
//            UiUtils.showToast(this, "Enter valid Aadhaar", false);
//            return;
//        }
//
//        UiUtils.showLoading(this);
//
//        try {
//            JSONObject body = new JSONObject();
//            body.put("mobile", mobile);
//            body.put("aadhaar", aadhaar);
////            body.put("referenceKey", referenceKey);
////            body.put("otp", "");
//
//            ApiClient.post(this, "dmt/send-remitter-otp", TOKEN, body, response -> {
//
//                runOnUiThread(() -> {
//
//                    UiUtils.dismissLoading();
//
//                    if (response == null) {
//                        UiUtils.showToast(this, "Server error", false);
//                        return;
//                    }
//
//                    if (response.optInt("status") == 1) {
//
//                        JSONObject data = response.optJSONObject("data");
//                        if (data != null) {
//                            referenceKey = data.optString("referenceKey");
//                        }
//
//                        // 🔥 UI CHANGE
//                        layoutOtp.setVisibility(View.VISIBLE);
//                        btnAction.setText("Verify OTP");
//                        isOtpSent = true;
//
//                        etAadhaar.setEnabled(false);
//
//                        UiUtils.showToast(this, "OTP Sent Successfully", true);
//
//                    } else {
//                        UiUtils.showToast(this, response.optString("message"), false);
//                    }
//                });
//
//            }, error -> {
//                UiUtils.dismissLoading();
//                UiUtils.showToast(this, error, false);
//            });
//
//        } catch (Exception e) {
//            UiUtils.dismissLoading();
//            e.printStackTrace();
//        }
//    }


private void sendOtp() {

    String aadhaar = etAadhaar.getText() != null
            ? etAadhaar.getText().toString().trim() : "";

    if (aadhaar.length() != 12) {
        UiUtils.showToast(this, "Enter valid Aadhaar", false);
        return;
    }

    UiUtils.showLoading(this);

    try {
        JSONObject body = new JSONObject();
        body.put("mobile", mobile);
        body.put("aadhaar", aadhaar);

        ApiClient.post(this, "dmt/send-remitter-otp", TOKEN, body, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            Log.d("DMT_OTP", "Response: " + response);

            if (response == null) {
                UiUtils.showToast(this, "Server error", false);
                return;
            }

            if (response.optInt("status") == 1) {
                JSONObject data = response.optJSONObject("data");

                if (data != null
                        && data.optInt("response_code") == 1
                        && data.optBoolean("otp_sent", false)) {

                    layoutOtp.setVisibility(View.VISIBLE);
                    btnAction.setText("Verify OTP");
                    isOtpSent = true;
                    etAadhaar.setEnabled(false);

                    UiUtils.showToast(this,
                            response.optString("message", "OTP Successfully sent"), true);
                } else {
                    UiUtils.showToast(this, "OTP could not be sent", false);
                }
            } else {
                UiUtils.showToast(this,
                        response.optString("message", "Something went wrong"), false);
            }

        }), error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, error, false);
        }));

    } catch (Exception e) {
        UiUtils.dismissLoading();
        e.printStackTrace();
    }
}

    // ================= VERIFY OTP =================
    private void verifyOtp() {

        String otp = etOtp.getText() != null
                ? etOtp.getText().toString().trim()
                : "";

        if (otp.length() < 4) {
            UiUtils.showToast(this, "Enter valid OTP", false);
            return;
        }

        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("mobile", mobile);
//            body.put("referenceKey", referenceKey);
            body.put("otp", otp);

//            ApiClient.post(this, "dmt/register-remitter ", TOKEN, body, response -> {
//
//                runOnUiThread(() -> {
//
//                    UiUtils.dismissLoading();
//
//                    if (response == null) {
//                        UiUtils.showToast(this, "Server error", false);
//                        return;
//                    }
//
//                    if (response.optInt("status") == 1) {
//
//                        JSONObject data = response.optJSONObject("data");
//                        if (data != null) {
//                            referenceKey = data.optString("referenceKey");
//
//                        }
//
//                        UiUtils.showToast(this, "OTP Verified", true);
//
//                        // 🚀 NEXT SCREEN
//                        Intent i = new Intent(DmtRegisterActivity.this, DmtCaptureActivity.class);
//                        i.putExtra("mobile", mobile);
//                        i.putExtra("aadhaar", etAadhaar.getText().toString().trim());
//                        i.putExtra("referenceKey", referenceKey);
//                        i.putExtra("wadh", wadh);
//                        startActivity(i);
//
//                        finish();
//
//                    } else {
//                        UiUtils.showToast(this, response.optString("message"), false);
//                    }
//                });
//
//            }, error -> {
//                UiUtils.dismissLoading();
//                UiUtils.showToast(this, error, false);
//            });

            ApiClient.post(this, "dmt/register-remitter", TOKEN, body, response -> {

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (response == null) {
                        UiUtils.showToast(this, "Server error", false);
                        return;
                    }

                    if (response.optInt("status") == 1) {

                        JSONObject data = response.optJSONObject("data");

                        if (data != null && data.optInt("response_code") == 1) {

                            UiUtils.showToast(
                                    this,
                                    response.optString(
                                            "message",
                                            "Mobile validated successfully"
                                    ),
                                    true
                            );

                            // NEXT SCREEN
                            Intent i = new Intent(
                                    DmtRegisterActivity.this,
                                    DmtCaptureActivity.class
                            );

                            i.putExtra("mobile", mobile);
                            i.putExtra("aadhaar",
                                    etAadhaar.getText().toString().trim());

                            // Pass existing values if required by next screen
                            i.putExtra("referenceKey", referenceKey);
                            i.putExtra("wadh", wadh);

                            startActivity(i);
                            finish();

                        } else {
                            UiUtils.showToast(
                                    this,
                                    response.optString("message", "Mobile validation failed"),
                                    false
                            );
                        }

                    } else {
                        UiUtils.showToast(
                                this,
                                response.optString("message", "Something went wrong"),
                                false
                        );
                    }
                });

            }, error -> {

                UiUtils.dismissLoading();
                UiUtils.showToast(this, error, false);

            });

        } catch (Exception e) {
            UiUtils.dismissLoading();
            e.printStackTrace();
        }
    }
}