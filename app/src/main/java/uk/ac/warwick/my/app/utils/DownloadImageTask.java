package uk.ac.warwick.my.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static uk.ac.warwick.my.app.Global.TAG;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private final View containerView;
    private final ImageView imageView;
    private final Context context;

    public DownloadImageTask(ImageView imageView, View containerView) {
        this.imageView = imageView;
        this.containerView = containerView;

        context = containerView.getContext();
    }

    protected Bitmap doInBackground(String... urls) {
        String imageUrl = urls[0];
        Bitmap bitmap = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            String digest = Hashing.md5().hashString(imageUrl, Charsets.UTF_8).toString();
            File file = new File(context.getCacheDir(), digest);

            if (!file.canRead()) {
                in = new URL(imageUrl).openStream();
                out = new FileOutputStream(file);
                ByteStreams.copy(in, out);
            }

            bitmap = BitmapFactory.decodeFile(file.getPath());
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving user photo", e);
        } finally {
            Closeables.closeQuietly(in);
            try {
                Closeables.close(out, true);
            } catch (IOException ignored) {
            }
        }

        return bitmap;
    }

    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
        containerView.setVisibility(View.VISIBLE);
    }
}