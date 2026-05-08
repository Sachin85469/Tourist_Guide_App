package com.arriva.touristguideapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private List<Integer> galleryImages = new ArrayList<>(); // Additional gallery images
    
    // New Fields
    private String tips = "";
    private String funFact = "";
    private String nearestStation = "";
    private String tag = "Popular"; // Default tag
    private double distance = -1.0; // Distance from user in km
    private boolean isTopPick = false;

    /**
     * Comprehensive Constructor including all fields.
     */
    public Place(String id, String name, String city, String category, String description, 
                 String budget, String crowdLevel, String bestTime, 
                 double latitude, double longitude, int imageResId,
                 String tips, String funFact, String nearestStation, String tag) {
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
        this.galleryImages.add(imageResId); // Add main image as first in gallery
        this.tips = tips;
        this.funFact = funFact;
        this.nearestStation = nearestStation;
        this.tag = tag;
    }

    /**
     * Constructor for existing code support.
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
        this.galleryImages.add(imageResId);
    }

    /**
     * Simplified Constructor for quick additions.
     */
    public Place(String name, String city, double latitude, double longitude, int imageResId, String category, String tag) {
        this.id = String.valueOf(name.hashCode());
        this.name = name;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageResId = imageResId;
        this.galleryImages.add(imageResId);
        this.category = category;
        this.tag = tag;
        this.description = "A beautiful place to visit in " + city;
        this.budget = "Medium";
        this.crowdLevel = "Moderate";
        this.bestTime = "Morning/Evening";
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
        this.galleryImages.add(imageResId);
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
    public String getTag() { return tag; }
    
    // Alias getters
    public double getLat() { return latitude; }
    public double getLng() { return longitude; }
    public int getImage() { return imageResId; }
    
    // New Getters
    public String getTips() { return tips; }
    public String getFunFact() { return funFact; }
    public String getNearestStation() { return nearestStation; }

    public List<Integer> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<Integer> galleryImages) {
        this.galleryImages = galleryImages;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean isTopPick() {
        return isTopPick;
    }

    public void setTopPick(boolean topPick) {
        isTopPick = topPick;
    }
}
