package com.akaalpay.dmt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.akaalpay.R;
import com.akaalpay.report.DmtReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class DmtTransferActivity extends AppCompatActivity {

    private TextView tvBeneName, tvAccount, tvBank, tvIfsc;

    private TextInputEditText etAmount, etTxnPassword, etOtp;

    private TextInputLayout layoutOtp;

    private Button btnTransfer;

    private LinearLayout btnImps, btnNeft;

    private TextView tvImps, tvNeft;

    private String selectedTxnType = "IMPS";

    private String TOKEN = "";

    private String mobile = "";
    private String beneId = "";

    private String beneName = "";
    private String account = "";
    private String ifsc = "";
    private String bankName = "";

    private String bankId = "";

    private String referenceKey = "";
    private String txnId = "";

    private boolean isOtpFlow = false;

    private FusedLocationProviderClient fusedLocationClient;

    private String latitude = "";
    private String longitude = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dmt_transfer);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        checkLocationEnabled();

        SharedPreferences pref =
                getSharedPreferences("BP_PREF",
                        Context.MODE_PRIVATE);

        TOKEN = pref.getString("TOKEN", "");

        try {

            mobile =
                    getIntent().getStringExtra("mobile");

            beneId =
                    getIntent().getStringExtra("bene_id");

            beneName =
                    getIntent().getStringExtra("bene_name");

            account =
                    getIntent().getStringExtra("account");

            ifsc =
                    getIntent().getStringExtra("ifsc");

            bankName =
                    getIntent().getStringExtra("bank_name");

            referenceKey =
                    getIntent().getStringExtra("referenceKey");
          //  bankId = getIntent().getStringExtra("bank_id");

//            if (bankId == null)
//                bankId = "";

            if (mobile != null && !mobile.isEmpty() && beneId != null && !beneId.isEmpty()) {
                fetchBeneficiaryDetails(mobile, beneId);
            } else {
                UiUtils.showToast(this, "Mobile or beneficiary id is missing, Please open page again!", false);
            }

            if (mobile == null) mobile = "";

            if (beneId == null) beneId = "";

            if (beneName == null) beneName = "";

            if (account == null) account = "";

            if (ifsc == null) ifsc = "";

            if (bankName == null) bankName = "";

            if (referenceKey == null) referenceKey = "";

            tvBeneName.setText(beneName);

            tvAccount.setText("A/C: " + account);

            tvBank.setText(bankName);

            tvIfsc.setText("IFSC: " + ifsc);

        } catch (Exception e) {

            e.printStackTrace();

            showDialogBox(
                    "Error",
                    "Unable to load transfer details."
            );

        }

        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        btnTransfer.setOnClickListener(v -> {

            if (latitude.isEmpty() || longitude.isEmpty()) {

                checkLocationEnabled();

                showDialogBox(
                        "Location Required",
                        "Please enable location first."
                );

                return;
            }

            if (!isOtpFlow) {

                submitTransaction();

            } else {

                verifyOtp();

            }

        });

    }


    // getBank Id
    private void fetchBeneficiaryDetails(String mobile, String beneId) {

        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("mobile", mobile);
            body.put("bene_id", beneId);

            ApiClient.post(this, "dmt/fetch-beneficiary-details", TOKEN, body,
                    response -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();
                        Log.d("DMT_BENE", "Response: " + response);

                        if (response == null) {
                            UiUtils.showToast(this, "Server error", false);
                            return;
                        }

                        if (response.optInt("status") != 1) {
                            UiUtils.showToast(this,
                                    response.optString("message", "Something went wrong"), false);
                            return;
                        }

                        JSONObject data = response.optJSONObject("data");

                        if (data == null || data.optInt("response_code") != 1) {
                            UiUtils.showToast(this,
                                    response.optString("message", "Beneficiary not found"), false);
                            return;
                        }

                        bankId = data.optString("bankid");   // ✅ response se bank id
                        Log.d("DMT_BENE", "bankId = " + bankId);

                    }),
                    error -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        UiUtils.showToast(this, error, false);
                    }));

        } catch (Exception e) {
            UiUtils.dismissLoading();
            e.printStackTrace();
        }
    }



    // ================= INIT =================

    private void initViews() {

        tvBeneName =
                findViewById(R.id.tvBeneName);

        tvAccount =
                findViewById(R.id.tvAccount);

        tvBank =
                findViewById(R.id.tvBank);

        tvIfsc =
                findViewById(R.id.tvIfsc);

        etAmount =
                findViewById(R.id.etAmount);

        etTxnPassword =
                findViewById(R.id.etTxnPassword);

        etOtp =
                findViewById(R.id.etOtp);

        layoutOtp =
                findViewById(R.id.layoutOtp);

        btnTransfer =
                findViewById(R.id.btnTransfer);

        btnImps =
                findViewById(R.id.btnImps);

        btnNeft =
                findViewById(R.id.btnNeft);

        tvImps =
                findViewById(R.id.tvImps);

        tvNeft =
                findViewById(R.id.tvNeft);

        btnImps.setOnClickListener(v -> {

            selectedTxnType = "IMPS";

            btnImps.setBackgroundResource(
                    R.drawable.bg_transfer_selected);

            btnNeft.setBackgroundColor(
                    Color.TRANSPARENT);

            tvImps.setTextColor(Color.WHITE);

            tvNeft.setTextColor(
                    Color.parseColor("#666666"));

        });

        btnNeft.setOnClickListener(v -> {

            selectedTxnType = "NEFT";

            btnNeft.setBackgroundResource(
                    R.drawable.bg_transfer_selected);

            btnImps.setBackgroundColor(
                    Color.TRANSPARENT);

            tvNeft.setTextColor(Color.WHITE);

            tvImps.setTextColor(
                    Color.parseColor("#666666"));

        });

    }

    // ================= LOCATION =================

    private void checkLocationEnabled() {

        LocationRequest locationRequest =
                LocationRequest.create();

        locationRequest.setPriority(
                Priority.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

        SettingsClient client =
                LocationServices.getSettingsClient(this);

        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {

                    getCurrentLocation();

                })

                .addOnFailureListener(e -> {

                    if (e instanceof ResolvableApiException) {

                        try {

                            ResolvableApiException resolvable =
                                    (ResolvableApiException) e;

                            resolvable.startResolutionForResult(
                                    DmtTransferActivity.this,
                                    1001
                            );

                        } catch (IntentSender.SendIntentException ex) {

                            ex.printStackTrace();

                        }

                    } else {

                        startActivity(
                                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        );

                    }

                });

    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );

            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        latitude =
                                String.valueOf(location.getLatitude());

                        longitude =
                                String.valueOf(location.getLongitude());

                    }

                });

    }

    private final ActivityResultLauncher<String>
            requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                        if (isGranted) {

                            getCurrentLocation();

                        } else {

                            showDialogBox(
                                    "Permission Denied",
                                    "Location permission required."
                            );

                        }

                    });

    // ================= SUBMIT TRANSACTION =================

    private void submitTransaction() {

        try {

            String amount =
                    etAmount.getText().toString().trim();

            String txnPassword =
                    etTxnPassword.getText().toString().trim();

            if (amount.isEmpty()) {

                showDialogBox(
                        "Warning",
                        "Please enter amount."
                );

                return;
            }

            if (txnPassword.isEmpty()) {

                showDialogBox(
                        "Warning",
                        "Please enter transaction password."
                );

                return;
            }

            UiUtils.showLoading(this);

            JSONObject body = new JSONObject();

            body.put("mobile", mobile);

            body.put("bene_id", beneId);

            body.put("amount", amount);

            body.put("bene_name", beneName);

            body.put("account", account);

            body.put("ifsc", ifsc);

            body.put("bank_id", bankId);

            body.put("dob", "01-01-1990");

            body.put("address", "Patna");

            body.put("pincode", "800001");

            body.put("gst_state", "10");

            body.put("txntype", selectedTxnType);

            body.put("transaction_password",
                    txnPassword);

            body.put("referenceKey",
                    referenceKey);

            body.put("latitude",
                    latitude);

            body.put("longitude",
                    longitude);

//            ApiClient.post(
//                    this,
//                    "dmt/transaction",
//                    TOKEN,
//                    body,
//
//                    response -> runOnUiThread(() -> {
//
//                        UiUtils.dismissLoading();
//
//                        try {
//
//                            if (response == null) {
//
//                                showDialogBox(
//                                        "Error",
//                                        "Server response failed."
//                                );
//
//                                return;
//                            }
//
//                            int status =
//                                    response.optInt("status", 0);
//
//                            String message =
//                                    response.optString("message");
//
//                            if (status == 1) {
//
//                                JSONObject data =
//                                        response.optJSONObject("data");
//
//                                if (data != null) {
//
//                                    String responseCode =
//                                            data.optString("response_code", "");
//
//                                    // OTP FLOW
//                                    if ("OTP".equalsIgnoreCase(responseCode)) {
//
//                                        txnId =
//                                                data.optString("txn_id");
//
//                                        referenceKey =
//                                                data.optString("referenceKey");
//
//                                        layoutOtp.setVisibility(
//                                                android.view.View.VISIBLE);
//
//                                        btnTransfer.setText(
//                                                "Verify OTP");
//
//                                        isOtpFlow = true;
//
//                                        etAmount.setEnabled(false);
//
//                                        etTxnPassword.setEnabled(false);
//
//                                        showDialogBox(
//                                                "Success",
//                                                "OTP Sent Successfully."
//                                        );
//
//                                    } else {
//
//                                        // SUCCESS RECEIPT FLOW
//                                        String id =
//                                                data.optString("id", "");
//
//                                        if (!id.isEmpty()) {
//
//                                            Intent i = new Intent(
//                                                    DmtTransferActivity.this,
//                                                    DmtReceiptActivity.class
//                                            );
//
//                                            i.putExtra("id", id);
//
//                                            startActivity(i);
//
//                                            finish();
//
//                                        } else {
//
//                                            showDialogBox(
//                                                    "Success",
//                                                    message
//                                            );
//
//                                        }
//
//                                    }
//
//                                } else {
//
//                                    showDialogBox(
//                                            "Success",
//                                            message
//                                    );
//
//                                }
//
//                            } else {
//
//                                showDialogBox(
//                                        "Failed",
//                                        message
//                                );
//
//                            }
//
//                        } catch (Exception e) {
//
//                            e.printStackTrace();
//
//                            showDialogBox(
//                                    "Error",
//                                    "Response parsing failed."
//                            );
//
//                        }
//
//                    }),
//
//                    error -> runOnUiThread(() -> {
//
//                        UiUtils.dismissLoading();
//
//                        showDialogBox(
//                                "Error",
//                                error
//                        );
//
//                    })
//
//            );



            ApiClient.post(
                    this,
                    "dmt/transaction",
                    TOKEN,
                    body,

                    response -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        try {

                            if (response == null) {

                                showDialogBox(
                                        "Error",
                                        "Server response failed."
                                );

                                return;
                            }

                            int status = response.optInt("status", 0);

                            String message = response.optString(
                                    "message",
                                    "Something went wrong."
                            );

                            if (status == 1) {

                                JSONObject data =
                                        response.optJSONObject("data");

                                if (data != null) {

                                    // OTP FLOW
                                    boolean txnOtp =
                                            data.optBoolean("txn_otp", false);

                                    if (txnOtp) {

                                        txnId = data.optString(
                                                "txn_otp_id",
                                                ""
                                        );

//                                        String otpValidity =
//                                                data.optString("otp_validity", "");
//
////                                        int amount =
////                                                data.optInt("amount", 0);
//
//                                        int chunkAmount =
//                                                data.optInt("chunk_amount", 0);
//
//                                        int chunkNo =
//                                                data.optInt("chunk_no", 0);
//
//                                        int totalChunks =
//                                                data.optInt("total_chunks", 0);

                                        layoutOtp.setVisibility(
                                                android.view.View.VISIBLE
                                        );

                                        btnTransfer.setText("Verify OTP");

                                        isOtpFlow = true;

                                        etAmount.setEnabled(false);

                                        etTxnPassword.setEnabled(false);

                                        showDialogBox(
                                                "Success",
                                                message
                                        );

                                    } else {

                                        // SUCCESS RECEIPT FLOW
                                        String id = data.optString(
                                                "id",
                                                ""
                                        );

                                        if (!id.isEmpty()) {

                                            Intent i = new Intent(
                                                    DmtTransferActivity.this,
                                                    DmtReceiptActivity.class
                                            );

                                            i.putExtra("id", id);

                                            startActivity(i);

                                            finish();

                                        } else {

                                            showDialogBox(
                                                    "Success",
                                                    message
                                            );

                                        }
                                    }

                                } else {

                                    showDialogBox(
                                            "Success",
                                            message
                                    );

                                }

                            } else {

                                showDialogBox(
                                        "Failed",
                                        message
                                );

                            }

                        } catch (Exception e) {

                            e.printStackTrace();

                            showDialogBox(
                                    "Error",
                                    "Response parsing failed."
                            );

                        }

                    }),

                    error -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        showDialogBox(
                                "Error",
                                error
                        );

                    })

            );

        } catch (Exception e) {

            UiUtils.dismissLoading();

            e.printStackTrace();

            showDialogBox(
                    "Error",
                    "Something went wrong."
            );

        }

    }

    // ================= VERIFY OTP =================



    private void verifyOtp() {

        try {

            String otp =
                    etOtp.getText().toString().trim();
            Log.d("DMT_VERIFY", "========== OTP VERIFY REQUEST ==========");
            Log.d("DMT_VERIFY", "txnId: " + txnId);
            Log.d("DMT_VERIFY", "OTP: " + otp);
            Log.d("DMT_VERIFY", "Latitude: " + latitude);
            Log.d("DMT_VERIFY", "Longitude: " + longitude);
         //   Log.d("DMT_VERIFY", "Request Body: " + body.toString());

            if (otp.length() < 4) {

                showDialogBox(
                        "Warning",
                        "Please enter valid OTP."
                );

                return;
            }

            if (txnId == null || txnId.isEmpty()) {

                showDialogBox(
                        "Error",
                        "Transaction OTP ID missing."
                );

                return;
            }

            UiUtils.showLoading(this);

            JSONObject body = new JSONObject();

            // New API response: txn_otp_id
            body.put("txn_otp_id", txnId);

            body.put("otp", otp);

            body.put("latitude", latitude);

            body.put("longitude", longitude);

            ApiClient.post(
                    this,
                    "dmt/verify-transaction",
                    TOKEN,
                    body,

                    response -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        try {
                            Log.d("DMT_VERIFY", "========== OTP VERIFY RESPONSE ==========");
                            Log.d("DMT_VERIFY", "Response: " +
                                    (response != null ? response.toString() : "NULL"));
                            if (response == null) {

                                showDialogBox(
                                        "Error",
                                        "Server response failed."
                                );

                                return;
                            }

                            int status =
                                    response.optInt("status", 0);

                            String message =
                                    response.optString(
                                            "message",
                                            "Something went wrong."
                                    );

                            if (status == 1) {

                                JSONObject data =
                                        response.optJSONObject("data");

                                if (data != null) {

                                    String id =
                                            data.optString("id", "");

                                    if (!id.isEmpty()) {

                                        Intent i = new Intent(
                                                DmtTransferActivity.this,
                                                DmtReceiptActivity.class
                                        );

                                        i.putExtra("id", id);

                                        startActivity(i);

                                        finish();

                                    } else {

                                        showDialogBox(
                                                "Success",
                                                message
                                        );

                                    }

                                } else {

                                    showDialogBox(
                                            "Success",
                                            message
                                    );

                                }

                            } else {

                                showDialogBox(
                                        "Failed",
                                        message
                                );

                            }

                        } catch (Exception e) {

                            e.printStackTrace();

                            showDialogBox(
                                    "Error",
                                    "OTP verification failed."
                            );

                        }

                    }),

                    error -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        showDialogBox(
                                "Error",
                                error
                        );

                    })

            );

        } catch (Exception e) {

            UiUtils.dismissLoading();

            e.printStackTrace();

            showDialogBox(
                    "Error",
                    "OTP verification failed."
            );

        }

    }


