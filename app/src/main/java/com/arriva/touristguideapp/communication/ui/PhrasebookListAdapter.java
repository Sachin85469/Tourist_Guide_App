package com.arriva.touristguideapp.communication.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
        void onSpeak(@NonNull Phrase phrase, @NonNull String textToSpeak);

        void onFavoriteToggled();
    }

    private final List<PhraseDisplayItem> items = new ArrayList<>();
    private final CommunicationPreferences preferences;
    private final PhraseActionListener listener;
    @Nullable
    private String speakingPhraseId;

    public PhrasebookListAdapter(@NonNull CommunicationPreferences preferences,
                                 @NonNull PhraseActionListener listener) {
        this.preferences = preferences;
        this.listener = listener;
    }

    public void submitItems(@NonNull List<PhraseDisplayItem> newItems) {
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
        holder.bind(items.get(position), preferences, listener, speakingPhraseId);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PhraseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOriginal;
        private final TextView tvTranslated;
        private final TextView tvCategory;
        private final TextView tvTranslationError;
        private final ProgressBar progressTranslation;
        private final ImageButton btnFavorite;
        private final ImageButton btnCopy;
        private final MaterialButton btnSpeak;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginal = itemView.findViewById(R.id.tvPhraseOriginal);
            tvTranslated = itemView.findViewById(R.id.tvPhraseTranslated);
            tvCategory = itemView.findViewById(R.id.tvPhraseCategory);
            tvTranslationError = itemView.findViewById(R.id.tvPhraseTranslationError);
            progressTranslation = itemView.findViewById(R.id.progressPhraseTranslation);
            btnFavorite = itemView.findViewById(R.id.btnPhraseFavorite);
            btnCopy = itemView.findViewById(R.id.btnPhraseCopy);
            btnSpeak = itemView.findViewById(R.id.btnPhraseSpeak);
        }

        void bind(@NonNull PhraseDisplayItem item,
                  @NonNull CommunicationPreferences preferences,
                  @NonNull PhraseActionListener listener,
                  @Nullable String speakingPhraseId) {
            Context context = itemView.getContext();
            Phrase phrase = item.getPhrase();
            tvOriginal.setText(phrase.getBaseText());
            tvCategory.setText(phrase.getCategory());

            btnFavorite.setImageResource(preferences.isFavorite(phrase.getId())
                    ? R.drawable.ic_favorite
                    : R.drawable.ic_favorite_border);
            btnFavorite.setOnClickListener(v -> {
                preferences.toggleFavorite(phrase.getId());
                listener.onFavoriteToggled();
            });

            switch (item.getState()) {
                case LOADING:
                    progressTranslation.setVisibility(View.VISIBLE);
                    tvTranslated.setVisibility(View.GONE);
                    tvTranslationError.setVisibility(View.GONE);
                    break;
                case ERROR:
                    progressTranslation.setVisibility(View.GONE);
                    tvTranslated.setVisibility(View.GONE);
                    tvTranslationError.setVisibility(View.VISIBLE);
                    tvTranslationError.setText(item.getErrorMessage() != null
                            ? item.getErrorMessage()
                            : context.getString(R.string.communication_translation_failed));
                    break;
                case READY:
                    progressTranslation.setVisibility(View.GONE);
                    tvTranslationError.setVisibility(View.GONE);
                    tvTranslated.setVisibility(View.VISIBLE);
                    tvTranslated.setText(item.getTranslatedText() != null
                            ? item.getTranslatedText()
                            : phrase.getBaseText());
                    break;
                default:
                    progressTranslation.setVisibility(View.GONE);
                    tvTranslationError.setVisibility(View.GONE);
                    tvTranslated.setVisibility(View.VISIBLE);
                    tvTranslated.setText("…");
                    break;
            }

            boolean speaking = phrase.getId().equals(speakingPhraseId);
            btnSpeak.setBackgroundColor(context.getColor(
                    speaking ? R.color.primary_light : R.color.white));

            btnSpeak.setOnClickListener(v -> {
                String text = item.getTranslatedText() != null && item.getState() == PhraseDisplayItem.TranslationUiState.READY
                        ? item.getTranslatedText()
                        : phrase.getBaseText();
                listener.onSpeak(phrase, text);
            });

            btnCopy.setOnClickListener(v -> {
                String copy = phrase.getBaseText();
                if (item.getTranslatedText() != null) {
                    copy = copy + "\n" + item.getTranslatedText();
                }
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("phrase", copy));
                    Toast.makeText(context, R.string.phrasebook_copied, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
