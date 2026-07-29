package com.arriva.touristguideapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a tourist place.
 * Scalable to include many cities and categories.
 * Images are stored as Firebase Storage references, never as manually entered URLs.
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
    private double rating = 0.0; // Average rating from reviews
    private long totalRatings = 0;
    private long totalComments = 0;
    
    // New Fields
    private String tips = "";
    private String funFact = "";
    private String nearestStation = "";
    private String tag = "Popular"; // Default tag
    private double distance = -1.0; // Distance from user in km
    private boolean isTopPick = false;
    private double searchScore = 0.0; // Runtime score for search/ranking
    private String catalogStatus = "published"; // Status: draft, published, archived
    private String categoryId;
    private String legacyCatalogId;
    private long viewedAt; // Timestamp for recently viewed (Requirement)
    /** Firebase Storage path created by the place-image uploader, e.g. places/{placeId}/cover.jpg. */
    private String imageRef;
    /** Additional Firebase Storage paths created by the uploader. */
    private List<String> galleryImageRefs = new ArrayList<>();

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
    public long getTotalRatings() { return totalRatings; }
    public long getTotalComments() { return totalComments; }
    public String getTag() { return tag; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setBudget(String budget) { this.budget = budget; }
    public void setCrowdLevel(String crowdLevel) { this.crowdLevel = crowdLevel; }
    public void setBestTime(String bestTime) { this.bestTime = bestTime; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTag(String tag) { this.tag = tag; }
    public void setTips(String tips) { this.tips = tips; }
    public void setFunFact(String funFact) { this.funFact = funFact; }
    public void setNearestStation(String nearestStation) { this.nearestStation = nearestStation; }

    // Alias getters
    public double getLat() { return latitude; }
    public double getLng() { return longitude; }
    
    // New Getters
    public String getTips() { return tips; }
    public String getFunFact() { return funFact; }
    public String getNearestStation() { return nearestStation; }

    public String getCatalogStatus() {
        return catalogStatus;
    }

    public void setCatalogStatus(String catalogStatus) {
        this.catalogStatus = catalogStatus;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getLegacyCatalogId() {
        return legacyCatalogId;
    }

    public void setLegacyCatalogId(String legacyCatalogId) {
        this.legacyCatalogId = legacyCatalogId;
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

    public double getSearchScore() {
        return searchScore;
    }

    public void setSearchScore(double searchScore) {
        this.searchScore = searchScore;
    }

    public long getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(long viewedAt) {
        this.viewedAt = viewedAt;
    }

    /**
     * The stored Firebase Storage reference for the cover image. This is a path/reference, not a
     * download URL, so access tokens and generated URLs never become catalog data.
     */
    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = normalizeImageRef(imageRef);
    }

    @NonNull
    public List<String> getGalleryImageRefs() {
        return new ArrayList<>(galleryImageRefs == null ? java.util.Collections.emptyList() : galleryImageRefs);
    }

    public void setGalleryImageRefs(List<String> galleryImageRefs) {
        this.galleryImageRefs = sanitizeImageRefs(galleryImageRefs);
    }

    /** Returns the cover first followed by unique gallery references. */
    @NonNull
    public List<String> getAllImageRefs() {
        java.util.LinkedHashSet<String> refs = new java.util.LinkedHashSet<>();
        String cover = normalizeImageRef(imageRef);
        if (cover != null) {
            refs.add(cover);
        }
        refs.addAll(sanitizeImageRefs(galleryImageRefs));
        return new ArrayList<>(refs);
    }

    private static String normalizeImageRef(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private static List<String> sanitizeImageRefs(List<String> refs) {
        java.util.LinkedHashSet<String> cleaned = new java.util.LinkedHashSet<>();
        if (refs != null) {
            for (String ref : refs) {
                String normalized = normalizeImageRef(ref);
                if (normalized != null) {
                    cleaned.add(normalized);
                }
            }
        }
        return new ArrayList<>(cleaned);
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setTotalRatings(long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Double.compare(place.latitude, latitude) == 0 &&
                Double.compare(place.longitude, longitude) == 0 &&
                Double.compare(place.rating, rating) == 0 &&
                totalRatings == place.totalRatings &&
                totalComments == place.totalComments &&
                java.util.Objects.equals(id, place.id) &&
                java.util.Objects.equals(name, place.name) &&
                java.util.Objects.equals(city, place.city) &&
                java.util.Objects.equals(category, place.category) &&
                java.util.Objects.equals(description, place.description) &&
                java.util.Objects.equals(budget, place.budget) &&
                java.util.Objects.equals(crowdLevel, place.crowdLevel) &&
                java.util.Objects.equals(bestTime, place.bestTime) &&
                java.util.Objects.equals(imageRef, place.imageRef) &&
                java.util.Objects.equals(galleryImageRefs, place.galleryImageRefs);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, city, category, description, budget, crowdLevel, bestTime, latitude, longitude, rating, totalRatings, totalComments, imageRef, galleryImageRefs);
    }
}
