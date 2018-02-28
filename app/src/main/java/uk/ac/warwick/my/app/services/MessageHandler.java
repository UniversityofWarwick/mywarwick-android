package uk.ac.warwick.my.app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_MAX;

public class MessageHandler extends FirebaseMessagingService {

    public MessageHandler() {
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String priority = message.getData().get("priority");

        if (priority != null && priority.equals("high")) {

            RemoteMessage.Notification notification = message.getNotification();

            Intent intent = new Intent(this, MainActivity.class);

            try {
                Map<String, String> messageData = message.getData();
                String id = messageData.get("notification_id");
                // we'd want 'id' to throw here if null. MW app should always pass id in JSON
                if (id == null) {
                    throw new NullPointerException("Unable to build notification when notification_id is null in message: " + message.toString());
                }

                String channel = messageData.get("channel");

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                        .setPriority(PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_warwick_notification)
                        .setContentTitle(notification.getTitle())
                        .setContentText(notification.getBody())
                        .setColor(this.getResources().getColor(R.color.colorAccent))
                        .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE)
                        .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel != null) {
                    NotificationChannel notificationChannel =
                            new NotificationChannel(channel, channel, NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(notificationChannel);
                }

                notificationManager.notify(id, 0, builder.build());
            } catch (NullPointerException e) {
                FirebaseCrash.report(e);
            }
        }
    }

}
