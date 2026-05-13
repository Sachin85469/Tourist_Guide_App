package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.arriva.touristguideapp.data.notifications.NotificationRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AdminBroadcastFragment extends Fragment {

    private EditText etTitle, etBody;
    private Spinner spinnerType;
    private Button btnSend;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_broadcast, container, false);
        
        etTitle = view.findViewById(R.id.etBroadcastTitle);
        etBody = view.findViewById(R.id.etBroadcastBody);
        spinnerType = view.findViewById(R.id.spinnerNotificationType);
        btnSend = view.findViewById(R.id.btnSendBroadcast);

        String[] types = {
            NotificationRepository.TYPE_ADMIN,
            NotificationRepository.TYPE_TRENDING,
            NotificationRepository.TYPE_NEARBY,
            NotificationRepository.TYPE_FAVORITES,
            NotificationRepository.TYPE_REVIEWS
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendBroadcast());

        return view;
    }

    private void sendBroadcast() {
        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(getContext(), "Title and Body are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // In a real production environment, this would trigger a Cloud Function.
        // For this phase, we'll log it and store it in a 'broadcasts' collection in Firestore.
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("title", title);
        broadcast.put("body", body);
        broadcast.put("type", type);
        broadcast.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        broadcast.put("status", "pending");

        FirebaseFirestore.getInstance().collection("broadcasts")
            .add(broadcast)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(getContext(), "Broadcast scheduled successfully!", Toast.LENGTH_LONG).show();
                etTitle.setText("");
                etBody.setText("");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to schedule broadcast: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
