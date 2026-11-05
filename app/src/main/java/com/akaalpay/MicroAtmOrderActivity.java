package com.akaalpay;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.HashMap;

public class MicroAtmOrderActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────
    // Order Form
    private LinearLayout layoutForm;
    private EditText etName, etMobile, etAddress, etCity, etState, etPincode;
    private TextView tvBaseAmt, tvGstAmt, tvTotalAmt;

    // Already Purchased Card
    private CardView layoutPurchased;
    private TextView tvOrderRef, tvOrderStatus, tvPayStatus, tvDeliveryAddr;

    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro_atm_order);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Form views
        layoutForm      = findViewById(R.id.layoutForm);
        etName          = findViewById(R.id.etName);
        etMobile        = findViewById(R.id.etMobile);
        etAddress       = findViewById(R.id.etAddress);
        etCity          = findViewById(R.id.etCity);
        etState         = findViewById(R.id.etState);
        etPincode       = findViewById(R.id.etPincode);
        tvBaseAmt       = findViewById(R.id.tvBaseAmt);
        tvGstAmt        = findViewById(R.id.tvGstAmt);
        tvTotalAmt      = findViewById(R.id.tvTotalAmt);

        // Purchased card views
        layoutPurchased = findViewById(R.id.cardPurchased);
        tvOrderRef      = findViewById(R.id.tvOrderRef);
        tvOrderStatus   = findViewById(R.id.tvOrderStatus);
        tvPayStatus     = findViewById(R.id.tvPayStatus);
        tvDeliveryAddr  = findViewById(R.id.tvDeliveryAddr);

        findViewById(R.id.btnPlaceOrder).setOnClickListener(v -> placeOrder());

        // Load pricing + check existing order
        loadPricingAndStatus();
    }

    // ── STEP 1: Pricing fetch + latest order check ──────────
    private void loadPricingAndStatus() {
        UiUtils.showLoading(this);

        ApiClient.get(this, "microatm-order/pricing", TOKEN, pricingRes -> {
            runOnUiThread(() -> {
                try {
                    JSONObject d = pricingRes.getJSONObject("data");


                    String base = d.getString("base_amount");
                    String gst = d.getString("gst_amount");
                    String total = d.getString("total_amount");

                    tvBaseAmt.setText("Base: ₹" + base);
                    tvGstAmt.setText("GST (18%): ₹" + gst);
                    tvTotalAmt.setText("₹" + total);

                    MaterialButton btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
                    btnPlaceOrder.setText("Pay ₹" + total + " from Wallet & Order");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            checkExistingOrder();

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Failed to load pricing", false);
        }));
    }

    // ── STEP 2: Check if order already placed ───────────────
    private void checkExistingOrder() {
        HashMap<String, String> params = new HashMap<>();
        params.put("page_no", "1");

        ApiClient.post(this, "microatm-order/list", TOKEN, new JSONObject(params), res -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                try {
                    JSONObject data = res.optJSONObject("data");
                    if (data != null && data.optInt("recordsTotal", 0) > 0) {
                        // Order exists → show purchased card
                        JSONObject latest = data.getJSONArray("list").getJSONObject(0);
                        showPurchasedCard(latest);
                    } else {
                        // No order → show form
                        layoutForm.setVisibility(View.VISIBLE);
                        layoutPurchased.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    layoutForm.setVisibility(View.VISIBLE);
                    layoutPurchased.setVisibility(View.GONE);
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            layoutForm.setVisibility(View.VISIBLE);
            layoutPurchased.setVisibility(View.GONE);
        }));
    }

    // ── Show "Already Purchased" card ────────────────────────
    private void showPurchasedCard(JSONObject order) {
        layoutForm.setVisibility(View.GONE);
        layoutPurchased.setVisibility(View.VISIBLE);

        try {
            tvOrderRef.setText("Order Ref: " + order.getString("order_ref"));
            tvDeliveryAddr.setText(
                    order.getString("delivery_address") + ", " +
                            order.getString("delivery_city") + " - " +
                            order.getString("delivery_pincode")
            );

            // Status label
            int status = order.optInt("status", 2);
            String statusLabel;
            switch (status) {
                case 0: statusLabel = "❌ Cancelled"; break;
                case 1: statusLabel = "✅ Delivered"; break;
                case 3: statusLabel = "🔄 Processing"; break;
                case 4: statusLabel = "🚚 Shipped"; break;
                default: statusLabel = "⏳ Pending";
            }
            tvOrderStatus.setText(statusLabel);

            // Payment label
            int payStatus = order.optInt("payment_status", 0);
            tvPayStatus.setText(payStatus == 1 ? "✅ Paid" : "⚠️ Payment Pending");

        } catch (Exception ignored) {}
    }

    // ── STEP 3: Place Order ──────────────────────────────────
    private void placeOrder() {
        String name    = etName.getText().toString().trim();
        String mobile  = etMobile.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String city    = etCity.getText().toString().trim();
        String state   = etState.getText().toString().trim();
        String pincode = etPincode.getText().toString().trim();

        if (name.isEmpty())           { toast("Enter delivery name");    return; }
        if (mobile.length() != 10)    { toast("Enter 10 digit mobile");  return; }
        if (address.isEmpty())        { toast("Enter address");          return; }
        if (city.isEmpty())           { toast("Enter city");             return; }
        if (state.isEmpty())          { toast("Enter state");            return; }
        if (pincode.length() != 6)    { toast("Enter 6 digit pincode");  return; }

        UiUtils.showLoading(this);

        HashMap<String, String> params = new HashMap<>();
        params.put("delivery_name",    name);
        params.put("delivery_mobile",  mobile);
        params.put("delivery_address", address);
        params.put("delivery_city",    city);
        params.put("delivery_state",   state);
        params.put("delivery_pincode", pincode);
        params.put("payment_mode",     "WALLET");

        ApiClient.post(this, "microatm-order/place", TOKEN, new JSONObject(params), res -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                try {
                    String msg = res.optString("message", "Order placed");
                    toast(msg);

                    JSONObject data = res.optJSONObject("data");
                    if (data != null) {
                        showPurchasedCard(data);
                    }
                } catch (Exception ignored) {
                    toast("Order placed successfully");
                    finish();
                }
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            toast("Failed to place order");
        }));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}