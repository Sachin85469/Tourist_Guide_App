package com.arriva.touristguideapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a tourist place.
 * Scalable to include many cities and categories.
 * Updated to exclusively use remote (Firestore) image URLs.
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
    
    // New Fields
    private String tips = "";
    private String funFact = "";
    private String nearestStation = "";
    private String tag = "Popular"; // Default tag
    private double distance = -1.0; // Distance from user in km
    private boolean isTopPick = false;

    /** HTTPS image URL from Firestore. */
    @Nullable
    private String imageUrl;

    /** Ordered remote gallery URLs from Firestore. */
    @NonNull
    private List<String> galleryImageUrls = new ArrayList<>();

    /** Optional stable category key from Firestore (e.g. {@code history}). */
    @Nullable
    private String categoryId;

    /** Firestore catalog lifecycle (e.g. published); optional for local static rows. */
    @Nullable
    private String catalogStatus;

    /** Optional id from a prior static catalog for migration tooling. */
    @Nullable
    private String legacyCatalogId;

    public Place() {
        // Required for Firestore serialization
    }

    /**
     * Minimal constructor for Firestore-bound objects.
     */
    public Place(String id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    /**
     * Comprehensive Constructor including most fields (excluding legacy drawable fields).
     */
    public Place(String id, String name, String city, String category, String description, 
                 String budget, String crowdLevel, String bestTime, 
                 double latitude, double longitude,
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
        this.tips = tips;
        this.funFact = funFact;
        this.nearestStation = nearestStation;
        this.tag = tag;
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
    public String getTag() { return tag; }
    
    // Alias getters
    public double getLat() { return latitude; }
    public double getLng() { return longitude; }
    
    // New Getters
    public String getTips() { return tips; }
    public String getFunFact() { return funFact; }
    public String getNearestStation() { return nearestStation; }

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

    public void setRating(double rating) {
        this.rating = rating;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    public List<String> getGalleryImageUrls() {
        return galleryImageUrls;
    }

    public void setGalleryImageUrls(@NonNull List<String> galleryImageUrls) {
        this.galleryImageUrls = new ArrayList<>(galleryImageUrls);
    }

    /** Same backing list as {@link #getGalleryImageUrls()} / Firestore {@code galleryUrls}. */
    @NonNull
    public List<String> getGalleryUrls() {
        return galleryImageUrls;
    }

    public void setGalleryUrls(@NonNull List<String> galleryUrls) {
        setGalleryImageUrls(galleryUrls);
    }

    @Nullable
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@Nullable String categoryId) {
        this.categoryId = categoryId;
    }

    @Nullable
    public String getCatalogStatus() {
        return catalogStatus;
    }

    public void setCatalogStatus(@Nullable String catalogStatus) {
        this.catalogStatus = catalogStatus;
    }

    @Nullable
    public String getLegacyCatalogId() {
        return legacyCatalogId;
    }

    public void setLegacyCatalogId(@Nullable String legacyCatalogId) {
        this.legacyCatalogId = legacyCatalogId;
    }

    /** True when a remote hero URL is present. */
    public boolean hasRemoteHeroImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    /** True when any remote gallery URL entries exist. */
    public boolean hasRemoteGalleryImages() {
        return galleryImageUrls != null && !galleryImageUrls.isEmpty();
    }
}
