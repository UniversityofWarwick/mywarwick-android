package uk.ac.warwick.my.app.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import uk.ac.warwick.my.app.services.EventFetcher;
import uk.ac.warwick.my.app.services.EventNotificationScheduler;

public class MyWarwickPreferences {

    public static final String FEATURES_PREFS = "features";

    private static final String CUSTOM = "__custom__";
    private static final String CUSTOM_SERVER_ADDRESS = "custom_server_address";
    private static final String NEEDS_RELOAD = "mywarwick_needsreload";
    private static final String PUSH_NOTIFICATION_TOKEN = "mywarwick_push_notification_token";
    private static final String SERVER = "mywarwick_server";
    private static final String TOUR_COMPLETE = "mywarwick_tour_complete";
    private static final String CHOSEN_BG = "mywarwick_chosen_background";
    private static final String IS_HIGH_CONTRAST_BG = "mywarwick_is_high_contrast_background";

    private static final String TIMETABLE_TOKEN = "mywarwick_timetable_token";
    private static final String TIMETABLE_NOTIFICATIONS_ENABLED = "mywarwick_timetable_notifications_enabled";
    private static final String TIMETABLE_NOTIFICATION_TIMING = "mywarwick_timetable_notification_timing";
    private static final String TIMETABLE_NOTIFICATIONS_SOUND_ENABLED = "mywarwick_timetable_notifications_sound_enabled";
    private static final String TIMETABLE_TOKEN_REFRESH = "mywarwick_timetable_token_refresh";

    private static final String DO_NOT_DISTURB_ENABLED = "mywarwick_do_not_disturb_enabled";
    private static final String DO_NOT_DISTURB_WKDAY_START_HR = "mywarwick_do_not_disturb_wkday_start_hr";
    private static final String DO_NOT_DISTURB_WKDAY_END_HR = "mywarwick_do_not_disturb_wkday_end_hr";
    private static final String DO_NOT_DISTURB_WKEND_START_HR = "mywarwick_do_not_disturb_wkend_start_hr";
    private static final String DO_NOT_DISTURB_WKEND_END_HR = "mywarwick_do_not_disturb_wkend_end_hr";

    private static final int DEFAULT_BACKGROUND = 1;
    private static final boolean DEFAULT_IS_HIGH_CONTRAST = false;

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences featurePreferences;

    public MyWarwickPreferences(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.featurePreferences = context.getSharedPreferences(FEATURES_PREFS, Context.MODE_PRIVATE);
    }

    public MyWarwickPreferences(Context context, SharedPreferences sharedPreferences, SharedPreferences featurePreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.featurePreferences = featurePreferences;
    }

    public void setFeature(String name, boolean value) {
        featurePreferences.edit()
                .putBoolean(name, value)
                .apply();
    }

    public boolean featureEnabled(String name) {
        return featurePreferences.getBoolean(name, false);
    }

    public String getPushNotificationToken() {
        return sharedPreferences.getString(PUSH_NOTIFICATION_TOKEN, null);
    }

    public void setPushNotificationToken(String token) {
        sharedPreferences.edit()
                .putString(PUSH_NOTIFICATION_TOKEN, token)
                .apply();
    }

    public String getAppURL() {
        String url = sharedPreferences.getString(SERVER, "");
        if (url.equals(CUSTOM)) {
            // get custom url from preference
            url = sharedPreferences.getString(CUSTOM_SERVER_ADDRESS, "");
            if (!url.startsWith("https://")) {
                url = String.format("https://%s", url);
            }
        }
        return url;
    }

    public String getAppHost() {
        return Uri.parse(getAppURL()).getHost();
    }

    /**
     * Stores whether the WebView needs to be reloaded - maybe we've signed out,
     * or changed the target server.
     */
    public boolean needsReload() {
        return sharedPreferences.getBoolean(NEEDS_RELOAD, false);
    }

    public void setNeedsReload(boolean needed) {
        sharedPreferences.edit().putBoolean(NEEDS_RELOAD, needed).apply();
    }

    public boolean isTourComplete() {
        return sharedPreferences.getBoolean(TOUR_COMPLETE, false);
    }

    public void setTourComplete() {
        sharedPreferences.edit().putBoolean(TOUR_COMPLETE, true).apply();
    }

