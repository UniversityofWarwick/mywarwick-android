package uk.ac.warwick.my.app.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import uk.ac.warwick.my.app.services.DownloadScheduler;
import uk.ac.warwick.my.app.services.EventNotificationScheduler;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "Triggering boot actions");
            new DownloadScheduler(context).scheduleRepeatingDownload();
            new EventNotificationScheduler(context).scheduleNextNotification();
        } else {
            Log.w(TAG, "Ignoring intent with action " + intent.getAction());
        }
    }
}
