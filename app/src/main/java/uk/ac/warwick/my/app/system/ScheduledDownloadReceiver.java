package uk.ac.warwick.my.app.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.warwick.my.app.services.EventFetcher;

public class ScheduledDownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        new Thread(() -> new EventFetcher(context).updateEvents()).start();
    }
}
