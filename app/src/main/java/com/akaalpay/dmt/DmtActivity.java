package com.akaalpay.dmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import org.json.JSONObject;

public class DmtActivity extends AppCompatActivity {
    private String TOKEN = "";
    private EditText etMobile;
    private Button btnSearch;
    private String mobile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmt);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);

        etMobile = findViewById(R.id.etMobile);
        btnSearch = findViewById(R.id.btnSearch);

        SharedPreferences pref = getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        btnSearch.setOnClickListener(v -> searchRemitter());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void searchRemitter() {
        mobile = etMobile.getText().toString().trim();
        if (mobile.length() != 10) {
            showDialogBox("Warning", "Enter valid 10 digit mobile number");
            return;
        }

        UiUtils.showLoading(this);
        JSONObject body = new JSONObject();
        try { body.put("mobile", mobile); } catch (Exception e) { e.printStackTrace(); }

        ApiClient.post(this, "dmt/query-remitter", TOKEN, body, response -> {
            runOnUiThread(() -> {
                UiUtils.dismissLoading();
                handleResponse(response);
            });
        }, error -> runOnUiThread(() -> {
            UiUtils.dismissLoading();
            showDialogBox("Error", "Server connection failed");
        }));
    }

    private void handleResponse(JSONObject response) {
        try {
            if (response == null || response.optInt("status") != 1) {
                showDialogBox("Failed", response != null ? response.optString("message") : "Error");
                return;
            }

            JSONObject data = response.getJSONObject("data");
            String statusType = data.optString("status_type");

            if ("FOUND".equalsIgnoreCase(statusType)) {
                // ✅ FOUND: Naya Premium Page Khulega
                Intent intent = new Intent(DmtActivity.this, DmtDashboardActivity.class);
                intent.putExtra("remitter_json", data.toString());
                intent.putExtra("mobile", mobile);
                startActivity(intent);
            } else {
                // ✅ NOT FOUND: Register Page Khulega
                Intent i = new Intent(DmtActivity.this, DmtRegisterActivity.class);
                i.putExtra("mobile", mobile);
                i.putExtra("referenceKey", data.optString("reference_key"));
                i.putExtra("wadh", data.optString("pidOptionWadh"));
                startActivity(i);
            }
        } catch (Exception e) {
            showDialogBox("Error", "Data parsing error");
        }
    }

    private void showDialogBox(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}