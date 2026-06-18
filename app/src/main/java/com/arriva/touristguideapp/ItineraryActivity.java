package com.arriva.touristguideapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.arriva.touristguideapp.data.notifications.NotificationModel;
import com.arriva.touristguideapp.data.notifications.NotificationRepository;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.arriva.touristguideapp.data.trips.Trip;
import com.arriva.touristguideapp.data.trips.TripRepository;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ItineraryActivity extends BaseActivity {

    private static final String TAG = "ItineraryActivity";
    private static final String API_URL = BuildConfig.ITINERARY_BACKEND_URL;
    private static final String REQUEST_TAG = "ai_trip_plan";
    private static final int MAX_CONTEXT_PLACES = 30;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double INITIAL_PROXIMITY_RADIUS_KM = 15.0;
    private static final double MAX_PROXIMITY_RADIUS_KM = 60.0;

    private RecyclerView rvItinerary;
    private ItineraryAdapter adapter;
    private TextView tvTitle;
    private PlaceRepository placeRepository;
    private TripRepository tripRepository;
    private NotificationRepository notificationRepository;
    private ExtendedFloatingActionButton btnSaveTrip;
    private final List<Place> selectedPlacesList = new ArrayList<>();
    private final List<Place> loadedPlaces = new ArrayList<>();
    private final List<DayPlan> currentPlan = new ArrayList<>();

    private View emptyState;
    private View offlineBanner;
    private View loadingState;
    private RequestQueue requestQueue;
    private String currentTripTitle = "Your Custom Trip";
    private String currentGeneralTips = "";
    private String currentBudget = "Mid-range";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        placeRepository = new PlaceRepository();
        tripRepository = new TripRepository();
        notificationRepository = new NotificationRepository(this);
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        emptyState = findViewById(R.id.emptyStatePlanner);
        offlineBanner = findViewById(R.id.offlinePlanBanner);
        loadingState = findViewById(R.id.loadingStatePlanner);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvTitle = findViewById(R.id.tvItineraryTitle);
        rvItinerary = findViewById(R.id.rvItinerary);
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));

        btnSaveTrip = findViewById(R.id.btnSaveTrip);
        if (btnSaveTrip != null) {
            btnSaveTrip.setOnClickListener(v -> saveThisItinerary());
        }

        boolean isQuickPlan = getIntent().getBooleanExtra("isQuickPlan", false);

        if (isQuickPlan) {
            renderQuickPlan();
        } else {
            int days = clampDays(getIntent().getIntExtra("days", 1));
            // Support multi-vibe ArrayList (from PlanTripActivity redesign).
            // Fall back to the single "type" string for backward compatibility.
            ArrayList<String> vibes = getIntent().getStringArrayListExtra("vibes");
            String type;
            if (vibes != null && !vibes.isEmpty()) {
                type = vibes.size() == 1 ? vibes.get(0) : String.join(", ", vibes);
            } else {
                type = valueOrDefault(getIntent().getStringExtra("type"), "Mixed");
            }
            currentBudget = valueOrDefault(getIntent().getStringExtra("budget"), "Mid-range");
            loadAndGeneratePlan(days, type, currentBudget);
        }
    }

    @Override
    protected void onDestroy() {
        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }
        super.onDestroy();
    }

    private void renderQuickPlan() {
        String customTitle = getIntent().getStringExtra("title");
        String quickPlanText = getIntent().getStringExtra("quickPlanText");
        ArrayList<String> quickPlaceIds = getIntent().getStringArrayListExtra("quickPlaceIds");
        currentTripTitle = valueOrDefault(customTitle, "Your Selection");
        currentGeneralTips = "";
        if (tvTitle != null) tvTitle.setText(currentTripTitle);

        List<DayPlan> plan = new ArrayList<>();
        if (!isBlank(quickPlanText)) {
            plan.add(new DayPlan("Your Selection", quickPlanText));
            showOfflineBanner(false);
            renderPlan(currentTripTitle, "", plan, false);
        } else {
            showEmptyState();
        }

        loadQuickPlanSelectedPlaces(quickPlaceIds);
    }

    private void loadQuickPlanSelectedPlaces(ArrayList<String> quickPlaceIds) {
        selectedPlacesList.clear();
        if (quickPlaceIds == null || quickPlaceIds.isEmpty()) {
            return;
        }

        if (btnSaveTrip != null) {
            btnSaveTrip.setEnabled(false);
        }

        placeRepository.fetchPublishedPlaces((places, origin, message) ->
                runOnUiThread(() -> {
                    loadedPlaces.clear();
                    loadedPlaces.addAll(places);

                    Map<String, Place> placesById = buildPlaceIdLookup(places);
                    selectedPlacesList.clear();
                    for (String placeId : quickPlaceIds) {
                        Place place = placesById.get(placeId);
                        if (place != null) {
                            selectedPlacesList.add(place);
                        }
                    }

                    if (btnSaveTrip != null) {
                        btnSaveTrip.setEnabled(true);
                    }
                }));
    }

    private void loadAndGeneratePlan(int days, String type, String budget) {
        setLoadingState(true);
        showOfflineBanner(false);
        if (tvTitle != null) tvTitle.setText("Creating your AI trip");

        placeRepository.fetchPublishedPlaces((places, origin, message) ->
                runOnUiThread(() -> {
                    if (places.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    loadedPlaces.clear();
                    loadedPlaces.addAll(places);

                    if (!isNetworkAvailable() || !isConfiguredEndpoint(API_URL)) {
                        showFallbackPlan(places, days, type, budget);
                        return;
                    }

                    generateAiPlan(days, type, budget, places);
                }));
    }

    private void generateAiPlan(int days, String type, String budget, List<Place> places) {
        try {
            JSONObject body = new JSONObject();
            body.put("days", days);
            body.put("type", type);
            body.put("budget", budget);
            body.put("places", buildPlacesPayload(type, budget, places));

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    body,
                    response -> handleAiResponse(response, places, days, type, budget),
                    error -> {
                        Log.w(TAG, "AI itinerary request failed", error);
                        showFallbackPlan(places, days, type, budget);
                    }
            );
            request.setTag(REQUEST_TAG);
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.w(TAG, "Could not create AI itinerary request", e);
            showFallbackPlan(places, days, type, budget);
        }
    }

    private JSONArray buildPlacesPayload(String type, String budget, List<Place> places) throws JSONException {
        List<Place> contextPlaces = filterPlacesForPlan(places, type, budget);
        if (contextPlaces.isEmpty()) {
            contextPlaces = new ArrayList<>(places);
        }

        contextPlaces.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

        JSONArray placesJson = new JSONArray();
        int count = Math.min(MAX_CONTEXT_PLACES, contextPlaces.size());
        for (int i = 0; i < count; i++) {
            Place place = contextPlaces.get(i);
            JSONObject placeJson = new JSONObject();
            placeJson.put("name", valueOrDefault(place.getName(), "Unnamed place"));
            placeJson.put("category", valueOrDefault(place.getCategory(), "Mixed"));
            placeJson.put("rating", place.getRating());
            placeJson.put("totalRatings", place.getTotalRatings());
            placeJson.put("tag", valueOrDefault(place.getTag(), ""));
            placeJson.put("budget", valueOrDefault(place.getBudget(), "Medium"));
            placeJson.put("bestTime", valueOrDefault(place.getBestTime(), "Any"));
            placeJson.put("city", valueOrDefault(place.getCity(), "Pune"));
            placeJson.put("latitude", place.getLatitude());
            placeJson.put("longitude", place.getLongitude());
            placeJson.put("description", trimToLength(valueOrDefault(place.getDescription(), "No description available."), 280));
            placesJson.put(placeJson);
        }
        return placesJson;
    }

    private void handleAiResponse(JSONObject response, List<Place> places, int days, String type, String budget) {
        try {
            ParsedAiPlan parsedPlan = parseAiPlan(response.toString(), places);
            if (parsedPlan.days.isEmpty()) {
                throw new JSONException("AI response did not include usable itinerary days.");
            }
            renderPlan(parsedPlan.tripTitle, parsedPlan.generalTips, parsedPlan.days, false);
        } catch (Exception e) {
            Log.w(TAG, "Could not parse AI itinerary response", e);
            showFallbackPlan(places, days, type, budget);
        }
    }

    private ParsedAiPlan parseAiPlan(String jsonText, List<Place> places) throws JSONException {
        JSONObject root = new JSONObject(jsonText);
        ParsedAiPlan parsedPlan = new ParsedAiPlan();
        parsedPlan.tripTitle = valueOrDefault(root.optString("tripTitle"), "Your AI Trip in Pune");
        parsedPlan.generalTips = valueOrDefault(root.optString("generalTips"), "");

        Map<String, Place> allowedPlaces = buildPlaceLookup(places);
        JSONArray daysJson = root.optJSONArray("days");
        if (daysJson == null) {
            return parsedPlan;
        }

        for (int i = 0; i < daysJson.length(); i++) {
            JSONObject dayJson = daysJson.optJSONObject(i);
            if (dayJson == null) continue;

            int dayNumber = dayJson.optInt("dayNumber", i + 1);
            DayPlan dayPlan = new DayPlan(
                    dayNumber,
                    valueOrDefault(dayJson.optString("dayTheme"), "Curated Pune highlights")
            );

            Set<String> namesUsedToday = new HashSet<>();
            JSONArray stopsJson = dayJson.optJSONArray("stops");
            if (stopsJson != null) {
                for (int j = 0; j < stopsJson.length(); j++) {
                    JSONObject stopJson = stopsJson.optJSONObject(j);
                    if (stopJson == null) continue;

                    String placeName = valueOrDefault(stopJson.optString("placeName"), "");
                    String placeKey = normalizeName(placeName);
                    if (isBlank(placeName)
                            || !allowedPlaces.containsKey(placeKey)
                            || !namesUsedToday.add(placeKey)) {
                        continue;
                    }

                    dayPlan.stops.add(new ItineraryStop(
                            placeName,
                            valueOrDefault(stopJson.optString("timeSlot"), fallbackTimeSlot(j)),
                            valueOrDefault(stopJson.optString("duration"), "Flexible"),
                            valueOrDefault(stopJson.optString("travelNote"), ""),
                            valueOrDefault(stopJson.optString("whyVisit"), "A strong fit for this trip."),
                            valueOrDefault(stopJson.optString("tips"), "Check opening hours before visiting.")
                    ));
                }
            }

            if (!dayPlan.stops.isEmpty()) {
                parsedPlan.days.add(dayPlan);
            }
        }
        return parsedPlan;
    }

    private void showFallbackPlan(List<Place> places, int days, String type, String budget) {
        SmartPlanResult fallbackPlan = generateSmartPlan(places, days, type, budget);
        String fallbackTitle = "Your " + days + "-Day " + type + " Trip";
        renderPlan(fallbackTitle, fallbackPlan.generalTips, fallbackPlan.days, true);
    }

    private SmartPlanResult generateSmartPlan(List<Place> allPlaces, int days, String type, String budget) {
        SmartPlanResult result = new SmartPlanResult();
        List<DayPlan> plan = new ArrayList<>();
        List<Place> filtered = filterPlacesForPlan(allPlaces, type, budget);
        List<Place> alternatives = buildAlternativePlaces(allPlaces, type, budget, filtered);
        Set<String> usedPlaceKeys = new HashSet<>();
        boolean addedAlternatives = false;
        TimeSlotAffinity[] slots = {
                TimeSlotAffinity.MORNING,
                TimeSlotAffinity.AFTERNOON,
                TimeSlotAffinity.EVENING
        };

        for (int i = 1; i <= days; i++) {
            DayPlan dayPlan = new DayPlan(i, fallbackDayTheme(type, i));
            Place previousStop = null;

            for (int j = 0; j < slots.length; j++) {
                Place place = j == 0
                        ? pickHighestRatedUnusedPlace(filtered, usedPlaceKeys, budget)
                        : pickNearestPlaceForSlot(filtered, usedPlaceKeys, previousStop, slots[j], budget);
                if (place == null) {
                    break;
                }

                addStopToDay(dayPlan, place, previousStop, j, usedPlaceKeys);
                previousStop = place;
            }

            if (dayPlan.stops.size() < 2) {
                int originalStopCount = dayPlan.stops.size();
                while (dayPlan.stops.size() < slots.length) {
                    int slotIndex = dayPlan.stops.size();
                    Place alternative = previousStop == null
                            ? pickHighestRatedUnusedPlace(alternatives, usedPlaceKeys, budget)
                            : pickNearestPlaceForSlot(alternatives, usedPlaceKeys, previousStop, slots[slotIndex], budget);
                    if (alternative == null) {
                        break;
                    }

                    addStopToDay(dayPlan, alternative, previousStop, slotIndex, usedPlaceKeys);
                    previousStop = alternative;
                }
                addedAlternatives = addedAlternatives || dayPlan.stops.size() > originalStopCount;
            }

            if (dayPlan.stops.isEmpty()) {
                dayPlan.rawText = "No more spots found for this category. Explore the main map for more!";
            }

            plan.add(dayPlan);
        }
        result.days.addAll(plan);
        result.generalTips = addedAlternatives
                ? buildLimitedSpotsTip(type)
                : "Generated using the basic offline planner.";
        return result;
    }

    private void addStopToDay(DayPlan dayPlan,
                              Place place,
                              Place previousStop,
                              int slotIndex,
                              Set<String> usedPlaceKeys) {
        usedPlaceKeys.add(placeKey(place));
        dayPlan.stops.add(new ItineraryStop(
                valueOrDefault(place.getName(), "Unnamed place"),
                fallbackTimeSlot(slotIndex),
                fallbackDuration(slotIndex),
                fallbackWhyVisit(place, previousStop),
                valueOrDefault(place.getTips(), "Check timings and travel time before you leave.")
        ));
    }

    private Place pickHighestRatedUnusedPlace(List<Place> places, Set<String> usedPlaceKeys, String budget) {
        Place best = null;
        for (Place place : places) {
            if (isUsedPlace(place, usedPlaceKeys)) {
                continue;
            }
            if (best == null || comparePlacePriority(place, best, budget) > 0) {
                best = place;
            }
        }
        return best;
    }

    private Place pickHighestRatedUnusedPlaceForSlot(List<Place> places,
                                                     Set<String> usedPlaceKeys,
                                                     TimeSlotAffinity slot,
                                                     String budget) {
        Place best = null;
        for (Place place : places) {
            if (isUsedPlace(place, usedPlaceKeys) || !fitsTimeSlot(place, slot)) {
                continue;
            }
            if (best == null || comparePlacePriority(place, best, budget) > 0) {
                best = place;
            }
        }
        return best;
    }

    private Place pickNearestPlaceForSlot(List<Place> places,
                                          Set<String> usedPlaceKeys,
                                          Place previousStop,
                                          TimeSlotAffinity slot,
                                          String budget) {
        if (!hasValidCoordinates(previousStop)) {
            Place timeMatch = pickHighestRatedUnusedPlaceForSlot(places, usedPlaceKeys, slot, budget);
            return timeMatch != null ? timeMatch : pickHighestRatedUnusedPlace(places, usedPlaceKeys, budget);
        }

        double radiusKm = INITIAL_PROXIMITY_RADIUS_KM;
        while (radiusKm <= MAX_PROXIMITY_RADIUS_KM) {
            Place place = findNearestUnusedPlace(places, usedPlaceKeys, previousStop, slot, true, radiusKm, budget);
            if (place != null) {
                return place;
            }
            radiusKm *= 2.0;
        }

        Place nearestAnyTime = findNearestUnusedPlace(
                places,
                usedPlaceKeys,
                previousStop,
                slot,
                false,
                Double.POSITIVE_INFINITY,
                budget
        );
        return nearestAnyTime != null ? nearestAnyTime : pickHighestRatedUnusedPlace(places, usedPlaceKeys, budget);
    }

    private Place findNearestUnusedPlace(List<Place> places,
                                         Set<String> usedPlaceKeys,
                                         Place previousStop,
                                         TimeSlotAffinity slot,
                                         boolean requireTimeMatch,
                                         double radiusKm,
                                         String budget) {
        Place best = null;
        double bestDistanceKm = Double.POSITIVE_INFINITY;
        for (Place place : places) {
            if (isUsedPlace(place, usedPlaceKeys)
                    || !hasValidCoordinates(place)
                    || (requireTimeMatch && !fitsTimeSlot(place, slot))) {
                continue;
            }

            double distanceKm = haversineDistanceKm(previousStop, place);
            if (!Double.isFinite(distanceKm) || distanceKm > radiusKm) {
                continue;
            }

            if (best == null
                    || distanceKm < bestDistanceKm
                    || (Math.abs(distanceKm - bestDistanceKm) < 0.1 && comparePlacePriority(place, best, budget) > 0)) {
                best = place;
                bestDistanceKm = distanceKm;
            }
        }
        return best;
    }

    private int comparePlacePriority(Place first, Place second, String budget) {
        int budgetCompare = Integer.compare(budgetPreferenceScore(first, budget), budgetPreferenceScore(second, budget));
        if (budgetCompare != 0) {
            return budgetCompare;
        }

        int ratingCompare = Double.compare(first.getRating(), second.getRating());
        if (ratingCompare != 0) {
            return ratingCompare;
        }
        return Long.compare(first.getTotalRatings(), second.getTotalRatings());
    }

    private int budgetPreferenceScore(Place place, String budget) {
        int tier = budgetTier(place.getBudget());
        if (isBlank(budget)) {
            return 0;
        }

        if (budget.equalsIgnoreCase("Luxury")) {
            if (tier >= 3) return 3;
            if (tier == 2) return 2;
            if (tier == 1) return 1;
            return 0;
        }

        if (budget.equalsIgnoreCase("Budget")) {
            if (tier == 1) return 3;
            if (tier == 2) return 1;
            if (tier >= 3) return -3;
            return 0;
        }

        if (budget.equalsIgnoreCase("Mid-range")) {
            if (tier == 2) return 3;
            if (tier == 1) return 2;
            if (tier >= 3) return -2;
            return 0;
        }

        return 0;
    }

    private int budgetTier(String budget) {
        String value = valueOrDefault(budget, "").toLowerCase(Locale.US);
        if (containsAny(value, "luxury", "premium", "high", "expensive")) {
            return 3;
        }
        if (containsAny(value, "mid", "medium", "moderate")) {
            return 2;
        }
        if (containsAny(value, "budget", "low", "free", "cheap")) {
            return 1;
        }
        return 0;
    }

    private List<Place> buildAlternativePlaces(List<Place> allPlaces,
                                               String type,
                                               String budget,
                                               List<Place> primaryPlaces) {
        Set<String> primaryKeys = new HashSet<>();
        for (Place place : primaryPlaces) {
            primaryKeys.add(placeKey(place));
        }

        List<String> adjacentCategories = adjacentCategoriesForType(type);
        List<Place> adjacentBudgetMatches = collectAlternativePlaces(
                allPlaces,
                primaryKeys,
                adjacentCategories,
                budget,
                true
        );
        if (!adjacentBudgetMatches.isEmpty()) {
            return adjacentBudgetMatches;
        }

        List<Place> budgetMatches = collectAlternativePlaces(
                allPlaces,
                primaryKeys,
                new ArrayList<>(),
                budget,
                true
        );
        if (!budgetMatches.isEmpty()) {
            return budgetMatches;
        }

        List<Place> adjacentAnyBudget = collectAlternativePlaces(
                allPlaces,
                primaryKeys,
                adjacentCategories,
                budget,
                false
        );
        if (!adjacentAnyBudget.isEmpty()) {
            return adjacentAnyBudget;
        }

        return collectAlternativePlaces(
                allPlaces,
                primaryKeys,
                new ArrayList<>(),
                budget,
                false
        );
    }

    private List<Place> collectAlternativePlaces(List<Place> allPlaces,
                                                 Set<String> primaryKeys,
                                                 List<String> categories,
                                                 String budget,
                                                 boolean requireBudgetMatch) {
        List<Place> alternatives = new ArrayList<>();
        for (Place place : allPlaces) {
            if (primaryKeys.contains(placeKey(place))) {
                continue;
            }
            if (!categories.isEmpty() && !categoryMatchesAny(place, categories)) {
                continue;
            }
            if (requireBudgetMatch && !matchesBudget(place, budget)) {
                continue;
            }
            alternatives.add(place);
        }
        return alternatives;
    }

    private List<String> adjacentCategoriesForType(String type) {
        List<String> categories = new ArrayList<>();
        if (isBlank(type) || type.equalsIgnoreCase("Mixed") || type.equalsIgnoreCase("All")) {
            return categories;
        }

        String normalizedType = type.trim().toLowerCase(Locale.US);
        if (normalizedType.equals("nature")) {
            categories.add("Adventure");
            categories.add("Spiritual");
        } else if (normalizedType.equals("adventure")) {
            categories.add("Nature");
            categories.add("History");
        } else if (normalizedType.equals("history")) {
            categories.add("Spiritual");
            categories.add("Shopping");
        } else if (normalizedType.equals("spiritual")) {
            categories.add("History");
            categories.add("Nature");
        } else if (normalizedType.equals("food")) {
            categories.add("Shopping");
            categories.add("Entertainment");
        } else if (normalizedType.equals("shopping")) {
            categories.add("Food");
            categories.add("Entertainment");
        } else if (normalizedType.equals("entertainment")) {
            categories.add("Food");
            categories.add("Shopping");
        }
        return categories;
    }

    private boolean categoryMatchesAny(Place place, List<String> categories) {
        String category = place.getCategory();
        if (isBlank(category)) {
            return false;
        }
        for (String candidate : categories) {
            if (category.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean fitsTimeSlot(Place place, TimeSlotAffinity slot) {
        TimeSlotAffinity affinity = classifyTimeSlotAffinity(place);
        return affinity == TimeSlotAffinity.ANY || affinity == slot;
    }

    private TimeSlotAffinity classifyTimeSlotAffinity(Place place) {
        String bestTime = valueOrDefault(place.getBestTime(), "").toLowerCase(Locale.US);
        if (containsAny(bestTime, "any", "all day", "full day")) {
            return TimeSlotAffinity.ANY;
        }

        boolean morning = containsAny(bestTime, "morning", "sunrise", "early");
        boolean afternoon = containsAny(bestTime, "afternoon", "noon", "day");
        boolean evening = containsAny(bestTime, "evening", "sunset", "night");
        int explicitMatches = (morning ? 1 : 0) + (afternoon ? 1 : 0) + (evening ? 1 : 0);

        if (explicitMatches == 1) {
            if (morning) return TimeSlotAffinity.MORNING;
            if (afternoon) return TimeSlotAffinity.AFTERNOON;
            return TimeSlotAffinity.EVENING;
        }
        if (explicitMatches > 1) {
            return TimeSlotAffinity.ANY;
        }

        String category = valueOrDefault(place.getCategory(), "").toLowerCase(Locale.US);
        if (containsAny(category, "nature", "adventure", "spiritual", "temple", "trek")) {
            return TimeSlotAffinity.MORNING;
        }
        if (containsAny(category, "shopping", "history", "museum", "culture", "art")) {
            return TimeSlotAffinity.AFTERNOON;
        }
        if (containsAny(category, "food", "entertainment", "nightlife")) {
            return TimeSlotAffinity.EVENING;
        }
        return TimeSlotAffinity.ANY;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private double haversineDistanceKm(Place first, Place second) {
        if (!hasValidCoordinates(first) || !hasValidCoordinates(second)) {
            return Double.POSITIVE_INFINITY;
        }

        double latDistance = Math.toRadians(second.getLatitude() - first.getLatitude());
        double lngDistance = Math.toRadians(second.getLongitude() - first.getLongitude());
        double startLat = Math.toRadians(first.getLatitude());
        double endLat = Math.toRadians(second.getLatitude());

        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(lngDistance / 2.0) * Math.sin(lngDistance / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    private boolean hasValidCoordinates(Place place) {
        if (place == null) {
            return false;
        }
        double latitude = place.getLatitude();
        double longitude = place.getLongitude();
        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0
                && !(Math.abs(latitude) < 0.000001 && Math.abs(longitude) < 0.000001);
    }

    private boolean isUsedPlace(Place place, Set<String> usedPlaceKeys) {
        return usedPlaceKeys.contains(placeKey(place));
    }

    private String placeKey(Place place) {
        if (place == null) {
            return "";
        }
        if (!isBlank(place.getId())) {
            return "id:" + place.getId().trim();
        }
        return "name:" + normalizeName(place.getName());
    }

    private List<Place> filterPlacesForPlan(List<Place> places, String type, String budget) {
        // Multi-vibe support: if type is comma-separated, apply OR logic across all vibes.
        if (type != null && type.contains(",")) {
            String[] types = type.split(",");
            List<Place> filtered = new ArrayList<>();
            for (Place place : places) {
                if (matchesBudget(place, budget)) {
                    for (String t : types) {
                        if (matchesType(place, t.trim())) {
                            filtered.add(place);
                            break; // OR logic: first match is enough
                        }
                    }
                }
            }
            return filtered;
        }
        // Single vibe — original path
        List<Place> filtered = new ArrayList<>();
        for (Place place : places) {
            if (matchesType(place, type) && matchesBudget(place, budget)) {
                filtered.add(place);
            }
        }
        return filtered;
    }

    private boolean matchesType(Place place, String type) {
        if (isBlank(type) || type.equalsIgnoreCase("Mixed") || type.equalsIgnoreCase("All")) {
            return true;
        }
        return place.getCategory() != null && place.getCategory().equalsIgnoreCase(type);
    }

    private boolean matchesBudget(Place place, String budget) {
        if (isBlank(budget)) {
            return true;
        }

        if (budget.equalsIgnoreCase("Luxury")) {
            // Luxury travelers can consider any place; ranking prefers High/Luxury places first.
            return true;
        }

        String placeBudget = place.getBudget();
        if (isBlank(placeBudget)) {
            return true;
        }

        if (budget.equalsIgnoreCase("Budget")) {
            return budgetTier(placeBudget) < 3;
        }

        if (budget.equalsIgnoreCase("Mid-range")) {
            return budgetTier(placeBudget) < 3;
        }

        return true;
    }

    private void renderPlan(String tripTitle, String generalTips, List<DayPlan> plan, boolean showOfflinePlan) {
        setLoadingState(false);
        showOfflineBanner(showOfflinePlan);

        currentTripTitle = valueOrDefault(tripTitle, "Your Custom Trip");
        currentGeneralTips = valueOrDefault(generalTips, "");
        if (tvTitle != null) tvTitle.setText(currentTripTitle);

        currentPlan.clear();
        currentPlan.addAll(plan);
        updateSelectedPlacesFromPlan();

        if (plan.isEmpty()) {
            showEmptyState();
            return;
        }

        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (rvItinerary != null) rvItinerary.setVisibility(View.VISIBLE);
        if (btnSaveTrip != null) btnSaveTrip.setVisibility(View.VISIBLE);

        adapter = new ItineraryAdapter(plan);
        rvItinerary.setAdapter(adapter);
    }

    private void updateSelectedPlacesFromPlan() {
        selectedPlacesList.clear();
        Map<String, Place> lookup = buildPlaceLookup(loadedPlaces);
        Set<String> added = new HashSet<>();
        for (DayPlan day : currentPlan) {
            for (ItineraryStop stop : day.stops) {
                String key = normalizeName(stop.placeName);
                Place place = lookup.get(key);
                if (place != null && added.add(key)) {
                    selectedPlacesList.add(place);
                }
            }
        }
    }

    private Map<String, Place> buildPlaceLookup(List<Place> places) {
        Map<String, Place> lookup = new HashMap<>();
        for (Place place : places) {
            if (!isBlank(place.getName())) {
                lookup.put(normalizeName(place.getName()), place);
            }
        }
        return lookup;
    }

    private Map<String, Place> buildPlaceIdLookup(List<Place> places) {
        Map<String, Place> lookup = new HashMap<>();
        for (Place place : places) {
            if (!isBlank(place.getId())) {
                lookup.put(place.getId(), place);
            }
        }
        return lookup;
    }

    private List<String> collectStopNames() {
        List<String> names = new ArrayList<>();
        for (DayPlan day : currentPlan) {
            for (ItineraryStop stop : day.stops) {
                if (!isBlank(stop.placeName)) {
                    names.add(stop.placeName);
                }
            }
            if (names.isEmpty() && !isBlank(day.rawText)) {
                String[] rawLines = day.rawText.split("\\n");
                for (String rawLine : rawLines) {
                    String name = rawLine.replaceFirst("^-\\s*", "").trim();
                    if (!isBlank(name)) {
                        names.add(name);
                    }
                }
            }
        }

        if (names.isEmpty()) {
            for (Place place : selectedPlacesList) {
                if (!isBlank(place.getName())) {
                    names.add(place.getName());
                }
            }
        }
        return names;
    }

    private void showEmptyState() {
        setLoadingState(false);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (rvItinerary != null) rvItinerary.setVisibility(View.GONE);
        if (btnSaveTrip != null) btnSaveTrip.setVisibility(View.GONE);
    }

    private void setLoadingState(boolean isLoading) {
        if (loadingState != null) loadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (rvItinerary != null) rvItinerary.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (emptyState != null && isLoading) emptyState.setVisibility(View.GONE);
        if (btnSaveTrip != null) btnSaveTrip.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showOfflineBanner(boolean show) {
        if (offlineBanner != null) {
            offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private boolean isConfiguredEndpoint(String url) {
        String endpoint = url == null ? "" : url.trim();
        return endpoint.startsWith("https://") || endpoint.startsWith("http://");
    }

    private int clampDays(int days) {
        return Math.max(1, Math.min(7, days));
    }

    private String fallbackTimeSlot(int index) {
        if (index == 0) return "Morning (8am-11am)";
        if (index == 1) return "Afternoon (12pm-3pm)";
        return "Evening (4pm-7pm)";
    }

    private String fallbackDuration(int index) {
        if (index == 0) return "2-3 hours";
        if (index == 1) return "2 hours";
        return "1-2 hours";
    }

    private String fallbackDayTheme(String type, int dayNumber) {
        if (isBlank(type) || type.equalsIgnoreCase("Mixed") || type.equalsIgnoreCase("All")) {
            return "Pune highlights and local favorites";
        }
        return type + " picks for Day " + dayNumber;
    }

    private String buildLimitedSpotsTip(String type) {
        String typeLabel = (isBlank(type) || type.equalsIgnoreCase("Mixed") || type.equalsIgnoreCase("All"))
                ? "matching"
                : type.trim();
        return "Limited " + typeLabel + " spots found near you - we've added some nearby alternatives.";
    }

    private String fallbackWhyVisit(Place place) {
        return fallbackWhyVisit(place, null);
    }

    private String fallbackWhyVisit(Place place, Place previousStop) {
        String description = trimToLength(place.getDescription(), 120);
        String proximity = buildProximityReason(place, previousStop);

        String baseReason;
        if (!isBlank(description)) {
            baseReason = description;
        } else {
            String category = valueOrDefault(place.getCategory(), "Pune");
            baseReason = "A popular " + category.toLowerCase(Locale.US) + " stop for this itinerary.";
        }

        if (!isBlank(proximity)) {
            return proximity + " " + baseReason;
        }
        return baseReason;
    }

    private String buildProximityReason(Place place, Place previousStop) {
        double distanceKm = haversineDistanceKm(previousStop, place);
        if (!Double.isFinite(distanceKm) || distanceKm > 20.0) {
            return "";
        }
        return "Just " + formatDistanceKm(distanceKm) + " from your last stop.";
    }

    private String formatDistanceKm(double distanceKm) {
        if (distanceKm < 1.0) {
            int meters = Math.max(100, (int) Math.round(distanceKm * 1000.0 / 100.0) * 100);
            return meters + "m";
        }
        if (distanceKm < 10.0) {
            double rounded = Math.round(distanceKm * 10.0) / 10.0;
            if (Math.abs(rounded - Math.round(rounded)) < 0.01) {
                return String.format(Locale.US, "%.0fkm", rounded);
            }
            return String.format(Locale.US, "%.1fkm", rounded);
        }
        return String.format(Locale.US, "%.0fkm", distanceKm);
    }

    private enum TimeSlotAffinity {
        MORNING,
        AFTERNOON,
        EVENING,
        ANY
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim().replace('\n', ' ');
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String normalizeName(String name) {
        return valueOrDefault(name, "").trim().toLowerCase(Locale.US);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static class ParsedAiPlan {
        String tripTitle = "Your AI Trip in Pune";
        String generalTips = "";
        final List<DayPlan> days = new ArrayList<>();
    }

    private static class SmartPlanResult {
        String generalTips = "Generated using the basic offline planner.";
        final List<DayPlan> days = new ArrayList<>();
    }

    private static class DayPlan {
        int dayNumber;
        String dayTitle;
        String dayTheme;
        String rawText;
        final List<ItineraryStop> stops = new ArrayList<>();

        DayPlan(int dayNumber, String dayTheme) {
            this.dayNumber = dayNumber;
            this.dayTitle = "Day " + dayNumber;
            this.dayTheme = dayTheme;
            this.rawText = "";
        }

        DayPlan(String dayTitle, String rawText) {
            this.dayNumber = 0;
            this.dayTitle = dayTitle;
            this.dayTheme = "";
            this.rawText = rawText;
        }
    }

    private static class ItineraryStop {
        String placeName;
        String timeSlot;
        String duration;
        String travelNote;
        String whyVisit;
        String tips;

        ItineraryStop(String placeName, String timeSlot, String duration, String whyVisit, String tips) {
            this(placeName, timeSlot, duration, "", whyVisit, tips);
        }

        ItineraryStop(String placeName, String timeSlot, String duration, String travelNote, String whyVisit, String tips) {
            this.placeName = placeName;
            this.timeSlot = timeSlot;
            this.duration = duration;
            this.travelNote = travelNote;
            this.whyVisit = whyVisit;
            this.tips = tips;
        }
    }

    private static class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {
        private final List<DayPlan> plans;

        ItineraryAdapter(List<DayPlan> plans) {
            this.plans = plans;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DayPlan plan = plans.get(position);
            holder.tvDayTitle.setText(valueOrDefault(plan.dayTitle, "Day " + (position + 1)));

            if (isBlank(plan.dayTheme)) {
                holder.tvDayTheme.setVisibility(View.GONE);
            } else {
                holder.tvDayTheme.setVisibility(View.VISIBLE);
                holder.tvDayTheme.setText(plan.dayTheme);
            }

            holder.stopContainer.removeAllViews();
            if (plan.stops.isEmpty()) {
                holder.stopContainer.setVisibility(View.GONE);
                holder.tvDayPlaces.setVisibility(View.VISIBLE);
                holder.tvDayPlaces.setText(valueOrDefault(plan.rawText, ""));
                return;
            }

            holder.tvDayPlaces.setVisibility(View.GONE);
            holder.stopContainer.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (ItineraryStop stop : plan.stops) {
                View stopView = inflater.inflate(R.layout.item_itinerary_stop, holder.stopContainer, false);
                bindStop(stopView, stop);
                holder.stopContainer.addView(stopView);
            }
        }

        @Override
        public int getItemCount() {
            return plans.size();
        }

        private void bindStop(View stopView, ItineraryStop stop) {
            TextView tvTimeSlot = stopView.findViewById(R.id.tvStopTimeSlot);
            TextView tvPlaceName = stopView.findViewById(R.id.tvStopPlaceName);
            TextView tvDuration = stopView.findViewById(R.id.tvStopDuration);
            TextView tvTravelNote = stopView.findViewById(R.id.tvStopTravelNote);
            TextView tvTravelNoteLabel = stopView.findViewById(R.id.tvStopTravelNoteLabel);
            TextView tvWhyVisit = stopView.findViewById(R.id.tvStopWhyVisit);
            TextView tvTips = stopView.findViewById(R.id.tvStopTips);
            TextView tvTipsLabel = stopView.findViewById(R.id.tvStopTipsLabel);

            tvTimeSlot.setText(valueOrDefault(stop.timeSlot, "Flexible"));
            applyTimeSlotBadge(tvTimeSlot, stop.timeSlot);
            tvPlaceName.setText(valueOrDefault(stop.placeName, "Place"));
            tvDuration.setText(valueOrDefault(stop.duration, "Flexible"));
            tvWhyVisit.setText(valueOrDefault(stop.whyVisit, "A good fit for this day."));

            if (isBlank(stop.travelNote)) {
                tvTravelNote.setVisibility(View.GONE);
                tvTravelNoteLabel.setVisibility(View.GONE);
            } else {
                tvTravelNote.setVisibility(View.VISIBLE);
                tvTravelNoteLabel.setVisibility(View.VISIBLE);
                tvTravelNote.setText(stop.travelNote);
            }

            if (isBlank(stop.tips)) {
                tvTips.setVisibility(View.GONE);
                tvTipsLabel.setVisibility(View.GONE);
            } else {
                tvTips.setVisibility(View.VISIBLE);
                tvTipsLabel.setVisibility(View.VISIBLE);
                tvTips.setText(stop.tips);
            }
        }

        private void applyTimeSlotBadge(TextView textView, String timeSlot) {
            String lower = valueOrDefault(timeSlot, "").toLowerCase(Locale.US);
            int backgroundColor = Color.parseColor("#EDE7F6");
            int textColor = Color.parseColor("#32145F");

            if (lower.contains("morning")) {
                backgroundColor = Color.parseColor("#FFF3B0");
                textColor = Color.parseColor("#5D4600");
            } else if (lower.contains("afternoon")) {
                backgroundColor = Color.parseColor("#FFE0B2");
                textColor = Color.parseColor("#6D3600");
            } else if (lower.contains("evening")) {
                backgroundColor = Color.parseColor("#E8DEF8");
                textColor = Color.parseColor("#32145F");
            }

            GradientDrawable background = new GradientDrawable();
            background.setColor(backgroundColor);
            background.setCornerRadius(dp(textView.getContext(), 14));
            textView.setBackground(background);
            textView.setTextColor(textColor);
        }

        private int dp(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayTitle;
            TextView tvDayTheme;
            TextView tvDayPlaces;
            LinearLayout stopContainer;

            ViewHolder(View itemView) {
                super(itemView);
                tvDayTitle = itemView.findViewById(R.id.tvDayTitle);
                tvDayTheme = itemView.findViewById(R.id.tvDayTheme);
                tvDayPlaces = itemView.findViewById(R.id.tvDayPlaces);
                stopContainer = itemView.findViewById(R.id.stopContainer);
            }
        }
    }

    private void saveThisItinerary() {
        String title = valueOrDefault(currentTripTitle, tvTitle != null ? tvTitle.getText().toString() : "My Trip");
        if (isBlank(title)) title = "My Trip";

        Trip trip = new Trip();
        trip.setTitle(title);
        String location = selectedPlacesList.isEmpty() ? "Pune" : selectedPlacesList.get(0).getCity();
        trip.setDestinationName(isBlank(location) ? title : location);
        trip.setLocation(valueOrDefault(location, "Pune"));
        trip.setStatus("planned");
        trip.setPlaces(selectedPlacesList);

        List<String> activities = new ArrayList<>();
        List<String> stopNames = collectStopNames();
        for (String stopName : stopNames) {
            activities.add("Visit " + stopName);
        }

        for (Place place : selectedPlacesList) {
            if ((trip.getImageUrl() == null || trip.getImageUrl().trim().isEmpty())
                    && place.getImageUrl() != null
                    && !place.getImageUrl().trim().isEmpty()) {
                trip.setImageUrl(place.getImageUrl());
            }
        }
        trip.setActivities(activities);
        trip.setNotes(isBlank(currentGeneralTips) ? "Generated from the trip planner." : currentGeneralTips);
        trip.setBudget(currentBudget);

        Calendar cal = Calendar.getInstance();
        trip.setStartDate(cal.getTime());
        int days = clampDays(getIntent().getIntExtra("days", Math.max(1, currentPlan.size())));
        cal.add(Calendar.DATE, days - 1);
        trip.setEndDate(cal.getTime());

        if (btnSaveTrip != null) {
            btnSaveTrip.setEnabled(false);
        }

        tripRepository.saveTripAsync(trip).addOnCompleteListener(task -> {
            if (btnSaveTrip != null) {
                btnSaveTrip.setEnabled(true);
            }
            if (task.isSuccessful()) {
                Toast.makeText(ItineraryActivity.this, "Trip saved to history!", Toast.LENGTH_SHORT).show();

                String tripLabel = trip.getTitle();
                if (isBlank(tripLabel)) {
                    tripLabel = trip.getDestinationName();
                }
                if (isBlank(tripLabel)) {
                    tripLabel = "Trip";
                }
                com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                        ItineraryActivity.this,
                        com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.TRIP_CREATED,
                        tripLabel
                );

                notificationRepository.addNotification(
                        "Trip Created",
                        "Your trip '" + trip.getTitle() + "' was planned successfully.",
                        NotificationModel.TYPE_TRIP
                );

                finish();
            } else {
                Toast.makeText(ItineraryActivity.this, "Failed to save trip: "
                        + (task.getException() != null ? task.getException().getMessage() : "unknown"), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
