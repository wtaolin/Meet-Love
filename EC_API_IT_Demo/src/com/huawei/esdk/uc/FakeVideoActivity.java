package com.huawei.esdk.uc;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Gallen on 2016/10/15.
 */
public class FakeVideoActivity extends Activity{
    private static final String tag="FakeVideoActivity";
    private Camera myCamera = null;
    private SurfaceView mPreviewSV = null;
    private SurfaceHolder mySurfaceHolder = null;

    private VideoView image;

    private boolean handling = false;
    private byte[] data = null;
    private final int TIME = 1000;
    private Timer timer = null;
    private TimerTask task = null;

    private ImageView warn1, warn2, warn3;
    private ImageView emotion;

    private RequestQueue requestQueue;

    private SeekBar seekBar;

    private Handler degree_handler = new Handler() {
        public void handleMessage(Message msg) {
            Log.e(tag, "anger: " + msg.what);
            ObjectAnimator animation = (ObjectAnimator) seekBar.getTag();
            if (animation != null)
                animation.cancel();

            animation = ObjectAnimator.ofInt(seekBar, "progress", seekBar.getProgress() + msg.what);
            animation.setDuration(6000); // 6 second
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
            seekBar.setTag(animation);
        }
    };

    private Handler handler = new Handler() {
        private int count = 0;
        private View view = null;
        private ImageView[] images = new ImageView[5];
        public void handleMessage(Message msg) {
            Log.e(tag, "take picture");
            if (view == null) {
                view = LayoutInflater.from(FakeVideoActivity.this).inflate(R.layout.images, null, false);
                int[] ids = {R.id.img1, R.id.img2, R.id.img3};
                for (int i = 0; i < 3; i++) {
                    images[i] = (ImageView) view.findViewById(ids[i]);
                }
            }
            if (data == null)
                return;
            handling = true;
            Camera.Parameters parameters = myCamera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;
            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            images[count++].setImageBitmap(bitmap);

            if(count == 3) {
                view.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                view.buildDrawingCache();
                Bitmap smallBitmap = view.getDrawingCache();

                count = 0;
                view = LayoutInflater.from(FakeVideoActivity.this).inflate(R.layout.images, null, false);
                int[] ids = {R.id.img1, R.id.img2, R.id.img3};
                for (int i = 0; i < 3; i++) {
                    images[i] = (ImageView) view.findViewById(ids[i]);
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(smallBitmap, 0, 0, smallBitmap.getWidth(), smallBitmap.getHeight(), matrix, true);

                int size = rotatedBitmap.getWidth() * rotatedBitmap.getHeight() * 4;
                // 创建一个字节数组输出流,流的大小为size
                ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
                // 设置位图的压缩格式，质量为100%，并放入字节数组输出流中
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 50, baos);
                // 将字节数组输出流转化为字节数组byte[]
                final byte[] imagedata = baos.toByteArray();

                Log.e(tag, "length: " + imagedata.length);
                StringRequest stringRequest = new StringRequest(Request.Method.POST,
                        "https://api.projectoxford.ai/emotion/v1.0/recognize",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.e(tag, "response -> " + response);
                                JSONArray result;
                                try {
                                    result = new JSONArray(response);
                                    double anger, happiness;
                                    int score = 0;
                                    for (int i = 0; i < 3 && i < result.length(); i++) {
                                        JSONObject scores = result.optJSONObject(i).optJSONObject("scores");
                                        anger = scores.optDouble("anger");
                                        happiness = scores.optDouble("happiness");
                                        score += (int)((anger-happiness) * 10000);
                                    }
                                    degree_handler.sendEmptyMessage(score);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(tag, error.getMessage(), error);
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> mHeaders = new HashMap<String, String>();
                        mHeaders.put("Content-Type", "application/octet-stream");
                        mHeaders.put("Ocp-Apim-Subscription-Key", "88c8f9f77238458dad06674b9cfec8fb");
                        return mHeaders;
                    }

                    @Override
                    public byte[] getBody() {
                        return imagedata;
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/octet-stream";
                    }
                };
                requestQueue.add(stringRequest);
            }

            handling = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(tag, "onCreate");
        //设置全屏无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window myWindow = this.getWindow();
        myWindow.setFlags(flag, flag);

        setContentView(R.layout.activity_fake_video);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setEnabled(false);
        seekBar.setProgress((int)(0.5 * seekBar.getMax()));

        image = (VideoView)findViewById(R.id.image);
        //初始化SurfaceView
        mPreviewSV = (SurfaceView)findViewById(R.id.video);
        mySurfaceHolder = mPreviewSV.getHolder();
        mySurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);//translucent半透明 transparent透明
        mySurfaceHolder.addCallback(new myCallBack(Camera.CameraInfo.CAMERA_FACING_BACK));
        mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        image.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.default_video));
        image.start();
        image.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                image.seekTo(0);
                if (!image.isPlaying())
                    image.start();
            }
        });
        image.setZOrderOnTop(true);

        findViewById(R.id.interact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTimer();
                myCamera.setPreviewCallback(null);
                myCamera.stopPreview();
                myCamera.release();
                myCamera = null;
                Intent in = new Intent(FakeVideoActivity.this, InteractVideoActivity.class);
                startActivity(in);
                finish();
            }
        });
        findViewById(R.id.hangoff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        warn1 = (ImageView)findViewById(R.id.img1);
        warn2 = (ImageView)findViewById(R.id.img2);
        warn3 = (ImageView)findViewById(R.id.img3);
        emotion = (ImageView)findViewById(R.id.emotion);
        emotion.setImageResource(R.drawable.happy);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 9000)
                    warn3.setVisibility(View.INVISIBLE);
                else
                    warn3.setVisibility(View.VISIBLE);
                if (progress < 7000)
                    warn2.setVisibility(View.INVISIBLE);
                else
                    warn2.setVisibility(View.VISIBLE);
                if (progress < 5000)
                    warn1.setVisibility(View.INVISIBLE);
                else
                    warn1.setVisibility(View.VISIBLE);

                if (progress > 7500)
                    emotion.setImageResource(R.drawable.angry);
                else if (progress > 5000)
                    emotion.setImageResource(R.drawable.unhappy);
                else if (progress > 2500)
                    emotion.setImageResource(R.drawable.happy);
                else
                    emotion.setImageResource(R.drawable.smile);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        restartTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimer();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void restartTimer() {
        stopTimer();
        timer = new Timer();
        task = new TimerTask(){
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        timer.schedule(task, TIME, TIME);
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
                        if (!handling)
                            data = bytes;
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
}
