package uk.ac.warwick.start.app.bridge;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import uk.ac.warwick.start.app.Global;
import uk.ac.warwick.start.app.user.AnonymousUser;
import uk.ac.warwick.start.app.user.AuthenticatedUser;
import uk.ac.warwick.start.app.user.User;
import uk.ac.warwick.start.app.user.SsoUrls;

public class StartWebViewClient extends WebViewClient {

    private StartState start;

    public StartWebViewClient(StartState startClient) {
        this.start = startClient;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
        Uri url = Uri.parse(urlString);

        if (url.getScheme().equalsIgnoreCase("start")) {
            view.evaluateJavascript("Start.APP", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    try {
                        JSONObject state = new JSONObject(value);

                        start.setApplicationOrigins(getApplicationOriginsFromState(state));
                        start.setPath(state.getString("currentPath"));
                        start.setUnreadNotificationCount(state.getInt("unreadNotificationCount"));
                        start.setUser(getUserFromState(state));
                        start.setSsoUrls(getSsoUrlsFromState(state));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return true;
        }

        if (!start.isApplicationOrigin(getOrigin(url))) {
            Intent intent = new Intent(Intent.ACTION_VIEW, url);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);
            return true;
        }

        return false;
    }

    private String getOrigin(Uri url) {
        return url.getScheme() + "://" + url.getHost();
    }

    private User getUserFromState(JSONObject state) throws JSONException {
        JSONObject userObject = state.getJSONObject("user");

        if (userObject.getBoolean("authenticated")) {
            return new AuthenticatedUser(
                    userObject.getString("usercode"),
                    userObject.getString("name"),
                    userObject.getJSONObject("photo").getString("url")
            );
        } else {
            return new AnonymousUser();
        }
    }

    private Collection<String> getApplicationOriginsFromState(JSONObject state) throws JSONException {
        JSONArray originsArray = state.getJSONArray("applicationOrigins");

        Collection<String> origins = new ArrayList<>(originsArray.length() + 1);
        origins.add("https://" + Global.getStartHost());

        for (int i = 0; i < originsArray.length(); i++) {
            String origin = originsArray.getString(i);
            origins.add(origin);
        }

        return origins;
    }

    private SsoUrls getSsoUrlsFromState(JSONObject state) throws JSONException {
        JSONObject urlsObject = state.getJSONObject("ssoUrls");

        return new SsoUrls(
                urlsObject.getString("login"),
                urlsObject.getString("logout")
        );
    }

}
