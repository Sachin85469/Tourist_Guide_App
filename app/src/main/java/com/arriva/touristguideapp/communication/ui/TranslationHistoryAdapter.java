package com.arriva.touristguideapp.communication.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationPreferences;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;

import java.util.List;

public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryClick(@NonNull CommunicationPreferences.TranslationHistoryEntry entry);
    }

    private final List<CommunicationPreferences.TranslationHistoryEntry> entries;
    private final OnHistoryClickListener listener;

    public TranslationHistoryAdapter(@NonNull List<CommunicationPreferences.TranslationHistoryEntry> entries,
                                     @NonNull OnHistoryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(entries.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMeta;
        private final TextView tvSource;
        private final TextView tvTarget;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMeta = itemView.findViewById(R.id.tvHistoryMeta);
            tvSource = itemView.findViewById(R.id.tvHistorySource);
            tvTarget = itemView.findViewById(R.id.tvHistoryTarget);
        }

        void bind(@NonNull CommunicationPreferences.TranslationHistoryEntry entry,
                  @NonNull OnHistoryClickListener listener) {
            String sourceName = LanguageRegistry.requireFromCode(entry.sourceCode).getDisplayName();
            String targetName = LanguageRegistry.requireFromCode(entry.targetCode).getDisplayName();
            tvMeta.setText(sourceName + " → " + targetName);
            tvSource.setText(entry.sourceText);
            tvTarget.setText(entry.translatedText);
            itemView.setOnClickListener(v -> listener.onHistoryClick(entry));
        }
    }
}
