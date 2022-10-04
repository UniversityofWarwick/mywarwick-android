package uk.ac.warwick.my.app.bridge;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.Keep;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.user.AnonymousUser;
import uk.ac.warwick.my.app.user.AuthenticatedUser;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

/**
 * Provides an interface for the WebView page to call back to the app,
 * and also implements some methods for making calls on the page.
 */
@Keep
public class MyWarwickJavaScriptInterface {

    private static final String OUTLOOK_PACKAGE = "com.microsoft.office.outlook";
    private static final Uri OUTLOOK_URI = Uri.parse("ms-outlook://");

    private final MyWarwickState state;
    private final JavascriptInvoker invoker;
    private final MyWarwickPreferences preferences;

    private static final String TAG = MyWarwickJavaScriptInterface.class.getName();

    public MyWarwickJavaScriptInterface(JavascriptInvoker invoker, MyWarwickState state, MyWarwickPreferences preferences) {
        this.state = state;
        this.invoker = invoker;
        this.preferences = preferences;
    }

    @JavascriptInterface
    public String getAppVersion() {
        return String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE);
    }

    @JavascriptInterface
    public int getAppBuild() {
        return BuildConfig.VERSION_CODE;
    }

    @JavascriptInterface
    public void loadDeviceDetails() {
        state.getActivity().runOnUiThread(() -> {
            String json = state.getDeviceDetails().toString().replace("'", "\\'");
            invoker.invokeMyWarwickMethod("feedback('" + json + "')");
        });
    }

    /**
     * Called by the web page when it is ready and the
     * MyWarwick variable is ready to be used.
     */
    @JavascriptInterface
    public void ready() {
        Log.d("MyWarwick", "JS ready()");
        invoker.ready();
    }

    @JavascriptInterface
    public void setUser(String user) {
        try {
            JSONObject object = new JSONObject(user);

            state.setUser(getUserFromJSONObject(object));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void setBackgroundToDisplay(int newBg, boolean isHighContrast) {
        state.onBackgroundChange(newBg, isHighContrast);
    }

    @JavascriptInterface
    public void setWebSignOnUrls(String signInUrl, String signOutUrl) {
        state.setSsoUrls(new SsoUrls(signInUrl, signOutUrl));
    }

    @JavascriptInterface
    public void setUnreadNotificationCount(int count) {
        state.setUnreadNotificationCount(count);
    }

    @JavascriptInterface
    public void setPath(String path) {
        state.setPath(path);
    }

    @JavascriptInterface
    public void setAppCached(Boolean cached) {
        state.setAppCached(cached);
    }

    @JavascriptInterface
    public void launchTour() {
        state.launchTour();
    }

    @JavascriptInterface
    public void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        String openMailLocalised = "Open email app";
        state.getActivity().startActivity(Intent.createChooser(intent, openMailLocalised));
    }

    @JavascriptInterface
    public void setTimetableToken(String token) {
        Log.i(Global.TAG, "Timetable token set to " + token);
        preferences.setTimetableToken(token);
    }

    @JavascriptInterface
    public void openOutlookApp() {
        if (isPackageInstalled(OUTLOOK_PACKAGE)) {
            Intent openOutlook = new Intent(Intent.ACTION_VIEW);
            openOutlook.setData(OUTLOOK_URI);
            state.getActivity().startActivity(openOutlook);
        } else {
            Toast.makeText(state.getActivity(), "Outlook app is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public void setTimetableNotificationsEnabled(boolean enabled) {
        preferences.setTimetableNotificationsEnabled(enabled);
    }

    @JavascriptInterface
    public void setTimetableNotificationTiming(int timing) {
        preferences.setTimetableNotificationTiming(timing);
    }

    @JavascriptInterface
    public void setTimetableNotificationsSoundEnabled(boolean enabled) {
        preferences.setTimetableNotificationsSoundEnabled(enabled);
    }

    @JavascriptInterface
    public void setFeatures(String jsonFeatures) {
        try {
            JSONObject features = new JSONObject(jsonFeatures);
            Iterator<String> keys = features.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                Log.d(TAG, "Setting feature: " + key + " -> " + features.getBoolean(key));
                preferences.setFeature(key, features.getBoolean(key));
            }
            // rebuild menu with new feature set
            state.getActivity().invalidateOptionsMenu();
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager packageManager = state.getActivity().getApplicationContext().getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private User getUserFromJSONObject(JSONObject user) throws JSONException {
        if (user.getBoolean("authenticated")) {
            JSONObject photo = user.getJSONObject("photo");

            return new AuthenticatedUser(
                    user.getString("usercode"),
                    user.getString("name"),
                    photo.getString("url"),
                    user.optBoolean("authoritative", true) // assume authoritative if not specified (by old web app version)
            );
        } else {
            return new AnonymousUser(user.optBoolean("authoritative", false)); // assume non-authoritative if unspecified
        }
    }

}
