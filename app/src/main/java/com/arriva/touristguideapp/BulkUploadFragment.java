package com.arriva.touristguideapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.arriva.touristguideapp.data.analytics.AuditLogger;
import com.arriva.touristguideapp.data.places.FirestorePlaceDataSource;
import java.util.ArrayList;
import java.util.List;

public class BulkUploadFragment extends Fragment {

    private Button btnCsv, btnJson;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirestorePlaceDataSource dataSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bulk_upload, container, false);
        
        btnCsv = view.findViewById(R.id.btnImportCsv);
        btnJson = view.findViewById(R.id.btnImportJson);
        progressBar = view.findViewById(R.id.pbUpload);
        tvStatus = view.findViewById(R.id.tvUploadStatus);
        
        dataSource = new FirestorePlaceDataSource();

        btnCsv.setOnClickListener(v -> simulateUpload("CSV"));
        btnJson.setOnClickListener(v -> simulateUpload("JSON"));

        return view;
    }

    private void simulateUpload(String type) {
        btnCsv.setEnabled(false);
        btnJson.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Parsing " + type + " data...");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<Place> mockPlaces = createMockPlaces();
            uploadBatch(mockPlaces, type);
        }, 2000);
    }

    private List<Place> createMockPlaces() {
        List<Place> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Place p = new Place();
            p.setName("Bulk Place " + i);
            p.setCity("Pune");
            p.setCatalogStatus("draft");
            list.add(p);
        }
        return list;
    }

    private void uploadBatch(List<Place> places, String type) {
        tvStatus.setText("Uploading " + places.size() + " places...");
        
        // In a real app, use WriteBatch for efficiency
        int[] successCount = {0};
        for (Place p : places) {
            dataSource.savePlace(p).addOnSuccessListener(aVoid -> {
                successCount[0]++;
                if (successCount[0] == places.size()) {
                    onUploadComplete(places.size(), type);
                }
            });
        }
    }

    private void onUploadComplete(int count, String type) {
        progressBar.setVisibility(View.GONE);
        btnCsv.setEnabled(true);
        btnJson.setEnabled(true);
        tvStatus.setText("Successfully uploaded " + count + " places from " + type);
        AuditLogger.logAction("BULK_UPLOAD", "Multiple", "Type: " + type + ", Count: " + count);
    }
}
