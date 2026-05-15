package com.arriva.touristguideapp.data.phrasebook;

/**
 * Firestore collection and field names for the travel phrasebook.
 */
public final class PhrasebookFirestoreContract {

    public static final String COLLECTION_PHRASEBOOK = "phrasebook";

    public static final String FIELD_ENGLISH = "englishText";
    public static final String FIELD_MARATHI = "marathiText";
    public static final String FIELD_HINDI = "hindiText";
    public static final String FIELD_CATEGORY = "category";

    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_TRANSPORT = "Transport";
    public static final String CATEGORY_FOOD = "Food";
    public static final String CATEGORY_EMERGENCY = "Emergency";
    public static final String CATEGORY_SHOPPING = "Shopping";
    public static final String CATEGORY_GREETINGS = "Greetings";

    public static final String[] CATEGORIES = {
            CATEGORY_TRANSPORT,
            CATEGORY_FOOD,
            CATEGORY_EMERGENCY,
            CATEGORY_SHOPPING,
            CATEGORY_GREETINGS
    };

    private PhrasebookFirestoreContract() {
    }
}
