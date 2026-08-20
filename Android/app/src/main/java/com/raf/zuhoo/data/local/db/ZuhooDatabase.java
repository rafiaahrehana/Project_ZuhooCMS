package com.raf.zuhoo.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CachedItem.class, CacheMeta.class}, version = 1, exportSchema = false)
public abstract class ZuhooDatabase extends RoomDatabase {

    public abstract CacheDao cacheDao();

    public static ZuhooDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                        ZuhooDatabase.class, "zuhoo-cache.db")
                // Everything in here is a disposable copy of server state, so throwing it away on
                // a schema change is correct — there is nothing to migrate and nothing to lose.
                .fallbackToDestructiveMigration()
                .build();
    }
}
