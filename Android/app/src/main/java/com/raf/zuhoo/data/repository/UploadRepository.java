package com.raf.zuhoo.data.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;

public class UploadRepository {

    /** Matches the backend's spring.servlet.multipart max-file-size. */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final Context appContext;
    private final ApiService apiService;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public UploadRepository(Context context) {
        appContext = context.getApplicationContext();
        apiService = ApiClient.getClient(appContext);
    }

    /** Thrown when the chosen file is unreadable or larger than the server will accept. */
    public static class UploadException extends Exception {
        UploadException(String message) {
            super(message);
        }
    }

    /** Reported on the main thread when the file can't be read or is too big to send. */
    public interface ReadErrorListener {
        void onReadError(String message);
    }

    /**
     * Reads the picked content and posts it as multipart.
     *
     * Read into memory because a content:// Uri from the document picker isn't a File — there's
     * no path to hand OkHttp — and the 10 MB server cap bounds how much that can be. The read
     * happens off the main thread: a 10 MB copy on the UI thread is an ANR waiting to happen.
     */
    public void upload(Uri uri, ReadErrorListener readErrorListener,
                       Callback<Map<String, String>> callback) {

        io.execute(() -> {

            ContentResolver resolver = appContext.getContentResolver();

            String fileName = displayName(resolver, uri);
            String mimeType = resolver.getType(uri);

            final byte[] bytes;

            try {
                bytes = read(resolver, uri);
            } catch (UploadException e) {
                main.post(() -> readErrorListener.onReadError(e.getMessage()));
                return;
            }

            RequestBody body = RequestBody.create(bytes,
                    MediaType.parse(mimeType != null ? mimeType : "application/octet-stream"));

            // The part name must be "file" — that's the @RequestParam the controller binds.
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, body);

            // enqueue() is safe from any thread and delivers the callback on the main thread.
            apiService.uploadFile(part).enqueue(callback);
        });
    }

    private byte[] read(ContentResolver resolver, Uri uri) throws UploadException {

        try (InputStream input = resolver.openInputStream(uri)) {

            if (input == null) {
                throw new UploadException("Could not open the selected file");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;

            while ((read = input.read(buffer)) != -1) {

                total += read;

                // Stop as soon as the cap is passed rather than buffering a huge file only to
                // have the server reject it.
                if (total > MAX_FILE_BYTES) {
                    throw new UploadException("File is larger than 10 MB");
                }

                output.write(buffer, 0, read);
            }

            return output.toByteArray();

        } catch (IOException e) {
            throw new UploadException("Could not read the selected file");
        }
    }

    private String displayName(ContentResolver resolver, Uri uri) {

        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
            }

        } catch (Exception ignored) {
            // Fall through to the generic name below — a missing display name is not worth
            // failing an upload over.
        }

        return "attachment";
    }
}
