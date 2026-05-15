package com.arriva.touristguideapp.migration;

import android.content.Context;
import com.arriva.touristguideapp.DataProvider;
import com.arriva.touristguideapp.Place;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility to export local {@link DataProvider} data to a JSON format compatible with Firestore import tools.
 */
public class DataProviderFirestoreExporter {

    public static final String DEFAULT_OUTPUT_FILENAME = "places_firestore_export_generated.json";

    /**
     * Exports all places from {@link DataProvider} to a JSON file in the app's internal files directory.
     *
     * @param context Application context
     * @return Absolute path to the generated file
     * @throws IOException   If file writing fails
     * @throws JSONException If JSON construction fails
     */
    public static String writeExportToAppFiles(Context context) throws IOException, JSONException {
        List<Place> places = DataProvider.getAllPlaces();
        
        JSONObject root = new JSONObject();
        root.put("collection", "places");
        root.put("documentCount", places.size());

        JSONArray documents = new JSONArray();
        for (Place place : places) {
            JSONObject doc = new JSONObject();
            doc.put("id", place.getId());

            JSONObject fields = new JSONObject();
            fields.put("status", place.getCatalogStatus() != null ? place.getCatalogStatus() : "published");
            fields.put("name", place.getName());
            fields.put("category", place.getCategory());
            fields.put("city", place.getCity());
            fields.put("isTopPick", place.isTopPick());
            fields.put("description", place.getDescription());
            fields.put("ratingAvg", place.getRating());
            
            // Add other fields if necessary for complete migration
            fields.put("latitude", place.getLatitude());
            fields.put("longitude", place.getLongitude());
            fields.put("budget", place.getBudget());
            fields.put("crowdLevel", place.getCrowdLevel());
            fields.put("bestTime", place.getBestTime());
            fields.put("tips", place.getTips());
            fields.put("funFact", place.getFunFact());
            fields.put("nearestStation", place.getNearestStation());
            fields.put("tag", place.getTag());

            doc.put("fields", fields);
            documents.put(doc);
        }
        root.put("documents", documents);

        File outputFile = new File(context.getFilesDir(), DEFAULT_OUTPUT_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }

        return outputFile.getAbsolutePath();
    }
}
