package com.akaalpay.recharge;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

public class RechargeReceiptActivity extends AppCompatActivity {

    private TextView txtAmount, txtStatus, txtDate;
    private TextView txtMobile, txtOperator, txtCircle;
    private TextView txtReference, txtAck, txtMessage;
    private Button btnDone, btnCheckStatus;
    private ImageView btnBack;
    private RelativeLayout layoutStatus;

    private String TOKEN = "";
    private String id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        // 🔥 TOKEN load
        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        // 🔥 get id
        id = getIntent().getStringExtra("id");

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());

        btnCheckStatus.setOnClickListener(v -> loadReceipt());

        loadReceipt();
    }

    private void initViews() {
        txtAmount = findViewById(R.id.txtAmount);
        txtStatus = findViewById(R.id.txtStatus);
        txtDate = findViewById(R.id.txtDate);

        txtMobile = findViewById(R.id.txtMobile);
        txtOperator = findViewById(R.id.txtOperator);
        txtCircle = findViewById(R.id.txtCircle);

        txtReference = findViewById(R.id.txtReference);
        txtAck = findViewById(R.id.txtAck);
        txtMessage = findViewById(R.id.txtMessage);

        btnDone = findViewById(R.id.btnDone);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        btnBack = findViewById(R.id.btnBack);

        layoutStatus = findViewById(R.id.layoutStatus);
    }

    private void loadReceipt() {

        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try {
            p.put("id", id);
        } catch (Exception ignored) {}

        ApiClient.post(this, "recharge/receipt", TOKEN, p, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            JSONObject data = response.optJSONObject("data");

            if (data == null) {
                UiUtils.showToast(this, "No data found", false);
                return;
            }

            // 🔥 SET DATA
            txtAmount.setText("₹" + data.optString("amount"));
            txtMobile.setText("Mobile : " + data.optString("ca_number"));
            txtOperator.setText("Operator : " + data.optString("operator_name"));
            txtCircle.setText("Circle : " + data.optString("mobile_circle"));
            txtReference.setText("Reference ID : " + data.optString("reference_id"));
            txtAck.setText("ACK No : " + data.optString("ackno"));
            txtMessage.setText(data.optString("response_message"));
            txtDate.setText(data.optString("date"));

            // 🔥 STATUS + COLOR
            String status = data.optString("status");

            if (status.equals("1")) {

                txtStatus.setText("Success");
                txtStatus.setTextColor(0xFFFFFFFF);
                btnCheckStatus.setVisibility(Button.GONE);

                // ✅ GREEN
                layoutStatus.setBackgroundColor(0xFF22C55E);

            } else if (status.equals("0")) {

                txtStatus.setText("Failed");
                txtStatus.setTextColor(0xFFFFFFFF);
                btnCheckStatus.setVisibility(Button.GONE);

                // ❌ RED
                layoutStatus.setBackgroundColor(0xFFEF4444);

            } else {

                txtStatus.setText("Pending");
                txtStatus.setTextColor(0xFFFFFFFF);
                btnCheckStatus.setVisibility(Button.VISIBLE);

                // ⏳ YELLOW
                layoutStatus.setBackgroundColor(0xFFFACC15);
            }

        }), error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Failed to load receipt", false);

        }));
    }
}