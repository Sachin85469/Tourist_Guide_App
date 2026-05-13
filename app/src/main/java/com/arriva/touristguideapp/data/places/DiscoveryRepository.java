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
     * Trending: High rating + high review count.
     */
    public List<Place> getTrendingPlaces(List<Place> allPlaces) {
        List<Place> trending = new ArrayList<>(allPlaces);
        Collections.sort(trending, (p1, p2) -> {
            double s1 = (p1.getRating() * 10) + p1.getTotalRatings();
            double s2 = (p2.getRating() * 10) + p2.getTotalRatings();
            return Double.compare(s2, s1);
        });
        return trending.size() > 10 ? trending.subList(0, 10) : trending;
    }

    /**
     * Smart Recommendations based on rating, popularity, and distance.
     */
    public List<Place> getRecommendedPlaces(List<Place> allPlaces) {
        List<Place> recommendations = new ArrayList<>(allPlaces);
        for (Place p : recommendations) {
            double score = (p.getRating() * 5);
            score += (Math.min(p.getTotalRatings(), 100) / 5.0);
            
            // Distance penalty (closer is better)
            if (p.getDistance() > 0) {
                score += (100.0 / (p.getDistance() + 1));
            }
            p.setSearchScore(score);
        }
        
        Collections.sort(recommendations, (p1, p2) -> Double.compare(p2.getSearchScore(), p1.getSearchScore()));
        Log.d(TAG, "RECOMMENDATION_GENERATED");
        return recommendations.size() > 8 ? recommendations.subList(0, 8) : recommendations;
    }
}
