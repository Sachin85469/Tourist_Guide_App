package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.arriva.touristguideapp.data.analytics.AnalyticsRepository;
import java.util.List;
import java.util.Map;

public class AdminAnalyticsFragment extends Fragment {

    private TextView tvMostViewed, tvTopRated, tvTrendingSearches;
    private AnalyticsRepository analyticsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_analytics, container, false);
        tvMostViewed = view.findViewById(R.id.tvMostViewed);
        tvTopRated = view.findViewById(R.id.tvTopRated);
        tvTrendingSearches = view.findViewById(R.id.tvTrendingSearches);
        
        analyticsRepository = new AnalyticsRepository();
        loadAnalytics();
        
        return view;
    }

    private void loadAnalytics() {
        analyticsRepository.getMostViewedPlaces(5, data -> {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> item : data) {
                sb.append("• ").append(item.get("placeId"))
                  .append(": ").append(item.get("totalViews")).append(" views\n");
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvMostViewed.setText(sb.toString()));
            }
        });

        // Simplified Top Rated from PlaceRepository logic
        new com.arriva.touristguideapp.data.places.PlaceRepository().fetchPublishedPlaces((places, origin, message) -> {
            java.util.Collections.sort(places, (p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(5, places.size());
            for (int i = 0; i < limit; i++) {
                Place p = places.get(i);
                sb.append("• ").append(p.getName()).append(": ").append(p.getRating()).append(" ⭐\n");
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvTopRated.setText(sb.toString()));
            }
        });
        
        // Trending searches - placeholders
        tvTrendingSearches.setText("• Forts in Pune\n• Street food\n• Weekend getaways");
    }
}
