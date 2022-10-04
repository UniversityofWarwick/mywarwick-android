package uk.ac.warwick.my.app.user;

import android.net.Uri;

import java.util.Objects;

public class SsoUrls {

    private static final String MY_WARWICK_REFRESH_PARAM = "myWarwickRefresh";

    public static boolean isLoginRefresh(Uri url) {
        return url.getBooleanQueryParameter(MY_WARWICK_REFRESH_PARAM, false);
    }

    private final String loginUrl;
    private final String logoutUrl;

    public SsoUrls(String loginUrl, String logoutUrl) {
        this.loginUrl = loginUrl;
        this.logoutUrl = logoutUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SsoUrls that = (SsoUrls) o;

        boolean loginUrlsEqual = Objects.equals(this.loginUrl, that.loginUrl);
        boolean logoutUrlsEqual = Objects.equals(this.logoutUrl, that.logoutUrl);

        return loginUrlsEqual && logoutUrlsEqual;
    }
}
