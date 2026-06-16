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
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_MODEL = "claude-sonnet-4-6";
    private static final String REQUEST_TAG = "ai_trip_plan";
    private static final int MAX_CONTEXT_PLACES = 30;

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
            String type = valueOrDefault(getIntent().getStringExtra("type"), "Mixed");
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

        int count = 2;
        if (customTitle != null) {
            if (customTitle.contains("Half Day")) count = 4;
            else if (customTitle.contains("Full Day")) count = 5;
        }
        List<Place> topPicks = DataProvider.getTopPicks();
        if (topPicks.isEmpty()) {
            topPicks = DataProvider.getAllPlaces();
        }
        selectedPlacesList.clear();
        for (int i = 0; i < Math.min(count, topPicks.size()); i++) {
            selectedPlacesList.add(topPicks.get(i));
        }
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

                    if (!isNetworkAvailable() || !isConfiguredKey(BuildConfig.ANTHROPIC_API_KEY)) {
                        showFallbackPlan(places, days, type, budget);
                        return;
                    }

                    generateAiPlan(days, type, budget, places);
                }));
    }

    private void generateAiPlan(int days, String type, String budget, List<Place> places) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", ANTHROPIC_MODEL);
            body.put("max_tokens", 2000);
            body.put("system", "You are a travel guide assistant. Always respond with valid JSON only.");

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", buildAiPrompt(days, type, budget, places));
            messages.put(userMessage);
            body.put("messages", messages);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    body,
                    response -> handleAiResponse(response, places, days, type, budget),
                    error -> {
                        Log.w(TAG, "AI itinerary request failed", error);
                        showFallbackPlan(places, days, type, budget);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-api-key", BuildConfig.ANTHROPIC_API_KEY.trim());
                    headers.put("anthropic-version", "2023-06-01");
                    headers.put("content-type", "application/json");
                    return headers;
                }
            };
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

    private String buildAiPrompt(int days, String type, String budget, List<Place> places) {
        String contextString = buildPlacesContext(type, budget, places);
        return "You are a travel expert for Pune, India. The user wants a "
                + days + "-day " + type + " trip on a " + budget + " budget.\n"
                + "Available places:\n" + contextString + "\n\n"
                + "Return ONLY a valid JSON object (no extra text, no markdown) in this exact format:\n"
                + "{\n"
                + "  \"tripTitle\": \"Your 2-Day Nature Escape in Pune\",\n"
                + "  \"days\": [\n"
                + "    {\n"
                + "      \"dayNumber\": 1,\n"
                + "      \"dayTheme\": \"Morning freshness and green trails\",\n"
                + "      \"stops\": [\n"
                + "        {\n"
                + "          \"placeName\": \"Sinhagad Fort\",\n"
                + "          \"timeSlot\": \"Morning (8am-11am)\",\n"
                + "          \"duration\": \"3 hours\",\n"
                + "          \"whyVisit\": \"Best experienced at sunrise before crowds arrive.\",\n"
                + "          \"tips\": \"Carry water and wear comfortable shoes.\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"generalTips\": \"Book accommodation near Koregaon Park for easy access.\"\n"
                + "}\n\n"
                + "Rules:\n"
                + "- Only use places from the provided list.\n"
                + "- Do not repeat a place on the same day.\n"
                + "- Spread stops sensibly across morning/afternoon/evening.\n"
                + "- Match the type filter: if type is 'Nature', only include nature places.\n"
                + "- If type is 'Mixed', use variety across categories.\n"
                + "- Respect the budget: for 'Budget' trips, avoid places with budget='High'.";
    }

    private String buildPlacesContext(String type, String budget, List<Place> places) {
        List<Place> contextPlaces = filterPlacesForPlan(places, type, budget);
        if (contextPlaces.isEmpty()) {
            contextPlaces = new ArrayList<>(places);
        }

        contextPlaces.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

        StringBuilder context = new StringBuilder();
        int count = Math.min(MAX_CONTEXT_PLACES, contextPlaces.size());
        for (int i = 0; i < count; i++) {
            Place place = contextPlaces.get(i);
            String category = valueOrDefault(place.getCategory(), "Mixed");
            String rating = place.getTotalRatings() > 0
                    ? String.format(Locale.US, "%.1f (%d reviews)", place.getRating(), place.getTotalRatings())
                    : "New (0 reviews)";
            String bestFor = valueOrDefault(place.getTag(), category + " lovers");
            String placeBudget = valueOrDefault(place.getBudget(), "Medium");
            String description = trimToLength(valueOrDefault(place.getDescription(), "No description available."), 140);

            context.append("Place: ").append(valueOrDefault(place.getName(), "Unnamed place"))
                    .append(" | Category: ").append(category)
                    .append(" | Rating: ").append(rating)
                    .append(" | Best for: ").append(bestFor)
                    .append(" | Budget: ").append(placeBudget)
                    .append(" | City: ").append(valueOrDefault(place.getCity(), "Pune"))
                    .append(" | Description: ").append(description)
                    .append('\n');
        }
        return context.toString();
    }

    private void handleAiResponse(JSONObject response, List<Place> places, int days, String type, String budget) {
        try {
            String jsonText = extractAssistantText(response);
            ParsedAiPlan parsedPlan = parseAiPlan(jsonText, places);
            if (parsedPlan.days.isEmpty()) {
                throw new JSONException("AI response did not include usable itinerary days.");
            }
            renderPlan(parsedPlan.tripTitle, parsedPlan.generalTips, parsedPlan.days, false);
        } catch (Exception e) {
            Log.w(TAG, "Could not parse AI itinerary response", e);
            showFallbackPlan(places, days, type, budget);
        }
    }

    private String extractAssistantText(JSONObject response) throws JSONException {
        JSONArray content = response.optJSONArray("content");
        if (content == null) {
            throw new JSONException("Missing content array.");
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block != null && "text".equals(block.optString("type"))) {
                String text = block.optString("text");
                if (!isBlank(text)) {
                    if (result.length() > 0) result.append('\n');
                    result.append(text);
                }
            }
        }
        if (result.length() == 0) {
            throw new JSONException("Empty assistant response.");
        }
        return extractJsonObject(result.toString());
    }

    private String extractJsonObject(String text) throws JSONException {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new JSONException("No JSON object found.");
        }
        return cleaned.substring(start, end + 1);
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
        List<DayPlan> fallbackPlan = generateSmartPlan(places, days, type, budget);
        String fallbackTitle = "Your " + days + "-Day " + type + " Trip";
        renderPlan(fallbackTitle, "Generated using the basic offline planner.", fallbackPlan, true);
    }

    private List<DayPlan> generateSmartPlan(List<Place> allPlaces, int days, String type, String budget) {
        List<DayPlan> plan = new ArrayList<>();
        List<Place> filtered = filterPlacesForPlan(allPlaces, type, budget);

        filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

        int placesPerDay = 3;
        int currentIdx = 0;

        for (int i = 1; i <= days; i++) {
            DayPlan dayPlan = new DayPlan(i, fallbackDayTheme(type, i));
            for (int j = 0; j < placesPerDay && currentIdx < filtered.size(); j++) {
                Place place = filtered.get(currentIdx++);
                dayPlan.stops.add(new ItineraryStop(
                        valueOrDefault(place.getName(), "Unnamed place"),
                        fallbackTimeSlot(j),
                        fallbackDuration(j),
                        fallbackWhyVisit(place),
                        valueOrDefault(place.getTips(), "Check timings and travel time before you leave.")
                ));
            }

            if (dayPlan.stops.isEmpty()) {
                dayPlan.rawText = "No more spots found for this category. Explore the main map for more!";
            }

            plan.add(dayPlan);
        }
        return plan;
    }

    private List<Place> filterPlacesForPlan(List<Place> places, String type, String budget) {
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
        if (isBlank(budget) || budget.equalsIgnoreCase("Luxury")) {
            return true;
        }

        String placeBudget = place.getBudget();
        if (isBlank(placeBudget)) {
            return true;
        }

        if (budget.equalsIgnoreCase("Budget")) {
            return !placeBudget.equalsIgnoreCase("High") && !placeBudget.equalsIgnoreCase("Luxury");
        }

        if (budget.equalsIgnoreCase("Mid-range")) {
            return !placeBudget.equalsIgnoreCase("High") && !placeBudget.equalsIgnoreCase("Luxury");
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
        Map<String, Place> lookup = buildPlaceLookup(loadedPlaces.isEmpty() ? DataProvider.getAllPlaces() : loadedPlaces);
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

    private List<String> collectStopNames() {
        List<String> names = new ArrayList<>();
        for (DayPlan day : currentPlan) {
            for (ItineraryStop stop : day.stops) {
                if (!isBlank(stop.placeName)) {
                    names.add(stop.placeName);
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

    private boolean isConfiguredKey(String apiKey) {
        String key = apiKey == null ? "" : apiKey.trim();
        return !key.isEmpty()
                && !key.equalsIgnoreCase("YOUR_KEY")
                && !key.startsWith("YOUR_");
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

    private String fallbackWhyVisit(Place place) {
        String description = trimToLength(place.getDescription(), 120);
        if (!isBlank(description)) {
            return description;
        }
        String category = valueOrDefault(place.getCategory(), "Pune");
        return "A popular " + category.toLowerCase(Locale.US) + " stop for this itinerary.";
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
        String whyVisit;
        String tips;

        ItineraryStop(String placeName, String timeSlot, String duration, String whyVisit, String tips) {
            this.placeName = placeName;
            this.timeSlot = timeSlot;
            this.duration = duration;
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
            TextView tvWhyVisit = stopView.findViewById(R.id.tvStopWhyVisit);
            TextView tvTips = stopView.findViewById(R.id.tvStopTips);
            TextView tvTipsLabel = stopView.findViewById(R.id.tvStopTipsLabel);

            tvTimeSlot.setText(valueOrDefault(stop.timeSlot, "Flexible"));
            applyTimeSlotBadge(tvTimeSlot, stop.timeSlot);
            tvPlaceName.setText(valueOrDefault(stop.placeName, "Place"));
            tvDuration.setText(valueOrDefault(stop.duration, "Flexible"));
            tvWhyVisit.setText(valueOrDefault(stop.whyVisit, "A good fit for this day."));

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
