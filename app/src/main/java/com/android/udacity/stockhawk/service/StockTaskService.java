package com.android.udacity.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.android.udacity.stockhawk.StockHawkApplication;
import com.android.udacity.stockhawk.busEvents.ServerDownEvent;
import com.android.udacity.stockhawk.busEvents.SymbolEvent;
import com.android.udacity.stockhawk.data.QuoteColumns;
import com.android.udacity.stockhawk.data.QuoteProvider;
import com.android.udacity.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.inject.Inject;

/**
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();
  public static final String APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";

  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  @Inject Bus mBus;
  @Inject OkHttpClient mClient;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
    ((StockHawkApplication) mContext.getApplicationContext()).getComponent().inject(this);
  }

  private String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = mClient.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
        + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor == null || initQueryCursor.getCount() == 0){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else {
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\"");
          mStoredSymbols.append(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
          mStoredSymbols.append("\",");

          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder.length() > 0){
      urlString = urlStringBuilder.toString();
      Log.d("StockTaskService", "URL: " + urlString);
      try{
        getResponse = fetchData(urlString);

        // server doesn't find table on datatables.org (backend of yahoo.finance.quotes)
        if (getResponse.startsWith("{\"error\":{")) {
          throw new IllegalStateException("Server is down");
        }

        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }

          ArrayList<ContentProviderOperation> results = Utils.quoteJsonToContentVals(getResponse);
          if (!results.isEmpty()) {
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, results);
          }
          else {
              Log.i(LOG_TAG, "Symbol not found");
              mBus.post(new SymbolEvent(SymbolEvent.STATE.FAILURE));
          }
        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      }
      catch (IOException e){
        e.printStackTrace();
      }
      catch (IllegalStateException e) {
        Log.e(LOG_TAG, "Server is down");
        mBus.post(new ServerDownEvent());
      }
      catch (NullPointerException e) {
        Log.e(LOG_TAG, "Update failed: " + e.getMessage());
      }
    }

    sendWidgetBroadcast();

    return result;
  }

  private void sendWidgetBroadcast() {
    Log.d(LOG_TAG, "sendWidgetBroadcast");
    Intent widgetBroadcastIntent = new Intent(APPWIDGET_UPDATE)
            .setPackage(mContext.getPackageName());
    mContext.sendBroadcast(widgetBroadcastIntent);
  }

}
