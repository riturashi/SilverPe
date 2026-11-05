package com.silverpe.aeps;

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

import com.silverpe.DashboardActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class FinoTwoFaAuthActivity extends BaseLocationActivity {

    private String TOKEN = "";

    private String selectedDevice = "Mantra L1";

    private String selectedPkg = "com.mantra.mfs110.rdservice";

    private String bankRoute = "";

    private TextView tvDeviceShortName, tvName, tvMobile, tvAadhar;

    private Button btnCapture;

    private CheckBox cbConsent;

    private static final int RD_SERVICE_REQUEST = 1001;

    private ArrayList<String> deviceNames = new ArrayList<>();

    private ArrayList<String> devicePkgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fino_2fa_auth);
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

                showDialogBox("Warning", "Please accept the consent first");
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {

            Intent intent = new Intent(this, DashboardActivity.class);

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

            finish();

            overridePendingTransition(0, 0);
        });
    }

    private void initViews() {

        tvDeviceShortName = findViewById(R.id.tvDeviceShortName);

        cbConsent = findViewById(R.id.cbConsent);

        btnCapture = findViewById(R.id.btnScanNow);

        tvName = findViewById(R.id.tvName);

        tvMobile = findViewById(R.id.tvMobile);

        tvAadhar = findViewById(R.id.tvAadhar);
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(this, DashboardActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);

        finish();

        overridePendingTransition(0, 0);
    }

    // ================= LOCATION CALLBACK (BaseLocationActivity) =================

    @Override
    protected void onLocationReady() {
        openRdService();
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

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_search_device, null);

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

            btnCapture.setText(
                    selectedDevice.contains("FACE")
                            ? "FACE SCAN"
                            : "SCAN BIOMETRIC"
            );

            dialog.dismiss();
        });

        dialog.show();
    }

    // ================= DIALOG =================

    private void showDialogBox(String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSuccessDialog(String message) {

        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {

                    startActivity(
                            new Intent(
                                    this,
                                    FinoAepsDashboardActivity.class
                            )
                    );

                    finish();
                })
                .show();
    }

    // ================= PROFILE =================

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

                            tvAadhar.setText(
                                    "Aadhaar: " +
                                            data.optString(
                                                    "profile_aadhar",
                                                    "XXXXXXXXXXXX"
                                            )
                            );

                            bankRoute = data.optString(
                                    "bank_route",
                                    ""
                            );
                        }

                    } catch (Exception ignored) {
                    }

                }),

                error -> showDialogBox(
                        "Error",
                        "Profile fetch failed"
                )
        );
    }

    // ================= RD =================

    private void openRdService() {

        String action;

        String pidOptions;

        if (selectedDevice.contains("FACE")) {

            // ---- FACE ----
            action = "in.gov.uidai.rdservice.face.CAPTURE";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"0\" " +
                            "pCount=\"1\" format=\"0\" pidVer=\"2.0\" " +
                            "timeout=\"10000\"/>" +
                            "<CustOpts>" +
                            "<Param name=\"txnId\" value=\"" +
                            UUID.randomUUID().toString() +
                            "\"/>" +
                            "</CustOpts>" +
                            "</PidOptions>";

        } else if (selectedDevice.contains("Iris")) {

            // ---- IRIS ----
            action = "in.gov.uidai.rdservice.iris.CAPTURE";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" " +
                            "iType=\"0\" pCount=\"0\" format=\"0\" " +
                            "pidVer=\"2.0\" timeout=\"10000\"/>" +
                            "</PidOptions>";

        } else {

            // ---- FINGERPRINT ----
            action = "in.gov.uidai.rdservice.fp.CAPTURE";

            pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" " +
                            "format=\"0\" pidVer=\"2.0\" timeout=\"10000\"/>" +
                            "</PidOptions>";
        }

        if (isPackageInstalled(selectedPkg)) {

            Intent intent = new Intent(action);

            intent.setPackage(selectedPkg);

            if (selectedDevice.contains("FACE")) {

                intent.putExtra("request", pidOptions);

            } else {

                intent.putExtra("PID_OPTIONS", pidOptions);
            }

            startActivityForResult(intent, RD_SERVICE_REQUEST);

        } else {

            showInstallDialog(selectedPkg, selectedDevice);
        }
    }

    // ================= RESULT =================

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RD_SERVICE_REQUEST) return;

        if (data == null) {

            showDialogBox(
                    "Error",
                    "No response from RD Service"
            );

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

                if (val != null &&
                        val.toString().startsWith("<?xml")) {

                    pidXml = val.toString();

                    break;
                }
            }
        }

        if (pidXml == null) {

            showDialogBox(
                    "Error",
                    "PID XML not received from RD Service"
            );

            return;
        }

        String errCode = getXmlValue(pidXml, "errCode");

        String errInfo = getXmlValue(pidXml, "errInfo");

        if ("0".equals(errCode)) {

            doKycSubmit(pidXml);

        } else {

            String message =
                    (errInfo != null && !errInfo.isEmpty())
                            ? errInfo
                            : "Capture Failed (Error Code: " + errCode + ")";

            showDialogBox("RD Error", message);
        }
    }

    private String getXmlValue(String xml, String key) {

        try {

            int start = xml.indexOf(key + "=\"");

            if (start == -1) return null;

            start += key.length() + 2;

            int end = xml.indexOf("\"", start);

            return xml.substring(start, end);

        } catch (Exception ignored) {

            return null;
        }
    }

    // ================= API =================

    private void doKycSubmit(String pidXml) {

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {

            body.put("device", selectedDevice);

            body.put("pid", pidXml);

            body.put("latitude", liveLat);

            body.put("longitude", liveLog);

        } catch (JSONException ignored) {
        }

        ApiClient.post(
                this,
                "aeps/2fa/authenticate",
                TOKEN,
                body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    JSONObject data = response.optJSONObject("data");

                    if (response != null
                            && response.optInt("status") == 1
                            && data != null
                            && data.optInt("response_code") == 1) {

                        showSuccessDialog("2FA Auth Done!");

                    } else {

                        String msg =
                                (response != null)
                                        ? response.optString(
                                        "message",
                                        "Submission Failed"
                                )
                                        : "Submission Failed";

                        showDialogBox("Failed", msg);
                    }

                }),

                err -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    showDialogBox(
                            "Error",
                            "Server Connection Failed"
                    );
                })
        );
    }

    // ================= UTIL =================

    private boolean isPackageInstalled(String packageName) {

        try {

            getPackageManager()
                    .getPackageInfo(packageName, 0);

            return true;

        } catch (Exception ignored) {

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
                                        Uri.parse(
                                                "market://details?id=" + pkg
                                        )
                                )
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }
}