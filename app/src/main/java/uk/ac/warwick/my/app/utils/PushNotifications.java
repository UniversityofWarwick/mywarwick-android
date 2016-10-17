package uk.ac.warwick.my.app.utils;

import com.google.firebase.iid.FirebaseInstanceId;

public class PushNotifications {

    public static final String TOKEN_REFRESH = "uk.ac.warwick.my.app.TOKEN_REFRESH";

    public static String getToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }

}
