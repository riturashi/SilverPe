package com.akaalpay.instantpay;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

/**
 * Entry form only - mobile number, amount, consent. On a successful
 * generate-qr call, hands off to UatmQrActivity (a separate page) which
 * owns the QR display, countdown, polling and status checks.
 */
public class UatmActivity extends BaseActivity {

    private ImageView btnBack;
    private TextInputEditText etMobile, etAmount;
    private MaterialButton btnGenerateQr;
    private MaterialButton btn100, btn500, btn1000, btn2000, btn3000, btn4000, btn5000;
    private MaterialCheckBox cbDeclaration;

    private MaterialButton[] amountButtons;
    private MaterialButton lastSelectedBtn = null;
    private boolean isProgrammaticAmountChange = false;

    private String TOKEN = "";

    private double liveLat = 0.0;
    private double liveLog = 0.0;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uatm);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        btnBack.setOnClickListener(v -> finish());
        btnGenerateQr.setOnClickListener(v -> { hideKeyboard(); handleLocationAndGenerateQR(); });

        // Consent checkbox - enables/disables the Generate QR button
        cbDeclaration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnGenerateQr.setEnabled(isChecked);
            btnGenerateQr.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            isChecked ? Color.parseColor("#301BD3") : Color.parseColor("#B0BEC5")
                    )
            );
        });

        // Quick amount shortcut buttons
        amountButtons = new MaterialButton[]{btn100, btn500, btn1000, btn2000, btn3000, btn4000, btn5000};

        setupAmountButton(btn100, "100");
        setupAmountButton(btn500, "500");
        setupAmountButton(btn1000, "1000");
        setupAmountButton(btn2000, "2000");
        setupAmountButton(btn3000, "3000");
        setupAmountButton(btn4000, "4000");
        setupAmountButton(btn5000, "5000");

        // If the person types a custom amount by hand, none of the
        // shortcut buttons should still look "selected".
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isProgrammaticAmountChange) {
                    clearAmountHighlights();
                }
            }
        });
    }

    private void setupAmountButton(MaterialButton btn, String amount) {
        btn.setOnClickListener(v -> selectAmountButton(btn, amount));
    }

    /**
     * Sets the amount, highlights only the clicked button, and resets
     * every other shortcut button back to its default look. Looping
     * over all buttons every time (instead of just remembering "the
     * last one") guarantees there's never a stale highlight left behind.
     */
    private void selectAmountButton(MaterialButton selected, String amount) {

        isProgrammaticAmountChange = true;
        etAmount.setText(amount);
        etAmount.setSelection(etAmount.getText().length());
        isProgrammaticAmountChange = false;

        for (MaterialButton btn : amountButtons) {

            boolean isSelected = (btn == selected);

            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            isSelected ? Color.parseColor("#301BD3") : Color.WHITE
                    )
            );

            btn.setTextColor(
                    isSelected ? Color.WHITE : Color.parseColor("#301BD3")
            );
        }

        lastSelectedBtn = selected;
    }

    private void clearAmountHighlights() {

        if (amountButtons == null) return;

        for (MaterialButton btn : amountButtons) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#301BD3"));
        }

        lastSelectedBtn = null;
    }

    private void initViews() {
        btnBack       = findViewById(R.id.btnBack);
        etMobile      = findViewById(R.id.etMobile);
        etAmount      = findViewById(R.id.etAmount);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        cbDeclaration = findViewById(R.id.cbDeclaration);

        btn100  = findViewById(R.id.btn100);
        btn500  = findViewById(R.id.btn500);
        btn1000 = findViewById(R.id.btn1000);
        btn2000 = findViewById(R.id.btn2000);
        btn3000 = findViewById(R.id.btn3000);
        btn4000 = findViewById(R.id.btn4000);
        btn5000 = findViewById(R.id.btn5000);
    }

    private void hideKeyboard() {
        etMobile.clearFocus();
        etAmount.clearFocus();
        View focused = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    focused != null ? focused.getWindowToken() : etMobile.getWindowToken(), 0);
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    liveLat = location.getLatitude();
                    liveLog = location.getLongitude();
                }
            });
        }
    }

    private void handleLocationAndGenerateQR() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                liveLat = location.getLatitude();
                liveLog = location.getLongitude();
                generateQr();
            } else {
                showLocationPopup();
            }
        });
    }

    private void showLocationPopup() {
        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("Please Enable GPS Location")
                .setPositiveButton("Settings",
                        (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .show();
    }

    private void generateQr() {
        String mobile = etMobile.getText().toString().trim();
        String amount = etAmount.getText().toString().trim();

        if (mobile.length() != 10) { UiUtils.showToast(this, "Enter Valid Mobile Number", false); return; }
        if (amount.isEmpty())       { UiUtils.showToast(this, "Enter Amount", false); return; }

        int amtInt = Integer.parseInt(amount);


        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try {
            p.put("mobile", mobile);
            p.put("amount", amount);
            p.put("latitude",  String.valueOf(liveLat));
            p.put("longitude", String.valueOf(liveLog));
        } catch (Exception ignored) {}

        ApiClient.post(this, "ip-uatm/generate-qr", TOKEN, p,
                response -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        if (response.optInt("status") != 1) {
                            showErrorDialog(response.optString("message")); return;
                        }
                        JSONObject data = response.getJSONObject("data");

                        String txnId        = data.optString("id");
                        String ipayId       = data.optString("ipay_id");
                        String qrString     = data.optString("qr_string");
                        String amountVal    = data.optString("amount");
                        int    expirySeconds = data.optInt("expiry_sec", 30);

                        // Hand off to the dedicated QR page - it owns the
                        // QR display, the countdown, polling and status
                        // checks from here on.
                        Intent intent = new Intent(UatmActivity.this, UatmQrActivity.class);
                        intent.putExtra("txn_id", txnId);
                        intent.putExtra("ipay_id", ipayId);
                        intent.putExtra("qr_string", qrString);
                        intent.putExtra("mobile", mobile);
                        intent.putExtra("amount", amountVal);
                        intent.putExtra("expiry_sec", expirySeconds);
                        startActivity(intent);

                    } catch (Exception e) { showErrorDialog("QR Generation Failed"); }
                }),
                error -> runOnUiThread(() -> { UiUtils.dismissLoading(); showErrorDialog("Network Error"); })
        );
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("UPI ATM")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleLocationAndGenerateQR();
            } else {
                UiUtils.showToast(this, "Location Permission Required", false);
            }
        }
    }
}