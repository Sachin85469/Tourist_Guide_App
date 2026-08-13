package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class PhraseMapper {

    private PhraseMapper() {
    }

    @Nullable
    public static PhraseDto fromSnapshot(@NonNull DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }
        PhraseDto dto = snapshot.toObject(PhraseDto.class);
        if (dto == null) {
            return null;
        }
        dto.setId(snapshot.getId());
        return dto.isValid() ? dto : null;
    }

    @NonNull
    public static List<Phrase> toPhrases(@NonNull List<PhraseDto> dtos) {
        List<Phrase> result = new ArrayList<>(dtos.size());
        for (PhraseDto dto : dtos) {
            Phrase phrase = toPhrase(dto);
            if (phrase != null) {
                result.add(phrase);
            }
        }
        return result;
    }

    @Nullable
    public static Phrase toPhrase(@NonNull PhraseDto dto) {
        if (!dto.isValid()) {
            return null;
        }
        return new Phrase(
                dto.getId(),
                dto.getEnglishText() != null ? dto.getEnglishText() : "",
                dto.getHindiText() != null ? dto.getHindiText() : "",
                dto.getMarathiText() != null ? dto.getMarathiText() : "",
                dto.getCategory() != null ? dto.getCategory().trim() : ""
        );
    }
}
