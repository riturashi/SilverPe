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
import com.silverpe.R;
import com.silverpe.report.AirtelAepsReceiptActivity;
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

public class AirtelAepsDashboardActivity extends BaseActivity {

    // ── Views ──────────────────────────────────────────────────────────
    private TextInputEditText etMobile, etAadhar, etAmount, etBank, etDevice;
    private TextInputLayout layoutMobile, layoutAadhar;

    // Tab containers
    private LinearLayout tabCW, tabBE, tabMS, tabCD;

    // Aadhaar progress segments
    private View aadharSeg1, aadharSeg2, aadharSeg3;

    // Amount quick-chips
    private TextView chip500, chip1000, chip2000, chip5000;

    // Amount card (shown only for CW / CD)
    private LinearLayout layoutAmount;

    // OTP row (shown inline under amount, only for CW > 5000)
    private LinearLayout layoutOtpStatus;
    private TextView tvOtpStatus;
    private MaterialButton btnSendOtp;

    // Bank / Device row containers (for touch-intercept fix)
    private RelativeLayout rowBank, rowDevice;

    // ── State ──────────────────────────────────────────────────────────
    private String transactionType    = "CW";
    private String selectedBankId     = "";
    private String TOKEN              = "";
    private String selectedPkg        = "";
    private String selectedDeviceName = "";
    private ArrayList<String> bankNames = new ArrayList<>();
    private ArrayList<String> bankIds   = new ArrayList<>();

    // OTP state (NPCI: CW amount > 5000 requires OTP bound to biometric capture)
    private String otpReferenceKey        = "";
    private String pendingOtp             = "";
    private boolean otpVerifiedForThisTxn = false;
    private boolean otpRequestInFlight    = false; // prevents double "Send OTP" taps

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
        setContentView(R.layout.activity_airtel_aeps_dashboard);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupTabListeners();
        setupAadharSegments();
        setupQuickChips();
        setupAmountWatcher();
        setupTouchInterceptFix();
        fetchBankList();
        fetchLocationSilently();

        // Default active tab
        selectTab("CW");

        // Button / field listeners
        etBank.setOnClickListener(v -> showSearchableBankDialog());
        etDevice.setOnClickListener(v -> showDevicePickerDialog());
        btnSendOtp.setOnClickListener(v -> onSendOtpClicked());
        findViewById(R.id.btnProceed).setOnClickListener(v -> ensureFreshLocationThenProceed());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Settings se GPS ON karke wapas aaye - automatically capture resume karo
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
        tabCD = findViewById(R.id.tabCD);

        aadharSeg1 = findViewById(R.id.aadharSeg1);
        aadharSeg2 = findViewById(R.id.aadharSeg2);
        aadharSeg3 = findViewById(R.id.aadharSeg3);

        chip500  = findViewById(R.id.chip500);
        chip1000 = findViewById(R.id.chip1000);
        chip2000 = findViewById(R.id.chip2000);
        chip5000 = findViewById(R.id.chip5000);

        layoutOtpStatus = findViewById(R.id.layoutOtpStatus);
        tvOtpStatus     = findViewById(R.id.tvOtpStatus);
        btnSendOtp      = findViewById(R.id.btnSendOtp);

