package uk.ac.warwick.my.app.bridge;

import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

public interface MyWarwickListener {

    void onPathChange(String currentPath);

    void onUnreadNotificationCountChange(int count);

    void onUserChange(User user);

    void onSetSsoUrls(SsoUrls ssoUrls);

}
