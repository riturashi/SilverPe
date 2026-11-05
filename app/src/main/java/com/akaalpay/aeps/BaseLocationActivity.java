package com.akaalpay.aeps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Yeh base class sabhi Activities extend karengi jinme location zaroori hai
 * (2FA Auth, Cash Withdrawal, Mini ATM, DMT waghera).
 *
 * Isme "GPS OFF -> Settings -> wapas aane par auto-resume" wala poora
 * location-fix hai. Isko sirf EK BAAR yaha likhna hai, har Activity me
 * dobara likhne ki zarurat nahi.
 *
 * Kaise use karein (child Activity me):
 *   1) "extends BaseLocationActivity" likho (AppCompatActivity ki jagah)
 *   2) Button click par fetchLocationAndProceed() call karo
 *   3) onLocationReady() method override karo - location mil jaane ke
 *      baad wahi apna RD-service / payment flow call karo
 */
public abstract class BaseLocationActivity extends AppCompatActivity {

    // Child Activity in dono variables ko seedha use kar sakti hai
    protected double liveLat = 0.0;
    protected double liveLog = 0.0;

    private FusedLocationProviderClient fusedLocationClient;

    // Jab GPS OFF hone ki wajah se Settings khula ho, aur user wapas aaye,
    // to yeh flag batata hai ki scan/pay flow ko automatically resume karna hai
    private boolean pendingScanAfterLocationEnable = false;

    private LocationCallback locationCallback;

    private static final int LOCATION_SETTINGS_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST = 9002;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Settings se wapas aaye - ab GPS ON hai kya, check karo
        if (pendingScanAfterLocationEnable && isLocationEnabled()) {

            pendingScanAfterLocationEnable = false;

            fetchLocationAndProceed();
        }
    }

    /**
     * Child Activity apne "Scan" / "Pay" button ke click listener me
     * ISI method ko call karegi. Yeh permission check karega, GPS ON hai
     * ya nahi check karega, aur location milne ke baad onLocationReady()
     * call karega.
     */
    protected void fetchLocationAndProceed() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        if (!isLocationEnabled()) {

            pendingScanAfterLocationEnable = true;

            showLocationPopup();

            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {

                    if (location != null) {

                        liveLat = location.getLatitude();

                        liveLog = location.getLongitude();

                        onLocationReady();

                    } else {

                        // Provider ON hai lekin cached location abhi nahi bana -
                        // fresh live location maango
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(this, e -> requestFreshLocation());
    }

    /**
     * Har child Activity ISKO OVERRIDE karegi. Location mil jaane ke
     * baad (liveLat, liveLog set ho chuke honge) yaha apna asli kaam
     * likhna hai - jaise openRdService(), ya payment API call, etc.
     */
    protected abstract void onLocationReady();

    // ================= INTERNAL - ismein kuch change karne ki zarurat nahi =================

    private boolean isLocationEnabled() {

        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (lm == null) return false;

        try {

            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        } catch (Exception e) {

            return false;
        }
    }

    private void requestFreshLocation() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setNumUpdates(1);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult result) {

                fusedLocationClient.removeLocationUpdates(locationCallback);

                if (result != null && result.getLastLocation() != null) {

                    liveLat = result.getLastLocation().getLatitude();

                    liveLog = result.getLastLocation().getLongitude();

                    onLocationReady();

                } else {

                    Toast.makeText(
                            BaseLocationActivity.this,
                            "Unable to fetch current location. Please try again.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        };

        try {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

        } catch (Exception ignored) {
        }
    }

    private void showLocationPopup() {

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("GPS must be ON for Authentication. Enable now?")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        (d, w) -> startActivityForResult(
                                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                LOCATION_SETTINGS_REQUEST
                        ))
                .setNegativeButton("Cancel",
                        (d, w) -> pendingScanAfterLocationEnable = false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SETTINGS_REQUEST
                && pendingScanAfterLocationEnable
                && isLocationEnabled()) {

            pendingScanAfterLocationEnable = false;

            fetchLocationAndProceed();
        }

        // NOTE: child Activity apna RD_SERVICE_REQUEST wala onActivityResult
        // alag se likhegi (usme super.onActivityResult() call karna mat bhoolna)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                fetchLocationAndProceed();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is required for Authentication",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (locationCallback != null && fusedLocationClient != null) {

            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}