package com.android.udacity.stockhawk.dependencyInjection;

import com.android.udacity.stockhawk.rest.HistoricalDataClientImpl;
import com.android.udacity.stockhawk.service.StockTaskService;
import com.android.udacity.stockhawk.ui.LineGraphActivity;
import com.android.udacity.stockhawk.ui.MyStocksActivity;


public interface BasicComponent {
    void inject(MyStocksActivity target);
    void inject(LineGraphActivity target);
    void inject(StockTaskService target);
    void inject(HistoricalDataClientImpl target);
}
