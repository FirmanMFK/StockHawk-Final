package com.android.udacity.stockhawk.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.customMatcher.ToastMatcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.StringEndsWith.endsWith;

/**
 * Created by FirmanMFK
 */
@RunWith(AndroidJUnit4.class)
public class ToastTest {
    @Rule
    public ActivityTestRule<MyStocksActivity> mActivityRule = new ActivityTestRule<>(
            MyStocksActivity.class);

    @Test
    public void shouldShowSymbolNotFoundToast() {
        onView(withId(R.id.fab)).perform(click());

        onView(allOf(withClassName(endsWith("EditText")), withHint(R.string.input_hint)))
                .perform(typeText("Test"));

        onView(withText("OK")).perform(click());

        onView(withText(R.string.symbol_not_found)).inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
