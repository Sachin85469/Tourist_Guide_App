package com.arriva.touristguideapp.migration;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.DataProvider;
import com.arriva.touristguideapp.Place;
import com.arriva.touristguideapp.data.places.PlacesFirestoreContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Exports every {@link Place} from {@link DataProvider} into Firestore-shaped JSON for batch import.
 * <p>
 * Document path: {@code places/{id}} where {@code id} equals {@link Place#getId()} (stable, favorites-safe).
 * Remote image fields are omitted until Storage URLs exist; drawable keys are recorded for migration ops.
 * <p>
 * Run {@link #buildExportJson(Context)} from an instrumented test or debug tooling, then batch-import via Admin SDK.
 */
public final class DataProviderFirestoreExporter {

    public static final String DEFAULT_OUTPUT_FILENAME = "places_firestore_export_generated.json";

    private DataProviderFirestoreExporter() {
    }

    /**
     * Builds the full export wrapper JSON string (pretty-printed with indent 2).
     */
    @NonNull
    public static String buildExportJson(@NonNull Context context) throws JSONException {
        List<Place> places = new ArrayList<>(DataProvider.getAllPlaces());
        Collections.sort(places, Comparator.comparing(Place::getId, Comparator.nullsFirst(String::compareTo)));

        JSONArray documents = new JSONArray();
        for (Place p : places) {
            documents.put(buildDocumentNode(context, p));
        }

        JSONObject root = new JSONObject();
        root.put("exportVersion", 2);
        root.put("collection", PlacesFirestoreContract.COLLECTION_PLACES);
        root.put("generatedBy", "DataProviderFirestoreExporter");
        root.put("documentCount", places.size());
        root.put("documents", documents);
        return root.toString(2);
    }

    @NonNull
    private static JSONObject buildDocumentNode(@NonNull Context context, @NonNull Place place) throws JSONException {
        String docId = place.getId();
        JSONObject node = new JSONObject();
        node.put("id", docId);
        node.put("fields", buildFieldsObject(context, place));
        return node;
    }

    @NonNull
    private static JSONObject buildFieldsObject(@NonNull Context context, @NonNull Place place) throws JSONException {
        JSONObject f = new JSONObject();
        f.put(PlacesFirestoreContract.FIELD_STATUS, PlacesFirestoreContract.STATUS_PUBLISHED);
        f.put(PlacesFirestoreContract.FIELD_LEGACY_ID, place.getId());
        f.put("slug", slugForPlace(place));
        f.put(PlacesFirestoreContract.FIELD_NAME, place.getName());
        f.put(PlacesFirestoreContract.FIELD_CITY, place.getCity());
        f.put(PlacesFirestoreContract.FIELD_CATEGORY, place.getCategory());
        f.put(PlacesFirestoreContract.FIELD_CATEGORY_ID, categoryIdFromCategory(place.getCategory()));
        f.put(PlacesFirestoreContract.FIELD_DESCRIPTION, place.getDescription());
        f.put(PlacesFirestoreContract.FIELD_BUDGET, place.getBudget());
        f.put(PlacesFirestoreContract.FIELD_CROWD_LEVEL, place.getCrowdLevel());
        f.put(PlacesFirestoreContract.FIELD_BEST_TIME, place.getBestTime());
        f.put(PlacesFirestoreContract.FIELD_LATITUDE, place.getLatitude());
        f.put(PlacesFirestoreContract.FIELD_LONGITUDE, place.getLongitude());
        f.put(PlacesFirestoreContract.FIELD_RATING_AVG, place.getRating());
        f.put(PlacesFirestoreContract.FIELD_IS_TOP_PICK, place.isTopPick());
        f.put(PlacesFirestoreContract.FIELD_TIPS, nullToEmpty(place.getTips()));
        f.put(PlacesFirestoreContract.FIELD_FUN_FACT, nullToEmpty(place.getFunFact()));
        f.put(PlacesFirestoreContract.FIELD_NEAREST_STATION, nullToEmpty(place.getNearestStation()));
        f.put(PlacesFirestoreContract.FIELD_TAG, place.getTag());

        // No imageUrl / heroImageUrl / galleryImageUrls — add after Firebase Storage upload.

        String heroKey = drawableEntryName(context, place.getImageResId());
        if (heroKey != null) {
            f.put("drawableAssetKey", heroKey);
        }
        JSONArray galleryKeys = new JSONArray();
        List<Integer> gallery = place.getGalleryImages();
        if (gallery != null) {
            for (Integer resId : gallery) {
                if (resId == null) {
                    continue;
                }
                String key = drawableEntryName(context, resId);
                if (key != null && !alreadyInJsonArray(galleryKeys, key)) {
                    galleryKeys.put(key);
                }
            }
        }
        if (galleryKeys.length() > 0) {
            f.put("galleryDrawableKeys", galleryKeys);
        }
        return f;
    }

    private static boolean alreadyInJsonArray(JSONArray arr, String key) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            if (key.equals(arr.getString(i))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    /**
     * Stable, Firestore-safe slug (letters, digits, underscores only). Not used as document id by default.
     */
    @NonNull
    public static String slugForPlace(@NonNull Place p) {
        Locale en = Locale.US;
        String city = p.getCity() == null ? "unknown" : p.getCity();
        String name = p.getName() == null ? "unnamed" : p.getName();
        String combined = (city + "_" + name).toLowerCase(en);
        combined = combined.replaceAll("[^a-z0-9]+", "_");
        combined = combined.replaceAll("_+", "_");
        combined = combined.replaceAll("^_+|_+$", "");
        return "place_" + combined;
    }

    @NonNull
    private static String categoryIdFromCategory(@Nullable String category) {
        if (category == null || category.trim().isEmpty()) {
            return "general";
        }
        return category.trim().toLowerCase(Locale.US);
    }

    @Nullable
    private static String drawableEntryName(@NonNull Context context, int resId) {
        if (resId == 0) {
            return null;
        }
        try {
            Resources r = context.getResources();
            return r.getResourceEntryName(resId);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Writes {@link #buildExportJson(Context)} to {@code targetFile} (UTF-8).
     */
    public static void writeExportToFile(@NonNull Context context, @NonNull File targetFile) throws IOException, JSONException {
        String json = buildExportJson(context);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        try (FileOutputStream fos = new FileOutputStream(targetFile, false)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Convenience: writes to {@link Context#getFilesDir()}/{@value #DEFAULT_OUTPUT_FILENAME}.
     *
     * @return absolute path of the written file
     */
    @NonNull
    public static String writeExportToAppFiles(@NonNull Context context) throws IOException, JSONException {
        File out = new File(context.getFilesDir(), DEFAULT_OUTPUT_FILENAME);
        writeExportToFile(context, out);
        return out.getAbsolutePath();
    }
}
