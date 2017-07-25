package uk.ac.warwick.my.app.bridge;


import android.net.Uri;

import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

public interface MyWarwickListener {

    void onPathChange(String currentPath);

    void onUnreadNotificationCountChange(int count);

    void onUserChange(User user);

    void onSetSsoUrls(SsoUrls ssoUrls);

    void onBackgroundChange(int newBgId);

    // Call when the HTML fails to load because we haven't cached anything yet.
    void onUncachedPageFail();

    boolean onSsoUrl(Uri url);

}
