package com.example.touristguideapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides dummy data for the Tourist Guide application.
 */
public class DataProvider {

    public static List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();

        // Historical Places
        places.add(new Place(
                "p1", "Sinhagad Fort", "Pune", "historical",
                "A historic hill fortress with a great trekking experience and panoramic views of the city. Famous for its local Pithla-Bhakri food.",
                "Low", "High", "Monsoon", 18.3663, 73.7559, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p2", "Shaniwar Wada", "Pune", "historical",
                "The 18th-century seat of the Peshwas. A symbol of Maratha heritage with a beautiful garden and a sound-and-light show.",
                "Low", "Moderate", "Winter", 18.5194, 73.8553, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p3", "Aga Khan Palace", "Pune", "historical",
                "Built in 1892, this palace is a majestic building with Italian arches. It is a memorial to Mahatma Gandhi and Kasturba Gandhi.",
                "Medium", "Low", "Post-Monsoon", 18.5523, 73.9015, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p4", "Pataleshwar Caves", "Pune", "historical",
                "An 8th-century rock-cut cave temple dedicated to Lord Shiva. It is carved out of a single enormous basalt rock.",
                "Low", "Low", "All year", 18.5276, 73.8504, android.R.drawable.ic_menu_gallery
        ));

        // Nature Places
        places.add(new Place(
                "p5", "Mulshi Dam", "Pune", "nature",
                "A serene lake surrounded by lush green Sahyadri mountains. An ideal spot for day trips, camping, and monsoon photography.",
                "Low", "Moderate", "Monsoon", 18.5034, 73.5123, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p6", "Vetal Tekdi", "Pune", "nature",
                "The highest point in Pune city. Popular among locals for morning walks, bird watching, and stunning sunset views.",
                "Low", "Moderate", "All year", 18.5283, 73.8247, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p7", "Pashan Lake", "Pune", "nature",
                "A calm man-made lake that attracts several migratory birds during winter. Perfect for nature lovers and photographers.",
                "Low", "Low", "Winter", 18.5367, 73.7844, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p8", "Saras Baug", "Pune", "nature",
                "A historic park featuring a beautiful pond and a famous Ganpati temple. Great for family outings in the evening.",
                "Low", "High", "All year", 18.4998, 73.8473, android.R.drawable.ic_menu_gallery
        ));

        // Food Places
        places.add(new Place(
                "p9", "Vaishali Restaurant", "Pune", "food",
                "Located on the iconic FC Road, it is legendary for its South Indian breakfast, specifically Mysore Masala Dosa and Filter Coffee.",
                "Medium", "High", "Morning", 18.5255, 73.8415, android.R.drawable.ic_menu_gallery
        ));

        places.add(new Place(
                "p10", "Kayani Bakery", "Pune", "food",
                "Oldest and most famous Parsi bakery in Pune. Renowned for its world-famous Shrewsbury biscuits and milk bread.",
                "Low", "High", "All year", 18.5196, 73.8767, android.R.drawable.ic_menu_gallery
        ));

        return places;
    }
}
