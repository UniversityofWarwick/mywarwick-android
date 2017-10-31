package uk.ac.warwick.my.app.activities;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.EdgeEffect;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.bridge.JavascriptInvoker;
import uk.ac.warwick.my.app.bridge.MyWarwickJavaScriptInterface;
import uk.ac.warwick.my.app.bridge.MyWarwickListener;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.bridge.MyWarwickState;
import uk.ac.warwick.my.app.bridge.MyWarwickWebViewClient;
import uk.ac.warwick.my.app.data.EventDao;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;
import uk.ac.warwick.my.app.utils.DownloadImageTask;
import uk.ac.warwick.my.app.utils.PushNotifications;

public class MainActivity extends AppCompatActivity implements OnTabSelectListener, OnTabReselectListener, MyWarwickListener {

    public static final String ROOT_PATH = "/";
    public static final String EDIT_PATH = "/edit";
    public static final String SEARCH_PATH = "/search";
    public static final String NOTIFICATIONS_PATH = "/notifications";
    public static final String ACTIVITY_PATH = "/activity";
    public static final String NEWS_PATH = "/news";
    public static final String SETTINGS_PATH = "/settings";
    public static final String POST_TOUR_PATH = "/post_tour";
    public static final String ALERTS_SHORTCUT_ACTION = "uk.ac.warwick.my.app.SHORTCUT_ALERTS";
    public static final String ACTIVITY_SHORTCUT_ACTION = "uk.ac.warwick.my.app.SHORTCUT_ACTIVITY";
    public static final String SEARCH_SHORTCUT_ACTION = "uk.ac.warwick.my.app.SHORTCUT_SEARCH";

    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

    public static final int TAB_INDEX_ACTIVITIES = 2;

    public static final int TAB_INDEX_NOTIFICATIONS = 1;
    public static final int SIGN_IN = 1;

    public static final int TOUR = 2;

    private static final int LOCATION_PERMISSION_REQUEST = 0;

    private static final String TAG = "MainActivity";
    public static final float disabledTabAlpha = 0.3f;
    public static final float enabledTabAlpha = 1;

