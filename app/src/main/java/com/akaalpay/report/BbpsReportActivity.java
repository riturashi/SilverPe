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

public class BbpsReportActivity extends BaseActivity {

    RecyclerView recyclerReport;
    BbpsReportAdapter adapter;

    ArrayList<BbpsReportModel> reportList = new ArrayList<>();

    String fromDate = "";
    String toDate = "";
    String status = "";

    int page = 1;
    int totalPages = 1;

    boolean isLoading = false;

    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbps_report);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        recyclerReport = findViewById(R.id.recyclerReport);

        layoutManager = new LinearLayoutManager(this);
        recyclerReport.setLayoutManager(layoutManager);

        adapter = new BbpsReportAdapter(this, reportList);
        recyclerReport.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnFilter).setOnClickListener(v -> openFilter());

        // Pagination Scroll
        recyclerReport.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && page < totalPages) {

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {

                        page++;
                        loadReport();
                    }

                } else if (!isLoading && page >= totalPages) {

                    UiUtils.showToast(BbpsReportActivity.this,
                            "No Record Found", false);
                }
            }
        });

        loadReport();
    }

    private void loadReport() {

        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE)
                .getString("TOKEN", "");

        isLoading = true;

        UiUtils.showLoading(this);

        try {

            JSONObject body = new JSONObject();

            body.put("page_no", page);
            body.put("fromDate", fromDate);
            body.put("toDate", toDate);
            body.put("status", status);

            ApiClient.post(this, "bbps/view-all", TOKEN, body, response -> {

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    try {

                        int apiStatus = response.optInt("status");

                        if (apiStatus == 0) {

                            page--;
                            isLoading = false;

                            UiUtils.showToast(BbpsReportActivity.this,
                                    "No Record Found", false);

                            return;
                        }

                        JSONObject data = response.getJSONObject("data");

                        totalPages = data.optInt("totalPages");

                        JSONArray list = data.optJSONArray("list");

                        if (list == null || list.length() == 0) {

                            isLoading = false;

                            UiUtils.showToast(BbpsReportActivity.this,
                                    "No Record Found", false);

                            return;
                        }

                        if (page == 1) {
                            reportList.clear();
                        }

                        for (int i = 0; i < list.length(); i++) {

                            JSONObject obj = list.getJSONObject(i);

                            BbpsReportModel model = new BbpsReportModel();

                            model.id = obj.optString("id");

                            // ✅ BBPS Mapping Fix
                            model.mobile = obj.optString("canumber");
                            model.aadhar = obj.optString("biller_name");
                            model.amount = obj.optString("amount");
                            model.type = obj.optString("category");
                            model.rrn = obj.optString("reference_id");
                            model.ackno = obj.optString("ackno");
                            model.reference = obj.optString("reference_id");
                            model.status = obj.optInt("status");
                            model.date = obj.optString("date");

                            reportList.add(model);
                        }

                        adapter.notifyDataSetChanged();

                        isLoading = false;

                    } catch (Exception e) {

                        isLoading = false;

                        UiUtils.showToast(BbpsReportActivity.this,
                                "Parsing Error", false);
                    }

                });

            }, error -> {

                runOnUiThread(() -> {

                    UiUtils.dismissLoading();

                    isLoading = false;

                    UiUtils.showToast(BbpsReportActivity.this,
                            "Server not responding", false);

                });

            });

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

        // Pre-fill
        if (!fromDate.isEmpty()) tvFromDate.setText(fromDate);
        if (!toDate.isEmpty())   tvToDate.setText(toDate);

        // Status — All, Success, Pending, Failed
        String[] statusList = {"All", "Success", "Pending", "Failed"};
        spStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statusList));

        // From Date picker
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

        // To Date picker
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

        // Reset
        btnReset.setOnClickListener(v -> {
            fromDate = ""; toDate = ""; status = "";
            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        // Apply
        btnApply.setOnClickListener(v -> {
            fromDate = tvFromDate.getText().toString().equals("Select date") ? ""
                    : tvFromDate.getText().toString();
            toDate   = tvToDate.getText().toString().equals("Select date") ? ""
                    : tvToDate.getText().toString();

            String st = spStatus.getSelectedItem().toString();
            switch (st) {
                case "Success": status = "1"; break;
                case "Failed":  status = "0"; break;
                case "Pending": status = "2"; break;
                default:        status = "";  break;
            }

            page = 1;
            loadReport();
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }
}