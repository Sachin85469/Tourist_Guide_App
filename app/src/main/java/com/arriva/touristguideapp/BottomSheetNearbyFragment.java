package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet that displays a distance-sorted list of nearby places.
 *
 * <p>Created by {@link MainActivity} after GPS + Room/Firestore query completes.
 * The caller passes:
 * <ul>
 *   <li>{@link #ARG_TITLE}  — header title string (e.g. "📍 Within 5km of you")</li>
 *   <li>{@link #ARG_PLACES} — serialized place list via {@link #withPlaces}</li>
 * </ul>
 */
public class BottomSheetNearbyFragment extends BottomSheetDialogFragment
        implements PlaceAdapter.OnItemClickListener {

    public static final String TAG = "BottomSheetNearby";

    private static final String ARG_TITLE  = "nearby_title";
    private static final String ARG_PLACES = "nearby_places";

    // Passed at construction time; re-fetched from arguments in onCreateView
    private String title;
    private List<Place> places;

    // ─── Factory ──────────────────────────────────────────────────────────────

    /**
     * Creates a new instance with places already fetched by the caller.
     *
     * @param title  header string shown in the sheet (already contains emoji + radius text)
     * @param places distance-sorted list; must not be null
     */
    public static BottomSheetNearbyFragment withPlaces(@NonNull String title,
                                                       @NonNull List<Place> places) {
        BottomSheetNearbyFragment f = new BottomSheetNearbyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putSerializable(ARG_PLACES, new ArrayList<>(places));
        f.setArguments(args);
        return f;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_nearby_now, container, false);

        // Restore args
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "📍 Nearby places");
            //noinspection unchecked
            ArrayList<Place> serialized =
                    (ArrayList<Place>) getArguments().getSerializable(ARG_PLACES);
            places = serialized != null ? serialized : new ArrayList<>();
        } else {
            title = "📍 Nearby places";
            places = new ArrayList<>();
        }

        // Header
        TextView tvTitle    = root.findViewById(R.id.tvNearbyTitle);
        TextView tvSubtitle = root.findViewById(R.id.tvNearbySubtitle);
        if (tvTitle != null)    tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(places.size() + " place" + (places.size() == 1 ? "" : "s") + " found · sorted by distance");

        // Close button
        View btnClose = root.findViewById(R.id.btnNearbyClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        // Loading frame — hidden (data already loaded before sheet is shown)
        View loadingFrame = root.findViewById(R.id.nearbyLoadingFrame);
        if (loadingFrame != null) loadingFrame.setVisibility(View.GONE);

        // RecyclerView / empty state
        RecyclerView recycler  = root.findViewById(R.id.recyclerNearby);
        View emptyLayout       = root.findViewById(R.id.nearbyEmptyLayout);

        if (places.isEmpty()) {
            if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
            if (recycler != null)    recycler.setVisibility(View.GONE);
        } else {
            if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
            if (recycler != null) {
                recycler.setVisibility(View.VISIBLE);
                recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                PlaceAdapter adapter = new PlaceAdapter(places, this);
                recycler.setAdapter(adapter);
                // Slide-in animation
                recycler.setAlpha(0f);
                recycler.animate().alpha(1f).setDuration(250).start();
            }
        }

        return root;
    }

    // ─── Place click ──────────────────────────────────────────────────────────

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(requireContext(), PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        startActivity(intent);
        if (requireActivity() != null) {
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        dismiss();
    }
}
