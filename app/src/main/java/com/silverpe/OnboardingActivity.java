package com.silverpe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS = "BP_PREF";
    private static final String KEY_ONBOARDING_DONE = "ONBOARDING_DONE";

    private static final int[] ICONS = {
            R.drawable.ic_onboard_wallet,
            R.drawable.ic_onboard_bolt,
            R.drawable.ic_onboard_shield
    };

    private static final String[] TITLES = {
            "Manage Your Business",
            "Fast & Easy Payments",
            "Safe & Secure"
    };

    private static final String[] DESCS = {
            "Handle all your business services and transactions from one simple app.",
            "Complete payments and services in just a few taps, anytime you need.",
            "Your account is protected with secure login. Never share your password or OTP."
    };

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private MaterialButton btnNext;
    private TextView tvSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Agar user pehle onboarding dekh chuka hai to seedha login
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        dotsContainer = findViewById(R.id.dotsContainer);

        viewPager.setAdapter(new SlideAdapter());
        setupDots();
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                boolean last = position == TITLES.length - 1;
                btnNext.setText(last ? "Get Started" : "Next");
                tvSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < TITLES.length - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    // ---------- DOTS ----------
    private void setupDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TITLES.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int selected) {
        int active = ContextCompat.getColor(this, R.color.feno_primary);
        int inactive = Color.parseColor("#D0D0D0");

        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            boolean isSelected = i == selected;

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(4));
            bg.setColor(isSelected ? active : inactive);
            dot.setBackground(bg);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = isSelected ? dp(20) : dp(8);
            dot.setLayoutParams(lp);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply();
        openLogin();
    }

    private void openLogin() {
        startActivity(new Intent(this, SplashDecisionActivity.class));
        finish();
    }

    // ---------- ADAPTER ----------
    private static class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            h.img.setImageResource(ICONS[position]);
            h.title.setText(TITLES[position]);
            h.desc.setText(DESCS[position]);
        }

        @Override
        public int getItemCount() {
            return TITLES.length;
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView title, desc;

            Holder(@NonNull View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imgSlide);
                title = itemView.findViewById(R.id.tvSlideTitle);
                desc = itemView.findViewById(R.id.tvSlideDesc);
            }
        }
    }
}