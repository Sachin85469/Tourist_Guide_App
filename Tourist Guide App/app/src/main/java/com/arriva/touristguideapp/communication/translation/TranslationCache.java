package com.arriva.touristguideapp.communication.translation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * In-memory + disk cache for translation results (phrase list + free text).
 */
public class TranslationCache {

    private static final String PREFS = "translation_cache_prefs";
    private static final int MEMORY_MAX = 256;

    private final LruCache<String, String> memory = new LruCache<>(MEMORY_MAX);
    private final SharedPreferences disk;

    public TranslationCache(@NonNull Context context) {
        disk = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public String phraseKey(@NonNull String phraseId,
                            @NonNull String sourceCode,
                            @NonNull String targetCode) {
        return phraseId + "|" + sourceCode + "|" + targetCode;
    }

    @NonNull
    public String textKey(@NonNull String text,
                          @NonNull String sourceCode,
                          @NonNull String targetCode) {
        String raw = sourceCode + ">" + targetCode + ">" + text.trim().toLowerCase(Locale.US);
        return "t_" + sha1(raw);
    }

    @Nullable
    public String get(@NonNull String key) {
        String mem = memory.get(key);
        if (mem != null) {
            return mem;
        }
        return disk.getString(key, null);
    }

    public void put(@NonNull String key, @NonNull String translated) {
        memory.put(key, translated);
        disk.edit().putString(key, translated).apply();
    }

    public void clearPair(@NonNull String sourceCode, @NonNull String targetCode) {
        String suffix = "|" + sourceCode + "|" + targetCode;
        SharedPreferences.Editor editor = disk.edit();
        for (String key : disk.getAll().keySet()) {
            if (key.endsWith(suffix) || key.contains(sourceCode + ">" + targetCode)) {
                editor.remove(key);
                memory.remove(key);
            }
        }
        editor.apply();
    }

    @NonNull
    private static String sha1(@NonNull String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
