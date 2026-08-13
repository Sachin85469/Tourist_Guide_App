package com.arriva.touristguideapp;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.arriva.touristguideapp.data.analytics.AuditLogger;
import com.arriva.touristguideapp.data.places.FirestorePlaceDataSource;
import com.arriva.touristguideapp.data.repository.PlaceImageStorage;
import com.arriva.touristguideapp.utils.ImageUtils;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class PlaceEditorActivity extends BaseActivity {

    private EditText etName, etCity, etDescription, etLat, etLng;
    private Spinner spinnerStatus;
    private Button btnSave, btnDelete;
    private Button btnChooseImage;
    private ImageView ivPlaceImage;
    private TextView tvImageStatus;
    private Place currentPlace;
    private FirestorePlaceDataSource dataSource;
    private PlaceImageStorage imageStorage;
    private Uri selectedImageUri;
    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedImageUri = uri;
                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .placeholder(ImageUtils.LOADING_PLACEHOLDER)
                        .error(ImageUtils.DEFAULT_TRAVEL_IMAGE)
                        .into(ivPlaceImage);
                tvImageStatus.setText("Image selected. It will upload when the place is saved.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_editor);

        dataSource = new FirestorePlaceDataSource();
        imageStorage = new PlaceImageStorage(this);
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
        btnChooseImage.setOnClickListener(v -> imagePicker.launch("image/*"));
    }

    private void initViews() {
        etName = findViewById(R.id.etEditorName);
        etCity = findViewById(R.id.etEditorCity);
        etDescription = findViewById(R.id.etEditorDescription);
        etLat = findViewById(R.id.etEditorLat);
        etLng = findViewById(R.id.etEditorLng);
        spinnerStatus = findViewById(R.id.spinnerEditorStatus);
        btnSave = findViewById(R.id.btnSavePlace);
        btnDelete = findViewById(R.id.btnDeletePlace);
        btnChooseImage = findViewById(R.id.btnChoosePlaceImage);
        ivPlaceImage = findViewById(R.id.ivEditorPlaceImage);
        tvImageStatus = findViewById(R.id.tvEditorImageStatus);

        String[] statuses = {"published", "draft", "pending", "archived"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void populateFields() {
        etName.setText(currentPlace.getName());
        etCity.setText(currentPlace.getCity());
        etDescription.setText(currentPlace.getDescription());
        etLat.setText(String.valueOf(currentPlace.getLatitude()));
        etLng.setText(String.valueOf(currentPlace.getLongitude()));
        ImageUtils.loadPlaceMainImage(ivPlaceImage, currentPlace);
        if (currentPlace.getImageRef() != null) {
            tvImageStatus.setText("Current image is linked to Firebase Storage.");
        }
        
        // Find status index
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatus.getAdapter();
        int pos = adapter.getPosition(currentPlace.getCatalogStatus());
        if (pos >= 0) spinnerStatus.setSelection(pos);
    }

    private void savePlace() {
        currentPlace.setName(etName.getText().toString().trim());
        currentPlace.setCity(etCity.getText().toString().trim());
        currentPlace.setDescription(etDescription.getText().toString().trim());
        currentPlace.setCatalogStatus(spinnerStatus.getSelectedItem().toString());

        try {
            currentPlace.setLatitude(Double.parseDouble(etLat.getText().toString()));
            currentPlace.setLongitude(Double.parseDouble(etLng.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPlace.getId() == null || currentPlace.getId().trim().isEmpty()) {
            currentPlace.setId(dataSource.createPlaceId());
        }

        setSaving(true, selectedImageUri != null ? "Uploading image..." : "Saving place...");
        Task<String> imageTask = selectedImageUri == null
                ? Tasks.forResult(currentPlace.getImageRef())
                : imageStorage.uploadCoverImage(selectedImageUri, currentPlace.getId());

        imageTask.addOnSuccessListener(imageRef -> {
            if (imageRef != null) {
                currentPlace.setImageRef(imageRef);
            }
            dataSource.savePlace(currentPlace).addOnCompleteListener(task -> {
                setSaving(false, null);
            if (task.isSuccessful()) {
                AuditLogger.logAction("PLACE_SAVED", currentPlace.getId(), currentPlace.getName());
                Toast.makeText(this, "Place saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
            });
        }).addOnFailureListener(error -> {
            setSaving(false, "Image upload failed. Please try again.");
            Toast.makeText(this, "Image upload failed. The place was not saved.", Toast.LENGTH_LONG).show();
        });
    }

    private void setSaving(boolean saving, String status) {
        btnSave.setEnabled(!saving);
        btnChooseImage.setEnabled(!saving);
        if (status != null) {
            tvImageStatus.setText(status);
        }
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
        dataSource.deletePlace(currentPlace.getId())
            .addOnSuccessListener(aVoid -> {
                AuditLogger.logAction("PLACE_DELETED", currentPlace.getId(), currentPlace.getName());
                Toast.makeText(this, "Place deleted", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("PlaceEditorActivity", "Admin delete failed", e);
                Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
