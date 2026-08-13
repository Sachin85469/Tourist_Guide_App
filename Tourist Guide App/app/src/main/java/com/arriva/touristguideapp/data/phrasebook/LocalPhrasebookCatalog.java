package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bundled phrases with pre-translated English, Hindi, and Marathi content.
 */
public class LocalPhrasebookCatalog {

    @NonNull
    public List<Phrase> getAllPhrases() {
        List<Phrase> phrases = new ArrayList<>();
        phrases.addAll(greetingPhrases());
        phrases.addAll(transportPhrases());
        phrases.addAll(foodPhrases());
        phrases.addAll(shoppingPhrases());
        phrases.addAll(emergencyPhrases());
        return phrases;
    }

    private List<Phrase> greetingPhrases() {
        return Arrays.asList(
                phrase("g1", "Hello", "नमस्ते", "नमस्कार", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("g2", "Thank you", "धन्यवाद", "धन्यवाद", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("g3", "Good morning", "शुभ प्रभात", "शुभ सकाळ", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("g4", "How are you?", "आप कैसे हैं?", "तुम्ही कसे आहात?", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("g5", "Nice to meet you", "आपसे मिलकर खुशी हुई", "तुम्हाला भेटून आनंद झाला", PhrasebookFirestoreContract.CATEGORY_GREETINGS),
                phrase("g6", "Goodbye", "अलविदा", "पुन्हा भेटू", PhrasebookFirestoreContract.CATEGORY_GREETINGS)
        );
    }

    private List<Phrase> transportPhrases() {
        return Arrays.asList(
                phrase("t1", "Where is the railway station?", "रेलवे स्टेशन कहाँ है?", "रेल्वे स्टेशन कुठे आहे?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("t2", "How much is the fare?", "किराया कितना है?", "भाडे किती आहे?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("t3", "Please stop here", "कृपया यहाँ रुकिए", "कृपया इथे थांबा", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("t4", "Which bus goes to city center?", "शहर के केंद्र में कौन सी बस जाती है?", "कोणती बस शहराच्या मध्यभागी जाते?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("t5", "I need a taxi", "मुझे एक टैक्सी चाहिए", "मला टॅक्सी हवी आहे", PhrasebookFirestoreContract.CATEGORY_TRANSPORT),
                phrase("t6", "How long will it take?", "इसमें कितना समय लगेगा?", "किती वेळ लागेल?", PhrasebookFirestoreContract.CATEGORY_TRANSPORT)
        );
    }

    private List<Phrase> foodPhrases() {
        return Arrays.asList(
                phrase("f1", "I would like water", "मुझे पानी चाहिए", "मला पाणी हवे आहे", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("f2", "What is today's special?", "आज का विशेष क्या है?", "आजचे विशेष काय आहे?", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("f3", "The bill, please", "बिल, कृपया", "बिल, कृपया", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("f4", "Is this spicy?", "क्या यह तीखा है?", "हे तिखट आहे का?", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("f5", "I am vegetarian", "मैं शाकाहारी हूँ", "मी शाकाहारी आहे", PhrasebookFirestoreContract.CATEGORY_FOOD),
                phrase("f6", "One tea, please", "एक चाय, कृपया", "एक चहा, कृपया", PhrasebookFirestoreContract.CATEGORY_FOOD)
        );
    }

    private List<Phrase> shoppingPhrases() {
        return Arrays.asList(
                phrase("s1", "How much does this cost?", "इसकी कीमत क्या है?", "याची किंमत काय आहे?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("s2", "Do you have a smaller size?", "क्या आपके पास छोटा साइज है?", "तुमच्याकडे लहान साईझ आहे का?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("s3", "Can I get a discount?", "क्या मुझे छूट मिल सकती है?", "मला सवलत मिळेल का?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("s4", "I will take this", "मैं यह लूँगा", "मी हे घेईन", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("s5", "Do you accept card?", "क्या आप कार्ड स्वीकार करते हैं?", "तुम्ही कार्ड स्वीकारता का?", PhrasebookFirestoreContract.CATEGORY_SHOPPING),
                phrase("s6", "Where is the market?", "बाजार कहाँ है?", "बाजार कुठे आहे?", PhrasebookFirestoreContract.CATEGORY_SHOPPING)
        );
    }

    private List<Phrase> emergencyPhrases() {
        return Arrays.asList(
                phrase("e1", "Help!", "मदद!", "मदत!", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("e2", "Call the police", "पुलिस को बुलाओ", "पोलीस बोलवा", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("e3", "I need a doctor", "मुझे डॉक्टर की जरूरत है", "मला डॉक्टरची गरज आहे", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("e4", "Where is the hospital?", "अस्पताल कहाँ है?", "रुग्णालय कुठे आहे?", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("e5", "I lost my passport", "मेरा पासपोर्ट खो गया है", "माझा पासपोर्ट हरवला आहे", PhrasebookFirestoreContract.CATEGORY_EMERGENCY),
                phrase("e6", "Call an ambulance", "एम्बुलेंस बुलाओ", "रुग्णवाहिका बोलवा", PhrasebookFirestoreContract.CATEGORY_EMERGENCY)
        );
    }

    @NonNull
    private static Phrase phrase(@NonNull String id, @NonNull String en, @NonNull String hi, @NonNull String mr, @NonNull String category) {
        return new Phrase(id, en, hi, mr, category);
    }
}
