package uk.ac.warwick.my.app.bridge;

import java.util.Collection;

import uk.ac.warwick.my.app.user.SsoUrls;
import uk.ac.warwick.my.app.user.User;

/**
 * A place to put all of My Warwick's state, that can notify an interested
 * listener when things change.
 */
public class MyWarwickState {

    private User user;
    private int unreadNotificationCount;
    private String path;
    private SsoUrls ssoUrls;
    private Collection<String> applicationOrigins;
    private String appHost;
    private MyWarwickListener listener;


    public MyWarwickState(MyWarwickListener listener, String appHost) {
        this.appHost = appHost;
        this.listener = listener;
    }

    public String getAppHost() {
        return appHost;
    }

    public SsoUrls getSsoUrls() {
        return ssoUrls;
    }

    public void setSsoUrls(SsoUrls ssoUrls) {
        this.ssoUrls = ssoUrls;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (listener != null && !path.equals(this.path)) {
            listener.onPathChange(path);
        }

        this.path = path;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        if (listener != null && !user.equals(this.user)) {
            listener.onUserChange(user);
        }

        this.user = user;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(int count) {
        if (listener != null && count != this.unreadNotificationCount) {
            listener.onUnreadNotificationCountChange(count);
        }

        this.unreadNotificationCount = count;
    }

    public Collection<String> getApplicationOrigins() {
        return applicationOrigins;
    }

    public void setApplicationOrigins(Collection<String> applicationOrigins) {
        this.applicationOrigins = applicationOrigins;
    }

    public boolean isApplicationOrigin(String url) {
        if (applicationOrigins == null) {
            return true;
        }

        for (String origin : applicationOrigins) {
            if (url.startsWith(origin)) {
                return true;
            }
        }

        return false;
    }
}
