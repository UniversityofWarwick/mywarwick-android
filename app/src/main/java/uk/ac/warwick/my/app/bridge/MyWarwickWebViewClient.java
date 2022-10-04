package uk.ac.warwick.my.app.bridge;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

public class MyWarwickWebViewClient extends WebViewClient {

    private static final String TAG = "MyWarwickWebViewClient";

    private final MyWarwickPreferences preferences;
    private final MyWarwickListener listener;
    private final MainActivity activity;

    public MyWarwickWebViewClient(MyWarwickPreferences preferences, MyWarwickListener listener, MainActivity activity) {
        this.preferences = preferences;
        this.listener = listener;
        this.activity = activity;
    }

    public static void openCustomTab(CustomTabsSession tabsSession, AppCompatActivity activity, WebView view, Uri url, int toolbarColor) {
        if (tabsSession != null) {
            CustomTabsIntent intent = new CustomTabsIntent.Builder(tabsSession)
                    .setToolbarColor(toolbarColor)
                    .setCloseButtonIcon(BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_arrow_back_white_24dp))
                    .setStartAnimations(activity, R.anim.slide_in_right, R.anim.slide_out_left)
                    .setExitAnimations(activity, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .addDefaultShareMenuItem()
                    .build();

            intent.intent.putExtra(Intent.EXTRA_REFERRER, view.getUrl());

            try {
                // If Chrome Custom Tabs is not available, the default browser will be launched instead
                intent.launchUrl(activity, url);
            } catch (IllegalArgumentException | ActivityNotFoundException e) {
                // Didn't work; try starting a browser with a simple intent
                MyWarwickWebViewClient.openPlainViewActivity(view.getContext(), url);
            }
        } else {
            Log.d(TAG, "Opening plain browser because no Custom Tabs client found (crashed?)");
            MyWarwickWebViewClient.openPlainViewActivity(view.getContext(), url);
        }
    }

    private static void openPlainViewActivity(Context context, Uri url) {
        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, url);
        fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(fallbackIntent);
        } catch (ActivityNotFoundException e2) {
            // Really didn't work; give up and let ourselves know
            try {
                FirebaseCrashlytics.getInstance().log("Caught ActivityNotFoundException when trying to open a URL");
                FirebaseCrashlytics.getInstance().recordException(e2);
            } catch (IllegalStateException ignored) {
            }

            Toast.makeText(context, "We couldn't open this link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Catches failure to load a page by showing a helpful message.
     */
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request.isForMainFrame()) {
            switch (error.getErrorCode()) {
                case ERROR_HOST_LOOKUP:
                case ERROR_CONNECT:
                case ERROR_TIMEOUT:
                    Log.d(Global.TAG, "onReceivedError for " + request.getUrl());
                    view.loadUrl("about:blank");
                    listener.onUncachedPageFail();
                    break;
                default:
                    super.onReceivedError(view, request, error);
            }
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d("MyWarwick", "Page loaded: " + url);
        String js = view.getContext().getString(R.string.bridge);
        view.loadUrl("javascript:" + js);

        flushCookies();
    }

    private void flushCookies() {
        CookieManager.getInstance().flush();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        String host = url.getHost();

        if (host != null) {
            if (host.equals(Global.getWebSignOnHost()) || host.equals(Global.MS_LOGIN_HOST)) {
                return listener.onSsoUrl(url);
            } else if (host.equals(preferences.getAppHost())) {
                return false;
            }
        }

        MyWarwickWebViewClient.openCustomTab(activity.getCustomTabsSession(), activity, view, url, activity.getThemePrimaryColour());

        return true;
    }

}
