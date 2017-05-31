package com.huawei.esdk.uc.function;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

import com.huawei.common.res.LocContext;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.videoengine.ViERenderer;
import com.huawei.voip.CallManager;
import com.huawei.voip.data.VideoCapsN;

/**
 * 视频渲染处理类
 */
@SuppressLint("NewApi")
public final class VideoFunc
{
    private static VideoFunc ins;

    /** 视图指针默认值 */
    public static final int INVALID_VALUE = -1;

    /** 后置摄像头 */
    public static final int BACK_CAMERA = 0;

    /** 前置摄像头 */
    public static final int FRONT_CAMERA = 1;

    // / 视频横竖屏情况，仅对对移动平台有效{1,2,3}
    /** 1：竖屏 **/
    public static final int PORTRAIT = 1;

    /** 2：横屏 **/
    public static final int LANDSCAPE = 2;

    /** 3：反向横屏 **/
    public static final int REVERSE_LANDSCAPE = 3;

    /** 摄像头数量 */
    private static int numberOfCameras = Camera.getNumberOfCameras();

    /** 本地隐藏视频视图，用一个像素布局 */
    private static SurfaceView localVideo;

    /** 远端视频视图 */
    private static SurfaceView remoteVideo;
    

    /** 协商后的视频方向 */
    int consultOri = LANDSCAPE;

    private VideoCapsN videoCaps;

    private VideoFunc()
    {
        videoCaps = new VideoCapsN();
        configVideoCaps();
    }

    public static VideoFunc getIns()
    {
        if (ins == null)
        {
            ins = new VideoFunc();
        }
        return ins;
    }

    private void configVideoCaps()
    {
        
        // 设置默认采用的摄像头
        setDefaultCamera();

        // 设置为竖屏
        videoCaps.setOrient(PORTRAIT);
        videoCaps.setOrientPortrait(getVideoChangeOrientation(0,
                isFrontCamera()) / 90);
        videoCaps.setOrientLandscape(getVideoChangeOrientation(90,
                isFrontCamera()) / 90);
        videoCaps.setOrientSeascape(getVideoChangeOrientation(270,
                isFrontCamera()) / 90);
    }

    /**
     * 如果有前置摄像头,就设置前置摄像头;否则设置后置摄像头.
     */
    private void setDefaultCamera()
    {
        if (canSwitchCamera())
        {
            videoCaps.setCameraIndex(FRONT_CAMERA);
        }
        else
        {
            videoCaps.setCameraIndex(BACK_CAMERA);
        }
    }

    private boolean isFrontCamera()
    {
        return videoCaps.getCameraIndex() == FRONT_CAMERA;
    }

    /**
     * 处理voip注册成功后的视频相应处理.
     */
    public void handleVoipRegisterSuccess()
    {
        deployGlobalVideoCaps();
    }

    /**
     * 根据Activity取回的界面旋转度数来计算需要转动的度数.
     * @param degree
     * @return
     */
    private int getVideoChangeOrientation(int degree, boolean isfront)
    {
        int resultDegree = 0;
        if (isfront)
        {
            // 注意: 魅族手机与寻常手机不一致.需要分别做判断.正常手机采用下面方法即可
            // resultDegree = (VoipFunc.getIns().getVideoInfo()
            // .getCameraOrientation(FRONT_CAMERA) + degree) % 360;
            if (degree == 0)
            {
                resultDegree = 270;
            }
            else if (degree == 90)
            {
                resultDegree = 0;
            }
            else if (degree == 270)
            {
                resultDegree = 180;
            }

        }
        else
        {
            resultDegree = (90 - degree + 360) % 360;
        }
        // Logger.debug(TAG, "" + resultDegree);
        return resultDegree;
    }

