package com.arriva.touristguideapp.data.places;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PlaceEntity> places);

    @Query("SELECT * FROM places WHERE LOWER(city) = LOWER(:city) ORDER BY rating DESC, name COLLATE NOCASE ASC")
    List<PlaceEntity> getByCity(String city);

    @Query("SELECT * FROM places WHERE category IN (:categories) ORDER BY rating DESC, name COLLATE NOCASE ASC")
    List<PlaceEntity> getByCategories(List<String> categories);

    @Query("SELECT * FROM places ORDER BY rating DESC, name COLLATE NOCASE ASC")
    List<PlaceEntity> getAll();

    @Query("DELETE FROM places WHERE lastSynced < :timestamp")
    void deleteOlderThan(long timestamp);

    @Query("SELECT COUNT(*) FROM places")
    int getCount();
}
