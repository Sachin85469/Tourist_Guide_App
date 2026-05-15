package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bundled fallback phrases when Firestore is empty or unavailable (also used for offline cache seed).
 */
public class LocalPhrasebookCatalog {

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
                phrase("transport_1", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "Where is the railway station?",
                        "रेल्वे स्टेशन कुठे आहे?",
                        "रेलवे स्टेशन कहाँ है?"),
                phrase("transport_2", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "How much is the fare?",
                        "भाडे किती आहे?",
                        "किराया कितना है?"),
                phrase("transport_3", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "Please stop here.",
                        "कृपया येथे थांबा.",
                        "कृपया यहाँ रुकिए।"),
                phrase("transport_4", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "Which bus goes to the city center?",
                        "शहराच्या मध्यभागी कोणती बस जाते?",
                        "शहर के केंद्र में कौन सी बस जाती है?"),
                phrase("transport_5", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "I need a taxi.",
                        "मला टॅक्सी हवी आहे.",
                        "मुझे टैक्सी चाहिए।"),
                phrase("transport_6", PhrasebookFirestoreContract.CATEGORY_TRANSPORT,
                        "How long will it take?",
                        "किती वेळ लागेल?",
                        "कितना समय लगेगा?")
        );
    }

    private List<Phrase> foodPhrases() {
        return Arrays.asList(
                phrase("food_1", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "I would like water, please.",
                        "मला पाणी हवे आहे, कृपया.",
                        "मुझे पानी चाहिए, कृपया।"),
                phrase("food_2", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "What is today's special?",
                        "आजचा विशेष काय आहे?",
                        "आज का स्पेशल क्या है?"),
                phrase("food_3", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "The bill, please.",
                        "बिल द्या, कृपया.",
                        "बिल दीजिए, कृपया।"),
                phrase("food_4", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "Is this spicy?",
                        "हे तिखट आहे का?",
                        "क्या यह मसालेदार है?"),
                phrase("food_5", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "I am vegetarian.",
                        "मी शाकाहारी आहे.",
                        "मैं शाकाहारी हूँ।"),
                phrase("food_6", PhrasebookFirestoreContract.CATEGORY_FOOD,
                        "One tea, please.",
                        "एक चहा द्या.",
                        "एक चाय दीजिए।")
        );
    }

    private List<Phrase> emergencyPhrases() {
        return Arrays.asList(
                phrase("emergency_1", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "Help!",
                        "मदत!",
                        "मदद!"),
                phrase("emergency_2", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "Call the police.",
                        "पोलीसांना बोलावा.",
                        "पुलिस को बुलाइए।"),
                phrase("emergency_3", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "I need a doctor.",
                        "मला डॉक्टर हवा आहे.",
                        "मुझे डॉक्टर चाहिए।"),
                phrase("emergency_4", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "Where is the hospital?",
                        "रुग्णालय कुठे आहे?",
                        "अस्पताल कहाँ है?"),
                phrase("emergency_5", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "I lost my passport.",
                        "माझा पासपोर्ट हरवला.",
                        "मेरा पासपोर्ट खो गया।"),
                phrase("emergency_6", PhrasebookFirestoreContract.CATEGORY_EMERGENCY,
                        "Call an ambulance.",
                        "अँब्युलन्स बोलावा.",
                        "एम्बुलेंस बुलाइए।")
        );
    }

    private List<Phrase> shoppingPhrases() {
        return Arrays.asList(
                phrase("shopping_1", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "How much does this cost?",
                        "याची किंमत किती?",
                        "इसकी कीमत कितनी है?"),
                phrase("shopping_2", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "Do you have a smaller size?",
                        "लहान साईज आहे का?",
                        "छोटा साइज है?"),
                phrase("shopping_3", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "Can I get a discount?",
                        "सूट मिळेल का?",
                        "छूट मिल सकती है?"),
                phrase("shopping_4", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "I will take this.",
                        "मी हे घेतो.",
                        "मैं यह लूँगा।"),
                phrase("shopping_5", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "Do you accept card?",
                        "कार्ड स्वीकारता का?",
                        "कार्ड स्वीकारते हैं?"),
                phrase("shopping_6", PhrasebookFirestoreContract.CATEGORY_SHOPPING,
                        "Where is the nearest market?",
                        "जवळचे बाजार कुठे आहे?",
                        "नज़दीकी बाज़ार कहाँ है?")
        );
    }

    private List<Phrase> greetingPhrases() {
        return Arrays.asList(
                phrase("greetings_1", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "Hello.",
                        "नमस्कार.",
                        "नमस्ते।"),
                phrase("greetings_2", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "Thank you.",
                        "धन्यवाद.",
                        "धन्यवाद।"),
                phrase("greetings_3", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "Good morning.",
                        "सुप्रभात.",
                        "सुप्रभात।"),
                phrase("greetings_4", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "How are you?",
                        "तुम्ही कसे आहात?",
                        "आप कैसे हैं?"),
                phrase("greetings_5", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "Nice to meet you.",
                        "तुम्हाला भेटून आनंद झाला.",
                        "आपसे मिलकर खुशी हुई।"),
                phrase("greetings_6", PhrasebookFirestoreContract.CATEGORY_GREETINGS,
                        "Goodbye.",
                        "पुन्हा भेटू.",
                        "फिर मिलेंगे।")
        );
    }

    @NonNull
    private static Phrase phrase(@NonNull String id,
                                 @NonNull String category,
                                 @NonNull String english,
                                 @NonNull String marathi,
                                 @NonNull String hindi) {
        return new Phrase(id, english, marathi, hindi, category);
    }
}
