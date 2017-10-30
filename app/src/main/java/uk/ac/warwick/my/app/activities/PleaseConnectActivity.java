package uk.ac.warwick.my.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import uk.ac.warwick.my.app.R;

/**
 * Shows a message instructing the user to connect to the Internet,
 * and will return to the MainActivity when a connection is detected.
 */
public class PleaseConnectActivity extends AppCompatActivity {

    private static final String TAG = "PleaseConnectActivity";

    private ConnectivityManager connectivityManager;

    private BroadcastReceiver connectivity = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            checkConnectivity();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_please_connect);
        registerReceiver(connectivity, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        connectivityManager = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // we might be connected already...
        checkConnectivity();
    }

    private void checkConnectivity() {
        NetworkInfo netinfo = connectivityManager.getActiveNetworkInfo();
        if (netinfo != null && netinfo.isConnected()) {
            Log.i(TAG, "Looks like we're connected");
            returnToMain();
        }
    }

    private void returnToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectivity);
    }
}
