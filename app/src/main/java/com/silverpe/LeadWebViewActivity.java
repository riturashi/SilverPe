package com.silverpe;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.utils.StatusBarUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LeadWebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lead_web_view);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        ImageView   btnBack     = findViewById(R.id.btnBack);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        webView                 = findViewById(R.id.webView);

        btnBack.setOnClickListener(v -> finish());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Progress bar
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient());

        String url     = getIntent().getStringExtra("url");
        String encdata = getIntent().getStringExtra("encdata");

        try {
            String postData = "encdata=" + URLEncoder.encode(encdata, "UTF-8");
            webView.postUrl(url, postData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hardware back — WebView history check
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}