package com.akaalpay.report;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.akaalpay.R;
import com.akaalpay.recharge.DthReportActivity;
import com.akaalpay.recharge.RechargeReportActivity;

public class ReportsFragment extends Fragment implements View.OnClickListener {

    public ReportsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_report, container, false);
//        Window window = requireActivity().getWindow();
//
//        WindowCompat.setDecorFitsSystemWindows(window, true);
//
//        window.setStatusBarColor(
//                ContextCompat.getColor(requireContext(), R.color.feno_primary)
//        );
//
//        WindowInsetsControllerCompat controller =
//                WindowCompat.getInsetsController(
//                        window, window.getDecorView()
//                );
//
//        controller.show(WindowInsetsCompat.Type.statusBars());
//        controller.setAppearanceLightStatusBars(false);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> goToHome());

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goToHome();
                    }
                }
        );


        // Banking
        view.findViewById(R.id.btnAeps).setOnClickListener(this);
        view.findViewById(R.id.btnAeps1).setOnClickListener(this);
        view.findViewById(R.id.btnMicroAtm).setOnClickListener(this);
        view.findViewById(R.id.btnMoneyTransfer).setOnClickListener(this);
        view.findViewById(R.id.btnExpressMoneyTransfer).setOnClickListener(this);
        view.findViewById(R.id.btnAadharPay).setOnClickListener(this);
        view.findViewById(R.id.btnMoveToBank).setOnClickListener(this);
        view.findViewById(R.id.btnFundRequest).setOnClickListener(this);

        // Naye 3 buttons
        view.findViewById(R.id.btnUpiCollection).setOnClickListener(this);
        view.findViewById(R.id.btnCms).setOnClickListener(this);
        view.findViewById(R.id.btnWalletTransfer).setOnClickListener(this);

        // Utility
        view.findViewById(R.id.btnMobile).setOnClickListener(this);
        view.findViewById(R.id.btnDth).setOnClickListener(this);
        view.findViewById(R.id.btnBBPS).setOnClickListener(this);
        view.findViewById(R.id.btnLead).setOnClickListener(this);

        return view;
    }

    private void goToHome() {

        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                requireActivity().findViewById(R.id.bottomNavigation);

        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home); // ✅ ye automatically HomeFragment load karega
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnAeps) {
            startActivity(new Intent(getContext(), AirtelAepsReportActivity.class));

        } else if (id == R.id.btnAeps1) {
            startActivity(new Intent(getContext(), FinoAepsReportActivity.class));

        } else if (id == R.id.btnMicroAtm) {
            startActivity(new Intent(getContext(), MicroAtmReportActivity.class));

        } else if (id == R.id.btnAadharPay) {
            startActivity(new Intent(getContext(), FinoAadharPayReportActivity.class));

        } else if (id == R.id.btnUpiCollection) {
            startActivity(new Intent(getContext(), UatmReportActivity.class));


        } else if (id == R.id.btnMoveToBank) {
            startActivity(new Intent(getContext(), PayoutReportActivity.class));


        } else if (id == R.id.btnBBPS) {
            startActivity(new Intent(getContext(), BbpsReportActivity.class));

        } else if (id == R.id.btnMoneyTransfer) {
            startActivity(new Intent(getContext(), DmtReportActivity.class));

        } else if (id == R.id.btnExpressMoneyTransfer) {
            startActivity(new Intent(getContext(), ExpressDmtReportActivity.class));

        } else if (id == R.id.btnMobile) {
            startActivity(new Intent(getContext(), RechargeReportActivity.class));

        } else if (id == R.id.btnLead) {
            startActivity(new Intent(getContext(), LeadReportActivity.class));

        } else if (id == R.id.btnDth) {
            startActivity(new Intent(getContext(), DthReportActivity.class));

        } else if (id == R.id.btnFundRequest) {
            startActivity(new Intent(getContext(), FundRequestReportActivity.class));

        } else {
            // btnMoneyTransfer, btnMobile, btnDth
            Toast.makeText(getContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
        }
    }
}