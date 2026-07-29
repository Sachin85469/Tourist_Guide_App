package com.arriva.touristguideapp.data.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;
import java.util.UUID;

/** Uploads place images and returns only their Firebase Storage path/reference. */
public final class PlaceImageStorage {

    private final ContentResolver contentResolver;
    private final FirebaseStorage storage;

    public PlaceImageStorage(@NonNull Context context) {
        this(context.getApplicationContext().getContentResolver(), FirebaseStorage.getInstance());
    }

    PlaceImageStorage(@NonNull ContentResolver contentResolver, @NonNull FirebaseStorage storage) {
        this.contentResolver = contentResolver;
        this.storage = storage;
    }

    /**
     * Uploads the cover image under the place id and returns its stable Storage path. The returned
     * value is suitable for the {@code imageRef} Firestore field and is never a download URL.
     */
    @NonNull
    public Task<String> uploadCoverImage(@NonNull Uri source, @NonNull String placeId) {
        String safePlaceId = placeId.trim();
        if (safePlaceId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("A place id is required before uploading an image."));
        }

        String extension = extensionFor(source);
        String filename = "cover_" + UUID.randomUUID() + "." + extension;
        StorageReference reference = storage.getReference()
                .child("places")
                .child(safePlaceId)
                .child(filename);

        return reference.putFile(source)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception error = task.getException();
                        throw error != null ? error : new IllegalStateException("Image upload failed.");
                    }
                    return reference.getPath();
                });
    }

    private String extensionFor(@NonNull Uri source) {
        String mimeType = contentResolver.getType(source);
        String extension = mimeType == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null || extension.trim().isEmpty()) {
            return "jpg";
        }
        return extension.toLowerCase(Locale.US);
    }
}
