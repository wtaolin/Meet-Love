package com.huawei.esdk.uc.utils;

import android.os.Environment;

import java.io.File;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.Constant;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.ecs.mtk.log.LogLevel;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.common.CrashExceptionHandler;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.utils.AndroidLogger;

/**
 * 本地log工具类
 * */
public final class LocalLog 
{

    public static final String APPTAG = "UC_API_DEMO";
    
    public static final String FRAMEWORKTAG = "FrameWork";
   
    private static final String LOGFILE = "/ucApiDemoLog.txt";
    
//    public static final String LOG_PATH = UCAPIApp.getApp().getFilesDir() + "/ucApiDemoLog";
    public static final String LOG_PATH = Environment.getExternalStorageDirectory().getPath() + "/ucApiDemoLog";
    
    public static final String LOG_PATH_RELATIVE = "/eSpaceAppLog";
    
    private static final int LOGMAXSIZE = 10485760;
    
    /**
     * Default constructor
     */
    private LocalLog()
    {
    }

    /**
     * 初始化日志设置
     */
    public static void initializeLog()
    {
        File file = new File(LOG_PATH);
        
        if (!(file.isDirectory() && file.exists()))
        {
            if(!file.mkdirs())
            {
                Logger.info(LocalLog.APPTAG, "Creat directory fail...");
            }
        }

//        setSaveLogSwitch(SelfDataHandler.getIns().getSelfData().isLogFeedBack());
        setSaveLogSwitch(true);
        Thread.setDefaultUncaughtExceptionHandler(new CrashExceptionHandler());
    }
    
    /**
     * Set whether to save the log
     * @param isOpen ： true means open, false means close
     */
    public static void setSaveLogSwitch(boolean isOpen)
    {
        if(Logger.getLogger() == null)
        {
            Logger.setLogger(new AndroidLogger());
        }
        
        SelfDataHandler.getIns().getSelfData().setLogFeedBack(isOpen);

        Logger.setMaxLogFileSize(LOGMAXSIZE);
        
        Logger.setLogFile(LOG_PATH + LOGFILE);

        if (isOpen)
        {
            Logger.setLogLevel(LogLevel.DEBUG);
        }
        else 
        {
            Logger.setLogLevel(LogLevel.INFO);
        }
        
        // VOIP会在初始化VOIP的时候开一次，espaceApp创建的时候也调用了此接口，不过因为callManager为null，所以不会有问题
        VoipFunc.getIns().setVoipLog(isOpen);
    } 
    
    public static void setVoipLog(boolean isOpen)
    {
        String path = Constant.ESPACE_LOG_PATH + Constant.VOIP_LOG_PATH;
        CommonVariables.getIns().setFastLogSwitch(path, isOpen);
    }
}
