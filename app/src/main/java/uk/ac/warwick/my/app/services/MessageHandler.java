package uk.ac.warwick.my.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.PRIORITY_MAX;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.channelExists;

public class MessageHandler extends FirebaseMessagingService {

    public MessageHandler() {
    }

    private void buildAndSend(Context context, NotificationCompat.Builder builder, int priority, String title, String body, String id) throws NullPointerException {
        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager != null) {
            notificationManager.notify(id, 0,
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

            buildAndSend(this, builder, priority, title, body, id);

        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }
}
