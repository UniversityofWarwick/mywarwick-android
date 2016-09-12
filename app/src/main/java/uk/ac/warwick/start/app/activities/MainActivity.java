package uk.ac.warwick.start.app.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import uk.ac.warwick.start.app.Global;
import uk.ac.warwick.start.app.R;
import uk.ac.warwick.start.app.bridge.StartListener;
import uk.ac.warwick.start.app.bridge.StartState;
import uk.ac.warwick.start.app.bridge.StartWebViewClient;
import uk.ac.warwick.start.app.user.User;
import uk.ac.warwick.start.app.utils.DownloadImageTask;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener, StartListener {

    public static final int ITEM_NOTIFICATIONS = 1;
    public static final int ITEM_ACTIVITIES = 2;
    public static final String ROOT_PATH = "/";

    public static final int SIGN_IN = 1;

    private ActionBarDrawerToggle toggle;
    private StartState start = new StartState(this);
    private MenuItem searchItem;

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        DrawerLayout drawer = getDrawer();
        drawer.closeDrawer(GravityCompat.START);

        startNavigate(getPathForNavigationItem(item.getItemId()));

        return true;
    }

    @Override
    public void onPathChange(String path) {
        NavigationView navigationView = getNavigationView();
        navigationView.setCheckedItem(getNavigationItemForPath(path));

        setTitle(getTitleForPath(path));

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            if (path.startsWith("/tiles")) {
                // Display a back arrow in place of the drawer indicator
                toggle.setDrawerIndicatorEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                // Restore the drawer indicator
                actionBar.setDisplayHomeAsUpEnabled(false);
                toggle.setDrawerIndicatorEnabled(true);
            }
        }

        if (path.equals("/search") && searchItem != null) {
            // Show the search field in the action bar on /search
            MenuItemCompat.expandActionView(searchItem);
        }
    }

    @Override
    public void onUserChange(User user) {
        NavigationView navigationView = getNavigationView();

        ImageView photoView = (ImageView) navigationView.findViewById(R.id.image_view);
        TextView primaryNameView = (TextView) navigationView.findViewById(R.id.primary_name_view);
        TextView secondaryNameView = (TextView) navigationView.findViewById(R.id.secondary_name_view);

        Menu drawerMenu = getNavigationView().getMenu();

        if (user.isSignedIn()) {
            primaryNameView.setText(user.getName());
            secondaryNameView.setText(user.getUsercode());

            new DownloadImageTask(photoView).execute(user.getPhotoUrl());

            drawerMenu.getItem(ITEM_NOTIFICATIONS).setEnabled(true);
            drawerMenu.getItem(ITEM_ACTIVITIES).setEnabled(true);
        } else {
            primaryNameView.setText(null);
            secondaryNameView.setText(null);

            photoView.setImageURI(null);

            drawerMenu.getItem(ITEM_NOTIFICATIONS).setEnabled(false);
            drawerMenu.getItem(ITEM_ACTIVITIES).setEnabled(false);
        }

        // Cause the options menu to be updated to reflect the signed in/out state
        supportInvalidateOptionsMenu();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = getDrawer();
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        NavigationView navigationView = getNavigationView();
        navigationView.setNavigationItemSelectedListener(this);

        WebView webView = getWebView();
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + getString(R.string.user_agent));

        StartWebViewClient webViewClient = new StartWebViewClient(start);
        webView.setWebViewClient(webViewClient);

        webView.loadUrl("https://" + Global.getStartHost());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onStart() {
        super.onStart();

        // Let the embedded app know that it's being brought to the foreground
        getWebView().evaluateJavascript("Start.appToForeground()", null);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = getDrawer();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            // If the drawer is open, close it
            drawer.closeDrawer(GravityCompat.START);
        } else if (start.getPath().startsWith("/tiles")) {
            // If we are looking at zoomed tile, unzoom it
            startNavigate(ROOT_PATH);
        } else {
            // Otherwise do the default thing
            super.onBackPressed();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void startNavigate(String path) {
        getWebView().evaluateJavascript(String.format("Start.navigate('%s')", path), null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void startSearch(String query) {
        getWebView().evaluateJavascript(String.format("Start.search('%s')", query.replace("'", "\\'")), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (start.getSsoUrls() != null) {
            getMenuInflater().inflate(start.getUser().isSignedIn() ? R.menu.signed_in : R.menu.signed_out, menu);
        }

        searchItem = menu.findItem(R.id.action_search);

        if (searchItem != null) {
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setQueryHint(getString(R.string.search_warwick));

            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // When pressing the back arrow in the search field, go back to /
                    startNavigate(ROOT_PATH);
                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    startSearch(query);
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
                if (start.getSsoUrls() != null) {
                    Intent intent = new Intent(this, WebViewActivity.class);
                    intent.putExtra(WebViewActivity.EXTRA_URL, start.getSsoUrls().getLoginUrl());
                    intent.putExtra(WebViewActivity.EXTRA_DESTINATION_HOST, Global.getStartHost());
                    intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.action_sign_in));
                    startActivityForResult(intent, SIGN_IN);
                }
                return true;
            case R.id.action_sign_out:
                if (start.getSsoUrls() != null) {
                    getWebView().loadUrl(start.getSsoUrls().getLogoutUrl());
                }
                return true;
            case R.id.action_account_settings:
                Intent intent = new Intent(this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.EXTRA_URL, "https://" + Global.getWebSignOnHost() + "/origin/account");
                intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.its_account));
                startActivity(intent);
                return true;
            case R.id.action_reload:
                getWebView().reload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    private NavigationView getNavigationView() {
        return (NavigationView) findViewById(R.id.nav_view);
    }

    private WebView getWebView() {
        return (WebView) findViewById(R.id.web_view);
    }

    public String getPathForNavigationItem(int id) {
        switch (id) {
            case R.id.nav_me:
                return "/";
            case R.id.nav_notifications:
                return "/notifications";
            case R.id.nav_activity:
                return "/activity";
            case R.id.nav_news:
                return "/news";
            case R.id.nav_search:
                return "/search";
            default:
                return "/";
        }
    }

    public int getNavigationItemForPath(String path) {
        switch (path) {
            case "/":
                return R.id.nav_me;
            case "/notifications":
                return R.id.nav_notifications;
            case "/activity":
                return R.id.nav_activity;
            case "/news":
                return R.id.nav_news;
            case "/search":
                return R.id.nav_search;
            default:
                return R.id.nav_me;
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
