package com.akaalpay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class LeadGenerationActivity extends AppCompatActivity {
    private String TOKEN = "";
    private int activationCharge = 999;
    private String selectedProduct = "";
    private String selectedProductLabel = "";

    private LinearLayout layoutActivation, layoutLeadForm;
    private TextView tvActivationCharge, tvProductTitle, tvProductSubtitle;
    private Button btnActivate;
    private TextView tvFormProductLabel;
    private TextInputEditText etName, etMobile, etEmail;
    private Button btnSubmitLead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lead_generation);

        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        selectedProduct = getIntent().getStringExtra("product_code");
        selectedProductLabel = getIntent().getStringExtra("product_label");

        if (selectedProduct == null) selectedProduct = "PL";
        if (selectedProductLabel == null) selectedProductLabel = "Personal Loan";

        initViews();

        tvProductTitle.setText(selectedProductLabel);
        tvProductSubtitle.setText(getProductSubtitle(selectedProduct));
        tvFormProductLabel.setText("Apply for: " + selectedProductLabel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnActivate.setOnClickListener(v -> showActivationConfirmDialog());
        btnSubmitLead.setOnClickListener(v -> {
            if (validateForm()) showSubmitConfirmDialog();
        });

        checkActivationStatus();
    }

    private void initViews() {
        layoutActivation = findViewById(R.id.layoutActivation);
        layoutLeadForm = findViewById(R.id.layoutLeadForm);
        tvActivationCharge = findViewById(R.id.tvActivationCharge);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvProductSubtitle = findViewById(R.id.tvProductSubtitle);
        btnActivate = findViewById(R.id.btnActivate);
        tvFormProductLabel = findViewById(R.id.tvFormProductLabel);
        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        btnSubmitLead = findViewById(R.id.btnSubmitLead);
    }

    private String getProductSubtitle(String code) {
        switch (code) {
            case "PL":
                return "Quick funds for personal needs";
            case "BL":
                return "Grow your business";
            case "IL":
                return "Loan against insurance policy";
            case "CC":
                return "Instant credit card";
            case "SA":
                return "Open savings account";
            default:
                return "Financial Lead Service";
        }
    }

    private void showActivationLayout() {
        layoutActivation.setVisibility(View.VISIBLE);
        layoutLeadForm.setVisibility(View.GONE);
    }

    private void showLeadForm() {
        layoutActivation.setVisibility(View.GONE);
        layoutLeadForm.setVisibility(View.VISIBLE);
    }

    // ─── CHECK ACTIVATION ───────────────────────────
    private void checkActivationStatus() {
        UiUtils.showLoading(this);

        ApiClient.get(this, "lead-generation/check-activation", TOKEN, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                if (response == null) {
                    UiUtils.showToast(this, "Server not responding", false);
                    return;
                }

                JSONObject data = response.optJSONObject("data");
                boolean activated = data != null && data.optBoolean("is_activated", false);
                activationCharge = data != null ? data.optInt("charge", 999) : 999;

                tvActivationCharge.setText("₹" + activationCharge);
                btnActivate.setText("🚀  Activate — ₹" + activationCharge);

                if (activated) showLeadForm();
                else showActivationLayout();
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, error, false);
        }));
    }

    // ─── ACTIVATE ───────────────────────────────────
    private void showActivationConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Activate Service")
                .setMessage(
                        "₹" + activationCharge + " will be deducted from your wallet.\n\n" +
                                "• One-time charge, non-refundable\n" +
                                "• Lifetime access to all 5 products\n" +
                                "• SA Account opening not available in all areas\n\n"
                )
                .setPositiveButton("Yes, Activate", (d, w) -> callActivateApi())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void callActivateApi() {
        UiUtils.showLoading(this);
        ApiClient.post(this, "lead-generation/activate", TOKEN, new JSONObject(), response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                if (response == null) {
                    UiUtils.showToast(this, "Server not responding", false);
                    return;
                }

                if (response.optInt("status") == 1) {
                    JSONObject data = response.optJSONObject("data");
                    String refId = data != null ? data.optString("ref_id", "") : "";
                    new AlertDialog.Builder(this)
                            .setTitle("Activated!")
                            .setMessage("Service activated successfully!\nRef ID: " + refId)
                            .setPositiveButton("Continue", (d, w) -> showLeadForm())
                            .setCancelable(false)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Activation Failed")
                            .setMessage(response.optString("message", "Something went wrong"))
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show();
        }));
    }

    // ─── LEAD FORM ──────────────────────────────────
    private boolean validateForm() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }
        if (mobile.length() != 10) {
            etMobile.setError("Enter valid 10-digit number");
            etMobile.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email address");
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    private void showSubmitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Submission")
                .setMessage(
                        "Product : " + selectedProductLabel + "\n" +
                                "Name    : " + etName.getText().toString().trim() + "\n" +
                                "Mobile  : " + etMobile.getText().toString().trim() + "\n" +
                                "Email   : " + etEmail.getText().toString().trim() + "\n\n" +
                                "Proceed with this submission?"
                )
                .setPositiveButton("Submit", (d, w) -> callGenerateLeadApi())
                .setNegativeButton("Edit", null)
                .show();
    }

    private void callGenerateLeadApi() {
        UiUtils.showLoading(this);
        JSONObject body = new JSONObject();
        try {
            body.put("name", etName.getText().toString().trim());
            body.put("mobile_no", etMobile.getText().toString().trim());
            body.put("email", etEmail.getText().toString().trim());
            body.put("product", selectedProduct);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(this, "lead-generation/generate", TOKEN, body, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                if (response == null) {
                    UiUtils.showToast(this, "Server not responding", false);
                    return;
                }

                if (response.optInt("status") == 1) {
                    JSONObject data = response.optJSONObject("data");
                    String url = data != null ? data.optString("url", "") : "";
                    String encdata = data != null ? data.optString("encdata", "") : "";

                    // Clear form
                    etName.setText("");
                    etMobile.setText("");
                    etEmail.setText("");

                    if (!url.isEmpty() && !encdata.isEmpty()) {
                        Intent intent = new Intent(this, LeadWebViewActivity.class);
                        intent.putExtra("url", url);
                        intent.putExtra("encdata", encdata);
                        startActivity(intent);
                    } else {
                        UiUtils.showToast(this, response.optString("message"), true);
                    }
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Submission Failed")
                            .setMessage(response.optString("message", "Something went wrong"))
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, error, false);
        }));
    }
}