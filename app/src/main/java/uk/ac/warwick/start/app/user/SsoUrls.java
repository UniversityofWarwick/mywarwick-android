package uk.ac.warwick.start.app.user;

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

}
