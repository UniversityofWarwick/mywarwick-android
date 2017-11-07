package uk.ac.warwick.my.app.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.warwick.my.app.services.DownloadScheduler;
import uk.ac.warwick.my.app.services.EventNotificationScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new DownloadScheduler(context).scheduleRepeatingDownload();
        new EventNotificationScheduler(context).scheduleNextNotification();
    }
}
