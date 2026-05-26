package com.arriva.touristguideapp;

import java.util.List;

public class HomeSection {
    public static final String TYPE_WELCOME = "welcome";
    public static final String TYPE_CATEGORIES = "categories";
    public static final String TYPE_TOP_PICKS = "top_picks";
    public static final String TYPE_PLAN_TRIP = "plan_trip";
    public static final String TYPE_PHRASEBOOK = "phrasebook";
    public static final String TYPE_TRENDING = "trending";
    public static final String TYPE_RECOMMENDED = "recommended";
    public static final String TYPE_RECENTLY_VIEWED = "recently_viewed";
    public static final String TYPE_ALL_PLACES_HEADER = "all_places_header";
    public static final String TYPE_PLACE = "place";
    public static final String TYPE_MAP_PREVIEW = "map_preview";

    private String type;
    private String title;
    private List<Place> data;
    private Place singlePlace; // For individual items if needed

    public HomeSection(String type) {
        this.type = type;
    }

    public HomeSection(String type, String title) {
        this.type = type;
        this.title = title;
    }

    public HomeSection(String type, List<Place> data) {
        this.type = type;
        this.data = data;
    }

    public HomeSection(String type, Place singlePlace) {
        this.type = type;
        this.singlePlace = singlePlace;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public List<Place> getData() {
        return data;
    }

    public void setData(List<Place> data) {
        this.data = data;
    }

    public Place getSinglePlace() {
        return singlePlace;
    }
}
