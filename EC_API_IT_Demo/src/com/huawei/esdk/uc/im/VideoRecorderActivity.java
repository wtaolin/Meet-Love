package com.huawei.esdk.uc.im;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huawei.contacts.ContactLogic;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;

/**
 * 录制视频
 * 调用MediaPlayer进行自定义录制视频，可以设定视频的编解码，码率，帧率。
 * 使录制的视频达到最好的显示效果，同时还不占用太大的空间。
 *
 * 仅供android 3.0以上版本使用。
 * Created by h00203586.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class VideoRecorderActivity extends BaseActivity implements Callback
{
    private static final String TAG = VideoRecorderActivity.class.getSimpleName();
    
    /**
     * 计时器的名称
     */
    private static final String TIMER_NAME = "record video";

    /**
     * 计时器计算的间隔时间
     */
    private static final int TIMER_PERIOD = 200;

    /**
     * 一秒钟对应的毫秒数。
     */
    private static final int SECOND_LENGTH = 1000;

    /**
     * 默认宽度
     */
    private static final int DEFAULT_WIDTH = 480;

    /**
     * 默认高度
     */
    private static final int DEFAULT_HEIGHT = 640;

    /**
     * DEGREE 360
     */
    private static final int DEGREE_CIRCLE = 360;

    /**
     * 录制视频的默认比例
     */
    private static final float RATIO_ASPECT = 3.f / 4;

    /**
     * handler what参数
     */
    private static final int REFRESH_RECORD_TIME = 1000;

    /**
     *  视频的比特率（字 bit） 1.5Mbps
     *
     */
    private static final int VIDEO_BIT_RATE = 1024 * 1500;

    /**
     * 音频的比特率 192kbps
     */
    private static final int AUDIO_BIT_RATE = 1024 * 192;

    /**
     * 显示捕获的视频的surface
     */
    private SurfaceView surface;

    /**
     * 记录并显示时间的空间
     */
    private TextView recordTime;

    /**
     * 切换摄像头的按钮
     */
    private ImageView switchCamera;

    /**
     * 录像按钮，点击即开始录像
     */
    private ImageView recordBtn;

    /**
     * SurfaceView对应的holder，用于与视频源建立连接。
     */
    private SurfaceHolder holder;

    /**
     * 摄像头
     */
    private Camera mCamera;

    /**
     * 媒体流
     */
    private MediaRecorder mMediaRecorder;

    /**
     * 是否在录像
     */
    private boolean isRecording = false;

    /**
     * 是否在录像前预览
     */
    private boolean isPreviewing = false;

    /**
     * 计时器，记录录像时间
     */
    private Timer timer;

    /**
     * 计时器中记时次数。
     */
    private int timerCount = 0;

    /**
     * android 的handler
     */
    private android.os.Handler handler;

    /**
     * 录像存放位置
     */
    private String filePath;

    /**
     * 前后摄像头的索引
     */
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    /**
     * 摄像显示的比率
     */
    private float ratio = RATIO_ASPECT;

    /**
     * 记录视频的一些参数： 如分辨率，帧数等。
     */
    private CamcorderProfile profile = null;

    /**
     * 时间记录前显示的红色闪动记号
     */
    private ImageView rad;

    /**
     * 时间显示的控件；控制时间是否显示
     */
    private LinearLayout timeLayout;

    @Override
    public void initializeData()
    {
        //获取视频录制文件存储的路径，如果传入的路径为空，退出。
        filePath = getIntent().getStringExtra(MediaStore.EXTRA_OUTPUT);
        if (TextUtils.isEmpty(filePath))
        {
            Logger.warn(TAG, "filePath is empty!");
            UCAPIApp.getApp().popActivity(this);
            return;
        }

        //初始化handler
        initHandler();

        //获取录制视频适宜的参数； 获取视频长宽比，用于视频页面显示
        profile = getProfile();
        ratio = getRatio(profile);
    }

    /**
     * 初始化android的 handler
     */
    private void initHandler()
    {
        handler = new android.os.Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what == REFRESH_RECORD_TIME)
                {
                    //在UI线程刷新时间在界面的显示。
                    int second = timerCount * TIMER_PERIOD / SECOND_LENGTH;
                    if (second >= 1)
                    {
                        recordBtn.setEnabled(true);
                        recordBtn.setClickable(true);
                    }

                    recordTime.setText(DateUtil.getTimeString(second));

                    if (second >= ContactLogic.getIns().getMyOtherInfo().getUmVideoRecordLength())
                    {
                        backToChatActivity();
                    }

                    if (rad.getVisibility() == View.VISIBLE)
                    {
                        rad.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        rad.setVisibility(View.VISIBLE);
                    }
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //根据ratio设置surface布局。
        setSurfaceLayout();
        setCameraDisplayOrientation(cameraId, mCamera);
    }

    @Override
    public void initializeComposition()
    {
        //设置全屏，即录制时全屏显示。
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //设置显示的内容
        setContentView(R.layout.video_recorder);

        //涂鸦后打开视频录制，背景颜色错误
        RelativeLayout root = (RelativeLayout) findViewById(R.id.root);
        root.setBackgroundColor(getResources().getColor(R.color.black));

        //初始化控件
        recordTime = (TextView) findViewById(R.id.record_time);
        switchCamera = (ImageView) findViewById(R.id.switch_camera);
        recordBtn = (ImageView) findViewById(R.id.record_btn);
        rad = (ImageView) findViewById(R.id.rad);
        timeLayout = (LinearLayout) findViewById(R.id.time_layout);

        surface = (SurfaceView) findViewById(R.id.record_surface);
        holder = surface.getHolder();
        holder.addCallback(this);

        //根据ratio设置surface布局。
        setSurfaceLayout();
        //ACTION 在3.0版本前还需要使用。
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        recordBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (isRecording)
                {
                    //onPause时会stopRecorder
                    backToChatActivity();
                }
                else
                {
                    recordVideo();
                }
            }
        });

        switchCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                //切换摄像头
                switchCameraId();

                //刷新界面
                refresh();

                //重新打开摄像头
                openCamera(cameraId,true);
                surfaceCreated(holder);
            }
        });
    }

    /**
     * 录制视频
     */
    private void recordVideo()
    {
        if (!openCamera(cameraId,false))
        {
            return;
        }

        surfaceCreated(holder);

        updateView(true);

        switchCamera.setVisibility(View.GONE);

        recordBtn.setEnabled(false);
        recordBtn.setClickable(false);
        DeviceUtil.setKeepScreenOn(VideoRecorderActivity.this);

        //三星note4需要延时拍摄视频。否则第一帧图片为黑色或黄色。
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                //如果Activity正在结束，不往下处理。
                if (isFinishing())
                {
                    return;
                }

                // 初始化视频录制
                if (prepareVideoRecorder())
                {
                    startRecorder();
                }
                else
                {
                    // 视频录制初始化失败
                    releaseMediaRecorder();
                }
            }
        }, 500);
    }

    /**
     * 刷新数据，包括长宽比、
     */
    private void refresh()
    {
        CamcorderProfile profile = getProfile();
        if (profile != this.profile)
        {
            this.profile = profile;
            ratio = getRatio(profile);

            setSurfaceLayout();
        }
    }

    /**
     * 根据适宜的profile获取视频的长宽比
     * @param profile 视频录制的profile
     * @return
     */
    private float getRatio(CamcorderProfile profile)
    {
        float ratio = RATIO_ASPECT;
        if (profile != null)
        {
            ratio = 1.f * profile.videoFrameHeight / profile.videoFrameWidth;
        }
        return ratio;
    }

    /**
     * 设置surface的布局： 长宽比
     */
    private void setSurfaceLayout()
    {
        //获取手机分辨率高宽
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int reqWidth = metrics.widthPixels;

//        CamcorderProfile.QUALITY_QVGA 4/3
        holder.setFixedSize(reqWidth, (int)(reqWidth / ratio));
    }

    /**
     * 停止录制后， 返回到聊天页面
     */
    private void backToChatActivity()
    {

        int second = timerCount * TIMER_PERIOD / SECOND_LENGTH;
        if (second >= 1)
        {
            //录制成功的标志位
            setResult(RESULT_OK);
        }
        else
        {
            setResult(RESULT_CANCELED);
        }

        UCAPIApp.getApp().popActivity(this);
    }

    /**
     * 停止记录视频。
     */
    private void stopRecorder()
    {
        if (mMediaRecorder == null)
        {
            return ;
        }

        // 停止录制视频并且释放资源
        try
        {
            mMediaRecorder.stop();
        }
        catch (IllegalStateException exception)
        {
            Logger.error(TAG, "error = " + exception.toString());
        }
        catch (RuntimeException exe)
        {
            Logger.error(TAG, "error = " + exe.toString());
        }

        releaseMediaRecorder();
        mCamera.lock();

        stopTimer();

        DeviceUtil.releaseKeepScreen();

        updateView(false);
        isRecording = false;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);//set default mode.


    }

    /**
     * 开启视频录制
     */
    private void startRecorder()
    {
        // 开始录制视频
        try
        {
            mMediaRecorder.start();
        }
        catch (IllegalStateException exception)
        {
            Logger.error(TAG, "startRecorder = " + exception.toString());
            releaseMediaRecorder();
            return;
        }

        isRecording = true;

        switchCamera.setVisibility(View.GONE);

        recordBtn.setEnabled(false);
        recordBtn.setClickable(false);
        startTimer();

        DeviceUtil.setKeepScreenOn(this);
    }

    private void updateView(boolean update)
    {
        if (update)
        {
            // 设置按钮为录制状态
            setRecordBtnBackground(R.drawable.camera_stop_selector);
            timeLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            // 通知视频录制结束
            setRecordBtnBackground(R.drawable.camera_player_selector);
            timeLayout.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * 开启计时器，记录记时时间
     */
    private void startTimer()
    {
        timerCount = 0;
        timer = new Timer(TIMER_NAME);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                handler.sendEmptyMessage(REFRESH_RECORD_TIME);
                timerCount++;
            }
        }, TIMER_PERIOD, TIMER_PERIOD); // 启动录制时间显示
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (KeyEvent.KEYCODE_BACK == keyCode && isRecording)
        {
            backToChatActivity();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 关闭计时器
     */
    private void stopTimer()
    {
        if (timer == null)
        {
            return;
        }

        timer.purge();
        timer.cancel();
        timer = null;
    }



    /**
     * 初始化MediaRecorder
     * 初始化的顺序要注意。
     * @return 成功返回true
     */
    private boolean prepareVideoRecorder()
    {
        // Step 1: 解锁摄像头，设置recorder的camera。
        mCamera.stopPreview();  //优先stopPreview（暂时只发现对于Mate1必须调用）
        mCamera.unlock();

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: 设置音视频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: 设置录制视频的质量 (requires API Level 8 or higher)
        try
        {
            int longSide = 0;
            if (profile != null)
            {
                mMediaRecorder.setProfile(profile);
                longSide = profile.videoFrameWidth > profile.videoFrameHeight
                    ? profile.videoFrameWidth : profile.videoFrameHeight;
            }
            if (longSide < DEFAULT_HEIGHT) //如果长的一边比640大（可能是720），不设置videoSize；
            {
                mMediaRecorder.setVideoSize(DEFAULT_HEIGHT, DEFAULT_WIDTH);
            }

            // Step 4: 设置文件输出路径,输出的视频方向（横竖屏支持）
           // mMediaRecorder.setOrientationHint(getVideoOri(cameraId));
            int mOrientation = getCameraDisplayOrientation(cameraId, mCamera);
            mMediaRecorder.setOrientationHint(mOrientation);
            mMediaRecorder.setOutputFile(filePath);

            // Step 5: 设置preview显示的surface
            mMediaRecorder.setPreviewDisplay(holder.getSurface());

            mMediaRecorder.setOnErrorListener(new MyErrorListener());

            // Step 6: 准备配置
            mMediaRecorder.prepare();
        }
        catch (IllegalStateException e)
        {
            Logger.error(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        catch (IOException e)
        {
            Logger.error(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private static class MyErrorListener implements MediaRecorder.OnErrorListener
    {
        @Override
        public void onError(MediaRecorder mediaRecorder, int what, int extra)
        {
            Logger.error(TAG, "record error, what = " + what + "/extra = " + extra);
        }
    }

    /**
     * 切换cameraid。
     * 如果当前是前置摄像头，则切换得到后置摄像头。
     * 如果当前是后置摄像头，则切换到前置摄像头。
     */
    private void switchCameraId()
    {
        if (Camera.CameraInfo.CAMERA_FACING_BACK == cameraId)
        {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else
        {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    /**
     * 打开摄像头
     *
     * @param cameraId
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private boolean openCamera(int cameraId, boolean continuesPictures)
    {
        int cameraCount = Camera.getNumberOfCameras();

        // 摄像头已经打开的情况下release
        if (isPreviewing)
        {
            releaseCamera();
        }

        //没有获取到摄像头时直接返回
        if (cameraCount == 0)
        {
            Logger.error(TAG, "sorry, don't find camera!");
            UCAPIApp.getApp().popActivity(this);

            return false;
        }

        boolean openSuccess = false;
        try
        {
            if (cameraCount > 1)  //只有一个摄像头时，不显示切换摄像头按钮
            {
                switchCamera.setVisibility(View.VISIBLE);

                mCamera = Camera.open(cameraId);
            }
            else
            {
                switchCamera.setVisibility(View.GONE);
                mCamera = Camera.open();
            }

            openSuccess = true;

           // mCamera.setDisplayOrientation(getCameraOri(cameraId));
            setCameraDisplayOrientation(cameraId, mCamera);
            DeviceUtil.setCameraParams(mCamera, ratio, continuesPictures);

            return true;
        }
        catch (IllegalStateException ie)
        {
            Logger.error(TAG, "sorry, camera set Exception ! + " + ie.toString());
            UCAPIApp.getApp().popActivity(this);
            return false;
        }
        catch (Exception e)
        {
            //1 如果打开成功，则需要关闭摄像头并释放。否则后续则无法调用摄像头。释放在onPause里面
            //2 如果打开失败，则需要置摄像头引用为null。避免后续引用
            if (!openSuccess)
            {
                mCamera = null;
            }
            Logger.error(TAG, "sorry, camera open Exception ! + " + e.toString());
            UCAPIApp.getApp().popActivity(this);
            return false;
        }
    }

    /**
     * 设置视频输出的方向
     * @param mCameraId cameraId
     * @return
     */
    public int getVideoOri(int mCameraId)
    {
        final boolean isGingerbread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        if (!isGingerbread)
        {
            Logger.debug(TAG, "api < 9");
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);

        return info.orientation;
    }

    /**
     * 设置摄像头展示的方位
     * @param cameraId
     * @return 返回orientation
     */
    public int getCameraOri(int cameraId)
    {
        final boolean isGingerbread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        if(!isGingerbread)
        {
            Logger.debug(TAG, "api < 9");
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        //degress为0时即竖屏，正向
        int degrees = 0;

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % DEGREE_CIRCLE;
            result = (DEGREE_CIRCLE - result) % DEGREE_CIRCLE;
        }
        else
        {
            result = (info.orientation - degrees + DEGREE_CIRCLE) % DEGREE_CIRCLE;
        }

        return result;
    }

    public void clearData()
    {

        if (mCamera != null)
        {
            mCamera.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        try
        {
            if (mCamera != null)
            {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

                isPreviewing = true;
            }
        }
        catch (IOException e)
        {
            Logger.error(TAG, "Error setting camera preview: " + e.getMessage());
        }
        catch (RuntimeException re)
        {
            Logger.error(TAG, "Error runtime: " + re.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3)
    {
//
//        mCamera.autoFocus(new Camera.AutoFocusCallback()
//        {
//            @Override
//            public void onAutoFocus(boolean b, Camera camera)
//            {
//                if(b){
//                    openCamera(cameraId);
//                    mCamera.cancelAutoFocus();
//                }
//            }
//        });
        // 如果预览有变化或者旋转，需要先停止预览重新开启

        // 预览的surface不存在
        if (holder.getSurface() == null)
        {
            return;
        }

        try
        {
            mCamera.setPreviewDisplay(null);
            mCamera.stopPreview();
        }
        catch (Exception e)
        {
            Logger.warn(TAG, e);
            // 忽略： mCamera可能不存在
        }

        try
        {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            isPreviewing = true;
        }
        catch (Exception e)
        {
            Logger.error(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        openCamera(cameraId, true);  //打开摄像头，对应onPause
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        stopRecorder();       // 释放mMediaRecorder资源
        releaseCamera();              // 释放摄像头占用资源

        backToChatActivity();
    }

    @Override
    protected void onStop()
    {
        // 释放mMediaRecorder资源
        stopRecorder();
        // 释放摄像头占用资源
        releaseCamera();
        super.onStop();
    }

    /**
     * 是否录制视频用到的资源
     */
    private void releaseMediaRecorder()
    {
        if (mMediaRecorder != null)
        {
            mMediaRecorder.reset();   // 清除配置
            mMediaRecorder.release(); // 释放recorder对象
            mMediaRecorder = null;
            if(null != mCamera)
            {
                mCamera.lock();           // 锁定camera供后面使用
            }
        }
    }

    /**
     * 释放摄像头资源
     */
    private void releaseCamera()
    {
        if (mCamera != null)
        {
            mCamera.stopPreview();
            try
            {
                mCamera.setPreviewDisplay(null);
            }
            catch (IOException e)
            {
                Logger.error(TAG, "Camera stop preview error.");
            }
            mCamera.release();        // 释放摄像头资源，其他地方使用。
            mCamera = null;
        }

        isPreviewing = false;
    }

    /**
     * 设置录音按钮的背景
     * @param res
     */
    public void setRecordBtnBackground(int res)
    {
        recordBtn.setBackgroundResource(res);
    }

    /**
     * 获取录制视频使用的分辨率，编解码格式设置的profile
     *
     * @return 返回获取的profile
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public CamcorderProfile getProfile()
    {
        boolean isHoneyComb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

        CamcorderProfile profile = null;
        if (isHoneyComb)
        {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P))
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
            }
            else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF))
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF);
            }
            else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA))
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
            }
            else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF))
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QCIF);
            }
        }
        else
        {
            profile = getLowVersionProfile(cameraId);
        }

        if (profile != null)
        {
            Logger.debug(TAG, "frame rate= " + profile.videoFrameRate
                + "/videoBitRate = " + profile.videoBitRate
                +"/frame videoFrameWidth= " + profile.videoFrameWidth
                + "/frame videoFrameHeight= " + profile.videoFrameHeight
                + "/profile.videoCodec= " + profile.videoCodec);

            //设置比特率，帧率
            //caution: 这里不能设置帧率，H264为动态帧率。而且华为D1 ，华为较早出的手机都不能设置帧率。
            profile.videoBitRate = VIDEO_BIT_RATE;

            profile.audioBitRate = AUDIO_BIT_RATE;
            profile.audioChannels = 2;
        }
        return profile;
    }

    /**
     * 获取低版本使用的profile
     * @param cameraId
     * @return
     */
    private CamcorderProfile getLowVersionProfile(int cameraId)
    {
        CamcorderProfile profile = null;
        try
        {
            profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        }
        catch (RuntimeException exception)
        {
            try
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
            }
            catch (RuntimeException e)
            {
                profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
        }

        return profile;
    }

    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera)
    {

        final boolean isGingerbread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        if(!isGingerbread)
        {
            Logger.debug(TAG, "api < 9--default set to 90");
            camera.setDisplayOrientation(90);
            return;
        }

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public int  getCameraDisplayOrientation(int cameraId, android.hardware.Camera camera)
    {

        final boolean isGingerbread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        if(!isGingerbread)
        {
            Logger.debug(TAG, "api < 9");
            return 90;
        }
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


}
