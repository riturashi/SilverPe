package com.akaalpay.bbps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.report.BbpsReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class BBPSActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView tvTitle, tvBillInfo;
    AutoCompleteTextView spBiller;        // ✅ TextView nahi, AutoCompleteTextView
    LinearLayout dynamicBox;

    TextInputLayout boxAmount, boxMobile;
    TextInputEditText etAmount, etMobile;

    MaterialButton btnFetch, btnPay;
    MaterialCardView cardBill;

    String TOKEN = "";
    String categoryCode = "", categoryName = "";
    String selectedBillerId = "";
    String fetchType = "";
    String fetchId = "";
    boolean adhoc = false;

    // ✅ NAYA — biller_info se milega, default AGT
    String paymentChannel = "AGT";

    ArrayList<String> billerNames = new ArrayList<>();
    ArrayList<String> billerIds = new ArrayList<>();
    ArrayList<View> fieldViews = new ArrayList<>();
    ArrayList<String> fieldNames = new ArrayList<>();

    // ─────────────────────────────────────────
    //  ON CREATE
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_bbps);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        init();   // ✅ pehle init — phir sab kuch

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        categoryCode = getIntent().getStringExtra("category");
        categoryName = getIntent().getStringExtra("name");
        tvTitle.setText(categoryName);

        btnBack.setOnClickListener(v -> finish());

        loadBillers();

        btnFetch.setOnClickListener(v -> fetchBill());
        btnPay.setOnClickListener(v -> payBill());
    }

    // ─────────────────────────────────────────
    //  INIT — sirf findViewById yahan
    // ─────────────────────────────────────────
    private void init() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        spBiller = findViewById(R.id.spBiller);   // ✅ pehle find karo
        dynamicBox = findViewById(R.id.dynamicBox);

        boxAmount = findViewById(R.id.boxAmount);
        boxMobile = findViewById(R.id.boxMobile);
        etAmount = findViewById(R.id.etAmount);
        etMobile = findViewById(R.id.etMobile);

        btnFetch = findViewById(R.id.btnFetch);
        btnPay = findViewById(R.id.btnPay);

        cardBill = findViewById(R.id.cardBill);
        tvBillInfo = findViewById(R.id.tvBillInfo);

        // ✅ spBiller fix — focus hatao, touch pe sheet kholo
        spBiller.setFocusable(false);
        spBiller.setFocusableInTouchMode(false);
        spBiller.setCursorVisible(false);
        spBiller.setKeyListener(null);

        spBiller.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                openBillerSheet();
            }
            return true;
        });
    }

    // ─────────────────────────────────────────
    //  ALERT DIALOG HELPER
    // ─────────────────────────────────────────
    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    // ─────────────────────────────────────────
    //  OPEN BILLER BOTTOM SHEET
    // ─────────────────────────────────────────
    private void openBillerSheet() {

        if (billerNames.isEmpty()) {
            showDialog("Wait", "Billers are still loading, please wait.");
            return;
        }

        SelectionBottomSheet.newInstance("Select Biller", billerNames, (name, pos) -> {
            spBiller.setText(name);
            selectedBillerId = billerIds.get(pos);
            loadBillerInfo(selectedBillerId);
        }).show(getSupportFragmentManager(), "BILLER");
    }

    // ─────────────────────────────────────────
    //  LOAD BILLERS
    // ─────────────────────────────────────────
    private void loadBillers() {
        try {
            JSONObject body = new JSONObject();
            body.put("category", categoryCode);

            UiUtils.showLoading(this);

            ApiClient.post(this, "bbps/biller-list", TOKEN, body,

                    response -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        try {
                            JSONArray arr = response.getJSONArray("data");
                            billerNames.clear();
                            billerIds.clear();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject ob = arr.getJSONObject(i);
                                billerNames.add(ob.getString("biller_name"));
                                billerIds.add(ob.getString("biller_id"));
                            }
                        } catch (Exception e) {
                            showDialog("Error", "Failed to parse biller list.");
                        }
                    }),

                    error -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        showDialog("Error", "Failed to load billers:\n" + error);
                    }));

        } catch (Exception e) {
            showDialog("Error", "Something went wrong while loading billers.");
        }
    }

    // ─────────────────────────────────────────
    //  LOAD BILLER INFO
    // ─────────────────────────────────────────
    private void loadBillerInfo(String billerId) {
        resetScreen();

        try {
            JSONObject body = new JSONObject();
            body.put("billerId", billerId);

            UiUtils.showLoading(this);

            ApiClient.post(this, "bbps/biller-info", TOKEN, body,

                    response -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();

                        try {
                            boolean status = response.optBoolean("status", false);

                            if (!status) {
                                showDialog("Failed", response.optString("message", "Failed to load biller info."));
                                return;
                            }

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) {
                                showDialog("Error", "Biller data not found.");
                                return;
                            }

                            fetchType = data.optString("billerFetchRequiremet", "NOT_SUPPORTED");

                            adhoc = data.optString("billerAdhoc", "false").equalsIgnoreCase("true");

                            // Payment Channel
                            paymentChannel = data.optString("billerPaymentChannels", "AGT");

                            JSONArray groups = data.optJSONArray("billerInputParams");
                            if (groups == null) groups = new JSONArray();

                            for (int a = 0; a < groups.length(); a++) {
                                JSONObject group = groups.optJSONObject(a);
                                if (group == null) continue;

                                JSONArray params = group.optJSONArray("paramsList");
                                if (params == null) continue;

                                for (int i = 0; i < params.length(); i++) {
                                    JSONObject p = params.optJSONObject(i);
                                    if (p == null) continue;

                                    String visible = p.optString("visibility", "true");
                                    if (visible.equalsIgnoreCase("false")) continue;

                                    createDynamicField(p);
                                }
                            }

                            if (fetchType.equalsIgnoreCase("NOT_SUPPORTED")) {
                                boxMobile.setVisibility(View.VISIBLE);
                                boxAmount.setVisibility(View.VISIBLE);
                                btnPay.setVisibility(View.VISIBLE);
                            } else {
                                boxMobile.setVisibility(View.VISIBLE);
                                btnFetch.setVisibility(View.VISIBLE);
                            }

                        } catch (Exception e) {
                            showDialog("Error", "Failed to parse biller info.");
                        }
                    }),

