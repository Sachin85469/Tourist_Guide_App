package com.arriva.touristguideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mood / Vibe screen — Phase 4 real-engine rewrite.
 *
 * <p>On open it silently:
 * <ol>
 *   <li>Reads the device hour from the local clock.</li>
 *   <li>Gets the last known GPS location via FusedLocationProviderClient.</li>
 *   <li>Calls the OpenWeatherMap free API through {@link WeatherFetcher}.</li>
 *   <li>Passes hour + weatherMain to {@link MoodEngine} and pre-selects the suggested chips.</li>
 * </ol>
 *
 * <p>The user can still tap any chip to toggle it on/off before pressing "Find Places".
 */
public class MoodActivity extends BaseActivity {

    private static final String TAG = "MoodActivity";
    private static final int REQUEST_LOCATION = 201;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private TextView tvWeatherHint;
    private TextView tvSuggestionReason;
    private View     cardSuggestionHint;

    // ── Chip views ────────────────────────────────────────────────────────────
    private Map<String, TextView> chipMap = new HashMap<>();

    // ── State ─────────────────────────────────────────────────────────────────
    private final Set<String> selectedCategories = new HashSet<>();
    private FusedLocationProviderClient fusedLocationClient;

    // Hour cached early so the weather callback can still use it
    private int currentHour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        // ── Bind views ────────────────────────────────────────────────────────
        tvWeatherHint     = findViewById(R.id.tvWeatherHint);
        tvSuggestionReason = findViewById(R.id.tvSuggestionReason);
        cardSuggestionHint = findViewById(R.id.cardSuggestionHint);

        View btnBack = findViewById(R.id.btnMoodBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ── Register chips ────────────────────────────────────────────────────
        bindChip(R.id.chipMoodHistorical, MoodEngine.CAT_HISTORICAL);
        bindChip(R.id.chipMoodNature,     MoodEngine.CAT_NATURE);
        bindChip(R.id.chipMoodReligious,  MoodEngine.CAT_RELIGIOUS);
        bindChip(R.id.chipMoodFood,       MoodEngine.CAT_FOOD);
        bindChip(R.id.chipMoodCulture,    MoodEngine.CAT_CULTURE);
        bindChip(R.id.chipMoodAdventure,  MoodEngine.CAT_ADVENTURE);
        bindChip(R.id.chipMoodScenic,     MoodEngine.CAT_SCENIC);
        bindChip(R.id.chipMoodShopping,   MoodEngine.CAT_SHOPPING);

        // ── "Find Places" button ──────────────────────────────────────────────
        View btnFind = findViewById(R.id.btnFindPlaces);
        if (btnFind != null) {
            btnFind.setOnClickListener(v -> openCategoryPlaces());
        }

        // ── Start async engine ────────────────────────────────────────────────
        loadWeatherAndSuggest();
    }

    // ─── Engine ───────────────────────────────────────────────────────────────

