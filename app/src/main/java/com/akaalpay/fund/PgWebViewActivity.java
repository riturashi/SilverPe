package com.akaalpay.fund;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

public class PgWebViewActivity extends BaseActivity {

    WebView webView;
    ProgressBar progressBar;

    private static final String CALLBACK_MARKER = "CallBackController/fund_request_pg";

    boolean resultHandled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pg_webview);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        webView = findViewById(R.id.webViewPg);
        progressBar = findViewById(R.id.pgProgress);

        String gatewayUrl = getIntent().getStringExtra("gateway_url");
        String encData    = getIntent().getStringExtra("enc_data");
        String clientCode = getIntent().getStringExtra("client_code");

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Third-party cookies enable karo (bank/3DS redirect ke liye zaroori)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Mixed content allow karo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Normal http/https URLs WebView khud handle kare
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }

                // Baaki sab (upi://, phonepe://, tez://, paytmmp://, intent:// etc.)
                // ko respective app me launch karo
                try {
                    Intent intent;
                    if (url.startsWith("intent://")) {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    }
                    startActivity(intent);

                } catch (ActivityNotFoundException e) {
                    UiUtils.showToast(PgWebViewActivity.this,
                            "Required app not installed", false);
                } catch (Exception e) {
                    UiUtils.showToast(PgWebViewActivity.this,
                            "Unable to open payment app", false);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (!resultHandled && url != null && url.contains(CALLBACK_MARKER)) {
                    resultHandled = true;
                    checkResultOnPage(view);
                }
            }
        });

        String html = "<html><body onload=\"document.forms[0].submit()\">"
                + "<form method=\"POST\" action=\"" + gatewayUrl + "\">"
                + "<input type=\"hidden\" name=\"encData\" value=\"" + escapeHtml(encData) + "\"/>"
                + "<input type=\"hidden\" name=\"clientCode\" value=\"" + escapeHtml(clientCode) + "\"/>"
                + "</form></body></html>";

        webView.loadDataWithBaseURL("https://akaalpay.com/", html, "text/html", "UTF-8", null);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void checkResultOnPage(WebView view) {
        view.evaluateJavascript(
                "(function(){return document.body ? document.body.innerText : '';})();",
                value -> {
                    String pageText = value == null ? "" : value.toLowerCase();
                    boolean success = pageText.contains("payment successful");
                    showResultDialog(success);
                }
        );
    }

    private void showResultDialog(boolean success) {
        new AlertDialog.Builder(this)
                .setTitle(success ? "Payment Successful" : "Payment Failed / Pending")
                .setMessage(success
                        ? "Amount has been credited to your wallet."
                        : "If the amount was deducted, it will reflect in your wallet shortly. You can check the status in the Fund Request list.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Payment?")
                .setMessage("Are you sure you want to cancel this payment?")
                .setPositiveButton("Yes", (d, w) -> finish())
                .setNegativeButton("No", null)
                .show();
    }
}