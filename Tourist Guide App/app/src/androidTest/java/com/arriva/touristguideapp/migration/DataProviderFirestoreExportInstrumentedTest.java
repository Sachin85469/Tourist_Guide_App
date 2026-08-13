package com.arriva.touristguideapp.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Generates {@link DataProviderFirestoreExporter#DEFAULT_OUTPUT_FILENAME} on device from live {@link com.arriva.touristguideapp.DataProvider} data.
 * <p>
 * Run (from project root):<br>
 * {@code ./gradlew :app:connectedDebugAndroidTest --tests com.arriva.touristguideapp.migration.DataProviderFirestoreExportInstrumentedTest}<br>
 * Pull file:<br>
 * {@code adb exec-out run-as com.arriva.touristguideapp cat files/places_firestore_export_generated.json > tools/places_firestore_export_generated.json}<br>
 * (Or use Device File Explorer in Android Studio: {@code /data/data/com.arriva.touristguideapp/files/}.)
 */
@RunWith(AndroidJUnit4.class)
public class DataProviderFirestoreExportInstrumentedTest {

    @Test
    public void export_generatesValidFirestoreJson() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String path = DataProviderFirestoreExporter.writeExportToAppFiles(ctx);
        File f = new File(path);
        assertTrue(f.exists());
        assertTrue(f.length() > 100);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) {
                buf.write(b, 0, n);
            }
        }
        JSONObject root = new JSONObject(buf.toString(StandardCharsets.UTF_8.name()));
        assertEquals("places", root.getString("collection"));
        assertEquals(19, root.getInt("documentCount"));
        JSONArray docs = root.getJSONArray("documents");
        assertEquals(19, docs.length());

        JSONObject first = docs.getJSONObject(0);
        assertTrue(first.has("id"));
        assertTrue(first.has("fields"));
        JSONObject fields = first.getJSONObject("fields");
        assertTrue(fields.has("status"));
        assertTrue(fields.has("name"));
        assertTrue(fields.has("category"));
        assertTrue(fields.has("city"));
        assertTrue(fields.has("isTopPick"));
        assertTrue(fields.has("description"));
        assertTrue(fields.has("ratingAvg"));
    }
}
