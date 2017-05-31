package com.huawei.esdk.uc;

import com.huawei.application.BaseApp;
import com.huawei.common.os.EventHandler;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PhoneContactCache;
import com.huawei.contacts.SearchCache;
import com.huawei.dao.DbVindicate;
import com.huawei.device.DeviceManager;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.groupzone.controller.GroupZoneFunc;
import com.huawei.http.HttpCloudHandler;
import com.huawei.module.um.UmFunc;
import com.huawei.reportstatistics.controller.EventReporter;
import com.huawei.service.ServiceProxy;

/**
 * Created by lWX303895 on 2016/3/11.
 */
public class ApplicationHandler
{

    public static ApplicationHandler getIns()
    {
        return appHandler;
    }

    private static ApplicationHandler appHandler = new ApplicationHandler();
    /**
     * 强制退出时间
     */
    public static final int FORCE_EXIT_TIME = 1000;

    public void exitOrLogout()
    {

        //发送请求。
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // 清除在线状态
                SelfInfoUtil.getIns().setToLogoutStatus();
                ServiceProxy service = UCAPIApp.getApp().getService();
                if (null != service)
                {
                    // 注销时要clearAll，传true
                    service.logout(false);
                }
                if (ContactLogic.getIns().getAbility().isAllUmAbility())
                {
                    HttpCloudHandler.ins().clear();
                }
            }
        }, "logout thread");
        thread.start();

        //等待3秒后再注销。
        EventHandler.getIns().postDelayed(exitRunnable, FORCE_EXIT_TIME);
    }

    private final Runnable exitRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            logoutOrExit();
        }
    };

    private void logoutOrExit()
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                    exit();
            }
        };
        Thread t = new Thread(runnable , "LogoutOrExit");
        t.start();
    }

    private void exit()
    {
        clearResource();
        //停服务、退出SVN
        stopService();
        //注销广播
        BaseApp.unregisterDefaultLocalReceiver();
        //所有界面出栈
        UCAPIApp.getApp().popAllExcept(null);
        //停止进程
        DeviceManager.killProcess();
    }

    /**
     *清除资源
     */
    public void clearResource()
    {
        //清除线程管理器
        ThreadManager.getInstance().clearThreadResource();

        //关闭数据库
        DbVindicate.getIns().closeDb();

        //清除数据
        clearContactData();

    }

    /**
     * 停服务(退出时调用)
     */
    public void stopService()
    {
        UCAPIApp.getApp().stopImService();
    }

    /**
     * 注销时清理联系人相关数据
     */
    private void clearContactData()
    {
        HeadPhotoUtil.getIns().cleanPhotos();

        UmFunc.getIns().clear();
        GroupZoneFunc.ins().clear();
        ImFunc.getIns().clear();
        PhoneContactCache.getIns().clear();
        ContactCache.getIns().clear();
        SearchCache.getIns().clear();
        EventReporter.getIns().clear();
		//退出时清除未读消息容器
        UnreadMessageManager.getIns().clearUnreadManager();
    }
}
