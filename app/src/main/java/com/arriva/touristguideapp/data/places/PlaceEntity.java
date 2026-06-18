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
    public String imageUrl;
    public double rating;
    public double latitude;
    public double longitude;
    public boolean isFeatured;
    public long lastSynced;

    public PlaceEntity(@NonNull String id,
                       String name,
                       String category,
                       String city,
                       String district,
                       String description,
                       String imageUrl,
                       double rating,
                       double latitude,
                       double longitude,
                       boolean isFeatured,
                       long lastSynced) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.city = city;
        this.district = district;
        this.description = description;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFeatured = isFeatured;
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
                clean(place.getImageUrl()),
                place.getRating(),
                place.getLatitude(),
                place.getLongitude(),
                place.isTopPick(),
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
        place.setImageUrl(imageUrl);
        place.setRating(rating);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        place.setTopPick(isFeatured);
        place.setTag(isFeatured ? "Featured" : "Saved");
        place.setCatalogStatus(PlacesFirestoreContract.STATUS_PUBLISHED);
        return place;
    }

    @NonNull
    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
