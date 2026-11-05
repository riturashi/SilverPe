package com.silverpe.dmt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class DmtCaptureActivity extends AppCompatActivity {

    private String TOKEN = "";

    private String selectedDevice = "Mantra L1";
    private String selectedPkg = "com.mantra.mfs110.rdservice";

    private CheckBox cbConsent;
    private TextView tvDeviceShortName, tvMobile, tvAadhar;
    private ImageView imgSelectedDevice;
    private Button btnScanNow;

    private String mobile = "", referenceKey = "", aadhaar = "", wadh = "";

    private double liveLat = 0.0, liveLog = 0.0;

    private FusedLocationProviderClient fusedLocationClient;

    private static final int RD_SERVICE_REQUEST = 5;

    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayList<String> devicePkgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmt_capture);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        loadDeviceList();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        mobile = getIntent().getStringExtra("mobile");
        referenceKey = getIntent().getStringExtra("referenceKey");
        aadhaar = getIntent().getStringExtra("aadhaar");
        wadh = getIntent().getStringExtra("wadh");

        if (mobile == null) mobile = "";
        if (referenceKey == null) referenceKey = "";
        if (aadhaar == null) aadhaar = "";
        if (wadh == null) wadh = "";

        tvMobile.setText("+91 " + mobile);

        if (aadhaar.length() >= 4) {
            tvAadhar.setText("XXXX XXXX " + aadhaar.substring(aadhaar.length() - 4));
        }

        tvDeviceShortName.setText(selectedDevice);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangeDevice).setOnClickListener(v -> showSearchableDeviceDialog());

        btnScanNow.setOnClickListener(v -> {

            if (!cbConsent.isChecked()) {
                UiUtils.showToast(this, "Please accept declaration", false);
                return;
            }

            if (wadh.isEmpty()) {
                UiUtils.showToast(this, "WADH missing, retry again", false);
                return;
            }

            handleLocationAndStart();
        });
    }

    private void initViews() {

        cbConsent = findViewById(R.id.cbConsent);

        tvDeviceShortName = findViewById(R.id.tvDeviceShortName);

        imgSelectedDevice = findViewById(R.id.imgSelectedDevice);

        tvMobile = findViewById(R.id.tvMobile);

        tvAadhar = findViewById(R.id.tvAadhar);

        btnScanNow = findViewById(R.id.btnScanNow);
    }

    private void loadDeviceList() {

        deviceNames.clear();
        devicePkgs.clear();

        deviceNames.add("Mantra L1");
        devicePkgs.add("com.mantra.mfs110.rdservice");

        deviceNames.add("Morpho L1");
        devicePkgs.add("com.idemia.l1rdservice");

        deviceNames.add("Startek L1");
        devicePkgs.add("com.acpl.registersdk_l1");

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

        searchEdit.setHint("Search Device");

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
                btnScanNow.setText("PROCEED FACE SCAN");
            } else if (selectedDevice.contains("Iris")) {
                btnScanNow.setText("SCAN IRIS");
            } else {
                btnScanNow.setText("SCAN BIOMETRIC");
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void handleLocationAndStart() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );

            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {

            if (location != null) {

                liveLat = location.getLatitude();

                liveLog = location.getLongitude();

                checkAppAndCapture();

            } else {

                showLocationPopup();
            }
        });
    }

    private void showLocationPopup() {

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("Enable GPS for eKYC")
                .setPositiveButton("Settings",
                        (d, w) -> startActivity(
                                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        ))
                .show();
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
        String extraKey;

        if (selectedDevice.contains("FACE")) {

            // ── FACE ──────────────────────────────────────────
            action   = "in.gov.uidai.rdservice.face.CAPTURE";
            extraKey = "request";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"2\" iCount=\"1\" iType=\"2\" pCount=\"0\" " +
                            "pType=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"20000\" " +
                            "wadh=\"" + wadh + "\"/>" +
                            "<CustOpts>" +
                            "<Param name=\"txnId\" value=\"" + UUID.randomUUID().toString() + "\"/>" +
                            "</CustOpts>" +
                            "</PidOptions>";

        } else if (selectedDevice.contains("Iris")) {

            // ── IRIS ──────────────────────────────────────────
            action   = "in.gov.uidai.rdservice.iris.CAPTURE";
            extraKey = "PID_OPTIONS";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" iType=\"2\" pCount=\"0\" " +
                            "format=\"0\" pidVer=\"2.0\" timeout=\"15000\" posh=\"UNKNOWN\" env=\"P\" " +
                            "wadh=\"" + wadh + "\" />" +
                            "</PidOptions>";

        } else {

            // ── FINGER (Mantra / Morpho / Startek / Secugen) ──
            action   = "in.gov.uidai.rdservice.fp.CAPTURE";
            extraKey = "PID_OPTIONS";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\">" +
                            "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" pCount=\"0\" format=\"0\" " +
                            "pidVer=\"2.0\" timeout=\"15000\" posh=\"UNKNOWN\" env=\"P\" " +
                            "wadh=\"" + wadh + "\" />" +
                            "</PidOptions>";
        }

        Intent intent = new Intent(action);
        intent.setPackage(selectedPkg);
        intent.putExtra(extraKey, pidOptions);

        startActivityForResult(intent, RD_SERVICE_REQUEST);
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

            submitKyc(pidXml);

        } else {

            showDebugDialog(
                    "Capture Failed",
                    parseErrorInfo(pidXml)
            );
        }
    }

    private String parseErrorInfo(String xml) {

        try {

            if (xml.contains("errInfo=\"")) {

                return xml.split("errInfo=\"")[1].split("\"")[0];

            } else if (xml.contains("errInfo='")) {

                return xml.split("errInfo='")[1].split("'")[0];
            }

        } catch (Exception e) {

            return "Unknown Error";
        }

        return "Error parsing XML";
    }

    private void showDebugDialog(String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void submitKyc(String pidXml) {

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {

            body.put("mobile", mobile);

            body.put("referenceKey", referenceKey);

            body.put("pid", pidXml);

            body.put("latitude", liveLat);
            body.put("device", deviceNames);

            body.put("longitude", liveLog);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        ApiClient.post(
                this,
                "dmt/register-remitter-kyc",
                TOKEN,
                body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (response != null &&
                            response.optInt("status") == 1) {

                        new AlertDialog.Builder(DmtCaptureActivity.this)
                                .setTitle("Success")
                                .setMessage("Remitter Added Successfully\n\nNow Start The Transaction")
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, which) -> {

                                    Intent i = new Intent(
                                            DmtCaptureActivity.this,
                                            DmtActivity.class
                                    );

                                    i.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(i);

                                }).show();

                    } else {

                        showDebugDialog(
                                "KYC Failed",
                                response != null ?
                                        response.optString("message") :
                                        "Error"
                        );
                    }

                }),

                err -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    showDebugDialog("API Error", err);
                })
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
                .setPositiveButton("Install", (d, w) ->
                        startActivity(
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + pkg)
                                )
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }
}