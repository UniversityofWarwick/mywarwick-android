package uk.ac.warwick.my.app.bridge;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;

public class MyWarwickWebViewClient extends WebViewClient {

    private final MyWarwickPreferences preferences;
    private final MyWarwickListener listener;

    public MyWarwickWebViewClient(MyWarwickPreferences preferences, MyWarwickListener listener) {
        this.preferences = preferences;
        this.listener = listener;
    }


    /**
     * This method is deprecated on newer APIs but we are using older APIs. Note that this
     * should only be called for errors on the top level page, whereas the replacement APIs are
     * called for subresources as well, so would need to watch out for that.
     *
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

        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        view.getContext().startActivity(intent);
        return true;
    }

}
