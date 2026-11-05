package com.silverpe.dmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class AddBeneficiaryActivity extends AppCompatActivity {

    private AutoCompleteTextView bankDropdown;
    private EditText accNo, ifsc, holder, otp;
    private LinearLayout otpLayout;
    private Button submitBtn;

    private String selectedBankId = "";
    private String referenceKey = "";
    private String beneId = "";
    private String mobile = "";
    private String TOKEN = "";

    private List<String> bankNames = new ArrayList<>();
    private Map<String, String> bankMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_beneficiary);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        mobile = getIntent().getStringExtra("mobile");

        loadBankList();

        submitBtn.setOnClickListener(v -> {
            if (referenceKey.isEmpty()) {
                registerBeneficiary();
            } else {
                verifyOtp();
            }
        });
    }

    private void initViews() {
        bankDropdown = findViewById(R.id.payoutAddBankName);
        accNo = findViewById(R.id.payoutAddAccNo);
        ifsc = findViewById(R.id.payoutAddIfsc);
        holder = findViewById(R.id.payoutAddHolder);
        otp = findViewById(R.id.etAddBankOtp);

        otpLayout = findViewById(R.id.layoutAddBankOtp);
        submitBtn = findViewById(R.id.payoutBtnSaveBank);
    }

    // ================= BANK LIST =================
    private void loadBankList() {

        UiUtils.showLoading(this);

        ApiClient.get(this, "dmt/bank-list", TOKEN, response -> {

            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                try {

                    if (response.optInt("status") != 1) {
                        showDialog("Error", "Bank list failed");
                        return;
                    }

                    JSONArray arr = response.getJSONArray("data");

                    bankNames.clear();
                    bankMap.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject obj = arr.getJSONObject(i);

                        String name = obj.optString("name");
                        String id = String.valueOf(obj.optInt("bankId"));

                        bankNames.add(name);
                        bankMap.put(name, id);
                    }

                    // 👉 IMPORTANT CHANGE
                    // Dropdown adapter hata diya
                    // Dialog me use hoga

                    bankDropdown.setOnClickListener(v -> openBankDialog());

                } catch (Exception e) {
                    showDialog("Error", "Parsing error");
                }
            });

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDialog("Error", "Server connection failed");
        }));
    }

    private void openBankDialog() {

        if (bankNames == null || bankNames.size() == 0) {
            Toast.makeText(this, "Bank list not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_bank_list, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText search = view.findViewById(R.id.searchBank);
        ListView listView = view.findViewById(R.id.bankList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                bankNames
        );

        listView.setAdapter(adapter);

        // SELECT BANK
        listView.setOnItemClickListener((parent, v, position, id) -> {

            String selected = adapter.getItem(position);

            bankDropdown.setText(selected);
            selectedBankId = bankMap.get(selected);

            dialog.dismiss();
        });

        // SEARCH FILTER
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
        });
    }

    // ================= REGISTER =================
    private void registerBeneficiary() {

        String name = holder.getText().toString().trim();
        String acc = accNo.getText().toString().trim();
        String ifscCode = ifsc.getText().toString().trim();

        if (selectedBankId.isEmpty()) {
            showDialog("Warning", "Select Bank");
            return;
        }

        if (name.isEmpty()) {
            showDialog("Warning", "Enter Account Holder Name");
            return;
        }

        if (acc.isEmpty()) {
            showDialog("Warning", "Enter Account Number");
            return;
        }

        if (ifscCode.isEmpty()) {
            showDialog("Warning", "Enter IFSC Code");
            return;
        }

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {
            body.put("mobile", mobile);
            body.put("benename", name);
            body.put("bankid", selectedBankId);
            body.put("accno", acc);
            body.put("ifsc_code", ifscCode);
        } catch (Exception e) {}

        ApiClient.post(this, "dmt/register-beneficiary", TOKEN, body, response -> {

            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                try {

                    if (response.optInt("status") == 1) {

                        JSONObject data = response.getJSONObject("data");

                        referenceKey = data.optString("referenceKey");
                        beneId = data.optString("beneficiaryId");

                        showOtpDialog();

                    } else {
                        showDialog("Failed", response.optString("message"));
                    }

                } catch (Exception e) {
                    showDialog("Error", "Parsing failed");
                }

            });

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDialog("Error", "Server error");
        }));
    }

    private void showOtpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("OTP Sent")
                .setMessage("OTP sent on remitter mobile number")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {

                    otpLayout.setVisibility(View.VISIBLE);
                    submitBtn.setText("Verify OTP");

                })
                .show();
    }

    // ================= VERIFY OTP =================
    private void verifyOtp() {

        String otpValue = otp.getText().toString().trim();

        if (otpValue.isEmpty()) {
            showDialog("Warning", "Enter OTP");
            return;
        }

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {
            body.put("mobile", mobile);
            body.put("otp", otpValue);
            body.put("referenceKey", referenceKey);
            body.put("beneId", beneId);
        } catch (Exception e) {}

        ApiClient.post(this, "dmt/register-beneficiary-verify", TOKEN, body, response -> {

            runOnUiThread(() -> {
                UiUtils.dismissLoading();

                try {

                    if (response.optInt("status") == 1) {

                        showSuccessDialog(response.optString("message"));

                    } else {
                        showDialog("Failed", response.optString("message"));
                    }

                } catch (Exception e) {
                    showDialog("Error", "Parsing failed");
                }

            });

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDialog("Error", "Server error");
        }));
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {

                    finish();

                })
                .show();
    }

    // ================= DIALOG =================
    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}