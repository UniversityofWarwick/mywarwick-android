package uk.ac.warwick.my.app.bridge;

import android.app.Activity;
import android.util.DisplayMetrics;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.warwick.my.app.helper.Objects;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

/**
 * A place to put all of My Warwick's state, that can notify an interested
 * listener when things change.
 */
public class MyWarwickState {

    private User user;
    private int unreadNotificationCount;
    private int bgId = 0;
    private boolean isHighContrast = false;
    private String path;
    private SsoUrls ssoUrls;
    private MyWarwickListener listener;
    private Activity activity;
    private Boolean appCached;
    private JSONObject staticDeviceDetails;

    public MyWarwickState(MyWarwickListener listener, Activity activity) {
        this.listener = listener;
        this.activity = activity;
        setupStaticDeviceDetails();
    }

    private void setupStaticDeviceDetails() {
        try {
            staticDeviceDetails = new JSONObject();
            staticDeviceDetails.put("os", "Android");
            staticDeviceDetails.put("os-version", System.getProperty("os.version") + " (" + android.os.Build.VERSION.INCREMENTAL + ")");
            staticDeviceDetails.put("os-api", android.os.Build.VERSION.SDK_INT);
            staticDeviceDetails.put("device", android.os.Build.DEVICE);
            staticDeviceDetails.put("model", android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")");
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public SsoUrls getSsoUrls() {
        return ssoUrls;
    }

    public void setSsoUrls(SsoUrls ssoUrls) {
        if (listener != null && !ssoUrls.equals(this.ssoUrls)) {
            listener.onSetSsoUrls(ssoUrls);
        }
        this.ssoUrls = ssoUrls;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (listener != null && !path.equals(this.path)) {
            listener.onPathChange(path);
        }

        this.path = path;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        if (listener != null && !Objects.equals(user, this.user)) {
            listener.onUserChange(user);
        }

        this.user = user;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(int count) {
        if (listener != null && count != this.unreadNotificationCount) {
            listener.onUnreadNotificationCountChange(count);
        }

        this.unreadNotificationCount = count;
    }

    public void setAppCached(Boolean appCached) {
        this.appCached = appCached;
    }

    public Boolean getAppCached() {
        return appCached;
    }

    public JSONObject getDeviceDetails() {
        try {
            JSONObject details = new JSONObject(staticDeviceDetails.toString());
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            details.put("screen-width", metrics.widthPixels);
            details.put("screen-height", metrics.heightPixels);
            details.put("path", getPath());
            return details;
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return new JSONObject();
        }
    }

    public int getBgId() {
        return this.bgId;
    }

    public void setBgId(int bgId) {
        this.bgId = bgId;
    }

    public boolean getisHighContrast() { return this.isHighContrast; }

    public void setHighContrast(boolean isHighContrast) {
        this.isHighContrast = isHighContrast;
    }

    protected void onBackgroundChange(int bgId, boolean isHighContrast) {
        if (this.bgId != bgId || this.isHighContrast != isHighContrast) {
            listener.onBackgroundChange(bgId, isHighContrast);
        }
        this.setBgId(bgId);
        this.setHighContrast(isHighContrast);
    }

    public void launchTour() {
        listener.launchTour();
    }

    public Activity getActivity() {
        return activity;
    }

    public boolean isUserSignedIn() {
        return user != null && user.isSignedIn();
    }
}
