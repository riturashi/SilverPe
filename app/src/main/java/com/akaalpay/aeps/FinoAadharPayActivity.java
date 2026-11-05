package com.akaalpay.aeps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.report.FinoAadharPayReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.gms.location.*;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class FinoAadharPayActivity extends BaseActivity {

    private TextInputEditText etMobile, etAadhar, etAmount, etBank, etDevice;
    private TextInputLayout layoutMobile, layoutAadhar;
    private String transactionType = "M", selectedBankId = "", TOKEN = "";
    private String selectedPkg = "", selectedDeviceName = "";
    private ArrayList<String> bankNames = new ArrayList<>();
    private ArrayList<String> bankIds = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private double liveLat = 0.0, liveLog = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fino_aadharpay_dashboard);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        fetchBankList();
        checkLocationPermission();

        etBank.setOnClickListener(v -> showSearchableBankDialog());
        etDevice.setOnClickListener(v -> showDevicePickerDialog());
        findViewById(R.id.btnProceed).setOnClickListener(v -> validateAndCapture());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etMobile = findViewById(R.id.etMobile);
        etAadhar = findViewById(R.id.etAadhar);
        etAmount = findViewById(R.id.etAmount);
        etBank   = findViewById(R.id.etBank);
        etDevice = findViewById(R.id.etDevice);
        layoutAadhar = findViewById(R.id.layoutAadhar);

        // FIX: TextInputEditText fields ko focusable false karo
        // taaki click event consume na ho aur dialog open ho
        etBank.setFocusable(false);
        etDevice.setFocusable(false);
    }

    // ══════════════════════════════════════════════════════════════════
    //  BANK PICKER
    // ══════════════════════════════════════════════════════════════════
    private void showSearchableBankDialog() {
        if (bankNames.isEmpty()) { fetchBankList(); return; }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_search_bank, null);
        EditText searchEdit = v.findViewById(R.id.searchBank);
        ListView listView   = v.findViewById(R.id.bankList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, bankNames);
        listView.setAdapter(adapter);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = adapter.getItem(position);
            etBank.setText(selectedName);
            for (int i = 0; i < bankNames.size(); i++) {
                if (bankNames.get(i).equals(selectedName)) {
                    selectedBankId = bankIds.get(i);
                    break;
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  DEVICE PICKER
    // ══════════════════════════════════════════════════════════════════
    private void showDevicePickerDialog() {
        String[] devices = {
                "Mantra L1", "Morpho L1", "Startek L1",
                "Secugen L1", "Mantra Iris", "FACE"
        };
        String[] pkgs = {
                "com.mantra.mfs110.rdservice",
                "com.idemia.l1rdservice",
                "com.acpl.com.startek.l1.rdservice",
                "com.secugen.rdservice",
                "com.mantra.mis100v2.rdservice",
                "in.gov.uidai.facerd"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Device")
                .setItems(devices, (dialog, which) -> {
                    selectedDeviceName = devices[which];
                    selectedPkg        = pkgs[which];
                    etDevice.setText(selectedDeviceName);
                }).show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  VALIDATE & CAPTURE
    // ══════════════════════════════════════════════════════════════════
    private void validateAndCapture() {
        if (liveLat == 0.0) { checkLocationPermission(); return; }
        if (etMobile.getText().toString().length() != 10) {
            etMobile.setError("10 Digits Required"); return;
        }
        if (etAadhar.getText().toString().length() != 12) {
            etAadhar.setError("12 Digits Required"); return;
        }
        if (selectedBankId.isEmpty()) {
            UiUtils.showToast(this, "Please Select Bank", false); return;
        }
        if (selectedPkg.isEmpty()) {
            UiUtils.showToast(this, "Please Select Device", false); return;
        }
        if (!isAppInstalled(selectedPkg)) {
            showInstallDialog(selectedPkg, selectedDeviceName); return;
        }

        startCapture();
    }

    private void startCapture() {

        if (selectedDeviceName.contains("FACE")) {

            String pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\" env=\"P\">" +
                            "<Opts fCount=\"0\" fType=\"2\" iCount=\"1\" iType=\"2\" " +
                            "pCount=\"0\" pType=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"20000\"/>" +
                            "<CustOpts>" +
                            "<Param name=\"txnId\" value=\"" + UUID.randomUUID() + "\"/>" +
                            "<Param name=\"purpose\" value=\"auth\"/>" +
                            "<Param name=\"language\" value=\"en\"/>" +
                            "</CustOpts>" +
                            "</PidOptions>";

            Intent intent = new Intent("in.gov.uidai.rdservice.face.CAPTURE");
            intent.setPackage(selectedPkg);
            intent.putExtra("request", pidOptions);
            startActivityForResult(intent, 5);

        } else if (selectedDeviceName.contains("Iris")) {

            String pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\">" +
                            "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" iType=\"2\" " +
                            "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" " +
                            "posh=\"UNKNOWN\" env=\"P\" />" +
                            "</PidOptions>";

            Intent intent = new Intent("in.gov.uidai.rdservice.iris.CAPTURE");
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, 5);

        } else {

            String pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\">" +
                            "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" " +
                            "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" " +
                            "posh=\"UNKNOWN\" env=\"P\" />" +
                            "</PidOptions>";

            Intent intent = new Intent("in.gov.uidai.rdservice.fp.CAPTURE");
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, 5);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ON ACTIVITY RESULT
    // ══════════════════════════════════════════════════════════════════
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 5) return;

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
            submitFinalAeps(pidXml);
        } else {
            showDebugDialog("Capture Failed", parseErrorInfo(pidXml));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  SUBMIT API
    // ══════════════════════════════════════════════════════════════════
    private void submitFinalAeps(String pid) {
        if (isFinishing() || isDestroyed()) return;

        UiUtils.showLoading(this);

        String endpoint;
        switch (transactionType) {
            case "BE": endpoint = "aeps/balance-enquiry";    break;
            case "MS": endpoint = "aeps/mini-statement";     break;
            case "M":  endpoint = "aadhar_pay/transaction";  break;
            default:   endpoint = "aadhar_pay/transaction";  break;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("mobile",    etMobile.getText().toString().trim());
            body.put("aadhar",    etAadhar.getText().toString().trim());
            body.put("bank",      selectedBankId);
            body.put("pid",       pid);
            body.put("device",    selectedDeviceName);
            body.put("latitude",  liveLat);
            body.put("longitude", liveLog);
            body.put("type",      "M");
            body.put("ipaddress", getDeviceIpAddress());

            if ("M".equals(transactionType) || "CD".equals(transactionType)) {
                body.put("amount", etAmount.getText().toString().trim());
            }
        } catch (Exception e) {
            UiUtils.dismissLoading();
            showDebugDialog("Request Error", "Failed to prepare transaction data.");
            return;
        }

        ApiClient.post(this, endpoint, TOKEN, body, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                if (response == null) {
                    showDebugDialog("System Error", "Server returned an empty response.");
                    return;
                }

                try {
                    int status            = response.optInt("status", 0);
                    String apiMessage     = response.optString("message", "Transaction Failed");
                    JSONObject dataObject = response.optJSONObject("data");

                    if (status == 1 && dataObject != null) {
                        int resCode = dataObject.optInt("response_code", -1);
                        if (resCode == 1) {
                            Intent intent = new Intent(
                                    FinoAadharPayActivity.this,
                                    FinoAadharPayReceiptActivity.class);
                            intent.putExtra("from", "transaction");
                            intent.putExtra("id", dataObject.optString("id", ""));
                            startActivity(intent);
                            finish();
                        } else {
                            showDebugDialog("Transaction Failed", apiMessage);
                        }
                    } else {
                        showDebugDialog("Message", apiMessage);
                    }
                } catch (Exception e) {
                    showDebugDialog("Data Error", "Unable to parse server response.");
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDebugDialog("Connection Error", "Timeout or no internet. Please check your network.");
        }));
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    liveLat = loc.getLatitude();
                    liveLog = loc.getLongitude();
                }
            });
        }
    }

    public String getDeviceIpAddress() {
        try {
            for (java.util.Enumeration<java.net.NetworkInterface> en =
                 java.net.NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                java.net.NetworkInterface intf = en.nextElement();
                for (java.util.Enumeration<java.net.InetAddress> enumIpAddr =
                     intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            // silent
        }
        return "0.0.0.0";
    }

    private void fetchBankList() {
        ApiClient.get(this, "aeps/bank-list", TOKEN, response -> {
            try {
                JSONArray data = response.getJSONArray("data");
                bankNames.clear();
                bankIds.clear();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    bankNames.add(obj.getString("bankName"));
                    bankIds.add(obj.getString("bid"));
                }
            } catch (Exception e) {
                // silent
            }
        }, null);
    }

    private boolean isAppInstalled(String packageName) {
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
                        (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + pkg))))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String parseErrorInfo(String xml) {
        try {
            if (xml.contains("errInfo=\""))
                return xml.split("errInfo=\"")[1].split("\"")[0];
            if (xml.contains("errInfo='"))
                return xml.split("errInfo='")[1].split("'")[0];
        } catch (Exception e) {
            return "Unknown Error in RD Service";
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
}