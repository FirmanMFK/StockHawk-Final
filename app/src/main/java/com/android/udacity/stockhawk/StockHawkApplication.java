package com.android.udacity.stockhawk;

import android.app.Application;

import com.android.udacity.stockhawk.dependencyInjection.AndroidModule;
import com.android.udacity.stockhawk.dependencyInjection.ApplicationModule;
import com.android.udacity.stockhawk.dependencyInjection.BasicComponent;
import com.android.udacity.stockhawk.dependencyInjection.DaggerApplicationComponent;

public class StockHawkApplication extends Application {
    private BasicComponent mComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        mComponent = createComponent();
    }

    @SuppressWarnings("WeakerAccess")
    public BasicComponent createComponent() {
        return DaggerApplicationComponent.builder()
                .androidModule(new AndroidModule(this))
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public BasicComponent getComponent() {
        return mComponent;
    }
}