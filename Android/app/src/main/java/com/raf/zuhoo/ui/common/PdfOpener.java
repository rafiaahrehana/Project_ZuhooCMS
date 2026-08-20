package com.raf.zuhoo.ui.common;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.raf.zuhoo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;

/**
 * Writes a downloaded PDF response to the app's cache and opens it in the device's PDF viewer.
 * Same steps InvoiceDetailActivity established first — pulled out here so a second screen
 * (payslips) doesn't have to re-copy the FileProvider dance.
 */
public final class PdfOpener {

    private PdfOpener() {
    }

    /** cacheFileName should be unique per document, e.g. "payslip-42.pdf". */
    public static void writeAndOpen(Activity activity, ResponseBody body, String cacheFileName) {

        try {
            File dir = new File(activity.getCacheDir(), "pdfs");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, cacheFileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(body.bytes());
            }

            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, R.string.error_pdf_no_viewer, Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
            Toast.makeText(activity, R.string.error_pdf_download_failed, Toast.LENGTH_LONG).show();
        }
    }
}
