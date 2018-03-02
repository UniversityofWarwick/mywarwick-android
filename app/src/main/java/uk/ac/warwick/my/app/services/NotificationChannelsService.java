package uk.ac.warwick.my.app.services;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;

@SuppressLint("NewAPI")
public class NotificationChannelsService {

    final static String TIMETABLE_EVENTS_CHANNEL_ID = "timetable_events";

    private final static NotificationChannel[] channels = new NotificationChannel[]{
            new NotificationChannel(TIMETABLE_EVENTS_CHANNEL_ID, "Timetable events", NotificationManager.IMPORTANCE_DEFAULT),
            new NotificationChannel("two_step_codes", "Two-step codes", NotificationManager.IMPORTANCE_HIGH),
            new NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_DEFAULT)
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
