package com.android.udacity.stockhawk.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
