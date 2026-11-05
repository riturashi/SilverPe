package com.silverpe;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class BrowserRedirectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        String url = getIntent().getStringExtra("url");
        String encdata = getIntent().getStringExtra("encdata");

        String html =
                "<html>" +
                        "<body onload='document.f.submit()'>" +
                        "<form name='f' method='POST' action='" + url + "'>" +
                        "<input type='hidden' name='encdata' value='" + encdata + "'/>" +
                        "</form>" +
                        "</body>" +
                        "</html>";

        webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
        );
    }
}