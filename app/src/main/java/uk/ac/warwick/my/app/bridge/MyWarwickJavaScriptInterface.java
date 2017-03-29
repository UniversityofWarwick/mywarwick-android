package uk.ac.warwick.my.app.bridge;

import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.warwick.my.app.user.AnonymousUser;
import uk.ac.warwick.my.app.user.AuthenticatedUser;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

/**
 * Provides an interface for the WebView page to call back to the app,
 * and also implements some methods for making calls on the page.
 */
public class MyWarwickJavaScriptInterface {

    private final MyWarwickState startState;
    private final JavascriptInvoker invoker;

    public MyWarwickJavaScriptInterface(JavascriptInvoker invoker, MyWarwickState startState) {
        this.startState = startState;
        this.invoker = invoker;
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

            startState.setUser(getUserFromJSONObject(object));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void setWebSignOnUrls(String signInUrl, String signOutUrl) {
        startState.setSsoUrls(new SsoUrls(signInUrl, signOutUrl));
    }

    @JavascriptInterface
    public void setUnreadNotificationCount(int count) {
        startState.setUnreadNotificationCount(count);
    }

    @JavascriptInterface
    public void setPath(String path) {
        startState.setPath(path);
    }

    @JavascriptInterface
    public void setAppCached(Boolean cached) {
        startState.setAppCached(cached);
    }

    private User getUserFromJSONObject(JSONObject user) throws JSONException {
        if (user.getBoolean("authenticated")) {
            JSONObject photo = user.getJSONObject("photo");

            return new AuthenticatedUser(
                    user.getString("usercode"),
                    user.getString("name"),
                    photo.getString("url"),
                    user.getBoolean("authoritative")
            );
        } else {
            return new AnonymousUser(user.getBoolean("authoritative"));
        }
    }

}
