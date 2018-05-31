package uk.ac.warwick.my.app.services;

import android.app.Notification;
import android.content.Context;

import java.util.Date;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

public class NotificationScheduler {
    private final Context context;
    private final MyWarwickPreferences preferences;

    public NotificationScheduler(final Context context, final MyWarwickPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    public void schedule(Notification notification, Date date) {

    }
}
