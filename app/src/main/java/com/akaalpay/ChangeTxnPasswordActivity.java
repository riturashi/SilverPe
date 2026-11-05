package com.akaalpay;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

public class ChangeTxnPasswordActivity extends AppCompatActivity {

    EditText etOtp, etTxnPassword, etConfirmTxn;
    Button btnSubmit;

    String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_txn_password);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        etOtp = findViewById(R.id.etOtp);
        etTxnPassword = findViewById(R.id.etTxnPassword);
        etConfirmTxn = findViewById(R.id.etConfirmTxn);
        btnSubmit = findViewById(R.id.btnSubmit);

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 🔥 AUTO OTP CALL
        sendOtp();

        btnSubmit.setOnClickListener(v -> changeTxnPassword());
    }


    private void sendOtp() {

        if (TOKEN.isEmpty()) {
            showDialog("Session Expired, Login Again");
            return;
        }

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {
            body.put("type", "transaction_password");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(this, "user/password-change-otp", TOKEN, body, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                if (response != null && response.optInt("status") == 1) {

                    Toast.makeText(this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();

                } else {
                    showDialog(response != null ? response.optString("message") : "Error");
                }

            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            showDialog("Server Error");

        }));
    }

    // 🔥 CHANGE TXN PASSWORD
    private void changeTxnPassword() {

        String otp = etOtp.getText().toString().trim();
        String txnPass = etTxnPassword.getText().toString().trim();
        String confirm = etConfirmTxn.getText().toString().trim();

        if (otp.length() != 6) {
            showDialog("Enter valid 6 digit OTP");
            return;
        }

        if (!txnPass.matches("\\d{6}")) {
            showDialog("Txn Password must be 6 digit numeric");
            return;
        }

        if (!txnPass.equals(confirm)) {
            showDialog("Txn Password does not match");
            return;
        }

        if (TOKEN.isEmpty()) {
            showDialog("Session Expired, Login Again");
            return;
        }

        UiUtils.showLoading(this);

        JSONObject body = new JSONObject();

        try {
            body.put("otp", otp);
            body.put("txn_password", txnPass);
            body.put("txn_confirm_cpassword", confirm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(this, "user/change-txn-password", TOKEN, body, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                if (response != null && response.optInt("status") == 1) {

                    showSuccessDialog(response.optString("message"));

                } else {
                    showDialog(response != null ? response.optString("message") : "Error");
                }

            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            showDialog("Server Error");

        }));
    }

    private void showDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSuccessDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }
}