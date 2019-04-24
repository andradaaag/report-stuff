package com.mobile.andrada.reportstuff.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Utils {
    public static String convertGeoPointToAdress(Context context, GeoPoint geoPoint) {
        if (geoPoint == null)
            return "";

        Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(
                    geoPoint.getLatitude(),
                    geoPoint.getLongitude(),
                    1);

            StringBuilder address = new StringBuilder();
            if (addresses.size() > 0) {
                for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
                    address.append(addresses.get(0).getAddressLine(i)).append(" ");
            }
            return address.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
