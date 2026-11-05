package com.akaalpay.dmt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.utils.StatusBarUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class DmtDashboardActivity extends AppCompatActivity {

    private TextView tvName, tvLimit, tvMobileDisplay;
    private LinearLayout layoutBeneficiaries;
    private ImageView btnAddBeneficiary;

    private String mobile = "";
    private String referenceKey = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dmt_dashboard);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        try {

            String jsonData =
                    getIntent().getStringExtra("remitter_json");

            mobile =
                    getIntent().getStringExtra("mobile");

            findViewById(R.id.btnBack)
                    .setOnClickListener(v -> finish());

            if (jsonData != null) {

                JSONObject data = new JSONObject(jsonData);

                // MAIN RESPONSE REFERENCE KEY
                referenceKey =
                        data.optString("reference_key");

                displayData(data, mobile);

            }

        } catch (Exception e) {

            Toast.makeText(this,
                    "Error loading details",
                    Toast.LENGTH_SHORT).show();

            finish();
        }

        // ADD BENEFICIARY
        btnAddBeneficiary.setOnClickListener(v -> {

            Intent i = new Intent(
                    DmtDashboardActivity.this,
                    AddBeneficiaryActivity.class
            );

            i.putExtra("mobile", mobile);

            startActivity(i);

        });
    }

    // ================= INIT VIEWS =================

    private void initViews() {

        tvName = findViewById(R.id.tvName);

        tvLimit = findViewById(R.id.tvLimit);

        tvMobileDisplay =
                findViewById(R.id.tvMobileDisplay);

        layoutBeneficiaries =
                findViewById(R.id.layoutBeneficiaries);

        btnAddBeneficiary =
                findViewById(R.id.btnAddBeneficiary);

    }

    // ================= DISPLAY DATA =================

    private void displayData(JSONObject data,
                             String mobile) {

        try {

            tvName.setText(
                    data.optString("fname")
                            + " "
                            + data.optString("lname")
            );

            tvMobileDisplay.setText(
                    "+91 " + mobile
            );

            tvLimit.setText(
                    "Available Limit: ₹"
                            + data.optString("limit_available")
            );

            layoutBeneficiaries.removeAllViews();

            JSONArray list =
                    data.optJSONArray("beneficiaries");

            if (list != null && list.length() > 0) {

                for (int i = 0; i < list.length(); i++) {

                    JSONObject bene =
                            list.optJSONObject(i);

                    renderBeneCard(bene);

                }

            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // ================= BENEFICIARY CARD =================

    private void renderBeneCard(JSONObject bene) {

        try {

            View item = getLayoutInflater().inflate(
                    R.layout.item_beneficiary,
                    layoutBeneficiaries,
                    false
            );

            TextView tvBeneName =
                    item.findViewById(R.id.tvBeneName);

            TextView tvAccount =
                    item.findViewById(R.id.tvAccount);

            TextView tvBank =
                    item.findViewById(R.id.tvBank);

            TextView tvIfsc =
                    item.findViewById(R.id.tvIfsc);

            View btnTransfer =
                    item.findViewById(R.id.btnTransfer);

            // SET DATA

            tvBeneName.setText(
                    bene.optString("name")
            );

            tvAccount.setText(
                    "A/C: "
                            + bene.optString("account_no")
            );

            tvBank.setText(
                    bene.optString("bank_name")
            );

            tvIfsc.setText(
                    "IFSC: "
                            + bene.optString("ifsc")
            );

            // ================= TRANSFER BUTTON =================

            btnTransfer.setOnClickListener(v -> {

                try {

                    Intent i = new Intent(
                            DmtDashboardActivity.this,
                            DmtTransferActivity.class
                    );

                    // MOBILE
                    i.putExtra(
                            "mobile",
                            mobile
                    );

                    // BENEFICIARY DETAILS
                    i.putExtra(
                            "bene_id",
                            bene.optString("bene_id")
                    );

                    i.putExtra(
                            "bank_id",
                            bene.optString("bank_id")
                    );

                    i.putExtra(
                            "bene_name",
                            bene.optString("name")
                    );

                    i.putExtra(
                            "account",
                            bene.optString("account_no")
                    );

                    i.putExtra(
                            "ifsc",
                            bene.optString("ifsc")
                    );

                    i.putExtra(
                            "bank_id",
                            bene.optString("bank_name")
                    );

                    // MAIN RESPONSE REFERENCE KEY
                    i.putExtra(
                            "referenceKey",
                            referenceKey
                    );

                    startActivity(i);

                } catch (Exception e) {

                    Toast.makeText(
                            DmtDashboardActivity.this,
                            "Unknown Error Please Try After Sometime.",
                            Toast.LENGTH_SHORT
                    ).show();

                }

            });

            layoutBeneficiaries.addView(item);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }
}