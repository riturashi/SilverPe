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

public class Airtel2FaAuthActivity extends BaseLocationActivity {

    private String TOKEN = "";

    private String selectedDevice = "Mantra L1";
    private String selectedPkg = "com.mantra.mfs110.rdservice";

    private CheckBox cbConsent;

    private TextView tvDeviceShortName, tvName, tvMobile, tvAadhar;

    private ImageView imgSelectedDevice;

    private Button btnSubmitAuth;

    private static final int RD_SERVICE_REQUEST = 5;

    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayList<String> devicePkgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_airtel_2fa_auth);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        loadDeviceList();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);

        TOKEN = pref.getString("TOKEN", "");

        fetchProfile();

        tvDeviceShortName.setText(selectedDevice);

        findViewById(R.id.btnChangeDevice).setOnClickListener(v -> showSearchableDeviceDialog());

        btnSubmitAuth.setOnClickListener(v -> {

            if (cbConsent.isChecked()) {

                fetchLocationAndProceed();

            } else {

                UiUtils.showToast(this, "Please accept the declaration", false);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {

        cbConsent = findViewById(R.id.cbConsent);

        tvDeviceShortName = findViewById(R.id.tvDeviceShortName);

        imgSelectedDevice = findViewById(R.id.imgSelectedDevice);

        tvName = findViewById(R.id.tvName);

        tvMobile = findViewById(R.id.tvMobile);

        tvAadhar = findViewById(R.id.tvAadhar);

        btnSubmitAuth = findViewById(R.id.btnScanNow);
    }

    // ================= LOCATION CALLBACK (BaseLocationActivity) =================

    @Override
    protected void onLocationReady() {
        checkAppAndCapture();
    }

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

                btnSubmitAuth.setText("PROCEED FACE SCAN");

            } else if (selectedDevice.contains("Iris")) {

                btnSubmitAuth.setText("PROCEED IRIS SCAN");

            } else {

                btnSubmitAuth.setText("TAP TO SCAN");
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void checkAppAndCapture() {

        if (isPackageInstalled(selectedPkg)) {

            startCapture();

        } else {

            showInstallDialog(selectedPkg, selectedDevice);
        }
    }

    private void startCapture() {

        String action;
        String pidOptions;

        if (selectedDevice.contains("FACE")) {

            action = "in.gov.uidai.rdservice.face.CAPTURE";
            pidOptions = getFacePidOptions();

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("request", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);

        } else if (selectedDevice.contains("Iris")) {

            action = "in.gov.uidai.rdservice.iris.CAPTURE";
            pidOptions = getIrisPidOptions();

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);

        } else {

            action = "in.gov.uidai.rdservice.fp.CAPTURE";
            pidOptions = getFpPidOptions();

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, RD_SERVICE_REQUEST);
        }
    }

    private String getFpPidOptions() {

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<PidOptions ver=\"1.0\">" +
                "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" " +
                "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" " +
                "posh=\"UNKNOWN\" env=\"P\" />" +
                "</PidOptions>";
    }

    private String getIrisPidOptions() {

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<PidOptions ver=\"1.0\">" +
                "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" iType=\"2\" " +
                "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" " +
                "posh=\"UNKNOWN\" env=\"P\" />" +
                "</PidOptions>";
    }

    private String getFacePidOptions() {

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<PidOptions ver=\"1.0\" env=\"P\">" +
                "<Opts fCount=\"0\" fType=\"2\" iCount=\"1\" iType=\"2\" " +
                "pCount=\"0\" pType=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"20000\"/>" +
                "<CustOpts>" +
                "<Param name=\"txnId\" value=\"" + UUID.randomUUID().toString() + "\"/>" +
                "<Param name=\"purpose\" value=\"auth\"/>" +
                "<Param name=\"language\" value=\"en\"/>" +
                "</CustOpts>" +
                "</PidOptions>";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RD_SERVICE_REQUEST) return;

        if (data == null) {

            UiUtils.showToast(this, "RD Service returned no data", false);

            return;
        }

        String pidXml = null;

        if (data.hasExtra("response")) {

            pidXml = data.getStringExtra("response");
        }

        if (pidXml == null && data.hasExtra("PID_DATA")) {

            pidXml = data.getStringExtra("PID_DATA");
        }

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

        if (pidXml.contains("errCode=\"0\"") ||
                pidXml.contains("errCode='0'")) {

            submit2Fa(pidXml);

        } else {

            String err = parseErrorInfo(pidXml);

            showDebugDialog("Capture Failed", err);
        }
    }

    private void showDebugDialog(String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String parseErrorInfo(String xml) {

        try {

            if (xml.contains("errInfo=\"")) {

                return xml.split("errInfo=\"")[1].split("\"")[0];

            } else if (xml.contains("errInfo='")) {

                return xml.split("errInfo='")[1].split("'")[0];
            }

        } catch (Exception e) {

            return "Unknown Error in RD Service";
        }

        return "Error parsing XML";
    }

    private void submit2Fa(String pidXml) {

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {

            body.put("device", selectedDevice);

            body.put("pid", pidXml);

            body.put("latitude", liveLat);

            body.put("longitude", liveLog);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        ApiClient.post(
                this,
                "ip-aeps/ip-aeps-auth-login",
                TOKEN,
                body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (response == null) {

                        UiUtils.showToast(this, "Server not responding", false);

                        return;
                    }

                    int status = response.optInt("status");

                    JSONObject data = response.optJSONObject("data");

                    int responseCode = data != null ?
                            data.optInt("response_code") : 0;

                    String message = response.optString("message");

                    if (status == 1 && responseCode == 1) {

                        UiUtils.showToast(this, "2FA Success", true);

                        startActivity(
                                new Intent(
                                        Airtel2FaAuthActivity.this,
                                        AirtelAepsDashboardActivity.class
                                )
                        );

                        finish();

                    } else {

                        new AlertDialog.Builder(Airtel2FaAuthActivity.this)
                                .setTitle("Authentication Failed")
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton("OK",
                                        (dialog, which) -> dialog.dismiss())
                                .show();
                    }

                }),

                err -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    new AlertDialog.Builder(Airtel2FaAuthActivity.this)
                            .setTitle("API Error")
                            .setMessage(err)
                            .setPositiveButton("OK", null)
                            .show();
                })
        );
    }

    private void fetchProfile() {

        ApiClient.get(
                this,
                "user/my-profile",
                TOKEN,

                response -> runOnUiThread(() -> {

                    try {

                        JSONObject data = response.optJSONObject("data");

                        if (data != null) {

                            tvName.setText(
                                    data.optString("name", "Merchant")
                            );

                            tvMobile.setText(
                                    "+91 " + data.optString("mobile")
                            );

                            String adr = data.optString(
                                    "profile_aadhar",
                                    "XXXXXXXXXXXX"
                            );

                            tvAadhar.setText("Aadhaar: " + adr);
                        }

                    } catch (Exception e) {

                        e.printStackTrace();
                    }

                }),

                error -> {
                }
        );
    }

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