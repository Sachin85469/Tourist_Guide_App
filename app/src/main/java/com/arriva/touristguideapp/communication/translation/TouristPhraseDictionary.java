package com.arriva.touristguideapp.communication.translation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Curated dictionary for high-quality tourist-friendly translations.
 * Overrides literal ML Kit translations for common greetings and travel phrases.
 */
public final class TouristPhraseDictionary {

    private static final Map<String, Map<String, String>> DICTIONARY = new HashMap<>();

    static {
        // English to Marathi (mr)
        Map<String, String> enToMr = new HashMap<>();
        enToMr.put("hello", "नमस्कार");
        enToMr.put("hi", "नमस्कार");
        enToMr.put("thank you", "धन्यवाद");
        enToMr.put("thanks", "आभारी आहे");
        enToMr.put("help", "मदत");
        enToMr.put("help me", "मला मदत करा");
        enToMr.put("where is the railway station?", "रेल्वे स्टेशन कुठे आहे?");
        enToMr.put("where is the hospital?", "रुग्णालय कुठे आहे?");
        enToMr.put("how much is this?", "याची किंमत काय आहे?");
        enToMr.put("sorry", "क्षमस्व");
        enToMr.put("please", "कृपया");
        enToMr.put("good morning", "शुभ सकाळ");
        enToMr.put("good night", "शुभ रात्री");
        enToMr.put("yes", "हो");
        enToMr.put("no", "नाही");
        enToMr.put("water", "पाणी");
        enToMr.put("i need a taxi", "मला टॅक्सी हवी आहे");
        enToMr.put("stop here", "इथे थांबा");
        DICTIONARY.put("en-mr", enToMr);

        // English to Hindi (hi)
        Map<String, String> enToHi = new HashMap<>();
        enToHi.put("hello", "नमस्ते");
        enToHi.put("hi", "नमस्ते");
        enToHi.put("thank you", "धन्यवाद");
        enToHi.put("thanks", "शुक्रिया");
        enToHi.put("help", "मदद");
        enToHi.put("help me", "मेरी मदद करें");
        enToHi.put("where is the railway station?", "रेलवे स्टेशन कहाँ है?");
        enToHi.put("where is the hospital?", "अस्पताल कहाँ है?");
        enToHi.put("how much is this?", "यह कितने का है?");
        enToHi.put("sorry", "माफ़ कीजिये");
        enToHi.put("please", "कृपया");
        enToHi.put("good morning", "शुभ प्रभात");
        enToHi.put("good night", "शुभ रात्रि");
        enToHi.put("yes", "हाँ");
        enToHi.put("no", "नहीं");
        enToHi.put("water", "पानी");
        enToHi.put("i need a taxi", "मुझे एक टैक्सी चाहिए");
        enToHi.put("stop here", "यहाँ रुकिए");
        DICTIONARY.put("en-hi", enToHi);
        
        // Add more pairs as needed (mr-en, hi-en, hi-mr, etc.)
    }

    private TouristPhraseDictionary() {}

    @Nullable
    public static String translate(@NonNull String text, @NonNull String sourceLang, @NonNull String targetLang) {
        String key = sourceLang.toLowerCase() + "-" + targetLang.toLowerCase();
        Map<String, String> pairs = DICTIONARY.get(key);
        if (pairs == null) {
            return null;
        }
        return pairs.get(text.toLowerCase().trim().replace("?", ""));
    }

    /** Helper that checks for a few variations (with and without punctuation) */
    @Nullable
    public static String lookup(@NonNull String text, @NonNull String sourceLang, @NonNull String targetLang) {
        String clean = text.toLowerCase().trim();
        String result = translate(clean, sourceLang, targetLang);
        if (result != null) return result;
        
        // Try without trailing punctuation
        if (clean.endsWith("?") || clean.endsWith(".") || clean.endsWith("!")) {
            result = translate(clean.substring(0, clean.length() - 1), sourceLang, targetLang);
        }
        
        return result;
    }
}