//                    response -> runOnUiThread(() -> {
//                        UiUtils.dismissLoading();
//
//                        try {
//                            int status = response.optInt("status", 0);
//                            if (status != 1) {
//                                showDialog("Failed", response.optString("message", "Failed to load biller info."));
//                                return;
//                            }
//
//                            JSONObject data = response.optJSONObject("data");
//                            if (data == null) {
//                                showDialog("Error", "Biller data not found.");
//                                return;
//                            }
//
//                            fetchType = data.optString("billerFetchRequiremet", "NOT_SUPPORTED");
//
//                            adhoc = data.optString("billerAdhoc", "false").equalsIgnoreCase("true");
//
//                            // ✅ NAYA — biller ko AGT chahiye ya INT, wahi save karo
//                            paymentChannel = data.optString("billerPaymentChannels", "AGT");
//
//                            JSONArray groups = data.optJSONArray("billerInputParams");
//                            if (groups == null) groups = new JSONArray();
//
//                            for (int a = 0; a < groups.length(); a++) {
//                                JSONObject group = groups.optJSONObject(a);
//                                if (group == null) continue;
//
//                                JSONArray params = group.optJSONArray("paramsList");
//                                if (params == null) continue;
//
//                                for (int i = 0; i < params.length(); i++) {
//                                    JSONObject p = params.optJSONObject(i);
//                                    if (p == null) continue;
//
//                                    String visible = p.optString("visibility", "true");
//                                    if (visible.equalsIgnoreCase("false")) continue;
//
//                                    createDynamicField(p);
//                                }
//                            }
//
//                            if (fetchType.equalsIgnoreCase("NOT_SUPPORTED")) {
//                                boxMobile.setVisibility(View.VISIBLE);
//                                boxAmount.setVisibility(View.VISIBLE);
//                                btnPay.setVisibility(View.VISIBLE);
//                            } else {
//                                boxMobile.setVisibility(View.VISIBLE);
//                                btnFetch.setVisibility(View.VISIBLE);
//                            }
//
//                        } catch (Exception e) {
//                            showDialog("Error", "Failed to parse biller info.");
//                        }
//                    }),

                    error -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        showDialog("Error", "Biller info API failed:\n" + error);
                    }));

        } catch (Exception e) {
            showDialog("Error", "Something went wrong while loading biller info.");
        }
    }

    // ─────────────────────────────────────────
    //  CREATE DYNAMIC FIELD
    // ─────────────────────────────────────────


    private void createDynamicField(JSONObject p) {
        try {
            String label = p.optString("paramName", "");
            String type = p.optString("dataType", "TEXT");
            String max = p.optString("maxLength", "");
            String min = p.optString("minLength", "");
            String regex = p.optString("regEx", "");
            String values = p.optString("values", "");

            fieldNames.add(label);

            int primaryColor = androidx.core.content.ContextCompat.getColor(
                    this, R.color.feno_primary
            );

            TextInputLayout til = new TextInputLayout(
                    new ContextThemeWrapper(this, R.style.BBPSInputStyle)
            );

            til.setHint(label);
            til.setHintTextColor(
                    android.content.res.ColorStateList.valueOf(primaryColor)
            );

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            lp.topMargin = dpToPx(14);
            til.setLayoutParams(lp);

            // DROPDOWN
            if (!values.trim().isEmpty()) {

                AutoCompleteTextView ac = new AutoCompleteTextView(
                        new ContextThemeWrapper(this, R.style.BBPSInputStyle)
                );

                ac.setTextSize(15f);
                ac.setTextColor(android.graphics.Color.BLACK);
                ac.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16));
                ac.setInputType(InputType.TYPE_NULL);
                ac.setFocusable(false);
                ac.setClickable(true);

                String[] arr = values.split(",");

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_1, arr
                );

                ac.setAdapter(adapter);

                ac.setOnClickListener(v -> ac.showDropDown());

                til.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
                til.setStartIconDrawable(android.R.drawable.ic_menu_agenda);
                til.setStartIconTintList(
                        android.content.res.ColorStateList.valueOf(primaryColor)
                );

                til.addView(ac);
                fieldViews.add(ac);

            } else {

                TextInputEditText et = new TextInputEditText(
                        new ContextThemeWrapper(this, R.style.BBPSInputStyle)
                );

                et.setTextSize(15f);
                et.setTextColor(android.graphics.Color.BLACK);
                et.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16));

                if (type.equalsIgnoreCase("NUMERIC")) {

                    et.setInputType(InputType.TYPE_CLASS_NUMBER);

                    til.setStartIconDrawable(android.R.drawable.ic_menu_call);

                } else if (type.equalsIgnoreCase("ALPHANUMERIC")) {

                    et.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

                    til.setStartIconDrawable(android.R.drawable.ic_menu_edit);

                } else {

                    et.setInputType(InputType.TYPE_CLASS_TEXT);

                    til.setStartIconDrawable(android.R.drawable.ic_menu_edit);
                }

                til.setStartIconTintList(
                        android.content.res.ColorStateList.valueOf(primaryColor)
                );

                if (!max.isEmpty()) {
                    et.setFilters(new InputFilter[]{
                            new InputFilter.LengthFilter(Integer.parseInt(max))
                    });
                }

                til.addView(et);
                fieldViews.add(et);

            }

            dynamicBox.addView(til);

        } catch (Exception e) {
            e.printStackTrace();
            showDialog("Error", "Failed to create input field: " + e.getMessage());
        }
    }