//    private void verifyOtp() {
//
//        try {
//
//            String otp =
//                    etOtp.getText().toString().trim();
//
//            if (otp.length() < 4) {
//
//                showDialogBox(
//                        "Warning",
//                        "Please enter valid OTP."
//                );
//
//                return;
//            }
//
//            UiUtils.showLoading(this);
//
//            JSONObject body = new JSONObject();
//
//            body.put("txn_id", txnId);
//
//            body.put("otp", otp);
//
//            body.put("referenceKey",
//                    referenceKey);
//
//            body.put("latitude",
//                    latitude);
//
//            body.put("longitude",
//                    longitude);
//
//            ApiClient.post(
//                    this,
//                    "dmt/transaction",
//                    TOKEN,
//                    body,
//
//                    response -> runOnUiThread(() -> {
//
//                        UiUtils.dismissLoading();
//
//                        try {
//
//                            if (response == null) {
//
//                                showDialogBox(
//                                        "Error",
//                                        "Server response failed."
//                                );
//
//                                return;
//                            }
//
//                            int status =
//                                    response.optInt("status", 0);
//
//                            String message =
//                                    response.optString("message");
//
//                            if (status == 1) {
//
//                                JSONObject data =
//                                        response.optJSONObject("data");
//
//                                if (data != null) {
//
//                                    String id =
//                                            data.optString("id", "");
//
//                                    if (!id.isEmpty()) {
//
//                                        Intent i = new Intent(
//                                                DmtTransferActivity.this,
//                                                DmtReceiptActivity.class
//                                        );
//
//                                        i.putExtra("id", id);
//
//                                        startActivity(i);
//
//                                        finish();
//
//                                    } else {
//
//                                        showDialogBox(
//                                                "Success",
//                                                message
//                                        );
//
//                                    }
//
//                                } else {
//
//                                    showDialogBox(
//                                            "Success",
//                                            message
//                                    );
//
//                                }
//
//                            } else {
//
//                                showDialogBox(
//                                        "Failed",
//                                        message
//                                );
//
//                            }
//
//                        } catch (Exception e) {
//
//                            e.printStackTrace();
//
//                            showDialogBox(
//                                    "Error",
//                                    "OTP verification failed."
//                            );
//
//                        }
//
//                    }),
//
//                    error -> runOnUiThread(() -> {
//
//                        UiUtils.dismissLoading();
//
//                        showDialogBox(
//                                "Error",
//                                error
//                        );
//
//                    })
//
//            );
//
//        } catch (Exception e) {
//
//            UiUtils.dismissLoading();
//
//            e.printStackTrace();
//
//            showDialogBox(
//                    "Error",
//                    "OTP verification failed."
//            );
//
//        }
//
//    }

    // ================= DIALOG =================

    private void showDialogBox(String title,
                               String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();

    }

}