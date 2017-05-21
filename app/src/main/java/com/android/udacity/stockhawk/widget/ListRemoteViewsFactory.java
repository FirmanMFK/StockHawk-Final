package com.android.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.android.udacity.stockhawk.R;
import com.android.udacity.stockhawk.data.QuoteColumns;
import com.android.udacity.stockhawk.data.QuoteProvider;
import com.android.udacity.stockhawk.ui.LineGraphActivity;

public class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Cursor mCursor = null;
    private Context mContext = null;

    public ListRemoteViewsFactory(Context applicationContext, Intent intent) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {
        mCursor = getCursor();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = getCursor();
    }

    @Override
    public RemoteViews getLoadingView() {
        // use default loading view
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if ((mCursor == null) || !mCursor.moveToPosition(position)) {
            return null;
        }

        final String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        final String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));

        RemoteViews remoteView = new RemoteViews(
                mContext.getPackageName(),
                R.layout.widget_collection_item);

        remoteView.setTextViewText(R.id.stock_symbol, symbol);
        remoteView.setTextViewText(R.id.change, change);

        Intent intent = new Intent();
        intent.putExtra(LineGraphActivity.KEY_SYM, symbol);
        remoteView.setOnClickFillInIntent(R.id.widget_list_item, intent);

        return remoteView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        if (mCursor.moveToPosition(position)) {
            return mCursor.getLong(mCursor.getColumnIndex(QuoteColumns._ID));
        }

        return position;
    }

    private Cursor getCursor() {
        final long token = Binder.clearCallingIdentity();

        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{
                            QuoteColumns._ID,
                            QuoteColumns.SYMBOL,
                            QuoteColumns.CHANGE},
                    QuoteColumns.ISCURRENT + " = ?", new String[]{"1"},
                    null
            );
        }
        finally {
            Binder.restoreCallingIdentity(token);
        }

        return cursor;
    }
}
