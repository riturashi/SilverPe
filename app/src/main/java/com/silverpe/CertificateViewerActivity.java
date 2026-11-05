package com.silverpe;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;

import org.json.JSONObject;

public class CertificateViewerActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    private ImageView btnBack;

    private AppCompatButton btnPrint;
    private AppCompatButton btnDownload;

    private String TOKEN = "";
    private String certificateUrl = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_view);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        // INIT
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        btnBack = findViewById(R.id.btnBack);
        btnPrint = findViewById(R.id.btnPrint);
        btnDownload = findViewById(R.id.btnDownload);

        // TOKEN
        SharedPreferences pref =
                getSharedPreferences(
                        "BP_PREF",
                        Context.MODE_PRIVATE
                );

        TOKEN = pref.getString("TOKEN", "");

        // WEBVIEW SETTINGS
        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            Bitmap favicon
                    ) {

                        progressBar.setVisibility(
                                View.VISIBLE
                        );
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        progressBar.setVisibility(
                                View.GONE
                        );
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceError error
                    ) {

                        progressBar.setVisibility(
                                View.GONE
                        );

                        Toast.makeText(
                                CertificateViewerActivity.this,
                                "Failed to load certificate",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // DOWNLOAD LISTENER
        webView.setDownloadListener(
                new DownloadListener() {

                    @Override
                    public void onDownloadStart(
                            String url,
                            String userAgent,
                            String contentDisposition,
                            String mimeType,
                            long contentLength
                    ) {

                        try {

                            DownloadManager.Request request =
                                    new DownloadManager.Request(
                                            Uri.parse(url)
                                    );

                            request.setTitle(
                                    "Bharat Pay Certificate"
                            );

                            request.setDescription(
                                    "Downloading Certificate..."
                            );

                            request.setNotificationVisibility(
                                    DownloadManager.Request
                                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            );

                            request.setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    "BharatPay_Certificate_"
                                            + System.currentTimeMillis()
                                            + ".pdf"
                            );

                            DownloadManager manager =
                                    (DownloadManager)
                                            getSystemService(
                                                    DOWNLOAD_SERVICE
                                            );

                            if (manager != null) {

                                manager.enqueue(request);

                                Toast.makeText(
                                        CertificateViewerActivity.this,
                                        "Download Started",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                        } catch (Exception e) {

                            Toast.makeText(
                                    CertificateViewerActivity.this,
                                    "Download Failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );

        // FETCH CERTIFICATE URL
        fetchCertificateUrl();

        // BACK
        btnBack.setOnClickListener(v -> finish());

        // PRINT
        btnPrint.setOnClickListener(v -> printCertificate());

        // DOWNLOAD
        btnDownload.setOnClickListener(v -> downloadCertificate());
    }

    // =========================================
    // FETCH CERTIFICATE URL
    // =========================================

    private void fetchCertificateUrl() {

        progressBar.setVisibility(View.VISIBLE);

        ApiClient.get(
                this,
                "settings",
                TOKEN,

                response -> runOnUiThread(() -> {

                    try {

                        if (response != null
                                && response.optInt("status") == 1) {

                            JSONObject data =
                                    response.getJSONObject("data");

                            certificateUrl =
                                    data.optString(
                                            "certificate",
                                            ""
                                    );

                            if (certificateUrl.isEmpty()) {

                                progressBar.setVisibility(
                                        View.GONE
                                );

                                Toast.makeText(
                                        this,
                                        "Certificate URL not found",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            webView.loadUrl(certificateUrl);

                        } else {

                            progressBar.setVisibility(
                                    View.GONE
                            );

                            Toast.makeText(
                                    this,
                                    "Unable to load certificate",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } catch (Exception e) {

                        progressBar.setVisibility(
                                View.GONE
                        );

                        Toast.makeText(
                                this,
                                "Something went wrong",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }),

                error -> runOnUiThread(() -> {

                    progressBar.setVisibility(
                            View.GONE
                    );

                    Toast.makeText(
                            this,
                            "Server Error",
                            Toast.LENGTH_SHORT
                    ).show();
                })
        );
    }

    // =========================================
    // PRINT
    // =========================================

    private void printCertificate() {

        try {

            PrintManager printManager =
                    (PrintManager)
                            getSystemService(
                                    Context.PRINT_SERVICE
                            );

            String jobName =
                    "BharatPayCertificate";

            PrintDocumentAdapter adapter =
                    webView.createPrintDocumentAdapter(
                            jobName
                    );

            if (printManager != null) {

                printManager.print(
                        jobName,
                        adapter,
                        new PrintAttributes.Builder().build()
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Print Failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================
    // DOWNLOAD
    // =========================================

    private void downloadCertificate() {

        try {

            if (certificateUrl == null
                    || certificateUrl.isEmpty()) {

                Toast.makeText(
                        this,
                        "Certificate URL not found",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(
                            Uri.parse(certificateUrl)
                    );

            request.setTitle(
                    "Bharat Pay Certificate"
            );

            request.setDescription(
                    "Downloading..."
            );

            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "BharatPay_Certificate_"
                            + System.currentTimeMillis()
                            + ".pdf"
            );

            DownloadManager manager =
                    (DownloadManager)
                            getSystemService(
                                    DOWNLOAD_SERVICE
                            );

            if (manager != null) {

                manager.enqueue(request);

                Toast.makeText(
                        this,
                        "Download Started",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Download Failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================
    // BACK
    // =========================================

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}