//    private void createDynamicField(JSONObject p) {
//        try {
//            String label = p.optString("paramName", "");
//            String type = p.optString("dataType", "TEXT");
//            String max = p.optString("maxLength", "");
//            String values = p.optString("values", "");
//
//            fieldNames.add(label);
//
//            TextInputLayout til = new TextInputLayout(new ContextThemeWrapper(this, R.style.BBPSInputStyle));
//
//            til.setHint(label);
//            til.setHintTextColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("@color/feno_primary")));
//
//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//            lp.topMargin = dpToPx(14);
//            til.setLayoutParams(lp);
//
//            // ── DROPDOWN ──
//            if (!values.trim().isEmpty()) {
//
//                AutoCompleteTextView ac = new AutoCompleteTextView(new ContextThemeWrapper(this, R.style.BBPSInputStyle));
//
//                ac.setTextSize(15f);
//                ac.setTextColor(android.graphics.Color.parseColor("#111111"));
//                ac.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16));
//                ac.setInputType(InputType.TYPE_NULL);
//                ac.setFocusable(false);
//                ac.setClickable(true);
//
//                String[] arr = values.split(",");
//                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arr);
//                ac.setAdapter(adapter);
//
//                ac.setOnTouchListener((v, event) -> {
//                    if (event.getAction() == MotionEvent.ACTION_UP) {
//                        ac.showDropDown();
//                    }
//                    return true;
//                });
//
//                til.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
//                til.setStartIconDrawable(android.R.drawable.ic_menu_agenda);
//                til.setStartIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("@color/feno_primary")));
//
//                til.addView(ac);
//                fieldViews.add(ac);   // ✅ directly ac add karo
//
//            }
//            // ── NORMAL FIELD ──
//            else {
//
//                TextInputEditText et = new TextInputEditText(new ContextThemeWrapper(this, R.style.BBPSInputStyle));
//
//                et.setTextSize(15f);
//                et.setTextColor(android.graphics.Color.parseColor("#111111"));
//                et.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16));
//
//                if (type.equalsIgnoreCase("NUMERIC")) {
//                    et.setInputType(InputType.TYPE_CLASS_NUMBER);
//                    til.setStartIconDrawable(android.R.drawable.ic_menu_call);
//                } else {
//                    et.setInputType(InputType.TYPE_CLASS_TEXT);
//                    til.setStartIconDrawable(android.R.drawable.ic_menu_edit);
//                }
//
//                til.setStartIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("@color/feno_primary")));
//
//                if (!max.isEmpty()) {
//                    et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.parseInt(max))});
//                }
//
//                til.addView(et);
//                fieldViews.add(et);   // ✅ directly et add karo
//            }
//
//            dynamicBox.addView(til);
//
//        } catch (Exception e) {
//            showDialog("Error", "Failed to create input field.");
//        }
//    }

    // ─────────────────────────────────────────
    //  COLLECT PARAMS
    // ─────────────────────────────────────────
    private JSONArray collectParams() {
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < fieldViews.size(); i++) {
                View v = fieldViews.get(i);
                String name = fieldNames.get(i);
                String value = "";

                if (v instanceof TextInputEditText) {
                    value = ((TextInputEditText) v).getText().toString().trim();
                } else if (v instanceof AutoCompleteTextView) {
                    value = ((AutoCompleteTextView) v).getText().toString().trim();
                } else if (v instanceof EditText) {
                    value = ((EditText) v).getText().toString().trim();
                }

                if (!value.isEmpty()) {
                    JSONObject ob = new JSONObject();
                    ob.put("paramName", name);
                    ob.put("paramValue", value);
                    arr.put(ob);
                }
            }
        } catch (Exception e) {
            showDialog("Error", "Failed to collect input parameters.");
        }
        return arr;
    }

    // ─────────────────────────────────────────
    //  FETCH BILL
    // ─────────────────────────────────────────
    private void fetchBill() {
        try {
            if (selectedBillerId.isEmpty()) {
                showDialog("Validation", "Please select a biller.");
                return;
            }

            if (etMobile.getText().toString().trim().isEmpty()) {
                showDialog("Validation", "Please enter mobile number.");
                return;
            }

            JSONObject body = new JSONObject();
            body.put("billerId", selectedBillerId);
            body.put("mobile", etMobile.getText().toString().trim());
            body.put("input", collectParams());
            body.put("paymentChannel", paymentChannel);   // ✅ NAYA

            UiUtils.showLoading(this);

            ApiClient.post(this, "bbps/fetchbill", TOKEN, body,

                    response -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        try {
                            int status = response.optInt("status", 0);
                            String msg = response.optString("message", "Failed");

                            if (status != 1) {
                                showDialog("Failed", msg);
                                return;
                            }

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) {
                                showDialog("Error", "No bill data received.");
                                return;
                            }

                            fetchId = data.optString("billFetchResponseId", "");

                            JSONObject bill = data.optJSONObject("billerResponse");
                            if (bill == null) bill = new JSONObject();

                            String amountPaisa = bill.optString("billAmount", "0");
                            double rupees = Double.parseDouble(amountPaisa) / 100.0;
                            String amount = String.format(java.util.Locale.US, "%.2f", rupees);

                            etAmount.setText(amount);

                            StringBuilder sb = new StringBuilder();
                            sb.append("Customer : ").append(bill.optString("customerName", "-")).append("\n");
                            sb.append("Bill No  : ").append(bill.optString("billNumber", "-")).append("\n");
                            sb.append("Bill Date: ").append(bill.optString("billDate", "-")).append("\n");
                            sb.append("Due Date : ").append(bill.optString("dueDate", "-")).append("\n");
                            sb.append("Amount   : ₹").append(amount).append("\n");

                            JSONObject add = data.optJSONObject("additionalInfo");
                            if (add != null) {
                                JSONArray info = add.optJSONArray("info");
                                if (info != null) {
                                    for (int i = 0; i < info.length(); i++) {
                                        JSONObject ob = info.optJSONObject(i);
                                        if (ob == null) continue;
                                        sb.append(ob.optString("infoName")).append(" : ").append(ob.optString("infoValue")).append("\n");
                                    }
                                }
                            }

                            tvBillInfo.setText(sb.toString());

                            cardBill.setVisibility(View.VISIBLE);
                            boxAmount.setVisibility(View.VISIBLE);
                            btnPay.setVisibility(View.VISIBLE);
                            btnFetch.setVisibility(View.GONE);

                        } catch (Exception e) {
                            showDialog("Error", "Failed to parse bill response.");
                        }
                    }),

                    error -> runOnUiThread(() -> {
                        UiUtils.dismissLoading();
                        showDialog("Error", "Bill fetch failed:\n" + error);
                    }));

        } catch (Exception e) {
            showDialog("Error", "Something went wrong while fetching bill.");
        }
    }

    // ─────────────────────────────────────────
    //  PAY BILL
    // ─────────────────────────────────────────
    private void payBill() {

        try {

            if (selectedBillerId.isEmpty()) {
                showDialog("Validation", "Please select a biller.");
                return;
            }

            String rawAmount = etAmount.getText().toString().trim();
            if (rawAmount.isEmpty()) {
                showDialog("Validation", "Please enter amount.");
                return;
            }

            JSONObject body = new JSONObject();
            body.put("category", categoryCode);
            body.put("operator", selectedBillerId);

            double amt = Double.parseDouble(rawAmount);
            String finalAmount = String.format(java.util.Locale.US, "%.2f", amt);

            body.put("amount", finalAmount);
            body.put("ca_number", getFirstValue());
            body.put("fetch_id", fetchId);
            body.put("mobile", etMobile.getText().toString().trim());
            body.put("input", collectParams());
            body.put("payment_channel", paymentChannel);   // ✅ NAYA

            UiUtils.showLoading(this);

            ApiClient.post(this, "bbps/bdc-pay-bill", TOKEN, body, response -> runOnUiThread(() -> {
                UiUtils.dismissLoading();

                try {
                    int status = response.optInt("status");


                    if (status == 0) {
                        String message = response.optString("message");
                        showDialog("Response", message);
                        return;
                    }

                    // ✅ SUCCESS CASE
                    if (status == 1) {
                        JSONObject data = response.optJSONObject("data");
                        String id = "";

                        if (data != null) {
                            id = data.optString("id");
                        }

                        Intent intent = new Intent(BBPSActivity.this, BbpsReceiptActivity.class);
                        intent.putExtra("id", id);
                        startActivity(intent);
                        finish();
                    }

                } catch (Exception e) {
                    showDialog("Error", "Response handling failed.");
                }
            }), error -> runOnUiThread(() -> {
                UiUtils.dismissLoading();
                showDialog("Error", error);
            }));

        } catch (Exception e) {

            showDialog("Error", "Something went wrong while processing payment.");
        }
    }

    // ─────────────────────────────────────────
    //  GET FIRST VALUE
    // ─────────────────────────────────────────
    private String getFirstValue() {
        try {
            if (fieldViews.isEmpty()) return "";
            View v = fieldViews.get(0);
            if (v instanceof EditText) {
                return ((EditText) v).getText().toString().trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    // ─────────────────────────────────────────
    //  RESET SCREEN
    // ─────────────────────────────────────────
    private void resetScreen() {
        dynamicBox.removeAllViews();
        fieldViews.clear();
        fieldNames.clear();

        boxAmount.setVisibility(View.GONE);
        boxMobile.setVisibility(View.GONE);
        btnFetch.setVisibility(View.GONE);
        btnPay.setVisibility(View.GONE);
        cardBill.setVisibility(View.GONE);

        etAmount.setText("");
        etMobile.setText("");
        fetchId = "";
    }

    // ─────────────────────────────────────────
    //  DP TO PX
    // ─────────────────────────────────────────
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}