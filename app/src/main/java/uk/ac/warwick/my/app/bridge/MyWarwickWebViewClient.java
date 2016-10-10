package uk.ac.warwick.my.app.bridge;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.ac.warwick.my.app.R;

public class MyWarwickWebViewClient extends WebViewClient {

    private final MyWarwickPreferences preferences;

    public MyWarwickWebViewClient(MyWarwickPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        String js = view.getContext().getString(R.string.bridge);
        view.loadUrl("javascript:" + js);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
        Uri url = Uri.parse(urlString);

        if (!url.getHost().equals(preferences.getAppHost())) {
            Intent intent = new Intent(Intent.ACTION_VIEW, url);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);
            return true;
        }

        return false;
    }

}
