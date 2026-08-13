package com.arriva.touristguideapp.data.places;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {PlaceEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "arriva_places.db";
    private static volatile AppDatabase instance;

    public abstract PlaceDao placeDao();

    private static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
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
                    + "`imageRef` TEXT, "
                    + "`galleryImageRefs` TEXT, "
                    + "`lastSynced` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("INSERT INTO `places_new` "
                    + "(`id`, `name`, `category`, `city`, `district`, `description`, "
                    + "`rating`, `latitude`, `longitude`, `isFeatured`, `imageRef`, `galleryImageRefs`, `lastSynced`) "
                    + "SELECT `id`, `name`, `category`, `city`, `district`, `description`, "
                    + "`rating`, `latitude`, `longitude`, `isFeatured`, NULL, NULL, `lastSynced` "
                    + "FROM `places`");
            database.execSQL("DROP TABLE `places`");
            database.execSQL("ALTER TABLE `places_new` RENAME TO `places`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_city` ON `places` (`city`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_category` ON `places` (`category`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_places_lastSynced` ON `places` (`lastSynced`)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `places` ADD COLUMN `imageRef` TEXT");
            database.execSQL("ALTER TABLE `places` ADD COLUMN `galleryImageRefs` TEXT");
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
                            .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }

}
