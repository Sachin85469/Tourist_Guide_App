package com.arriva.touristguideapp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public final class InterestSelectionNavigator {
    private static final String TAG = "InterestSelectionNav";
    private static final String COLLECTION_USERS = "users";
    public static final String FIELD_TRAVEL_INTERESTS = "travelInterests";

    private InterestSelectionNavigator() {
    }

    public static void openNext(Activity activity) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            openLogin(activity);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_USERS).document(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Could not read user interests gate", task.getException());
                        openMain(activity);
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        createUserDocumentThenOpenInterests(activity, db, firebaseUser);
                        return;
                    }

                    if (hasSeenInterestSelection(document)) {
                        openMain(activity);
                    } else {
                        openInterestSelection(activity);
                    }
                });
    }

    private static boolean hasSeenInterestSelection(DocumentSnapshot document) {
        return document.contains(FIELD_TRAVEL_INTERESTS)
                && document.get(FIELD_TRAVEL_INTERESTS) != null;
    }

    private static void createUserDocumentThenOpenInterests(Activity activity,
                                                           FirebaseFirestore db,
                                                           FirebaseUser firebaseUser) {
        User newUser = new User(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                ""
        );
        db.collection(COLLECTION_USERS).document(firebaseUser.getUid()).set(newUser)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Could not create user document before interests", task.getException());
                    }
                    openInterestSelection(activity);
                });
    }

    static void openMain(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void openInterestSelection(Activity activity) {
        Intent intent = new Intent(activity, InterestSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void openLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
