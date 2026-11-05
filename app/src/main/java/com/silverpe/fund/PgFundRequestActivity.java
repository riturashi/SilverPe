package com.silverpe.fund;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class PgFundRequestActivity extends BaseActivity {

    EditText etPgAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pg_fund_request);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        etPgAmount = findViewById(R.id.etPgAmount);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnProceedPg).setOnClickListener(v -> validateAndInitiate());
    }

    private void validateAndInitiate() {

        String amountStr = etPgAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etPgAmount.setError("Enter Amount");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            etPgAmount.setError("Enter valid amount");
            return;
        }

        if (amount < 100 || amount > 100000) {
            etPgAmount.setError("Amount must be between ₹100 and ₹1,00,000");
            return;
        }

        initiatePg(amount);
    }

    private void initiatePg(int amount) {

        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");

        UiUtils.showLoading(this);

        JSONObject params = new JSONObject();
        try {
            params.put("amount", amount);
        } catch (JSONException e) {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Something went wrong", false);
            return;
        }

        ApiClient.post(
                this,
                "fund-request/pg-initiate",
                TOKEN,
                params,

                response -> runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    try {
                        int status = response.optInt("status");

                        if (status == 1) {

                            JSONObject data = response.getJSONObject("data");

                            Intent intent = new Intent(PgFundRequestActivity.this, PgWebViewActivity.class);
                            intent.putExtra("gateway_url", data.getString("gateway_url"));
                            intent.putExtra("enc_data", data.getString("enc_data"));
                            intent.putExtra("client_code", data.getString("client_code"));
                            intent.putExtra("client_txn_id", data.getString("client_txn_id"));
                            startActivity(intent);
                            finish();

                        } else {
                            UiUtils.showToast(PgFundRequestActivity.this, response.optString("message"), false);
                        }

                    } catch (Exception e) {
                        UiUtils.showToast(PgFundRequestActivity.this, "Response Error: " + e.getMessage(), false);
                    }
                }),

                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    UiUtils.showToast(PgFundRequestActivity.this, "Request Failed: " + error, false);
                })
        );
    }
}