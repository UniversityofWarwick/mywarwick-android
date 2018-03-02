package uk.ac.warwick.my.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.PRIORITY_MAX;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.channelExists;

public class MessageHandler extends FirebaseMessagingService {

    private final static boolean apiLevelSupportsNotificationChannels = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

    public MessageHandler() {
    }

    private void buildAndSend(NotificationCompat.Builder builder, int priority, String title, String body, String id) throws NullPointerException {
        ((NotificationManager) builder.mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, 0,
                builder
                        .setPriority(priority)
                        .setSmallIcon(R.drawable.ic_warwick_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setColor(this.getResources().getColor(R.color.colorAccent))
                        .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE)
                        .setContentIntent(PendingIntent.getActivity(builder.mContext, 0, new Intent(builder.mContext, MainActivity.class), 0))
                        .build()
        );
    }

    private int getPriorityCode(String priorityStr) {
        if (priorityStr != null && priorityStr.equals("high")) {
            return apiLevelSupportsNotificationChannels ? IMPORTANCE_HIGH : PRIORITY_MAX;
        }
        return apiLevelSupportsNotificationChannels ? IMPORTANCE_DEFAULT : PRIORITY_DEFAULT;
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> messageData = message.getData();

        String channelId = messageData.get("android_channel_id");

        try {
            String id = messageData.get("id");
            // we'd want 'id' to throw here if null. MW app should always pass id in JSON
            if (id == null) {
                throw new NullPointerException("Unable to build notification when notification_id is null in message: " + message.toString());
            }
            String title = messageData.get("title");
            String body = messageData.get("body");
            int priority = getPriorityCode(messageData.get("priority"));

            if (apiLevelSupportsNotificationChannels && channelExists(channelId)) {
                buildAndSend(new NotificationCompat.Builder(this, channelId), priority, title, body, id);
            } else {
                buildAndSend(new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id)), priority, title, body, id);
            }

        } catch (NullPointerException e) {
            FirebaseCrash.report(e);
        }
    }
}
