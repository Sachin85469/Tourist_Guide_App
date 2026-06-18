package com.arriva.touristguideapp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataProvider provides initial static data for the app.
 * Note: Local drawable resource IDs are now removed. The app relies on Firestore image URLs.
 */
public class DataProvider {

    private static List<Place> allPlaces = null;

    private static void initializeData() {
        if (allPlaces != null) return;

        // Remove any seeded, hardcoded places — app must rely solely on Firestore data.
        allPlaces = new ArrayList<>();
    }

    public static List<Place> getPlaces() {
        return getAllPlaces();
    }

    public static List<Place> getAllPlaces() {
        initializeData();
        return new ArrayList<>(allPlaces);
    }

    public static List<Place> getPlacesByCategory(String category) {
        initializeData();
        List<Place> filtered = new ArrayList<>();
        for (Place place : allPlaces) {
            if (place.getCategory().equalsIgnoreCase(category)) {
                filtered.add(place);
            }
        }
        return filtered;
    }

    public static List<Place> getTopPicks() {
        initializeData();
        List<Place> topPicks = new ArrayList<>();
        for (Place place : allPlaces) {
            if (place.isTopPick()) {
                topPicks.add(place);
            }
        }
        return topPicks;
    }

    public static List<Place> getDefaultTopPicks() {
        return getTopPicks();
    }

    public static List<Place> searchPlaces(String query) {
        initializeData();
        if (query == null || query.trim().isEmpty()) {
            return getAllPlaces();
        }
        String lowerQuery = query.toLowerCase().trim();
        List<Place> results = new ArrayList<>();
        for (Place place : allPlaces) {
            if (place.getName().toLowerCase().contains(lowerQuery) ||
                place.getCategory().toLowerCase().contains(lowerQuery) ||
                place.getTag().toLowerCase().contains(lowerQuery)) {
                results.add(place);
            }
        }
        return results;
    }

    public static List<Place> sortByDistance(List<Place> places) {
        places.sort(Comparator.comparingDouble(Place::getDistance));
        return places;
    }

    public static List<Place> sortByName(List<Place> places) {
        places.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        return places;
    }

    public static Map<String, Integer> getCategoryCount(List<Place> places) {
        Map<String, Integer> map = new HashMap<>();
        for (Place place : places) {
            String category = place.getCategory();
            if (category != null) {
                map.put(category, map.getOrDefault(category, 0) + 1);
            }
        }
        return map;
    }
}
