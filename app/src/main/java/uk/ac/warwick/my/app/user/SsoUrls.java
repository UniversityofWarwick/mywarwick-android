package uk.ac.warwick.my.app.user;

public class SsoUrls {

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

        SsoUrls ssoUrls = (SsoUrls) o;

        if (!loginUrl.equals(ssoUrls.loginUrl)) return false;
        return logoutUrl.equals(ssoUrls.logoutUrl);

    }
}
