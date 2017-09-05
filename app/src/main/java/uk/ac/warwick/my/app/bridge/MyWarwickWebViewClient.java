package uk.ac.warwick.my.app.bridge;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.activities.MainActivity;

public class MyWarwickWebViewClient extends WebViewClient {

    private final MyWarwickPreferences preferences;
    private final MyWarwickListener listener;
    private final MainActivity activity;

    public MyWarwickWebViewClient(MyWarwickPreferences preferences, MyWarwickListener listener, MainActivity activity) {
        this.preferences = preferences;
        this.listener = listener;
        this.activity = activity;
    }

    /**
     * This method is deprecated on newer APIs but we are using older APIs. Note that this
     * should only be called for errors on the top level page, whereas the replacement APIs are
     * called for subresources as well, so would need to watch out for that.
     * <p>
     * Catches failure to load a page by showing a helpful message.
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        switch (errorCode) {
            case ERROR_HOST_LOOKUP:
            case ERROR_CONNECT:
            case ERROR_TIMEOUT:
                Log.d(Global.TAG, "onReceivedError for " + failingUrl);
                view.loadUrl("about:blank");
                listener.onUncachedPageFail();
                break;
            default:
                super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d("MyWarwick", "Page loaded: " + url);
        String js = view.getContext().getString(R.string.bridge);
        view.loadUrl("javascript:" + js);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
        Uri url = Uri.parse(urlString);
        String host = url.getHost();

        if (host.equals(Global.getWebSignOnHost())) {
            return listener.onSsoUrl(url);
        } else if (host.equals(preferences.getAppHost())) {
            return false;
        }

        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(activity.getThemePrimaryColour())
                .setCloseButtonIcon(BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_arrow_back_white_24dp))
                .setStartAnimations(activity, R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(activity, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .build();

        intent.intent.putExtra(Intent.EXTRA_REFERRER, view.getUrl());

        // If Chrome Custom Tabs is not available, the default browser will be launched instead
        intent.launchUrl(activity, url);

        return true;
    }

}
