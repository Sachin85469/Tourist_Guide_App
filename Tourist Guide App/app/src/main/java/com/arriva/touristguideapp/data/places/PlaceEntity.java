package com.arriva.touristguideapp.data.places;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.arriva.touristguideapp.Place;

@Entity(
        tableName = "places",
        indices = {
                @Index("city"),
                @Index("category"),
                @Index("lastSynced")
        }
)
public class PlaceEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String category;
    public String city;
    public String district;
    public String description;
    public double rating;
    public double latitude;
    public double longitude;
    public boolean isFeatured;
    /** Firebase Storage path/reference for the cover image. */
    public String imageRef;
    /** Newline-delimited Firebase Storage paths for gallery images. */
    public String galleryImageRefs;
    public long lastSynced;

    public PlaceEntity(@NonNull String id,
                       String name,
                       String category,
                       String city,
                       String district,
                       String description,
                       double rating,
                       double latitude,
                       double longitude,
                       boolean isFeatured,
                       String imageRef,
                       String galleryImageRefs,
                       long lastSynced) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.city = city;
        this.district = district;
        this.description = description;
        this.rating = rating;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFeatured = isFeatured;
        this.imageRef = imageRef;
        this.galleryImageRefs = galleryImageRefs;
        this.lastSynced = lastSynced;
    }

    @NonNull
    public static PlaceEntity fromPlace(@NonNull Place place, long lastSynced) {
        String id = clean(place.getId());
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        return new PlaceEntity(
                id,
                clean(place.getName()),
                clean(place.getCategory()),
                clean(place.getCity()),
                "",
                clean(place.getDescription()),
                place.getRating(),
                place.getLatitude(),
                place.getLongitude(),
                place.isTopPick(),
                clean(place.getImageRef()),
                encodeImageRefs(place.getGalleryImageRefs()),
                lastSynced
        );
    }

    @NonNull
    public Place toPlace() {
        Place place = new Place();
        place.setId(id);
        place.setName(name);
        place.setCategory(category);
        place.setCity(city);
        place.setDescription(description);
        place.setRating(rating);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        place.setTopPick(isFeatured);
        place.setImageRef(imageRef);
        place.setGalleryImageRefs(decodeImageRefs(galleryImageRefs));
        place.setTag(isFeatured ? "Featured" : "Saved");
        place.setCatalogStatus(PlacesFirestoreContract.STATUS_PUBLISHED);
        return place;
    }

    @NonNull
    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private static String encodeImageRefs(@NonNull java.util.List<String> refs) {
        return android.text.TextUtils.join("\n", refs);
    }

    @NonNull
    private static java.util.List<String> decodeImageRefs(String value) {
        java.util.List<String> refs = new java.util.ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return refs;
        }
        for (String ref : value.split("\\n")) {
            String cleanRef = clean(ref);
            if (!cleanRef.isEmpty()) {
                refs.add(cleanRef);
            }
        }
        return refs;
    }
}
