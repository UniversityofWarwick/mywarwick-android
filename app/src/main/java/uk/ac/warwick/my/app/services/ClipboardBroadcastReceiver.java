package uk.ac.warwick.my.app.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import uk.ac.warwick.my.app.R;

public class ClipboardBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("2SA Code", intent.getStringExtra("code"));
        String notificationTag = intent.getStringExtra("id");
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, context.getString(R.string.tsa_copied), Toast.LENGTH_SHORT).show();
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationTag, 0);
        }
    }
}
