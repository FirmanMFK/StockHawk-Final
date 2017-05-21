package com.android.udacity.stockhawk.customMatcher;

import android.support.annotation.NonNull;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.internal.util.Checks;
import android.view.View;
import android.widget.Button;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Created by FirmanMFK
 */
public class ButtonTextColorMatcher {
    /**
     * Returns a matcher that matches {@link Button}s based on background color value.
     *
     * @param color {@link int} with color to match
     */
    @NonNull
    public static Matcher<View> withButtonTextColor(final long color) {
        Checks.checkNotNull(color);
        return new BoundedMatcher<View, Button>(Button.class) {
            @Override
            public void describeTo(final Description description) {
                description.appendText("with text color: ");
            }

            @Override
            public boolean matchesSafely(final Button button) {
                long colorButton = button.getTextColors().getDefaultColor();
                return color == button.getTextColors().getDefaultColor();
            }
        };
    }
}
