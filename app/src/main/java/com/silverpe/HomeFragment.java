package com.silverpe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.multidex.BuildConfig;

import android.content.Intent;

import com.silverpe.aeps.AirtelEntryActivity;
import com.silverpe.bbps.BBPSActivity;
import com.silverpe.dmt.DmtActivity;
import com.silverpe.fund.FundRequestBottomSheet;
import com.silverpe.instantpay.UpiEntryActivity;
import com.silverpe.payout.MoveToBankActivity;
import com.silverpe.recharge.DthRechargeActivity;
import com.silverpe.recharge.RechargeActivity;
import com.silverpe.support.SupportActivity;
import com.silverpe.utils.ApiClient;
import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private TextView tvUserName, tvWalletBalance, tvTodayEarning, tvGreeting, tvAnnouncement;
    private Banner bannerSlider;
    private String TOKEN = "";

    // ✅ version check ek hi baar dikhe (app session me), tab switch pe baar-baar na khule
    private static boolean versionCheckDone = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // ── Init Views ──────────────────────────────────────────────────────
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvTodayEarning = view.findViewById(R.id.tvTodayEarning);
        tvAnnouncement = view.findViewById(R.id.tvAnnouncement);
        bannerSlider = view.findViewById(R.id.bannerSlider);




        // ── Profile icon → open drawer ──────────────────────────────────────
        view.findViewById(R.id.imgProfile).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = requireActivity().findViewById(R.id.drawerLayout);
            drawer.openDrawer(androidx.core.view.GravityCompat.START);
        });

        // ── Marquee fix ─────────────────────────────────────────────────────
        tvAnnouncement.setSelected(true);

        // ── Token ───────────────────────────────────────────────────────────
        SharedPreferences pref = requireActivity().getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");

        setGreeting();

        if (!TOKEN.isEmpty()) {
            checkAppVersion();   // ✅ Home khulte hi version check
            loadProfile();
            loadBalance();
            loadBanners();
            loadAnnouncements();
        } else {
            Toast.makeText(getContext(), "Session expired, login again", Toast.LENGTH_SHORT).show();
        }

        setupClickListeners(view);

        // ── Premium entrance animations ─────────────────────────────────────
        setupEntranceAnimations(view);

        return view;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  APP VERSION CHECK
    // ════════════════════════════════════════════════════════════════════════

    private void checkAppVersion() {

        if (versionCheckDone) return; // dobara dialog na khule
        versionCheckDone = true;

        ApiClient.get(getActivity(), "user/app-version", TOKEN, response -> {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> handleVersionResponse(response));
            }
        }, error -> {
            // version check fail ho to silently ignore — Home normal load hota rahe
        });
    }

    private void handleVersionResponse(JSONObject response) {
        try {
            if (response == null || response.optInt("status") != 1) return;

            JSONObject data = response.getJSONObject("data");

            int latestVersion = data.optInt("latest_version", 0);
            int forceUpdate = data.optInt("force_update", 0);
            String title = data.optString("title", "Update Available");
            String message = data.optString("message", "A new version is available.");
            String playstoreUrl = data.optString("playstore_url", "");

            int currentVersionCode = BuildConfig.VERSION_CODE;

            if (latestVersion > currentVersionCode) {
                showUpdateDialog(forceUpdate == 1, title, message, playstoreUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showUpdateDialog(boolean isForce, String title, String message, String playstoreUrl) {

        if (getActivity() == null || getActivity().isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).setCancelable(!isForce);

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            openPlayStore(playstoreUrl);
            // force update ho to dialog dismiss nahi karte — user wapas yahi dialog dekhega
        });

        if (!isForce) {
            builder.setNegativeButton("Later", (dialog, which) -> dialog.dismiss());
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(!isForce);

        if (isForce) {
            dialog.setOnKeyListener((d, keyCode, event) -> true); // back button block
        }

        dialog.show();
    }

    private void openPlayStore(String url) {
        if (getActivity() == null) return;

        String packageName = requireActivity().getPackageName();

        try {
            // 1️⃣ Pehle native Play Store app try karo
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            marketIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(marketIntent);

        } catch (android.content.ActivityNotFoundException e) {
            // 2️⃣ Play Store app nahi hai (emulator) → browser fallback
            try {
                String fallbackUrl = (url != null && !url.isEmpty()) ? url : "https://play.google.com/store/apps/details?id=" + packageName;

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);

            } catch (Exception ex) {
                Toast.makeText(getContext(), "Unable to open Play Store", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PREMIUM ANIMATIONS
    // ════════════════════════════════════════════════════════════════════════

    private void setupEntranceAnimations(View view) {

        // Announcement card — fade + slide up
        View announcementCard = view.findViewById(R.id.announcementCard);
        if (announcementCard != null) {
            announcementCard.setAlpha(0f);
            announcementCard.setTranslationY(24f);
            announcementCard.animate().alpha(1f).translationY(0f).setDuration(420).setStartDelay(180).setInterpolator(new DecelerateInterpolator()).start();
        }

        // Wallet card — fade in
        View walletCard = view.findViewById(R.id.tvWalletBalance);
        if (walletCard != null) {
            walletCard.setAlpha(0f);
            walletCard.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
        }

        // Apply tap scale effect to al
        // l service cards
        setupTapEffect(view);
    }

    /**
     * Scale-bounce on touch — premium feel, no ripple needed
     */
    private void setupTapEffect(View root) {
        int[] btnIds = {
                // Banking
                R.id.btnAeps,
//                R.id.btnMicroAtm,
                R.id.btnMoneyTransfer,
//                R.id.btnExpressMoneyTransfer,
//                R.id.btnAadharPay,
                R.id.btnMoveToBank, R.id.btnUpiCollection,
//                R.id.btnCms,
                R.id.btnWalletTransfer,
                // Utility
                R.id.btnMobile, R.id.btnDth, R.id.btnAgentCollection, R.id.btnBroadbandPostpaid, R.id.btnCableTv, R.id.btnClubAssociation, R.id.btnCreditCard, R.id.btnDonation, R.id.btnDthBbps, R.id.btneChallan, R.id.btneEducationFees, R.id.btnElectricity, R.id.btnEvRecharge, R.id.btnFastag, R.id.btnGas, R.id.btnHousingSociety, R.id.btnInsurance, R.id.btnLandlinePostpaid, R.id.btnLoanRepayment, R.id.btnLpgGas, R.id.btnMobilePostpaid, R.id.btnMobilePrepaid, R.id.btnMunicipalTaxes, R.id.btnNationalPensionSystem, R.id.btnNcmcRecharge, R.id.btnPrepaidMeter, R.id.btnRecurringDeposit, R.id.btnRental, R.id.btnSubscription, R.id.btnWater,

                // R.id.btnLeadPersonalLoan,
                //  R.id.btnLeadBusinessLoan,
//                R.id.btnLeadInsuranceLoan,
//                R.id.btnLeadCreditCard,
                // R.id.btnLeadSavingAccount
        };

        for (int id : btnIds) {
            View btn = root.findViewById(id);
            if (btn != null) applyScaleTap(btn);
        }

        // Add money button bhi
        View addMoney = root.findViewById(R.id.btnAddMoney);
        if (addMoney != null) applyScaleTap(addMoney);
    }

    private void applyScaleTap(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start();
                    break;

                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(new OvershootInterpolator(2.2f)).start();
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        view.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    /**
     * Call this after balance API returns — smooth fade swap
     */
    private void animateBalanceUpdate(String newBalance) {
        tvWalletBalance.animate().alpha(0f).setDuration(140).withEndAction(() -> {
            tvWalletBalance.setText("₹ " + newBalance);
            tvWalletBalance.animate().alpha(1f).setDuration(260).start();
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BBPS HELPER
    // ════════════════════════════════════════════════════════════════════════

    private void openBBPS(String category, String name) {
        Intent intent = new Intent(requireActivity(), BBPSActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLICK LISTENERS  (original — untouched)
    // ════════════════════════════════════════════════════════════════════════

    private void setupClickListeners(View view) {

        // --- BANKING ---
//        view.findViewById(R.id.btnAeps).setOnClickListener(v -> {
//            AepsOptionBottomSheet bottomSheet = new AepsOptionBottomSheet();
//            bottomSheet.show(getChildFragmentManager(), "AepsBottomSheet");
//        });
        view.findViewById(R.id.btnAeps).setOnClickListener(v -> startActivity(new Intent(getActivity(), AirtelEntryActivity.class)));

        view.findViewById(R.id.btnMoneyTransfer).setOnClickListener(v -> startActivity(new Intent(getActivity(), DmtActivity.class)));

//        view.findViewById(R.id.btnExpressMoneyTransfer).setOnClickListener(v ->
//                startActivity(new Intent(getActivity(), ExpressDmtTransferActivity.class)));

        view.findViewById(R.id.btnAddMoney).setOnClickListener(v -> new FundRequestBottomSheet().show(getChildFragmentManager(), "fund_request_sheet"));

//        view.findViewById(R.id.btnAadharPay).setOnClickListener(v ->
//                startActivity(new Intent(getActivity(), AadharPayEntryActivity.class)));

//        view.findViewById(R.id.btnMicroAtm).setOnClickListener(v ->
//                startActivity(new Intent(getActivity(), MicroAtmActivity.class)));

        view.findViewById(R.id.btnMoveToBank).setOnClickListener(v -> startActivity(new Intent(requireActivity(), MoveToBankActivity.class)));

        view.findViewById(R.id.btnUpiCollection).setOnClickListener(v -> startActivity(new Intent(requireActivity(), UpiEntryActivity.class)));

//        view.findViewById(R.id.btnCms).setOnClickListener(v ->
//                startActivity(new Intent(getActivity(), CmsActivity.class)));

        view.findViewById(R.id.btnWalletTransfer).setOnClickListener(v -> startActivity(new Intent(getActivity(), WalletTransferActivity.class)));

        // --- UTILITY / RECHARGE ---
        view.findViewById(R.id.btnMobile).setOnClickListener(v -> startActivity(new Intent(requireActivity(), RechargeActivity.class)));

        view.findViewById(R.id.btnDth).setOnClickListener(v -> startActivity(new Intent(requireActivity(), DthRechargeActivity.class)));


// --- FINANCIAL SERVICES (Lead Generation) ---
//        view.findViewById(R.id.btnLeadPersonalLoan).setOnClickListener(v -> {
//            Intent i = new Intent(getActivity(), LeadGenerationActivity.class);
//            i.putExtra("product_code", "PL");
//            i.putExtra("product_label", "Personal Loan");
//            startActivity(i);
//        });

//        view.findViewById(R.id.btnLeadBusinessLoan).setOnClickListener(v -> {
//            Intent i = new Intent(getActivity(), LeadGenerationActivity.class);
//            i.putExtra("product_code", "BL");
//            i.putExtra("product_label", "Business Loan");
//            startActivity(i);
//        });

//        view.findViewById(R.id.btnLeadInsuranceLoan).setOnClickListener(v -> {
//            Intent i = new Intent(getActivity(), LeadGenerationActivity.class);
//            i.putExtra("product_code", "IL");
//            i.putExtra("product_label", "Insurance Loan");
//            startActivity(i);
//        });

//        view.findViewById(R.id.btnLeadCreditCard).setOnClickListener(v -> {
//            Intent i = new Intent(getActivity(), LeadGenerationActivity.class);
//            i.putExtra("product_code", "CC");
//            i.putExtra("product_label", "Credit Card Apply");
//            startActivity(i);
//        });

//        view.findViewById(R.id.btnLeadSavingAccount).setOnClickListener(v -> {
//            Intent i = new Intent(getActivity(), LeadGenerationActivity.class);
//            i.putExtra("product_code", "SA");
//            i.putExtra("product_label", "Saving Account");
//            startActivity(i);
//        });


        // --- BBPS ---
        view.findViewById(R.id.btnAgentCollection).setOnClickListener(v -> openBBPS("BC01", "Agent Collection"));

        view.findViewById(R.id.btnBroadbandPostpaid).setOnClickListener(v -> openBBPS("BC02", "Broadband Postpaid"));

        view.findViewById(R.id.btnCableTv).setOnClickListener(v -> openBBPS("BC03", "Cable TV"));

        view.findViewById(R.id.btnClubAssociation).setOnClickListener(v -> openBBPS("BC04", "Clubs and Associations"));

        view.findViewById(R.id.btnCreditCard).setOnClickListener(v -> openBBPS("BC05", "Credit Card"));

        view.findViewById(R.id.btnDonation).setOnClickListener(v -> openBBPS("BC06", "Donation"));

        view.findViewById(R.id.btnDthBbps).setOnClickListener(v -> openBBPS("BC07", "DTH"));

        view.findViewById(R.id.btneChallan).setOnClickListener(v -> openBBPS("BC08", "eChallan"));

        view.findViewById(R.id.btneEducationFees).setOnClickListener(v -> openBBPS("BC09", "Education Fees"));

        view.findViewById(R.id.btnElectricity).setOnClickListener(v -> openBBPS("BC10", "Electricity"));

        view.findViewById(R.id.btnEvRecharge).setOnClickListener(v -> openBBPS("BC11", "EV Recharge"));

        view.findViewById(R.id.btnFastag).setOnClickListener(v -> openBBPS("BC12", "Fastag"));

        view.findViewById(R.id.btnGas).setOnClickListener(v -> openBBPS("BC13", "Gas"));

        view.findViewById(R.id.btnHousingSociety).setOnClickListener(v -> openBBPS("BC14", "Housing Society"));

        view.findViewById(R.id.btnInsurance).setOnClickListener(v -> openBBPS("BC15", "Insurance"));

        view.findViewById(R.id.btnLandlinePostpaid).setOnClickListener(v -> openBBPS("BC16", "Landline Postpaid"));

        view.findViewById(R.id.btnLoanRepayment).setOnClickListener(v -> openBBPS("BC17", "Loan Repayment"));

        view.findViewById(R.id.btnLpgGas).setOnClickListener(v -> openBBPS("BC18", "LPG Gas"));

        view.findViewById(R.id.btnMobilePostpaid).setOnClickListener(v -> openBBPS("BC19", "Mobile Postpaid"));

        view.findViewById(R.id.btnMobilePrepaid).setOnClickListener(v -> openBBPS("BC20", "Mobile Prepaid"));

        view.findViewById(R.id.btnMunicipalTaxes).setOnClickListener(v -> openBBPS("BC22", "Municipal Taxes"));

        view.findViewById(R.id.btnNationalPensionSystem).setOnClickListener(v -> openBBPS("BC23", "National Pension System"));

        view.findViewById(R.id.btnNcmcRecharge).setOnClickListener(v -> openBBPS("BC24", "NCMC Recharge"));

        view.findViewById(R.id.btnPrepaidMeter).setOnClickListener(v -> openBBPS("BC25", "Prepaid Meter"));

        view.findViewById(R.id.btnRecurringDeposit).setOnClickListener(v -> openBBPS("BC26", "Recurring Deposit"));

        view.findViewById(R.id.btnRental).setOnClickListener(v -> openBBPS("BC27", "Rental"));

        view.findViewById(R.id.btnSubscription).setOnClickListener(v -> openBBPS("BC28", "Subscription"));

        view.findViewById(R.id.btnWater).setOnClickListener(v -> openBBPS("BC29", "Water"));

        // --- TOP BAR ---
        view.findViewById(R.id.btnNotification).setOnClickListener(v -> toast("No Any Notificaton."));

        view.findViewById(R.id.btnCallSupport).setOnClickListener(v -> v.getContext().startActivity(new Intent(v.getContext(), SupportActivity.class)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  API METHODS  (original — untouched)
    // ════════════════════════════════════════════════════════════════════════

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void setGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) tvGreeting.setText("Good Morning,");
        else if (hour < 17) tvGreeting.setText("Good Afternoon,");
        else tvGreeting.setText("Good Evening,");
    }

    private void loadProfile() {
        ApiClient.get(getActivity(), "user/my-profile", TOKEN, response -> {
            try {
                JSONObject data = response.getJSONObject("data");
                String name = data.optString("name", "User");
                requireActivity().runOnUiThread(() -> tvUserName.setText(name));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, error -> requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Profile load failed", Toast.LENGTH_SHORT).show()));
    }

    private void loadBalance() {
        ApiClient.get(getActivity(), "user/current-balance", TOKEN, response -> {
            try {
                JSONObject data = response.getJSONObject("data");
                String balance = data.optString("user_balance", "0.00");
                String earning = data.optString("todays_earning", "0");

                requireActivity().runOnUiThread(() -> {
                    // Premium fade-swap animation on balance
                    animateBalanceUpdate(balance);
                    tvTodayEarning.setText("₹ " + earning);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, error -> requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Balance load failed", Toast.LENGTH_SHORT).show()));
    }

    private void loadBanners() {

        // Initially hide banner
        bannerSlider.setVisibility(View.GONE);

        ApiClient.get(getActivity(), "banners", TOKEN, response -> {
            try {
                List<String> bannerImages = new ArrayList<>();

                if (response.has("data")) {
                    JSONObject dataObj = response.getJSONObject("data");

                    if (dataObj.has("banners")) {
                        JSONArray bannerArray = dataObj.getJSONArray("banners");

                        for (int i = 0; i < bannerArray.length(); i++) {
                            JSONObject obj = bannerArray.getJSONObject(i);

                            String url = obj.optString("image", "").trim();

                            if (!url.isEmpty()) {
                                bannerImages.add(url);
                            }
                        }
                    }
                }

                requireActivity().runOnUiThread(() -> {

                    // No banner: completely remove space
                    if (bannerImages.isEmpty()) {
                        bannerSlider.setVisibility(View.GONE);
                        return;
                    }

                    // Banner available: show space
                    bannerSlider.setVisibility(View.VISIBLE);

                    bannerSlider.setAdapter(new BannerImageAdapter<String>(bannerImages) {
                        @Override
                        public void onBindView(BannerImageHolder holder, String data,
                                               int position, int size) {

                            holder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                            com.bumptech.glide.Glide.with(holder.imageView)
                                    .load(data)
                                    .dontAnimate()
                                    .into(holder.imageView);
                        }
                    });

                    bannerSlider.setIndicator(new CircleIndicator(getContext()));
                    bannerSlider.start();
                });

            } catch (Exception e) {
                e.printStackTrace();

                requireActivity().runOnUiThread(() ->
                        bannerSlider.setVisibility(View.GONE));
            }

        }, error -> requireActivity().runOnUiThread(() -> {
            bannerSlider.setVisibility(View.GONE);
        }));
    }

    private void loadAnnouncements() {
        ApiClient.get(getActivity(), "banners", TOKEN, response -> {
            try {
                if (!response.has("data")) return;
                JSONObject dataObj = response.getJSONObject("data");
                if (!dataObj.has("announcements")) return;

                JSONArray arr = dataObj.getJSONArray("announcements");
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String desc = obj.optString("description");
                    if (!desc.isEmpty()) sb.append(" 🔹 ").append(desc);
                }

                if (sb.length() == 0) sb.append("Welcome to BharatDigiCash");

                String finalText = sb.toString();
                requireActivity().runOnUiThread(() -> {
                    tvAnnouncement.setText(finalText);
                    tvAnnouncement.setSelected(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, error -> requireActivity().runOnUiThread(() -> tvAnnouncement.setText("Welcome to FenoPay")));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void onResume() {
        super.onResume();
        if (bannerSlider != null) bannerSlider.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bannerSlider != null) bannerSlider.stop();
    }
}