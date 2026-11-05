package com.silverpe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.silverpe.utils.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import okhttp3.*;
import okhttp3.Response;

import com.google.android.material.textfield.TextInputLayout;


public class LoginActivity extends AppCompatActivity {

    private EditText etMobile, etPassword;
    private Button btnContinue, btnLogin;
    private TextInputLayout passwordContainer;
    private LinearLayout errorBox, btnCall, btnEmail;
    private TextView tvError, tvForgot;
   // private ImageView imgYoutube, imgFb, imgInsta, imgwhatapp;
    private String userId = "";
    private boolean isPasswordVisible = false;
    private TextView tvRegister;

    private BottomSheetDialog otpDialog;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(
                ContextCompat.getColor(this, R.color.feno_secondary_light)
        );
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );

        controller.show(WindowInsetsCompat.Type.statusBars());
        controller.setAppearanceLightStatusBars(true);
        initViews();

        // Auto check mobile on 10 digits
        etMobile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.length() == 10) checkMobile();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        btnContinue.setOnClickListener(v -> checkMobile());
        btnLogin.setOnClickListener(v -> requestOtp());
//        btnShowHide.setOnClickListener(v -> togglePasswordVisibility());
        tvRegister.setOnClickListener(v -> {
            openUrlInChrome("https://silverpe.com/app/register");
        });
        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        setupContactActions();
        setupSocialLinks();
    }

    private void initViews() {
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        btnContinue = findViewById(R.id.btnContinue);
        btnLogin = findViewById(R.id.btnLogin);
//        btnShowHide = findViewById(R.id.btnShowHide);
        passwordContainer = findViewById(R.id.passwordContainer);
        errorBox = findViewById(R.id.errorBox);
        tvError = findViewById(R.id.tvError);
        tvForgot = findViewById(R.id.tvForgot);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
//        imgYoutube = findViewById(R.id.imgYoutube);
//        imgFb = findViewById(R.id.imgFb);
//        imgInsta = findViewById(R.id.imgInsta);
//        imgwhatapp = findViewById(R.id.imgwhatapp);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void checkMobile() {
        String input = etMobile.getText().toString().trim();

        if (input.isEmpty()) {
            showError("Enter Mobile Number or User ID");
            return;
        }

        errorBox.setVisibility(View.GONE);

        // ✅ Loader START
        UiUtils.showLoading(LoginActivity.this);

        // ✅ Case 1: Mobile Number
        if (input.matches("\\d{10}")) {

            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();

                    RequestBody body = new FormBody.Builder()
                            .add("mobile", input)
                            .build();

                    Request req = new Request.Builder()
                            .url("https://silverpe.com/app/index.php/api/v1/login/check-mobile")
                            .post(body)
                            .build();

                    Response res = client.newCall(req).execute();
                    JSONObject json = new JSONObject(res.body().string());

                    runOnUiThread(() -> {

                        // ✅ Loader STOP
                        UiUtils.dismissLoading();

                        if (json.optInt("status") == 1) {

                            userId = json.optJSONObject("data").optString("user_id");

                            etMobile.setEnabled(false);
                            btnContinue.setVisibility(View.GONE);
                            passwordContainer.setVisibility(View.VISIBLE);
                            btnLogin.setVisibility(View.VISIBLE);
                            tvForgot.setVisibility(View.VISIBLE);

                        } else {
                            showError(json.optString("message"));
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        // ✅ Loader STOP on error
                        UiUtils.dismissLoading();
                        showError("Network Error");
                    });
                }
            }).start();

        } else {

            // ✅ Loader STOP (kyunki API call nahi hai)
            UiUtils.dismissLoading();

            userId = input;

            etMobile.setEnabled(false);
            btnContinue.setVisibility(View.GONE);
            passwordContainer.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
            tvForgot.setVisibility(View.VISIBLE);
        }
    }

    private void requestOtp() {

        String password = etPassword.getText().toString().trim();

        if (password.isEmpty()) {
            showError("Enter Password");
            return;
        }

        // ✅ Loader START
        UiUtils.showLoading(LoginActivity.this);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                RequestBody body = new FormBody.Builder()
                        .add("username", userId)
                        .add("password", password)
                        .build();

                Request req = new Request.Builder()
                        .url("https://silverpe.com/app/index.php/api/v1/login/request-otp")
                        .post(body)
                        .build();

                Response res =  client.newCall(req).execute();
                JSONObject json = new JSONObject(res.body().string());

                runOnUiThread(() -> {

                    // ✅ Loader STOP
                    UiUtils.dismissLoading();

                    if (json.optInt("status") == 1) {

                        String serverMessage = json.optString("message");
                        showOtpSheet(password, serverMessage);

                    } else {
                        showError(json.optString("message"));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {

                    // ✅ Loader STOP on error
                    UiUtils.dismissLoading();

                    showError("Network Error");
                });
            }
        }).start();
    }

    private void showOtpSheet(final String password, String messageFromServer) {
        otpDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_otp_bottom_sheet, null);
        otpDialog.setContentView(view);


        ((View) view.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);


        TextView tvOtpMessage = view.findViewById(R.id.tvOtpMessage);
        TextView tvResend = view.findViewById(R.id.tvResend);
        TextView tvTimer = view.findViewById(R.id.tvTimer);


        tvTimer.setVisibility(View.GONE);


        EditText[] otps = {
                view.findViewById(R.id.otp1), view.findViewById(R.id.otp2), view.findViewById(R.id.otp3),
                view.findViewById(R.id.otp4), view.findViewById(R.id.otp5), view.findViewById(R.id.otp6)
        };

        setupAutoMove(otps);

        // --- Keyboard aur Focus Logic ---
        otps[0].requestFocus();
        otpDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // --- Timer Start (5 min valid message + 120s resend lock) ---
        // Note: messageFromServer wo hai jo API se aaya jisme masked number hai
        startOtpTimerInsideMessage(tvOtpMessage, tvResend, messageFromServer);

        // Verify Button Click
        view.findViewById(R.id.btnVerify).setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (EditText e : otps) sb.append(e.getText().toString());
            if (sb.length() == 6) {
                verifyOtp(sb.toString(), password);
            } else {
                Toast.makeText(this, "Please enter 6 digit OTP", Toast.LENGTH_SHORT).show();
            }
        });

        // Resend Button Click
        tvResend.setOnClickListener(v -> {
            // Naya OTP request karein
            requestOtp(); // Ye function wapas request bhejega aur timer restart karega
            otpDialog.dismiss(); // Purana dialog band karke naya message ke sath khulega
        });

        otpDialog.show();
    }

    private void startOtpTimerInsideMessage(TextView tvOtpMessage, TextView tvResend, String baseMsg) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        tvResend.setEnabled(false);
        tvResend.setTextColor(android.graphics.Color.parseColor("#9CA3AF")); // Grey color

        // 300,000 ms = 5 Minutes total validity
        countDownTimer = new CountDownTimer(300000, 1000) {
            public void onTick(long ms) {
                long totalSec = ms / 1000;
                String timeStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);

                // 1. Message ke andar timer update karein
                // Agar server se aane wale message mein "valid for..." pehle se hai, toh hum use replace kar rahe hain
                tvOtpMessage.setText(baseMsg + " Valid for " + timeStr + " minutes.");

                // 2. Resend Button Logic (120 seconds wait)
                long elapsedSec = 300 - totalSec; // Kitne seconds beet gaye
                if (elapsedSec < 120) {
                    long remainingWait = 120 - elapsedSec;
                    tvResend.setText("Resend OTP (" + remainingWait + "s)");
                    tvResend.setEnabled(false);
                } else {
                    if (!tvResend.isEnabled()) {
                        tvResend.setEnabled(true);
                        tvResend.setTextColor(android.graphics.Color.parseColor("#2563EB")); // Blue color
                        tvResend.setText("Resend OTP");
                    }
                }
            }

            public void onFinish() {
                tvOtpMessage.setText(baseMsg + " OTP Expired.");
                tvResend.setEnabled(true);
                tvResend.setTextColor(android.graphics.Color.parseColor("#2563EB"));
                tvResend.setText("Resend OTP");
            }
        }.start();
    }

    private void verifyOtp(String otp, String password) {

        if (otp == null || otp.length() != 6) {
            Toast.makeText(this, "Enter valid 6 digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        UiUtils.showLoading(LoginActivity.this);

        getLiveLocationAndLogin(otp, password);
    }

    private void getLiveLocationAndLogin(String otp, String password) {
        final android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);

        // 1. Permission Check
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            UiUtils.dismissLoading();
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        // 2. Flag to prevent multiple calls
        final boolean[] isProcessed = {false};

        // 3. Define the Listener first (so we can remove it specifically)
        final android.location.LocationListener locationListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location loc) {
                if (!isProcessed[0]) {
                    isProcessed[0] = true;
                    locationManager.removeUpdates(this); // specific listener remove
                    loginWithOtp(otp, password, String.valueOf(loc.getLatitude()), String.valueOf(loc.getLongitude()));
                }
            }

            @Override
            public void onStatusChanged(String p, int s, Bundle e) {
            }

            @Override
            public void onProviderEnabled(String p) {
            }

            @Override
            public void onProviderDisabled(String p) {
            }
        };

        // 4. TIMEOUT LOGIC (10 Seconds)
        final android.os.Handler handler = new android.os.Handler();
        final Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isProcessed[0]) {
                    isProcessed[0] = true;
                    // FIX: Pass the specific listener instead of null to avoid ambiguity error
                    locationManager.removeUpdates(locationListener);

                    // Location nahi mili, toh 0.0 bhej kar login aage badhao
                    loginWithOtp(otp, password, "0.0", "0.0");
                }
            }
        };
        handler.postDelayed(timeoutRunnable, 10000); // 10 Sec Timeout

        // 5. Start Requesting
        try {
            boolean isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }

            // Agar dono band hain, toh turant 10 sec ka wait kare bina 0.0 bhej do
            if (!isGpsEnabled && !isNetworkEnabled) {
                if (!isProcessed[0]) {
                    isProcessed[0] = true;
                    handler.removeCallbacks(timeoutRunnable);
                    loginWithOtp(otp, password, "0.0", "0.0");
                }
            }

        } catch (Exception e) {
            if (!isProcessed[0]) {
                isProcessed[0] = true;
                handler.removeCallbacks(timeoutRunnable);
                locationManager.removeUpdates(locationListener);
                loginWithOtp(otp, password, "0.0", "0.0");
            }
        }
    }

    /* ============================= */
    /* OTP LOGIN API */
    /* ============================= */

    private void loginWithOtp(String otp,
                              String password,
                              String latitude,
                              String longitude) {

        new Thread(() -> {
            try {

                OkHttpClient client = new OkHttpClient();

                RequestBody body = new FormBody.Builder()
                        .add("username", userId)
                        .add("password", password)
                        .add("otp", otp)
                        .add("latitude", latitude)
                        .add("longitude", longitude)
                        .build();

                Request req = new Request.Builder()
                        .url("https://silverpe.com/app/index.php/api/v1/login/validate-otp")
                        .post(body)
                        .build();

                Response res = client.newCall(req).execute();

                JSONObject json =
                        new JSONObject(res.body().string());

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    if (json.optInt("status") == 1) {

                        String token =
                                json.optJSONObject("data")
                                        .optString("token");

                        getSharedPreferences("BP_PREF", MODE_PRIVATE)
                                .edit()
                                .putString("TOKEN", token)
                                .apply();

                        if (otpDialog != null &&
                                otpDialog.isShowing()) {
                            otpDialog.dismiss();
                        }

                        Toast.makeText(LoginActivity.this,
                                "Login Success",
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(
                                LoginActivity.this,
                                SplashDecisionActivity.class));

                        finish();

                    } else {

                        Toast.makeText(LoginActivity.this,
                                json.optString("message"),
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    Toast.makeText(LoginActivity.this,
                            "Network Error",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupAutoMove(EditText[] otps) {

        for (int i = 0; i < otps.length; i++) {

            final int current = i;

            // 👉 Forward Move
            otps[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && current < otps.length - 1) {
                        otps[current + 1].requestFocus();
                    }
                }
            });

            // 👉 Backspace Move (IMPORTANT 🔥)
            otps[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                        keyCode == android.view.KeyEvent.KEYCODE_DEL) {

                    if (otps[current].getText().toString().isEmpty() && current > 0) {
                        otps[current - 1].requestFocus();
                        otps[current - 1].setSelection(otps[current - 1].getText().length());
                    }
                }
                return false;
            });
        }
    }

    private void setupContactActions() {
        btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919386468815"))));
        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@bharatdigicash.com"));
            startActivity(Intent.createChooser(intent, "Email via..."));
        });

    }

    private void setupSocialLinks() {

        // YouTube
//        imgYoutube.setOnClickListener(v -> openUrl("https://www.youtube.com/@bharatdigicash"));
//
//        // Facebook
//        imgFb.setOnClickListener(v -> openUrl("https://www.facebook.com/bharatdigicash/"));
//
//        // Instagram
//        imgInsta.setOnClickListener(v -> openUrl("https://www.instagram.com/bharatdigicash/"));
//
//        // Whatapp
//        imgwhatapp.setOnClickListener(v -> openUrl("https://whatsapp.com/channel/0029VaA6iCtFsn0lwBr2g12l"));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrlInChrome(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.android.chrome"); // Force Chrome
            startActivity(intent);
        } catch (Exception e) {
            // Agar Chrome install nahi hai → normal browser open
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        errorBox.setVisibility(View.VISIBLE);
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible)
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else etPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        etPassword.setSelection(etPassword.length());
        isPasswordVisible = !isPasswordVisible;
    }
}