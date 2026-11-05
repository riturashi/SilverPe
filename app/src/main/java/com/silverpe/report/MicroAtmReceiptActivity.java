package com.silverpe.report;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.print.PrintHelper;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class MicroAtmReceiptActivity extends BaseActivity {

    TextView txtHeaderTitle, txtStatus, txtDate, txtAmount, txtAmountInWords;
    TextView txtReference, txtBank, txtRRN, txtAck, txtAadhar, txtMobile;
    TextView txtAvailableBalance, txtShopName;

    RelativeLayout layoutStatus;
    CardView layoutMiniStatement;
    LinearLayout miniStatementContainer, layoutAvailableBalance;
    View lineBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microatm_receipt);
        StatusBarUtils.setupStatusBar(this, R.id.layoutStatus);
        // 1. Initialization
        initViews();

        // 2. Get Data from Intent (Dashboard se aaya hua data)
        String id = getIntent().getStringExtra("id");
        String statementJson = getIntent().getStringExtra("statement_json");

        // 3. Agar Dashboard ne statement pehle hi bhej diya hai (Fast Load)
        if (statementJson != null && !statementJson.isEmpty()) {
            try {
                layoutMiniStatement.setVisibility(View.VISIBLE);
                JSONObject offlineData = new JSONObject();
                offlineData.put("ministatement", new JSONArray(statementJson));
                setupMiniStatementTable(offlineData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. Load Full Receipt from Server
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
        layoutAvailableBalance = findViewById(R.id.layoutAvailableBalance);
        txtAvailableBalance = findViewById(R.id.txtAvailableBalance);
        lineBalance = findViewById(R.id.lineBalance);
        txtShopName = findViewById(R.id.txtShopName);
        layoutMiniStatement = findViewById(R.id.layoutMiniStatement);
        miniStatementContainer = findViewById(R.id.miniStatementContainer);

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

            ApiClient.post(this, "microatm/receipt", TOKEN, body, response -> {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        int status = response.optInt("status");
                        if (status == 0) return;

                        JSONObject data = response.getJSONObject("data");
                        String txnTypeCode = data.optString("transaction_type");

                        // Header Logic
                        String title;
                        switch (txnTypeCode) {
                            case "MS":
                                title = "Mini Statement";
                                break;
                            case "BE":
                                title = "Balance Enquiry";
                                break;
                            case "CW":
                                title = "Cash Withdraw";
                                break;
                            case "CD":
                                title = "Cash Deposit";
                                break;
                            default:
                                title = "Transaction Receipt"; // Default title agar koi match na kare
                                break;
                        }
                        txtHeaderTitle.setText(title);
                        txtStatus.setText(data.optString("response_message"));

                        // Amount & Words Logic
                        if (txnTypeCode.equals("CW") || txnTypeCode.equals("CD")) {
                            String amt = data.optString("amount");
                            txtAmount.setText("₹" + amt);
                            txtAmountInWords.setVisibility(View.VISIBLE);
                            txtAmountInWords.setText(convertToWords(amt) + " RUPEES ONLY");
                        } else {
                            txtAmount.setText("₹" + data.optString("balanceamount"));
                            txtAmountInWords.setVisibility(View.GONE);
                        }

                        // Data Fill
                        txtDate.setText(data.optString("date"));
                        txtBank.setText("Bank : " + data.optString("cardnumber"));
                        txtReference.setText("Ref No : " + data.optString("referenceno"));
                        txtRRN.setText("RRN : " + data.optString("bankrrn"));
                        txtAck.setText("ACK : " + data.optString("ackno"));
                        txtAadhar.setText("Aadhar : " + data.optString("cardnumber"));
                        txtMobile.setText("Mobile : " + data.optString("mobile_no"));

                        // Shop Name
                        if (data.has("user_details")) {
                            txtShopName.setText(data.getJSONObject("user_details").optString("firm_name", "Bharat Pay Shop"));
                        }

                        // Status Color
                        int txnStatus = data.optInt("status");
                        if (txnStatus == 1) layoutStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                        else layoutStatus.setBackgroundColor(Color.parseColor("#C62828"));

                        // Update Table if API has statement data
                        if (txnTypeCode.equals("MS")) {
                            JSONArray arr = data.optJSONArray("ministatement");
                            if (arr != null && arr.length() > 0) {
                                layoutMiniStatement.setVisibility(View.VISIBLE);
                                setupMiniStatementTable(data);
                            }
                        }

                    } catch (Exception e) { e.printStackTrace(); }
                });
            }, error -> {
                runOnUiThread(() -> UiUtils.dismissLoading());
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupMiniStatementTable(JSONObject data) throws Exception {
        miniStatementContainer.removeAllViews();

        TextView title = new TextView(this);
        title.setText("RECENT TRANSACTIONS");
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(10, 20, 10, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        miniStatementContainer.addView(title);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setPadding(8, 15, 8, 15);
        headerRow.setBackgroundColor(Color.parseColor("@color/rec_border"));
        headerRow.addView(createColumn("Date", 0.25f, true, false));
        headerRow.addView(createColumn("Narration", 0.45f, true, false));
        headerRow.addView(createColumn("T", 0.10f, true, true));
        headerRow.addView(createColumn("Amount", 0.20f, true, true));
        miniStatementContainer.addView(headerRow);

        JSONArray arr = data.optJSONArray("ministatement");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(8, 18, 8, 18);

                row.addView(createColumn(obj.optString("date"), 0.25f, false, false));
                row.addView(createColumn(obj.optString("narration"), 0.45f, false, false));

                // Smart Type Logic (Narration se C/D nikalna)
                String narration = obj.optString("narration", "").toUpperCase();
                String type = " ";
                if (narration.contains(" C ") || narration.endsWith(" C")) type = "C";
                else if (narration.contains(" D ") || narration.endsWith(" D")) type = "D";

                TextView tvType = createColumn(type, 0.10f, false, true);
                if (type.equals("C")) tvType.setTextColor(Color.parseColor("#2E7D32"));
                else if (type.equals("D")) tvType.setTextColor(Color.parseColor("#C62828"));
                row.addView(tvType);

                row.addView(createColumn(obj.optString("amount"), 0.20f, false, true));
                miniStatementContainer.addView(row);

                View line = new View(this);
                line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                line.setBackgroundColor(Color.parseColor("#E0E0E0"));
                miniStatementContainer.addView(line);
            }
        }
    }

    private TextView createColumn(String text, float weight, boolean isHeader, boolean isRightAlign) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        tv.setText(text);
        tv.setTextSize(isHeader ? 11 : 10);
        tv.setTextColor(isHeader ? Color.BLACK : Color.parseColor("@color/text_head_title"));
        if (isHeader) tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(isRightAlign ? Gravity.END : Gravity.START);
        return tv;
    }

    private String convertToWords(String amountStr) {
        try {
            long n = (long) Double.parseDouble(amountStr);
            if (n == 0) return "ZERO";
            String[] units = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
            String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
            return getWords(n, units, tens).trim();
        } catch (Exception e) { return ""; }
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

            PrintHelper photoPrinter = new PrintHelper(this);
            photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            photoPrinter.printBitmap("AEPS_Receipt", bitmap);
        } catch (Exception e) { e.printStackTrace(); }
    }
}