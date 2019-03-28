package uk.ac.warwick.my.app;

public class Global {

    // Logging tag
    public static final String TAG = "MyWarwick";

    private Global() {
        throw new IllegalStateException("Private constructor");
    }

    public static String getWebSignOnHost() {
        return "websignon.warwick.ac.uk";
    }
    public static final String CUSTOM_TAB_PACKAGE_NAME_FALLBACK = "com.android.chrome";

}
