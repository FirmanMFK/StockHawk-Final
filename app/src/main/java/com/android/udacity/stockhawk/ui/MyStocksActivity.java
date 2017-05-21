package com.android.udacity.stockhawk.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.StockHawkApplication;
import com.android.udacity.stockhawk.busEvents.ServerDownEvent;
import com.android.udacity.stockhawk.busEvents.SymbolEvent;
import com.android.udacity.stockhawk.data.QuoteColumns;
import com.android.udacity.stockhawk.data.QuoteProvider;
import com.android.udacity.stockhawk.rest.QuoteCursorAdapter;
import com.android.udacity.stockhawk.rest.RecyclerViewItemClickListener;
import com.android.udacity.stockhawk.rest.Utils;
import com.android.udacity.stockhawk.service.StockIntentService;
import com.android.udacity.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.android.udacity.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
  private static final String KEY_TITLE = "STOCKHAWK_KEY_TITLE";

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  @SuppressWarnings("FieldCanBeLocal")
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  boolean isConnected;
  @Inject Bus mBus;
  @Bind(R.id.recycler_view) EmptyRecyclerView mRecyclerView;
  @Bind(R.id.empty_text) TextView mEmptyView;
  @Bind(R.id.empty_layout) LinearLayout mEmptyLayout;
  @Bind(R.id.server_down_text) TextView mServerDownText;
  @Bind(R.id.server_down_image) ImageView mServerDownImage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((StockHawkApplication) getApplication()).getComponent().inject(this);

    mContext = this;
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    isConnected = activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();

    setContentView(R.layout.activity_my_stocks);
    ButterKnife.bind(this);

    if (savedInstanceState == null) {
      // The intent service is for executing immediate pulls from the Yahoo API
      // GCMTaskService can only schedule tasks, they cannot execute immediately
      mServiceIntent = new Intent(this, StockIntentService.class);

      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected) {
        startService(mServiceIntent);
      } else {
        onServerDownEvent(new ServerDownEvent());
      }

      mTitle = getTitle();
    }
    else {
      mTitle = savedInstanceState.getCharSequence(KEY_TITLE);
    }
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override
              public void onItemClick(View v, int position) {
                // move to correct row in database
                mCursor.moveToPosition(position);
                String symbol = mCursor.getString(mCursor.getColumnIndex("symbol"));
                Log.d("MainActivity", "Clicked on " + symbol);

                Intent intent = new Intent(mContext, LineGraphActivity.class);
                intent.putExtra(LineGraphActivity.KEY_SYM, symbol);
                startActivity(intent);
              }
            }));

    mRecyclerView.setAdapter(mCursorAdapter);
    mRecyclerView.setEmptyView(mEmptyLayout);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(mRecyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (isConnected){
            new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { input.toString() }, null);
                  if (c != null && c.getCount() != 0) {
                    Toast toast =
                        Toast.makeText(MyStocksActivity.this, getString(R.string.stock_already_saved),
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                    toast.show();
                    c.close();
                  } else {
                    // Add the stock to DB
                    mServiceIntent.putExtra("tag", "add");
                    mServiceIntent.putExtra("symbol", input.toString());
                    startService(mServiceIntent);
                  }
                }
              })
              .show();
        } else {
          onServerDownEvent(new ServerDownEvent());
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(mRecyclerView);

    if (isConnected){
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    mBus.register(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    mBus.unregister(this);
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
      actionBar.setDisplayShowTitleEnabled(true);
      actionBar.setTitle(mTitle);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Subscribe
  public void onSymbolEvent(SymbolEvent e) {
    if (e.state == SymbolEvent.STATE.FAILURE) {
      Log.i("MainActivity", "Symbol not found");

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(mContext, getString(R.string.symbol_not_found), Toast.LENGTH_LONG).show();
        }
      });

    }
  }

  @Subscribe
  public void onServerDownEvent(ServerDownEvent e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mEmptyView.setVisibility(View.GONE);
        mServerDownImage.setVisibility(View.VISIBLE);
        mServerDownText.setVisibility(View.VISIBLE);
      }
    });
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putCharSequence(KEY_TITLE, mTitle);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    if (!isConnected) {

      // mark all content as not current
      ContentValues contentValues = new ContentValues();
      contentValues.put(QuoteColumns.ISCURRENT, 0);
      mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI,
              contentValues,
              null,
              null);
    }

    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
            new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                    QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
            QuoteColumns.ISCURRENT + " = ?",
            new String[]{"1"},
            null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }

}
