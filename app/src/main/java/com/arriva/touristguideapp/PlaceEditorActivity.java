package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.arriva.touristguideapp.data.analytics.AuditLogger;
import com.arriva.touristguideapp.data.places.FirestorePlaceDataSource;

public class PlaceEditorActivity extends AppCompatActivity {

    private EditText etName, etCity, etDescription, etImageUrl, etLat, etLng;
    private Spinner spinnerStatus;
    private Button btnSave, btnDelete;
    private Place currentPlace;
    private FirestorePlaceDataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_editor);

        dataSource = new FirestorePlaceDataSource();
        initViews();

        currentPlace = (Place) getIntent().getSerializableExtra("place");
        if (currentPlace != null) {
            populateFields();
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            currentPlace = new Place();
        }

        btnSave.setOnClickListener(v -> savePlace());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void initViews() {
        etName = findViewById(R.id.etEditorName);
        etCity = findViewById(R.id.etEditorCity);
        etDescription = findViewById(R.id.etEditorDescription);
        etImageUrl = findViewById(R.id.etEditorImageUrl);
        etLat = findViewById(R.id.etEditorLat);
        etLng = findViewById(R.id.etEditorLng);
        spinnerStatus = findViewById(R.id.spinnerEditorStatus);
        btnSave = findViewById(R.id.btnSavePlace);
        btnDelete = findViewById(R.id.btnDeletePlace);

        String[] statuses = {"published", "draft", "pending", "archived"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void populateFields() {
        etName.setText(currentPlace.getName());
        etCity.setText(currentPlace.getCity());
        etDescription.setText(currentPlace.getDescription());
        etImageUrl.setText(currentPlace.getImageUrl());
        etLat.setText(String.valueOf(currentPlace.getLatitude()));
        etLng.setText(String.valueOf(currentPlace.getLongitude()));
        
        // Find status index
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatus.getAdapter();
        int pos = adapter.getPosition(currentPlace.getCatalogStatus());
        if (pos >= 0) spinnerStatus.setSelection(pos);
    }

    private void savePlace() {
        currentPlace.setName(etName.getText().toString().trim());
        currentPlace.setCity(etCity.getText().toString().trim());
        currentPlace.setDescription(etDescription.getText().toString().trim());
        currentPlace.setImageUrl(etImageUrl.getText().toString().trim());
        currentPlace.setCatalogStatus(spinnerStatus.getSelectedItem().toString());

        try {
            currentPlace.setLatitude(Double.parseDouble(etLat.getText().toString()));
            currentPlace.setLongitude(Double.parseDouble(etLng.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        dataSource.savePlace(currentPlace).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AuditLogger.logAction("PLACE_SAVED", currentPlace.getId(), currentPlace.getName());
                Toast.makeText(this, "Place saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Place")
            .setMessage("Are you sure you want to delete this place?")
            .setPositiveButton("Delete", (d, w) -> deletePlace())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deletePlace() {
        dataSource.deletePlace(currentPlace.getId()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AuditLogger.logAction("PLACE_DELETED", currentPlace.getId(), currentPlace.getName());
                Toast.makeText(this, "Place deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
