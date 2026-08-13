package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.places.FirestorePlaceDataSource;
import com.arriva.touristguideapp.data.places.PlaceMapper;
import java.util.ArrayList;

public class ManagePlacesFragment extends Fragment {

    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private FirestorePlaceDataSource dataSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_places, container, false);
        recyclerView = view.findViewById(R.id.rvAdminPlaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        dataSource = new FirestorePlaceDataSource();
        loadPlaces();
        
        return view;
    }

    private void loadPlaces() {
        dataSource.fetchAllPlaces(50).addOnSuccessListener(queryDocumentSnapshots -> {
            java.util.List<Place> places = PlaceMapper.toPlaces(PlaceMapper.fromSnapshots(queryDocumentSnapshots.getDocuments()));
            adapter = new PlaceAdapter(places, place -> {
                Intent intent = new Intent(getActivity(), PlaceEditorActivity.class);
                intent.putExtra("place", place);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaces();
    }
}
