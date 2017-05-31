package com.huawei.esdk.uc.common;

import com.huawei.contacts.SelfDataHandler;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.utils.CustomExceptionHandler;


public class CrashExceptionHandler extends CustomExceptionHandler
{
    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        //应在开始处打印Throwable日志，避免crash循环导致无法查看crash日志
        logException(e);

        Logger.error(LocalLog.APPTAG, "crash exit! message = " + e.getMessage());
        SelfDataHandler.getIns().getSelfData().setCrashExit(true);
        
        UCAPIApp.getApp().stopImService();  
        

        UCAPIApp.getApp().popAllExcept(null);

        //涉及系统操作，需放到最后。
        super.uncaughtException(t, e);
    }
}
