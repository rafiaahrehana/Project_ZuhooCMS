package com.raf.zuhoo.ui.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.repository.UploadRepository;

import java.io.File;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Camera-then-upload for a single live selfie, used by attendance check-in/out.
 *
 * Deliberately camera-only (ActivityResultContracts.TakePicture), unlike AttachmentPicker's
 * document picker — a gallery pick would let someone attach an old photo instead of proving
 * they're here now. The captured file goes through the FileProvider "selfies" cache path
 * declared in file_paths.xml, then uploads immediately via the same two-step /api/upload flow
 * AttachmentPicker uses, so the composer only ever deals in a URL string.
 */
public class SelfieCapture {

    private final AppCompatActivity activity;
    private final ImageView preview;
    private final UploadRepository uploadRepository;
    private final ActivityResultLauncher<Uri> launcher;
    private final ActivityResultLauncher<String> cameraPermissionLauncher;

    private Uri pendingCaptureUri;
    private String url;

    public interface Listener {
        void onUploaded(String url);
        void onCleared();
    }

    private final Listener listener;

    /** Must be constructed during onCreate — registerForActivityResult requires it. */
    public SelfieCapture(AppCompatActivity activity, ImageView preview, Listener listener) {

        this.activity = activity;
        this.preview = preview;
        this.listener = listener;
        this.uploadRepository = new UploadRepository(activity);

        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(), this::onCaptured);

        // The system camera app enforces the caller's own CAMERA permission, not just the
        // FileProvider grant — launching TakePicture without it throws a SecurityException
        // rather than prompting, so this has to be requested first, same as LocationHelper does
        // for ACCESS_FINE_LOCATION.
        this.cameraPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(activity, R.string.error_camera_permission_required,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void capture() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {

        File dir = new File(activity.getCacheDir(), "selfies");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(activity, R.string.error_selfie_failed, Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(dir, "checkin_" + System.currentTimeMillis() + ".jpg");

        pendingCaptureUri = FileProvider.getUriForFile(
                activity, activity.getPackageName() + ".fileprovider", file);

        launcher.launch(pendingCaptureUri);
    }

    public String url() {
        return url;
    }

    public void clear() {
        url = null;
        preview.setImageDrawable(null);
        preview.setVisibility(android.view.View.GONE);
        listener.onCleared();
    }

    private void onCaptured(boolean success) {

        if (!success || pendingCaptureUri == null) {
            return;
        }

        preview.setImageURI(pendingCaptureUri);
        preview.setVisibility(android.view.View.VISIBLE);

        Uri uri = pendingCaptureUri;

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
                            Toast.makeText(activity, R.string.error_selfie_failed, Toast.LENGTH_LONG).show();
                            clear();
                            return;
                        }

                        url = response.body().get("fileUrl");
                        listener.onUploaded(url);
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        Toast.makeText(activity, R.string.error_selfie_failed, Toast.LENGTH_LONG).show();
                        clear();
                    }
                });
    }
}
