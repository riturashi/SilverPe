package com.akaalpay.report;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.app.DatePickerDialog;
import android.widget.TextView;

import com.akaalpay.utils.StatusBarUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Calendar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.UiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class FundRequestReportActivity extends BaseActivity {

    RecyclerView recyclerReport;
    FundRequestReportAdapter adapter;
    ArrayList<FundRequestReportModel> reportList = new ArrayList<>();

    String fromDate = "", toDate = "", status = "";
    int page = 1, totalPages = 1;
    boolean isLoading = false;
    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_request_report);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        recyclerReport = findViewById(R.id.recyclerReport);
        layoutManager = new LinearLayoutManager(this);
        recyclerReport.setLayoutManager(layoutManager);

        adapter = new FundRequestReportAdapter(this, reportList);
        recyclerReport.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilter).setOnClickListener(v -> openFilter());

        recyclerReport.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                int visible   = layoutManager.getChildCount();
                int total     = layoutManager.getItemCount();
                int firstPos  = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && page < totalPages) {
                    if ((visible + firstPos) >= total && firstPos >= 0) {
                        page++;
                        loadReport();
                    }
                } else if (!isLoading && page >= totalPages) {
                    UiUtils.showToast(FundRequestReportActivity.this, "No More Records", false);
                }
            }
        });

        loadReport();
    }

    private void loadReport() {
        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");
        isLoading = true;
        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("page_no", page);
            body.put("fromDate", fromDate);
            body.put("toDate", toDate);
            body.put("status", status);

            ApiClient.post(this, "fund-request/all-requests", TOKEN, body, response -> {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        int apiStatus = response.optInt("status");
                        if (apiStatus == 0) {
                            page--;
                            isLoading = false;
                            UiUtils.showToast(FundRequestReportActivity.this, "No Record Found", false);
                            return;
                        }

                        JSONObject data = response.getJSONObject("data");
                        totalPages = data.optInt("totalPages");
                        JSONArray list = data.optJSONArray("list");

                        if (list == null || list.length() == 0) {
                            isLoading = false;
                            UiUtils.showToast(FundRequestReportActivity.this, "No Record Found", false);
                            return;
                        }

                        if (page == 1) reportList.clear();

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            FundRequestReportModel model = new FundRequestReportModel();
                            model.requestId    = obj.optString("request_id");
                            model.bankName     = obj.optString("bank_name");
                            model.accountNo    = obj.optString("account_no");
                            model.accountHolder= obj.optString("account_holder");
                            model.ifscCode     = obj.optString("ifsc_code");
                            model.depositDate  = obj.optString("deposit_date");
                            model.paymentMode  = obj.optString("payment_mode");
                            model.amount       = obj.optString("amount");
                            model.receiptNo    = obj.optString("receipt_no");
//                            model.receiptImage = obj.isNull("receipt_image") ? "" : obj.optString("receipt_image");
                            model.remark       = obj.optString("remark");
                            model.adminRemark  = obj.optString("admin_remark");
                            model.status       = obj.optInt("status");
                            model.date         = obj.optString("date");
                            reportList.add(model);
                        }

                        adapter.notifyDataSetChanged();
                        isLoading = false;

                    } catch (Exception e) {
                        isLoading = false;
                        UiUtils.showToast(FundRequestReportActivity.this, "Parsing Error", false);
                    }
                });
            }, error -> runOnUiThread(() -> {
                UiUtils.dismissLoading();
                isLoading = false;
                UiUtils.showToast(FundRequestReportActivity.this, "Server not responding", false);
            }));

        } catch (Exception e) {
            UiUtils.dismissLoading();
            isLoading = false;
            UiUtils.showToast(this, "Request Error", false);
        }
    }

    private void openFilter() {

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        bottomSheet.setContentView(R.layout.filter_aeps_report);

        TextView tvFromDate = bottomSheet.findViewById(R.id.etFromDate);
        TextView tvToDate   = bottomSheet.findViewById(R.id.etToDate);
        Spinner  spStatus   = bottomSheet.findViewById(R.id.spStatus);
        Button   btnApply   = bottomSheet.findViewById(R.id.btnApply);
        Button   btnReset   = bottomSheet.findViewById(R.id.btnReset);

        if (!fromDate.isEmpty()) tvFromDate.setText(fromDate);
        if (!toDate.isEmpty())   tvToDate.setText(toDate);

        // Status — All, Approved, Pending, Rejected
        String[] statusList = {"All", "Approved", "Pending", "Rejected"};
        spStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statusList));

        bottomSheet.findViewById(R.id.layoutFromDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = year + "-" + String.format("%02d", month + 1)
                        + "-" + String.format("%02d", day);
                tvFromDate.setText(date);
                tvFromDate.setTextColor(getResources().getColor(android.R.color.black, null));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        bottomSheet.findViewById(R.id.layoutToDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = year + "-" + String.format("%02d", month + 1)
                        + "-" + String.format("%02d", day);
                tvToDate.setText(date);
                tvToDate.setTextColor(getResources().getColor(android.R.color.black, null));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnReset.setOnClickListener(v -> {
            fromDate = ""; toDate = ""; status = "";
            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            fromDate = tvFromDate.getText().toString().equals("Select date") ? ""
                    : tvFromDate.getText().toString();
            toDate   = tvToDate.getText().toString().equals("Select date") ? ""
                    : tvToDate.getText().toString();

            String st = spStatus.getSelectedItem().toString();
            switch (st) {
                case "Approved": status = "1"; break;
                case "Rejected": status = "2"; break;
                case "Pending":  status = "0"; break;
                default:         status = "";  break;
            }

            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }
}