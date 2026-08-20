package com.raf.zuhoo.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class CacheDao {

    @Query("SELECT * FROM cached_items WHERE collection = :collection ORDER BY position ASC")
    public abstract List<CachedItem> itemsFor(String collection);

    @Query("SELECT updatedAt FROM cache_meta WHERE collection = :collection")
    public abstract Long updatedAt(String collection);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertItems(List<CachedItem> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertMeta(CacheMeta meta);

    @Query("DELETE FROM cached_items WHERE collection = :collection")
    public abstract void clear(String collection);

    @Query("DELETE FROM cached_items")
    public abstract void clearAllItems();

    @Query("DELETE FROM cache_meta")
    public abstract void clearAllMeta();

    // Replace rather than merge: the server's page is the whole truth for this list, so a row
    // that has disappeared upstream must disappear here too.
    @Transaction
    public void replaceAll(String collection, List<CachedItem> items, long updatedAt) {
        clear(collection);
        insertItems(items);
        insertMeta(new CacheMeta(collection, updatedAt));
    }

    // Called on logout — one user's cached data must never be visible to the next account
    // signed in on the same device.
    @Transaction
    public void wipe() {
        clearAllItems();
        clearAllMeta();
    }
}
