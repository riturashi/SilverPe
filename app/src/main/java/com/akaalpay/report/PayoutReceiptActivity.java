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

public class PayoutReceiptActivity extends BaseActivity {

    TextView txtHeaderTitle, txtStatus, txtDate, txtAmount, txtAmountInWords;
    TextView txtReference, txtBank, txtAck;
    TextView txtAccount, txtIFSC, txtMode;

    RelativeLayout layoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        // ✅ SAME FLOW (ID PASS)
        String id = getIntent().getStringExtra("id");

        // ✅ API CALL
        loadReceipt(id);
    }

    private void initViews() {
        layoutStatus = findViewById(R.id.layoutStatus);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        txtStatus = findViewById(R.id.txtStatus);
        txtDate = findViewById(R.id.txtDate);
        txtAmount = findViewById(R.id.txtAmount);
        txtAmountInWords = findViewById(R.id.txtAmountInWords);

        txtBank = findViewById(R.id.txtBank);
        txtAccount = findViewById(R.id.txtAccount);
        txtIFSC = findViewById(R.id.txtIFSC);
        txtMode = findViewById(R.id.txtMode);
        txtReference = findViewById(R.id.txtReference);
        txtAck = findViewById(R.id.txtAck);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> generatePrintReceipt());
    }

    // ✅ SAME FUNCTION (ONLY API CHANGE)
    private void loadReceipt(String id) {

        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("id", id);

            // 🔥 CHANGE API HERE (IMPORTANT)
            ApiClient.post(this, "payout/receipt", TOKEN, body, response -> {

                runOnUiThread(() -> {
                    UiUtils.dismissLoading();

                    try {
                        int status = response.optInt("status");
                        if (status == 0) return;

                        JSONObject data = response.getJSONObject("data");

                        // HEADER
                        txtHeaderTitle.setText("Payout Receipt");
                        txtStatus.setText(data.optString("message"));

                        // AMOUNT
                        String amt = data.optString("amount");
                        txtAmount.setText("₹" + amt);
                        txtAmountInWords.setVisibility(View.VISIBLE);
                        txtAmountInWords.setText(convertToWords(amt) + " RUPEES ONLY");

                        // DATA BIND
                        txtDate.setText(data.optString("date"));
                        txtBank.setText("Bank : " + data.optString("bank_name"));
                        txtAccount.setText("Account : " + data.optString("account_no"));
                        txtIFSC.setText("IFSC : " + data.optString("ifsc"));
                        txtMode.setText("Mode : " + data.optString("mode"));
                        txtReference.setText("Reference ID : " + data.optString("refid"));
                        txtAck.setText("ACK : " + data.optString("ackno"));

                        // STATUS COLOR
                        int txnStatus = data.optInt("status");
                        if (txnStatus == 1)
                            layoutStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                        else
                            layoutStatus.setBackgroundColor(Color.parseColor("#C62828"));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            }, error -> runOnUiThread(UiUtils::dismissLoading));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SAME FUNCTION
    private String convertToWords(String amountStr) {
        try {
            long n = (long) Double.parseDouble(amountStr);
            if (n == 0) return "ZERO";

            String[] units = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX",
                    "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE",
                    "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN",
                    "SEVENTEEN", "EIGHTEEN", "NINETEEN"};

            String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY",
                    "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};

            return getWords(n, units, tens).trim();

        } catch (Exception e) {
            return "";
        }
    }

    private String getWords(long n, String[] units, String[] tens) {
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) (n / 10)] + " " + units[(int) (n % 10)];
        if (n < 1000) return units[(int) (n / 100)] + " HUNDRED " + getWords(n % 100, units, tens);
        if (n < 100000) return getWords(n / 1000, units, tens) + " THOUSAND " + getWords(n % 1000, units, tens);
        return getWords(n / 100000, units, tens) + " LAKH " + getWords(n % 100000, units, tens);
    }

    private void generatePrintReceipt() {
        try {
            findViewById(R.id.btnShare).setVisibility(View.GONE);
            findViewById(R.id.btnDone).setVisibility(View.GONE);
            findViewById(R.id.btnBack).setVisibility(View.GONE);

            View view = getWindow().getDecorView().findViewById(android.R.id.content);
            view.setDrawingCacheEnabled(true);
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            findViewById(R.id.btnShare).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
            findViewById(R.id.btnBack).setVisibility(View.VISIBLE);

            PrintHelper printer = new PrintHelper(this);
            printer.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printer.printBitmap("Payout_Receipt", bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}