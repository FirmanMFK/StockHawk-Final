package com.android.udacity.stockhawk.rest;

import android.app.Application;
import android.util.Log;

import com.db.chart.model.LineSet;
import com.android.udacity.stockhawk.StockHawkApplication;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.inject.Inject;


public class HistoricalDataClientImpl implements HistoricalDataClient {
    private final String LOG_TAG = this.getClass().getSimpleName();

    @Inject OkHttpClient mClient;

    public HistoricalDataClientImpl(Application application) {
        ((StockHawkApplication) application).getComponent().inject(this);
    }

    public LineSet getData(String symbol, int mDurationInDays) {
        StringBuilder url = new StringBuilder();

        String startDate = Utils.getCalculatedDate("yyyy-MM-dd", mDurationInDays * (-1) - 1);
        // end date is yesterday, because there isn't historical data for today yet
        String endDate = Utils.getCalculatedDate("yyyy-MM-dd", -1);

        url.append("https://query.yahooapis.com/v1/public/yql?")
                .append("q=select Date, Close from yahoo.finance.historicaldata where symbol in (\"")
                .append(symbol)
                .append("\") and startDate = \"")
                .append(startDate)
                .append("\" and endDate = \"")
                .append(endDate)
                .append("\"&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");

        String jsonResult = "";

        try {
            Request request = new Request.Builder()
                    .url(url.toString())
                    .build();

            Response response = mClient.newCall(request).execute();

            if (response.isSuccessful()) {
                jsonResult = response.body().string();
            }
            else {
                Log.e(LOG_TAG, "Error: " + response.toString());
            }
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Can't get historical data: " + e.toString());
            return new LineSet();
        }

        LineSet data = new LineSet();

        try {
            JSONObject reader = new JSONObject(jsonResult);

            JSONArray quoteArray = reader
                    .getJSONObject("query")
                    .getJSONObject("results")
                    .getJSONArray("quote");

            JSONObject quote;
            String date;
            float value;

            // data is sorted descending, but we want ascending
            for (int i = quoteArray.length() - 1; i >= 0; --i) {
                quote =  quoteArray.getJSONObject(i);
                date = quote.getString("Date");
                value = (float) quote.getDouble("Close");

                data.addPoint(date, value);

                Log.d(LOG_TAG, "[" + i + "]: " + date + ": " + value);
            }
        }
        catch (JSONException e) {
            Log.e(LOG_TAG, "Can't parse JSON: " + e.toString());
            Log.e(LOG_TAG, "JSON: " + jsonResult);
        }

        return data;
    }
}
