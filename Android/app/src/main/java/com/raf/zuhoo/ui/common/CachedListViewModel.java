package com.raf.zuhoo.ui.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.PageResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Shared behaviour for every paged list screen: show the cached copy immediately, then refresh
// from the network. If the refresh fails the cached rows stay on screen with a "last updated"
// stamp rather than the screen going blank and throwing an error at the user.
//
// Read-only by design — nothing is ever queued for later upload. Writes go straight to the API.
public abstract class CachedListViewModel<T> extends AndroidViewModel {

    private final ListCache cache;
    private final String collection;
    private final Class<T> type;

    private final MutableLiveData<List<T>> items = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Long> lastUpdated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showingCached = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> error = new MutableLiveData<>();

    private boolean loadedOnce;

    protected CachedListViewModel(@NonNull Application application, String collection, Class<T> type) {
        super(application);
        this.cache = ZuhooApplication.graph().listCache();
        this.collection = collection;
        this.type = type;
    }

    // Fire the network call for this list.
    protected abstract void fetch(Callback<PageResponse<T>> callback);

    // Row key for the cache. An item without one simply isn't cached.
    protected abstract Long idOf(T item);

    // Shown only when there's nothing cached to fall back on.
    protected abstract int loadErrorRes();

    public LiveData<List<T>> items() {
        return items;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<Long> lastUpdated() {
        return lastUpdated;
    }

    public LiveData<Boolean> showingCached() {
        return showingCached;
    }

    public LiveData<Event<Integer>> error() {
        return error;
    }

    public void start() {

        if (!loadedOnce) {
            loadedOnce = true;
            readCache();
        }

        refresh();
    }

    private void readCache() {

        cache.read(collection, type, (cached, updatedAt) -> {
            // Don't overwrite a network result that beat the disk read back.
            if (!cached.isEmpty() && items.getValue() == null) {
                items.setValue(cached);
                lastUpdated.setValue(updatedAt);
                showingCached.setValue(true);
            }
        });
    }

    public void refresh() {

        loading.setValue(true);

        fetch(new Callback<PageResponse<T>>() {

            @Override
            public void onResponse(Call<PageResponse<T>> call, Response<PageResponse<T>> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    onRefreshFailed();
                    return;
                }

                List<T> fresh = response.body().getContent();

                items.setValue(fresh);
                showingCached.setValue(false);
                lastUpdated.setValue(System.currentTimeMillis());

                List<Long> ids = new ArrayList<>(fresh.size());
                for (T item : fresh) {
                    ids.add(idOf(item));
                }

                cache.write(collection, fresh, ids, System.currentTimeMillis());
            }

            @Override
            public void onFailure(Call<PageResponse<T>> call, Throwable t) {
                loading.setValue(false);
                onRefreshFailed();
            }
        });
    }

    private void onRefreshFailed() {

        // With cached rows on screen the failure is already communicated by the "last updated"
        // stamp; an error toast on top of visible content is just noise.
        if (items.getValue() != null && !items.getValue().isEmpty()) {
            showingCached.setValue(true);
            return;
        }

        error.setValue(new Event<>(loadErrorRes()));
    }
}
