package uk.ac.warwick.my.app.activities;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.roughike.bottombar.BottomBar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.ac.warwick.my.app.R;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.Matchers.equalTo;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NotificationIntentTest {

    // Custom rule that passes in some intent data
    @Rule
    public NotificationIntentRule<MainActivity> mActivityTestRule = new NotificationIntentRule<>(MainActivity.class);

    @Test
    public void openNotifications() {
        // the important thing here is just that it hasn't exploded

        MainActivity mainActivity = mActivityTestRule.getActivity();
        View notificationsTab = mainActivity.findViewById(R.id.tab_notifications);
        BottomBar bottomBar = (BottomBar) mainActivity.findViewById(R.id.bottom_bar);
        assertThat(bottomBar.getCurrentTab(), equalTo(notificationsTab));

    }

}