    private WebView myWarwickWebView;
    private MyWarwickState myWarwick = new MyWarwickState(this, this);
    private MyWarwickPreferences preferences;
    private BroadcastReceiver tokenRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            registerForPushNotifications();
        }
    };
    private JavascriptInvoker invoker;
    private MenuItem editMenuItem;
    private MenuItem settingsMenuItem;
    private FirebaseAnalytics firebaseAnalytics;

    private boolean firstRunAfterTour = false;
    private int themePrimaryColour;
    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;

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
        try {
            FirebaseCrash.log("onPathChange: " + oldPath);
        } catch (IllegalStateException ignored) {
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(getTitleForPath(path));

                if (path.startsWith(SETTINGS_PATH)) {
                    BottomBar bottomBar = getBottomBar();
                    bottomBar.setVisibility(View.GONE);
                } else {
                    // Don't call listeners when the webview changes the tab
                    BottomBar bottomBar = getBottomBar();
                    bottomBar.setVisibility(View.VISIBLE);
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
                }

                ActionBar actionBar = getSupportActionBar();

                if (actionBar != null) {
                    if (path.matches("^/.+/.+") || path.startsWith(SETTINGS_PATH)) {
                        // Display a back arrow in place of the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    } else {
                        // Restore the drawer indicator
                        actionBar.setDisplayHomeAsUpEnabled(false);
                    }
                }

                updateEditMenuItem(path);
                updateSettingsMenuItem(path);
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

                        if (preferences.getTimetableToken() == null) {
                            registerForTimetable();
                        }
                    }
                } else {
                    photoView.setImageURI(null);
                    cardView.setVisibility(View.GONE);

                    if (user != null && user.isAuthoritative()) {
                        unregisterForPushNotifications();

                        preferences.setTimetableToken(null);
                        try (EventDao eventDao = new EventDao(getApplicationContext())) {
                            eventDao.deleteAll();
                        }
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

    private void registerForTimetable() {
        invoker.invokeMyWarwickMethodIfAvailable("registerForTimetable");
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
    public void onBackgroundChange(final int bgId, final boolean isHighContrast) {
        if (isHighContrast) {
            setImageViewToColor(bgId);
        } else {
            updateBackgroundDisplayed(bgId);
        }

        if (preferences.getBackgroundChoice() != bgId) {
            preferences.setBackgroundChoice(bgId);
            updateThemeColours(bgId);
        }

        if (preferences.getHighContrastChoice() != isHighContrast) {
            preferences.setHighContrastChoice(isHighContrast);
        }
    }

    private void setImageViewToColor(final @ColorInt int bgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.background);
                imageView.setImageDrawable(new ColorDrawable(getColourForTheme(bgId)));
            }
        });
    }

    private void updateBackgroundDisplayed(final int newBgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.background);
                Context ctx = imageView.getContext();
                int resourceIdentifier = ctx.getResources().getIdentifier(String.format("bg%02d", newBgId), "drawable", ctx.getPackageName());
                if (resourceIdentifier != 0) {
                    Glide.with(getApplicationContext()).asDrawable().load(resourceIdentifier).into(imageView);
                }
            }
        });
    }

    private void updateThemeColours(final int newId) {
        themePrimaryColour = getColourForTheme(newId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColourForTheme(newId)));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getDarkColourForTheme(newId));
                }

                getBottomBar().setActiveTabColor(getColourForTheme(newId));
                setEdgeEffectColour(myWarwickWebView, themePrimaryColour);
            }
        });
    }

    public int getColourForTheme(final int themeId) {
        switch (themeId) {
            case 2:
                return getResources().getColor(R.color.colorPrimary2);
            case 3:
                return getResources().getColor(R.color.colorPrimary3);
            case 4:
                return getResources().getColor(R.color.colorPrimary4);
            case 5:
                return getResources().getColor(R.color.colorPrimary5);
            default:
                return getResources().getColor(R.color.colorPrimary1);
        }
    }

    public int getDarkColourForTheme(final int themeId) {
        switch (themeId) {
            case 2:
                return getResources().getColor(R.color.colorPrimaryDark2);
            case 3:
                return getResources().getColor(R.color.colorPrimaryDark3);
            case 4:
                return getResources().getColor(R.color.colorPrimaryDark4);
            case 5:
                return getResources().getColor(R.color.colorPrimaryDark5);
            default:
                return getResources().getColor(R.color.colorPrimaryDark1);
        }
    }

    private void setEdgeEffectColour(WebView webView, int colour) {
        try {
            // com.android.webview.chromium.WebViewChromium
            Method getWebViewProvider = WebView.class.getDeclaredMethod("getWebViewProvider");
            if (!getWebViewProvider.isAccessible()) getWebViewProvider.setAccessible(true);
            Object provider = getWebViewProvider.invoke(webView, (Object[]) null);
            Method getViewDelegate = provider.getClass().getDeclaredMethod("getViewDelegate");
            if (!getViewDelegate.isAccessible()) getViewDelegate.setAccessible(true);
            Object delegate = getViewDelegate.invoke(provider, (Object[]) null);

            // org.chromium.android_webview.AwContents
            Field field = delegate.getClass().getDeclaredField("mAwContents");
            if (!field.isAccessible()) field.setAccessible(true);
            Object mAwContents = field.get(delegate);

            // org.chromium.android_webview.OverScrollGlow
            field = mAwContents.getClass().getDeclaredField("mOverScrollGlow");
            if (!field.isAccessible()) field.setAccessible(true);
            Object mOverScrollGlow = field.get(mAwContents);
            Class<?> OverScrollGlow = mOverScrollGlow.getClass();

            for (String name : new String[]{"mEdgeGlowTop", "mEdgeGlowBottom", "mEdgeGlowLeft", "mEdgeGlowRight"}) {
                field = OverScrollGlow.getDeclaredField(name);
                if (!field.isAccessible()) field.setAccessible(true);
                EdgeEffect edgeEffect = (EdgeEffect) field.get(mOverScrollGlow);
                setEdgeEffectColor(edgeEffect, colour);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private void setEdgeEffectColor(EdgeEffect edgeEffect, @ColorInt int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                edgeEffect.setColor(color);
                return;
            }
            for (String name : new String[]{"mEdge", "mGlow"}) {
                Field field = EdgeEffect.class.getDeclaredField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Drawable drawable = (Drawable) field.get(edgeEffect);
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawable.setCallback(null); // free up any references
            }
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }
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

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        myWarwickWebView = getWebView();
        myWarwickWebView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = myWarwickWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + getString(R.string.user_agent_prefix) + BuildConfig.VERSION_NAME);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isDebugBuild()) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        this.preferences = new MyWarwickPreferences(this);

        this.invoker = new JavascriptInvoker(myWarwickWebView);
        MyWarwickJavaScriptInterface javascriptInterface = new MyWarwickJavaScriptInterface(invoker, myWarwick, preferences);
        myWarwickWebView.addJavascriptInterface(javascriptInterface, "MyWarwickAndroid");

        MyWarwickWebViewClient webViewClient = new MyWarwickWebViewClient(preferences, this, this);
        myWarwickWebView.setWebChromeClient(new WebChromeClient() {
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
        }


        if (!hasLocationPermissions()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // User pressed DENY last time - lay on the explanation before we
                // ask them again.
                showLocationPermissionsDialog();
            } else {
                requestLocationPermissions();
            }
        }

        registerTokenRefreshReceiver();

        // TODO the permissions ask is asynchronous so the page loads anyway,
        // and might start the login page moments after we've asked for permission.
        // It still works, just looks a bit jarring.

        if (preferences.isTourComplete()) {
            loadWebView();
        } else {
            Intent intent = new Intent(this, TourActivity.class);

            startActivityForResult(intent, TOUR);
        }

        updateBackgroundDisplayed(preferences.getBackgroundChoice());
        updateThemeColours(preferences.getBackgroundChoice());
    }

    private void loadWebView() {
        if (firstRunAfterTour) {
            firstRunAfterTour = false;
            loadPath(POST_TOUR_PATH);
        } else if (isOpenedFromNotification()) {
            loadPath(NOTIFICATIONS_PATH);
        } else if (isOpenedFromSettingsUrl()) {
            loadPath(SETTINGS_PATH);
        } else {
            handleShortcuts();
        }
    }

    private void handleShortcuts() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (ALERTS_SHORTCUT_ACTION.equals(action)) {
            loadPath(NOTIFICATIONS_PATH);
        } else if (ACTIVITY_SHORTCUT_ACTION.equals(action)) {
            loadPath(ACTIVITY_PATH);
        } else if (SEARCH_SHORTCUT_ACTION.equals(action)) {
            loadPath(SEARCH_PATH);
        } else {
            loadPath(ROOT_PATH);
        }
    }

    private void loadPath(String path) {
        String root = preferences.getAppURL();

        if (!path.equals(ROOT_PATH)) {
            onPathChange(path);
        }

        try {
            FirebaseCrash.log("loadUrl: " + root + path);
        } catch (IllegalStateException ignored) {
        }
        myWarwickWebView.loadUrl(root + path);
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
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
                    Bundle bundle = new Bundle();
                    bundle.putString("permission", Manifest.permission.ACCESS_FINE_LOCATION);
                    firebaseAnalytics.logEvent("permission_denied", bundle);
                    Log.d(TAG, "Permission denied");
                }
                break;
        }
    }

    private void showLocationPermissionsDialog() {
        Bundle bundle = new Bundle();
        bundle.putString("permission", Manifest.permission.ACCESS_FINE_LOCATION);
        firebaseAnalytics.logEvent("permission_rationale", bundle);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.location_dialog_title)
                .setMessage(R.string.location_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.okay_ask, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        requestLocationPermissions();
                    }
                })
                .show();
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

        preferences.setPushNotificationToken(token);

        Log.i(TAG, "Registering for push notifications with token " + token);

        invoker.invokeMyWarwickMethod(String.format("registerForFCM('%s')", token));
    }

    private void unregisterForPushNotifications() {
        String token = preferences.getPushNotificationToken();

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

        initCustomTabs();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");

        if (preferences.needsReload()) {
            Log.i(TAG, "Reloading because something has changed");
            getWebView().reload();
            invoker.reset();
            preferences.setNeedsReload(false);
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

    private boolean isOpenedFromSettingsUrl() {
        Uri data = getIntent().getData();
        return data != null && SETTINGS_PATH.equals(data.getPath());
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
        menu.findItem(R.id.action_app_settings).setVisible(isDebugBuild());

        editMenuItem = menu.findItem(R.id.action_edit);
        updateEditMenuItem(myWarwick.getPath());
        settingsMenuItem = menu.findItem(R.id.action_settings);
        updateSettingsMenuItem(myWarwick.getPath());

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SIGN_IN && resultCode == RESULT_OK) {
            // We have returned from signing in - reload the web view
            getWebView().reload();
        }

        if (requestCode == TOUR) {
            preferences.setTourComplete();
            firstRunAfterTour = true;

            loadWebView();
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
            case R.id.action_app_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_edit:
                if (myWarwick.getPath().equals(ROOT_PATH)) {
                    appNavigate(EDIT_PATH);
                } else {
                    appNavigate(ROOT_PATH);
                }
                return true;
            case R.id.action_settings:
                appNavigate(SETTINGS_PATH);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startSignInActivity(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        intent.putExtra(WebViewActivity.EXTRA_DESTINATION_HOST, preferences.getAppHost());
        intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.action_sign_in));
        startActivityForResult(intent, SIGN_IN);

        myWarwick.setUser(null);
        preferences.setNeedsReload(true);
    }

    private void updateEditMenuItem(String path) {
        if (editMenuItem != null) {
            editMenuItem.setVisible(ROOT_PATH.equals(path) || EDIT_PATH.equals(path));

            if (ROOT_PATH.equals(path) || NOTIFICATIONS_PATH.equals(path)) {
                editMenuItem.setIcon(R.drawable.ic_mode_edit_white);
            } else {
                editMenuItem.setIcon(R.drawable.edit_button_layer);
                final int duration = 1000;
                LayerDrawable layer = (LayerDrawable) editMenuItem.getIcon();
                Drawable blueCircle = layer.getDrawable(0);
                ObjectAnimator animatorOut = ObjectAnimator.ofPropertyValuesHolder(blueCircle, PropertyValuesHolder.ofInt("alpha", 0));
                animatorOut.setTarget(blueCircle);
                animatorOut.setDuration(duration);
                ObjectAnimator animatorIn = animatorOut.clone();
                animatorIn.setValues(PropertyValuesHolder.ofInt("alpha", 255));
                AnimatorSet s = new AnimatorSet();
                s.playSequentially(animatorOut, animatorIn, animatorOut.clone());
                s.start();
            }
        }
    }

    private void updateSettingsMenuItem(String path) {
        if (settingsMenuItem != null) {
            settingsMenuItem.setVisible(path == null || !path.startsWith(SETTINGS_PATH));
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
//            case R.id.tab_news:
//                return NEWS_PATH;
            case R.id.tab_search:
                return SEARCH_PATH;
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
//            case NEWS_PATH:
//                return R.id.tab_news;
            case SEARCH_PATH:
                return R.id.tab_search;
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
            case SEARCH_PATH:
                return "Search";
            default:
                return getString(R.string.app_name_title);
        }
    }

    private boolean isDebugBuild() {
        return 0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
    }

    public void launchTour() {
        Intent intent = new Intent(this, TourActivity.class);
        startActivityForResult(intent, TOUR);
    }

    public int getThemePrimaryColour() {
        return themePrimaryColour;
    }

    private void initCustomTabs() {
        CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                Log.d(TAG, "Custom Tabs service connected");
                customTabsClient = client;

                client.warmup(0);

                customTabsSession = client.newSession(new CustomTabsCallback());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, connection);
    }

    public CustomTabsSession getCustomTabsSession() {
        return customTabsSession;
    }
}
