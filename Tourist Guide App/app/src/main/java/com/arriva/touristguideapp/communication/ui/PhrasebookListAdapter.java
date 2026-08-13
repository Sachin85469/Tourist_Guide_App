package com.arriva.touristguideapp.communication.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationPreferences;
import com.arriva.touristguideapp.data.phrasebook.Phrase;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PhrasebookListAdapter extends RecyclerView.Adapter<PhrasebookListAdapter.PhraseViewHolder> {

    public interface PhraseActionListener {
        void onSpeak(@NonNull Phrase phrase, @NonNull String textToSpeak, @NonNull String language);
        void onFavoriteToggled();
    }

    private final List<Phrase> items = new ArrayList<>();
    private final CommunicationPreferences preferences;
    private final PhraseActionListener listener;
    @Nullable
    private String speakingPhraseId;
    @NonNull
    private String selectedLanguage = "Marathi";

    public PhrasebookListAdapter(@NonNull CommunicationPreferences preferences,
                                 @NonNull PhraseActionListener listener) {
        this.preferences = preferences;
        this.listener = listener;
    }

    public void submitItems(@NonNull List<Phrase> newItems, @NonNull String language) {
        this.selectedLanguage = language;
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSpeakingPhraseId(@Nullable String phraseId) {
        speakingPhraseId = phraseId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhraseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phrase_card, parent, false);
        return new PhraseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhraseViewHolder holder, int position) {
        holder.bind(items.get(position), selectedLanguage, preferences, listener, speakingPhraseId);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PhraseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOriginal;
        private final TextView tvTranslated;
        private final TextView tvCategory;
        private final ImageButton btnFavorite;
        private final ImageButton btnCopy;
        private final MaterialButton btnSpeak;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginal = itemView.findViewById(R.id.tvPhraseOriginal);
            tvTranslated = itemView.findViewById(R.id.tvPhraseTranslated);
            tvCategory = itemView.findViewById(R.id.tvPhraseCategory);
            btnFavorite = itemView.findViewById(R.id.btnPhraseFavorite);
            btnCopy = itemView.findViewById(R.id.btnPhraseCopy);
            btnSpeak = itemView.findViewById(R.id.btnPhraseSpeak);

            // Ensure loaders and errors are hidden for Phrasebook
            View progress = itemView.findViewById(R.id.progressPhraseTranslation);
            if (progress != null) progress.setVisibility(View.GONE);
            View error = itemView.findViewById(R.id.tvPhraseTranslationError);
            if (error != null) error.setVisibility(View.GONE);
            View retry = itemView.findViewById(R.id.btnPhraseRetryTranslation);
            if (retry != null) retry.setVisibility(View.GONE);
            View voiceSelect = itemView.findViewById(R.id.btnPhraseVoiceSelect);
            if (voiceSelect != null) voiceSelect.setVisibility(View.GONE);
        }

        void bind(@NonNull Phrase phrase,
                  @NonNull String selectedLanguage,
                  @NonNull CommunicationPreferences preferences,
                  @NonNull PhraseActionListener listener,
                  @Nullable String speakingPhraseId) {
            Context context = itemView.getContext();
            
            tvOriginal.setText(phrase.getEnglish());
            tvCategory.setText(phrase.getCategory());
            
            // Unified translation logic as requested
            String translation;
            if (selectedLanguage.equals("Hindi")) {
                translation = phrase.getHindi();
            } else {
                translation = phrase.getMarathi();
            }
            tvTranslated.setText(translation);

            btnFavorite.setImageResource(preferences.isFavorite(phrase.getId())
                    ? R.drawable.ic_favorite
                    : R.drawable.ic_favorite_border);
            btnFavorite.setOnClickListener(v -> {
                preferences.toggleFavorite(phrase.getId());
                listener.onFavoriteToggled();
            });

            boolean speaking = phrase.getId().equals(speakingPhraseId);
            btnSpeak.setBackgroundColor(context.getColor(
                    speaking ? R.color.primary_light : R.color.white));

            // Speak currently visible text
            btnSpeak.setOnClickListener(v -> listener.onSpeak(phrase, translation, selectedLanguage));

            btnCopy.setOnClickListener(v -> {
                String copyText = phrase.getEnglish() + "\n" + translation;
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("phrase", copyText));
                    Toast.makeText(context, R.string.phrasebook_copied, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
