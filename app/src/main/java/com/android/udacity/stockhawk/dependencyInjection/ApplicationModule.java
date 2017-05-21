package com.android.udacity.stockhawk.dependencyInjection;

import android.app.Application;

import com.android.udacity.stockhawk.rest.HistoricalDataClient;
import com.android.udacity.stockhawk.rest.HistoricalDataClientImpl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@SuppressWarnings("unused")
@Module
public class ApplicationModule {
    private final Application mApplication;

    public ApplicationModule(Application mApplication) {
        this.mApplication = mApplication;
    }

    @Provides
    @Singleton
    public Bus provideBus() {
        return new Bus(ThreadEnforcer.ANY);
    }

    @Provides
    @Singleton
    public HistoricalDataClient provideHistoricalDataClient() {
        return new HistoricalDataClientImpl(mApplication);
    }

    @Provides
    public OkHttpClient providesClient() {
        return new OkHttpClient();
    }
}
