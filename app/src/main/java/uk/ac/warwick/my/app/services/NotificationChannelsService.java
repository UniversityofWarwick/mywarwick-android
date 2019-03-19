package uk.ac.warwick.my.app.services;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O)
public class NotificationChannelsService {

    final static String TIMETABLE_EVENTS_CHANNEL_ID = "timetable_events";
    public final static String TWO_STEP_CODES_CHANNEL_ID = "two_step_codes";

    private final static NotificationChannel[] channels = new NotificationChannel[]{
            new NotificationChannel(TIMETABLE_EVENTS_CHANNEL_ID, "Timetable events", NotificationManager.IMPORTANCE_DEFAULT),
            new NotificationChannel(TWO_STEP_CODES_CHANNEL_ID, "Two-step codes", NotificationManager.IMPORTANCE_HIGH),
            new NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            new NotificationChannel("urgent_alerts", "Urgent alerts", NotificationManager.IMPORTANCE_HIGH)
    };

    static boolean channelExists(String channelId) {
        if (channelId == null) return false;
        for (NotificationChannel channel : channels) {
            if (channel.getId().equals(channelId)) {
                return true;
            }
        }
        return false;
    }

    public static void buildNotificationChannels(NotificationManager manager) {
        for (NotificationChannel channel : channels) {
            manager.createNotificationChannel(channel);
        }
    }
}
