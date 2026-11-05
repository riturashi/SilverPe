package com.akaalpay.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import androidx.print.PrintHelper;

import org.json.JSONObject;

public class UpiAtmReceiptActivity extends BaseActivity {

    TextView txtHeaderTitle, txtStatus, txtDate, txtAmount;
    TextView txtMobile, txtReference, txtIpayId, txtQrStatus;
    TextView txtPayerName, txtPayerVpa, txtResponseMessage, txtShopName;
    RelativeLayout layoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uatm_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        String id = getIntent().getStringExtra("id");
        loadReceipt(id);
    }

    private void initViews() {
        layoutStatus        = findViewById(R.id.layoutStatus);
        txtHeaderTitle      = findViewById(R.id.txtHeaderTitle);
        txtStatus           = findViewById(R.id.txtStatus);
        txtDate             = findViewById(R.id.txtDate);
        txtAmount           = findViewById(R.id.txtAmount);
        txtMobile           = findViewById(R.id.txtMobile);
        txtReference        = findViewById(R.id.txtReference);
        txtIpayId           = findViewById(R.id.txtIpayId);
        txtQrStatus         = findViewById(R.id.txtQrStatus);
        txtPayerName        = findViewById(R.id.txtPayerName);
        txtPayerVpa         = findViewById(R.id.txtPayerVpa);
        txtResponseMessage  = findViewById(R.id.txtResponseMessage);
        txtShopName         = findViewById(R.id.txtShopName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> generatePrintReceipt());
    }

    private void loadReceipt(String id) {
        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE)
                .getString("TOKEN", "");
        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("id", id);

            ApiClient.post(this, "ip-uatm/receipt", TOKEN, body, response -> {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        if (response.optInt("status") == 0) return;

                        JSONObject data = response.getJSONObject("data");

                        // Amount
                        txtAmount.setText("₹" + data.optString("amount", "0.00"));

                        // Date
                        txtDate.setText(data.optString("date", "--"));

                        // Status color + message
                        int txnStatus   = data.optInt("status");
                        String qrStatus = data.optString("qr_status", "");

                        if (txnStatus == 1) {
                            layoutStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                            txtStatus.setText("Transaction Successful");
                        } else if (qrStatus.equalsIgnoreCase("PENDING") || qrStatus.equalsIgnoreCase("INITIATED")) {
                            layoutStatus.setBackgroundColor(Color.parseColor("#F9A825"));
                            txtStatus.setText("Transaction Pending");
                        } else {
                            layoutStatus.setBackgroundColor(Color.parseColor("#C62828"));
                            txtStatus.setText("Transaction Failed");
                        }

                        // Fields
                        txtMobile.setText("Mobile : "       + data.optString("mobile_no", "--"));
                        txtReference.setText("Ref No : "    + data.optString("external_ref", "--"));
                        txtIpayId.setText("IPay ID : "      + data.optString("ipay_id", "--"));
                        txtQrStatus.setText("QR Status : "  + qrStatus);

                        String payerName = data.optString("payer_name", "");
                        String payerVpa  = data.optString("payer_vpa", "");
                        txtPayerName.setText("Payer Name : " + (payerName.isEmpty() || payerName.equals("null") ? "--" : payerName));
                        txtPayerVpa.setText("Payer VPA : "   + (payerVpa.isEmpty()  || payerVpa.equals("null")  ? "--" : payerVpa));

                        txtResponseMessage.setText("Message : " + data.optString("response_message", "--"));


                        if (data.has("user_details")) {
                            txtShopName.setText(
                                    data.getJSONObject("user_details").optString("firm_name", "Bharat Pay Shop")
                            );
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, error -> runOnUiThread(UiUtils::dismissLoading));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePrintReceipt() {
        try {
            findViewById(R.id.btnShare).setVisibility(View.GONE);
            findViewById(R.id.btnDone).setVisibility(View.GONE);
            findViewById(R.id.btnBack).setVisibility(View.GONE);

            View view = getWindow().getDecorView().findViewById(android.R.id.content);
            view.setDrawingCacheEnabled(true);
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                    view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            findViewById(R.id.btnShare).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
            findViewById(R.id.btnBack).setVisibility(View.VISIBLE);

            PrintHelper photoPrinter = new PrintHelper(this);
            photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            photoPrinter.printBitmap("UATM_Receipt", bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}