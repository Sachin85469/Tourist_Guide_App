package com.example.touristguideapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProvider {

    public static List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();

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
        places.add(p1);

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
        places.add(p2);

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
        places.add(p3);

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
        places.add(p4);

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
        places.add(p5);

        // 6. Pune Zoo -> Nature
        places.add(new Place(
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
        ));

        // 7. FC Road -> Food
        places.add(new Place(
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
        ));

        // 8. Tulshi Baug -> Shopping
        places.add(new Place(
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
        ));

        // Additional Mappings
        places.add(new Place("Aga Khan Palace", "Pune", 18.5526, 73.9019, R.drawable.aga_khan_palace, "History", "History"));
        places.add(new Place("Chaturshringi Temple", "Pune", 18.5360, 73.8440, R.drawable.chaturshringi_temple, "Spiritual", "Spiritual"));
        places.add(new Place("Empress Garden", "Pune", 18.4966, 73.8728, R.drawable.empress_garden, "Nature", "Nature"));
        places.add(new Place("German Bakery (Koregaon Park)", "Pune", 18.5362, 73.8930, R.drawable.german_bakery_koregao_park, "Food", "Food"));
        places.add(new Place("High Street Baner", "Pune", 18.5590, 73.7868, R.drawable.high_street_baner, "Entertainment", "Entertainment"));
        places.add(new Place("ISKCON Temple", "Pune", 18.5635, 73.9167, R.drawable.iskcon_temple, "Spiritual", "Spiritual"));
        places.add(new Place("Lal Mahal", "Pune", 18.5195, 73.8553, R.drawable.lal_mahal, "History", "History"));
        places.add(new Place("Okayama Friendship Garden", "Pune", 18.5007, 73.8587, R.drawable.okyama_friendship_garden, "Nature", "Nature"));
        places.add(new Place("Parvati Hill", "Pune", 18.4925, 73.8537, R.drawable.parvati_hills, "Nature", "Nature"));
        places.add(new Place("Pashan Lake", "Pune", 18.5416, 73.8027, R.drawable.pashan_lake, "Nature", "Nature"));
        places.add(new Place("Phoenix Marketcity (Viman Nagar)", "Pune", 18.5679, 73.9143, R.drawable.phoenix_mall_viman_nagar, "Shopping", "Shopping"));

        return places;
    }

    public static List<Place> getAllPlaces() {
        return getPlaces();
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
