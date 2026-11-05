package com.akaalpay.dmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.report.ExpressDmtReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

/**
 * ============================================================================
 *  EXPRESS DMT — TRANSFER SCREEN
 * ============================================================================
 *  Unlike DmtTransferActivity (which arrives with a pre-selected, already
 *  registered beneficiary and needs an InstantPay OTP step for amounts
 *  above the free-flow threshold), Express DMT has no remitter/beneficiary
 *  registration and no OTP: the retailer types Account No + IFSC, hits
 *  "Verify" to fetch the real account holder name (a paid lookup, same
 *  charge model as DMT's own Account Verify), then transfers directly via
 *  RechargeKit in one shot.
 *
 *  Endpoints used (see api/v1/ExpressDmtController.php):
 *    POST express-dmt/verify-account   -> { account_number, ifsc_code, bene_name }
 *    POST express-dmt/transaction      -> { mobile, bene_name, account, ifsc,
 *                                           amount, txntype, transaction_password }
 * ============================================================================
 */
public class ExpressDmtTransferActivity extends AppCompatActivity {

    private TextInputEditText etMobile, etAccount, etIfsc, etBeneName, etAmount, etTxnPassword;

    private Button btnVerify, btnTransfer;

    private TextView tvVerifyResult;

    private String TOKEN = "";

    private boolean isAccountVerified = false;

