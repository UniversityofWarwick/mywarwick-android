package uk.ac.warwick.my.app.activities;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.InstanceIdResult;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.warwick.my.app.BuildConfig;
import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.bridge.JavascriptInvoker;
import uk.ac.warwick.my.app.bridge.MyWarwickFeatures;
import uk.ac.warwick.my.app.bridge.MyWarwickJavaScriptInterface;
import uk.ac.warwick.my.app.bridge.MyWarwickListener;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.bridge.MyWarwickState;
import uk.ac.warwick.my.app.bridge.MyWarwickWebViewClient;
import uk.ac.warwick.my.app.data.EventDao;
import uk.ac.warwick.my.app.services.EventFetcher;
import uk.ac.warwick.my.app.services.PushRegistrationAPI;
import uk.ac.warwick.my.app.user.AnonymousUser;
import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;
import uk.ac.warwick.my.app.utils.DownloadImageTask;
import uk.ac.warwick.my.app.utils.PushNotifications;

import static uk.ac.warwick.my.app.services.EventNotificationService.NOTIFICATION_ID;
import static uk.ac.warwick.my.app.services.NotificationChannelsService.buildNotificationChannels;

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
    private JavascriptInvoker invoker;
    private MenuItem editMenuItem;
    private MenuItem settingsMenuItem;
    private FirebaseAnalytics firebaseAnalytics;

    private boolean firstRunAfterTour = false;
    private int themePrimaryColour;
    private CustomTabsSession customTabsSession;
    private ScheduledExecutorService timetableEventUpdateScheduler;
    private AlertDialog locationPermissionsDialog;

    private CustomTabsServiceConnection tabsConnection;

    private void startTimetableEventUpdateTimer() {
        if (timetableEventUpdateScheduler == null) {
            timetableEventUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
            timetableEventUpdateScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Starting timetable event update.");
                    new EventFetcher(getApplicationContext()).updateEvents();
                }
            }, 0, 60, TimeUnit.SECONDS);
        }
    }
    private void stopTimetableEventUpdateTimer() {
        Log.d(TAG, "Stopping timetable event update timer.");
        this.timetableEventUpdateScheduler.shutdown();
        this.timetableEventUpdateScheduler = null;
    }

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
            Crashlytics.log("onPathChange: " + oldPath);
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
                    bottomBar.removeOnTabSelectListener();
                    bottomBar.removeOnTabReselectListener();
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
                    if (path.matches("^/.+/.+") || path.startsWith(SETTINGS_PATH) || (path.startsWith(EDIT_PATH) && preferences.featureEnabled(MyWarwickFeatures.EDIT_TILES_BTN))) {
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

    /**
     * Called whenever the user changes in the known state. This will include initial
     * startup, as the app launches not knowing anything about the user
     */
    @Override
    public void onUserChange(@Nullable final User user) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View accountPhotoView = getAccountPhotoView();
                View cardView = accountPhotoView.findViewById(R.id.image_card_view);
                ImageView photoView = accountPhotoView.findViewById(R.id.image_view);

                final boolean signedIn = (user != null && user.isSignedIn());

                if (signedIn) {
                    new DownloadImageTask(photoView, cardView).execute(user.getPhotoUrl());

                    if (user.isAuthoritative()) {
                        registerForPushNotifications();

                        if (preferences.getTimetableToken() == null) {
                            registerForTimetable();
                        } else if (preferences.isNeedsTimetableTokenRefresh()) {
                            Log.d(TAG, "Refreshing timetable token");
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
                ImageView imageView = findViewById(R.id.background);
                imageView.setImageDrawable(new ColorDrawable(getColourForTheme(bgId)));
            }
        });
    }

    private void updateBackgroundDisplayed(final int newBgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = findViewById(R.id.background);
                Context ctx = imageView.getContext();
                int resourceIdentifier = ctx.getResources().getIdentifier(String.format(Locale.ROOT,"bg%02d", newBgId), "drawable", ctx.getPackageName());
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
            case 6:
                return getResources().getColor(R.color.colorPrimary6);
            case 7:
                return getResources().getColor(R.color.colorPrimary7);
            case 8:
                return getResources().getColor(R.color.colorPrimary8);
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
            case 6:
                return getResources().getColor(R.color.colorPrimaryDark6);
            case 7:
                return getResources().getColor(R.color.colorPrimaryDark7);
            case 8:
                return getResources().getColor(R.color.colorPrimaryDark8);
            default:
                return getResources().getColor(R.color.colorPrimaryDark1);
        }
    }

    private void setEdgeEffectColour(WebView webView, int colour) {
        try {
            // com.android.webview.chromium.WebViewChromium
            @SuppressLint("PrivateApi")
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
        Toolbar toolbar = findViewById(R.id.toolbar);
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
            @SuppressLint("InflateParams") // we need to pass layout params, and have no access to the root.
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

        // TODO the permissions ask is asynchronous so the page loads anyway,
        // and might start the login page moments after we've asked for permission.
        // It still works, just looks a bit jarring.

        if (preferences.isTourComplete()) {
            loadWebView(getIntent(), true);
        } else {
            Intent intent = new Intent(this, TourActivity.class);

            startActivityForResult(intent, TOUR);
        }

        updateBackgroundDisplayed(preferences.getBackgroundChoice());
        updateThemeColours(preferences.getBackgroundChoice());
    }

    /**
     * Load a path within the webview based on flags and intents.
     *
     * @param firstLoad whether to load the Me view if nothing else matches, or do nothing.
     */
    private void loadWebView(Intent intent, boolean firstLoad) {
        if (firstRunAfterTour) {
            firstRunAfterTour = false;
            loadPath(POST_TOUR_PATH);
        } else if (isOpenedFromNotification(intent)) {
            loadPath(NOTIFICATIONS_PATH);
        } else if (isOpenedFromSettingsUrl(intent)) {
            loadPath(SETTINGS_PATH);
        } else {
            handleShortcuts(intent, firstLoad);
        }
    }

    private void handleShortcuts(Intent intent, boolean firstLoad) {
        String action = intent.getAction();

        if (ALERTS_SHORTCUT_ACTION.equals(action)) {
            loadPath(NOTIFICATIONS_PATH);
        } else if (ACTIVITY_SHORTCUT_ACTION.equals(action)) {
            loadPath(ACTIVITY_PATH);
        } else if (SEARCH_SHORTCUT_ACTION.equals(action)) {
            loadPath(SEARCH_PATH);
        } else if (firstLoad) {
            loadPath(ROOT_PATH);
        }
    }

    private void loadPath(String path) {
        String root = preferences.getAppURL();

        if (!path.equals(ROOT_PATH)) {
            onPathChange(path);
        }

        try {
            Crashlytics.log("loadUrl: " + root + path);
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
        if (locationPermissionsDialog == null) {
            locationPermissionsDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.location_dialog_title)
                    .setMessage(R.string.location_dialog_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.okay_ask, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            requestLocationPermissions();
                        }
                    }).create();
        }
        locationPermissionsDialog.show();
    }

    private void dismissLocationPermissionDialog() {
        if (locationPermissionsDialog != null && locationPermissionsDialog.isShowing()) {
            locationPermissionsDialog.dismiss();
        }
    }

    private void registerForPushNotifications() {
        User user = myWarwick.getUser();

        if (user == null || !user.isSignedIn() || !user.isAuthoritative()) {
            // Only do this for definitely signed-in users
            return;
        }

        PushNotifications.getToken(this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();

                preferences.setPushNotificationToken(token);

                Log.i(TAG, "Registering for push notifications with token " + token);

                invoker.invokeMyWarwickMethod(String.format("registerForFCM('%s')", token));
            }
        });
    }

    private void unregisterForPushNotifications() {
        String token = preferences.getPushNotificationToken();

        if (token == null) {
            // Nothing to do
            return;
        }

        Log.i(TAG, "Unregistering push notification token " + token);

        new PushRegistrationAPI(preferences).unregister(token);
    }

    @Override
    protected void onDestroy() {
        dismissLocationPermissionDialog();
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

        cancelNotificationFromIntent(getIntent());

        startTimetableEventUpdateTimer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildNotificationChannels((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");

        cancelNotificationFromIntent(intent);

        // Handles intents from notifications etc. if MainActivity was already running
        // (Otherwise handled in onCreate)
        loadWebView(intent, false);
    }

    private void cancelNotificationFromIntent(Intent intent) {
        int id = intent.getIntExtra(NOTIFICATION_ID, -1);

        if (id != -1) {
            getNotificationManager().cancel(id);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

            // Refresh token when opening backgrounded app.
            // Fresh app load is handled in onUserChange.
            if (myWarwick.isUserSignedIn() && preferences.isNeedsTimetableTokenRefresh()) {
                Log.d(TAG, "Refreshing timetable token");
                registerForTimetable();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimetableEventUpdateTimer();
        Log.d(TAG, "onStop");

        // matches init call in onStart
        deinitCustomTabs();
    }

    private boolean isOpenedFromNotification(Intent intent) {
        Bundle extras = intent.getExtras();

        return extras != null && extras.containsKey("from");
    }

    private boolean isOpenedFromSettingsUrl(Intent intent) {
        Uri data = intent.getData();
        return data != null && SETTINGS_PATH.equals(data.getPath());
    }

    @NonNull
    private View getAccountPhotoView() {
        if (getSupportActionBar() != null) {
            return getSupportActionBar().getCustomView();
        } else {
            throw new IllegalStateException("support action bar missing");
        }
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
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        if (requestCode == TOUR) {
            preferences.setTourComplete();
            firstRunAfterTour = true;

            // maybe `data` is the correct intent, but in practice we hit the
            // `firstRunAfterTour` case first which doesn't care about the intent anyway.
            loadWebView(getIntent(), true);
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
                } else if (preferences.featureEnabled(MyWarwickFeatures.EDIT_TILES_BTN)){
                    appNavigate(SETTINGS_PATH);
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

        myWarwick.setUser(new AnonymousUser(true));
        preferences.setNeedsReload(true);
    }

    private void updateEditMenuItem(String path) {
        if (editMenuItem != null) {
            boolean isRemoveEditBtnFeature = preferences.featureEnabled(MyWarwickFeatures.EDIT_TILES_BTN);
            editMenuItem.setVisible(!isRemoveEditBtnFeature && (ROOT_PATH.equals(path) || EDIT_PATH.equals(path)));

            if (ROOT_PATH.equals(path) || NOTIFICATIONS_PATH.equals(path)) {
                if (!isRemoveEditBtnFeature) {
                    editMenuItem.setIcon(R.drawable.ic_mode_edit_white);
                }
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
            if (path != null && path.startsWith(EDIT_PATH) && preferences.featureEnabled(MyWarwickFeatures.EDIT_TILES_BTN)) {
                settingsMenuItem.setVisible(false);
            } else {
                settingsMenuItem.setVisible(path == null || !path.startsWith(SETTINGS_PATH));
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
            case NEWS_PATH:
                return R.id.tab_news;
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

        PackageManager pm = getApplicationContext().getPackageManager();
        String packageToUse = Global.CUSTOM_TAB_PACKAGE_NAME_FALLBACK;

        // Use a representative URL to work out which packages are capable of opening a typical tab
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://warwick.ac.uk"));

        // Does the user have a default browser?
        String defaultViewHandlerPackageName = null;
        ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
        }

        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            // Check if this package also resolves the Custom Tabs service.
            if (pm.resolveService(serviceIntent, 0) != null) {
                // Great, add it to the list
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }

        if (!packagesSupportingCustomTabs.isEmpty()) {
            // prefer the user's default browser if it supports custom tabs
            if (packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
                packageToUse = defaultViewHandlerPackageName;
            } else {
                // arbitrarily pick the first one
                packageToUse = packagesSupportingCustomTabs.get(0);
            }
        }

        CustomTabsClient.bindCustomTabsService(this, packageToUse, tabsConnection);
    }

    public CustomTabsSession getCustomTabsSession() {
        return customTabsSession;
    }
}