    public void setBackgroundChoice(int bgId) {
        sharedPreferences.edit().putInt(CHOSEN_BG, bgId).apply();
    }

    public int getBackgroundChoice() {
        return sharedPreferences.getInt(CHOSEN_BG, DEFAULT_BACKGROUND);
    }

    public void setHighContrastChoice(boolean isHighContrast) {
        sharedPreferences.edit().putBoolean(IS_HIGH_CONTRAST_BG, isHighContrast).apply();
    }

    public boolean getHighContrastChoice() {
        return sharedPreferences.getBoolean(IS_HIGH_CONTRAST_BG, DEFAULT_IS_HIGH_CONTRAST);
    }

    public void setTimetableToken(String token) {
        sharedPreferences.edit()
                .putString(TIMETABLE_TOKEN, token)
                .remove(TIMETABLE_TOKEN_REFRESH)
                .apply();

        if (token != null) {
            new EventFetcher(context).updateEvents();
        }
    }

    public String getTimetableToken() {
        return sharedPreferences.getString(TIMETABLE_TOKEN, null);
    }

    public void setTimetableNotificationsEnabled(boolean enabled) {
        if (enabled != isTimetableNotificationsEnabled()) {
            sharedPreferences.edit().putBoolean(TIMETABLE_NOTIFICATIONS_ENABLED, enabled).apply();

            new EventNotificationScheduler(context).scheduleNextNotification();
        }
    }

    public boolean isTimetableNotificationsEnabled() {
        return sharedPreferences.getBoolean(TIMETABLE_NOTIFICATIONS_ENABLED, true);
    }

    public void setTimetableNotificationTiming(int timing) {
        if (timing != getTimetableNotificationTiming()) {
            sharedPreferences.edit().putInt(TIMETABLE_NOTIFICATION_TIMING, timing).apply();

            new EventNotificationScheduler(context).scheduleNextNotification();
        }
    }

    public int getTimetableNotificationTiming() {
        return sharedPreferences.getInt(TIMETABLE_NOTIFICATION_TIMING, 15);
    }

    public void setTimetableNotificationsSoundEnabled(boolean enabled) {
        if (enabled != isTimetableNotificationsSoundEnabled()) {
            sharedPreferences.edit().putBoolean(TIMETABLE_NOTIFICATIONS_SOUND_ENABLED, enabled).apply();
        }
    }

    public boolean isTimetableNotificationsSoundEnabled() {
        return sharedPreferences.getBoolean(TIMETABLE_NOTIFICATIONS_SOUND_ENABLED, true);
    }

    public void setNeedsTimetableTokenRefresh(boolean refresh) {
        sharedPreferences.edit().putBoolean(TIMETABLE_TOKEN_REFRESH, refresh).apply();
    }

    public boolean isNeedsTimetableTokenRefresh() {
        return sharedPreferences.getBoolean(TIMETABLE_TOKEN_REFRESH, false);
    }

    public void setDoNotDisturbEnabled(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(DO_NOT_DISTURB_ENABLED, isEnabled).apply();
    }

    public void setDoNotDisturbPeriods(String wkStart, String wkEnd, String wkndStart, String wkndEnd) {
        sharedPreferences.edit()
                .putString(DO_NOT_DISTURB_WKDAY_START_HR, wkStart)
                .putString(DO_NOT_DISTURB_WKDAY_END_HR, wkEnd)
                .putString(DO_NOT_DISTURB_WKEND_START_HR, wkndStart)
                .putString(DO_NOT_DISTURB_WKEND_END_HR, wkndEnd)
                .apply();
    }

    public boolean getDoNotDisturbEnabled() {
        return sharedPreferences.getBoolean(DO_NOT_DISTURB_ENABLED, false);
    }

    public String getDnDWeekdayStart() {
        return sharedPreferences.getString(DO_NOT_DISTURB_WKDAY_START_HR, "21:00");
    }

    public String getDnDWeekdayEnd() {
        return sharedPreferences.getString(DO_NOT_DISTURB_WKDAY_END_HR, "07:00");
    }

    public String getDnDWeekendStart() {
        return sharedPreferences.getString(DO_NOT_DISTURB_WKEND_START_HR, "21:00");
    }

    public String getDnDWeekendEnd() {
        return sharedPreferences.getString(DO_NOT_DISTURB_WKEND_END_HR, "07:00");
    }
}
