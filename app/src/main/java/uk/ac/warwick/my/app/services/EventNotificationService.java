package uk.ac.warwick.my.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.data.Event;
import uk.ac.warwick.my.app.data.EventDao;

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

    public void notify(Event event) {
        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_warwick_notification)
                .setContentTitle(getNotificationTitle(event))
                .setContentText(getNotificationText(event))
                .build();

        getNotificationManager().notify(event.getId(), notification);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private String formatTime(Event e) {
        return SimpleDateFormat.getTimeInstance().format(e.getStart());
    }

    private String getNotificationTitle(Event e) {
        if (e.getType() != null) {
            if (e.getParentShortName() != null) {
                return String.format("%s %s at %s", e.getParentShortName(), e.getType().toLowerCase(), formatTime(e));
            }

            return String.format("%s at %s", e.getType(), formatTime(e));
        }

        return String.format("Event at %s", formatTime(e));
    }

    private String getNotificationText(Event e) {
        return "A timetabled event is starting soon.";
    }
}
