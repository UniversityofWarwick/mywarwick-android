package uk.ac.warwick.start.app.bridge;

import uk.ac.warwick.start.app.user.User;

public interface StartListener {

    void onPathChange(String currentPath);

    void onUnreadNotificationCountChange(int count);

    void onUserChange(User user);

}
