package com.huawei.esdk.uc.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.huawei.application.BaseApp;
import com.huawei.common.CommonVariables;
import com.huawei.common.LocalBroadcast;
import com.huawei.common.constant.Constant;
import com.huawei.common.constant.UCResource;
import com.huawei.common.os.EventHandler;
import com.huawei.common.res.LocContext;
import com.huawei.config.param.SDKConfigParam;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.device.DeviceManager;
import com.huawei.ecs.mtk.log.LogLevel;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.ApplicationHandler;
import com.huawei.esdk.uc.CommonProc;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.UCAPIService;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.function.VoipNotification;
import com.huawei.esdk.uc.temp.DataProc;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.log.TagInfo;
import com.huawei.push.ExceptionHandler;
import com.huawei.service.EspaceService;
import com.huawei.service.ServiceProxy;
import com.huawei.utils.AndroidLogger;
import com.huawei.utils.io.FileUtil;
import com.huawei.utils.io.ZipUtil;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;

public class UCAPIApp extends Application
{
    private static final int LOG_MAX_SIZE = 10485760;
    private static final String ESPACE_LOG_FILE = "/eSpacelog.txt";

    private static final String LOG_PATH = Constant.ESPACE_EXT_STORAGE_PATH + Constant.ESPACE_LOG_PATH;

    private static final String TAG = UCAPIApp.class.getSimpleName();

    private static UCAPIApp instance = null;

    private ServiceProxy serviceProxy = null;

    private boolean mServiceStarted = false;

    private Stack<Activity> activityStack = new Stack<Activity>();

    private final List<Message> mQueue = new ArrayList<Message>();

    //    public static UCAPIApp getApp()
//    {
//        return instance;
//    }
    public static void setApp(UCAPIApp app)
    {
        instance = app;
    }

    public static UCAPIApp getApp()
    {
        if (instance == null)
        {
            return instance = new UCAPIApp();
        }
        return instance;
    }

    @Override
    public void onCreate()
    {
        setApp(this);
        super.onCreate();
        doOnCreate(this);
    }
    public void doOnCreate(Application app)
    {
        BaseApp.initData(app);

        //启动Application时初始化，否则其他地方引用可能崩溃（还未注册就在主线程调用引起的崩溃）
        EventHandler.getIns();

        if (BaseApp.isPushProcess(LocContext.getContext()))
        {
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
            return;
        }
        Logger.setLogger(new AndroidLogger());

        LocalLog.initializeLog();

//        initResourceFile();

        ImFunc.getIns();

        /**先注释*/
//        LocalBroadcast.getIns().registerBroadcastProc(new CommonProc());

        LocalBroadcast.getIns().registerBroadcastProc(DataProc.getIns().getProc());

        BaseApp.registerDefaultLocalReceiver();
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        doAttachBaseContext(base);
    }

    public void doAttachBaseContext(Context base)
    {
        MultiDex.install(base);
    }
    private void initResourceFile()
    {
        ThreadManager.getInstance().addToFixedThreadPool(new Runnable()
        {
            @Override
            public void run()
            {
                initDataConfRes();
            }
        });
    }

