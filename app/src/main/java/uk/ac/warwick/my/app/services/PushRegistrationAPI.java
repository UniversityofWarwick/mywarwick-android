package uk.ac.warwick.my.app.services;


import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class PushRegistrationAPI {
    private static final String TAG = PushRegistrationAPI.class.getSimpleName();

    private final OkHttpClient http;
    private MyWarwickPreferences preferences;

    public PushRegistrationAPI(MyWarwickPreferences preferences) {
        this.preferences = preferences;
        this.http = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(1, 1, TimeUnit.SECONDS))
                .build();
    }

    public void unregister(String deviceToken) {
        try {
            String url = preferences.getAppURL() + "/api/push/unsubscribe";

            Log.i(TAG, "Attempting to unregister device token");
            JSONObject body = new JSONObject()
                    .put("deviceToken", deviceToken);
            Request request = new Request.Builder()
                    .url(url)
                    .header(USER_AGENT, "MyWarwick/" + BuildConfig.VERSION_NAME)
                    .post(RequestBody.create(MediaType.parse("application/json"), body.toString()))
                    .build();

            http.newCall(request).enqueue(new Callback() {
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to unregister device token - " + e.toString());
                }

                public void onResponse(Call call, Response response) throws IOException {
                    Log.i(TAG, "Successfully unregistered device token");
                }
            });
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}
