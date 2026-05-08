package com.arriva.touristguideapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProvider {

    private static List<Place> allPlaces = null;

    /**
     * Initializes the static data if it hasn't been already.
     * This mimics a data fetch from a database or API.
     */
    private static void initializeData() {
        if (allPlaces != null) return;

        allPlaces = new ArrayList<>();

        // 1. Shaniwar Wada -> History
        Place p1 = new Place(
                "1",
                "Shaniwar Wada",
                "Pune",
                "History",
                "18th-century Maratha palace fort known for its massive gates and rich history. Built in 1732 by Peshwa Baji Rao I, it was once the seat of the Peshwa empire.",
                "Low",
                "High",
                "Evening",
                18.5196,
                73.8553,
                R.drawable.shaniwar_wada,
                "Visit during evening for light show. Avoid weekends.",
                "The fort is believed to be haunted by the ghost of Narayanrao.",
                "Pune Station (3 km)",
                "Popular"
        );
        p1.setGalleryImages(Arrays.asList(R.drawable.shaniwar_wada, R.drawable.sinhagad, R.drawable.dagadusheth));
        p1.setTopPick(true);
        p1.setDistance(3.0);
        allPlaces.add(p1);

        // 2. Dagdusheth Ganpati -> Spiritual
        Place p2 = new Place(
                "2",
                "Dagdusheth Ganpati",
                "Pune",
                "Spiritual",
                "Famous Ganesha temple known for its gold idol and grand Ganesh festival celebrations.",
                "Low",
                "Very High",
                "Morning",
                18.5165,
                73.8563,
                R.drawable.dagadusheth,
                "Visit early morning to avoid crowd.",
                "The idol is adorned with over 40kg gold.",
                "Pune Station (2.5 km)",
                "Popular"
        );
        p2.setGalleryImages(Arrays.asList(R.drawable.dagadusheth, R.drawable.shaniwar_wada));
        p2.setTopPick(true);
        p2.setDistance(2.5);
        allPlaces.add(p2);

        // 3. Sinhagad Fort -> Adventure
        Place p3 = new Place(
                "3",
                "Sinhagad Fort",
                "Pune",
                "Adventure",
                "Historic fort and trekking destination with panoramic views of the city.",
                "Low",
                "High",
                "Morning",
                18.3663,
                73.7550,
                R.drawable.sinhagad,
                "Start trek early morning.",
                "Known for Battle of Sinhagad.",
                "Pune Station (30 km)",
                "Adventure"
        );
        p3.setGalleryImages(Arrays.asList(R.drawable.sinhagad, R.drawable.khadakwasla));
        p3.setTopPick(true);
        p3.setDistance(30.0);
        allPlaces.add(p3);

        // 4. Khadakwasla Dam -> Nature
        Place p4 = new Place(
                "4",
                "Khadakwasla Dam",
                "Pune",
                "Nature",
                "Scenic dam known for sunset views and peaceful environment. Popular picnic spot.",
                "Low",
                "High",
                "Evening",
                18.4465,
                73.7650,
                R.drawable.khadakwasla,
                "Best during sunset. Avoid late night.",
                "Featured in Bollywood movie scenes.",
                "Shivajinagar (15 km)",
                "Couple"
        );
        p4.setGalleryImages(Arrays.asList(R.drawable.khadakwasla, R.drawable.sinhagad));
        p4.setDistance(15.0);
        allPlaces.add(p4);

        // 5. F.M. Live -> Entertainment
        Place p5 = new Place(
                "5",
                "F.M. Live",
                "Pune",
                "Entertainment",
                "Live radio broadcasting studio where shows are recorded and aired.",
                "Low",
                "Moderate",
                "Day",
                18.5362,
                73.8930,
                R.drawable.fm_live_koregaon_park,
                "Visit during live sessions for a better experience.",
                "Part of BIG FM network, one of India's popular radio stations.",
                "Koregaon Park (1 km)",
                "Popular"
        );
        p5.setGalleryImages(Arrays.asList(R.drawable.fm_live_koregaon_park, R.drawable.timezone_pune));
        p5.setDistance(1.0);
        allPlaces.add(p5);

        // 6. Pune Zoo -> Nature
        Place p6 = new Place(
                "6",
                "Pune Zoo",
                "Pune",
                "Nature",
                "Large zoo with wide variety of animals and snake park.",
                "Low",
                "High",
                "Morning",
                18.5308,
                73.8747,
                R.drawable.rajiv_gandi_zoological_park,
                "Visit early when animals are active.",
                "Known for rare animal species.",
                "Pune Station (6 km)",
                "Family"
        );
        p6.setDistance(6.0);
        allPlaces.add(p6);

        // 7. FC Road -> Food
        Place p7 = new Place(
                "7",
                "FC Road",
                "Pune",
                "Food",
                "Popular street food and shopping destination. Famous for its vibrant atmosphere and various eateries.",
                "Low",
                "Very High",
                "Evening",
                18.5204,
                73.8410,
                R.drawable.fc_road,
                "Try Misal Pav and street snacks.",
                "Heart of Pune's youth culture.",
                "Shivajinagar (1 km)",
                "Budget"
        );
        p7.setDistance(1.0);
        allPlaces.add(p7);

        // 8. Tulshi Baug -> Shopping
        Place p8 = new Place(
                "8",
                "Tulshi Baug",
                "Pune",
                "Shopping",
                "Famous traditional market for clothes, jewelry, and street shopping.",
                "Low",
                "Very High",
                "Afternoon",
                18.5163,
                73.8550,
                R.drawable.tulashi_bag,
                "Bargain properly while shopping.",
                "One of the oldest markets in Pune.",
                "Pune Station (2 km)",
                "Budget"
        );
        p8.setDistance(2.0);
        allPlaces.add(p8);

        // Additional Mappings
        allPlaces.add(new Place("Aga Khan Palace", "Pune", 18.5526, 73.9019, R.drawable.aga_khan_palace, "History", "History"));
        allPlaces.add(new Place("Chaturshringi Temple", "Pune", 18.5360, 73.8440, R.drawable.chaturshringi_temple, "Spiritual", "Spiritual"));
        allPlaces.add(new Place("Empress Garden", "Pune", 18.4966, 73.8728, R.drawable.empress_garden, "Nature", "Nature"));
        allPlaces.add(new Place("German Bakery (Koregaon Park)", "Pune", 18.5362, 73.8930, R.drawable.german_bakery_koregao_park, "Food", "Food"));
        allPlaces.add(new Place("High Street Baner", "Pune", 18.5590, 73.7868, R.drawable.high_street_baner, "Entertainment", "Entertainment"));
        allPlaces.add(new Place("ISKCON Temple", "Pune", 18.5635, 73.9167, R.drawable.iskcon_temple, "Spiritual", "Spiritual"));
        allPlaces.add(new Place("Lal Mahal", "Pune", 18.5195, 73.8553, R.drawable.lal_mahal, "History", "History"));
        allPlaces.add(new Place("Okayama Friendship Garden", "Pune", 18.5007, 73.8587, R.drawable.okyama_friendship_garden, "Nature", "Nature"));
        allPlaces.add(new Place("Parvati Hill", "Pune", 18.4925, 73.8537, R.drawable.parvati_hills, "Nature", "Nature"));
        allPlaces.add(new Place("Pashan Lake", "Pune", 18.5416, 73.8027, R.drawable.pashan_lake, "Nature", "Nature"));
        allPlaces.add(new Place("Phoenix Marketcity (Viman Nagar)", "Pune", 18.5679, 73.9143, R.drawable.phoenix_mall_viman_nagar, "Shopping", "Shopping"));
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
