package com.huawei.esdk.uc;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Gallen on 2016/10/15.
 */
public class MyService extends Service {
    private final static String TAG = "MyService";
    private MyBinder mBinder = new MyBinder();
    public final static String ACTION_RECORD = "ACTION_RECORD";

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        Log.e(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_RECORD)) {
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    public class MyBinder extends Binder {
        public MyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyService.this;
        }
    }
}
