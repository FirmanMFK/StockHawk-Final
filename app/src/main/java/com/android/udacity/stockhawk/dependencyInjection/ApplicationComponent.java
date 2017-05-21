package com.android.udacity.stockhawk.dependencyInjection;

import javax.inject.Singleton;

import dagger.Component;


@SuppressWarnings("unused")
@Singleton
@Component(modules = {
        AndroidModule.class,
        ApplicationModule.class
})
public interface ApplicationComponent extends BasicComponent {

}
