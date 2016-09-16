package uk.ac.warwick.start.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.ac.warwick.start.app.R;

public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_DESTINATION_HOST = "DESTINATION_HOST";
    public static final String EXTRA_TITLE = "TITLE";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title != null) {
            setTitle(title);
        }

        WebView webView = getWebView();
        webView.getSettings().setJavaScriptEnabled(true);

        String destinationHost = getIntent().getStringExtra(EXTRA_DESTINATION_HOST);
        webView.setWebViewClient(destinationHost != null ? new DestinationWebViewClient(destinationHost) : new WebViewClient());

        String url = getIntent().getStringExtra(EXTRA_URL);
        webView.loadUrl(url);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        WebView webView = getWebView();

        // Go back if there are any pages to go back to, otherwise finish the activity
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private WebView getWebView() {
        return (WebView) findViewById(R.id.web_view);
    }

    /**
     * A WebViewClient that will finish this Activity when a page on the destination
     * host is reached.
     */
    public class DestinationWebViewClient extends WebViewClient {

        private String destinationHost;

        DestinationWebViewClient(String destinationHost) {
            this.destinationHost = destinationHost;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
            Uri url = Uri.parse(urlString);

            if (url.getHost().equals(destinationHost)) {
                setResult(RESULT_OK, new Intent());
                finish();
                return true;
            }

            return false;
        }

    }

}