    private String selectedTxnType = "IMPS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_express_dmt_transfer);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();
        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Any edit to Account No / IFSC invalidates a previous verification —
        // forces re-verify so a transfer can never go out against a stale name.
        TextWatcher resetOnEdit = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                resetVerification();
            }
        };
        etAccount.addTextChangedListener(resetOnEdit);
        etIfsc.addTextChangedListener(resetOnEdit);

        btnVerify.setOnClickListener(v -> verifyAccount());

        btnTransfer.setOnClickListener(v -> submitTransaction());
    }

    private void initViews() {

        etMobile       = findViewById(R.id.etMobile);
        etAccount      = findViewById(R.id.etAccount);
        etIfsc         = findViewById(R.id.etIfsc);
        etBeneName     = findViewById(R.id.etBeneName);
        etAmount       = findViewById(R.id.etAmount);
        etTxnPassword  = findViewById(R.id.etTxnPassword);

        btnVerify      = findViewById(R.id.btnVerify);
        btnTransfer    = findViewById(R.id.btnTransfer);

        tvVerifyResult = findViewById(R.id.tvVerifyResult);
    }

    private void resetVerification() {
        isAccountVerified = false;
        tvVerifyResult.setText("");
        tvVerifyResult.setVisibility(android.view.View.GONE);
    }

    // ================= VERIFY ACCOUNT =================

    private void verifyAccount() {

        String accountNo = etAccount.getText().toString().trim();
        String ifsc       = etIfsc.getText().toString().trim().toUpperCase();

        if (accountNo.isEmpty() || ifsc.isEmpty()) {
            showDialogBox("Warning", "Please enter Account Number and IFSC Code first.");
            return;
        }

        UiUtils.showLoading(this);
        btnVerify.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("account_number", accountNo);
            body.put("ifsc_code", ifsc);
            body.put("bene_name", etBeneName.getText().toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(
                this,
                "express-dmt/verify-account",
                TOKEN,
                body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();
                    btnVerify.setEnabled(true);

                    try {

                        if (response == null) {
                            showDialogBox("Error", "Server response failed.");
                            return;
                        }

                        int status = response.optInt("status", 0);
                        String message = response.optString("message");

                        if (status == 1) {

                            JSONObject data = response.optJSONObject("data");

                            if (data != null) {

                                String holderName = data.optString("account_holder", "");
                                String bankName    = data.optString("bank_name", "");
                                String branchName  = data.optString("branch_name", "");

                                etBeneName.setText(holderName);
                                isAccountVerified = true;

                                String resultText = "Verified: " + holderName;
                                if (!bankName.isEmpty()) {
                                    resultText += "\n" + bankName + (branchName.isEmpty() ? "" : " - " + branchName);
                                }

                                tvVerifyResult.setText(resultText);
                                tvVerifyResult.setVisibility(android.view.View.VISIBLE);

                            }

                        } else {

                            resetVerification();
                            showDialogBox("Verification Failed", message);

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showDialogBox("Error", "Response parsing failed.");
                    }

                }),

                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    btnVerify.setEnabled(true);
                    showDialogBox("Error", error);
                })
        );
    }

    // ================= SUBMIT TRANSACTION =================

    private void submitTransaction() {

        try {

            String mobile      = etMobile.getText().toString().trim();
            String beneName    = etBeneName.getText().toString().trim();
            String account     = etAccount.getText().toString().trim();
            String ifsc         = etIfsc.getText().toString().trim().toUpperCase();
            String amount       = etAmount.getText().toString().trim();
            String txnPassword = etTxnPassword.getText().toString().trim();

            if (mobile.length() != 10) {
                showDialogBox("Warning", "Please enter a valid 10 digit mobile number.");
                return;
            }

            if (beneName.isEmpty()) {
                showDialogBox("Warning", "Beneficiary name is required.");
                return;
            }

            if (amount.isEmpty()) {
                showDialogBox("Warning", "Please enter amount.");
                return;
            }

            if (txnPassword.isEmpty()) {
                showDialogBox("Warning", "Please enter transaction password.");
                return;
            }

            // Final confirmation before hitting the transfer API — mirrors
            // the web flow's SweetAlert confirmation, same purpose: retailer
            // sees exactly what's about to go out before it's irreversible.
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Transfer")
                    .setMessage(
                            "Beneficiary: " + beneName + "\n" +
                                    "Account No: " + account + "\n" +
                                    "IFSC: " + ifsc + "\n" +
                                    "Mode: " + selectedTxnType + "\n" +
                                    "Amount: \u20B9" + amount
                    )
                    .setPositiveButton("Yes, Transfer Now", (dialog, which) -> doTransfer(
                            mobile, beneName, account, ifsc, amount, txnPassword
                    ))
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            showDialogBox("Error", "Something went wrong.");
        }
    }

    private void doTransfer(String mobile, String beneName, String account, String ifsc,
                            String amount, String txnPassword) {

        UiUtils.showLoading(this);
        btnTransfer.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("mobile", mobile);
            body.put("bene_name", beneName);
            body.put("account", account);
            body.put("ifsc", ifsc);
            body.put("amount", amount);
            body.put("txntype", selectedTxnType);
            body.put("transaction_password", txnPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(
                this,
                "express-dmt/transaction",
                TOKEN,
                body,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();
                    btnTransfer.setEnabled(true);

                    try {

                        if (response == null) {
                            showDialogBox("Error", "Server response failed.");
                            return;
                        }

                        int status = response.optInt("status", 0);
                        String message = response.optString("message");

                        if (status == 1) {

                            JSONObject data = response.optJSONObject("data");
                            String id = data != null ? data.optString("id", "") : "";

                            if (!id.isEmpty()) {

                                // Dedicated Express DMT receipt screen — its own
                                // API endpoint (express-dmt/receipt) and layout,
                                // since the field set differs slightly (no
                                // bank_name; IFSC shown instead).
                                Intent i = new Intent(ExpressDmtTransferActivity.this, ExpressDmtReceiptActivity.class);
                                i.putExtra("id", id);
                                startActivity(i);
                                finish();

                            } else {
                                showDialogBox("Success", message);
                            }

                        } else {
                            showDialogBox("Failed", message);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showDialogBox("Error", "Response parsing failed.");
                    }

                }),

                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    btnTransfer.setEnabled(true);
                    showDialogBox("Error", error);
                })
        );
    }

    // ================= DIALOG =================

    private void showDialogBox(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}