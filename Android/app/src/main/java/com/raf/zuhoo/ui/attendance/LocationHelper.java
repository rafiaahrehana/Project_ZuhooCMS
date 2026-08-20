package com.raf.zuhoo.ui.attendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Runtime location permission + a single GPS fix, built on plain LocationManager rather than
 * play-services-location — this app has no Play Services dependency anywhere else, and one
 * screen isn't reason enough to add one.
 */
public class LocationHelper {

    private static final long FIX_TIMEOUT_MS = 15_000;

    private final AppCompatActivity activity;
    private final LocationManager locationManager;
    private final ActivityResultLauncher<String> permissionLauncher;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    private Listener pendingListener;
    private LocationListener activeLocationListener;

    public interface Listener {
        void onLocation(double latitude, double longitude);
        void onUnavailable();
    }

    public LocationHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        this.permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        fetchLocation();
                    } else {
                        finish(null);
                    }
                });
    }

    public void requestLocation(Listener listener) {
        pendingListener = listener;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {

        String provider = bestProvider();
        if (locationManager == null || provider == null) {
            finish(null);
            return;
        }

        Location last = locationManager.getLastKnownLocation(provider);
        // A last-known fix under a minute old is close enough and avoids waiting on a fresh
        // GPS lock; anything older (or missing) falls through to a live single update.
        if (last != null && System.currentTimeMillis() - last.getTime() < 60_000) {
            finish(last);
            return;
        }

        activeLocationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                finish(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };

        locationManager.requestSingleUpdate(provider, activeLocationListener, Looper.getMainLooper());
        timeoutHandler.postDelayed(() -> finish(null), FIX_TIMEOUT_MS);
    }

    private String bestProvider() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    /** Delivers the result once and only once — the timeout and a real fix race each other. */
    private void finish(Location location) {

        if (pendingListener == null) {
            return;
        }

        timeoutHandler.removeCallbacksAndMessages(null);
        if (activeLocationListener != null) {
            locationManager.removeUpdates(activeLocationListener);
            activeLocationListener = null;
        }

        Listener listener = pendingListener;
        pendingListener = null;

        if (location != null) {
            listener.onLocation(location.getLatitude(), location.getLongitude());
        } else {
            listener.onUnavailable();
        }
    }
}