    private void initDataConfRes()
    {
        String path = ConferenceFunc.DATACONF_RES_PATH;
        File file = new File(path);

        File[] files = file.listFiles();

        if (file.exists())
        {
            if (files != null && files.length == 7)
            {
                return;
            }
            else
            {
                FileUtil.deleteFile(file);
            }
        }

        try
        {
            InputStream inputStream = getAssets().open("AnnoRes.zip");
            ZipUtil.unZipFile(inputStream, path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void startUCAPIService()
    {
        Intent service = new Intent(LocContext.getContext().getApplicationContext(), UCAPIService.class);
        LocContext.getContext().getApplicationContext().startService(service);
    }

    public void stopUCAPIService()
    {
        Intent service = new Intent(LocContext.getContext().getApplicationContext(), UCAPIService.class);
        LocContext.getContext().getApplicationContext().stopService(service);
    }

    public ServiceProxy getService()
    {
        return serviceProxy;
    }

    private synchronized void startImService()
    {

        if (!mServiceStarted)
        {
            startUCAPIService();
            Log.d(CommonUtil.APPTAG, TAG + " | startESpaceService");

            Intent intent = new Intent(LocContext.getContext().getApplicationContext(), EspaceService.class);
            intent.putExtra(UCResource.EXTRA_CHECK_AUTO_LOGIN, false);
            intent.putExtra(UCResource.EXTRA_BROADCAST_RECEIVER_PERMISSION,
                    "com.huawei.eSpaceMobileApp");
            intent.putExtra(UCResource.EXTRA_PROTOCOL_VERSION, 3);
            intent.putExtra(UCResource.EXTRA_CHAT_NOTIFICATION, false);
            intent.putExtra(UCResource.HTTP_LOG_PATH,
                    FileUtil.getSdcardPath());

            /** configServiceParam方法弃用，再次添加by lwx302895 start */
            SDKConfigParam param = new SDKConfigParam();
            SelfDataHandler handler = SelfDataHandler.getIns();
            param.setVoipSupport(handler.getSelfData().isVoIPSwitchFlag());
            param.setVoipSupport(handler.getSelfData().isVoIPSwitchFlag());
            param.addAbility(SDKConfigParam.Ability.CODE_OPOUS);
            param.addAbility(SDKConfigParam.Ability.VOIP_2833);
            param.addAbility(SDKConfigParam.Ability.VOIP_VIDEO);
            param.setClientType(SDKConfigParam.ClientType.UC_MOBILE);
            param.setMegTypeVersion((short) 3);
            param.setHttpLogPath(FileUtil.getSdcardPath());
            intent.putExtra(UCResource.SDK_CONFIG, param);
            /** by lwx303895 end */

            LocContext.getContext().startService(intent);
            LocContext.getContext().bindService(intent, mImServiceConn, Context.BIND_AUTO_CREATE);
        }
        else
        {
            synchronized (mQueue)
            {
                for (Message msg : mQueue)
                {
                    msg.sendToTarget();
                }
                mQueue.clear();
            }
        }
    }

    public void stopImService()
    {
        stopImService(true);
    }

    public synchronized void stopImService(boolean clearData)
    {
        if (mServiceStarted)
        {
            stopUCAPIService();
            Log.d(CommonUtil.APPTAG, TAG
                    + " | stop ImService because there's no active connections");
            if (serviceConnected())
            {
                Log.d(CommonUtil.APPTAG, TAG + " | unbindService ImService ");
                EspaceService.stopService(clearData);
                LocContext.getContext().getApplicationContext().unbindService(mImServiceConn);
                serviceProxy = null;
            }

            mServiceStarted = false;
        }
    }

    /**
     * 内部类建立与SDK服务的连接
     **/
    private final ServiceConnection mImServiceConn = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(CommonUtil.APPTAG, TAG + " | onServiceConnected");

            mServiceStarted = true;

            if (!(service instanceof EspaceService.ServiceBinder))
            {
                return;
            }
            // 获取代理服务
            serviceProxy = ((EspaceService.ServiceBinder) service).getService();


            VoipFunc.getIns().registerVoip(new VoipNotification());

            synchronized (mQueue)
            {
                for (Message msg : mQueue)
                {
                    msg.sendToTarget();
                }
                mQueue.clear();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(CommonUtil.APPTAG, TAG + " | onServiceDisconnected");
            serviceProxy = null;
            mServiceStarted = false;
        }
    };

    public void callWhenServiceConnected(Runnable callback)
    {
        //只不过是一个包含了一个Handler对象和callback的Message,会更加高效
        Message msg = Message.obtain(new Handler(), callback);


        if (serviceConnected())
        {
            msg.sendToTarget();
        }
        else
        {
            startImService();

            synchronized (mQueue)
            {
                mQueue.add(msg);
            }
        }
    }

    private boolean serviceConnected()
    {
        return null != serviceProxy;
    }

    public void addActivity(Activity activity)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | addActivity | activity = " + activity);
        activityStack.push(activity);
    }

    public void popWithoutFinish(Activity activity)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | popWithoutFinish | activity = "
                + activity);
        Activity temp = activityStack.pop();
        if (temp != activity)
        {
            activityStack.push(temp);
        }
    }

    public void popActivity(Activity activity)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | popActivity | activity = " + activity);
        if (activity != null)
        {
            activityStack.removeElement(activity);
            activity.finish();
        }
    }

    public Activity getCurActivity()
    {
        Log.d(CommonUtil.APPTAG, TAG + " | getCurActivity");
        if (!activityStack.isEmpty())
        {

            Activity currentActivity = activityStack.lastElement();
            if (null == currentActivity)
            {
                activityStack.pop();
                currentActivity = getCurActivity();
            }
            return currentActivity;
        }
        return null;
    }

    public void popAllExcept(Activity activity)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | popAllExcept | activity = " + activity);
        int size = activityStack.size();
        Activity temp;
        for (int i = 0; i < size; i++)
        {
            temp = activityStack.pop();
            if (null != temp && temp != activity)
            {
                temp.finish();
            }
        }
        if (null != activity)
        {
            activityStack.push(activity);
        }
    }

    public static void setSaveLogSwitch(boolean isOpen)
    {
        if (Logger.getLogger() == null)
        {
            Logger.setLogger(new AndroidLogger());
        }

        Logger.setMaxLogFileSize(LOG_MAX_SIZE);
        Logger.setLogFile(LOG_PATH + ESPACE_LOG_FILE);
        Logger.setLogLevel(isOpen ? LogLevel.DEBUG : LogLevel.INFO);
        SelfDataHandler.getIns().getSelfData().setLogFeedBack(isOpen);

        setVoipLog(isOpen);
    }

    public static void setVoipLog(boolean isOpen)
    {
        String path = Constant.ESPACE_LOG_PATH + Constant.VOIP_LOG_PATH;
        CommonVariables.getIns().setFastLogSwitch(path, isOpen);
    }
//


    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        doOnTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        doOnConfigurationChanged(newConfig);
    }

    public void doOnTrimMemory(int level)
    {
        super.onTrimMemory(level);

        if (BaseApp.isPushProcess(LocContext.getContext()))
        {
            return;
        }
        Logger.debug(TagInfo.APPTAG, "level = " + level);
        ThreadManager.getInstance().addToFixedThreadPool(new Runnable() {
            @Override
            public void run()
            {
//                ApplicationHandler.getIns().clearResource();
            }
        });

    }
    public void doOnConfigurationChanged(Configuration newConfig)
    {
        if (BaseApp.isPushProcess(LocContext.getContext()))
        {
            Logger.debug(TagInfo.APPTAG, "push process");
            return;
        }

//        String currentLan = DeviceManager.getLocalLanguage();
//        boolean languageChanged = !TextUtils.isEmpty(lastLan) && !lastLan.eq;
        super.onConfigurationChanged(newConfig);
    }
}
