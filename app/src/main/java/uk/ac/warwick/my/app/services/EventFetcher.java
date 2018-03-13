package uk.ac.warwick.my.app.services;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.data.Event;
import uk.ac.warwick.my.app.data.EventDao;
import uk.ac.warwick.my.app.utils.ISO8601RoughParser;

import static com.google.common.net.HttpHeaders.USER_AGENT;
import static uk.ac.warwick.my.app.Global.TAG;

public class EventFetcher {

    private static final ThreadLocal<ISO8601RoughParser> dateFormat = new ThreadLocal<ISO8601RoughParser>() {
        protected ISO8601RoughParser initialValue() {
            return new ISO8601RoughParser();
        }
    };

    private static final String X_TIMETABLE_TOKEN = "X-Timetable-Token";

    private final Context context;
    private final OkHttpClient http;
    private final MyWarwickPreferences preferences;

    public EventFetcher(Context context) {
        this(context, new MyWarwickPreferences(context));
    }

    public EventFetcher(Context context, MyWarwickPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        http = new OkHttpClient.Builder()
                .build();
    }

    private Collection<Event> fetchEvents() throws FetchException {
        String base = preferences.getAppURL();
        String token = preferences.getTimetableToken();

        if (token == null || token.isEmpty()) {
            throw new FetchException("No token for fetching timetable");
        }

        Request request = new Request.Builder()
                .url(base + "/api/timetable")
                .header(USER_AGENT, "MyWarwick/" + BuildConfig.VERSION_NAME)
                .header(X_TIMETABLE_TOKEN, token)
                .build();

        try {
            Response response = http.newCall(request).execute();
            ResponseBody body = response.body();

            if (body == null) {
                throw new FetchException("Response body was null");
            }

            if (!response.isSuccessful()) {
                if (response.code() == 401 || response.code() == 403) {
                    // Our token might be bad - request another one when we next have a chance
                    Log.d(TAG, "Received " + response.code() + " from server: setting token refresh flag");
                    preferences.setNeedsTimetableTokenRefresh(true);
                }

                throw new FetchException("Error response: " + response.code() + " " + response.message());
            }

            preferences.setNeedsTimetableTokenRefresh(false);

            JSONObject object = new JSONObject(body.string());

            JSONArray items = object.getJSONObject("data").getJSONArray("items");

            List<Event> events = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                events.add(buildEvent(items.getJSONObject(i)));
            }

            return events;
        } catch (IOException e) {
            throw new FetchException("Error fetching data", e);
        } catch (JSONException e) {
            throw new FetchException("Error parsing JSON", e);
        } catch (ParseException e) {
            throw new FetchException("Error parsing date in event data", e);
        }
    }

    public static class FetchException extends Exception {
        public FetchException(String message) {
            super(message);
        }

        public FetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void updateEvents() {
        try {
            Collection<Event> events = fetchEvents();

            try (EventDao dao = new EventDao(context)) {
                dao.replaceAll(events);
            }

            new EventNotificationScheduler(context).scheduleNextNotification();
        } catch (FetchException e) {
            Log.e(TAG, "Error updating events", e);
            try {
                Crashlytics.logException(new Exception("Error updating events", e));
            } catch (IllegalStateException e2) {
                Log.e(TAG, "Error reporting error!", e2);
            }
        }
    }

    Event buildEvent(JSONObject obj) throws JSONException, ParseException {
        Event event = new Event();

        event.setServerId(obj.getString("id"));
        event.setType(obj.getString("type"));
        event.setTitle(obj.optString("title"));
        event.setStart(dateFormat.get().parse(obj.getString("start")));
        event.setEnd(dateFormat.get().parse(obj.getString("end")));

        JSONObject location = obj.optJSONObject("location");
        if (location != null) {
            event.setLocation(location.getString("name"));
        }

        JSONObject parent = obj.optJSONObject("parent");
        if (parent != null) {
            event.setParentFullName(parent.optString("fullName"));
            event.setParentShortName(parent.optString("shortName"));
        }

        return event;
    }

}
