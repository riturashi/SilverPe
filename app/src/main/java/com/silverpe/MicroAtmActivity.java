package com.silverpe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.report.MicroAtmReceiptActivity;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.LocationHelper;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MicroAtmActivity extends AppCompatActivity {

    private String tType = "ATMCW", phpType = "CW", TOKEN = "";

    private EditText etMobile, etAmount;
    private LinearLayout layoutAmount;

    private double latitude = 0.0, longitude = 0.0;

    private String merchantCode = "";
    private String initiateId = "";

    // ✅ SDK RESULT
    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        // ✅ SDK se wapas aaya → Receipt open
                        Intent intent = new Intent(this, MicroAtmReceiptActivity.class);

                        // 🔥 FIX: refNo nahi, id bhejna hai
                        intent.putExtra("id", initiateId);

                        startActivity(intent);
                        finish();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro_atm);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        etMobile = findViewById(R.id.etMobile);
        etAmount = findViewById(R.id.etAmount);
        layoutAmount = findViewById(R.id.layoutAmount);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupAmountShortcuts();

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleGroup);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnBE) {
                    tType = "ATMBE";
                    phpType = "BE";
                    layoutAmount.setVisibility(View.GONE);
                } else {
                    tType = "ATMCW";
                    phpType = "CW";
                    layoutAmount.setVisibility(View.VISIBLE);
                }
            }
        });

        loadToken();
        loadProfile();

        findViewById(R.id.btnProceed).setOnClickListener(v -> validateAndInitiate());
        findViewById(R.id.btnOrderDevice).setOnClickListener(v -> {
            startActivity(new Intent(this, MicroAtmOrderActivity.class));
        });

    }


    private void setupAmountShortcuts() {
        int[] ids = {R.id.amt500, R.id.amt1000, R.id.amt2000, R.id.amt3000, R.id.amt5000, R.id.amt10000};
        String[] values = {"500", "1000", "2000", "3000", "5000", "10000"};

        for (int i = 0; i < ids.length; i++) {
            final String val = values[i];
            View btn = findViewById(ids[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    etAmount.setText(val);
                    etAmount.setSelection(etAmount.getText().length());
                });
            }
        }
    }

    // ✅ STEP 1
    private void validateAndInitiate() {

        if (etMobile.getText().toString().length() != 10) {
            UiUtils.showToast(this, "Enter 10 digit mobile", false);
            return;
        }

        if (phpType.equals("CW") && etAmount.getText().toString().trim().isEmpty()) {
            UiUtils.showToast(this, "Enter Amount", false);
            return;
        }

        if (merchantCode.isEmpty()) {
            UiUtils.showToast(this, "Merchant not loaded", false);
            return;
        }

        fetchLocationThenApi();
    }

    // ✅ STEP 2
    private void fetchLocationThenApi() {

        UiUtils.showLoading(this);

        new LocationHelper(this).getLocation(loc -> {
            runOnUiThread(() -> {

                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();

                    callInitiateApi();

                } else {
                    UiUtils.dismissLoading();
                    UiUtils.showToast(this, "Unable to fetch location", false);
                }
            });
        });
    }

    // ✅ STEP 3
    private void callInitiateApi() {

        HashMap<String, String> params = new HashMap<>();
        params.put("mobile", etMobile.getText().toString());
        params.put("type", phpType);
        params.put("amount", phpType.equals("CW") ? etAmount.getText().toString() : "0");
        params.put("deviceManufacturerId", "1");
        params.put("ipaddress", getDeviceIPAddress());
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));

        ApiClient.post(this, "microatm/initiate", TOKEN, new JSONObject(params), response -> {
            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                try {
                    JSONObject data = response.optJSONObject("data");

                    if (data != null && data.has("id")) {

                        initiateId = data.getString("id");

                        String refNo = data.getString("referenceno");

                        launchSDK(refNo);

                    } else {
                        UiUtils.showToast(this, "Invalid API response", false);
                    }

                } catch (Exception e) {
                    Log.e("MATM", e.getMessage());
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Server error", false);
        }));
    }

    // ✅ STEP 4
    private void launchSDK(String refNo) {

        if (refNo == null || refNo.isEmpty()) {
            UiUtils.showToast(this, "Reference missing", false);
            return;
        }

        try {
            Intent intent = new Intent(this, com.example.matm.MatmHostActivity.class);

            intent.putExtra("partnerid", "PS00443");
            intent.putExtra("partnerapikey", "UFMwMDQ0MzdmMjViMTljODk2MmY0ZTI5MDIwNjE3NGVlYzVhNzdk");
            intent.putExtra("submerchantid", merchantCode);
            intent.putExtra("mobile", etMobile.getText().toString());
            intent.putExtra("amount", phpType.equals("CW") ? etAmount.getText().toString() : "0");
            intent.putExtra("txnid", refNo);
            intent.putExtra("ttype", tType);
            intent.putExtra("latitude", String.valueOf(latitude));
            intent.putExtra("longitude", String.valueOf(longitude));
            intent.putExtra("remarks", "MicroATM Transaction");


            launcher.launch(intent);

        } catch (Exception e) {
            Log.e("SDK_ERROR", e.toString());
            UiUtils.showToast(this, "SDK Launch Failed", false);
        }
    }

    private String getDeviceIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.1.1";
    }

    private void loadToken() {
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
    }

    private void loadProfile() {
        ApiClient.get(this, "user/my-profile", TOKEN, response -> {
            runOnUiThread(() -> {
                try {
                    merchantCode = response.getJSONObject("data").optString("username", "");
                    Log.e("PROFILE", "Merchant: " + merchantCode);
                } catch (Exception ignored) {}
            });
        }, error -> {});
    }
}