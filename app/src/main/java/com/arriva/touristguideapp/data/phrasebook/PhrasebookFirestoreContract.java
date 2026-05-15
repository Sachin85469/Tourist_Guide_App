package com.arriva.touristguideapp.data.phrasebook;

/**
 * Firestore collection and field names for the travel phrasebook.
 */
public final class PhrasebookFirestoreContract {

    public static final String COLLECTION_PHRASEBOOK = "phrasebook";

    public static final String FIELD_BASE_TEXT = "baseText";
    public static final String FIELD_BASE_LANGUAGE = "baseLanguage";
    public static final String FIELD_CATEGORY = "category";

    /** @deprecated Legacy field — migrated to {@link #FIELD_BASE_TEXT} at read time. */
    public static final String FIELD_LEGACY_ENGLISH = "englishText";

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
