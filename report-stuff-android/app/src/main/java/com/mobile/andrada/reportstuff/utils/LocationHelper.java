package com.mobile.andrada.reportstuff.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.firebase.firestore.GeoPoint;

public class LocationHelper {
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;

    public static boolean checkForLocationPermission(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
            return false;
        }
        return true;
    }

    public static GeoPoint convertLocation(Location location){
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }
}
