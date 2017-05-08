package uk.ac.warwick.my.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import com.google.firebase.crash.FirebaseCrash;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabReselectListener;
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

public class MainActivity extends AppCompatActivity implements OnTabSelectListener, OnTabReselectListener, MyWarwickListener {

    public static final String ROOT_PATH = "/";
    public static final String EDIT_PATH = "/edit";
    public static final String ADD_PATH = EDIT_PATH + "/add";
    public static final String SEARCH_PATH = "/search";
    public static final String TILES_PATH = "/tiles";
    public static final String NOTIFICATIONS_PATH = "/notifications";
    public static final String ACTIVITY_PATH = "/activity";
    public static final String NEWS_PATH = "/news";

    public static final int TAB_INDEX_ACTIVITIES = 2;
    public static final int TAB_INDEX_NOTIFICATIONS = 1;

    public static final int SIGN_IN = 1;

    private static final int LOCATION_PERMISSION_REQUEST = 0;

    private static final String TAG = "MainActivity";

    public static final float disabledTabAlpha = 0.3f;
    public static final float enabledTabAlpha = 1;

    private WebView myWarwickWebView;
    private MyWarwickState myWarwick = new MyWarwickState(this);
    private MyWarwickPreferences myWarwickPreferences;
    private MenuItem searchItem;
    private BroadcastReceiver tokenRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            registerForPushNotifications();
        }
    };
    private JavascriptInvoker invoker;
    private MenuItem editMenuItem;

    @Override
    public void onTabSelected(@IdRes int tabId) {
        String path = getPathForTabItem(tabId);
        // Prevent double navigation (NEWSTART-500)
        if (!path.equals(myWarwick.getPath())) {
            appNavigate(path);
        } else {
            Log.d(TAG, "Not navigating to " + path + " because we're already on it.");
        }
    }

    @Override
    public void onTabReSelected(@IdRes int tabId) {
        String path = getPathForTabItem(tabId);
        appNavigate(path);
    }

    @Override
    public void onPathChange(final String path) {
        final String oldPath = myWarwick.getPath();
        FirebaseCrash.log("onPathChange: " + oldPath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(getTitleForPath(path));

                // Don't call listeners when the webview changes the tab
                BottomBar bottomBar = getBottomBar();
                bottomBar.setOnTabSelectListener(null, false);
                bottomBar.setOnTabReselectListener(null);
                // Only update the tab if the new path is on a different tab
                if (oldPath == null) {
                    bottomBar.selectTabWithId(getTabItemForPath(path));
                } else {
                    String oldTabPath = getPathForTabItem(getTabItemForPath(oldPath));
                    int newTabItem = getTabItemForPath(path);
                    String newTabPath = getPathForTabItem(newTabItem);
                    if (!oldTabPath.equals(newTabPath)) {
                        bottomBar.selectTabWithId(newTabItem);
                    }
                }
                bottomBar.setOnTabSelectListener(MainActivity.this, false);
                bottomBar.setOnTabReselectListener(MainActivity.this);

                ActionBar actionBar = getSupportActionBar();

                if (actionBar != null) {
                    if (path.startsWith(TILES_PATH) || path.startsWith(ADD_PATH)) {
                        // Display a back arrow in place of the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    } else {
                        // Restore the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(false);
                    }
                }

                if (path.equals(SEARCH_PATH) && searchItem != null) {
                    // Show the search field in the action bar on /search
                    MenuItemCompat.expandActionView(searchItem);
                }

                updateEditMenuItem(path);
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
    public void onUserChange(@Nullable final User user) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View accountPhotoView = getAccountPhotoView();
                View cardView = accountPhotoView.findViewById(R.id.image_card_view);
                ImageView photoView = (ImageView) accountPhotoView.findViewById(R.id.image_view);

                final boolean signedIn = (user != null && user.isSignedIn());

                if (signedIn) {
                    new DownloadImageTask(photoView, cardView).execute(user.getPhotoUrl());

                    if (user.isAuthoritative()) {
                        registerForPushNotifications();
                    }
                } else {
                    photoView.setImageURI(null);
                    cardView.setVisibility(View.GONE);

                    if (user != null && user.isAuthoritative()) {
                        unregisterForPushNotifications();
                    }
                }

                BottomBar bottomBar = getBottomBar();

                BottomBarTab notificationsTab = bottomBar.getTabAtPosition(TAB_INDEX_NOTIFICATIONS);
                BottomBarTab activitiesTab = bottomBar.getTabAtPosition(TAB_INDEX_ACTIVITIES);
                notificationsTab.setEnabled(signedIn);
                activitiesTab.setEnabled(signedIn);

                if (!signedIn) {
                    notificationsTab.setAlpha(disabledTabAlpha);
                    activitiesTab.setAlpha(disabledTabAlpha);
                } else {
                    notificationsTab.setAlpha(enabledTabAlpha);
                    activitiesTab.setAlpha(enabledTabAlpha);
                }

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

    @Override
    public void onUncachedPageFail() {
        Intent intent = new Intent(this, PleaseConnectActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    public boolean onSsoUrl(Uri url) {
        if (SsoUrls.isLoginRefresh(url)) {
            return false;
        } else if (myWarwick.getSsoUrls() != null &&
                myWarwick.getSsoUrls().getLogoutUrl() != null &&
                url.toString().equals(myWarwick.getSsoUrls().getLogoutUrl())
                ) {
            return false;
        } else {
            startSignInActivity(url.toString());
            return true;
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myWarwickWebView = getWebView();
        WebSettings settings = myWarwickWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + getString(R.string.user_agent));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isDebugBuild()) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        this.myWarwickPreferences = new MyWarwickPreferences(PreferenceManager.getDefaultSharedPreferences(this));
        this.invoker = new JavascriptInvoker(myWarwickWebView);
        MyWarwickJavaScriptInterface javascriptInterface = new MyWarwickJavaScriptInterface(invoker, myWarwick);
        myWarwickWebView.addJavascriptInterface(javascriptInterface, "MyWarwickAndroid");

        MyWarwickWebViewClient webViewClient = new MyWarwickWebViewClient(myWarwickPreferences, this);
        myWarwickWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Allow geolocation permission for any web page rendering inside this web view, upon request
                Log.d(TAG, "Allowed geolocation permission for " + origin);
                callback.invoke(origin, true, false);
            }
        });
        myWarwickWebView.setWebViewClient(webViewClient);

        getBottomBar().setOnTabSelectListener(this, false);
        getBottomBar().setOnTabReselectListener(this);

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

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationPermissionsDialog();
        } else {
            requestLocationPermissions();
        }

        if (isOpenedFromNotification()) {
            onPathChange(NOTIFICATIONS_PATH);
            FirebaseCrash.log("loadUrl: " + appURL + NOTIFICATIONS_PATH);
            myWarwickWebView.loadUrl(appURL + NOTIFICATIONS_PATH);
        } else {
            FirebaseCrash.log("loadUrl: " + appURL);
            myWarwickWebView.loadUrl(appURL);
        }

        registerTokenRefreshReceiver();
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted");
                } else {
                    Log.d(TAG, "Permission denied");
                }
                break;
        }
    }

    private void showLocationPermissionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.locationDialogTitle);
        builder.setMessage(R.string.locationDialogMessage);

        String positiveText = getString(R.string.allow);
        builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestLocationPermissions();
            }
        });

        String negativeText = getString(R.string.deny);
        builder.setNegativeButton(negativeText, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void registerTokenRefreshReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PushNotifications.TOKEN_REFRESH);

        registerReceiver(tokenRefreshReceiver, intentFilter);
    }

    private void registerForPushNotifications() {
        User user = myWarwick.getUser();

        if (user == null || !user.isSignedIn() || !user.isAuthoritative()) {
            // Only do this for definitely signed-in users
            return;
        }

        String token = PushNotifications.getToken();

        if (token == null) {
            // The token might not have been generated yet
            return;
        }

        myWarwickPreferences.setPushNotificationToken(token);

        Log.i(TAG, "Registering for push notifications with token " + token);

        invoker.invokeMyWarwickMethod(String.format("registerForFCM('%s')", token));
    }

    private void unregisterForPushNotifications() {
        String token = myWarwickPreferences.getPushNotificationToken();

        if (token == null) {
            // Nothing to do
            return;
        }

        Log.i(TAG, "Unregistering push notification token " + token);

        invoker.invokeMyWarwickMethod(String.format("unregisterForPush('%s')", token));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(tokenRefreshReceiver);
        invoker.reset();
        invoker.clear();
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");

        if (myWarwickPreferences.needsReload()) {
            Log.i(TAG, "Reloading because something has changed");
            getWebView().reload();
            invoker.reset();
            myWarwickPreferences.setNeedsReload(false);
        } else {
            // Let the embedded app know that it's being brought to the foreground
            // (Only need to do this if we didn't just reload the whole page)
            invoker.invokeMyWarwickMethod("onApplicationDidBecomeActive()");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
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
        if (myWarwickWebView.canGoBack()) {
            // Go back from editing, or zoomed tile, to tiles view
            myWarwickWebView.goBack();
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
            Log.d(TAG, "Generating options menu, user signed in: " + myWarwick.getUser().isSignedIn());
            getMenuInflater().inflate(myWarwick.getUser().isSignedIn() ? R.menu.signed_in : R.menu.signed_out, menu);
        } else {
            // We don't know anything about anything
            // Render the signed_in version (which won't have the account pic), so that Settings are always available.
            getMenuInflater().inflate(R.menu.signed_in, menu);
        }

        // NEWSTART-540 - When we add settings for non-debug stuff, we'll need
        // to revert this and hide the individual settings items instead.
        menu.findItem(R.id.action_settings).setVisible(isDebugBuild());

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

        editMenuItem = menu.findItem(R.id.action_edit);
        updateEditMenuItem(myWarwick.getPath());

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
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                if (myWarwick.getSsoUrls() != null && myWarwick.getSsoUrls().getLoginUrl() != null) {
                    startSignInActivity(myWarwick.getSsoUrls().getLoginUrl());
                }
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_edit:
                if (myWarwick.getPath().equals(ROOT_PATH)) {
                    appNavigate(EDIT_PATH);
                } else {
                    appNavigate(ROOT_PATH);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signOut() {
        if (myWarwick.getSsoUrls() != null) {
            FirebaseCrash.log("loadUrl: " + myWarwick.getSsoUrls().getLogoutUrl());
            getWebView().loadUrl(myWarwick.getSsoUrls().getLogoutUrl());

            myWarwick.setUser(null);
            myWarwickPreferences.setNeedsReload(true);
        }
    }

    private void startSignInActivity(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        intent.putExtra(WebViewActivity.EXTRA_DESTINATION_HOST, myWarwickPreferences.getAppHost());
        intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.action_sign_in));
        startActivityForResult(intent, SIGN_IN);

        myWarwick.setUser(null);
        myWarwickPreferences.setNeedsReload(true);
    }

    private void updateEditMenuItem(String path) {
        if (editMenuItem != null) {
            editMenuItem.setVisible(ROOT_PATH.equals(path) || EDIT_PATH.equals(path));

            if (ROOT_PATH.equals(path)) {
                editMenuItem.setIcon(R.drawable.ic_mode_edit_white);
            } else {
                editMenuItem.setIcon(R.drawable.ic_done_white);
            }
        }
    }

    private WebView getWebView() {
        return (WebView) findViewById(R.id.web_view);
    }

    public String getPathForTabItem(int id) {
        switch (id) {
            case R.id.tab_me:
                return ROOT_PATH;
            case R.id.tab_notifications:
                return NOTIFICATIONS_PATH;
            case R.id.tab_activity:
                return ACTIVITY_PATH;
            case R.id.tab_news:
                return NEWS_PATH;
            default:
                return ROOT_PATH;
        }
    }

    public int getTabItemForPath(String path) {
        switch (path) {
            case ROOT_PATH:
                return R.id.tab_me;
            case NOTIFICATIONS_PATH:
                return R.id.tab_notifications;
            case ACTIVITY_PATH:
                return R.id.tab_activity;
            case NEWS_PATH:
                return R.id.tab_news;
            default:
                return R.id.tab_me;
        }
    }

    public String getTitleForPath(String path) {
        switch (path) {
            case NOTIFICATIONS_PATH:
                return getString(R.string.notifications);
            case ACTIVITY_PATH:
                return getString(R.string.activity);
            case NEWS_PATH:
                return getString(R.string.news);
            default:
                return getString(R.string.app_name);
        }
    }

    private boolean isDebugBuild() {
        return 0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
    }
}
