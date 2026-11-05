package com.silverpe.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.silverpe.BaseActivity;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class SupportActivity extends BaseActivity {

    // Views initialization
    TextView tvCareMobile, tvCareEmail;
    TextView tvParentName, tvParentPost, tvParentMobile, tvParentEmail, labelDistributor;
    CardView cardDistributor;
    Button btnCallParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        // Back button logic
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Fetch data from API
        fetchSupportDetails();
    }

    private void initViews() {
        // Customer Care Views
        tvCareMobile = findViewById(R.id.tvCareMobile);
        tvCareEmail = findViewById(R.id.tvCareEmail);

        // Distributor Views
        tvParentName = findViewById(R.id.tvParentName);
        tvParentPost = findViewById(R.id.tvParentPost);
        tvParentMobile = findViewById(R.id.tvParentMobile);
        tvParentEmail = findViewById(R.id.tvParentEmail);

        labelDistributor = findViewById(R.id.labelDistributor);
        cardDistributor = findViewById(R.id.cardDistributor);
        btnCallParent = findViewById(R.id.btnCallParent);
    }

    private void fetchSupportDetails() {
        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        UiUtils.showLoading(this);

        ApiClient.get(this, "support_contact_details", TOKEN, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                try {
                    JSONObject data = response.getJSONObject("data");

                    // 1. Customer Care Details
                    String careEmail = data.optString("customer_care_email", "N/A");
                    String careNo = data.optString("customer_care_no", "N/A");
                    String careWhatsApp = data.optString("customer_whatapp_support", "N/A");

                    tvCareEmail.setText(careEmail);
                    tvCareMobile.setText(careNo);

                    // Click Listeners for Care
                    findViewById(R.id.layoutCallCare).setOnClickListener(v -> makeCall(careNo));
                    findViewById(R.id.layoutEmailCare).setOnClickListener(v -> sendEmail(careEmail));

                    // WhatsApp Care
                    
                    findViewById(R.id.layoutWhatsAppCare).setOnClickListener(v ->
                            openWhatsApp(careWhatsApp, "Hello BharatDigiCash Support Team, I need help."));

                    // 2. Parent/Distributor Details
                    if (!data.isNull("parent_name") && !data.optString("parent_name").isEmpty()) {
                        labelDistributor.setVisibility(View.VISIBLE);
                        cardDistributor.setVisibility(View.VISIBLE);

                        String pName = data.optString("parent_name", "N/A");
                        String pMobile = data.optString("parent_mobile", "N/A");
                        String pPost = data.optString("parent_post", "Distributor");
                        String pEmail = data.optString("parent_email", "N/A");

                        tvParentName.setText(pName);
                        tvParentPost.setText(pPost.toUpperCase());
                        tvParentMobile.setText(pMobile);
                        tvParentEmail.setText(pEmail);

                        // Click Listeners for Distributor
                        btnCallParent.setOnClickListener(v -> makeCall(pMobile));
                        findViewById(R.id.layoutCallParent).setOnClickListener(v -> makeCall(pMobile));
                        findViewById(R.id.layoutEmailParent).setOnClickListener(v -> sendEmail(pEmail));

                        // WhatsApp Distributor (Using Parent Mobile)
                        findViewById(R.id.layoutWhatsAppParent).setOnClickListener(v ->
                                openWhatsApp(pMobile, "Hello " + pName + ", I am your retailer and I need support."));

                    } else {
                        labelDistributor.setVisibility(View.GONE);
                        cardDistributor.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    UiUtils.showToast(this, "Response Error: " + e.getMessage(), false);
                }
            });
        }, error -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                UiUtils.showToast(this, "Server not responding", false);
            });
        });
    }

    // Method to Open WhatsApp
    private void openWhatsApp(String number, String message) {
        if (number == null || number.isEmpty() || number.equals("N/A")) {
            Toast.makeText(this, "WhatsApp number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clean number and add 91 if missing
        String cleanNumber = number.replaceAll("[^0-9]", "");
        if (cleanNumber.length() == 10) {
            cleanNumber = "91" + cleanNumber;
        }

        try {
            String url = "https://wa.me/" + cleanNumber + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    // Common method for Calling
    private void makeCall(String number) {
        if (number == null || number.isEmpty() || number.equals("N/A")) {
            Toast.makeText(this, "Number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // Common method for Emailing
    private void sendEmail(String email) {
        if (email == null || email.isEmpty() || email.equals("N/A")) {
            Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Query - FenoPay");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }
}