    /**
     * Phase 1 of the suggestion pipeline: get GPS then fetch weather.
     * Everything degrades gracefully — no crash if location/weather unavailable.
     */
    private void loadWeatherAndSuggest() {
        // Show time-based defaults immediately while we wait for location + weather
        applySuggestions(currentHour, null);

        if (tvWeatherHint != null) {
            tvWeatherHint.setText("Detecting weather…");
        }

        // Try to get GPS location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationThenWeather();
        } else {
            // No permission — fall back immediately to time-only suggestions
            Log.d(TAG, "Location permission not granted, using time-only suggestions");
            applyTimeOnlySuggestions();
        }
    }

    private void fetchLocationThenWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            applyTimeOnlySuggestions();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Log.d(TAG, "Got location: " + location.getLatitude() + "," + location.getLongitude());
                            String apiKey = BuildConfig.OWM_API_KEY;
                            WeatherFetcher.fetch(this, location.getLatitude(), location.getLongitude(),
                                    apiKey, weatherMain -> {
                                        Log.d(TAG, "Weather result: " + weatherMain);
                                        applySuggestions(currentHour, weatherMain);
                                    });
                        } else {
                            Log.d(TAG, "Last location null, using time-only suggestions");
                            applyTimeOnlySuggestions();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Location fetch failed: " + e.getMessage());
                        applyTimeOnlySuggestions();
                    });
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission revoked before weather lookup", e);
            applyTimeOnlySuggestions();
        }
    }

    private void applyTimeOnlySuggestions() {
        applySuggestions(currentHour, null);
        if (tvWeatherHint != null) {
            tvWeatherHint.setText("Using local time for suggestions");
        }
    }

    /**
     * Receives engine output and updates the UI: pre-selects chips + updates hint text.
     */
    private void applySuggestions(int hour, String weatherMain) {
        List<String> suggested = MoodEngine.getSuggestedCategories(hour, weatherMain);
        String reason = MoodEngine.getSuggestionReason(hour, weatherMain);

        // Clear all first, then select suggested ones
        for (Map.Entry<String, TextView> entry : chipMap.entrySet()) {
            setChipSelected(entry.getValue(), entry.getKey(), false);
        }
        selectedCategories.clear();

        for (String cat : suggested) {
            TextView chip = chipMap.get(cat);
            if (chip != null) {
                setChipSelected(chip, cat, true);
            }
        }

        // Update hint card
        if (tvSuggestionReason != null) {
            tvSuggestionReason.setText(reason);
        }

        // Update weather hint if we have real weather
        if (weatherMain != null && tvWeatherHint != null) {
            tvWeatherHint.setText(getWeatherEmoji(weatherMain) + "  " + weatherMain + " · " + formatHour(currentHour));
        } else if (tvWeatherHint != null && !tvWeatherHint.getText().toString().startsWith("Detect")) {
            // Already set by applyTimeOnlySuggestions
        } else if (tvWeatherHint != null) {
            tvWeatherHint.setText(formatHour(currentHour));
        }
    }

    // ─── Chip helpers ─────────────────────────────────────────────────────────

    private void bindChip(int viewId, String category) {
        TextView chip = findViewById(viewId);
        if (chip == null) return;
        chipMap.put(category, chip);
        chip.setOnClickListener(v -> toggleChip(chip, category));
    }

    private void toggleChip(TextView chip, String category) {
        boolean nowSelected = !chip.isSelected();
        setChipSelected(chip, category, nowSelected);

        // Subtle bounce animation
        chip.animate()
            .scaleX(0.92f).scaleY(0.92f).setDuration(80)
            .withEndAction(() ->
                chip.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            ).start();
    }

    private void setChipSelected(TextView chip, String category, boolean selected) {
        chip.setSelected(selected);
        if (selected) {
            selectedCategories.add(category);
        } else {
            selectedCategories.remove(category);
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void openCategoryPlaces() {
        if (selectedCategories.isEmpty()) {
            // Nothing selected — show all places
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "All");
            startActivity(intent);
        } else if (selectedCategories.size() == 1) {
            // Single category — use existing single-category path
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", selectedCategories.iterator().next());
            startActivity(intent);
        } else {
            // Multiple categories — use new multi-category path
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putStringArrayListExtra("categories", new ArrayList<>(selectedCategories));
            startActivity(intent);
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ─── Permission handling ──────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationThenWeather();
        }
    }

    // ─── Formatting helpers ───────────────────────────────────────────────────

    private String formatHour(int hour) {
        String period = hour < 12 ? "AM" : "PM";
        int h = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        return h + ":00 " + period;
    }

    private String getWeatherEmoji(String weatherMain) {
        if (weatherMain == null) return "🌡";
        switch (weatherMain) {
            case "Clear":        return "☀️";
            case "Clouds":       return "⛅";
            case "Rain":
            case "Drizzle":      return "🌧";
            case "Thunderstorm": return "⛈";
            case "Snow":         return "❄️";
            case "Mist":
            case "Fog":
            case "Haze":         return "🌫";
            default:             return "🌡";
        }
    }
}