    public void deploySessionVideoCaps()
    {
        Context context = LocContext.getContext();

        if (canSwitchCamera())
        {
            videoCaps.setCameraIndex(FRONT_CAMERA);
        }
        else
        {
            videoCaps.setCameraIndex(BACK_CAMERA);
        }

        if (localVideo == null)
        {
            localVideo = ViERenderer.CreateLocalRenderer(context);
            localVideo.setZOrderMediaOverlay(true);
        }
//        videoCaps.setPlaybackLocal(ViERenderer.getIndexOfSurface(localVideo));
        if (remoteVideo == null)
        {
            remoteVideo = ViERenderer.CreateRenderer(context, false);
            remoteVideo.setZOrderOnTop(false);
        }
        videoCaps.setPlaybackRemote(ViERenderer.getIndexOfSurface(remoteVideo));

        CallManager tempCallMan = VoipFunc.getIns().getCallManager();
        String sessionId =  videoCaps.getSessionId();
        if (tempCallMan != null)
        {
            tempCallMan.setVideoIndex(videoCaps.getCameraIndex());
            
          //新的sdk里面createVideoWindow及updateVideoWindow方法都变成私有的，由videoWindowAction来替代了。
            tempCallMan.videoWindowAction(0, videoCaps.getPlaybackRemote(), sessionId);
//            if(StringUtil.isStringEmpty(sessionId))
//            {
//                tempCallMan.createVideoWindow(0, videoCaps.getPlaybackRemote());
//                
//            }
//            else
//            {
//                tempCallMan.updateVideoWindow(0, videoCaps.getPlaybackRemote(), sessionId);
//            }
        }

        configVideoCaps();
        execute();
    }
    
    /**
     * 协商后的orient用此方法保存,不用再执行fast接口来设置.
     * @param orient
     */
    public void setOrient(int orient)
    {
       consultOri = orient;
    }

    public int getOrient()
    {
 
        return consultOri;
    }

    public void deployGlobalVideoCaps()
    {
        execute();
    }

    private void execute()
    {
        CallManager manager = VoipFunc.getIns().getCallManager();
        if (manager != null)
        {
            manager.setOrientParams(videoCaps);
//            manager.setCodecParams(false); // 设置融合会议，默认为false
        }
    }

    public void setCallId(String callId)
    {
        videoCaps.setSessionId(callId);
    }

    public SurfaceView getLocalVideoView()
    {
        if (localVideo != null)
        {
            return localVideo;
        }
        return null;
    }

    public SurfaceView getRemoteVideoView()
    {
        if (remoteVideo != null)
        {
            return remoteVideo;
        }
        return null;
    }

    public boolean closeVideo()
    {
        return VoipFunc.getIns().closeVideo();
    }

    public boolean openVideo()
    {
        return VoipFunc.getIns().upgradeVideo();
    }

    public void declineVideoInvite()
    {
        VoipFunc.getIns().declineVideoInvite();
    }

    public void agreeVideoUpgradte()
    {
        VoipFunc.getIns().agreeVideoUpgradte();
    }

    /**
     * 切换前后摄像头时,角度也会发生变化.
     */
    public void switchCamera()
    {
        int cameraIndex = videoCaps.getCameraIndex();
        Log.d(CommonUtil.APPTAG, "current camera index:" + cameraIndex);
        videoCaps.setCameraIndex((cameraIndex + 1) % 2);
        // 切换摄像头需要重写下方，fast目前没有处理
        videoCaps.setOrientPortrait(getVideoChangeOrientation(0, isFrontCamera()) / 90);
        videoCaps.setOrientLandscape(getVideoChangeOrientation(90, isFrontCamera()) / 90);
        videoCaps.setOrientSeascape(getVideoChangeOrientation(270, isFrontCamera()) / 90);
        
        execute();
    }
    
    public boolean isVideoNull()
    {
        return localVideo == null || remoteVideo == null;
    }

    public boolean canSwitchCamera()
    {
        return numberOfCameras > 1;
    }

    public void clearSurfaceView()
    {
        setDefaultCamera();
        
//        videoCaps.setPlaybackLocal(-1);
        videoCaps.setPlaybackRemote(-1);
        videoCaps.setSessionId(null);

        // 摄像头前后切换后，fast保留上次的值需要进行复位操作(3,0,2),默认为竖屏，协商后会调整为横屏
        // fast目前没有做切换摄像头下发处理，因此需要UI 处理规避切换摄像头的部分问题
        videoCaps.setOrientPortrait(getVideoChangeOrientation(0,
                isFrontCamera()) / 90);
        videoCaps.setOrientLandscape(getVideoChangeOrientation(90,
                isFrontCamera()) / 90);
        videoCaps.setOrientSeascape(getVideoChangeOrientation(270,
                isFrontCamera()) / 90);

        // 释放HME资源
        if(localVideo != null)
        {
            ViERenderer.setSurfaceNull(localVideo);
            localVideo = null;
        }
        if(remoteVideo != null)
        {
            ViERenderer.setSurfaceNull(remoteVideo);
            remoteVideo = null;
        }
    }
    public static int transOrient(int orient)
    {
        switch (orient)
        {
            case PORTRAIT:
            case LANDSCAPE:
            case REVERSE_LANDSCAPE:
                return orient;
            default:
                return LANDSCAPE;
        }
    }
}
