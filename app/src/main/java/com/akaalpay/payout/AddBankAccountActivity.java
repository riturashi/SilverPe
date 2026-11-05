package com.akaalpay.payout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddBankAccountActivity extends AppCompatActivity {

    private AutoCompleteTextView etBankDropdown;
    private TextInputEditText etAccNo, etIfsc, etHolder, etPass, etOtp;
    private TextInputLayout layoutOtp;
    private Button btnSave;

    private List<BankModel> bankList = new ArrayList<>();
    private String selectedBankId = "";
    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bank);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        fetchBankNames();

        btnSave.setOnClickListener(v -> handleBankSubmission());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etBankDropdown = findViewById(R.id.payoutAddBankName);
        etAccNo = findViewById(R.id.payoutAddAccNo);
        etIfsc = findViewById(R.id.payoutAddIfsc);
        etHolder = findViewById(R.id.payoutAddHolder);
        etPass = findViewById(R.id.payoutAddPassword);
        etOtp = findViewById(R.id.etAddBankOtp);
        layoutOtp = findViewById(R.id.layoutAddBankOtp);
        btnSave = findViewById(R.id.payoutBtnSaveBank);

        // 🔥 IMPORTANT (Dropdown disable)
        etBankDropdown.setFocusable(false);
        etBankDropdown.setClickable(true);

        etBankDropdown.setOnClickListener(v -> openBankDialog());
    }

    // 🔥 FETCH BANK LIST
    private void fetchBankNames() {

        UiUtils.showLoading(this);

        ApiClient.get(this, "payout/bank-list", TOKEN, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                if (response == null) {
                    showDialog("Error", "Server not responding");
                    return;
                }

                if (response.optInt("status") == 1) {

                    JSONArray data = response.optJSONArray("data");

                    if (data != null) {

                        bankList.clear();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);

                            bankList.add(new BankModel(
                                    obj.optString("bank_id"),
                                    obj.optString("bank_name"),
                                    obj.optString("ifsc")
                            ));
                        }
                    }

                } else {
                    showDialog("Error", response.optString("message"));
                }

            });

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDialog("Error", "Connection Failed");
        }));
    }


    private void openBankDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_search_bank, null);

        EditText search = view.findViewById(R.id.searchBank);
        ListView listView = view.findViewById(R.id.bankList);

        // 🔥 DIRECT MODEL LIST USE KARO
        ArrayAdapter<BankModel> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                bankList
        );

        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.show();

        // ✅ FIXED CLICK
        listView.setOnItemClickListener((parent, v, position, id) -> {

            BankModel selected = (BankModel) parent.getItemAtPosition(position);

            selectedBankId = selected.bank_id;
            etBankDropdown.setText(selected.bank_name);

            if (selected.ifsc != null && !selected.ifsc.isEmpty()) {
                etIfsc.setText(selected.ifsc);
            }

            dialog.dismiss();
        });

        // 🔥 SEARCH (WORKING)
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.getFilter().filter(s);
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // 🔥 SUBMIT
    private void handleBankSubmission() {

        String bankText = etBankDropdown.getText().toString().trim();

        if (selectedBankId.isEmpty() || bankText.isEmpty()) {
            showDialog("Error", "Please select a bank");
            return;
        }

        String acc = etAccNo.getText().toString().trim();
        String ifsc = etIfsc.getText().toString().trim();
        String holder = etHolder.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        if (acc.isEmpty() || ifsc.isEmpty() || holder.isEmpty() || pass.isEmpty()) {
            showDialog("Error", "Please fill all fields");
            return;
        }

        saveBankToServer(acc, ifsc, holder, pass);
    }

    // 🔥 API CALL
    private void saveBankToServer(String acc, String ifsc, String holder, String pass) {

        UiUtils.showLoading(this);
        btnSave.setEnabled(false);

        JSONObject params = new JSONObject();

        try {
            params.put("bank_id", selectedBankId);
            params.put("account_no", acc);
            params.put("ifsc_code", ifsc);
            params.put("holder_name", holder);
            params.put("transaction_password", pass);

            if (layoutOtp.getVisibility() == View.VISIBLE) {
                params.put("otp", etOtp.getText().toString().trim());
            }

        } catch (Exception ignored) {}

        ApiClient.post(this, "payout/add-bank", TOKEN, params, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();
                btnSave.setEnabled(true);

                if (response == null) {
                    showDialog("Error", "Server not responding");
                    return;
                }

                if (response.optInt("status") == 1) {

                    JSONObject data = response.optJSONObject("data");

                    if (data != null && data.optBoolean("otp_active", false)) {

                        layoutOtp.setVisibility(View.VISIBLE);
                        btnSave.setText("VERIFY OTP & SAVE");

                        showDialog("OTP Sent", "We Have Sent OTP On Your Registered Mobile No");

                    } else {

                        new AlertDialog.Builder(this)
                                .setTitle("Success")
                                .setMessage(response.optString("message"))
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, which) -> {

                                    // 🔥 ONLY SUCCESS → CLEAR FORM
                                    clearForm();

                                })
                                .show();
                    }

                } else {
                    showDialog("Failed", response.optString("message"));
                }

            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            btnSave.setEnabled(true);
            showDialog("Error", "Connection Failed");

        }));
    }
    private void clearForm() {

        etBankDropdown.setText("");
        etAccNo.setText("");
        etIfsc.setText("");
        etHolder.setText("");
        etPass.setText("");
        etOtp.setText("");

        selectedBankId = "";

        layoutOtp.setVisibility(View.GONE);
        btnSave.setText("SAVE BANK");

    }

    // 🔥 COMMON DIALOG
    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .show();
    }
}