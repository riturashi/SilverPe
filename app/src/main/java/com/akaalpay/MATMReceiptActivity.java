package com.akaalpay;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MATMReceiptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matm_receipt);

        // XML screenshot ke hisaab se exact IDs
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvRefNo = findViewById(R.id.tvRefNo); // Screenshot ki ID
        TextView tvRrn = findViewById(R.id.tvRrn);     // Screenshot ki ID
        TextView tvCard = findViewById(R.id.tvCard);   // Screenshot ki ID
        TextView tvBank = findViewById(R.id.tvBank);   // Screenshot ki ID
        TextView tvDate = findViewById(R.id.tvDate);
        ImageView ivStatusIcon = findViewById(R.id.ivStatusIcon);

        // Data set karne ka logic (Intent se aane wala data)
        if (getIntent() != null) {
            tvAmount.setText("₹ " + getIntent().getStringExtra("amount"));
            tvRefNo.setText(getIntent().getStringExtra("refNo"));
            tvRrn.setText(getIntent().getStringExtra("bankRrn"));
            tvCard.setText(getIntent().getStringExtra("cardNo"));
            tvBank.setText(getIntent().getStringExtra("bankName"));
            tvDate.setText(getIntent().getStringExtra("date"));

            String status = getIntent().getStringExtra("status");
            if (status != null && (status.equalsIgnoreCase("Success") || status.equals("1"))) {
                tvStatus.setText("SUCCESS");
                ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            } else {
                tvStatus.setText("FAILED");
                ivStatusIcon.setImageResource(android.R.drawable.ic_delete);
            }
        }

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}