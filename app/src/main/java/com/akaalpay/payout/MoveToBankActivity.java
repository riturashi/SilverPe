package com.akaalpay.payout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MoveToBankActivity extends AppCompatActivity {

    private RecyclerView rv;
    private List<BankAdapter.BankItem> bankList = new ArrayList<>();
    private ExtendedFloatingActionButton btnAddBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_to_bank);
        StatusBarUtils.setupStatusBar(this, R.id.topBar);
        rv = findViewById(R.id.rvBankList);
        btnAddBank = findViewById(R.id.btnAddBank);

        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnAddBank.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddBankAccountActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {

        UiUtils.showLoading(this);

        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        ApiClient.get(this, "payout/my-banks", token, response -> {

            runOnUiThread(() -> {

                UiUtils.dismissLoading();

                try {

                    if (response.optInt("status") == 1) {

                        JSONArray arr = response.optJSONArray("data");

                        bankList.clear();

                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {

                                JSONObject obj = arr.getJSONObject(i);

                                bankList.add(new BankAdapter.BankItem(
                                        obj.optString("bank_name"),
                                        obj.optInt("bank_id"),
                                        obj.optString("bene_id"),
                                        obj.optString("account_no"),
                                        obj.optString("ifsc"),
                                        obj.optString("holder_name"),
                                        obj.optInt("validation_status")
                                ));
                            }
                        }

                        rv.setAdapter(new BankAdapter(bankList, item -> {

                            Intent intent = new Intent(this, DoPayoutActivity.class);
                            intent.putExtra("BENE_ID", item.bene_id);
                            intent.putExtra("BANK_NAME", item.bank_name);
                            intent.putExtra("ACC_NO", item.account_no);
                            intent.putExtra("HOLDER_NAME", item.holder_name);
                            intent.putExtra("IFSC", item.ifsc);
                            startActivity(intent);
                        }));

                    } else {

                        UiUtils.showToast(this, response.optString("message"), false);
                    }

                } catch (Exception e) {
                    UiUtils.showToast(this, "Parsing Error", false);
                }

            });

        }, error -> runOnUiThread(() -> {

            UiUtils.dismissLoading();
            UiUtils.showToast(this, "Server not responding", false);

        }));
    }
}