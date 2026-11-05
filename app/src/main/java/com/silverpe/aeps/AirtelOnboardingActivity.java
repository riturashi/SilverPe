package com.silverpe.aeps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.silverpe.utils.UiUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class AirtelOnboardingActivity extends AppCompatActivity {

    private TextInputEditText etMobile, etTxnPass;
    private CheckBox cbConsent;
    private MaterialButton btnSubmit;
    private ImageView btnBack;

    private FusedLocationProviderClient fusedLocationClient;
    private String latitude = "0.0", longitude = "0.0";
    private String TOKEN = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airtel_onboarding);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        // SharedPreferences se token nikalna
        TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");

        initViews();
        setupLocation();

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                hitOnboardingApi();
            }
        });
    }

    private void initViews() {
        etMobile = findViewById(R.id.etOnbMobile);
        etTxnPass = findViewById(R.id.etOnbTxnPass);
        cbConsent = findViewById(R.id.cbConsent);
        btnSubmit = findViewById(R.id.btnOnbSubmit);
        btnBack = findViewById(R.id.btnBack);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = String.valueOf(location.getLatitude());
                longitude = String.valueOf(location.getLongitude());
            }
        });
    }

    private boolean validateInputs() {
        String mob = etMobile.getText().toString().trim();
        String pass = etTxnPass.getText().toString().trim();

        if (mob.length() != 10) {
            UiUtils.showToast(this, "Enter valid 10-digit mobile number", false);
            return false;
        }
        if (pass.isEmpty()) {
            UiUtils.showToast(this, "Enter Transaction Password", false);
            return false;
        }
        if (!cbConsent.isChecked()) {
            UiUtils.showToast(this, "Please accept consent to proceed", false);
            return false;
        }
        return true;
    }

    private void hitOnboardingApi() {
        // 🔄 Loader dikhao (UiUtils se)
        UiUtils.showLoading(this);

        String mob = etMobile.getText().toString().trim();
        String pass = etTxnPass.getText().toString().trim();
        String consentValue = cbConsent.isChecked() ? "1" : "0";

        JSONObject body = new JSONObject();
        try {
            body.put("mobile", mob);
            body.put("transaction_password", pass);
            body.put("consent", consentValue);
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.post(this, "ip-onboarding/start", TOKEN, body, response -> {
            runOnUiThread(() -> {
                // 🛑 Loader hatao
                UiUtils.dismissLoading();

                int status = response.optInt("status");
                String message = response.optString("message");
                JSONObject data = response.optJSONObject("data");

                if (status == 1 && data != null && data.optInt("response_code") == 1) {
                    // ✅ SUCCESS
                    UiUtils.showToast(this, message, true);

                    // Redirect to eKYC Page
                    startActivity(new Intent(this, AirtelEkycActivity.class));
                    finish();
                } else {
                    // ❌ FAILED (UIDAI Down or Incorrect Password)
                    UiUtils.showToast(this, message, false);
                }
            });
        }, error -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                UiUtils.showToast(this, "Network error, please try again", false);
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocation();
        }
    }
}