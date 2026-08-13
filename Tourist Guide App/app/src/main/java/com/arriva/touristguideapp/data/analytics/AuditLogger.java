package com.arriva.touristguideapp.data.analytics;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Logs administrative and moderation actions for auditing.
 */
public class AuditLogger {
    private static final String TAG = "AuditLogger";
    private static final String COLLECTION_AUDIT_LOGS = "audit_logs";

    public static void logAction(String action, String targetId, String detail) {
        String adminId = FirebaseAuth.getInstance().getUid();
        if (adminId == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("action", action);
        log.put("targetId", targetId);
        log.put("detail", detail);
        log.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection(COLLECTION_AUDIT_LOGS)
                .add(log)
                .addOnSuccessListener(ref -> Log.d(TAG, "ADMIN_ACTION_LOGGED: " + action))
                .addOnFailureListener(e -> Log.e(TAG, "FAILED_TO_LOG_AUDIT", e));
    }
}
