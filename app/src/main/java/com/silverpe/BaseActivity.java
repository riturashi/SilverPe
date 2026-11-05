package com.silverpe;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private View noInternetView;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup mainLayout = (ViewGroup) inflater.inflate(layoutResID, null);

        // Attach No Internet Layout
        noInternetView = inflater.inflate(R.layout.layout_no_internet, mainLayout, false);
        mainLayout.addView(noInternetView);

        super.setContentView(mainLayout);

        // Retry Button
        noInternetView.findViewById(R.id.btn_retry_global).setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                noInternetView.setVisibility(View.GONE);
                onInternetRestored();
            }
        });

        startNetworkCallback();
    }

    private void startNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    noInternetView.setVisibility(View.GONE);
                    onInternetRestored();
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> noInternetView.setVisibility(View.VISIBLE));
            }
        };

        cm.registerNetworkCallback(request, networkCallback);

        // Initial Check
        if (!isNetworkAvailable()) {
            noInternetView.setVisibility(View.VISIBLE);
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    // 🔥 Override this in activity for reload
    protected void onInternetRestored() {
    }
}