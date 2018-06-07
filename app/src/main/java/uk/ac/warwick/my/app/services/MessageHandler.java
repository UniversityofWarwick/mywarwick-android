package uk.ac.warwick.my.app.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.system.ScheduledDownloadReceiver;
import uk.ac.warwick.my.app.system.ScheduledNotificationReceiver;

import static android.app.Notification.*;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.channelExists;

public class MessageHandler extends FirebaseMessagingService {
    private static final String TAG = MessageHandler.class.getName();
    private DoNotDisturbService doNotDisturbService;

    public MessageHandler() {
    }

    @Override
    public void onCreate() {
        this.doNotDisturbService = new DoNotDisturbService(new MyWarwickPreferences(getApplicationContext()));
    }

    private void handleSend(Context context, NotificationCompat.Builder builder, int priority, String title, String body, String id) {
        Date doNotDisturbEnd = doNotDisturbService.getDoNotDisturbEnd(Calendar.getInstance());
        if (doNotDisturbEnd != null) {
            Log.d(TAG, "Rescheduling notification for " + DateFormat.getDateTimeInstance().format(doNotDisturbEnd));

            Intent intent = new Intent(context, ScheduledNotificationReceiver.class);
            intent.putExtra(ScheduledNotificationReceiver.NOTIFICATION, build(builder, priority, title, body));

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, doNotDisturbEnd.getTime(), pendingIntent);
            } catch (NullPointerException e) {
                Crashlytics.logException(e);
            }
        } else {
            NotificationManager notificationManager = getNotificationManager(context);
            if (notificationManager != null) {
                notificationManager.notify(id, 0, build(builder, priority, title, body));

            }
        }
    }

    private Notification build(NotificationCompat.Builder builder, int priority, String title, String body) throws NullPointerException {
        return builder
                .setPriority(priority)
                .setSmallIcon(R.drawable.ic_warwick_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(this.getResources().getColor(R.color.colorAccent))
                .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE | DEFAULT_SOUND)
                .setStyle(new NotificationCompat.BigTextStyle()) // allow multiline body
                .setContentIntent(PendingIntent.getActivity(builder.mContext, 0, new Intent(builder.mContext, MainActivity.class), 0))
                .build();
    }

    /**
     * Sets priority which is only used for pre-Oreo devices.
     * On Oreo+ channels completely replace this.
     */
    private int getPriorityCode(String priorityStr) {
        if (priorityStr != null && priorityStr.equals("high")) {
            return PRIORITY_MAX;
        }
        return PRIORITY_DEFAULT;
    }

    @Nullable
    private NotificationManager getNotificationManager(@NonNull Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> messageData = message.getData();

        String channelId = messageData.get("android_channel_id");

        // channelExists() func is available only to Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelExists(channelId)) {
                channelId = getString(R.string.default_notification_channel_id);
            }
        }

        try {
            String id = messageData.get("id");
            // we'd want 'id' to throw here if null. MW app should always pass id in JSON
            if (id == null) {
                throw new NullPointerException("Unable to build notification when notification_id is null in message: " + message.toString());
            }
            String title = messageData.get("title");
            String body = messageData.get("body");
            int priority = getPriorityCode(messageData.get("priority"));

            // Set a channel, which is ignored for anything before Oreo
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

            handleSend(this, builder, priority, title, body, id);

        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }
}