        rowBank   = findViewById(R.id.rowBank);
        rowDevice = findViewById(R.id.rowDevice);
    }

    // ══════════════════════════════════════════════════════════════════
    //  TOUCH INTERCEPT FIX (ScrollView ke andar tap delay wali dikkat)
    // ══════════════════════════════════════════════════════════════════
    private void setupTouchInterceptFix() {
        rowBank.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        rowDevice.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  PREMIUM TAB SWITCHING
    // ══════════════════════════════════════════════════════════════════
    private void setupTabListeners() {
        tabCW.setOnClickListener(v -> selectTab("CW"));
        tabBE.setOnClickListener(v -> selectTab("BE"));
        tabMS.setOnClickListener(v -> selectTab("MS"));
        tabCD.setOnClickListener(v -> selectTab("CD"));
    }

    private void selectTab(String type) {
        transactionType = type;

        // New txn type select hote hi purana OTP state clear karo
        resetOtpState();

        // Reset all to inactive
        setTabInactive(tabCW, R.id.tvTabCW);
        setTabInactive(tabBE, R.id.tvTabBE);
        setTabInactive(tabMS, R.id.tvTabMS);
        setTabInactive(tabCD, R.id.tvTabCD);

        // Activate selected
        switch (type) {
            case "CW": setTabActive(tabCW, R.id.tvTabCW); break;
            case "BE": setTabActive(tabBE, R.id.tvTabBE); break;
            case "MS": setTabActive(tabMS, R.id.tvTabMS); break;
            case "CD": setTabActive(tabCD, R.id.tvTabCD); break;
        }

        // Amount card visibility
        boolean showAmount = type.equals("CW") || type.equals("CD");
        layoutAmount.setVisibility(showAmount ? View.VISIBLE : View.GONE);

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
        chip500.setOnClickListener(v  -> setAmount("500"));
        chip1000.setOnClickListener(v -> setAmount("1000"));
        chip2000.setOnClickListener(v -> setAmount("2000"));
        chip5000.setOnClickListener(v -> setAmount("5000"));
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
                // Amount badalte hi purana OTP invalid maano (NPCI: OTP amount-bound hota hai)
                if (otpVerifiedForThisTxn || !otpReferenceKey.isEmpty()) {
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
        } else if (!otpReferenceKey.isEmpty()) {
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

    private void resetOtpState() {
        otpReferenceKey = "";
        pendingOtp = "";
        otpVerifiedForThisTxn = false;
        otpRequestInFlight = false;
        updateOtpStatusVisibility();
    }

    // ══════════════════════════════════════════════════════════════════
    //  SEND OTP BUTTON CLICK — inline validation pehle, phir OTP request
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
    //  BANK PICKER DIALOG
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
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                .setItems(DEVICE_NAMES, (dialog, which) -> {
                    selectedDeviceName = DEVICE_NAMES[which];
                    selectedPkg = DEVICE_PKGS[which];
                    etDevice.setText(selectedDeviceName);
                    etDevice.setFocusable(false);
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
                    UiUtils.showToast(AirtelAepsDashboardActivity.this,
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
    //  VALIDATE → (OTP already done via Send OTP button) → CAPTURE
    // ══════════════════════════════════════════════════════════════════
    private void validateAndCapture() {

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

        if (("CW".equals(transactionType) || "CD".equals(transactionType))
                && etAmount.getText().toString().trim().isEmpty()) {
            etAmount.setError("Amount Required"); return;
        }

        if (!isAppInstalled(selectedPkg)) {
            showInstallDialog(selectedPkg, selectedDeviceName);
            return;
        }

        // NPCI Rule: CW amount > 5000 => OTP verification required before biometric capture.
        // Ab OTP normally "Send OTP" button se pehle hi ho chuka hoga (amount type karte waqt).
        if (needsOtpNow()) {
            if (otpVerifiedForThisTxn && !otpReferenceKey.isEmpty()) {
                startCapture();
            } else {
                // Fallback: agar user ne Send OTP button skip kar diya, ab bhi maang lo
                UiUtils.showToast(this, "Please verify OTP before proceeding", false);
                requestWithdrawalOtp();
            }
            return;
        }

        startCapture();
    }

    // ══════════════════════════════════════════════════════════════════
    //  GENERATE OTP  →  POST ip-aeps/generate-otp
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

        ApiClient.post(this, "ip-aeps/generate-otp", TOKEN, body, response -> {
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
                        boolean otpRequired = dataObject.optBoolean("otp_required", true);

                        if (otpRequired) {
                            otpReferenceKey = dataObject.optString("reference_key", "");
                            updateOtpStatusVisibility();
                            UiUtils.showToast(this, "OTP sent to Aadhaar registered mobile", true);
                            showOtpEntryDialog();
                        } else {
                            // NPCI OTP not live on this route currently — proceed without OTP
                            otpReferenceKey = "";
                            pendingOtp = "";
                            otpVerifiedForThisTxn = true;
                            updateOtpStatusVisibility();
                        }
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
                .setPositiveButton("Verify", null) // manual override neeche
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
                    if (otpReferenceKey.isEmpty()) {
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

        String action;
        String pidOptions;

        if (selectedDeviceName.contains("FACE")) {

            action     = "in.gov.uidai.rdservice.face.CAPTURE";
            pidOptions = getFacePidOptions();

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("request", pidOptions);
            startActivityForResult(intent, 5);

        } else if (selectedDeviceName.contains("Iris")) {

            action     = "in.gov.uidai.rdservice.iris.CAPTURE";
            pidOptions = getIrisPidOptions();

            Intent intent = new Intent(action);
            intent.setPackage(selectedPkg);
            intent.putExtra("PID_OPTIONS", pidOptions);
            startActivityForResult(intent, 5);

        } else {

            action     = "in.gov.uidai.rdservice.fp.CAPTURE";
            pidOptions = getFpPidOptions(pendingOtp); // ← NPCI OTP embedded here for CW>5000

            Intent intent = new Intent(action);
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

        // 1 — Face RD
        if (data.hasExtra("response")) {
            pidXml = data.getStringExtra("response");
        }

        // 2 — Fingerprint RD (old devices)
        if (pidXml == null && data.hasExtra("PID_DATA")) {
            pidXml = data.getStringExtra("PID_DATA");
        }

        // 3 — Iris RD (Mantra MIS100)
        if (pidXml == null && data.hasExtra("pidData")) {
            pidXml = data.getStringExtra("pidData");
        }

        // 4 — Bundle fallback (Morpho, Startek, Iris)
        if (pidXml == null && data.getExtras() != null) {
            for (String key : data.getExtras().keySet()) {
                Object val = data.getExtras().get(key);
                if (val != null && val.toString().startsWith("<?xml")) {
                    pidXml = val.toString();
                    break;
                }
            }
        }

        // 5 — Nothing found
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
    //  SUBMIT AEPS API  (routes exactly as per routes.php api/v1/ip-aeps/*)
    // ══════════════════════════════════════════════════════════════════
    private void submitFinalAeps(String pid) {
        if (isFinishing() || isDestroyed()) return;

        UiUtils.showLoading(this);

        String endpoint;
        switch (transactionType) {
            case "BE": endpoint = "ip-aeps/balance-enquiry"; break;
            case "MS": endpoint = "ip-aeps/mini-statement";  break;
            case "CW": endpoint = "ip-aeps/cash-withdrawal"; break;
            case "CD": endpoint = "ip-aeps/cash-deposit";    break;
            default:   endpoint = "ip-aeps/balance-enquiry"; break;
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

            if ("CW".equals(transactionType) || "CD".equals(transactionType)) {
                body.put("amount", etAmount.getText().toString().trim());
            }

            // NPCI: reference_key required by backend when amount > 5000 for CW
            if ("CW".equals(transactionType) && !otpReferenceKey.isEmpty()) {
                body.put("reference_key", otpReferenceKey);
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
                                    AirtelAepsDashboardActivity.this,
                                    AirtelAepsReceiptActivity.class);
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
                            // Failed/pending txn: purana OTP/reference_key ab invalid,
                            // NPCI ke hisaab se fresh OTP hi lena hoga next attempt me
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
    //  PID OPTIONS
    // ══════════════════════════════════════════════════════════════════

    /**
     * @param otp OTP entered by user (required only for AEPS CW txns > Rs. 5000
     *            as per NPCI guidelines). Passed into PidOptions "otp" attribute
     *            so RD Service forwards it as part of biometric capture.
     */
    private String getFpPidOptions(String otp) {
        String otpAttr = (otp == null) ? "" : otp;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<PidOptions ver=\"1.0\">"
                + "<Opts fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" "
                + "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" "
                + "otp=\"" + otpAttr + "\" "
                + "posh=\"UNKNOWN\" env=\"P\" />"
                + "</PidOptions>";
    }

    private String getIrisPidOptions() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<PidOptions ver=\"1.0\">"
                + "<Opts fCount=\"0\" fType=\"0\" iCount=\"1\" iType=\"2\" "
                + "pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"15000\" "
                + "posh=\"UNKNOWN\" env=\"P\" />"
                + "</PidOptions>";
    }

    private String getFacePidOptions() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<PidOptions ver=\"1.0\" env=\"P\">"
                + "<Opts fCount=\"0\" fType=\"2\" iCount=\"1\" iType=\"2\" "
                + "pCount=\"0\" pType=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"20000\"/>"
                + "<CustOpts>"
                + "<Param name=\"txnId\" value=\"" + UUID.randomUUID() + "\"/>"
                + "<Param name=\"purpose\" value=\"auth\"/>"
                + "<Param name=\"language\" value=\"en\"/>"
                + "</CustOpts>"
                + "</PidOptions>";
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════
    private void fetchBankList() {
        ApiClient.get(this, "ip-aeps/bank-list", TOKEN, response -> {
            try {
                JSONArray data = response.getJSONArray("data");
                bankNames.clear();
                bankIds.clear();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    bankNames.add(obj.getString("bank_name"));
                    bankIds.add(obj.getString("bankId"));
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