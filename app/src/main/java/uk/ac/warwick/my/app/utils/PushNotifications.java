package uk.ac.warwick.my.app.utils;

import android.app.Activity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class PushNotifications {

    public static void getToken(Activity activity, OnSuccessListener<InstanceIdResult> onSuccess) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(activity, onSuccess);
    }

}
