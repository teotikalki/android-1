package org.owntracks.android;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;

import androidx.annotation.NonNull;

import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.DebugFileLogTree;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import timber.log.Timber;

public class App extends DaggerApplication  {
    private static Application sApplication;

    @Inject
    Preferences preferences;

    @Inject
    Runner runner;

    @Inject
    MessageProcessor messageProcessor;

    @Inject
    Parser parser; 

    @Inject
    @AppContext
    Context context;

    @Override
    public void onCreate() {
        sApplication = this;
        WorkManager.initialize(this, new Configuration.Builder().build());

        super.onCreate();

        if(preferences.getLogDebug()) {
            Timber.plant(new DebugFileLogTree(this));
        } else if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(@NonNull StackTraceElement element) {
                    return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();
                }
            });
        }

        if(BuildConfig.DEBUG) {
            Timber.e("StrictMode enabled in DEBUG build");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());

        }


        preferences.checkFirstStart();

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runner.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                messageProcessor.initialize();
            }
        }, 510);

    }


    public static void restart() {
        Intent intent = new Intent(App.getContext(), MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        AppComponent appComponent = DaggerAppComponent.builder().app(this).build();
        appComponent.inject(this);
        AppComponentProvider.setAppComponent(appComponent);
        return appComponent;
    }

    public static Application getApplication() {
        return sApplication;
    }

    @Deprecated
    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

}