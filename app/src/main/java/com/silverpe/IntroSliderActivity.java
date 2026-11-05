package com.silverpe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class IntroSliderActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("APP", MODE_PRIVATE);
        if (sp.getBoolean("intro_seen", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.viewPager);
        btnGetStarted = findViewById(R.id.btnGetStarted);

        viewPager.setAdapter(new IntroSliderAdapter(this));

        btnGetStarted.setOnClickListener(v -> {
            sp.edit().putBoolean("intro_seen", true).apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
