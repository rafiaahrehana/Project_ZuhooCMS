package com.raf.zuhoo.ui.common;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.repository.UploadRepository;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pick-then-upload for a single attachment, shared by the service-request and support-ticket
 * composers.
 *
 * The API is two-step by design: upload to /api/upload, get a URL back, then send that URL as
 * the message's attachmentUrl. So the file goes up as soon as it's picked, and the composer only
 * ever deals in a URL string.
 */
public class AttachmentPicker {

    private final AppCompatActivity activity;
    private final TextView chip;
    private final UploadRepository uploadRepository;
    private final ActivityResultLauncher<String[]> launcher;

    private String url;
    private String fileName;

    /** Must be constructed during onCreate — registerForActivityResult requires it. */
    public AttachmentPicker(AppCompatActivity activity, TextView chip) {

        this.activity = activity;
        this.chip = chip;
        this.uploadRepository = new UploadRepository(activity);

        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onPicked);

        chip.setOnClickListener(v -> clear());
    }

    public void pick() {
        // Any type — the backend accepts arbitrary files on /api/upload (the image-only rules
        // apply to the separate avatar endpoint).
        launcher.launch(new String[]{"*/*"});
    }

    public String url() {
        return url;
    }

    public String fileName() {
        return fileName;
    }

    public void clear() {
        url = null;
        fileName = null;
        chip.setVisibility(View.GONE);
    }

    private void onPicked(Uri uri) {

        if (uri == null) {
            return;
        }

        chip.setVisibility(View.VISIBLE);
        chip.setText(R.string.attachment_uploading);

        uploadRepository.upload(uri,
                message -> {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    clear();
                },
                new Callback<Map<String, String>>() {

                    @Override
                    public void onResponse(Call<Map<String, String>> call,
                                           Response<Map<String, String>> response) {

                        if (!response.isSuccessful() || response.body() == null
                                || response.body().get("fileUrl") == null) {
                            Toast.makeText(activity, R.string.error_attachment_failed,
                                    Toast.LENGTH_LONG).show();
                            clear();
                            return;
                        }

                        url = response.body().get("fileUrl");
                        fileName = response.body().get("fileName");

                        chip.setText(activity.getString(R.string.attachment_attached,
                                fileName != null ? fileName : ""));
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        Toast.makeText(activity, R.string.error_attachment_failed,
                                Toast.LENGTH_LONG).show();
                        clear();
                    }
                });
    }
}
