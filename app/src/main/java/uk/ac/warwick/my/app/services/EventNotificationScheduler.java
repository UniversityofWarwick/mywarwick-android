package uk.ac.warwick.my.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.data.Event;
import uk.ac.warwick.my.app.data.EventDao;
import uk.ac.warwick.my.app.system.AlarmReceiver;
import uk.ac.warwick.my.app.utils.CustomLogger;

import static uk.ac.warwick.my.app.Global.TAG;

public class EventNotificationScheduler {
    private final Context context;
    private final MyWarwickPreferences preferences;

    public EventNotificationScheduler(Context context) {
        this.context = context;
        preferences = new MyWarwickPreferences(context);
    }

    public void scheduleNextNotification() {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);

        // Cancel the next scheduled notification
        getAlarmManager().cancel(PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        if (!preferences.isTimetableNotificationsEnabled()) {
            // Notifications aren't enabled, so stop after cancelling any that were queued up
            CustomLogger.log(context, "Notifications aren't enabled, so stop after cancelling any that were queued up");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !getAlarmManager().canScheduleExactAlarms()) {
            // User has blocked the alarm permission so no point creating new ones
            CustomLogger.log(context, "User has blocked the alarm permission so no point creating new ones");
            return;
        }

        Event event = getNextNotificationEvent();

        if (event == null) {
            Log.i(TAG, "scheduleNextNotification called, but there are no future events");
            CustomLogger.log(context, "scheduleNextNotification called, but there are no future events");
            return;
        }

        alarmIntent.putExtra(AlarmReceiver.EVENT_SERVER_ID, event.getServerId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Date alarmDate = getNotificationDate(event);

        Log.i(TAG, "Scheduling a notification for event '" + event.getTitle() + "' at " + alarmDate);
        CustomLogger.log(context, "Scheduling a notification for event '" + event.getTitle() + "' at " + alarmDate);

        getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, alarmDate.getTime(), pendingIntent);
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private Event getNextNotificationEvent() {
        try (EventDao eventDao = new EventDao(context)) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(new Date());
            instance.add(Calendar.MINUTE, preferences.getTimetableNotificationTiming());
            Date date = instance.getTime();

            return eventDao.getFirstEventAfterDate(date);
        }
    }

    private Date getNotificationDate(Event event) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(event.getStart());
        instance.add(Calendar.MINUTE, -preferences.getTimetableNotificationTiming());
        return instance.getTime();
    }
}
