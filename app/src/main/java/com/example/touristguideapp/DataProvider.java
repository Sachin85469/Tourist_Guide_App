package com.example.touristguideapp;

import java.util.ArrayList;
import java.util.List;

public class DataProvider {

    public static List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();

        places.add(new Place(
                "1",
                "Shaniwar Wada",
                "Pune",
                "history",
                "18th-century Maratha palace fort known for its massive gates and rich history. Built in 1732 by Peshwa Baji Rao I, it was once the seat of the Peshwa empire.",
                "Low",
                "High",
                "Evening",
                18.5196,
                73.8553,
                R.drawable.shaniwar_wada,
                "Visit during evening for light show. Avoid weekends.",
                "The fort is believed to be haunted by the ghost of Narayanrao.",
                "Pune Station (3 km)"
        ));

        places.add(new Place(
                "2",
                "Dagdusheth Ganpati",
                "Pune",
                "spiritual",
                "Famous Ganesha temple known for its gold idol and grand Ganesh festival celebrations.",
                "Low",
                "Very High",
                "Morning",
                18.5165,
                73.8563,
                R.drawable.dagadusheth,
                "Visit early morning to avoid crowd.",
                "The idol is adorned with over 40kg gold.",
                "Pune Station (2.5 km)"
        ));

        places.add(new Place(
                "3",
                "Sinhagad Fort",
                "Pune",
                "adventure",
                "Historic fort and trekking destination with panoramic views of the city.",
                "Low",
                "High",
                "Morning",
                18.3663,
                73.7550,
                R.drawable.sinhagad,
                "Start trek early morning.",
                "Known for Battle of Sinhagad.",
                "Pune Station (30 km)"
        ));

        places.add(new Place(
                "4",
                "Khadakwasla Dam",
                "Pune",
                "nature",
                "Scenic dam known for sunset views and peaceful environment. Popular picnic spot.",
                "Low",
                "High",
                "Evening",
                18.4465,
                73.7650,
                R.drawable.khadakwasla,
                "Best during sunset. Avoid late night.",
                "Featured in Bollywood movie scenes.",
                "Shivajinagar (15 km)"
        ));

        places.add(new Place(
                "5",
                "F.M. Live (BIG FM Studio)",
                "Pune",
                "entertainment",
                "Live radio broadcasting studio where shows are recorded and aired.",
                "Low",
                "Moderate",
                "Day",
                18.5362,
                73.8930,
                R.drawable.fm_live_koregaon_park,
                "Visit during live sessions for a better experience.",
                "Part of BIG FM network, one of India's popular radio stations.",
                "Koregaon Park (1 km)"
        ));

        places.add(new Place(
                "6",
                "Appu Ghar",
                "Pune",
                "adventure",
                "Amusement park with water rides and family entertainment.",
                "Medium",
                "High",
                "Day",
                18.6298,
                73.7890,
                R.drawable.appu_ghar,
                "Visit on weekdays to avoid rush.",
                "One of India's oldest amusement brands.",
                "Pune Junction (8 km)"
        ));

        places.add(new Place(
                "7",
                "Kelkar Museum",
                "Pune",
                "history",
                "Museum with large collection of Indian artifacts and Mastani Mahal.",
                "Low",
                "Moderate",
                "Morning",
                18.5107,
                73.8530,
                R.drawable.raja_dinkar_kelkar_museum,
                "Hire guide for better understanding.",
                "Contains over 20000 artifacts.",
                "Pune Station (2 km)"
        ));

        places.add(new Place(
                "8",
                "Time Zone Mall",
                "Pune",
                "entertainment",
                "Indoor arcade with bowling, VR, and games.",
                "High",
                "High",
                "Evening",
                18.5610,
                73.9160,
                R.drawable.timezone_pune,
                "Buy combo game cards.",
                "International gaming brand.",
                "Pune Station (9 km)"
        ));

        places.add(new Place(
                "9",
                "Pune Zoo",
                "Pune",
                "nature",
                "Large zoo with wide variety of animals and snake park.",
                "Low",
                "High",
                "Morning",
                18.5308,
                73.8747,
                R.drawable.rajiv_gandi_zoological_park,
                "Visit early when animals are active.",
                "Known for rare animal species.",
                "Pune Station (6 km)"
        ));

        places.add(new Place(
                "10",
                "Tulshi Baug",
                "Pune",
                "shopping",
                "Famous traditional market for clothes, jewelry, and street shopping.",
                "Low",
                "Very High",
                "Afternoon",
                18.5163,
                73.8550,
                R.drawable.tulashi_bag,
                "Bargain properly while shopping.",
                "One of the oldest markets in Pune.",
                "Pune Station (2 km)"
        ));

        return places;
    }
}
