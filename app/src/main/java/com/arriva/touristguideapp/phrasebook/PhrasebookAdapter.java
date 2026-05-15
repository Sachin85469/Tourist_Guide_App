package com.arriva.touristguideapp.phrasebook;

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
import com.arriva.touristguideapp.data.phrasebook.Phrase;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookPreferences;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for phrase cards (English, Marathi, Hindi + TTS actions).
 */
public class PhrasebookAdapter extends RecyclerView.Adapter<PhrasebookAdapter.PhraseViewHolder> {

    public interface PhraseActionListener {
        void onSpeakMarathi(@NonNull Phrase phrase);

        void onSpeakHindi(@NonNull Phrase phrase);

        void onFavoriteToggled(@NonNull Phrase phrase, boolean isFavorite);
    }

    private final List<Phrase> phrases = new ArrayList<>();
    private final PhrasebookPreferences preferences;
    private final PhraseActionListener actionListener;
    @Nullable
    private String speakingPhraseId;
    @Nullable
    private PhrasebookTtsHelper.Language speakingLanguage;

    public PhrasebookAdapter(@NonNull PhrasebookPreferences preferences,
                             @NonNull PhraseActionListener actionListener) {
        this.preferences = preferences;
        this.actionListener = actionListener;
    }

    public void submitList(@NonNull List<Phrase> newPhrases) {
        phrases.clear();
        phrases.addAll(newPhrases);
        notifyDataSetChanged();
    }

    public void setSpeakingState(@Nullable String phraseId, @Nullable PhrasebookTtsHelper.Language language) {
        speakingPhraseId = phraseId;
        speakingLanguage = language;
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
        Phrase phrase = phrases.get(position);
        boolean isFavorite = preferences.isFavorite(phrase.getId());
        boolean marathiActive = phrase.getId().equals(speakingPhraseId)
                && speakingLanguage == PhrasebookTtsHelper.Language.MARATHI;
        boolean hindiActive = phrase.getId().equals(speakingPhraseId)
                && speakingLanguage == PhrasebookTtsHelper.Language.HINDI;
        holder.bind(phrase, isFavorite, marathiActive, hindiActive, actionListener, preferences);
    }

    @Override
    public int getItemCount() {
        return phrases.size();
    }

    static class PhraseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEnglish;
        private final TextView tvMarathi;
        private final TextView tvHindi;
        private final TextView tvCategory;
        private final ImageButton btnFavorite;
        private final ImageButton btnCopy;
        private final MaterialButton btnMarathiTts;
        private final MaterialButton btnHindiTts;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnglish = itemView.findViewById(R.id.tvPhraseEnglish);
            tvMarathi = itemView.findViewById(R.id.tvPhraseMarathi);
            tvHindi = itemView.findViewById(R.id.tvPhraseHindi);
            tvCategory = itemView.findViewById(R.id.tvPhraseCategory);
            btnFavorite = itemView.findViewById(R.id.btnPhraseFavorite);
            btnCopy = itemView.findViewById(R.id.btnPhraseCopy);
            btnMarathiTts = itemView.findViewById(R.id.btnSpeakMarathi);
            btnHindiTts = itemView.findViewById(R.id.btnSpeakHindi);
        }

        void bind(@NonNull Phrase phrase,
                  boolean isFavorite,
                  boolean marathiActive,
                  boolean hindiActive,
                  @NonNull PhraseActionListener listener,
                  @NonNull PhrasebookPreferences preferences) {
            Context context = itemView.getContext();
            tvEnglish.setText(phrase.getEnglishText());
            tvMarathi.setText(phrase.getMarathiText());
            tvHindi.setText(phrase.getHindiText());
            tvCategory.setText(phrase.getCategory());

            btnFavorite.setImageResource(isFavorite
                    ? R.drawable.ic_favorite
                    : R.drawable.ic_favorite_border);
            btnFavorite.setOnClickListener(v -> {
                preferences.toggleFavorite(phrase.getId());
                listener.onFavoriteToggled(phrase, preferences.isFavorite(phrase.getId()));
            });

            btnCopy.setOnClickListener(v -> copyPhrase(context, phrase));

            btnMarathiTts.setOnClickListener(v -> listener.onSpeakMarathi(phrase));
            btnHindiTts.setOnClickListener(v -> listener.onSpeakHindi(phrase));

            btnMarathiTts.setStrokeColorResource(marathiActive ? R.color.primary_dark : R.color.adaptive_card_stroke);
            btnHindiTts.setStrokeColorResource(hindiActive ? R.color.primary_dark : R.color.adaptive_card_stroke);
            if (marathiActive) {
                btnMarathiTts.setBackgroundColor(context.getColor(R.color.primary_light));
            } else {
                btnMarathiTts.setBackgroundColor(context.getColor(R.color.white));
            }
            if (hindiActive) {
                btnHindiTts.setBackgroundColor(context.getColor(R.color.primary_light));
            } else {
                btnHindiTts.setBackgroundColor(context.getColor(R.color.white));
            }
        }

        private static void copyPhrase(@NonNull Context context, @NonNull Phrase phrase) {
            String text = phrase.getEnglishText() + "\n"
                    + phrase.getMarathiText() + "\n"
                    + phrase.getHindiText();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("phrase", text));
                Toast.makeText(context, R.string.phrasebook_copied, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
