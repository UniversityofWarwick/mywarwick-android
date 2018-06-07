package uk.ac.warwick.my.app.system;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;

public class ScheduledNotificationReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION = "notification";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        try {
            manager.notify(0, notification);
        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }
}
