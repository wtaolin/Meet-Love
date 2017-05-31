package com.huawei.esdk.uc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Gallen on 2016/10/15.
 */
public class InteractVideoActivity extends Activity{
    private static final String tag="InteractVideoActivity";
    private Camera myCamera = null;
    private SurfaceView surface = null;
    private SurfaceHolder mySurfaceHolder = null;

    private ImageView emotion;
    private VideoView video;
    private Gesture gesture;
    private Expression expression;

    //For loading opencv library
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    gesture = new Gesture();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    //For create video thumbnail
    private Bitmap createVideoThumbnail() {
        Bitmap bitmap = null;
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {// MODE_CAPTURE_FRAME_ONLY
//          retriever
//                  .setMode(android.media.MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
//          retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
            retriever.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.default_video));
//          bitmap = retriever.captureFrame();
            String timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long time = Long.parseLong(timeString) * 1000;
            Log.i("TAG","time = " + time);
            bitmap = retriever.getFrameAtTime(time*31/160); //按视频长度比例选择帧
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return bitmap;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(tag, "onCreate");
        //设置全屏无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window myWindow = this.getWindow();
        myWindow.setFlags(flag, flag);

        setContentView(R.layout.activity_interact_video);

        video = (VideoView) findViewById(R.id.video);
        //初始化SurfaceView
        surface = (SurfaceView)findViewById(R.id.image);
        mySurfaceHolder = surface.getHolder();
        mySurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);//translucent半透明 transparent透明
        mySurfaceHolder.addCallback(new myCallBack(Camera.CameraInfo.CAMERA_FACING_FRONT));
        mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setZOrderOnTop(true);

        emotion = (ImageView)findViewById(R.id.emotion);

        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.default_video));
        video.start();
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                video.seekTo(0);
                if (!video.isPlaying())
                    video.start();
            }
        });

        //Init gesture detection
        expression = new Expression();

        findViewById(R.id.hangoff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private class myCallBack implements SurfaceHolder.Callback {
        private boolean isPreview = false;
        private int cameraId;

        public myCallBack(int id) {
            cameraId = id;
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.e(tag, "created");
            myCamera = Camera.open(cameraId);
            try {
                myCamera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                if(null != myCamera){
                    myCamera.release();
                    myCamera = null;
                }
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            Log.e(tag, "changed");
            if(isPreview){
                myCamera.stopPreview();
            }
            if(null != myCamera){
                Camera.Parameters myParam = myCamera.getParameters();
                myParam.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式

                //设置大小和方向等参数
                myParam.setPictureSize(1280, 720);
                myParam.setPreviewSize(1280, 720);
                //myParam.set("rotation", 90);
                myCamera.setDisplayOrientation(90);
                myParam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                myCamera.setParameters(myParam);
                myCamera.startPreview();
                myCamera.autoFocus(null);
                myCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        Camera.Parameters parameters = myCamera.getParameters();
                        int width = parameters.getPreviewSize().width;
                        int height = parameters.getPreviewSize().height;
                        YuvImage yuv = new YuvImage(bytes, parameters.getPreviewFormat(), width, height, null);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 90, out);
                        final byte[] data = out.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        // todo do something about bitmap
                        if (gesture.gestureEvent(bitmap)) {
                            listener.onPunch();
                        }
                    }
                });
                isPreview = true;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.e(tag, "destroyed");
            if(null != myCamera)
            {
                myCamera.setPreviewCallback(null);

                myCamera.stopPreview();
                isPreview = false;
                myCamera.release();
                myCamera = null;
            }
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            double width = expression.getWidth();
            Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.faint)).getBitmap();
            double height = width * bitmap.getHeight() / bitmap.getWidth();
            emotion.setImageResource(R.drawable.faint);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) emotion.getLayoutParams();
            lp.width = (int) width;
            lp.height = (int) height;
            double[] target = expression.getLeftEye();
            double[] src = {352, 560};
            lp.setMargins((int)(target[0] - src[0]), (int)(target[1] - src[1]), 0, 0);
            emotion.setLayoutParams(lp);
            emotion.setVisibility(View.VISIBLE);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
                    alphaAnim.setDuration(1000);
                    alphaAnim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            emotion.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    emotion.startAnimation(alphaAnim);
                }
            }, 1000);
        }
    };

    public interface ActionListener {
        void onPunch();
    }
    public ActionListener listener = new ActionListener() {
        private boolean lock = false;
        @Override
        public void onPunch() {
            // todo do something when punched
            if (lock)
                return;

            lock = true;
            Toast.makeText(InteractVideoActivity.this, "punched!", Toast.LENGTH_SHORT).show();
            expression.detectFace(createVideoThumbnail(),
                    new Expression.MyCallBack() {
                        @Override
                        public void onFaceDetected() {
                            handler.sendEmptyMessage(0);
                            lock = false;
                        }
                    });
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(tag, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(tag, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }
}
