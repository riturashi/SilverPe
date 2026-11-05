package com.akaalpay.report;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class LeadReportActivity extends BaseActivity {

    RecyclerView recyclerReport;
    LeadReportAdapter adapter;
    ArrayList<LeadReportModel> reportList = new ArrayList<>();

    String fromDate = "";
    String toDate   = "";
    String status   = "";
    String product  = "";

    int page       = 1;
    int totalPages = 1;
    boolean isLoading = false;

    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lead_report);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        recyclerReport = findViewById(R.id.recyclerReport);
        layoutManager  = new LinearLayoutManager(this);
        recyclerReport.setLayoutManager(layoutManager);

        adapter = new LeadReportAdapter(this, reportList);
        recyclerReport.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilter).setOnClickListener(v -> openFilter());

        // Pagination scroll
        recyclerReport.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visible  = layoutManager.getChildCount();
                int total    = layoutManager.getItemCount();
                int firstPos = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && page < totalPages) {
                    if ((visible + firstPos) >= total && firstPos >= 0) {
                        page++;
                        loadReport();
                    }
                } else if (!isLoading && page >= totalPages && total > 0) {
                    UiUtils.showToast(LeadReportActivity.this, "No more records", false);
                }
            }
        });

        loadReport();
    }

    // ─────────────────────────────────────────
    private void loadReport() {
        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE)
                .getString("TOKEN", "");

        isLoading = true;
        UiUtils.showLoading(this);

        try {
            JSONObject body = new JSONObject();
            body.put("page_no",  page);
            body.put("fromDate", fromDate);
            body.put("toDate",   toDate);
            body.put("status",   status);
            body.put("product",  product);

            ApiClient.post(this, "lead-generation/list", TOKEN, body, response -> {
                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    try {
                        int apiStatus = response.optInt("status");
                        if (apiStatus == 0) {
                            page--;
                            isLoading = false;
                            UiUtils.showToast(LeadReportActivity.this, "No records found", false);
                            return;
                        }

                        JSONObject data = response.getJSONObject("data");
                        totalPages = data.optInt("totalPages", 1);

                        JSONArray list = data.optJSONArray("list");
                        if (list == null || list.length() == 0) {
                            isLoading = false;
                            UiUtils.showToast(LeadReportActivity.this, "No records found", false);
                            return;
                        }

                        if (page == 1) reportList.clear();

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            LeadReportModel model  = new LeadReportModel();
                            model.id               = obj.optString("id");
                            model.ref_id           = obj.optString("ref_id");
                            model.name             = obj.optString("name");
                            model.mobile_no        = obj.optString("mobile_no");
                            model.email            = obj.optString("email");
                            model.product          = obj.optString("product");
                            model.executive_status = obj.optString("executive_status");
                            model.remarks          = obj.optString("remarks");
                            model.message          = obj.optString("message");
                            model.status           = obj.optInt("status");
                            model.date             = obj.optString("date");
                            reportList.add(model);
                        }

                        adapter.notifyDataSetChanged();
                        isLoading = false;

                    } catch (Exception e) {
                        isLoading = false;
                        UiUtils.showToast(LeadReportActivity.this, "Parsing error", false);
                    }
                });
            }, error -> runOnUiThread(() -> {
                UiUtils.dismissLoading();
                isLoading = false;
                UiUtils.showToast(LeadReportActivity.this, "Server not responding", false);
            }));

        } catch (Exception e) {
            UiUtils.dismissLoading();
            isLoading = false;
            UiUtils.showToast(this, "Request error", false);
        }
    }

    // ─────────────────────────────────────────
    // BOTTOM SHEET FILTER
    // ─────────────────────────────────────────
    private void openFilter() {

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        bottomSheet.setContentView(R.layout.filter_lead_report);

        TextView  tvFromDate  = bottomSheet.findViewById(R.id.etFromDate);
        TextView  tvToDate    = bottomSheet.findViewById(R.id.etToDate);
        Spinner   spProduct   = bottomSheet.findViewById(R.id.spProduct);
        Spinner   spStatus    = bottomSheet.findViewById(R.id.spStatus);
        Button    btnApply    = bottomSheet.findViewById(R.id.btnApply);
        Button    btnReset    = bottomSheet.findViewById(R.id.btnReset);

        // Pre-fill if already filtered
        if (!fromDate.isEmpty()) tvFromDate.setText(fromDate);
        if (!toDate.isEmpty())   tvToDate.setText(toDate);

        // Product spinner
        String[] productList = {"All", "PL — Personal Loan", "BL — Business Loan",
                "IL — Insurance Loan", "CC — Credit Card", "SA — Savings Account"};
        spProduct.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, productList));

        // Status spinner
        String[] statusList = {"All", "Success", "Failed", "Pending", "Processing"};
        spStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statusList));

        // From Date picker
        bottomSheet.findViewById(R.id.layoutFromDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = year + "-" +
                        String.format("%02d", month + 1) + "-" +
                        String.format("%02d", day);
                tvFromDate.setText(date);
                tvFromDate.setTextColor(getResources().getColor(android.R.color.black, null));
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // To Date picker
        bottomSheet.findViewById(R.id.layoutToDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = year + "-" +
                        String.format("%02d", month + 1) + "-" +
                        String.format("%02d", day);
                tvToDate.setText(date);
                tvToDate.setTextColor(getResources().getColor(android.R.color.black, null));
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Reset
        btnReset.setOnClickListener(v -> {
            fromDate = ""; toDate = ""; status = ""; product = "";
            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        // Apply
        btnApply.setOnClickListener(v -> {
            fromDate = tvFromDate.getText().toString().equals("Select date") ? "" : tvFromDate.getText().toString();
            toDate   = tvToDate.getText().toString().equals("Select date") ? "" : tvToDate.getText().toString();

            // Status
            String st = spStatus.getSelectedItem().toString();
            switch (st) {
                case "Success":    status = "1"; break;
                case "Failed":     status = "0"; break;
                case "Pending":    status = "2"; break;
                case "Processing": status = "3"; break;
                default:           status = "";  break;
            }

            // Product
            String pr = spProduct.getSelectedItem().toString();
            if (pr.equals("All")) {
                product = "";
            } else {
                product = pr.substring(0, 2); // "PL", "BL", etc.
            }

            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }
}