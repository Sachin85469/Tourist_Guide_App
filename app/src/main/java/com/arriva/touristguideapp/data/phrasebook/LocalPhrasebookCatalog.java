package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bundled fallback phrases (base language only).
 */
public class LocalPhrasebookCatalog {

    private static final String BASE_LANG = "en";

    @NonNull
    public List<Phrase> getAllPhrases() {
        List<Phrase> phrases = new ArrayList<>();
        phrases.addAll(transportPhrases());
        phrases.addAll(foodPhrases());
        phrases.addAll(emergencyPhrases());
        phrases.addAll(shoppingPhrases());
        phrases.addAll(greetingPhrases());
        return phrases;
    }

    private List<Phrase> transportPhrases() {
        return Arrays.asList(
                phrase("transport_1", "Where is the railway station?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("transport_2", "How much is the fare?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("transport_3", "Please stop here.", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("transport_4", "Which bus goes to the city center?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("transport_5", "I need a taxi.", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("transport_6", "How long will it take?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT)
        );
    }

    private List<Phrase> foodPhrases() {
        return Arrays.asList(
                phrase("food_1", "I would like water, please.", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("food_2", "What is today's special?", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("food_3", "The bill, please.", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("food_4", "Is this spicy?", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("food_5", "I am vegetarian.", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("food_6", "One tea, please.", PhrasebookFirestoreContract.CATEGORY_FOOD)
        );
    }

    private List<Phrase> emergencyPhrases() {
        return Arrays.asList(
                phrase("emergency_1", "Help!", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("emergency_2", "Call the police.", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("emergency_3", "I need a doctor.", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("emergency_4", "Where is the hospital?", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("emergency_5", "I lost my passport.", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("emergency_6", "Call an ambulance.", PhrasebookFirestoreContract.CATEGORY_EMERGENCY)
        );
    }

    private List<Phrase> shoppingPhrases() {
        return Arrays.asList(
                phrase("shopping_1", "How much does this cost?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("shopping_2", "Do you have a smaller size?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("shopping_3", "Can I get a discount?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("shopping_4", "I will take this.", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("shopping_5", "Do you accept card?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("shopping_6", "Where is the nearest market?", PhrasebookFirestoreContract.CATEGORY_SHOPPING)
        );
    }

    private List<Phrase> greetingPhrases() {
        return Arrays.asList(
                phrase("greetings_1", "Hello.", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("greetings_2", "Thank you.", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("greetings_3", "Good morning.", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("greetings_4", "How are you?", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("greetings_5", "Nice to meet you.", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("greetings_6", "Goodbye.", PhrasebookFirestoreContract.CATEGORY_GREETINGS)
        );
    }

    @NonNull
    private static Phrase phrase(@NonNull String id, @NonNull String baseText, @NonNull String category) {
        return new Phrase(id, baseText, BASE_LANG, category);
    }
}
