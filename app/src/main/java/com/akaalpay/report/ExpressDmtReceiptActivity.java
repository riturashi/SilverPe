package com.akaalpay.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.print.PrintHelper;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import org.json.JSONObject;

public class ExpressDmtReceiptActivity extends BaseActivity {

    TextView txtHeaderTitle, txtStatus, txtDate, txtAmount, txtAmountInWords;
    TextView txtReference, txtIfsc, txtUtr, txtAck, txtMobile, txtBeneName;
    TextView txtAccount, txtTxnType, txtShopName;

    RelativeLayout layoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_express_dmt_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        initViews();

        String id = getIntent().getStringExtra("id");
        loadReceipt(id);
    }

    private void initViews() {
        layoutStatus     = findViewById(R.id.layoutStatus);
        txtHeaderTitle   = findViewById(R.id.txtHeaderTitle);
        txtStatus        = findViewById(R.id.txtStatus);
        txtDate          = findViewById(R.id.txtDate);
        txtAmount        = findViewById(R.id.txtAmount);
        txtAmountInWords = findViewById(R.id.txtAmountInWords);
        txtReference     = findViewById(R.id.txtReference);
        txtIfsc          = findViewById(R.id.txtIfsc);
        txtUtr           = findViewById(R.id.txtUtr);
        txtAck           = findViewById(R.id.txtAck);
        txtMobile        = findViewById(R.id.txtMobile);
        txtBeneName      = findViewById(R.id.txtBeneName);
        txtAccount       = findViewById(R.id.txtAccount);
        txtTxnType       = findViewById(R.id.txtTxnType);
        txtShopName      = findViewById(R.id.txtShopName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> generatePrintReceipt());
    }

    private void loadReceipt(String id) {
        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("id", id);

            ApiClient.post(this, "express-dmt/receipt", TOKEN, body, response -> {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        if (response.optInt("status") == 0) return;

                        JSONObject data = response.getJSONObject("data");

                        txtHeaderTitle.setText("Express Money Transfer Receipt");

                        // Amount
                        String amt = data.optString("amount");
                        txtAmount.setText("₹" + amt);
                        txtAmountInWords.setVisibility(View.VISIBLE);
                        txtAmountInWords.setText(convertToWords(amt) + " RUPEES ONLY");

                        // Status
                        int txnStatus = 0;
                        try { txnStatus = Integer.parseInt(data.optString("status")); } catch (Exception ignored) {}

                        txtStatus.setText(data.optString("message"));
                        layoutStatus.setBackgroundColor(
                                txnStatus == 1
                                        ? Color.parseColor("#2E7D32")
                                        : txnStatus == 0
                                        ? Color.parseColor("#EF4444")
                                        : Color.parseColor("#FACC15")
                        );

                        // Fields (no bank_name for Express DMT — IFSC shown instead)
                        txtDate.setText(data.optString("date"));
                        txtBeneName.setText("Beneficiary : " + data.optString("benename"));
                        txtMobile.setText("Mobile : " + data.optString("mobile"));
                        txtAccount.setText("Account : " + data.optString("account_no"));
                        txtIfsc.setText("IFSC : " + data.optString("ifsc"));
                        txtTxnType.setText("Type : " + data.optString("txntype"));
                        txtUtr.setText("UTR : " + data.optString("utr"));
                        txtAck.setText("ACK : " + data.optString("ackno"));
                        txtReference.setText("Ref No : " + data.optString("referenceid"));

                        // Shop Name
                        if (data.has("user_details")) {
                            JSONObject user = data.getJSONObject("user_details");
                            txtShopName.setText(user.optString("firm_name", "Bharat Pay Shop"));
                        }

                    } catch (Exception e) { e.printStackTrace(); }
                });
            }, error -> runOnUiThread(() -> UiUtils.dismissLoading()));

        } catch (Exception e) { e.printStackTrace(); }
    }

    private String convertToWords(String amountStr) {
        try {
            long n = (long) Double.parseDouble(amountStr);
            if (n == 0) return "ZERO";
            String[] units = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN",
                    "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN",
                    "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
            String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY",
                    "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
            return getWords(n, units, tens).trim();
        } catch (Exception e) { return ""; }
    }

    private String getWords(long n, String[] units, String[] tens) {
        if (n < 20)     return units[(int) n];
        if (n < 100)    return tens[(int)(n / 10)] + " " + units[(int)(n % 10)];
        if (n < 1000)   return units[(int)(n / 100)] + " HUNDRED " + getWords(n % 100, units, tens);
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
            printer.printBitmap("ExpressDMT_Receipt", bitmap);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
