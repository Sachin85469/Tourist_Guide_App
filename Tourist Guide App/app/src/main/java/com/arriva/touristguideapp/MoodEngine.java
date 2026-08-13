package com.arriva.touristguideapp;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stateless engine that produces context-aware place-category suggestions
 * based on the current hour and OpenWeatherMap "weather.main" condition string.
 *
 * All category strings match the values used in Firestore / Place.getCategory().
 */
public class MoodEngine {

    // ─── Category constants (match Firestore values exactly) ─────────────────
    public static final String CAT_HISTORICAL = "Historical";
    public static final String CAT_NATURE     = "Nature";
    public static final String CAT_RELIGIOUS  = "Religious";
    public static final String CAT_FOOD       = "Food";
    public static final String CAT_CULTURE    = "Culture";
    public static final String CAT_ADVENTURE  = "Adventure";
    public static final String CAT_SCENIC     = "Scenic";
    public static final String CAT_SHOPPING   = "Shopping";

    // ─── Weather condition buckets ────────────────────────────────────────────
    private static final List<String> RAINY_CONDITIONS =
            Arrays.asList("Rain", "Drizzle", "Thunderstorm", "Snow", "Sleet");
    private static final List<String> CLOUDY_CONDITIONS =
            Arrays.asList("Clouds", "Mist", "Fog", "Haze", "Smoke", "Dust", "Sand", "Ash", "Squall", "Tornado");

    private MoodEngine() { /* static utility */ }

    /**
     * Returns an ordered list of suggested category strings.
     *
     * @param hour        0–23 from the device clock (local time)
     * @param weatherMain The "weather[0].main" string from OWM, e.g. "Clear", "Rain", "Clouds".
     *                    Pass null or empty string if weather is unavailable.
     * @return Non-empty list of suggested category labels.
     */
    @NonNull
    public static List<String> getSuggestedCategories(int hour, String weatherMain) {
        String condition = weatherMain == null ? "" : weatherMain.trim();

        // ── Rain / storm / snow → prefer indoor-friendly spots ────────────────
        if (matchesAny(condition, RAINY_CONDITIONS)) {
            return asList(CAT_RELIGIOUS, CAT_CULTURE, CAT_FOOD);
        }

        // ── Cloudy / hazy → mix of indoor + mild outdoor ──────────────────────
        if (matchesAny(condition, CLOUDY_CONDITIONS)) {
            return asList(CAT_HISTORICAL, CAT_CULTURE, CAT_FOOD);
        }

        // ── Night (20:00 – 05:59) ──────────────────────────────────────────────
        if (hour >= 20 || hour < 6) {
            return asList(CAT_FOOD, CAT_CULTURE, CAT_SCENIC);
        }

        // ── Morning (06:00 – 10:59) — clear ───────────────────────────────────
        if (hour < 11) {
            return asList(CAT_NATURE, CAT_ADVENTURE, CAT_SCENIC);
        }

        // ── Afternoon (11:00 – 15:59) — hot + clear ───────────────────────────
        if (hour < 16) {
            return asList(CAT_HISTORICAL, CAT_FOOD, CAT_RELIGIOUS);
        }

        // ── Evening (16:00 – 19:59) — golden hour ─────────────────────────────
        return asList(CAT_SCENIC, CAT_CULTURE, CAT_FOOD);
    }

    /**
     * Returns a human-readable reason string for the suggestion, shown in the UI.
     */
    @NonNull
    public static String getSuggestionReason(int hour, String weatherMain) {
        String condition = weatherMain == null ? "" : weatherMain.trim();

        if (matchesAny(condition, RAINY_CONDITIONS)) {
            return "It's " + condition.toLowerCase() + " outside — stay comfortable indoors";
        }
        if (matchesAny(condition, CLOUDY_CONDITIONS)) {
            return "Overcast skies are perfect for forts and culture";
        }
        if (hour >= 20 || hour < 6) {
            return "Evening vibes — great for food and lit-up landmarks";
        }
        if (hour < 11) {
            return "Cool morning — ideal for nature walks and adventures";
        }
        if (hour < 16) {
            return "Hot afternoon — shaded forts and indoor spots await";
        }
        return "Golden hour — best time for scenic and cultural spots";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static boolean matchesAny(@NonNull String condition, @NonNull List<String> list) {
        for (String item : list) {
            if (item.equalsIgnoreCase(condition)) return true;
        }
        return false;
    }

    @NonNull
    private static List<String> asList(String... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
