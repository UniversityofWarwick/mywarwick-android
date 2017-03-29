package uk.ac.warwick.my.app.bridge;

import android.content.SharedPreferences;
import android.net.Uri;

public class MyWarwickPreferences {

    private static final String CUSTOM = "__custom__";
    private static final String CUSTOM_SERVER_ADDRESS = "custom_server_address";
    private static final String NEEDS_RELOAD = "mywarwick_needsreload";
    private static final String PUSH_NOTIFICATION_TOKEN = "mywarwick_push_notification_token";
    private static final String SERVER = "mywarwick_server";
    public static final String PUSH_TOKEN_ACTIVE = "mywarwick_token_active";

    private SharedPreferences sharedPreferences;

    public MyWarwickPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public String getPushNotificationToken() {
        return sharedPreferences.getString(PUSH_NOTIFICATION_TOKEN, null);
    }

    public boolean isPushNotificationTokenActive() {
        return sharedPreferences.getBoolean(PUSH_TOKEN_ACTIVE, false);
    }

    public void setPushNotificationToken(String token) {
        sharedPreferences.edit()
                .putString(PUSH_NOTIFICATION_TOKEN, token)
                .putBoolean(PUSH_TOKEN_ACTIVE, true)
                .apply();
    }

    public void deactivatePushNotificationToken() {
        sharedPreferences.edit().putBoolean(PUSH_TOKEN_ACTIVE, false).apply();
    }

    public String getAppURL() {
        String url = sharedPreferences.getString(SERVER, "");
        if (url.equals(CUSTOM)) {
            // get custom url from preference
            url = sharedPreferences.getString(CUSTOM_SERVER_ADDRESS, "");
        }
        url = "https://swordfish.warwick.ac.uk";
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

}
