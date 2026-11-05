package com.silverpe.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.print.PrintHelper;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class BbpsReceiptActivity extends BaseActivity {

    TextView txtHeaderTitle, txtStatus, txtDate, txtAmount, txtAmountInWords;
    TextView txtReference, txtBank, txtRRN, txtAck, txtAadhar, txtMobile;
    TextView txtShopName;

    RelativeLayout layoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbps_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        String id = getIntent().getStringExtra("id");
        loadReceipt(id);
    }

    private void initViews() {

        layoutStatus = findViewById(R.id.layoutStatus);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        txtStatus = findViewById(R.id.txtStatus);
        txtDate = findViewById(R.id.txtDate);
        txtAmount = findViewById(R.id.txtAmount);
        txtAmountInWords = findViewById(R.id.txtAmountInWords);

        txtReference = findViewById(R.id.txtReference);
        txtBank = findViewById(R.id.txtBank);
        txtRRN = findViewById(R.id.txtRRN);
        txtAck = findViewById(R.id.txtAck);
        txtAadhar = findViewById(R.id.txtAadhar);
        txtMobile = findViewById(R.id.txtMobile);

        txtShopName = findViewById(R.id.txtShopName);

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

            // ✅ BBPS API
            ApiClient.post(this, "bbps/receipt", TOKEN, body, response -> {

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    try {

                        if (response.optInt("status") == 0) return;

                        JSONObject data = response.getJSONObject("data");

                        // 🔥 Header
                        txtHeaderTitle.setText(
                                data.optString("service_name", "BBPS Receipt")
                        );

                        // 🔥 Status
                        txtStatus.setText(data.optString("response_message"));

                        // 🔥 Amount
                        String amt = data.optString("amount");
                        txtAmount.setText("₹" + amt);

                        txtAmountInWords.setVisibility(View.VISIBLE);
                        txtAmountInWords.setText(
                                convertToWords(amt) + " RUPEES ONLY"
                        );

                        // 🔥 Date
                        txtDate.setText(data.optString("date"));

                        // 🔥 Biller Name
                        txtBank.setText("Biller : " + data.optString("biller_name"));

                        // 🔥 Reference
                        txtReference.setText("Ref No : " + data.optString("reference_id"));

                        // 🔥 RRN (reuse)
                        txtRRN.setText("Reference ID : " + data.optString("reference_id"));

                        // 🔥 ACK
                        String ack = data.optString("ackno");
                        txtAck.setText("ACK : " + (ack == null || ack.isEmpty() ? "-" : ack));

                        // 🔥 Customer Number
                        txtAadhar.setText("Customer No : " + data.optString("ca_number"));

                        // 🔥 Category
                        txtMobile.setText("Category : " + data.optString("category_name"));

                        // 🔥 Shop Name
                        if (data.has("user_details")) {
                            txtShopName.setText(
                                    data.getJSONObject("user_details")
                                            .optString("firm_name", "Bharat Pay Shop")
                            );
                        }

                        // 🔥 Status Color
                        int txnStatus = Integer.parseInt(data.optString("status", "0"));

                        if (txnStatus == 1) {

                            // ✅ SUCCESS
                            layoutStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                            txtStatus.setText("Success");

                        } else if (txnStatus == 2) {

                            // 🟡 PENDING
                            layoutStatus.setBackgroundColor(Color.parseColor("#F9A825"));
                            txtStatus.setText("Pending");

                        } else {

                            // ❌ FAILED
                            layoutStatus.setBackgroundColor(Color.parseColor("#C62828"));
                            txtStatus.setText("Failed");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });

            }, error -> runOnUiThread(() -> UiUtils.dismissLoading()));

        } catch (Exception e) {
            e.printStackTrace();
            UiUtils.dismissLoading();
        }
    }

    // 🔥 Amount in Words
    private String convertToWords(String amountStr) {
        try {
            long n = (long) Double.parseDouble(amountStr);

            if (n == 0) return "ZERO";

            String[] units = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE",
                    "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN",
                    "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN",
                    "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};

            String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY",
                    "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};

            return getWords(n, units, tens).trim();

        } catch (Exception e) {
            return "";
        }
    }

    private String getWords(long n, String[] units, String[] tens) {

        if (n < 20) return units[(int) n];

        if (n < 100)
            return tens[(int) (n / 10)] + " " + units[(int) (n % 10)];

        if (n < 1000)
            return units[(int) (n / 100)] + " HUNDRED " +
                    getWords(n % 100, units, tens);

        if (n < 100000)
            return getWords(n / 1000, units, tens) + " THOUSAND " +
                    getWords(n % 1000, units, tens);

        return getWords(n / 100000, units, tens) + " LAKH " +
                getWords(n % 100000, units, tens);
    }

    // 🔥 Print / Share
    private void generatePrintReceipt() {

        try {

            findViewById(R.id.btnShare).setVisibility(View.GONE);
            findViewById(R.id.btnDone).setVisibility(View.GONE);
            findViewById(R.id.btnBack).setVisibility(View.GONE);

            View view = getWindow().getDecorView().findViewById(android.R.id.content);

            view.setDrawingCacheEnabled(true);

            android.graphics.Bitmap bitmap =
                    android.graphics.Bitmap.createBitmap(view.getDrawingCache());

            view.setDrawingCacheEnabled(false);

            findViewById(R.id.btnShare).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
            findViewById(R.id.btnBack).setVisibility(View.VISIBLE);

            PrintHelper photoPrinter = new PrintHelper(this);
            photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            photoPrinter.printBitmap("BBPS_Receipt", bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}