package com.silverpe.aeps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.silverpe.BaseActivity;
import com.silverpe.DashboardActivity;
import com.silverpe.R;
import com.silverpe.report.FinoAepsReceiptActivity;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.gms.location.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class FinoAepsDashboardActivity extends BaseActivity {

    // ── Views ──────────────────────────────────────────────────────────
    private TextInputEditText etMobile, etAadhar, etAmount;
    private TextView etBank, etDevice;
    private TextInputLayout layoutMobile, layoutAadhar;
//feno Pay // 
    private LinearLayout tabCW, tabBE, tabMS;
    private View aadharSeg1, aadharSeg2, aadharSeg3;
    private LinearLayout layoutAmount;

    // OTP row (shown inline under amount, only for CW > 5000)
    private LinearLayout layoutOtpStatus;
    private TextView tvOtpStatus;
    private MaterialButton btnSendOtp;

    // ── State ──────────────────────────────────────────────────────────
    private String transactionType    = "CW";
    private String selectedBankId     = "";
    private String TOKEN              = "";
    private String selectedPkg        = "";
    private String selectedDeviceName = "";
    private ArrayList<String> bankNames = new ArrayList<>();
    private ArrayList<String> bankIds   = new ArrayList<>();

    // OTP state (NPCI: CW amount above 5000 requires OTP bound to biometric capture)
    private String otpRefId               = "";
    private String referenceNo            = "";
    private boolean otpVerifiedForThisTxn = false;
    private boolean otpRequestInFlight    = false;
    private String  pendingOtp            = "";   // verified OTP, sent inside PID at capture time

    private static final String[] DEVICE_NAMES = {
            "Mantra L1", "Morpho L1", "Startek L1",
            "Secugen L1", "Mantra Iris", "FACE"
    };
    private static final String[] DEVICE_PKGS = {
            "com.mantra.mfs110.rdservice",
            "com.idemia.l1rdservice",
            "com.acpl.com.startek.l1.rdservice",
            "com.secugen.rdservice",
            "com.mantra.mis100v2.rdservice",
            "in.gov.uidai.facerd"
    };

    // ── Location ───────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private double liveLat = 0.0, liveLog = 0.0;
    private boolean pendingCaptureAfterLocationEnable = false;
    private LocationCallback locationCallback;
    private static final int LOCATION_SETTINGS_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    // ══════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fino_aeps_dashboard);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupTabListeners();
        setupAadharSegments();
        setupQuickChips();
        setupAmountWatcher();
        fetchBankList();
        fetchLocationSilently();

        selectTab("CW");

        findViewById(R.id.layoutBankPicker).setOnClickListener(v -> showSearchableBankDialog());
        findViewById(R.id.layoutDevicePicker).setOnClickListener(v -> showDevicePickerDialog());
        btnSendOtp.setOnClickListener(v -> onSendOtpClicked());

        findViewById(R.id.btnProceed).setOnClickListener(v -> ensureFreshLocationThenProceed());
        findViewById(R.id.btnBack).setOnClickListener(v -> goToDashboard());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingCaptureAfterLocationEnable && isLocationEnabled()) {
            pendingCaptureAfterLocationEnable = false;
            fetchFreshLocationThenValidate();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  INIT VIEWS
    // ══════════════════════════════════════════════════════════════════
    private void initViews() {
        etMobile     = findViewById(R.id.etMobile);
        etAadhar     = findViewById(R.id.etAadhar);
        etAmount     = findViewById(R.id.etAmount);

        etBank       = findViewById(R.id.etBank);
        etDevice     = findViewById(R.id.etDevice);

        layoutMobile = findViewById(R.id.layoutMobile);
        layoutAadhar = findViewById(R.id.layoutAadhar);
        layoutAmount = findViewById(R.id.layoutAmount);

        tabCW = findViewById(R.id.tabCW);
        tabBE = findViewById(R.id.tabBE);
        tabMS = findViewById(R.id.tabMS);

        aadharSeg1 = findViewById(R.id.aadharSeg1);
        aadharSeg2 = findViewById(R.id.aadharSeg2);
        aadharSeg3 = findViewById(R.id.aadharSeg3);

        layoutOtpStatus = findViewById(R.id.layoutOtpStatus);
        tvOtpStatus     = findViewById(R.id.tvOtpStatus);
        btnSendOtp      = findViewById(R.id.btnSendOtp);
    }

    // ══════════════════════════════════════════════════════════════════
    //  TAB SWITCHING
    // ══════════════════════════════════════════════════════════════════
    private void setupTabListeners() {
        tabCW.setOnClickListener(v -> selectTab("CW"));
        tabBE.setOnClickListener(v -> selectTab("BE"));
        tabMS.setOnClickListener(v -> selectTab("MS"));
    }

    private void selectTab(String type) {
        transactionType = type;

        resetOtpState();

        setTabInactive(tabCW, R.id.tvTabCW);
        setTabInactive(tabBE, R.id.tvTabBE);
        setTabInactive(tabMS, R.id.tvTabMS);

        switch (type) {
            case "CW": setTabActive(tabCW, R.id.tvTabCW); break;
            case "BE": setTabActive(tabBE, R.id.tvTabBE); break;
            case "MS": setTabActive(tabMS, R.id.tvTabMS); break;
        }

        layoutAmount.setVisibility(type.equals("CW") ? View.VISIBLE : View.GONE);
        updateOtpStatusVisibility();
    }

    private void setTabActive(LinearLayout tab, int tvId) {
        tab.setBackgroundColor(0xFFFFFFFF);
        TextView tv = tab.findViewById(tvId);
        if (tv != null) {
            tv.setTextColor(0xFF1F3C88);
            tv.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
        }
    }

    private void setTabInactive(LinearLayout tab, int tvId) {
        tab.setBackgroundColor(0x33FFFFFF);
        TextView tv = tab.findViewById(tvId);
        if (tv != null) {
            tv.setTextColor(0xCCFFFFFF);
            tv.setTypeface(android.graphics.Typeface.create(
                    "sans-serif", android.graphics.Typeface.NORMAL));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  AADHAAR SEGMENT PROGRESS
    // ══════════════════════════════════════════════════════════════════
    private void setupAadharSegments() {
        etAadhar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                int len = s.length();
                aadharSeg1.setAlpha(len >= 4  ? 1.0f : 0.15f);
                aadharSeg2.setAlpha(len >= 8  ? 1.0f : 0.15f);
                aadharSeg3.setAlpha(len >= 12 ? 1.0f : 0.15f);
            }
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  QUICK AMOUNT CHIPS
    // ══════════════════════════════════════════════════════════════════
    private void setupQuickChips() {
        findViewById(R.id.chip500).setOnClickListener(v  -> setAmount("500"));
        findViewById(R.id.chip1000).setOnClickListener(v -> setAmount("1000"));
        findViewById(R.id.chip2000).setOnClickListener(v -> setAmount("2000"));
        findViewById(R.id.chip5000).setOnClickListener(v -> setAmount("5000"));
    }

    private void setAmount(String amount) {
        etAmount.setText(amount);
        etAmount.setSelection(amount.length());
    }

    // ══════════════════════════════════════════════════════════════════
    //  AMOUNT WATCHER → OTP STATUS ROW SHOW/HIDE
    // ══════════════════════════════════════════════════════════════════
    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Amount badalte hi purana OTP turant invalid — naya OTP lena zaroori
                if (otpVerifiedForThisTxn || !otpRefId.isEmpty() || !referenceNo.isEmpty()) {
                    resetOtpState();
                }
                updateOtpStatusVisibility();
            }
        });
    }

    private double getAmountValue() {
        try {
            return Double.parseDouble(etAmount.getText().toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // NPCI threshold: amount ABOVE 5000 (not equal to)
    private boolean needsOtpNow() {
        return "CW".equals(transactionType) && getAmountValue() > 5000;
    }

    private void updateOtpStatusVisibility() {
        if (layoutOtpStatus == null) return;

        boolean needsOtp = needsOtpNow();

        if (!needsOtp) {
            layoutOtpStatus.setVisibility(View.GONE);
            return;
        }

        layoutOtpStatus.setVisibility(View.VISIBLE);

        if (otpVerifiedForThisTxn) {
            tvOtpStatus.setText("✅ OTP Verified — Ready to Scan");
            tvOtpStatus.setTextColor(0xFF1B8A3B);
            btnSendOtp.setVisibility(View.GONE);
        } else if (otpRequestInFlight) {
            tvOtpStatus.setText("Sending OTP…");
            tvOtpStatus.setTextColor(0xFFB35400);
            btnSendOtp.setVisibility(View.GONE);
        } else if (!otpRefId.isEmpty()) {
            tvOtpStatus.setText("OTP sent — enter code to verify");
            tvOtpStatus.setTextColor(0xFFB35400);
            btnSendOtp.setText("Resend");
            btnSendOtp.setVisibility(View.VISIBLE);
        } else {
            tvOtpStatus.setText("⚠️ OTP required for withdrawal above ₹5,000");
            tvOtpStatus.setTextColor(0xFFB35400);
            btnSendOtp.setText("Send OTP");
            btnSendOtp.setVisibility(View.VISIBLE);
        }
    }

    // Har baar naya OTP maangte waqt purana state turant discard —
    // taaki ek expired/used OTP kabhi bhi dobara transaction ke liye use na ho.
    private void resetOtpState() {
        otpRefId = "";
        referenceNo = "";
        otpVerifiedForThisTxn = false;
        otpRequestInFlight = false;
        pendingOtp = "";
        updateOtpStatusVisibility();
    }

    // ══════════════════════════════════════════════════════════════════
    //  SEND OTP BUTTON CLICK
    // ══════════════════════════════════════════════════════════════════
    private void onSendOtpClicked() {
        if (otpRequestInFlight) return;

        if (etMobile.getText().toString().length() != 10) {
            etMobile.setError("10 Digits Required");
            UiUtils.showToast(this, "Please enter valid mobile number first", false);
            return;
        }
        if (etAadhar.getText().toString().length() != 12) {
            etAadhar.setError("12 Digits Required");
            UiUtils.showToast(this, "Please enter valid Aadhaar number first", false);
            return;
        }
        if (selectedBankId.isEmpty()) {
            UiUtils.showToast(this, "Please select bank first", false);
            return;
        }

        requestWithdrawalOtp();
    }

    // ══════════════════════════════════════════════════════════════════
    //  BANK PICKER DIALOG (Searchable)
    // ══════════════════════════════════════════════════════════════════
    private void showSearchableBankDialog() {
        if (bankNames.isEmpty()) {
            fetchBankList();
            UiUtils.showToast(this, "Bank list loading, please wait...", false);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_search_bank, null);
        EditText searchEdit = v.findViewById(R.id.searchBank);
        ListView listView   = v.findViewById(R.id.bankList);

        final ArrayList<String> filteredNames = new ArrayList<>(bankNames);
        final ArrayList<String> filteredIds   = new ArrayList<>(bankIds);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, filteredNames);
        listView.setAdapter(adapter);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filteredNames.clear();
                filteredIds.clear();
                for (int i = 0; i < bankNames.size(); i++) {
                    if (bankNames.get(i).toLowerCase().contains(query)) {
                        filteredNames.add(bankNames.get(i));
                        filteredIds.add(bankIds.get(i));
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            etBank.setText(filteredNames.get(position));
            selectedBankId = filteredIds.get(position);
            // Bank badalte hi purana OTP invalid maano
            resetOtpState();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  DEVICE PICKER DIALOG
    // ══════════════════════════════════════════════════════════════════
    private void showDevicePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Device")
                .setItems(DEVICE_NAMES, (dlg, which) -> {
                    selectedDeviceName = DEVICE_NAMES[which];
                    selectedPkg        = DEVICE_PKGS[which];
                    etDevice.setText(selectedDeviceName);
                }).show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  LOCATION HELPERS
    // ══════════════════════════════════════════════════════════════════
    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private void fetchLocationSilently() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    liveLat = loc.getLatitude();
                    liveLog = loc.getLongitude();
                }
            });
        }
    }

    private void ensureFreshLocationThenProceed() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        if (!isLocationEnabled()) {
            pendingCaptureAfterLocationEnable = true;
            showLocationPopup();
            return;
        }

        fetchFreshLocationThenValidate();
    }

    private void fetchFreshLocationThenValidate() {

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {

            if (loc != null) {
                liveLat = loc.getLatitude();
                liveLog = loc.getLongitude();
                validateAndCapture();
            } else {
                requestFreshLocation();
            }

        }).addOnFailureListener(e -> requestFreshLocation());
    }

    private void requestFreshLocation() {

        UiUtils.showLoading(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {

                fusedLocationClient.removeLocationUpdates(locationCallback);
                UiUtils.dismissLoading();

                if (result != null && result.getLastLocation() != null) {
                    liveLat = result.getLastLocation().getLatitude();
                    liveLog = result.getLastLocation().getLongitude();
                    validateAndCapture();
                } else {
                    UiUtils.showToast(FinoAepsDashboardActivity.this,
                            "Unable to fetch location. Please try again.", false);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
        } catch (Exception e) {
            UiUtils.dismissLoading();
        }
    }

    private void showLocationPopup() {
        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("GPS must be ON for Authentication. Enable now?")
                .setCancelable(false)
                .setPositiveButton("Settings", (d, w) -> startActivityForResult(
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        LOCATION_SETTINGS_REQUEST))
                .setNegativeButton("Cancel", (d, w) -> pendingCaptureAfterLocationEnable = false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ensureFreshLocationThenProceed();
            } else {
                UiUtils.showToast(this, "Location permission is required for Authentication", false);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  VALIDATE & CAPTURE
    // ══════════════════════════════════════════════════════════════════
    private void validateAndCapture() {

        if (etMobile.getText() == null ||
                etMobile.getText().toString().trim().length() != 10) {
            etMobile.setError("10 Digits Required");
            etMobile.requestFocus();
            return;
        }
        if (etAadhar.getText() == null ||
                etAadhar.getText().toString().trim().length() != 12) {
            etAadhar.setError("12 Digits Required");
            etAadhar.requestFocus();
            return;
        }
        if (selectedBankId.isEmpty()) {
            UiUtils.showToast(this, "Please Select Bank", false);
            return;
        }
        if ("CW".equals(transactionType)) {
            if (etAmount.getText() == null ||
                    etAmount.getText().toString().trim().isEmpty()) {
                etAmount.setError("Amount Required");
                etAmount.requestFocus();
                return;
            }
        }
        if (selectedPkg.isEmpty()) {
            UiUtils.showToast(this, "Please Select Device", false);
            return;
        }
        if (!isAppInstalled(selectedPkg)) {
            showInstallDialog(selectedPkg, selectedDeviceName);
            return;
        }

        // NPCI Rule: CW amount above 5000 => OTP verification required before capture
        if (needsOtpNow()) {
            if (otpVerifiedForThisTxn && !otpRefId.isEmpty() && !referenceNo.isEmpty()) {
                startCapture();
            } else {
                UiUtils.showToast(this, "Please verify OTP before proceeding", false);
                requestWithdrawalOtp();
            }
            return;
        }

        startCapture();
    }

    // ══════════════════════════════════════════════════════════════════
    //  GENERATE OTP → POST aeps/generate-otp
    // ══════════════════════════════════════════════════════════════════
    private void requestWithdrawalOtp() {
        otpRequestInFlight = true;
        updateOtpStatusVisibility();
        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();
        try {
            body.put("mobile", etMobile.getText().toString().trim());
            body.put("aadhar", etAadhar.getText().toString().trim());
            body.put("bank", selectedBankId);
            body.put("amount", etAmount.getText().toString().trim());
            body.put("latitude", liveLat);
            body.put("longitude", liveLog);
        } catch (Exception e) {
            UiUtils.dismissLoading();
            otpRequestInFlight = false;
            updateOtpStatusVisibility();
            showDebugDialog("Request Error", "Failed to prepare OTP request.");
            return;
        }

        ApiClient.post(this, "aeps/generate-otp", TOKEN, body, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                otpRequestInFlight = false;

                if (response == null) {
                    updateOtpStatusVisibility();
                    showDebugDialog("System Error", "Server returned an empty response.");
                    return;
                }
                try {
                    int status = response.optInt("status", 0);
                    String apiMessage = response.optString("message", "Unable to send OTP.");
                    JSONObject dataObject = response.optJSONObject("data");

                    if (status == 1 && dataObject != null) {
                        otpRefId    = dataObject.optString("otp_refid", "");
                        referenceNo = dataObject.optString("reference_no", "");
                        updateOtpStatusVisibility();
                        UiUtils.showToast(this, "OTP sent to Aadhaar registered mobile", true);
                        showOtpEntryDialog();
                    } else {
                        resetOtpState();
                        showDebugDialog("OTP Failed", apiMessage);
                    }
                } catch (Exception e) {
                    updateOtpStatusVisibility();
                    showDebugDialog("Data Error", "Unable to parse OTP response.");
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            otpRequestInFlight = false;
            updateOtpStatusVisibility();
            showDebugDialog("Connection Error", "Timeout or no internet while requesting OTP.");
        }));
    }

    // ══════════════════════════════════════════════════════════════════
    //  OTP ENTRY DIALOG (6-digit) + RESEND
    // ══════════════════════════════════════════════════════════════════
    private void showOtpEntryDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_enter_otp, null);
        EditText etOtp     = v.findViewById(R.id.etOtp);
        TextView tvResend  = v.findViewById(R.id.tvResendOtp);
        TextView tvOtpNote = v.findViewById(R.id.tvOtpNote);

        tvOtpNote.setText("As per NPCI guidelines, OTP verification is required for withdrawal amount above ₹5,000.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter OTP")
                .setView(v)
                .setCancelable(false)
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", (d, w) -> resetOtpState())
                .create();

        tvResend.setOnClickListener(view -> {
            dialog.dismiss();
            requestWithdrawalOtp();
        });

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String otp = etOtp.getText().toString().trim();
                    if (otp.length() != 6) {
                        etOtp.setError("Enter complete 6 digit OTP");
                        return;
                    }
                    if (otpRefId.isEmpty() || referenceNo.isEmpty()) {
                        UiUtils.showToast(this, "OTP session expired. Please resend OTP.", false);
                        dialog.dismiss();
                        updateOtpStatusVisibility();
                        return;
                    }
                    pendingOtp = otp;
                    otpVerifiedForThisTxn = true;
                    updateOtpStatusVisibility();
                    dialog.dismiss();
                    UiUtils.showToast(this, "OTP Verified. You can Scan & Proceed now.", true);
                })
        );

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  START RD SERVICE CAPTURE
    // ══════════════════════════════════════════════════════════════════
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

            // NPCI: verified OTP embedded here for CW above 5000; empty when not required
            String otpAttr = pendingOtp;
            String pidOptions =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<PidOptions ver=\"1.0\">" +
                            "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" " +
                            "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" " +
                            "otp=\"" + otpAttr + "\" " +
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

        if (requestCode == LOCATION_SETTINGS_REQUEST) {
            if (pendingCaptureAfterLocationEnable && isLocationEnabled()) {
                pendingCaptureAfterLocationEnable = false;
                fetchFreshLocationThenValidate();
            }
            return;
        }

        if (requestCode != 5) return;

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
        if (pidXml == null && data.hasExtra("pidData")) {
            pidXml = data.getStringExtra("pidData");
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
            case "BE": endpoint = "aeps/balance-enquiry"; break;
            case "MS": endpoint = "aeps/mini-statement";  break;
            case "CW": endpoint = "aeps/cash-withdrawal"; break;
            default:   endpoint = "aeps/balance-enquiry"; break;
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
            body.put("type",      transactionType);
            body.put("ipaddress", getDeviceIpAddress());
            body.put("remark",    "AEPS Transaction");

            if ("CW".equals(transactionType)) {
                body.put("amount", etAmount.getText().toString().trim());
            }

            // NPCI: attach otp_refid + reference_no when amount above 5000
            if (needsOtpNow()) {
                body.put("otp_refid", otpRefId);
                body.put("reference_no", referenceNo);
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
                                    FinoAepsDashboardActivity.this,
                                    FinoAepsReceiptActivity.class);
                            intent.putExtra("from", "transaction");
                            intent.putExtra("id", dataObject.optString("id", ""));

                            if ("MS".equals(transactionType)) {
                                JSONArray statementArray =
                                        dataObject.optJSONArray("ministatement");
                                if (statementArray != null && statementArray.length() > 0) {
                                    intent.putExtra("statement_json",
                                            statementArray.toString());
                                }
                            }

                            startActivity(intent);
                            resetFormAfterTxn();
                        } else {
                            showDebugDialog("Transaction Failed", apiMessage);
                            // Failed/pending: purana OTP ab bekaar hai, fresh OTP lena zaroori
                            if (needsOtpNow()) {
                                resetOtpState();
                            }
                        }
                    } else {
                        showDebugDialog("Message", apiMessage);
                        if (needsOtpNow()) {
                            resetOtpState();
                        }
                    }
                } catch (Exception e) {
                    showDebugDialog("Data Error", "Unable to parse server response.");
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDebugDialog("Connection Error",
                    "Timeout or no internet. Please check your network.");
            if (needsOtpNow()) {
                resetOtpState();
            }
        }));
    }

    private void resetFormAfterTxn() {
        etMobile.setText("");
        etAadhar.setText("");
        etAmount.setText("");
        etBank.setText("");
        etDevice.setText("");
        selectedBankId = "";
        selectedPkg = "";
        selectedDeviceName = "";
        resetOtpState();
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════
    @Override
    public void onBackPressed() {
        goToDashboard();
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}