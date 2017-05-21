package com.android.udacity.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.data.QuoteColumns;
import com.android.udacity.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Utils {
  // avoid initialization of class
  private Utils() {};

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject;
    JSONArray resultsArray;
    Log.i(LOG_TAG, "GET FB: " +JSON);
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");

          ContentProviderOperation cpo = buildBatchOperation(jsonObject);

          if (cpo == null) {
            throw new NullPointerException("test");
          }

          batchOperations.add(cpo);
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            ContentProviderOperation cpo;
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              cpo = buildBatchOperation(jsonObject);

              if (cpo == null) {
                continue;
              }

              batchOperations.add(cpo);
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    catch (NullPointerException e) {
      Log.e(LOG_TAG, "Symbol no: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0, 1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());

    Double changeDouble;

    try {
      changeDouble = Double.parseDouble(change);
    }
    catch (NumberFormatException e) {
      Log.e(LOG_TAG, "Can't parse change: " + e.toString());
      return "0.00";
    }

    double round = (double) Math.round(changeDouble * 100) / 100;
    change = String.format("%.2f", round);
    StringBuilder changeBuilder = new StringBuilder(change);
    changeBuilder.insert(0, weight);
    changeBuilder.append(ampersand);
    change = changeBuilder.toString();
    return change;
  }

  @Nullable
  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      String bid = jsonObject.getString("Bid");

      if (bid.equals("null")) {
        return null;
      }

      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(bid));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  public static String getCalculatedDate(String dateFormat, int days)
          throws NullPointerException {

    if(dateFormat == null) {
      throw new NullPointerException("dateFormat is null");
    }

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, days);
    SimpleDateFormat s = new SimpleDateFormat(dateFormat);

    return s.format(new Date(cal.getTimeInMillis()));
  }

  public static double roundToNextTen(double v, boolean directionUp)
          throws IllegalArgumentException {
    if (v < 0) {
      throw new IllegalArgumentException("value has to be greater or equal to zero");
    }

    double moduloValue = v % 10;

    if (moduloValue != 0){
      if (directionUp) {
        double x = 10 - (moduloValue);
        v += x;
      } else {
        v -= moduloValue;
      }
    }

    return v;
  }

  public static @Nullable String convertDateToWeekday(String date,
                                                      Context context)
                                                      throws NullPointerException {
    if (date == null) {
      throw new NullPointerException("date is null");
    }

    if (context == null) {
      throw new NullPointerException("context is null");
    }

    // if it is yesterday's date return "Yesterday"
    if (date.equals(getCalculatedDate("yyyy-MM-dd", -1))) {
      return context.getString(R.string.yesterday);
    }

    // format YYYY-MM-DD
    String[] datePieces = date.split("-");
    Calendar calendar = Calendar.getInstance();

    // month parameter in Calendar.set(int, int, int) start with zero not one!
    int month = Integer.parseInt(datePieces[1]) -1;

    calendar.set(Integer.parseInt(datePieces[0]),
            month,
            Integer.parseInt(datePieces[2]));

    int dayOfTheWeek = calendar.get(Calendar.DAY_OF_WEEK);

    String dayString = null;

    switch (dayOfTheWeek) {
      case Calendar.MONDAY:
        dayString = context.getString(R.string.mon_short);
        break;
      case Calendar.TUESDAY:
        dayString = context.getString(R.string.tue_short);
        break;
      case Calendar.WEDNESDAY:
        dayString = context.getString(R.string.wed_short);
        break;
      case Calendar.THURSDAY:
        dayString = context.getString(R.string.thu_short);
        break;
      case Calendar.FRIDAY:
        dayString = context.getString(R.string.fri_short);
        break;
      case Calendar.SATURDAY:
        dayString = context.getString(R.string.sat_short);
        break;
      case Calendar.SUNDAY:
        dayString = context.getString(R.string.sun_short);
        break;
    }

    return dayString;
  }
}
