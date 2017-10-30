package uk.ac.warwick.my.app.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import uk.ac.warwick.my.app.services.EventNotificationScheduler;
import uk.ac.warwick.my.app.services.EventNotificationService;

import static uk.ac.warwick.my.app.Global.TAG;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EVENT_SERVER_ID = "uk.ac.warwick.my.app.event_server_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String serverId = intent.getStringExtra(EVENT_SERVER_ID);

        if (serverId == null) {
            Log.w(TAG, "AlarmReceiver invoked with missing " + EVENT_SERVER_ID + " extra");
        } else {
            new EventNotificationService(context).notify(serverId);
        }

        // In any case, schedule the next notification now
        new EventNotificationScheduler(context).scheduleNextNotification();
    }
}
