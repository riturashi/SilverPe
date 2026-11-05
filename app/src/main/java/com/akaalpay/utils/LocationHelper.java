package com.akaalpay.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationHelper {
    private final Activity activity;
    private final FusedLocationProviderClient client;

    public interface LocationCallback {
        void onLocation(Location location);
    }

    public LocationHelper(Activity activity) {
        this.activity = activity;
        this.client = LocationServices.getFusedLocationProviderClient(activity);
    }

    public void getLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            callback.onLocation(null);
            return;
        }

        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocation(location);
            } else {
                // If last location is null, try requesting a fresh one
                callback.onLocation(null);
            }
        }).addOnFailureListener(e -> callback.onLocation(null));
    }
}