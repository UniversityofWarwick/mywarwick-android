package uk.ac.warwick.my.app.bridge;

import uk.ac.warwick.my.app.user.User;

public interface MyWarwickListener {

    void onPathChange(String currentPath);

    void onUnreadNotificationCountChange(int count);

    void onUserChange(User user);

}
