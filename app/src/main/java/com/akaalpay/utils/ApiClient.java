package com.akaalpay.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;

import com.akaalpay.LoginActivity;
import com.akaalpay.NoInternetActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class ApiClient {

    public interface SuccessCallback {
        void onSuccess(JSONObject response);
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    // ✅ GLOBAL CLIENT (90 sec timeout)
//    private static final OkHttpClient client = new OkHttpClient.Builder()
//            .connectTimeout(90, TimeUnit.SECONDS)
//            .readTimeout(90, TimeUnit.SECONDS)
//            .writeTimeout(90, TimeUnit.SECONDS)
//            .retryOnConnectionFailure(true)
//            .build();



    private static final List<Cookie> cookieStore = new CopyOnWriteArrayList<>();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    for (Cookie nc : cookies) {
                        // same name wali purani cookie hata do
                        cookieStore.removeIf(c -> c.name().equals(nc.name())
                                && c.domain().equals(nc.domain()));
                        cookieStore.add(nc);
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> valid = new ArrayList<>();
                    for (Cookie c : cookieStore) {
                        if (c.expiresAt() >= System.currentTimeMillis() && c.matches(url)) {
                            valid.add(c);
                        }
                    }
                    return valid;
                }
            })
            .build();


    // ─────────────────────────────────────────────────────────────
    // 1. DEVELOPER MODE CHECK
    // ─────────────────────────────────────────────────────────────
    private static boolean isDeveloperModeEnabled(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    private static void handleDeveloperMode(Context context) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("⚠️ Security Alert")
                    .setMessage("Developer Options / USB Debugging is ON.\n\n" +
                            "For security reasons, the app cannot continue.\n\n" +
                            "Please turn off Developer Options and try again.")
                    .setCancelable(false)
                    .setPositiveButton("Close App", (d, w) -> {
                        if (context instanceof androidx.appcompat.app.AppCompatActivity) {
                            ((androidx.appcompat.app.AppCompatActivity) context).finishAffinity();
                        }
                    })
                    .show();
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 2. INTERNET CHECK
    // ─────────────────────────────────────────────────────────────
    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(network);
        return cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static void handleNoInternet(Context context) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> {
            Intent intent = new Intent(context, NoInternetActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 3. TOKEN EXPIRY CHECK
    // ─────────────────────────────────────────────────────────────
    private static boolean isTokenExpired(JSONObject response) {
        if (response == null) return false;
        int status = response.optInt("status", -1);
        String message = response.optString("message", "");
        return status == 0 && message.toLowerCase().contains("token");
    }

    private static void handleTokenExpiry(Context context) {
        SharedPreferences pref =
                context.getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        pref.edit().clear().apply();

        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────────────────────
    public static void get(
            Context context,
            String endpoint,
            String token,
            SuccessCallback success,
            ErrorCallback error
    ) {
        // ✅ Developer Mode check first
//        if (isDeveloperModeEnabled(context)) {
//            handleDeveloperMode(context);
//            return;
//        }

        // ✅ Internet check
        if (!isNetworkAvailable(context)) {
            handleNoInternet(context);
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ApiConfig.BASE_URL + endpoint)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String res = response.body().string();
                JSONObject json = new JSONObject(res);

                // ✅ Token expiry check
                if (isTokenExpired(json)) {
                    handleTokenExpiry(context);
                    return;
                }

                success.onSuccess(json);

            } catch (java.net.UnknownHostException | java.net.SocketTimeoutException e) {
                handleNoInternet(context);
            } catch (Exception e) {
                error.onError(e.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // POST
    // ─────────────────────────────────────────────────────────────
    public static void post(
            Context context,
            String endpoint,
            String token,
            JSONObject body,
            SuccessCallback success,
            ErrorCallback error
    ) {
        // ✅ Developer Mode check first
//        if (isDeveloperModeEnabled(context)) {
//            handleDeveloperMode(context);
//            return;
//        }

        // ✅ Internet check
        if (!isNetworkAvailable(context)) {
            handleNoInternet(context);
            return;
        }

        new Thread(() -> {
            try {
                FormBody.Builder formBuilder = new FormBody.Builder();
                Iterator<String> keys = body.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    formBuilder.add(key, body.optString(key));
                }

                RequestBody requestBody = formBuilder.build();

                Request request = new Request.Builder()
                        .url(ApiConfig.BASE_URL + endpoint)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String res = response.body().string();
                JSONObject json = new JSONObject(res);

                // ✅ Token expiry check
                if (isTokenExpired(json)) {
                    handleTokenExpiry(context);
                    return;
                }

                success.onSuccess(json);

            } catch (java.net.UnknownHostException | java.net.SocketTimeoutException e) {
                handleNoInternet(context);
            } catch (Exception e) {
                error.onError(e.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // MULTIPART POST
    // ─────────────────────────────────────────────────────────────
    public static void postMultipart(
            Context context,
            String endpoint,
            String token,
            MultipartBody body,
            SuccessCallback success,
            ErrorCallback error
    ) {
        // ✅ Developer Mode check first
//        if (isDeveloperModeEnabled(context)) {
//            handleDeveloperMode(context);
//            return;
//        }

        // ✅ Internet check
        if (!isNetworkAvailable(context)) {
            handleNoInternet(context);
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ApiConfig.BASE_URL + endpoint)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String res = response.body().string();
                JSONObject json = new JSONObject(res);

                // ✅ Token expiry check
                if (isTokenExpired(json)) {
                    handleTokenExpiry(context);
                    return;
                }

                success.onSuccess(json);

            } catch (java.net.UnknownHostException | java.net.SocketTimeoutException e) {
                handleNoInternet(context);
            } catch (Exception e) {
                error.onError(e.getMessage());
            }
        }).start();
    }
}