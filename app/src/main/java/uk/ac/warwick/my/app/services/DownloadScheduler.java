package uk.ac.warwick.my.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.Date;

import uk.ac.warwick.my.app.system.ScheduledDownloadReceiver;

public class DownloadScheduler {
    private final Context context;

    public DownloadScheduler(Context context) {
        this.context = context;
    }

    public void scheduleRepeatingDownload() {
        Intent intent = new Intent(context, ScheduledDownloadReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 0);
        getAlarmManager().setInexactRepeating(0, getNextTriggerDate(new Date()).getTime(), AlarmManager.INTERVAL_DAY, broadcast);
    }

    private Date getNextTriggerDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 5);
        return calendar.getTime();
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
