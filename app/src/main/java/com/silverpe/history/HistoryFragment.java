package com.silverpe.history;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.silverpe.HomeFragment;
import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private View tvEmpty;

    private HistoryAdapter adapter;

    private final List<HistoryModel> list = new ArrayList<>();

    private String TOKEN = "";

    // Filter
    private String fromDate = "";
    private String toDate = "";
    private String type = "";

    // Pagination
    private int pageNo = 1;
    private int totalPages = 1;

    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = v.findViewById(R.id.recyclerHistory);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        LinearLayoutManager lm =
                new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(lm);

        adapter = new HistoryAdapter(getContext(), list);

        recyclerView.setAdapter(adapter);

        SharedPreferences pref =
                requireActivity().getSharedPreferences(
                        "BP_PREF",
                        Context.MODE_PRIVATE
                );

        TOKEN = pref.getString("TOKEN", "");

        // ================= BACK =================

        v.findViewById(R.id.btnBack).setOnClickListener(view -> {

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new HomeFragment()
                    )
                    .commit();
        });

        // ================= HARDWARE BACK =================

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(
                                                R.id.fragment_container,
                                                new HomeFragment()
                                        )
                                        .commit();
                            }
                        });

        // ================= FILTER =================

        v.findViewById(R.id.btnFilter)
                .setOnClickListener(btn -> openFilter());

        // ================= FIRST LOAD =================

        loadHistory(pageNo);

        // ================= PAGINATION =================

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(@NonNull RecyclerView rv,
                                           int dx,
                                           int dy) {

                        super.onScrolled(rv, dx, dy);

                        if (dy > 0 && !isLoading) {

                            int visible =
                                    lm.getChildCount();

                            int total =
                                    lm.getItemCount();

                            int firstPos =
                                    lm.findFirstVisibleItemPosition();

                            if ((visible + firstPos) >= total
                                    && pageNo < totalPages) {

                                pageNo++;

                                loadHistory(pageNo);
                            }
                        }
                    }
                });

        return v;
    }

    // ================= LOAD HISTORY =================

    private void loadHistory(int page) {

        if (isLoading) return;

        isLoading = true;

        if (page == 1) {

            list.clear();

            requireActivity().runOnUiThread(() -> {

                adapter.notifyDataSetChanged();

                tvEmpty.setVisibility(View.GONE);

                UiUtils.showLoading(requireActivity());
            });
        }

        try {

            JSONObject body = new JSONObject();

            body.put("page_no", page);
            body.put("fromDate", fromDate);
            body.put("toDate", toDate);
            body.put("type", type);

            ApiClient.post(
                    getActivity(),
                    "fund-list/all",
                    TOKEN,
                    body,

                    response -> {

                        requireActivity().runOnUiThread(() -> {

                            try {

                                JSONObject data =
                                        response.getJSONObject("data");

                                totalPages =
                                        data.optInt("totalPages", 1);

                                JSONArray arr =
                                        data.getJSONArray("list");

                                for (int i = 0;
                                     i < arr.length();
                                     i++) {

                                    JSONObject o =
                                            arr.getJSONObject(i);

                                    HistoryModel m =
                                            new HistoryModel();

                                    m.serviceName =
                                            o.optString("service_name");

                                    m.serviceImage =
                                            o.optString("service_image");

                                    m.type =
                                            o.optString("type");

                                    m.narration =
                                            o.optString("narration");

                                    m.amount =
                                            o.optString("amount");

                                    m.beforeBalance =
                                            o.optString("before_balance");

                                    m.updatedBalance =
                                            o.optString("updated_balance");

                                    m.date =
                                            o.optString("date");

                                    list.add(m);
                                }

                                adapter.notifyDataSetChanged();

                                tvEmpty.setVisibility(
                                        list.isEmpty()
                                                ? View.VISIBLE
                                                : View.GONE
                                );

                            } catch (Exception e) {

                                e.printStackTrace();
                            }

                            UiUtils.dismissLoading();

                            isLoading = false;
                        });
                    },

                    error -> {

                        requireActivity().runOnUiThread(() -> {

                            UiUtils.dismissLoading();

                            isLoading = false;
                        });
                    });

        } catch (Exception e) {

            e.printStackTrace();

            UiUtils.dismissLoading();

            isLoading = false;
        }
    }

    // ================= FILTER =================

    private void openFilter() {

        BottomSheetDialog sheet =
                new BottomSheetDialog(requireContext());

        sheet.setContentView(R.layout.filter_history);

        TextView tvFrom =
                sheet.findViewById(R.id.etFromDate);

        TextView tvTo =
                sheet.findViewById(R.id.etToDate);

        Spinner spType =
                sheet.findViewById(R.id.spType);

        Button btnApply =
                sheet.findViewById(R.id.btnApply);

        Button btnReset =
                sheet.findViewById(R.id.btnReset);

        if (tvFrom == null
                || tvTo == null
                || spType == null
                || btnApply == null
                || btnReset == null) {

            return;
        }

        // Old Values
        if (!fromDate.isEmpty()) {
            tvFrom.setText(fromDate);
        }

        if (!toDate.isEmpty()) {
            tvTo.setText(toDate);
        }

        // Spinner
        String[] types = {
                "All",
                "Credit",
                "Debit"
        };

        spType.setAdapter(
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        types
                )
        );

        if (type.equals("C")) {
            spType.setSelection(1);
        } else if (type.equals("D")) {
            spType.setSelection(2);
        }

        // ================= FROM DATE =================

        View fromLayout =
                sheet.findViewById(R.id.layoutFromDate);

        if (fromLayout != null) {

            fromLayout.setOnClickListener(v -> {

                Calendar cal =
                        Calendar.getInstance();

                new DatePickerDialog(
                        requireContext(),

                        (view, y, m, d) -> {

                            String date =
                                    y + "-"
                                            + String.format("%02d", m + 1)
                                            + "-"
                                            + String.format("%02d", d);

                            tvFrom.setText(date);

                            tvFrom.setTextColor(0xFF000000);
                        },

                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)

                ).show();
            });
        }

        // ================= TO DATE =================

        View toLayout =
                sheet.findViewById(R.id.layoutToDate);

        if (toLayout != null) {

            toLayout.setOnClickListener(v -> {

                Calendar cal =
                        Calendar.getInstance();

                new DatePickerDialog(
                        requireContext(),

                        (view, y, m, d) -> {

                            String date =
                                    y + "-"
                                            + String.format("%02d", m + 1)
                                            + "-"
                                            + String.format("%02d", d);

                            tvTo.setText(date);

                            tvTo.setTextColor(0xFF000000);
                        },

                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)

                ).show();
            });
        }

        // ================= APPLY =================

        btnApply.setOnClickListener(v -> {

            fromDate =
                    tvFrom.getText().toString()
                            .equals("Select date")
                            ? ""
                            : tvFrom.getText().toString();

            toDate =
                    tvTo.getText().toString()
                            .equals("Select date")
                            ? ""
                            : tvTo.getText().toString();

            String t =
                    spType.getSelectedItem().toString();

            switch (t) {

                case "Credit":
                    type = "C";
                    break;

                case "Debit":
                    type = "D";
                    break;

                default:
                    type = "";
                    break;
            }

            // Same Date Fix
            if (!fromDate.isEmpty()
                    && fromDate.equals(toDate)) {

                try {

                    SimpleDateFormat sdf =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                            );

                    Date d = sdf.parse(toDate);

                    Calendar cal =
                            Calendar.getInstance();

                    if (d != null) {

                        cal.setTime(d);

                        cal.add(
                                Calendar.DAY_OF_MONTH,
                                1
                        );

                        toDate =
                                sdf.format(cal.getTime());
                    }

                } catch (Exception ignored) {
                }
            }

            pageNo = 1;

            list.clear();

            adapter.notifyDataSetChanged();

            loadHistory(pageNo);

            sheet.dismiss();
        });

        // ================= RESET =================

        btnReset.setOnClickListener(v -> {

            fromDate = "";
            toDate = "";
            type = "";

            pageNo = 1;

            list.clear();

            adapter.notifyDataSetChanged();

            loadHistory(pageNo);

            sheet.dismiss();
        });

        sheet.show();
    }
}