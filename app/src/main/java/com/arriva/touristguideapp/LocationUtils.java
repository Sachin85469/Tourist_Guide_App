package com.arriva.touristguideapp;

import android.location.Location;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for location and distance calculations.
 */
public class LocationUtils {
    private static final String TAG = "LocationUtils";

    /**
     * Calculates distance between two points in kilometers.
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0;
    }

    /**
     * Formats distance for display.
     */
    public static String formatDistance(double distanceInKm) {
        if (distanceInKm < 0) return "Unknown distance";
        if (distanceInKm < 1.0) {
            return String.format(Locale.getDefault(), "%.0f m away", distanceInKm * 1000);
        }
        return String.format(Locale.getDefault(), "%.1f km away", distanceInKm);
    }

    /**
     * Finds and sorts nearby places.
     */
    public static List<Place> getNearbyPlaces(List<Place> allPlaces, double userLat, double userLng, int limit) {
        if (allPlaces == null || allPlaces.isEmpty()) return new ArrayList<>();

        List<Place> nearby = new ArrayList<>(allPlaces);
        for (Place p : nearby) {
            p.setDistance(calculateDistance(userLat, userLng, p.getLat(), p.getLng()));
        }

        Collections.sort(nearby, Comparator.comparingDouble(Place::getDistance));

        Log.d(TAG, "NEARBY_PLACES_CALCULATED count=" + Math.min(nearby.size(), limit));
        return nearby.size() > limit ? nearby.subList(0, limit) : nearby;
    }
}
