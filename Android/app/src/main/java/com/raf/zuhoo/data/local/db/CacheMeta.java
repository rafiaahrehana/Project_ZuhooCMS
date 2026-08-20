package com.raf.zuhoo.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// When each cached list was last successfully refreshed, so the UI can say "showing data from
// 10 minutes ago" instead of silently presenting stale rows as current.
@Entity(tableName = "cache_meta")
public class CacheMeta {

    @PrimaryKey
    @NonNull
    public String collection = "";

    public long updatedAt;

    public CacheMeta() {
    }

    // See CachedItem — @Ignore keeps Room on the no-arg constructor.
    @Ignore
    public CacheMeta(@NonNull String collection, long updatedAt) {
        this.collection = collection;
        this.updatedAt = updatedAt;
    }
}
