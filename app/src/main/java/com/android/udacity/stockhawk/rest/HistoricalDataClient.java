package com.android.udacity.stockhawk.rest;

import com.db.chart.model.LineSet;

public interface HistoricalDataClient {
    LineSet getData(String symbol, int mDurationInDays);
}
