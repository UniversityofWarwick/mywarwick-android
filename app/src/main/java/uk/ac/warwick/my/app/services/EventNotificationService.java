package uk.ac.warwick.my.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.data.Event;
import uk.ac.warwick.my.app.data.EventDao;
import uk.ac.warwick.my.app.utils.CustomLogger;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_MAX;
import static androidx.core.app.NotificationCompat.CATEGORY_EVENT;
import static uk.ac.warwick.my.app.Global.TAG;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.TIMETABLE_EVENTS_CHANNEL_ID;

public class EventNotificationService {
    public static final String NOTIFICATION_ID = "uk.ac.warwick.my.app.notification_id";

    private final Context context;

    private final MyWarwickPreferences preferences;

    public EventNotificationService(Context context) {
        this.context = context;
        this.preferences = new MyWarwickPreferences(context);
    }

    public EventNotificationService(Context context, MyWarwickPreferences prefs) {
        this.context = context;
        this.preferences = prefs;
    }

    public void notify(String serverId) {
        try (EventDao eventDao = new EventDao(context)) {
            Event event = eventDao.findByServerId(serverId);

            if (event == null) {
                Log.w(TAG, "Event " + serverId + " not found in database");
                CustomLogger.log(context, "Event " + serverId + " not found in database");
                return;
            }

            for (Event ev : eventDao.findAllByStart(event.getStart())) {
                notify(ev);
            }
        }
    }
    private void notify(Event event) {
        Integer id = event.getId();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_ID, id);

        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(context, TIMETABLE_EVENTS_CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }

         builder
             .setPriority(PRIORITY_MAX)
             .setSmallIcon(R.drawable.ic_warwick_notification)
             .setContentTitle(getNotificationTitle(event))
             .setContentText(getNotificationText(event))
             .setColor(context.getResources().getColor(R.color.colorAccent))
             .setCategory(CATEGORY_EVENT)
             .setWhen(event.getStart().getTime())
             .setShowWhen(false)
             .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE)
             .setContentIntent(PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE));

        if (preferences.isTimetableNotificationsSoundEnabled()) {
            builder.setSound(Uri.parse(String.format("android.resource://%s/%s", context.getPackageName(), R.raw.timetable_alarm)));
        }

        Notification notification = builder.build();
        CustomLogger.log(context, String.format("Sending notification %d for %s at %s", id, getNotificationTitle(event), event.getStart().getTime()));
        getNotificationManager().notify(id, notification);
        CustomLogger.log(context, String.format("Dispatched notification %d for %s at %s", id, getNotificationTitle(event), event.getStart().getTime()));
    }

    private NotificationManager getNotificationManager() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NotificationChannel nc = nm.getNotificationChannel(TIMETABLE_EVENTS_CHANNEL_ID);
            CustomLogger.log(context, String.format(
                    "areNotificationsEnabled: %s, areNotificationsPaused: %s",
                    nm.areNotificationsEnabled(),
                    nm.areNotificationsPaused()
            ));
            if (nc != null) {
                CustomLogger.log(context, String.format(
                        "getImportance: %s",
                        nc.getImportance()
                ));
            }
        }
        return nm;
    }

    private String getNotificationTitle(Event e) {
        if (e.getParentShortName() == null || e.getParentShortName().isEmpty()) {
            return e.getType();
        }

        return String.format("%s %s", e.getParentShortName(), e.getType());
    }

    String getNotificationText(Event e) {
        String locationPart = e.getLocation() == null ? "" : e.getLocation() + ", ";
        return String.format("%s%s", locationPart, formatTime(e));
    }

    private String formatTime(Event e) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        return String.format("%s – %s", timeFormat.format(e.getStart()), timeFormat.format(e.getEnd()));
    }
}
