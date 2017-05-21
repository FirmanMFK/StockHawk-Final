package com.android.udacity.stockhawk.customMatcher;


import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.Button;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Created by FirmanMFK
 */
public final class ButtonBackgroundMatcher {
    /**
     * Returns a matcher that matches {@link Button}s based on background color value.
     *
     * @param color {@link Matcher} of {@link ColorFilter} with color to match
     */
    @NonNull
    public static Matcher<View> withButtonBackgroundColor(final int color) {

        return new BoundedMatcher<View, Button>(Button.class) {

            @Override
            public void describeTo(final Description description) {
                description.appendText("with background color: ");
            }

            @Override
            public boolean matchesSafely(final Button button) {
                ColorDrawable backgroundColor = (ColorDrawable) button.getBackground();
                return color == backgroundColor.getColor();
            }
        };
    }
}
