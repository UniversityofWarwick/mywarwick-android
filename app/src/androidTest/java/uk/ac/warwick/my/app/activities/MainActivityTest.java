package uk.ac.warwick.my.app.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.web.model.Atoms;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.ac.warwick.my.app.CustomAtoms;
import uk.ac.warwick.my.app.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.web.assertion.WebViewAssertions.*;
import static android.support.test.espresso.web.sugar.Web.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.ac.warwick.my.app.CustomAtoms.*;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void scrollRememberingTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onWebView()
            .check(webMatches(Atoms.getTitle(), containsString("Warwick")))
            .perform(setScrollY(200));

        onViewWithId(R.id.tab_notifications)
            .perform(click());

        onViewWithId(R.id.tab_me)
            .perform(click());

        onWebView().check(webMatches(getScrollY(), is(200)));
    }

    private ViewInteraction onViewWithId(int id) {
        return onView(visibleWithId(id));
    }

    private Matcher<View> visibleWithId(int id) {
        return allOf(
            withId(id),
            isDisplayed()
        );
    }

}
