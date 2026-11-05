package com.akaalpay;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

import java.util.HashMap;

public class WalletTransferActivity extends AppCompatActivity {

    // 🔹 Views
    EditText etUserId, etAmount, etPassword, etRemark;
    TextView tvName, tvFirm;
    LinearLayout layoutUser, layoutForm;
    Button btnSearch, btnTransfer;
    ImageView btnBack;

    // 🔹 Token
    String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_transfer);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        try {

            // ✅ Initialize Views
            etUserId = findViewById(R.id.etUserId);
            etAmount = findViewById(R.id.etAmount);
            etPassword = findViewById(R.id.etPassword);
            etRemark = findViewById(R.id.etRemark);

            tvName = findViewById(R.id.tvName);
            tvFirm = findViewById(R.id.tvFirm);

            layoutUser = findViewById(R.id.layoutUser);
            layoutForm = findViewById(R.id.layoutForm);

            btnSearch = findViewById(R.id.btnSearch);
            btnTransfer = findViewById(R.id.btnTransfer);
            btnBack = findViewById(R.id.btnBack);

            // ✅ Back button
            btnBack.setOnClickListener(v -> finish());

            // ✅ Load token
            loadToken();

            // ✅ Click events
            btnSearch.setOnClickListener(v -> searchUser());
            btnTransfer.setOnClickListener(v -> validateAndTransfer());

        } catch (Exception e) {
            Log.e("INIT_ERROR", e.getMessage());
        }
    }

    // ✅ Load TOKEN
    private void loadToken() {
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE)
                .getString("TOKEN", "");
    }

    // ✅ STEP 1: SEARCH USER
    private void searchUser() {

        String userId = etUserId.getText().toString().trim();

        if (userId.isEmpty()) {
            toast("Enter User ID");
            return;
        }

        UiUtils.showLoading(this);

        HashMap<String, String> map = new HashMap<>();
        map.put("user_id", userId);

        ApiClient.post(this, "fund-transfer/search-user", TOKEN,
                new JSONObject(map),
                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    try {

                        if (response.getInt("status") == 1) {

                            JSONObject data = response.getJSONObject("data");

                            tvName.setText(data.getString("name"));
                            tvFirm.setText(data.getString("firm_name"));

                            // ✅ Show UI
                            layoutUser.setVisibility(View.VISIBLE);
                            layoutForm.setVisibility(View.VISIBLE);

                            // ✅ Lock User ID
                            etUserId.setEnabled(false);

                            // ✅ Hide Search Button
                            btnSearch.setVisibility(View.GONE);

                        } else {

                            // ❌ Fail
                            layoutUser.setVisibility(View.GONE);
                            layoutForm.setVisibility(View.GONE);

                            btnSearch.setVisibility(View.VISIBLE);

                            dialog("Failed", response.getString("message"));
                        }

                    } catch (Exception e) {
                        Log.e("SEARCH_ERROR", e.getMessage());
                    }

                }),
                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    btnSearch.setVisibility(View.VISIBLE);
                    dialog("Error", "Server error");
                })
        );
    }

    // ✅ STEP 2: VALIDATE
    private void validateAndTransfer() {

        String amount = etAmount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (amount.isEmpty()) {
            toast("Enter Amount");
            return;
        }

        if (password.isEmpty()) {
            toast("Enter Transaction Password");
            return;
        }

        callTransferApi();
    }

    // ✅ STEP 3: TRANSFER API
    private void callTransferApi() {

        UiUtils.showLoading(this);

        HashMap<String, String> map = new HashMap<>();
        map.put("user_id", etUserId.getText().toString().trim());
        map.put("amount", etAmount.getText().toString().trim());
        map.put("transaction_password", etPassword.getText().toString().trim());
        map.put("remark", etRemark.getText().toString().trim());

        ApiClient.post(this, "fund-transfer/transfer", TOKEN,
                new JSONObject(map),
                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    try {

                        if (response.getInt("status") == 1) {

                            String message = response.getString("message");

                            dialog("Success", message);

                            // ✅ Reset fields
                            etAmount.setText("");
                            etPassword.setText("");
                            etRemark.setText("");

                        } else {
                            dialog("Failed", response.getString("message"));
                        }

                    } catch (Exception e) {
                        Log.e("TRANSFER_ERROR", e.getMessage());
                    }

                }),
                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    dialog("Error", "Server error");
                })
        );
    }

    // ✅ Toast
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ✅ Dialog
    private void dialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}