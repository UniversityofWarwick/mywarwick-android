package uk.ac.warwick.my.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

import static android.app.Notification.*;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.TWO_STEP_CODES_CHANNEL_ID;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.channelExists;

public class MessageHandler extends FirebaseMessagingService {

    public static final String COPY_CODE_ACTION = "uk.ac.warwick.ACTION_COPY_2SA_CODE";
    final Pattern twoStepCodePattern = Pattern.compile("[0-9]{6}");

    public MessageHandler() {
    }


    private void buildAndSend(Context context, NotificationCompat.Builder builder, int priority, String title, String body, String id, String channel) throws NullPointerException {
        NotificationManager notificationManager = getNotificationManager(context);
        if (notificationManager != null) {
            if (body == null || body.isEmpty()) {
                body = title;
                title = null;
            }
            NotificationCompat.Builder partialBuild = builder
                    .setPriority(priority)
                    .setSmallIcon(R.drawable.ic_warwick_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setColor(this.getResources().getColor(R.color.colorAccent))
                    .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE | DEFAULT_SOUND)
                    .setStyle(new NotificationCompat.BigTextStyle()) // allow multiline body
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class).putExtra("from", "notification"), FLAG_CANCEL_CURRENT));

            if (channel.equals(TWO_STEP_CODES_CHANNEL_ID)) {
                partialBuild = this.enrichTwoStepCodeNotification(id, context, title == null ? body : title, builder);
            }

            notificationManager.notify(id, 0,
                partialBuild
                    .build()
            );
        }
    }

    private NotificationCompat.Builder enrichTwoStepCodeNotification(String id, Context context, String notificationText, NotificationCompat.Builder builder) {
        Matcher matcher = twoStepCodePattern.matcher(notificationText);
        if (notificationText != null && matcher.find()) {
            final String code = matcher.group(0);
            Intent copy = new Intent(context, ClipboardBroadcastReceiver.class);
            copy.putExtra("code", code);
            copy.putExtra("id", id);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, copy, PendingIntent.FLAG_UPDATE_CURRENT);
            builder = builder.addAction(R.drawable.ic_content_copy_black_24dp, "Copy", pendingIntent);
        }
        return builder;
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

            buildAndSend(this, builder, priority, title, body, id, channelId);

        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }

}
