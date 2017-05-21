package com.android.udacity.stockhawk.dependencyInjection;

import com.android.udacity.stockhawk.ui.LineGraphActivityTest;
import com.android.udacity.stockhawk.ui.MyStocksActivityTest;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by FirmanMFK
 */
@Singleton
@Component(modules = { MockModule.class })
public interface TestingComponent extends BasicComponent {
    void inject(LineGraphActivityTest target);
    void inject(MyStocksActivityTest target);
}
