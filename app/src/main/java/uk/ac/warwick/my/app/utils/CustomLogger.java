package uk.ac.warwick.my.app.utils;

import static com.google.common.net.HttpHeaders.USER_AGENT;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

public class CustomLogger {
    public static void log(Context context, String message) {
        MyWarwickPreferences preferences = new MyWarwickPreferences(context);
        OkHttpClient http = new OkHttpClient.Builder().build();
        String base = preferences.getAppURL();

        try {
            JSONArray postBody = new JSONArray().put(new JSONObject()
                    .put("message", message));
            Request request = new Request.Builder()
                    .url(base + "/api/errors/js")
                    .header(USER_AGENT, "MyWarwick/" + BuildConfig.VERSION_NAME)
                    .post(RequestBody.create(MediaType.parse("application/json"), postBody.toString()))
                    .build();
            Response response = http.newCall(request).execute();
            ResponseBody body = response.body();

            if (body == null) {
                throw new RuntimeException("Response body was null");
            }

            if (!response.isSuccessful()) {
                throw new RuntimeException("Error response: " + response.code() + " " + response.message());
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Error logging custom data (" + message + ")", e);
        }
    }
}
