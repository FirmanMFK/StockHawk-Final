package com.android.udacity.stockhawk.ui;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.db.chart.model.LineSet;
import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.StockHawkApplication;
import com.android.udacity.stockhawk.dependencyInjection.TestingComponent;
import com.android.udacity.stockhawk.rest.HistoricalDataClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.inject.Inject;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
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
public class MyStocksActivityTest {
    @Inject
    HistoricalDataClient mHistoricalDataClient;

    @Rule
    public ActivityTestRule<MyStocksActivity> mActivityRule = new ActivityTestRule<>(
            MyStocksActivity.class,
            true,
            false); // do not launch the activity immediately

    @Before
    public void setUp() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        StockHawkApplication app
                = (StockHawkApplication) instrumentation.getTargetContext().getApplicationContext();
        TestingComponent component = (TestingComponent) app.getComponent();
        component.inject(this);
    }

    @Test
    public void shouldOpenSymbolSearch() {
        mActivityRule.launchActivity(new Intent());

        onView(withId(R.id.fab)).perform(click());

        onView(withText(R.string.symbol_search)).check(matches(isDisplayed()));
        onView(withText(R.string.content_test)).check(matches(isDisplayed()));
        onView(allOf(withClassName(endsWith("EditText")), withHint(R.string.input_hint)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void shouldShowLineGraphActivity() {
        String stringStubDate[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        float floatStub[] = {(float)75.0, (float)75.0, (float)100.0};
        LineSet dataStub = new LineSet(stringStubDate, floatStub);

//        Response.Builder responseBuilder = new Response.Builder();
//        responseBuilder.message("Test").networkResponse().build();

        Mockito.when(mHistoricalDataClient.getData("YHOO", 7)).thenReturn(dataStub);

        mActivityRule.launchActivity(new Intent());

        onView(withText("YHOO")).perform(click());

        onView(withClassName(endsWith("LineChartView"))).check(matches(isDisplayed()));
    }
}