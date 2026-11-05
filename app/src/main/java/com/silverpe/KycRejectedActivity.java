package com.silverpe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;

import org.json.JSONObject;

public class KycRejectedActivity extends AppCompatActivity {

    private String careNo = "", careEmail = "", careWhatsApp = "";
    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc_rejected);
        StatusBarUtils.setupStatusBar(this, R.id.header);
        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        initViews();
        fetchSupportData();
    }

    private void initViews() {
        TextView tvRejectRemark = findViewById(R.id.tvRejectRemark);
        Button btnResubmit = findViewById(R.id.btnResubmit);

        // Intent se Remark lena
        String remark = getIntent().getStringExtra("REMARK");

        // Agar remark khali hai to default message dikhana
        if (remark == null || remark.isEmpty()) {
            remark = "Your documents were rejected. Please re-upload clear copies.";
        }

        // Sirf English Remark set karna
        tvRejectRemark.setText(remark);

        // Header Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Resubmit Click -> Go to KycUploadActivity
        btnResubmit.setOnClickListener(v -> {
            Intent intent = new Intent(this, KycUploadActivity.class);
            startActivity(intent);
            finish();
        });


        // 2. Support Buttons Click Listeners
        findViewById(R.id.btnWhatsappSupport).setOnClickListener(v -> {
            if (!careWhatsApp.isEmpty()) openWhatsApp(careWhatsApp, "My KYC is rejected. Help me.");
            else UiUtils.showToast(this, "Not Available", false);
        });

        findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            if (!careNo.isEmpty()) makeCall(careNo);
            else UiUtils.showToast(this, "Not Available", false);
        });

        findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            if (!careEmail.isEmpty()) sendEmail(careEmail);
            else UiUtils.showToast(this, "Not Available", false);
        });
    }

    private void fetchSupportData() {
        ApiClient.get(this, "support_contact_details", TOKEN, response -> {
            runOnUiThread(() -> {
                JSONObject data = response.optJSONObject("data");
                if (data != null) {
                    careEmail = data.optString("customer_care_email", "");
                    careNo = data.optString("customer_care_no", "");
                    careWhatsApp = data.optString("customer_whatapp_support", "");
                }
            });
        }, error -> {});
    }

    // Helper Methods
    private void openWhatsApp(String number, String msg) {
        String cleanNumber = number.replaceAll("[^0-9]", "");
        if (cleanNumber.length() == 10) cleanNumber = "91" + cleanNumber;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + cleanNumber + "?text=" + Uri.encode(msg))));
        } catch (Exception e) { Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show(); }
    }

    private void makeCall(String number) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    private void sendEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "KYC Rejection Query");
        startActivity(intent);
    }
}