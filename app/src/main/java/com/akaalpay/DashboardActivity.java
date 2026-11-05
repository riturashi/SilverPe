package com.akaalpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.akaalpay.history.HistoryFragment;
import com.akaalpay.utils.ApiClient;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private DrawerLayout drawer;
    private ImageView sideImgUser, imgFb, imgInsta, imgYoutube, imgWhatapp;
    private TextView tvName, tvUserId;
    private String userId = "";


    private int currentSelectedId = -1;

    // 🔥 ADD THESE (missing tha)
    private View menuTerms, menuPrivacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Window window = getWindow();
        window.clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        window.setStatusBarColor(
                Color.parseColor("#301BD3")
        );
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );
        controller.show(WindowInsetsCompat.Type.statusBars());
        controller.setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.mainContent),
                (view, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            view.getPaddingLeft(),
                            insets.top,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return windowInsets;
                }
        );
        initViews();
        setupBottomNav(savedInstanceState);
        setupSidebarLinks();
        setupSocialLinks();
        loadUserData();
    }

    private void initViews() {
        drawer = findViewById(R.id.drawerLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        View sidebar = findViewById(R.id.sidebar);

        sideImgUser = sidebar.findViewById(R.id.side_imgUser);
        tvName = sidebar.findViewById(R.id.tvName);
        tvUserId = sidebar.findViewById(R.id.tvUserId);

        imgFb = findViewById(R.id.imgFb);
        imgInsta = findViewById(R.id.imgInsta);
        imgYoutube = findViewById(R.id.imgYoutube);
        imgWhatapp = findViewById(R.id.imgWhatapp);

        // 🔥 IMPORTANT
        menuTerms = findViewById(R.id.menuTerms);
        menuPrivacy = findViewById(R.id.menuPrivacy);

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
        sidebar.getLayoutParams().width = width;
    }

    // 🔥 MAIN FIXED SIDEBAR
    private void setupSidebarLinks() {

        setupItem(findViewById(R.id.menuHome), "Home", R.drawable.ic_home_design);
        setupItem(findViewById(R.id.menuProfile), "Profile", R.drawable.ic_profile_design);
        setupItem(findViewById(R.id.menuReport), "Report", R.drawable.ic_report_design);
        setupItem(findViewById(R.id.menuCertificate), "Certificate", R.drawable.ic_retailer_certificate);

        setupItem(findViewById(R.id.menuTds), "194N TDS Exception", R.drawable.ic_194tds);
        setupItem(findViewById(R.id.menuChangePass), "Change Password", R.drawable.ic_password_new);
        setupItem(findViewById(R.id.menuChangeTxnPass), "Change Transaction Password", R.drawable.ic_password_new);

        setupItem(menuTerms, "Terms & Conditions", R.drawable.ic_terms_condition);
        setupItem(menuPrivacy, "Privacy Policy", R.drawable.ic_terms_condition);

        setupItem(findViewById(R.id.menuSupport), "Support", R.drawable.ic_support_icon);
        setupItem(findViewById(R.id.menuLogout), "Logout", R.drawable.ic_logout);

        // 🔥 CLICK + NAV SYNC
        findViewById(R.id.menuHome).setOnClickListener(v -> openFragment(new HomeFragment(), R.id.nav_home));
        findViewById(R.id.menuProfile).setOnClickListener(v -> openFragment(new ProfileFragment(), R.id.nav_profile));
        findViewById(R.id.menuReport).setOnClickListener(v -> openFragment(new com.akaalpay.report.ReportsFragment(), R.id.nav_reports));
        findViewById(R.id.menuChangePass).setOnClickListener(v -> {
            closeDrawer();

            new Handler().postDelayed(() -> {
                startActivity(new Intent(DashboardActivity.this, ChangePasswordActivity.class));
            }, 200);
        });

        findViewById(R.id.menuChangeTxnPass).setOnClickListener(v -> {
            closeDrawer();

            new Handler().postDelayed(() -> {
                startActivity(new Intent(DashboardActivity.this, ChangeTxnPasswordActivity.class));
            }, 200);
        });
        findViewById(R.id.menuCertificate).setOnClickListener(v -> {
            closeDrawer();

            new Handler().postDelayed(() -> {

                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent i = new Intent(DashboardActivity.this, CertificateViewerActivity.class);
                i.putExtra("USER_ID", userId);
                startActivity(i);

            }, 200);
        });

        // TERMS
        menuTerms.setOnClickListener(v -> {
            closeDrawer();
            new Handler().postDelayed(() ->
                    openUrl("https://akaalpay.com/termscondition.html"), 200);
        });

        // PRIVACY
        menuPrivacy.setOnClickListener(v -> {
            closeDrawer();
            new Handler().postDelayed(() ->
                    openUrl("https://akaalpay.com/privacypolicy.html"), 200);
        });

        // SUPPORT
        findViewById(R.id.menuSupport).setOnClickListener(v -> {
            closeDrawer();
            new Handler().postDelayed(() ->
                    startActivity(new Intent(this, com.akaalpay.support.SupportActivity.class)), 200);
        });

        // LOGOUT
        findViewById(R.id.menuLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void setupItem(View view, String title, int iconRes) {
        if (view != null) {
            TextView tv = view.findViewById(R.id.nav_text);
            ImageView img = view.findViewById(R.id.nav_icon);
            if (tv != null) tv.setText(title);
            if (img != null) img.setImageResource(iconRes);
        }
    }

    // 🔥 NAV + SIDEBAR SYNC METHOD
    private void openFragment(Fragment fragment, int navId) {
        closeDrawer();

        new Handler().postDelayed(() -> {
            currentSelectedId = navId;
            bottomNavigation.setSelectedItemId(navId); // 🔥 SYNC
            loadFragment(fragment);
        }, 250);
    }

    private void setupSocialLinks() {
        imgYoutube.setOnClickListener(v -> openUrl("https://www.youtube.com/@bharatdigicash"));
        imgFb.setOnClickListener(v -> openUrl("https://www.facebook.com/bharatdigicash/"));
        imgInsta.setOnClickListener(v -> openUrl("https://www.instagram.com/bharatdigicash/"));
        imgWhatapp.setOnClickListener(v -> openUrl("https://whatsapp.com/channel/0029VaA6iCtFsn0lwBr2g12l"));
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        pref.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
        SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        ApiClient.get(this, "user/my-profile", token, response -> {
            try {
                JSONObject data = response.getJSONObject("data");

                String name = data.optString("name", "User");
                String username = data.optString("username", "---");
                userId = username;
                String img = data.optString("profile_image", "");

                runOnUiThread(() -> {
                    tvName.setText(name);
                    tvUserId.setText("ID: " + username);

                    Glide.with(this)
                            .load(img)
                            .circleCrop()
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .into(sideImgUser);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, error -> {
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
//        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
//                new FragmentManager.FragmentLifecycleCallbacks() {
//                    @Override
//                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
//                        boolean yellow = f instanceof ProfileFragment;
//                        Window w = getWindow();
//                        w.setStatusBarColor(ContextCompat.getColor(DashboardActivity.this,
//                                yellow ? R.color.feno_primary : R.color.white));
//                        WindowCompat.getInsetsController(w, w.getDecorView())
//                                .setAppearanceLightStatusBars(true);
//                    }
//                }, false);
    }

    private void closeDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    private void setupBottomNav(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {

            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }

            int id = item.getItemId();
            if (id == currentSelectedId) return true;

            currentSelectedId = id;

            if (id == R.id.nav_home) loadFragment(new HomeFragment());
            else if (id == R.id.nav_reports) loadFragment(new com.akaalpay.report.ReportsFragment());
            else if (id == R.id.nav_profile) loadFragment(new ProfileFragment());
            else if (id == R.id.nav_more) loadFragment(new HistoryFragment());

            return true;
        });
    }
}