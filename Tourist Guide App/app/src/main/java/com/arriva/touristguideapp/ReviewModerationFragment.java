package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.analytics.AuditLogger;
import com.arriva.touristguideapp.data.reviews.FirestoreReviewDataSource;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReviewModerationFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private FirestoreReviewDataSource dataSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_moderation, container, false);
        recyclerView = view.findViewById(R.id.rvModerationReviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        dataSource = new FirestoreReviewDataSource();
        loadReports();
        
        return view;
    }

    private void loadReports() {
        dataSource.fetchReports().addOnSuccessListener(queryDocumentSnapshots -> {
            adapter = new ReportAdapter(queryDocumentSnapshots.getDocuments());
            recyclerView.setAdapter(adapter);
        });
    }

    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
        private List<DocumentSnapshot> reports;

        public ReportAdapter(List<DocumentSnapshot> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = reports.get(position);
            String reason = doc.getString("reason");
            String placeId = doc.getString("placeId");
            String reviewUserId = doc.getString("reviewUserId");
            
            holder.text1.setText("Reason: " + reason);
            holder.text2.setText("Place: " + placeId + " | User: " + reviewUserId);
            
            holder.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Moderate Review")
                    .setItems(new String[]{"Delete Review", "Hide Review", "Dismiss Report"}, (d, w) -> {
                        if (w == 0) deleteReview(doc, placeId, reviewUserId);
                        else if (w == 1) hideReview(doc, placeId, reviewUserId);
                        else dismissReport(doc);
                    })
                    .show();
            });
        }

        @Override
        public int getItemCount() { return reports.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }

    private void deleteReview(DocumentSnapshot reportDoc, String placeId, String userId) {
        dataSource.deleteReview(placeId, userId).addOnSuccessListener(aVoid -> {
            AuditLogger.logAction("REVIEW_DELETED_MODERATION", userId, "From place: " + placeId);
            dismissReport(reportDoc);
        });
    }

    private void hideReview(DocumentSnapshot reportDoc, String placeId, String userId) {
        dataSource.updateReviewStatus(placeId, userId, "hidden").addOnSuccessListener(aVoid -> {
            AuditLogger.logAction("REVIEW_HIDDEN_MODERATION", userId, "From place: " + placeId);
            dismissReport(reportDoc);
        });
    }

    private void dismissReport(DocumentSnapshot reportDoc) {
        reportDoc.getReference().update("status", "resolved").addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Report resolved", Toast.LENGTH_SHORT).show();
            loadReports();
        });
    }
}
