package com.huawei.esdk.uc.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.huawei.common.res.LocContext;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.device.PlatformInfo;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.utils.io.FileUtil;

import java.io.File;
import java.util.List;

/**
 * @author xKF49568
 * @since 2012-4-24 下午09:41:00
 * @author renxianglin 去掉了部分代码
 */
public final class DeviceUtil
{
    /**
     * TAG
     */
    private static final String TAG = "DeviceUtil";
    
    /**
     * 电源管理 屏幕锁
     */
    private static PowerManager.WakeLock screenLock = null;


    /**
     * 构造方法
     *
     * @author xKF49568
     */
    private DeviceUtil()
    {
    	
    }

   

    public static boolean isWifiConnect()
    {
        ConnectivityManager cm = (ConnectivityManager) LocContext.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (null != wifi && wifi.isConnected())
        {
            return true;
        }

        return false;
    }

    /**
     * 新加判定app是否在前台的方法,还未经过测试比较{@code DeviceUtil.isTopApp()}.
     * @return
     */
    public static boolean isAppOnForeground()
    {
        Context context = LocContext.getContext().getApplicationContext();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getPackageName();

        List<RunningAppProcessInfo> appProcesses = activityManager
                        .getRunningAppProcesses();
        if (appProcesses == null)
        {
            return false;
        }

        for (RunningAppProcessInfo appProcess : appProcesses)
        {
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isFirstInstall()
    {
        String version = SelfDataHandler.getIns().getSelfData().getVersion();
        if(TextUtils.isEmpty(version))
        {
            return true;
        }

        return false;
    }


    /**
     * 获取当前程序字体大小
     * @return
     */
    public static float getFontScale()
    {
        Configuration config = LocContext.getContext().getResources().getConfiguration();
        Logger.debug(TAG, "The font scale is " + config.fontScale);
        return config.fontScale;
    }

    /**
     * 是否是默认字体大小
     * @return
     */
    public static boolean isLargerThanDefFontScale()
    {
        return getFontScale() > 1;
    }

    public static void setConfigToDefault()
    {
        Resources res = LocContext.getContext().getResources();
        Configuration config = res.getConfiguration();
        config.fontScale = 1.0f;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static boolean isDeviceSupported()
    {
        return PlatformInfo.getCPUInfo() == PlatformInfo.CPUInstruction.CPU_V7;
    }

    public static class MySensorEventListener implements SensorEventListener
    {
        /**
         * 是否语音播放监听
         */
        private boolean isAudioPlay;

        public boolean isAudioPlay()
        {
            return isAudioPlay;
        }

        public void setAudioPlay(boolean isAudioPlay)
        {
            this.isAudioPlay = isAudioPlay;
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasJellyBeanMR1()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean hasIceCreamSandwitch()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
    
    public static void setKeepScreenOn(Context context)
    {
        synchronized (DeviceUtil.class)
        {
            if (null == screenLock)
            {
                PowerManager pm = (PowerManager) context
                        .getSystemService(Context.POWER_SERVICE);
                screenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                        TAG);
                screenLock.acquire();
                //screenLock.setReferenceCounted(false);
            }
        }
    }
    
    public static void releaseKeepScreen()
    {
        try
        {
            if (null != screenLock && screenLock.isHeld())
            {
                screenLock.release();
            }

            screenLock = null;   
        }
        catch(Exception e)
        {
            Logger.debug(TAG, "screenLock.release exception, possible WakeLock under-locked ");
        }
    }
    
    /**
     * 设置摄像头预览时的界面大小
     * 要求： 预览界面的显示比率与要求比率一致（即长宽比)
     * @param camera Camera对象
     * @param ratio 比率 (高 / 宽）
     */
    public static void setCameraParams(Camera camera, float ratio,
            boolean continuesPictures)
    {
        Camera.Parameters params = camera.getParameters();
        List<String> supportModes = params.getSupportedFocusModes();

        //获取预览界面支持的size，从中取出符合条件的可以用的size
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (null != sizes)
        {
            for (Camera.Size size : sizes)
            {
                if (Math.abs(1.f * size.height / size.width - ratio) < 0.01f)
                {
                    Logger.debug(TAG, "height = " + size.height
                            + "width = " + size.width);
                    params.setPreviewSize(size.width, size.height);
                    //设置此参数后拍摄视频没有延时
                    if (continuesPictures)
                    {
                        if ((supportModes != null && supportModes.contains(
                                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                                && DeviceUtil.hasIceCreamSandwitch()))
                        {
                            params.setFocusMode(
                                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                    }
                    else
                    {

                        if ((supportModes != null && supportModes.contains(
                                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)))
                        {
                            params.setFocusMode(
                                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                        else if ((supportModes != null && supportModes.contains(
                                Camera.Parameters.FOCUS_MODE_INFINITY)))
                        {
                            params.setFocusMode(
                                    Camera.Parameters.FOCUS_MODE_INFINITY);
                        }
                        else
                        {
                            params.setFocusMode(
                                    Camera.Parameters.WHITE_BALANCE_AUTO);
                        }

                    }
                    params.setJpegQuality(100);
                    camera.setParameters(params);

                    break;
                }
            }
        }
    }
    
    /**
     * 通知android的相册刷新显示
     * @param path
     */
    public static  void notifyAlbum(Activity activity, String path)
    {
        if (TextUtils.isEmpty(path))
        {
            return;
        }

        File file = new File(path);
        if (!file.exists())
        {
            return;
        }

        activity.sendBroadcast(
            new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            Uri.fromFile(file)));
    }
    
    /**
     * judge if the phone is idle
     * @return <code>true</code> if idle,otherwise <code>false</code>.
     */
    public static boolean isCallStateIdle()
    {
        TelephonyManager tm = (TelephonyManager) LocContext.getContext()
            .getSystemService(Service.TELEPHONY_SERVICE);
        return tm.getCallState() == TelephonyManager.CALL_STATE_IDLE;
    }
    
    /**
     * 是否支持保存文件
     *
     * @return true 支持保存文件
     */
    public static boolean isEnableSave(Context context)
    {
        if (!FileUtil.isSaveFileEnable())
        {
            ToastUtil.showToast(context, R.string.feedback_sdcard_prompt);
            return false;
        }

        return true;
    }
}
