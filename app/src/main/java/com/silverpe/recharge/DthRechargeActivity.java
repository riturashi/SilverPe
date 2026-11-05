package com.silverpe.recharge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DthRechargeActivity extends BaseActivity {

    private TextInputEditText etDth, etAmount;
    private TextView spOperator;
    private TextView tvCustomerName, tvDthDetails;
    private MaterialButton btnRecharge, btnFetch;
    private MaterialButton btnTypeMobile, btnTypeId;
    private ImageView btnBack;

    private String TOKEN = "";
    private String selectedOperatorId = "";
    private String selectedType = "MOBILE"; // Default MOBILE

    private final List<String> operatorNames = new ArrayList<>();
    private final List<String> operatorIds   = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dth_recharge);

        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        btnBack.setOnClickListener(v -> finish());

        // Default type = MOBILE selected
        setTypeSelected("MOBILE");

        loadOperators();

        // Type Toggle
        btnTypeMobile.setOnClickListener(v -> setTypeSelected("MOBILE"));
        btnTypeId.setOnClickListener(v -> setTypeSelected("ID"));

        // Operator Select
        spOperator.setOnClickListener(v -> {
            if (operatorNames.isEmpty()) {
                UiUtils.showToast(this, "Loading operators...", false);
                return;
            }
            SelectionBottomSheet.newInstance("Select Operator", operatorNames, (name, position) -> {
                spOperator.setText(name);
                selectedOperatorId = operatorIds.get(position);
            }).show(getSupportFragmentManager(), "OPERATOR");
        });

        btnFetch.setOnClickListener(v -> fetchDthInfo());
        btnRecharge.setOnClickListener(v -> submitRecharge());
    }

    private void initViews() {
        btnBack        = findViewById(R.id.btnBack);
        etDth          = findViewById(R.id.etDth);
        etAmount       = findViewById(R.id.etAmount);
        spOperator     = findViewById(R.id.spOperator);
        btnRecharge    = findViewById(R.id.btnRecharge);
        btnFetch       = findViewById(R.id.btnFetch);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvDthDetails   = findViewById(R.id.tvDthDetails);
        btnTypeMobile  = findViewById(R.id.btnTypeMobile);
        btnTypeId      = findViewById(R.id.btnTypeId);
    }

    // Type button UI toggle
    private void setTypeSelected(String type) {
        selectedType = type;

        if (type.equals("MOBILE")) {
            btnTypeMobile.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF301BD3));
            btnTypeId.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFBBBBBB));
            etDth.setHint("");
        } else {
            btnTypeId.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF301BD3));
            btnTypeMobile.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFBBBBBB));
            etDth.setHint("");
        }

        // Reset info on type change
        tvCustomerName.setText("");
        tvDthDetails.setVisibility(TextView.GONE);
        tvDthDetails.setText("");
        etDth.setText("");
    }

    private void loadOperators() {
        ApiClient.get(this, "dth/operator-list", TOKEN, response -> {
            try {
                JSONArray arr = response.getJSONArray("data");

                operatorNames.clear();
                operatorIds.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    operatorNames.add(o.getString("operator_name"));
                    operatorIds.add(o.getString("operator_id")); // AIRTELTV_B2B etc
                }

            } catch (Exception ignored) {}
        }, error -> {});
    }

    private void fetchDthInfo() {

        String dthNo = etDth.getText() != null ? etDth.getText().toString().trim() : "";

        if (dthNo.isEmpty()) {
            UiUtils.showToast(this, "Enter DTH Number / Mobile", false);
            return;
        }

        if (selectedOperatorId.isEmpty()) {
            UiUtils.showToast(this, "Select Operator", false);
            return;
        }

        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try {
            p.put("dth_no",   dthNo);
            p.put("operator", selectedOperatorId); // Noble code e.g. AIRTELTV_B2B
            p.put("type",     selectedType);       // MOBILE or ID
        } catch (Exception ignored) {}

        ApiClient.post(this, "dth/dth-info", TOKEN, p, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            try {
                if (response.optInt("status") == 1) {

                    JSONObject data = response.getJSONObject("data");

                    String name          = data.optString("name", "");
                    String balance       = data.optString("balance", "");
                    String plan          = data.optString("plan", "");
                    String billAmount    = data.optString("bill_amount", "");
                    String nextDate      = data.optString("NextRechargeDate", "");
                    String accountStatus = data.optString("status", "");

                    // Customer name
                    tvCustomerName.setText(name.isEmpty() ? "—" : name);

                    // Details
                    String details =
                            "Plan     : " + (plan.isEmpty() ? "—" : plan) + "\n" +
                                    "Monthly  : ₹" + (billAmount.isEmpty() ? "—" : billAmount) + "\n" +
                                    "Balance  : ₹" + (balance.isEmpty() ? "—" : balance) + "\n" +
                                    "Next Date: " + (nextDate.isEmpty() ? "—" : nextDate) + "\n" +
                                    "Status   : " + (accountStatus.isEmpty() ? "—" : accountStatus);

                    tvDthDetails.setText(details);
                    tvDthDetails.setVisibility(TextView.VISIBLE);

                    // Auto fill amount
                    if (!billAmount.isEmpty()) {
                        etAmount.setText(billAmount);
                    }

                } else {
                    showErrorDialog(response.optString("message", "Failed to fetch DTH info"));
                }

            } catch (Exception e) {
                showErrorDialog("Invalid response");
            }

        }), error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showErrorDialog("Fetch Failed. Check network.");
        }));
    }

    private void submitRecharge() {

        String dth    = etDth.getText() != null ? etDth.getText().toString().trim() : "";
        String amount = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

        if (dth.isEmpty()) {
            UiUtils.showToast(this, "Enter DTH Number / Mobile", false);
            return;
        }

        if (amount.isEmpty()) {
            UiUtils.showToast(this, "Enter Amount", false);
            return;
        }

        if (selectedOperatorId.isEmpty()) {
            UiUtils.showToast(this, "Select Operator", false);
            return;
        }

        // ✅ Direct recharge — no confirmation dialog
        doRecharge(dth, amount);
    }

    private void doRecharge(String dth, String amount) {

        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try {
            p.put("dth_no",      dth);
            p.put("operator_id", selectedOperatorId); // AIRTELTV_B2B etc
            p.put("amount",      amount);
        } catch (Exception ignored) {}

        ApiClient.post(this, "dth/do-recharge", TOKEN, p, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            try {
                if (response.optInt("status") == 1) {

                    JSONObject data = response.optJSONObject("data");
                    String id = data != null ? data.optString("id", "") : "";

                    if (!id.isEmpty()) {
                        // Reset form
                        resetForm();

                        // Go to receipt
                        Intent i = new Intent(this, DthReceiptActivity.class);
                        i.putExtra("id", id);
                        startActivity(i);

                    } else {
                        showErrorDialog("Transaction ID not found");
                    }

                } else {
                    showErrorDialog(response.optString("message", "Recharge Failed"));
                }

            } catch (Exception e) {
                showErrorDialog("Something went wrong");
            }

        }), error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showErrorDialog("Recharge Failed. Check network.");
        }));
    }

    private void resetForm() {
        etDth.setText("");
        etAmount.setText("");
        spOperator.setText("");
        selectedOperatorId = "";
        tvCustomerName.setText("");
        tvDthDetails.setText("");
        tvDthDetails.setVisibility(TextView.GONE);
        setTypeSelected("MOBILE");
    }

    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Response")
                .setMessage(msg)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }
}