package uk.ac.warwick.my.app.activities;

import android.app.Activity;
import android.content.Intent;
import androidx.test.rule.ActivityTestRule;

/**
 * Adds a "from" extra data to the loading intent, which is enough to
 * trigger the "open straight into Notifications" behaviour.
 */
class NotificationIntentRule<T extends Activity> extends ActivityTestRule<T> {
    public NotificationIntentRule(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected Intent getActivityIntent() {
        return super.getActivityIntent().putExtra("from", "a_notification");
    }
}
