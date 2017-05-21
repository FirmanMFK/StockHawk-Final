package com.android.udacity.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.CubicEase;
import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.StockHawkApplication;
import com.android.udacity.stockhawk.rest.HistoricalDataClient;
import com.android.udacity.stockhawk.rest.Utils;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LineGraphActivity extends AppCompatActivity {


    enum TIME_FRAME {WEEK, TWO_WEEKS, MONTH, INVALID}

    public static final String KEY_SYM = "LineGraphActivity.KEY_SYM";
    private static final String STATE_LAST_BUTTON = "stockhawk.linegraph.last.button";
    private static final String STATE_SYMBOL = "stockhawk.linegraph.symbol";
    private static final String STATE_DATA_VALUES = "stockhawk.linegraph.data.values";
    private static final String STATE_DATA_LABELS = "stockhawk.linegraph.data.labels";
    private final String LOG_TAG = this.getClass().getSimpleName();

    @Bind(R.id.loader) ProgressBar mProgressBar;
    @Bind(R.id.linechart) LineChartView mLineChartView;
    @Bind(R.id.durationButtons) RelativeLayout mButtons;
    @Bind(R.id.button_week) Button mButtonWeek;
    @Bind(R.id.button_month) Button mButtonMonth;
    @Bind(R.id.button_two_weeks) Button mButtonTwoWeeks;

    private PublishSubject<Integer> mHistoricalDataSubject;
    private Subscription mHistoricalDataSubscription;
    private String mSymbol;
    private int mLastButtonPressedId;
    private float[] mGraphValues;
    private String[] mGraphLabels;
    @Inject HistoricalDataClient mRestClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);
        ButterKnife.bind(this);

        ((StockHawkApplication)getApplication()).getComponent().inject(this);

        createObservable();

        if (savedInstanceState != null) {
            mSymbol = savedInstanceState.getString(STATE_SYMBOL);
            mLastButtonPressedId = savedInstanceState.getInt(STATE_LAST_BUTTON);

            for (Button b : new Button[]{mButtonWeek, mButtonTwoWeeks, mButtonMonth}) {
                if (b.getId() == mLastButtonPressedId) {
                    b.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
                    b.setTextColor(Color.BLACK);
                }
                else {
                    b.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                    b.setTextColor(Color.WHITE);
                }
            }

            float[] values = savedInstanceState.getFloatArray(STATE_DATA_VALUES);
            String[] labels = savedInstanceState.getStringArray(STATE_DATA_LABELS);

            if (labels != null && values != null) {
                updateGraph(new LineSet(labels, values));
            }
        }
        else {
            Intent intent = getIntent();
            mSymbol = intent.getStringExtra(KEY_SYM);

            mButtonTwoWeeks.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            mButtonMonth.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

            // set variable to a valid value
            mLastButtonPressedId = R.id.button_month;
            // always start with weekly overview
            buttonClick(mButtonWeek);
        }
    }

    private void createObservable() {
        Log.d(LOG_TAG, "createObservable");
        mHistoricalDataSubject = PublishSubject.create();

        mHistoricalDataSubscription = mHistoricalDataSubject
                .observeOn(Schedulers.io())
                .map(new Func1<Integer, LineSet>() {
                    @Override
                    public LineSet call(Integer duration) {
                        Log.d(LOG_TAG, "map");
                        return mRestClient.getData(mSymbol, duration);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<LineSet>() {

                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOG_TAG, "Couldn't get update:" + e.toString());
                    }

                    @Override
                    public void onNext(LineSet data) {
                        Log.d(LOG_TAG, "onNext");
                        updateGraph(data);
                    }
        });
    }

    @OnClick({R.id.button_week,
              R.id.button_two_weeks,
              R.id.button_month})
    public void buttonClick(View button) {
        int buttonId = button.getId();
        Button lastButton;

        if (buttonId == mLastButtonPressedId) {
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mLineChartView.setVisibility(View.GONE);

        switch(mLastButtonPressedId) {
            case R.id.button_week:
                lastButton = mButtonWeek;
                break;
            case R.id.button_two_weeks:
                lastButton = mButtonTwoWeeks;
                break;
            case R.id.button_month:
                lastButton = mButtonMonth;
                break;
            default:
                Log.e(LOG_TAG, "Invalid mLastButtonPressedId: " + mLastButtonPressedId);
                return;
        }

        lastButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        lastButton.setTextColor(Color.WHITE);

        button.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
        ((Button) button).setTextColor(Color.BLACK);
        mLastButtonPressedId = buttonId;

        switch (buttonId) {
            case R.id.button_week:
                mHistoricalDataSubject.onNext(7);
                break;
            case R.id.button_two_weeks:
                mHistoricalDataSubject.onNext(14);
                break;
            case R.id.button_month:
                mHistoricalDataSubject.onNext(30);
                break;
            default:
                Log.e(LOG_TAG, "Invalid buttonId: " + mLastButtonPressedId);
        }
    }

    private void updateGraph(LineSet data) {
        mLineChartView.reset();
        int dataSize = data.size();
        Log.d(LOG_TAG, "dataSize: " + dataSize);

        mGraphValues = new float[dataSize];
        mGraphLabels = new String[dataSize];

        LineSet formatedData = getFormatedLabels(data, dataSize);

        if (formatedData == null) {
            Log.e(LOG_TAG, "formated data is null");
            return;
        }

        mLineChartView.setAxisColor(Color.WHITE)
                .setLabelsColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        mLineChartView.setGrid(ChartView.GridType.VERTICAL, 2, dataSize, paint);

        formatedData.setColor(Color.YELLOW)
                .setDotsRadius(Tools.fromDpToPx(4))
                .setDotsStrokeThickness(Tools.fromDpToPx(3))
                .setDotsStrokeColor(Color.YELLOW)
                .setThickness(10);

        try {
            mLineChartView.addData(formatedData);
        }
        catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Can't add formatedData: " + e.toString());
            return;
        }

        Animation anim = new Animation()
                .setEasing(new CubicEase()
                );

        StringBuilder descriptionBuilder = new StringBuilder();

        float minValue = Float.MAX_VALUE, maxValue = Float.MIN_VALUE, value;

        for (int i = 0; i < dataSize; ++i) {
            value = formatedData.getValue(i);

            minValue = min(minValue, value);
            maxValue = max(maxValue, value);

            descriptionBuilder.append(formatedData.getLabel(i))
                    .append(": ")
                    .append(value);

            // add coma for all, but the last entry
            if (i < formatedData.size() - 1) {
                descriptionBuilder.append(", ");
            }
        }

        mLineChartView.setContentDescription(descriptionBuilder.toString());

        int maxRoundUp = (int) Utils.roundToNextTen(maxValue, true);
        int minRoundDown = (int) Utils.roundToNextTen(minValue, false);

        mLineChartView.setAxisBorderValues(minRoundDown,
                maxRoundUp,
                (maxRoundUp - minRoundDown) / 5);

        mProgressBar.setVisibility(View.GONE);
        mLineChartView.setVisibility(View.VISIBLE);

        mLineChartView.show(anim);
    }

    private LineSet getFormatedLabels(LineSet data, int dataSize) {
        String[] formatedDates = new String[dataSize];

        switch (getSelectedTimeFrame()) {
            case WEEK:
                for (int i = 0; i < dataSize; ++i) {
                    try {
                        mGraphLabels[i] = data.getLabel(i);
                        formatedDates[i] = Utils.convertDateToWeekday(mGraphLabels[i], this);
                        Log.d(LOG_TAG, "formatedDate:" + formatedDates[i]);
                    }
                    catch (NullPointerException e) {
                        Log.e(LOG_TAG, "data.getLabel(" + i + ") is null:" + e.toString());
                    }
                    mGraphValues[i] = data.getValue(i);
                }
                break;
            case TWO_WEEKS:
                for (int i = 0; i < dataSize; ++i) {
                    mGraphLabels[i] = data.getLabel(i);

                    if (i == dataSize - 1) {
                        formatedDates[i] = Utils.convertDateToWeekday(mGraphLabels[i], this);
                    }

                    else if (i == 0 || i == 4 || i == 7) {
                        formatedDates[i] = mGraphLabels[i];
                    }
                    else {
                        formatedDates[i] = "";
                    }

                    mGraphValues[i] = data.getValue(i);
                }
                break;
            case MONTH:
                for (int i = 0; i < dataSize; ++i) {
                    mGraphLabels[i] = data.getLabel(i);

                    if (i == dataSize - 1) {
                        formatedDates[i] = Utils.convertDateToWeekday(mGraphLabels[i], this);
                    }

                    else if ((i % 5) == 0) {
                        formatedDates[i] = mGraphLabels[i];
                    }
                    else {
                        formatedDates[i] = "";
                    }

                    mGraphValues[i] = data.getValue(i);
                }
                break;
            default:
                Log.e(LOG_TAG, "Time frame is invalid");
                return null;
        }

        return new LineSet(formatedDates, mGraphValues);
    }

    private TIME_FRAME getSelectedTimeFrame() {
        if (mButtonWeek.getCurrentTextColor() == Color.BLACK) {
            return TIME_FRAME.WEEK;
        }
        else if (mButtonTwoWeeks.getCurrentTextColor() == Color.BLACK) {
            return TIME_FRAME.TWO_WEEKS;
        }
        else if (mButtonMonth.getCurrentTextColor() == Color.BLACK) {
            return TIME_FRAME.MONTH;
        }

        return TIME_FRAME.INVALID;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_LAST_BUTTON, mLastButtonPressedId);
        savedInstanceState.putString(STATE_SYMBOL, mSymbol);
        savedInstanceState.putFloatArray(STATE_DATA_VALUES, mGraphValues);
        savedInstanceState.putStringArray(STATE_DATA_LABELS, mGraphLabels);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mHistoricalDataSubscription != null && !mHistoricalDataSubscription.isUnsubscribed()) {
            mHistoricalDataSubscription.unsubscribe();
        }
    }
}
