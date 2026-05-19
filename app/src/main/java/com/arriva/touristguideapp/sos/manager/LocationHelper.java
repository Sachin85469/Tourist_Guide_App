package com.arriva.touristguideapp.sos.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.sos.utils.Logger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {
    private final Context context;
    private final FusedLocationProviderClient client;

    public LocationHelper(Context context) {
        this.context = context;
        this.client = LocationServices.getFusedLocationProviderClient(context);
    }

    public interface LocationCallback {
        void onLocationResult(String mapsLink);
    }

    public void getLastLocation(LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.e("Location Failed: Permission not granted");
            callback.onLocationResult(null);
            return;
        }

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    callback.onLocationResult(formatLocation(location));
                } else {
                    requestNewLocation(callback);
                }
            }).addOnFailureListener(e -> {
                Logger.e("Error getting last location", e);
                requestNewLocation(callback);
            });
        } catch (Exception e) {
            Logger.e("Exception in getLastLocation", e);
            callback.onLocationResult(null);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocation(LocationCallback callback) {
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            callback.onLocationResult(formatLocation(location));
                        } else {
                            callback.onLocationResult(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Logger.e("Error getting current location", e);
                        callback.onLocationResult(null);
                    });
        } catch (Exception e) {
            Logger.e("Exception in requestNewLocation", e);
            callback.onLocationResult(null);
        }
    }

    private String formatLocation(Location location) {
        if (location == null) return null;
        return "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
    }
}
