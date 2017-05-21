package com.android.udacity.stockhawk.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by FirmanMFK
 */
@RunWith(AndroidJUnit4.class)
public class LineGraphActivityTest {

    @Rule
    public ActivityTestRule<LineGraphActivity> mActivityRule = new ActivityTestRule<>(
            LineGraphActivity.class,
            true,
            false); // do not launch the activity immediately

    @Inject HistoricalDataClient mHistoricalDataClient;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        StockHawkApplication app = (StockHawkApplication) instrumentation.getTargetContext().getApplicationContext();
        TestingComponent component = (TestingComponent) app.getComponent();
        component.inject(this);
    }

    @Test
    public void shouldShowWeekGraph() {
        String stringStubDate[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        String stringStubDay[] = {"Mon", "Sun", "Thu"};
        float floatStub[] = {(float)75.0, (float)75.0, (float)100.0};
        LineSet dataStub = new LineSet(stringStubDate, floatStub);

        Mockito.when(mHistoricalDataClient.getData(
                Mockito.anyString(),
                Mockito.anyInt()))
                .thenReturn(dataStub);

        Intent intent = new Intent();
        intent.putExtra(LineGraphActivity.KEY_SYM, "test");

        mActivityRule.launchActivity(intent);

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubDay, floatStub)
                )));
    }

    @Test
    public void shouldShowTwoWeekGraph() {
        String stringStubDate[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        String stringStubDay[] = {"2016-03-14", "", "Thu"};
        float floatStub[] = {(float)75.0, (float)100.0, (float)150.0};
        LineSet dataStub = new LineSet(stringStubDate, floatStub);

        Mockito.when(mHistoricalDataClient.getData("test2", 7)).thenReturn(dataStub);
        Mockito.when(mHistoricalDataClient.getData("test2", 14)).thenReturn(dataStub);

        Intent intent = new Intent();
        intent.putExtra(LineGraphActivity.KEY_SYM, "test2");

        mActivityRule.launchActivity(intent);

        onView(withId(R.id.button_two_weeks)).perform(click());

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubDay, floatStub)
                )));
    }

    @Test
    public void shouldShowMonthGraph() {
        String stringStubDate[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        String stringStubDay[] = {"2016-03-14", "", "Thu"};
        float floatStub[] = {(float)7.0, (float)15.0, (float)20.0};
        LineSet dataStub = new LineSet(stringStubDate, floatStub);

        Mockito.when(mHistoricalDataClient.getData("test3", 7)).thenReturn(dataStub);
        Mockito.when(mHistoricalDataClient.getData("test3", 30)).thenReturn(dataStub);

        Intent intent = new Intent();
        intent.putExtra(LineGraphActivity.KEY_SYM, "test3");

        mActivityRule.launchActivity(intent);

        onView(withId(R.id.button_month)).perform(click());

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubDay, floatStub)
                )));
    }

    @Test
    public void shouldRetainStateAfterRotate() {
        String stringStubDateWeek[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        float floatStubWeek[] = {(float)75.0, (float)75.0, (float)100.0};

        Mockito.when(mHistoricalDataClient.getData("testRotate", 7))
                .thenReturn(new LineSet(stringStubDateWeek, floatStubWeek));

        String stringStubDateTwoWeeks[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        String stringStubLabelsTwoWeeks[] = {"2016-03-14", "", "Thu"};
        float floatStubTwoWeeks[] = {(float)75.0, (float)100.0, (float)150.0};

        Mockito.when(mHistoricalDataClient.getData("testRotate", 14))
                .thenReturn(new LineSet(stringStubDateTwoWeeks, floatStubTwoWeeks));

        String stringStubMonth[] = {"2016-03-14", "2016-02-14", "2016-01-14"};
        String stringStubLabelsMonth[] = {"2016-03-14", "", "Thu"};
        float floatStubMonth[] = {(float)7.0, (float)15.0, (float)20.0};

        Mockito.when(mHistoricalDataClient.getData("testRotate", 30))
                .thenReturn(new LineSet(stringStubMonth, floatStubMonth));

        Intent intent = new Intent();
        intent.putExtra(LineGraphActivity.KEY_SYM, "testRotate");

        mActivityRule.launchActivity(intent);

        onView(withId(R.id.button_two_weeks)).perform(click());

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubLabelsTwoWeeks, floatStubTwoWeeks)
                )));

        rotateScreen();

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubLabelsTwoWeeks, floatStubTwoWeeks)
                )));

        onView(withId(R.id.button_month)).perform(click());

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubLabelsMonth, floatStubMonth)
                )));

        rotateScreen();

        onView(withId(R.id.linechart)).check(
                matches(withContentDescription(
                        createGraphHint(stringStubLabelsMonth, floatStubMonth)
                )));
    }

    private String createGraphHint(String[] labels, float[] values) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < labels.length; ++i) {
            stringBuilder.append(labels[i])
                    .append(": ")
                    .append(values[i]);

            if (i < labels.length - 1) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
    }

    private void rotateScreen() {
        Context context = InstrumentationRegistry.getTargetContext();
        int orientation = context.getResources().getConfiguration().orientation;

        Activity activity = mActivityRule.getActivity();
        activity.setRequestedOrientation(
                (orientation == Configuration.ORIENTATION_PORTRAIT) ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}