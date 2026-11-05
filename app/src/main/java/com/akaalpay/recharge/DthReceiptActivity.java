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

public class DthReceiptActivity extends AppCompatActivity {

    private TextView txtAmount, txtStatus, txtDate;
    private TextView txtDthNo, txtOperator, txtReference, txtAck, txtMessage;
    private Button btnDone, btnCheckStatus;
    private ImageView btnBack;
    private RelativeLayout layoutStatus;

    private String TOKEN = "";
    private String id    = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dth_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        id = getIntent().getStringExtra("id");

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());
        btnCheckStatus.setOnClickListener(v -> loadReceipt());

        loadReceipt();
    }

    private void initViews() {
        txtAmount      = findViewById(R.id.txtAmount);
        txtStatus      = findViewById(R.id.txtStatus);
        txtDate        = findViewById(R.id.txtDate);
        txtDthNo       = findViewById(R.id.txtDthNo);
        txtOperator    = findViewById(R.id.txtOperator);
        txtReference   = findViewById(R.id.txtReference);
        txtAck         = findViewById(R.id.txtAck);
        txtMessage     = findViewById(R.id.txtMessage);
        btnDone        = findViewById(R.id.btnDone);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        btnBack        = findViewById(R.id.btnBack);
        layoutStatus   = findViewById(R.id.layoutStatus);
    }

    private void loadReceipt() {

        UiUtils.showLoading(this);

        JSONObject p = new JSONObject();
        try { p.put("id", id); } catch (Exception ignored) {}

        ApiClient.post(this, "dth/receipt", TOKEN, p, response -> runOnUiThread(() -> {

            UiUtils.dismissLoading();

            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                UiUtils.showToast(this, "No data found", false);
                return;
            }

            txtAmount.setText("₹" + data.optString("amount"));
            txtDthNo.setText("DTH No : " + data.optString("ca_number"));
            txtOperator.setText("Operator : " + data.optString("operator_name"));
            txtReference.setText("Reference ID : " + data.optString("reference_id"));
            txtAck.setText("ACK No : " + data.optString("ackno"));
            txtMessage.setText(data.optString("response_message"));
            txtDate.setText(data.optString("date"));

            String status = data.optString("status");

            switch (status) {
                case "1":
                    txtStatus.setText("Success ✓");
                    layoutStatus.setBackgroundColor(0xFF22C55E);
                    btnCheckStatus.setVisibility(Button.GONE);
                    break;
                case "0":
                    txtStatus.setText("Failed ✗");
                    layoutStatus.setBackgroundColor(0xFFEF4444);
                    btnCheckStatus.setVisibility(Button.GONE);
                    break;
                default:
                    txtStatus.setText("Pending ⏳");
                    layoutStatus.setBackgroundColor(0xFFFACC15);
                    btnCheckStatus.setVisibility(Button.VISIBLE);
                    break;
            }

        }), error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Failed to load receipt", false);
        }));
    }
}