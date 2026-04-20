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
                "historical",
                "18th-century Maratha palace fort known for its massive gates and rich history. Built in 1732 by Peshwa Baji Rao I, it was once the seat of the Peshwa empire.",
                "Low",
                "High",
                "Evening",
                18.5196,
                73.8554,
                R.drawable.shaniwar_wada,
                "Visit during evening for light show. Avoid weekends.",
                "The fort is believed to be haunted by the ghost of Narayanrao.",
                "Pune Station (3 km)"
        ));

        places.add(new Place(
                "2",
                "Dagadusheth Halwai Ganapati Temple",
                "Pune",
                "spiritual",
                "Famous Ganesha temple known for its gold idol and grand Ganesh festival celebrations.",
                "Low",
                "Very High",
                "Morning",
                18.5163,
                73.8538,
                R.drawable.dagadusheth,
                "Visit early morning to avoid crowd.",
                "The idol is adorned with over 40kg gold.",
                "Pune Station (2.5 km)"
        ));

        places.add(new Place(
                "3",
                "Khadakwasla Dam",
                "Pune",
                "nature",
                "Scenic dam known for sunset views and peaceful environment. Popular picnic spot.",
                "Low",
                "High",
                "Evening",
                18.4239,
                73.7615,
                R.drawable.khadakwasla,
                "Best during sunset. Avoid late night.",
                "Featured in Bollywood movie scenes.",
                "Shivajinagar (15 km)"
        ));

        places.add(new Place(
                "4",
                "Tulshi Baug",
                "Pune",
                "shopping",
                "Famous traditional market for clothes, jewelry, and street shopping.",
                "Low",
                "Very High",
                "Afternoon",
                18.5147,
                73.8551,
                R.drawable.tulashi_bag,
                "Bargain properly while shopping.",
                "One of the oldest markets in Pune.",
                "Pune Station (2 km)"
        ));

        places.add(new Place(
                "5",
                "Sinhagad Fort",
                "Pune",
                "adventure",
                "Historic fort and trekking destination with panoramic views of the city.",
                "Low",
                "High",
                "Morning",
                18.3659,
                73.7467,
                R.drawable.sinhagad,
                "Start trek early morning.",
                "Known for Battle of Sinhagad.",
                "Pune Station (30 km)"
        ));

        places.add(new Place(
                "6",
                "Appu Ghar",
                "Pune",
                "entertainment",
                "Amusement park with water rides and family entertainment.",
                "Medium",
                "High",
                "Day",
                18.5355,
                73.8907,
                R.drawable.appu_ghar,
                "Visit on weekdays to avoid rush.",
                "One of India's oldest amusement brands.",
                "Pune Junction (8 km)"
        ));

        places.add(new Place(
                "7",
                "Raja Dinkar Kelkar Museum",
                "Pune",
                "cultural",
                "Museum with large collection of Indian artifacts and Mastani Mahal.",
                "Low",
                "Moderate",
                "Morning",
                18.5136,
                73.8492,
                R.drawable.raja_dinkar_kelkar_museum,
                "Hire guide for better understanding.",
                "Contains over 20000 artifacts.",
                "Pune Station (2 km)"
        ));

        places.add(new Place(
                "8",
                "Timezone Phoenix Mall",
                "Pune",
                "gaming",
                "Indoor arcade with bowling, VR, and games.",
                "High",
                "High",
                "Evening",
                18.5622,
                73.9006,
                R.drawable.timezone_pune,
                "Buy combo game cards.",
                "International gaming brand.",
                "Pune Station (9 km)"
        ));

        places.add(new Place(
                "9",
                "F.M. Live Koregaon Park",
                "Pune",
                "entertainment",
                "Popular nightlife spot with karaoke, comedy, and live music.",
                "High",
                "Very High",
                "Night",
                18.5362,
                73.8868,
                R.drawable.fm_live_koregaon_park,
                "Book in advance for weekends.",
                "Hosts live performances.",
                "Pune Station (4.5 km)"
        ));

        places.add(new Place(
                "10",
                "Rajiv Gandhi Zoological Park",
                "Pune",
                "nature",
                "Large zoo with wide variety of animals and snake park.",
                "Low",
                "High",
                "Morning",
                18.5098,
                73.8534,
                R.drawable.rajiv_gandi_zoological_park,
                "Visit early when animals are active.",
                "Known for rare animal species.",
                "Pune Station (6 km)"
        ));

        return places;
    }
}
