package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {PlaceEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "arriva_places.db";
    private static volatile AppDatabase instance;

    public abstract PlaceDao placeDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            if (!hasColumn(database, "places", "imageUrl")) {
                return;
            }

            database.execSQL("CREATE TABLE IF NOT EXISTS `places_new` ("
                    + "`id` TEXT NOT NULL, "
                    + "`name` TEXT, "
                    + "`category` TEXT, "
                    + "`city` TEXT, "
                    + "`district` TEXT, "
                    + "`description` TEXT, "
                    + "`rating` REAL NOT NULL, "
                    + "`latitude` REAL NOT NULL, "
                    + "`longitude` REAL NOT NULL, "
                    + "`isFeatured` INTEGER NOT NULL, "
                    + "`lastSynced` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("INSERT INTO `places_new` "
                    + "(`id`, `name`, `category`, `city`, `district`, `description`, "
                    + "`rating`, `latitude`, `longitude`, `isFeatured`, `lastSynced`) "
                    + "SELECT `id`, `name`, `category`, `city`, `district`, `description`, "
                    + "`rating`, `latitude`, `longitude`, `isFeatured`, `lastSynced` "
                    + "FROM `places`");
            database.execSQL("DROP TABLE `places`");
            database.execSQL("ALTER TABLE `places_new` RENAME TO `places`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_city` ON `places` (`city`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_category` ON `places` (`category`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_lastSynced` ON `places` (`lastSynced`)");
        }
    };

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                     AppDatabase.class,
                                     DATABASE_NAME
                             )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }

    private static boolean hasColumn(@NonNull SupportSQLiteDatabase database,
                                     @NonNull String table,
                                     @NonNull String column) {
        try (Cursor cursor = database.query("PRAGMA table_info(`" + table + "`)")) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && column.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }
}
