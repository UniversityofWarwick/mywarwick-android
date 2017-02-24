package uk.ac.warwick.my.app.bridge;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class MyWarwickPreferences {

    private SharedPreferences sharedPreferences;

    public MyWarwickPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public String getAppURL() {
        String appHost = sharedPreferences.getString("mywarwick_server", "");
        if (appHost.equals("__custom__")) {
            // get custom url from preference
            appHost = sharedPreferences.getString("custom_server_address", "");
        }
        return appHost;
    }

    public String getAppHost() {
        return Uri.parse(getAppURL()).getHost();
    }

    /**
     * Stores whether the WebView needs to be reloaded - maybe we've signed out,
     * or changed the target server.
     */
    public boolean needsReload() {
        return sharedPreferences.getBoolean("mywarwick_needsreload", false);
    }

    public void setNeedsReload(boolean needed) {
        sharedPreferences.edit().putBoolean("mywarwick_needsreload", needed).apply();
    }

}
