package com.akaalpay.recharge;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;

import org.json.*;

import java.util.*;

public class PlanBottomSheet extends BottomSheetDialogFragment {

    private String operator, circle, mobile;
    private OnPlanSelect listener;

    private RecyclerView recyclerPlans;
    private TabLayout planTabs;
    private ProgressBar loader;
    private PlanAdapter adapter;

    private final List<JSONObject> currentDisplayList = new ArrayList<>();
    private JSONObject allPlansByCategory = new JSONObject();

    public interface OnPlanSelect {
        void onSelect(String amount);
    }

    // ✅ NEW INSTANCE WITH MOBILE
    public static PlanBottomSheet newInstance(String op, String cir, String mob, OnPlanSelect l) {
        PlanBottomSheet s = new PlanBottomSheet();
        s.listener = l;

        Bundle b = new Bundle();
        b.putString("operator", op);
        b.putString("circle", cir);
        b.putString("mobile", mob);

        s.setArguments(b);
        return s;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            operator = getArguments().getString("operator");
            circle = getArguments().getString("circle");
            mobile = getArguments().getString("mobile");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottomsheet_plans, container, false);

        planTabs = v.findViewById(R.id.planTabs);
        recyclerPlans = v.findViewById(R.id.recyclerPlans);
        loader = v.findViewById(R.id.planLoader);

        recyclerPlans.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlanAdapter(currentDisplayList, listener, this);
        recyclerPlans.setAdapter(adapter);

        planTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterPlans(tab.getText().toString());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadPlans();

        return v;
    }

    // ✅ API CALL
    private void loadPlans() {

        loader.setVisibility(View.VISIBLE);

        String token = requireContext()
                .getSharedPreferences("BP_PREF", Context.MODE_PRIVATE)
                .getString("TOKEN", "");

        JSONObject body = new JSONObject();
        try {
//            body.put("rechargeNumber", mobile);
//            body.put("operatorCode", operator);
//            body.put("circleCode", circle);

            body.put("mobile", mobile);
            body.put("operator", operator);
            body.put("circle", circle);
        } catch (Exception ignored) {}

        ApiClient.post(getContext(), "recharge/brwose-plans", token, body, response -> {

            try {

                if (response.optInt("status") != 1) {

                    String msg = response.optString("message", "Failed");
                    requireActivity().runOnUiThread(() -> {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    });

                    return;
                }

                JSONObject data = response.optJSONObject("data");

                if (data != null) {

                    Object plansObj = data.opt("plans");

                    if (plansObj instanceof JSONObject) {
                        allPlansByCategory = (JSONObject) plansObj;
                    } else {
                        allPlansByCategory = new JSONObject();
                    }

                    requireActivity().runOnUiThread(() -> {
                        loader.setVisibility(View.GONE);
                        setupTabs();
                    });
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> loader.setVisibility(View.GONE));
            }

        }, error -> {
            requireActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ✅ CREATE TABS
    private void setupTabs() {

        planTabs.removeAllTabs();

        if (allPlansByCategory.length() == 0) return;

        Iterator<String> keys = allPlansByCategory.keys();
        boolean first = true;

        while (keys.hasNext()) {

            String key = keys.next();
            planTabs.addTab(planTabs.newTab().setText(key));

            if (first) {
                filterPlans(key);
                first = false;
            }
        }
    }

    // ✅ FILTER PLANS
    private void filterPlans(String category) {

        try {

            if (!allPlansByCategory.has(category)) return;

            JSONArray arr = allPlansByCategory.getJSONArray(category);

            List<JSONObject> temp = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                temp.add(arr.getJSONObject(i));
            }

            adapter.updateList(temp);
            recyclerPlans.scrollToPosition(0);

        } catch (Exception ignored) {}
    }
}