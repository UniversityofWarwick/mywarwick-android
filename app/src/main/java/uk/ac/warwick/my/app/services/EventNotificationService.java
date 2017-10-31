package uk.ac.warwick.my.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.data.Event;
import uk.ac.warwick.my.app.data.EventDao;

import static android.app.Notification.PRIORITY_MAX;
import static uk.ac.warwick.my.app.Global.TAG;

public class EventNotificationService {
    private final Context context;

    public EventNotificationService(Context context) {
        this.context = context;
    }

    public void notify(String serverId) {
        Event event;
        try (EventDao eventDao = new EventDao(context)) {
            event = eventDao.findByServerId(serverId);
        }

        if (event == null) {
            Log.w(TAG, "Event " + serverId + " not found in database");
            return;
        }

        notify(event);
    }

    private void notify(Event event) {
        Notification notification = new NotificationCompat.Builder(context)
                .setPriority(PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_warwick_notification)
                .setContentTitle(getNotificationTitle(event))
                .setContentText(getNotificationText(event))
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .build();

        getNotificationManager().notify(1, notification);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private String getNotificationTitle(Event e) {
        if (e.getParentShortName() == null || e.getParentShortName().isEmpty()) {
            return e.getType();
        }

        return String.format("%s %s", e.getParentShortName(), e.getType());
    }

    private String getNotificationText(Event e) {
        return String.format("%s, %s", e.getLocation(), formatTime(e));
    }

    private String formatTime(Event e) {
        DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

        return String.format("%s – %s", timeFormat.format(e.getStart()), timeFormat.format(e.getEnd()));
    }
}
