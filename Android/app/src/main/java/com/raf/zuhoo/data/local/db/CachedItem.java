package com.raf.zuhoo.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

// One row per cached list item, stored as the raw JSON the API returned.
//
// A single generic table rather than one entity per DTO: the cache is only ever read back
// wholesale to render a list, never queried by field, so mirroring every DTO's columns (plus
// TypeConverters for nested objects like invoice line items) would be structure without a
// purpose. Storing the JSON also means a DTO gaining a field needs no migration.
@Entity(tableName = "cached_items", primaryKeys = {"collection", "itemId"})
public class CachedItem {

    // Which list this row belongs to — see CacheKeys.
    @NonNull
    public String collection = "";

    public long itemId;

    // Preserves the server's ordering, which the JSON itself doesn't carry.
    public int position;

    public String json;

    public CachedItem() {
    }

    // @Ignore so Room uses the no-arg constructor and doesn't warn about the ambiguity — this
    // one exists purely for building rows in code.
    @Ignore
    public CachedItem(@NonNull String collection, long itemId, int position, String json) {
        this.collection = collection;
        this.itemId = itemId;
        this.position = position;
        this.json = json;
    }
}
