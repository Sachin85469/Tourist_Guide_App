package com.example.touristguideapp;

import java.io.Serializable;

/**
 * Data model for a tourist place.
 * Scalable to include many cities and categories.
 */
public class Place implements Serializable {
    private String id;
    private String name;
    private String city;
    private String category;
    private String description;
    private String budget;      // Low, Medium, High
    private String crowdLevel;  // Low, Moderate, High
    private String bestTime;
    private double latitude;
    private double longitude;
    private double rating = 4.0; // Default rating
    private int imageResId;     // Local drawable resource ID
    
    // New Fields
    private String tips = "";
    private String funFact = "";
    private String nearestStation = "";

    /**
     * New Constructor including all requested fields.
     */
    public Place(String id, String name, String city, String category, String description, 
                 String budget, String crowdLevel, String bestTime, 
                 double latitude, double longitude, int imageResId,
                 String tips, String funFact, String nearestStation) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.category = category;
        this.description = description;
        this.budget = budget;
        this.crowdLevel = crowdLevel;
        this.bestTime = bestTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageResId = imageResId;
        this.tips = tips;
        this.funFact = funFact;
        this.nearestStation = nearestStation;
    }

    /**
     * Old Constructor to support existing code.
     */
    public Place(String id, String name, String city, String category, String description, 
                 String budget, String crowdLevel, String bestTime, 
                 double latitude, double longitude, int imageResId) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.category = category;
        this.description = description;
        this.budget = budget;
        this.crowdLevel = crowdLevel;
        this.bestTime = bestTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageResId = imageResId;
    }

    /**
     * Legacy Constructor including Rating.
     */
    public Place(String id, String name, String city, String category, String description, 
                 String budget, String crowdLevel, String bestTime, 
                 double latitude, double longitude, double rating, int imageResId) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.category = category;
        this.description = description;
        this.budget = budget;
        this.crowdLevel = crowdLevel;
        this.bestTime = bestTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.imageResId = imageResId;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getBudget() { return budget; }
    public String getCrowdLevel() { return crowdLevel; }
    public String getBestTime() { return bestTime; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getRating() { return rating; }
    public int getImageResId() { return imageResId; }
    
    // Alias getters for shorter access
    public double getLat() { return latitude; }
    public double getLng() { return longitude; }
    public int getImage() { return imageResId; }
    
    // New Getters
    public String getTips() { return tips; }
    public String getFunFact() { return funFact; }
    public String getNearestStation() { return nearestStation; }
}
