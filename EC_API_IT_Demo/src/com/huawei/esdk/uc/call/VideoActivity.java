package com.huawei.esdk.uc.call;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.PersonalContact;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.VideoFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.voip.TupHelper;
import com.huawei.voip.data.OrientChange;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoActivity extends BaseActivity implements OnClickListener,
        BaseReceiver
{
    private static final String TAG = VideoActivity.class.getSimpleName();

    private static final int VIDEO_COLOSE = 1001;

    private static final int CLOSED = 1002;

    private static final int VIDEOUPDATE = 1003;
    
    private static final int VIDEO_ORIENT_CHANGE = 1016;

    private VideoFunc videoHolder;

    private TextView tvDisplayName;

    private TextView tvNumber;

    private FrameLayout localView;

    private FrameLayout remoteView;

    private ImageButton btnRemoveVideo;

    private ImageButton btnSwitchVideo;

    private ImageButton btnMute;

    // private ImageButton btnLoadVoice;

    // private ImageButton btnDial;

    private ImageButton btnClosed;

    private String[] sipActions;

    private Handler handler;

    /** 头像显示*/
    private ImageView ivHead;

    private ContactHeadFetcher headFetcher;

    @Override
    public void initializeData()
    {
        // 保持屏幕常亮
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        videoHolder = VideoFunc.getIns();
        initHandler();
        initSipReceiver();
    }

    private void initSipReceiver()
    {
        sipActions = new String[] {VoipFunc.VIDEO_REMOVE, 
                VoipFunc.CALL_CLOSED, VoipFunc.VIDEO_ADD_SUCESS,
                VoipFunc.REFRESH_LOCAL_VIEW, VoipFunc.REFRESH_REMOTE_VIEW,
                VoipFunc.VIDEO_CHANGE_ORIENT};
        VoipFunc.getIns().registerBroadcast(this, sipActions);
    }

    private void initHandler()
    {
        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                Logger.debug(CommonUtil.APPTAG, TAG + " | msg.what=" + msg.what);
                switch (msg.what)
                {
                case VIDEO_COLOSE:
                    skipMediaActivity();
                    break;
                case VIDEOUPDATE:
                    addSufaceView(true);
                    break;
                case CLOSED:
                    finish();
                    break;
                case VIDEO_ORIENT_CHANGE:
                    changeScreenOrient(msg.arg1);
                    break;
                default:
                    break;
                }
            }
        };
    }

    @Override
    public void clearData()
    {
        VoipFunc.getIns().unRegisterBroadcast(this, sipActions);
        //在界面destroy的时候,需要出栈.也就是此界面只能在栈顶.
        UCAPIApp.getApp().popWithoutFinish(this);
    }

    @Override
    public void initializeComposition()
    {
        setScreenOrient();
        Configuration config = getResources().getConfiguration();
        Logger.debug(CommonUtil.APPTAG, TAG + "|" + config.orientation);

        setContentView(R.layout.activity_video);

        tvDisplayName = (TextView) findViewById(R.id.dispalyName);
        tvNumber = (TextView) findViewById(R.id.number);
        ivHead = (ImageView) findViewById(R.id.avatar);
        headFetcher = new ContactHeadFetcher(VideoActivity.this);
        
        if (videoHolder.getOrient() == VideoFunc.PORTRAIT)
        {
            localView = (FrameLayout) findViewById(R.id.local_video);
        }
        else
        {
            localView = (FrameLayout) findViewById(R.id.local_video_landscape);
        }
        

        remoteView = (FrameLayout) findViewById(R.id.remote_video);

        btnRemoveVideo = (ImageButton) findViewById(R.id.btn_exitvideo);
        btnSwitchVideo = (ImageButton) findViewById(R.id.btn_switch);
        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        // btnLoadVoice = (ImageButton) findViewById(R.id.btn_loadvice);
        // btnDial = (ImageButton) findViewById(R.id.btn_dial);
        btnClosed = (ImageButton) findViewById(R.id.btn_hangup);

        PersonalContact currentContact = VoipFunc.getIns().getCallPersonal();
        if (currentContact != null)
        {
            headFetcher.loadHead(currentContact, ivHead, false);
            String displayName = ContactFunc.getIns().getDisplayName(currentContact);
            String callNumber = currentContact.getBinderNumber();
            tvDisplayName.setText(displayName);
            tvNumber.setText(callNumber );
        }
        else
        {
            PersonalContact contact = VoipFunc.getIns().getCurrentCallPerson();
            if (contact != null)
            {
                headFetcher.loadHead(contact, ivHead,false);
            }
            String displayName = VoipFunc.getIns().getCurrentCallPersonName();
            String callNumber = VoipFunc.getIns().getCurrentCallNumber();
            tvDisplayName.setText(displayName);
            tvNumber.setText(callNumber);
        }
//        headFetcher.loadHead(displayName,ivHead,false);

        btnRemoveVideo.setVisibility(View.VISIBLE);
        btnSwitchVideo.setVisibility(View.VISIBLE);
        btnMute.setVisibility(View.VISIBLE);
        // btnLoadVoice.setVisibility(View.VISIBLE);
        // btnDial.setVisibility(View.VISIBLE);
        btnClosed.setVisibility(View.VISIBLE);

        remoteView.setOnClickListener(this);

        btnRemoveVideo.setOnClickListener(this);
        btnSwitchVideo.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        // btnLoadVoice.setOnClickListener(this);
        // btnDial.setOnClickListener(this);
        btnClosed.setOnClickListener(this);

        updateView();
    }
    
    
    private void setScreenOrient()
    {
        int orient = videoHolder.getOrient();
        Logger.debug(CommonUtil.APPTAG, "orient:" + orient);
        final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
        if (VideoFunc.LANDSCAPE == orient)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else if (VideoFunc.REVERSE_LANDSCAPE == orient)
        {
            //在2.2中可以设置屏幕的方向为反转横屏:setRequestedOrientation(8);
            //因为系统没有公开出这个参数的设置，
            //不过在源码里面已经定义了SCREEN_ORIENTATION_REVERSE_LANDSCAPE这个参数
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO)
            {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            // 2.3或是以后可以直接设置
            else if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void updateView()
    {
        if (!VideoFunc.getIns().canSwitchCamera())
        {
            btnSwitchVideo.setVisibility(View.GONE);
        }

        if (VoipFunc.getIns().isMute())
        {
            btnMute.setImageResource(R.drawable.icon_phonic_normal);
        }
        else
        {
            btnMute.setImageResource(R.drawable.icon_mute_normal);
        }

        // if (!VoipFunc.getIns().isSpeaker())
        // {
        // btnLoadVoice
        // .setImageResource(R.drawable.icon_loadvoice_normal);
        // }
        // else
        // {
        // btnLoadVoice
        // .setImageResource(R.drawable.icon_loadvoice_focus);
        // }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        case R.id.remote_video:

            break;
        case R.id.btn_exitvideo:

            VoipFunc.getIns().closeVideo();
            skipMediaActivity();
            break;
        case R.id.btn_switch:

            VideoFunc.getIns().switchCamera();
            break;
        case R.id.btn_mute:

            if (VoipFunc.getIns().mute(VoipFunc.MICROPHONE,
                    !VoipFunc.getIns().isMute()))
            {
                VoipFunc.getIns().setMuteStatus(!VoipFunc.getIns().isMute());
                updateView();
            }
            break;
        // case R.id.btn_loadvice:
        //
        // // 调用系统接口，所以此处接口不可以
        // VoipFunc.getIns().switchAudioRoute();
        // updateView();
        // break;
        // case R.id.btn_dial:

        // break;
        case R.id.btn_hangup:

            VoipFunc.getIns().hangup();
            break;

        default:
            break;
        }
    }

    private void addSufaceView(ViewGroup container, SurfaceView child)
    {
        if (child == null)
        {
            return;
        }
        if (child.getParent() != null)
        {
            ViewGroup vGroup = (ViewGroup) child.getParent();
            vGroup.removeAllViews();
        }
        container.addView(child);
    }

    private void addSufaceView(boolean onlyLocal)
    {
        if (!onlyLocal)
        {
            addSufaceView(remoteView, videoHolder.getRemoteVideoView());
        }
        addSufaceView(localView, videoHolder.getLocalVideoView());
    }

    private void removeSurfaceView()
    {
        localView.removeAllViews();
        remoteView.removeAllViews();
    }

    private void skipMediaActivity()
    {
        Intent intent = new Intent(this, MediaActivity.class);
        //返回语音通话界面时，添加一个标识符来判断头像显示，从什么地方取头像
        boolean showHeaderTag = getIntent().getBooleanExtra("makeCallTag", false);
        intent.putExtra("showHeader", showHeaderTag);
        startActivity(intent);
        finish();
    }

    @Override
    public void onReceive(String id, BaseData data)
    {
        if (VoipFunc.VIDEO_REMOVE.equals(id))
        {
            handler.sendEmptyMessage(VIDEO_COLOSE);
        }
        else if (VoipFunc.CALL_CLOSED.equals(id))
        {
            handler.sendEmptyMessage(CLOSED);
        }
        else if (VoipFunc.VIDEO_ADD_SUCESS.equals(id))
        {
            handler.sendEmptyMessageDelayed(VIDEOUPDATE, 5000l);
        }
        else if (VoipFunc.REFRESH_LOCAL_VIEW.equals(id))
        {
            handler.sendEmptyMessage(VIDEOUPDATE);
        }
        else if (VoipFunc.VIDEO_CHANGE_ORIENT.equals(id))
        {
            if (data instanceof OrientChange)
            {
                Message msg = new Message();
                msg.what = VIDEO_ORIENT_CHANGE;
                msg.arg1 = ((OrientChange) data).getOrient();
                handler.sendMessage(msg);
            }
        }
    }

    @Override
    protected void onPause()
    {
        handler.removeMessages(VIDEOUPDATE);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        addSufaceView(false);
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        removeSurfaceView();
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        VoipFunc.getIns().unRegisterBroadcast(this, sipActions);
    }
    
    private void changeScreenOrient(int orient)
    {
        if (orient == videoHolder.getOrient())
        {
            Logger.info(CommonUtil.APPTAG, TAG + "The same orient!");
            return;
        }

        Logger.info(CommonUtil.APPTAG, TAG + "new orient:" + orient);
        videoHolder.setOrient(orient);
        setScreenOrient();

        localView.setVisibility(View.INVISIBLE);
        if (VideoFunc.PORTRAIT == orient)
        {
            localView = (FrameLayout) findViewById(R.id.local_video);
        }
        else
        {
            localView = (FrameLayout) findViewById(R.id.local_video_landscape);
        }

        addSufaceView(localView, videoHolder.getLocalVideoView());
        addSufaceView(remoteView, videoHolder.getRemoteVideoView());
    }

    /**
     * 避免视频界面点击系统返回按键后，无法再次回到视频界面
     */
    @Override
    public void onBackPressed()
    {
//        super.onBackPressed();
    }
}
