package com.akaalpay.payout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.report.PayoutReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.Locale;

public class DoPayoutActivity extends AppCompatActivity {

    private String beneId, TOKEN = "";

    private TextInputEditText etAmount, etTxnPassword, etOtp;
    private TextInputLayout layoutOtp;
    private RadioGroup rgMode;
    private RadioButton rbImps, rbNeft;
    private LinearLayout btnImpsView, btnNeftView;
    private Button btnSubmit;

    private TextView tvBank, tvAcc, tvIfsc, tvHolder;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_payout);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();
        setIntentData();
        setupTransferToggle();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) tts.setLanguage(Locale.US);
        });

        btnSubmit.setOnClickListener(v -> handlePayout());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etAmount      = findViewById(R.id.etAmount);
        etTxnPassword = findViewById(R.id.etTxnPassword);
        etOtp         = findViewById(R.id.etOtp);
        layoutOtp     = findViewById(R.id.layoutOtp);
        rgMode        = findViewById(R.id.rgMode);
        rbImps        = findViewById(R.id.rbImps);
        rbNeft        = findViewById(R.id.rbNeft);
        btnImpsView   = findViewById(R.id.btnImpsView);
        btnNeftView   = findViewById(R.id.btnNeftView);
        btnSubmit     = findViewById(R.id.btnSubmitPayout);

        tvBank   = findViewById(R.id.tvTargetBank);
        tvAcc    = findViewById(R.id.tvTargetAcc);
        tvIfsc   = findViewById(R.id.tvTargetIfsc);
        tvHolder = findViewById(R.id.tvTargetHolder);
    }

    private void setupTransferToggle() {
        // Default: IMPS selected
        selectImps();

        btnImpsView.setOnClickListener(v -> selectImps());
        btnNeftView.setOnClickListener(v -> selectNeft());
    }

    private void selectImps() {
        rbImps.setChecked(true);

        btnImpsView.setBackgroundResource(R.drawable.bg_transfer_selected);
        btnNeftView.setBackgroundColor(Color.TRANSPARENT);

        ((TextView) btnImpsView.getChildAt(0)).setTextColor(Color.WHITE);
        ((TextView) btnImpsView.getChildAt(1)).setTextColor(Color.parseColor("#CCE0FF"));

        ((TextView) btnNeftView.getChildAt(0)).setTextColor(Color.parseColor("#666666"));
        ((TextView) btnNeftView.getChildAt(1)).setTextColor(Color.parseColor("@color/poweredby"));
    }

    private void selectNeft() {
        rbNeft.setChecked(true);

        btnNeftView.setBackgroundResource(R.drawable.bg_transfer_selected);
        btnImpsView.setBackgroundColor(Color.TRANSPARENT);

        ((TextView) btnNeftView.getChildAt(0)).setTextColor(Color.WHITE);
        ((TextView) btnNeftView.getChildAt(1)).setTextColor(Color.parseColor("#CCE0FF"));

        ((TextView) btnImpsView.getChildAt(0)).setTextColor(Color.parseColor("#666666"));
        ((TextView) btnImpsView.getChildAt(1)).setTextColor(Color.parseColor("@color/poweredby"));
    }

    private void setIntentData() {
        Intent i = getIntent();

        beneId = i.getStringExtra("BENE_ID");

        String bank   = i.getStringExtra("BANK_NAME");
        String acc    = i.getStringExtra("ACC_NO");
        String ifsc   = i.getStringExtra("IFSC");
        String holder = i.getStringExtra("HOLDER_NAME");

        tvBank.setText(bank     != null ? bank   : "N/A");
        tvHolder.setText(holder != null ? holder : "N/A");
        tvIfsc.setText(ifsc     != null ? ifsc   : "N/A");

        if (acc != null && !acc.isEmpty()) {
            tvAcc.setText(acc);
        } else {
            tvAcc.setText("N/A");
        }
    }

    private void handlePayout() {
        String amount   = etAmount.getText().toString().trim();
        String password = etTxnPassword.getText().toString().trim();

        if (amount.isEmpty()) {
            showExitDialog("Error", "Please enter amount");
            return;
        }

        if (password.isEmpty()) {
            showExitDialog("Error", "Enter transaction password");
            return;
        }

        processTransaction(amount, password);
    }

    private void processTransaction(String amount, String password) {
        UiUtils.showLoading(this);
        btnSubmit.setEnabled(false);

        JSONObject params = new JSONObject();

        try {
            params.put("bene_id", beneId);
            params.put("amount", amount);
            params.put("transaction_password", password);
            params.put("mode",
                    (rgMode.getCheckedRadioButtonId() == R.id.rbImps) ? "imps" : "neft");

            if (layoutOtp.getVisibility() == View.VISIBLE) {
                params.put("otp", etOtp.getText().toString().trim());
            }

        } catch (Exception ignored) {}

        ApiClient.post(this, "payout/payout", TOKEN, params, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();
                btnSubmit.setEnabled(true);

                if (response == null) {
                    showExitDialog("Failed", "Server not responding");
                    return;
                }

                int status       = response.optInt("status");
                JSONObject data  = response.optJSONObject("data");

                if (status == 1 && data != null && data.optBoolean("otp_active", false)) {
                    layoutOtp.setVisibility(View.VISIBLE);
                    btnSubmit.setText("VERIFY & PAYOUT");
                    showExitDialog("OTP Required", response.optString("message"));
                    return;
                }

                if (data != null && data.has("id")) {
                    if (status == 1) playSuccessSound();

                    Intent intent = new Intent(DoPayoutActivity.this, PayoutReceiptActivity.class);
                    intent.putExtra("id", data.optString("id"));
                    startActivity(intent);
                    finish();

                } else {
                    String message = response.optString("message", "Transaction failed");
                    showExitDialog("Error", message);
                }
            });

        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            btnSubmit.setEnabled(true);
            showExitDialog("Error", "Connection Failed");
        }));
    }

    private void showExitDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .show();
    }

    private void playSuccessSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.success_sound);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}