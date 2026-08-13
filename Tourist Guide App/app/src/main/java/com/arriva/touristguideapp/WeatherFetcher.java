package com.arriva.touristguideapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetches the current weather condition string from OpenWeatherMap (free tier).
 *
 * <p>All callbacks are delivered on the main thread. On any network error,
 * parse failure, or timeout the callback is invoked with {@code null} so that
 * callers can gracefully fall back to time-based suggestions without crashing.
 *
 * <p>Usage:
 * <pre>
 *   WeatherFetcher.fetch(context, lat, lon, apiKey, weatherMain -> {
 *       List&lt;String&gt; cats = MoodEngine.getSuggestedCategories(hour, weatherMain);
 *       // weatherMain may be null — MoodEngine handles that correctly
 *   });
 * </pre>
 */
public final class WeatherFetcher {

    private static final String TAG = "WeatherFetcher";

    /** OWM current-weather endpoint (metric units). */
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

    /** Timeout in milliseconds — keep short so UI doesn't stall. */
    private static final int TIMEOUT_MS = 5_000;

    public interface Callback {
        /**
         * @param weatherMain the {@code weather[0].main} value (e.g. "Rain", "Clear", "Clouds"),
         *                    or {@code null} if the fetch failed or timed out.
         */
        @MainThread
        void onResult(@Nullable String weatherMain);
    }

    private WeatherFetcher() { /* static utility */ }

    /**
     * Asynchronously fetches the current weather at the given coordinates.
     *
     * @param context  any Context (application context used internally)
     * @param lat      latitude in decimal degrees
     * @param lon      longitude in decimal degrees
     * @param apiKey   OpenWeatherMap API key; if blank the callback is called immediately with null
     * @param callback result delivered on the main thread
     */
    @MainThread
    public static void fetch(Context context,
                             double lat,
                             double lon,
                             String apiKey,
                             Callback callback) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.w(TAG, "OWM API key is empty — skipping weather fetch, falling back to time-only");
            callback.onResult(null);
            return;
        }

        String url = String.format(java.util.Locale.US, BASE_URL, lat, lon, apiKey.trim());
        Log.d(TAG, "Fetching weather: lat=" + lat + " lon=" + lon);

        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray weatherArr = response.getJSONArray("weather");
                        String main = weatherArr.getJSONObject(0).getString("main");
                        Log.d(TAG, "Weather fetched: " + main);
                        callback.onResult(main);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse weather response", e);
                        callback.onResult(null);
                    }
                },
                error -> {
                    Log.w(TAG, "Weather fetch error: " + error.getMessage());
                    callback.onResult(null);
                }
        );

        // Short timeout so the UI doesn't wait too long before showing fallback suggestions
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                0,               // no retries — fail fast
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }
}
