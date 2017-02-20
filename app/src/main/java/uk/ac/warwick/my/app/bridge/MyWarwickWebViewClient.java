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
    private final JavascriptInvoker invoker;

    public MyWarwickWebViewClient(JavascriptInvoker invoker, MyWarwickPreferences preferences) {
        this.preferences = preferences;
        this.invoker = invoker;
    }

    private String lastLoadedUrl = null;

    @Override
    public void onPageFinished(WebView view, String url) {
        if (!url.equals(lastLoadedUrl)) {
            Log.d("MyWarwick", "Page loaded: " + url);
            String js = view.getContext().getString(R.string.bridge);
            view.loadUrl("javascript:" + js);
        }
        lastLoadedUrl = url;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
        Uri url = Uri.parse(urlString);
        String host = url.getHost();

        if (host.equals(preferences.getAppHost()) || host.equals(Global.getWebSignOnHost())) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        view.getContext().startActivity(intent);
        return true;
    }

}
