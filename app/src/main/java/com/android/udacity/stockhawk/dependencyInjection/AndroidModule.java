package com.android.udacity.stockhawk.dependencyInjection;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@SuppressWarnings("unused")
@Module
public class AndroidModule {
    private final Application mApplication;

    public AndroidModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return mApplication;
    }
}
