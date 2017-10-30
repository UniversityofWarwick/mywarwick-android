package uk.ac.warwick.my.app.activities;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import uk.ac.warwick.my.app.Global;
import uk.ac.warwick.my.app.R;
import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences sharedPreferences;
        private MyWarwickPreferences myWarwickPreferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            sharedPreferences = getPreferenceScreen().getSharedPreferences();
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            myWarwickPreferences = new MyWarwickPreferences(getActivity(), sharedPreferences);
            this.enableCustomAppHostTextFieldIfDesired(sharedPreferences);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case "mywarwick_server":
                    Log.d(Global.TAG, "Target server has changed, signalling a reload is required");
                    myWarwickPreferences.setNeedsReload(true);
                    this.enableCustomAppHostTextFieldIfDesired(sharedPreferences);
                    break;
                case "custom_server_address":
                    Log.d(Global.TAG, "Custom server has changed, signalling a reload is required");
                    myWarwickPreferences.setNeedsReload(true);
                    break;
            }
        }

        private void enableCustomAppHostTextFieldIfDesired(SharedPreferences sharedPreferences) {
            getPreferenceScreen().findPreference("custom_server_address").setEnabled(
                            sharedPreferences.getString("mywarwick_server", "").equals("__custom__")
                    );
        }
    }
}
