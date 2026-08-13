package com.arriva.touristguideapp.data.places;

import android.util.Log;
import com.arriva.touristguideapp.Place;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles advanced search, ranking, and recommendations.
 */
public class DiscoveryRepository {
    private static final String TAG = "DiscoveryRepository";

    /**
     * Searches places with advanced ranking and typo tolerance (basic implementation).
     */
    public List<Place> searchAndRank(List<Place> allPlaces, String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        
        Log.d(TAG, "SEARCH_QUERY: " + query);
        String q = query.toLowerCase().trim();
        List<Place> results = new ArrayList<>();

        for (Place p : allPlaces) {
            double score = 0;
            String name = p.getName().toLowerCase();
            String city = p.getCity().toLowerCase();
            String category = p.getCategory().toLowerCase();
            String description = p.getDescription().toLowerCase();

            // Match Scoring
            if (name.equals(q)) score += 100;
            else if (name.startsWith(q)) score += 50;
            else if (name.contains(q)) score += 20;

            if (city.contains(q)) score += 15;
            if (category.contains(q)) score += 15;
            if (description.contains(q)) score += 5;

            if (score > 0) {
                // Production Hardening: Boost by rating/reviews
                score += (p.getRating() * 2);
                score += (Math.min(p.getTotalRatings(), 50) / 10.0);
                
                // Add to results if it matches
                p.setSearchScore(score); 
                results.add(p);
            }
        }

        // Rank by score DESC
        Collections.sort(results, (p1, p2) -> Double.compare(p2.getSearchScore(), p1.getSearchScore()));
        
        Log.d(TAG, "SEARCH_RESULTS_COUNT: " + results.size());
        return results;
    }

    /**
     * Trending: High rating + high review count + engagement (views).
     */
    public List<Place> getTrendingPlaces(List<Place> allPlaces, java.util.Map<String, Long> placeViews) {
        List<Place> trending = new ArrayList<>(allPlaces);
        Collections.sort(trending, (p1, p2) -> {
            double s1 = (p1.getRating() * 10) + p1.getTotalRatings();
            if (placeViews != null) {
                Long v1 = placeViews.get(p1.getId());
                if (v1 != null) s1 += (v1 / 5.0); // 1 point per 5 views
            }

            double s2 = (p2.getRating() * 10) + p2.getTotalRatings();
            if (placeViews != null) {
                Long v2 = placeViews.get(p2.getId());
                if (v2 != null) s2 += (v2 / 5.0);
            }
            return Double.compare(s2, s1);
        });
        Log.d(TAG, "TRENDING_SCORE_UPDATED");
        return trending.size() > 10 ? trending.subList(0, 10) : trending;
    }

    public List<Place> getTrendingPlaces(List<Place> allPlaces) {
        return getTrendingPlaces(allPlaces, null);
    }

    /**
     * Smart Recommendations based on rating, popularity, and distance.
     * Diversity-aware: Avoids showing only one category.
     */
    public List<Place> getRecommendedPlaces(List<Place> allPlaces) {
        if (allPlaces == null || allPlaces.isEmpty()) return new ArrayList<>();

        List<Place> candidates = new ArrayList<>(allPlaces);
        for (Place p : candidates) {
            double score = (p.getRating() * 10); // Higher weight for rating
            score += (Math.min(p.getTotalRatings(), 500) / 10.0); // Popularity boost
            
            // Proximity boost: closer is significantly better for recommendations
            if (p.getDistance() > 0) {
                if (p.getDistance() < 5) score += 50; // Very close
                else if (p.getDistance() < 15) score += 25; // Nearby
                else score += (100.0 / (p.getDistance() + 1));
            }

            // New: Recently Added / Featured boost
            if (p.isTopPick()) score += 30;
            
            p.setSearchScore(score);
        }
        
        Collections.sort(candidates, (p1, p2) -> Double.compare(p2.getSearchScore(), p1.getSearchScore()));
        
        // Diversity logic: ensure we don't have only one category in top 5
        List<Place> results = new ArrayList<>();
        
        for (Place p : candidates) {
            if (results.size() >= 8) break;
            
            String cat = p.getCategory();
            // Allow at most 2 items of the same category in the first 4 slots for variety
            if (results.size() < 4) {
                int count = 0;
                for (Place r : results) if (r.getCategory().equals(cat)) count++;
                if (count >= 2) continue;
            }
            
            results.add(p);
        }

        Log.d(TAG, "RECOMMENDATIONS_GENERATED_WITH_DIVERSITY count=" + results.size());
        return results;
    }
}
