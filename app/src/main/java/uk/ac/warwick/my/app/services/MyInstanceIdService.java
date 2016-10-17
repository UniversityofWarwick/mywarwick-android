package uk.ac.warwick.my.app.services;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;

import static uk.ac.warwick.my.app.utils.PushNotifications.TOKEN_REFRESH;

public class MyInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent();
        intent.setAction(TOKEN_REFRESH);

        sendBroadcast(intent);
    }

}
