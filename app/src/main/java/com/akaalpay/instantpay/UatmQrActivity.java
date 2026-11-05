package com.akaalpay.instantpay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.report.UpiAtmReceiptActivity;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated QR display page. Everything that used to happen inline on
 * the form (countdown, 5s polling, manual check-status, the final
 * receipt hand-off) now lives here, on its own clean screen.
 */
public class UatmQrActivity extends BaseActivity {

    private ImageView btnBack, imgQr;
    private TextView tvAmount, tvMobile, tvTimer, tvWaiting;
    private MaterialButton btnCheckStatus;
    private LinearLayout layoutWaiting;
    private CircularTimerView timerProgress;

    private View overlaySupportedApps;
    private ImageView imgFullScreen;
    private View progressFullScreen;
    private TextView tvViewAllApps;
    private TextView btnCloseFullImage;

    private static final String SUPPORTED_APPS_URL =
            "https://akaalpay.com/assets/images/upisupported.png";

    private String TOKEN = "";
    private String txnId = "";
    private String ipayId = "";

    private Handler pollHandler;
    private Runnable pollRunnable;
    private android.os.CountDownTimer countDownTimer;

    private boolean isTerminalStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uatm_qr);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        btnBack.setOnClickListener(v -> finish());
        btnCheckStatus.setOnClickListener(v -> checkFinalStatus());

        tvViewAllApps.setOnClickListener(v -> openSupportedAppsOverlay());
        btnCloseFullImage.setOnClickListener(v -> closeSupportedAppsOverlay());

        txnId = getIntent().getStringExtra("txn_id");
        ipayId = getIntent().getStringExtra("ipay_id");

        String qrString = getIntent().getStringExtra("qr_string");
        String mobile = getIntent().getStringExtra("mobile");
        String amountVal = getIntent().getStringExtra("amount");
        int expirySeconds = getIntent().getIntExtra("expiry_sec", 30);

        tvAmount.setText("₹ " + amountVal);
        tvMobile.setText(mobile);

        generateQrImage(qrString);

        startTimer(expirySeconds);
        startPolling();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgQr = findViewById(R.id.imgQr);
        tvAmount = findViewById(R.id.tvAmount);
        tvMobile = findViewById(R.id.tvMobile);
        tvTimer = findViewById(R.id.tvTimer);
        tvWaiting = findViewById(R.id.tvWaiting);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        layoutWaiting = findViewById(R.id.layoutWaiting);
        timerProgress = findViewById(R.id.timerProgress);

        overlaySupportedApps = findViewById(R.id.overlaySupportedApps);
        imgFullScreen = findViewById(R.id.imgFullScreen);
        progressFullScreen = findViewById(R.id.progressFullScreen);
        tvViewAllApps = findViewById(R.id.tvViewAllApps);
        btnCloseFullImage = findViewById(R.id.btnCloseFullImage);
    }

    // ==================== SUPPORTED APPS OVERLAY ====================

    private void openSupportedAppsOverlay() {

        overlaySupportedApps.setVisibility(View.VISIBLE);
        progressFullScreen.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(SUPPORTED_APPS_URL)
                .listener(new RequestListener<Drawable>() {

                    @Override
                    public boolean onLoadFailed(
                            GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource
                    ) {
                        progressFullScreen.setVisibility(View.GONE);
                        UiUtils.showToast(
                                UatmQrActivity.this,
                                "Unable to load image",
                                false
                        );
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        progressFullScreen.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imgFullScreen);
    }

    private void closeSupportedAppsOverlay() {
        overlaySupportedApps.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (overlaySupportedApps.getVisibility() == View.VISIBLE) {
            closeSupportedAppsOverlay();
        } else {
            super.onBackPressed();
        }
    }

    // ==================== QR GENERATION (with centre logo) ====================

    private void generateQrImage(String qrString) {
        try {
            // High error-correction so covering ~20-25% of the QR with a
            // logo in the middle still leaves it fully scannable.
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrString, BarcodeFormat.QR_CODE, 800, 800, hints);

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.createBitmap(matrix);

            // NOTE: replace R.drawable.ic_app_logo with your actual
            // company logo drawable name.
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_slogo);

            Bitmap finalBitmap = (logo != null) ? overlayLogo(qrBitmap, logo) : qrBitmap;

            imgQr.setImageBitmap(finalBitmap);

        } catch (Exception e) {
            showErrorDialog("Unable To Generate QR");
        }
    }

    /**
     * Draws a small white rounded "badge" behind the logo (so it stays
     * crisp regardless of what QR pixels are underneath) and centres
     * the logo on top of it.
     */
    private Bitmap overlayLogo(Bitmap qrBitmap, Bitmap logo) {

        int qrSize = qrBitmap.getWidth();
        int logoSize = qrSize / 5; // ~20% of the QR - safe with H-level correction

        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true);

        Bitmap combined = qrBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(combined);

        int left = (qrSize - logoSize) / 2;
        int top = (qrSize - logoSize) / 2;
        int pad = 10;

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);

        canvas.drawRoundRect(
                new RectF(left - pad, top - pad, left + logoSize + pad, top + logoSize + pad),
                14, 14, bgPaint
        );

        canvas.drawBitmap(scaledLogo, left, top, null);

        return combined;
    }

    // ==================== TIMER ====================

    private void startTimer(int totalSeconds) {
        if (countDownTimer != null) countDownTimer.cancel();

        final int duration = Math.max(1, totalSeconds);
        timerProgress.setMax(duration);
        timerProgress.setProgress(duration);
        tvTimer.setText(String.valueOf(duration));
        applyTimerColor(duration, duration);

        countDownTimer = new android.os.CountDownTimer(duration * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText(String.valueOf(secondsLeft));
                timerProgress.setProgress(secondsLeft);
                applyTimerColor(secondsLeft, duration);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0");
                timerProgress.setProgress(0);
                applyTimerColor(0, duration);
                if (!isTerminalStatus) {
                    layoutWaiting.setVisibility(View.VISIBLE);
                    btnCheckStatus.setVisibility(View.VISIBLE);
                }
            }
        };
        countDownTimer.start();
    }

    private void applyTimerColor(int secondsLeft, int totalSeconds) {
        float fraction = totalSeconds > 0 ? (float) secondsLeft / totalSeconds : 0f;
        int color;
        if (fraction > 0.66f) color = Color.parseColor("#2E7D32");
        else if (fraction > 0.33f) color = Color.parseColor("#F9A825");
        else color = Color.parseColor("#E53935");
        timerProgress.setRingColor(color);
        tvTimer.setTextColor(color);
    }

    // ==================== POLLING ====================

    private void startPolling() {
        stopPolling();
        pollHandler = new Handler();
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                callPollStatus();
                pollHandler.postDelayed(this, 5000);
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void callPollStatus() {
        JSONObject p = new JSONObject();
        try {
            p.put("txn_id", txnId);
        } catch (Exception ignored) {
        }

        ApiClient.post(this, "ip-uatm/poll-status", TOKEN, p,
                response -> {
                    try {
                        JSONObject data = response.optJSONObject("data");
                        String qrStatus = (data != null) ? data.optString("qr_status", "") : "";

                        if (qrStatus.equalsIgnoreCase("SUCCESS")) {
                            runOnUiThread(() -> {
                                isTerminalStatus = true;
                                stopPolling();
                                if (countDownTimer != null) countDownTimer.cancel();
                                openReceipt();
                            });
                        } else if (!qrStatus.equalsIgnoreCase("INITIATED")) {
                            runOnUiThread(() -> {
                                isTerminalStatus = true;
                                stopPolling();
                                if (countDownTimer != null) countDownTimer.cancel();
                                layoutWaiting.setVisibility(View.GONE);
                                showExpiredDialog(response.optString("message", "Transaction Failed"));
                            });
                        }
                    } catch (Exception ignored) {
                    }
                },
                error -> {
                }
        );
    }

    // ==================== MANUAL CHECK ====================

    private void checkFinalStatus() {
        UiUtils.showLoading(this);
        JSONObject p = new JSONObject();
        try {
            p.put("ipay_id", ipayId);
            p.put("txn_id", txnId);
        } catch (Exception ignored) {
        }

        ApiClient.post(this, "ip-uatm/check-status", TOKEN, p,
                response -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        JSONObject data = response.optJSONObject("data");
                        String qrStatus = (data != null) ? data.optString("qr_status", "") : "";

                        if (qrStatus.equalsIgnoreCase("SUCCESS")) {
                            isTerminalStatus = true;
                            stopPolling();
                            openReceipt();
                        } else if (qrStatus.equalsIgnoreCase("INITIATED")) {
                            showErrorDialog(response.optString("message", "Please Wait..."));
                        } else {
                            isTerminalStatus = true;
                            stopPolling();
                            layoutWaiting.setVisibility(View.GONE);
                            showExpiredDialog(response.optString("message", "Transaction Failed"));
                        }
                    } catch (Exception e) {
                        showErrorDialog("Status Check Failed");
                    }
                }),
                error -> runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    showErrorDialog("Network Error");
                })
        );
    }

    // ==================== NAVIGATION ====================

    private void openReceipt() {
        Intent intent = new Intent(UatmQrActivity.this, UpiAtmReceiptActivity.class);
        intent.putExtra("id", txnId);
        startActivity(intent);
        finish();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("UPI ATM")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showExpiredDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("UPI ATM")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}