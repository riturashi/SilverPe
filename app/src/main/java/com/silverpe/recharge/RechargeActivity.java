package com.silverpe.recharge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class RechargeActivity extends BaseActivity {

    private TextInputEditText etMobile, etAmount;
    private TextView tvCustomerView;
    private TextView spOperator, spCircle; // 🔥 changed
    private Button btnRecharge, btnViewPlan;
    private ImageView btnBack;

    private String TOKEN = "";
    private String selectedOperatorId = "";
    private String selectedCircle = "";
    private String selectedOperatorCode = "";
    private String selectedCircleCode = "";
    private final List<String> circleCodes = new ArrayList<>();

    private final List<String> operatorNames = new ArrayList<>();
    private final List<String> operatorCodes = new ArrayList<>();

    private final List<String> circleNames = new ArrayList<>();


    private final List<String> operatorIds = new ArrayList<>();

    private void resetForm() {
        etMobile.setText("");
        etAmount.setText("");

        spOperator.setText("Select Operator");
        spCircle.setText("Select Circle");

        selectedOperatorId = "";
        selectedCircle = "";

        tvCustomerView.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        btnBack.setOnClickListener(v -> finish());

        loadOperators();
        loadCircles();

        etMobile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                tvCustomerView.setText(s.toString());

                // reset
                selectedOperatorId = "";
                spOperator.setText("Select Operator");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 10) checkHLR(s.toString());
            }
        });

        // 🔥 Operator Bottom Sheet
        spOperator.setOnClickListener(v -> {
            SelectionBottomSheet.newInstance("Select Operator", operatorNames, (name, position) -> {
                spOperator.setText(name);
                selectedOperatorId = operatorIds.get(position);
            }).show(getSupportFragmentManager(), "OPERATOR");
        });

        // 🔥 Circle Bottom Sheet
        spCircle.setOnClickListener(v -> {
            SelectionBottomSheet.newInstance("Select Circle", circleNames, (name, position) -> {
                spCircle.setText(name);
                selectedCircle = circleCodes.get(position); // ✅ FIX
            }).show(getSupportFragmentManager(), "CIRCLE");
        });

        btnRecharge.setOnClickListener(v -> submitRecharge());
        btnViewPlan.setOnClickListener(v -> openPlanSheet());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etMobile = findViewById(R.id.etMobile);
        tvCustomerView = findViewById(R.id.tvCustomerView);
        spOperator = findViewById(R.id.spOperator);
        spCircle = findViewById(R.id.spCircle);
        etAmount = findViewById(R.id.etAmount);
        btnViewPlan = findViewById(R.id.btnViewPlan);
        btnRecharge = findViewById(R.id.btnRecharge);
    }

    private void loadOperators() {
        ApiClient.get(this, "recharge/operator-list", TOKEN, response -> {
            try {
                JSONArray arr = response.getJSONArray("data");

                operatorNames.clear();
                operatorIds.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    operatorNames.add(o.getString("operator_name"));
                    operatorIds.add(o.getString("operator_id"));
                }

            } catch (Exception ignored) {
            }
        }, error -> {
        });
    }

    private void loadCircles() {
        ApiClient.get(this, "recharge/circle-list", TOKEN, response -> {
            try {
                JSONArray arr = response.getJSONArray("data");

                circleNames.clear();
                circleCodes.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    circleNames.add(o.getString("circle_name"));   // UI
                    circleCodes.add(o.getString("circle_code"));   // API
                }

            } catch (Exception ignored) {
            }
        }, error -> {
        });
    }

    private void checkHLR(String mobile) {

        if (!isFinishing() && !isDestroyed()) {
            UiUtils.showLoading(this);
        }

        JSONObject p = new JSONObject();
        try {
            p.put("mobile", mobile);
        } catch (Exception ignored) {
        }

        ApiClient.post(this, "recharge/hlrcheck", TOKEN, p, response -> {
            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                try {
                    if (response.has("data") && !response.isNull("data")) {

                        JSONObject data = response.optJSONObject("data");

                        if (data != null) {

                            // 🔥 NEW VALUES
                            String operatorName = data.optString("operator_name", "");
                            String operatorCode = data.optString("operator_code", "");

                            String circleName = data.optString("circle_name", "");
                            String circleCode = data.optString("circle_code", "");

                            // 🔥 IMPORTANT: API के लिए code save
                            selectedOperatorId = operatorCode;
                            selectedCircle = circleCode;

                            // 🔥 UI में name दिखाओ
                            if (!operatorName.isEmpty()) {
                                spOperator.setText(operatorName);
                            }

                            if (!circleName.isEmpty()) {
                                spCircle.setText(circleName);
                            }

                        } else {
                            resetHLR();
                        }

                    } else {
                        resetHLR();
                    }

                } catch (Exception e) {
                    resetHLR();
                    UiUtils.showToast(this, "Parsing error", false);
                }

            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            resetHLR();
            UiUtils.showToast(this, "HLR Failed / Network Issue", false);

        }));
    }

    private void resetHLR() {
        selectedOperatorId = "";
        selectedCircle = "";

        spOperator.setText("Select Operator");
        spCircle.setText("Select Circle");
    }
    // 🔥 IMPORTANT CHANGE MARKED WITH ⭐

    private void submitRecharge() {

        String mobile = etMobile.getText().toString();
        String amount = etAmount.getText().toString();

        if (mobile.length() != 10) {
            UiUtils.showToast(this, "Enter 10 digit mobile", false);
            return;
        }

        if (amount.isEmpty()) {
            UiUtils.showToast(this, "Enter amount", false);
            return;
        }

        if (selectedOperatorId.isEmpty()) {
            UiUtils.showToast(this, "Select operator", false);
            return;
        }

        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try {
            p.put("operator_id", selectedOperatorId);
            p.put("mobile_no", mobile);
            p.put("circle", selectedCircle);
            p.put("amount", amount);
        } catch (Exception ignored) {
        }

        ApiClient.post(this, "recharge/do-recharge", TOKEN, p, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            try {
                int status = response.optInt("status", 0);
                String message = response.optString("message", "Something went wrong");

                // ✅ SUCCESS
                if (response.has("data") && !response.isNull("data")) {

                    JSONObject data = response.getJSONObject("data");
                    String id = data.optString("id", "");

                    if (!id.isEmpty()) {


                        resetForm(); //

                        Intent i = new Intent(RechargeActivity.this, RechargeReceiptActivity.class);
                        i.putExtra("id", id);
                        startActivity(i);

                    } else {
                        showErrorDialog("Transaction ID not found");
                    }

                } else {
                    // ❌ FAILED / ERROR
                    showErrorDialog(message);
                }

            } catch (Exception e) {
                showErrorDialog("Something went wrong");
            }

        }), error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            showErrorDialog("Recharge Failed");

        }));
    }

    private void showErrorDialog(String msg) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Response")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openPlanSheet() {
        if (spOperator.getText().toString().isEmpty()) {
            UiUtils.showToast(this, "Select operator first", false);
            return;
        }

        String mobile = etMobile.getText().toString().trim();

        if (mobile.length() != 10) {
            etMobile.setError("Enter valid mobile number");
            return;
        }

        PlanBottomSheet.newInstance(
                selectedOperatorId,
                selectedCircle,
                mobile,
                amount -> etAmount.setText(amount)
        ).show(getSupportFragmentManager(), "PLAN");
    }
}