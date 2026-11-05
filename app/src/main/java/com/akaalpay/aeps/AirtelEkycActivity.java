package com.akaalpay.aeps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class AirtelEkycActivity extends BaseLocationActivity {

    private String TOKEN = "", wadhValueFromApi = "";

    private String selectedDevice = "Mantra L1";

    private String selectedPkg = "com.mantra.mfs110.rdservice";

    private TextView tvDeviceShortName, tvName, tvMobile, tvAadhar;

    private Button btnCapture;

    private CheckBox cbConsent;

    private static final int RD_SERVICE_REQUEST = 1001;

    private ArrayList<String> deviceNames = new ArrayList<>();

    private ArrayList<String> devicePkgs = new ArrayList<>();

    // ================= FACE WADH CONSTANTS (same as Jio) =================

    private static final String VERSION = "2.5";
    private static final String RESIDENT_AUTHENTICATION_TYPE = "P";
    private static final String RESIDENT_CONSENT = "Y";
    private static final String LOCAL_LANGUAGE_IR = "N";
    private static final String DECRYPTION = "N";
    private static final String PRINT_FORMAT = "N";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_airtel_ekyc);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        loadDeviceList();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);

        TOKEN = pref.getString("TOKEN", "");

        fetchProfile();

        tvDeviceShortName.setText(selectedDevice);

        findViewById(R.id.btnChangeDevice)
                .setOnClickListener(v -> showSearchableDeviceDialog());

        btnCapture.setOnClickListener(v -> {

            if (cbConsent.isChecked()) {

                fetchLocationAndProceed();

            } else {

                UiUtils.showToast(this, "Please accept the consent first", false);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {

        tvDeviceShortName = findViewById(R.id.tvDeviceShortName);

        cbConsent = findViewById(R.id.cbConsent);

        btnCapture = findViewById(R.id.btnScanNow);

        tvName = findViewById(R.id.tvName);

        tvMobile = findViewById(R.id.tvMobile);

        tvAadhar = findViewById(R.id.tvAadhar);
    }

    // ================= LOCATION CALLBACK (BaseLocationActivity) =================

    @Override
    protected void onLocationReady() {
        checkStatusAndCapture();
    }

    // ================= DEVICE =================

    private void loadDeviceList() {

        deviceNames.clear();

        devicePkgs.clear();

        deviceNames.add("Mantra L1");
        devicePkgs.add("com.mantra.mfs110.rdservice");

        deviceNames.add("Morpho L1");
        devicePkgs.add("com.idemia.l1rdservice");

        deviceNames.add("Startek L1");
        devicePkgs.add("com.acpl.com.startek.l1.rdservice");

        deviceNames.add("Secugen L1");
        devicePkgs.add("com.secugen.rdservice");

        deviceNames.add("Mantra Iris");
        devicePkgs.add("com.mantra.mis100v2.rdservice");

        deviceNames.add("FACE");
        devicePkgs.add("in.gov.uidai.facerd");
    }

    private void showSearchableDeviceDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_search_device, null);

        EditText searchEdit = view.findViewById(R.id.searchDevice);

        ListView listView = view.findViewById(R.id.deviceList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceNames
        );

        listView.setAdapter(adapter);

        builder.setView(view);

        AlertDialog dialog = builder.create();

        searchEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {

            String selectedName = adapter.getItem(position);

            selectedDevice = selectedName;

            for (int i = 0; i < deviceNames.size(); i++) {

                if (deviceNames.get(i).equals(selectedName)) {

                    selectedPkg = devicePkgs.get(i);

                    break;
                }
            }

            tvDeviceShortName.setText(selectedDevice);

            if (selectedDevice.contains("FACE")) {

                btnCapture.setText("FACE SCAN");

            } else if (selectedDevice.contains("Iris")) {

                btnCapture.setText("IRIS SCAN");

            } else {

                btnCapture.setText("SCAN BIOMETRIC");
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    // ================= PROFILE =================

    private void fetchProfile() {

        ApiClient.get(this, "user/my-profile", TOKEN,

                response -> runOnUiThread(() -> {

                    try {

                        JSONObject data = response.optJSONObject("data");

                        if (data != null) {

                            tvName.setText(data.optString("name", "Merchant"));

                            tvMobile.setText("+91 " + data.optString("mobile"));

                            String adr = data.optString("profile_aadhar", "XXXXXXXXXXXX");

                            tvAadhar.setText("Aadhaar: " + adr);
                        }

                    } catch (Exception e) {
                        // silent
                    }

                }),

                error -> {
                });
    }

    // ================= STATUS =================

    private void checkStatusAndCapture() {

        if (TOKEN.isEmpty()) {

            UiUtils.showToast(this, "Session Expired", false);

            return;
        }

        UiUtils.showLoading(this);

        ApiClient.get(this, "ip-aeps/ip-kyc-status", TOKEN,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (response != null && response.optInt("status") == 1) {

                        JSONObject data = response.optJSONObject("data");

                        int responseCode = response.optInt("response_code");

                        if (data != null) {

                            if (responseCode == 0) {

                                responseCode = data.optInt("response_code");
                            }

                            wadhValueFromApi = data.optString("ekyc_wadh_value", "");
                        }

                        if (responseCode == 2) {

                            openRdService(wadhValueFromApi);

                        } else {

                            String message = response.optString("message");

                            new AlertDialog.Builder(this)
                                    .setTitle("Status")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }

                    } else {

                        UiUtils.showToast(
                                this,
                                response != null ? response.optString("message") : "Server Error",
                                false
                        );
                    }

                }),

                err -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    UiUtils.showToast(this, "Connection Failed", false);
                })
        );
    }

    // ================= FACE WADH GENERATE (same as Jio) =================

    private String getWADH() {
        return getWADHValue(
                VERSION +
                        RESIDENT_AUTHENTICATION_TYPE +
                        RESIDENT_CONSENT +
                        LOCAL_LANGUAGE_IR +
                        DECRYPTION +
                        PRINT_FORMAT
        );
    }

    private String getWADHValue(String plainWADH) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainWADH.getBytes());
            return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // ================= RD =================

    private void openRdService(String wadh) {

        if (!isPackageInstalled(selectedPkg)) {

            showInstallDialog(selectedPkg, selectedDevice);

            return;
        }

        String action;
        String pidOptions;

        if (selectedDevice.contains("FACE")) {

            // NOTE: FACE ke liye ab status-API wala "wadh" use nahi hota -
            // Jio jaisa locally SHA-256 se generate karke use hota hai.
            action = "in.gov.uidai.rdservice.face.CAPTURE";

            String faceWadh = getWADH();

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"0\" " +
                            "pCount=\"1\" format=\"0\" pidVer=\"2.0\" " +
                            "timeout=\"10000\" wadh=\"" + faceWadh + "\"/>" +
                            "<CustOpts>" +
                            "<Param name=\"txnId\" value=\"" + UUID.randomUUID() + "\"/>" +
                            "</CustOpts>" +
                            "</PidOptions>";

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("request", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);

        } else if (selectedDevice.contains("Iris")) {

            action = "in.gov.uidai.rdservice.iris.CAPTURE";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" iType=\"2\" " +
                            "pCount=\"0\" format=\"0\" pidVer=\"2.0\" " +
                            "timeout=\"15000\" wadh=\"" + wadh + "\"/>" +
                            "</PidOptions>";

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);

        } else {

            action = "in.gov.uidai.rdservice.fp.CAPTURE";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"1\" fType=\"2\" format=\"0\" " +
                            "pidVer=\"2.0\" timeout=\"10000\" " +
                            "wadh=\"" + wadh + "\"/>" +
                            "</PidOptions>";

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);
        }
    }

    // ================= RESULT =================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RD_SERVICE_REQUEST) return;

        if (data == null) {

            UiUtils.showToast(this, "RD Service returned no data", false);

            return;
        }

        String pidXml = null;

        // 1 — Face RD
        if (data.hasExtra("response")) {

            pidXml = data.getStringExtra("response");
        }

        // 2 — Fingerprint RD
        if (pidXml == null && data.hasExtra("PID_DATA")) {

            pidXml = data.getStringExtra("PID_DATA");
        }

        // 3 — Iris RD (Mantra MIS100)
        if (pidXml == null && data.hasExtra("pidData")) {

            pidXml = data.getStringExtra("pidData");
        }

        // 4 — Bundle fallback
        if (pidXml == null && data.getExtras() != null) {

            for (String key : data.getExtras().keySet()) {

                Object val = data.getExtras().get(key);

                if (val != null && val.toString().startsWith("<?xml")) {

                    pidXml = val.toString();

                    break;
                }
            }
        }

        if (pidXml == null) {

            UiUtils.showToast(this, "PID XML not received", false);

            return;
        }

        if (pidXml.contains("errCode=\"0\"") || pidXml.contains("errCode='0'")) {

            doKycSubmit(pidXml);

        } else {

            UiUtils.showToast(this, "Capture Failed", false);
        }
    }

    // ================= SUBMIT =================

    private void doKycSubmit(String pidXml) {

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {

            body.put("device", selectedDevice);

            body.put("pid", pidXml);

            body.put("latitude", liveLat);

            body.put("longitude", liveLog);

        } catch (JSONException e) {
            // silent
        }

        ApiClient.post(this, "ip-aeps-auth/do-ekyc", TOKEN, body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (response != null && response.optInt("response_code") == 1) {

                        UiUtils.showToast(this, "eKYC Success!", true);

                        startActivity(new Intent(this, Airtel2FaAuthActivity.class));

                        finish();

                    } else {

                        UiUtils.showToast(
                                this,
                                response != null ? response.optString("message") : "Submission Failed",
                                false
                        );
                    }

                }),

                err -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    UiUtils.showToast(this, "Server Connection Failed", false);
                })
        );
    }

    // ================= UTIL =================

    private boolean isPackageInstalled(String packageName) {

        try {

            getPackageManager().getPackageInfo(packageName, 0);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    private void showInstallDialog(String pkg, String name) {

        new AlertDialog.Builder(this)
                .setTitle("Install RD Service")
                .setMessage("Please install RD service for " + name)
                .setPositiveButton("Install",
                        (d, w) -> startActivity(
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + pkg)
                                )
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }
}