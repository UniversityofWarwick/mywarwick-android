package uk.ac.warwick.my.app.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabSelectListener;

import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.bridge.JavascriptInvoker;
import uk.ac.warwick.my.app.bridge.MyWarwickJavaScriptInterface;
import uk.ac.warwick.my.app.bridge.MyWarwickListener;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.bridge.MyWarwickState;
import uk.ac.warwick.my.app.bridge.MyWarwickWebViewClient;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;
import uk.ac.warwick.my.app.utils.DownloadImageTask;
import uk.ac.warwick.my.app.utils.PushNotifications;

public class MainActivity extends AppCompatActivity implements OnTabSelectListener, MyWarwickListener {

    public static final int TAB_INDEX_NOTIFICATIONS = 1;
    public static final int TAB_INDEX_ACTIVITIES = 2;

    public static final String ROOT_PATH = "/";
    public static final String SEARCH_PATH = "/search";

    public static final int SIGN_IN = 1;
    public static final String NOTIFICATIONS_PATH = "/notifications";

    private MyWarwickState myWarwick = new MyWarwickState(this);
    private MyWarwickPreferences myWarwickPreferences;
    private MenuItem searchItem;
    private BroadcastReceiver tokenRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            registerForPushNotifications();
        }
    };
    private MyWarwickJavaScriptInterface javascriptInterface;
    private JavascriptInvoker invoker;

    @Override
    public void onTabSelected(@IdRes int tabId) {
        String path = getPathForTabItem(tabId);

        // Prevent double navigation (NEWSTART-500)
        if (!path.equals(myWarwick.getPath())) {
            appNavigate(path);
        } else {
            Log.d("MyWarwick", "Not navigating to " + path + " because we're already on it.");
        }
    }

    @Override
    public void onPathChange(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(getTitleForPath(path));

                getBottomBar().selectTabWithId(getTabItemForPath(path));

                ActionBar actionBar = getSupportActionBar();

                if (actionBar != null) {
                    if (path.startsWith("/tiles")) {
                        // Display a back arrow in place of the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    } else {
                        // Restore the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(false);
                    }
                }

                if (path.equals("/search") && searchItem != null) {
                    // Show the search field in the action bar on /search
                    MenuItemCompat.expandActionView(searchItem);
                }
            }
        });
    }

    @Override
    public void onUnreadNotificationCountChange(final int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BottomBarTab tab = getBottomBar().getTabAtPosition(TAB_INDEX_NOTIFICATIONS);

                tab.setBadgeCount(count);
            }
        });
    }

    @Override
    public void onUserChange(final User user) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View accountPhotoView = getAccountPhotoView();

                View cardView = accountPhotoView.findViewById(R.id.image_card_view);
                ImageView photoView = (ImageView) accountPhotoView.findViewById(R.id.image_view);

                if (user.isSignedIn()) {
                    new DownloadImageTask(photoView, cardView).execute(user.getPhotoUrl());

                    registerForPushNotifications();
                } else {
                    photoView.setImageURI(null);
                    cardView.setVisibility(View.GONE);
                }

                BottomBar bottomBar = getBottomBar();

                bottomBar.getTabAtPosition(TAB_INDEX_NOTIFICATIONS).setEnabled(user.isSignedIn());
                bottomBar.getTabAtPosition(TAB_INDEX_ACTIVITIES).setEnabled(user.isSignedIn());

                // Cause the options menu to be updated to reflect the signed in/out state
                supportInvalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onSetSsoUrls(final SsoUrls ssoUrls) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                supportInvalidateOptionsMenu();
            }
        });
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        WebView webView = getWebView();
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + getString(R.string.user_agent));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        this.myWarwickPreferences = new MyWarwickPreferences(PreferenceManager.getDefaultSharedPreferences(this));
        this.invoker = new JavascriptInvoker(webView);
        this.javascriptInterface = new MyWarwickJavaScriptInterface(invoker, myWarwick);
        webView.addJavascriptInterface(javascriptInterface, "MyWarwickAndroid");

        MyWarwickWebViewClient webViewClient = new MyWarwickWebViewClient(invoker, myWarwickPreferences);
        webView.setWebViewClient(webViewClient);

        getBottomBar().setOnTabSelectListener(this);

        ActionBar actionBar = getSupportActionBar();

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_TITLE);
            View accountPhotoView = getLayoutInflater().inflate(R.layout.account_photo_view, null);
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END);
            actionBar.setCustomView(accountPhotoView, layoutParams);

            View cardView = accountPhotoView.findViewById(R.id.image_card_view);
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAccountPopupMenu(v);
                }
            });
        }

        String appURL = myWarwickPreferences.getAppURL();
        if (isOpenedFromNotification()) {
            onPathChange(NOTIFICATIONS_PATH);
            webView.loadUrl(appURL + NOTIFICATIONS_PATH);
        } else {
            webView.loadUrl(appURL);
        }

        registerTokenRefreshReceiver();
    }

    private void registerTokenRefreshReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PushNotifications.TOKEN_REFRESH);

        registerReceiver(tokenRefreshReceiver, intentFilter);
    }

    private void registerForPushNotifications() {
        User user = myWarwick.getUser();

        if (user == null || !user.isSignedIn()) {
            // Only do this for signed-in users
            return;
        }

        String token = PushNotifications.getToken();

        if (token == null) {
            // The token might not have been generated yet
            return;
        }

        invoker.invokeMyWarwickMethod(String.format("registerForFCM('%s')", token));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(tokenRefreshReceiver);
        invoker.reset();
        invoker.clear();
        Log.d("MyWarwick", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Let the embedded app know that it's being brought to the foreground
        invoker.invokeMyWarwickMethod("onApplicationDidBecomeActive()");
    }

    private boolean isOpenedFromNotification() {
        Bundle extras = getIntent().getExtras();

        return extras != null && extras.containsKey("from");
    }

    private void showAccountPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.account);
        popup.show();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_account_settings:
                        startAccountSettingsActivity();
                        return true;
                    case R.id.action_sign_out:
                        signOut();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void startAccountSettingsActivity() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, "https://" + Global.getWebSignOnHost() + "/origin/account");
        intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.account_settings));
        startActivity(intent);
    }

    private View getAccountPhotoView() {
        return getSupportActionBar().getCustomView();
    }

    private BottomBar getBottomBar() {
        return (BottomBar) findViewById(R.id.bottom_bar);
    }

    @Override
    public void onBackPressed() {
        if (myWarwick.getPath() != null && myWarwick.getPath().startsWith("/tiles")) {
            // If we are looking at zoomed tile, unzoom it
            appNavigate(ROOT_PATH);
        } else {
            // Otherwise do the default thing
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }

    private void appNavigate(String path) {
        invoker.invokeMyWarwickMethod(String.format("navigate('%s')", path));
    }

    private void appSearch(String query) {
        invoker.invokeMyWarwickMethod(String.format("search('%s')", query.replace("'", "\\'")));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (myWarwick.getSsoUrls() != null && myWarwick.getUser() != null) {
            getMenuInflater().inflate(myWarwick.getUser().isSignedIn() ? R.menu.signed_in : R.menu.signed_out, menu);
        }

        searchItem = menu.findItem(R.id.action_search);

        if (searchItem != null) {
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setQueryHint(getString(R.string.search_warwick));

            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    appNavigate(SEARCH_PATH);
                    getBottomBar().setVisibility(View.GONE);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // When pressing the back arrow in the search field, go back to /
                    getBottomBar().setVisibility(View.VISIBLE);
                    appNavigate(ROOT_PATH);
                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    appSearch(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SIGN_IN && resultCode == RESULT_OK) {
            // We have returned from signing in - reload the web view
            getWebView().reload();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                startSignInActivity();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signOut() {
        if (myWarwick.getSsoUrls() != null) {
            getWebView().loadUrl(myWarwick.getSsoUrls().getLogoutUrl());
        }
    }

    private void startSignInActivity() {
        if (myWarwick.getSsoUrls() != null) {
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(WebViewActivity.EXTRA_URL, myWarwick.getSsoUrls().getLoginUrl());
            intent.putExtra(WebViewActivity.EXTRA_DESTINATION_HOST, myWarwickPreferences.getAppHost());
            intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.action_sign_in));
            startActivityForResult(intent, SIGN_IN);
        }
    }

    private WebView getWebView() {
        return (WebView) findViewById(R.id.web_view);
    }

    public String getPathForTabItem(int id) {
        switch (id) {
            case R.id.tab_me:
                return "/";
            case R.id.tab_notifications:
                return "/notifications";
            case R.id.tab_activity:
                return "/activity";
            case R.id.tab_news:
                return "/news";
            default:
                return "/";
        }
    }

    public int getTabItemForPath(String path) {
        switch (path) {
            case "/":
                return R.id.tab_me;
            case "/notifications":
                return R.id.tab_notifications;
            case "/activity":
                return R.id.tab_activity;
            case "/news":
                return R.id.tab_news;
            default:
                return R.id.tab_me;
        }
    }

    public String getTitleForPath(String path) {
        switch (path) {
            case "/notifications":
                return getString(R.string.notifications);
            case "/activity":
                return getString(R.string.activity);
            case "/news":
                return getString(R.string.news);
            default:
                return getString(R.string.app_name);
        }
    }
}
