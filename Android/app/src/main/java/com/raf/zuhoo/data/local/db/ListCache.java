package com.raf.zuhoo.data.local.db;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Read-only cache of the last successful response for a list, so a screen can render immediately
// on open and still show something useful with no connection.
//
// Deliberately NOT an offline-write layer: nothing is ever queued or synced back. Every mutation
// in the app goes straight to the API, and this only ever holds a copy of what the server last
// said.
public class ListCache {

    public static final String SERVICE_REQUESTS = "service_requests";
    public static final String INVOICES = "invoices";
    public static final String NOTIFICATIONS = "notifications";
    public static final String LEAVE_REQUESTS = "leave_requests";
    public static final String LEAVE_APPROVALS = "leave_approvals";
    public static final String EXPENSES = "expenses";
    public static final String EXPENSE_APPROVALS = "expense_approvals";

    public interface Callback<T> {
        void onResult(List<T> items, Long updatedAt);
    }

    private final CacheDao dao;
    private final Gson gson = new Gson();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ListCache(ZuhooDatabase database) {
        dao = database.cacheDao();
    }

    public <T> void read(String collection, Class<T> type, Callback<T> callback) {

        io.execute(() -> {

            List<CachedItem> rows = dao.itemsFor(collection);
            Long updatedAt = dao.updatedAt(collection);

            List<T> items = new ArrayList<>(rows.size());
            for (CachedItem row : rows) {
                items.add(gson.fromJson(row.json, type));
            }

            main.post(() -> callback.onResult(items, updatedAt));
        });
    }

    // ids must line up with items — the id is the row key, so a list whose items have no id
    // simply isn't cached rather than being stored under a fabricated one.
    public <T> void write(String collection, List<T> items, List<Long> ids, long updatedAt) {

        List<CachedItem> rows = new ArrayList<>(items.size());

        for (int i = 0; i < items.size(); i++) {
            Long id = ids.get(i);
            if (id == null) {
                continue;
            }
            rows.add(new CachedItem(collection, id, i, gson.toJson(items.get(i))));
        }

        io.execute(() -> dao.replaceAll(collection, rows, updatedAt));
    }

    public void wipe() {
        io.execute(dao::wipe);
    }
}
