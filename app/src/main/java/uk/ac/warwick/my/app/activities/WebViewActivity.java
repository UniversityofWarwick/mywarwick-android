package uk.ac.warwick.my.app.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.bridge.MyWarwickWebViewClient;
import uk.ac.warwick.my.app.helper.Objects;

/**
 * Used as a full screen web page for signing in to websignon.
 */
public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_DESTINATION_HOST = "DESTINATION_HOST";
    public static final String EXTRA_TITLE = "TITLE";
    private static final String TAG = "WebViewActivity";

    private CustomTabsSession customTabsSession;
    private CustomTabsServiceConnection tabsConnection;

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
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + getString(R.string.user_agent_prefix) + BuildConfig.VERSION_NAME);

        String destinationHost = getIntent().getStringExtra(EXTRA_DESTINATION_HOST);
        webView.setWebViewClient(destinationHost != null ? new DestinationWebViewClient(this, destinationHost) : new WebViewClient());

        String url = getIntent().getStringExtra(EXTRA_URL);
        try {
            FirebaseCrashlytics.getInstance().log("loadUrl: " + url);
        } catch (IllegalStateException ignored) {
        }
        webView.loadUrl(url);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        initCustomTabs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        // matches init call in onStart
        deinitCustomTabs();
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

    private void deinitCustomTabs() {
        if (tabsConnection != null) {
            unbindService(tabsConnection);
            tabsConnection = null;
        }
    }

    private void initCustomTabs() {
        tabsConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                Log.d(TAG, "Custom Tabs service connected");
                client.warmup(0);
                customTabsSession = client.newSession(new CustomTabsCallback());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e(TAG, "Custom Tabs service disconnected/crashed");
                customTabsSession = null;
            }
        };

        CustomTabsClient.bindCustomTabsService(this, Global.CUSTOM_TAB_PACKAGE_NAME_FALLBACK, tabsConnection);
    }

    /**
     * A WebViewClient that will finish this Activity when a page on the destination
     * host is reached.
     */
    public class DestinationWebViewClient extends WebViewClient {

        private WebViewActivity activity;
        private String destinationHost;

        DestinationWebViewClient(WebViewActivity activity, String destinationHost) {
            this.activity = activity;
            this.destinationHost = destinationHost;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
            Uri url = Uri.parse(urlString);
            String host = url.getHost();

            if (Objects.equals(url.getHost(), destinationHost)) {
                setResult(RESULT_OK, new Intent());
                finish();
                return true;
            }

            if (!Objects.equals(host, Global.getWebSignOnHost()) && !Objects.equals(host, Global.MS_LOGIN_HOST)) {
                MyWarwickWebViewClient.openCustomTab(customTabsSession, activity, view, url, activity.getResources().getColor(R.color.colorPrimary1));

                return true;
            }

            return false;
        }

    }

}
