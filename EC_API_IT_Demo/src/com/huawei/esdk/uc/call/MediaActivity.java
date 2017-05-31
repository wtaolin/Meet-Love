package com.huawei.esdk.uc.call;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.conf.ConferenceManageActivity;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.VideoFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;
import com.huawei.utils.StringUtil;
import com.huawei.voip.data.EventData;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MediaActivity extends BaseActivity implements OnClickListener,
        BaseReceiver
{
    private static final int UPDATE_VIEW = 1;

    private static final int GOTO_CONF_VIEW = 4;

    private static final int GOTO_VIDEO_VIEW = 6;

    private static final int VIDEOINVITE = 11;

    private static final int UPDATE_VIDEO_FAILED = 13;

    private static final int DECLINE_VIDEO = 14;

    private static final int TIME_OUT = 30 * 1000;

    private ImageView ivCallLog;
    
    /**
     * 通话界面头像
     */
    private ImageView callHeadPhoto;

    private TextView tvDisplayName;

    private TextView tvNumber;

    private AlertDialog videoInviteDialog;

    private Button btnCancel;

    private Button btnAccept;

    private Button btnRefuse;

    private ImageButton btnCallKeeping;

    private ImageButton btnMute;

    // private ImageButton btnLoadVoice;

    private ImageButton btnDial;

    private ImageButton btnVideo;

    private View callTwinceView;

    private EditText edTwiceCodeNumber;

    private String callNumber;

    private String displayName;
    
    private ContactHeadFetcher headFetcher;

    /**通话用户的信息*/
    private PersonalContact personalContact;

    private boolean needMakeCall;

    private int[] twicePhoneIds =
    { R.id.callZero, R.id.callOne, R.id.callTwo, R.id.callThree, R.id.callFour,
            R.id.callFive, R.id.callSix, R.id.callSeven, R.id.callEight,
            R.id.callNine, R.id.callX, R.id.callJ };

    private String[] twiceCmdCode =
    { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#" };

    private final String[] sipBroadcast = new String[]
    { VoipFunc.CALL_CONNECTED, VoipFunc.CALL_HOLD, VoipFunc.CALL_HOLD_FAILED,
            VoipFunc.GOTO_CONF_VIEW, VoipFunc.VIDEO_INVITE,
            VoipFunc.VIDEO_ADD_SUCESS, VoipFunc.VIDEO_ADD_FAILED,
            VoipFunc.CALL_CLOSED };

    @Override
    public void onReceive(String action, BaseData arg1)
    {
        if (VoipFunc.CALL_CONNECTED.equals(action))
        {
            // 接通
            if (VoipFunc.getIns().isVideo())
            {
                sendMessage(GOTO_VIDEO_VIEW, null);
            }
            else
            {
                sendMessage(UPDATE_VIEW, null);
            }
        }
        else if (VoipFunc.CALL_HOLD.equals(action))
        {
            sendMessage(UPDATE_VIEW, null);
        }
        else if (VoipFunc.CALL_HOLD_FAILED.equals(action))
        {
            sendMessage(UPDATE_VIEW, null);
        }
        else if (VoipFunc.VIDEO_INVITE.equals(action))
        {
            sendMessage(VIDEOINVITE, null);
        }
        else if (VoipFunc.GOTO_CONF_VIEW.equals(action))
        {
            sendMessage(GOTO_CONF_VIEW, ((EventData) arg1).getRawData());
        }
        else if (VoipFunc.VIDEO_ADD_SUCESS.equals(action))
        {
            sendMessage(GOTO_VIDEO_VIEW, null);
        }
        else if (VoipFunc.VIDEO_ADD_FAILED.equals(action))
        {
            sendMessage(UPDATE_VIDEO_FAILED, null);
        }
        else if (VoipFunc.CALL_CLOSED.equals(action))
        {
            finish();
        }
    }

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UPDATE_VIEW:
                    updateView();
                    break;
                case GOTO_VIDEO_VIEW:
                    updateView();
                    skipVideoActivity();
                    break;
                case UPDATE_VIDEO_FAILED:

                    btnVideo.setEnabled(true);
                    break;
                case VIDEOINVITE:
                    showSkipVideoDialog();
                    break;
                case GOTO_CONF_VIEW:
                    skipActivity((String) msg.obj);
                    break;
                case DECLINE_VIDEO:
                    closeVideoInviteDialog();
                    VideoFunc.getIns().declineVideoInvite();
                    VoipFunc.getIns().setVideo(false);
                    break;

                default:
                    break;
            }
        };
    };

    private void sendMessage(int what, Object obj)
    {
        Message msg = handler.obtainMessage(what, obj);
        handler.sendMessage(msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        initDate();
        initView();
    }

    /**
     * 判断是否呼叫，和头像加载的逻辑
     *头像逻辑：若拨打电话，PersonalContact从ContactAdapter获取；接听电话PersonalContact从CallSession获取，加载头像不会出问题。
     * author lwx302895
     * time 2016-03-18
     */
    private void initDate()
    {
    	headFetcher = new ContactHeadFetcher(MediaActivity.this);
        callHeadPhoto = (ImageView) findViewById(R.id.avatar);
        tvDisplayName = (TextView) findViewById(R.id.dispalyName);
        tvNumber = (TextView) findViewById(R.id.number);
    	
        registerSipCallback();

        // 是否要在这里拨打电话
        needMakeCall = getIntent().getBooleanExtra("needMakeCall",
                false);
        callNumber = getIntent().getStringExtra("callNumber");
        displayName = getIntent().getStringExtra("displayName");

        boolean isVideo = getIntent().getBooleanExtra("isVideo", false);

        boolean isHeader = getIntent().getBooleanExtra("showHeader", false);
        if (needMakeCall)
        {
            boolean isVoipCallSuccess = VoipFunc.getIns().doVoipCall(
                    callNumber, isVideo);

            if (!isVoipCallSuccess)
            {
                Toast.makeText(this, R.string.call_voip_fail,
                        Toast.LENGTH_SHORT).show();
            }
        }

        String name = "";
        String number = "";

        if (isHeader)
        {
            //从ContactAdapter中获取PersonalContact,加载头像
            personalContact = VoipFunc.getIns().getCallPersonal();
            if (personalContact != null)
            {
                headFetcher.loadHead(personalContact, callHeadPhoto,false);
                name = displayName = ContactFunc.getIns().getDisplayName(personalContact);
                number = personalContact.getBinderNumber();
            }
        }
        else
        {
            //从CallSession获取到用户信息，加载头像
            personalContact = VoipFunc.getIns().getCurrentCallPerson();
            if (personalContact != null)
            {
                headFetcher.loadHead(personalContact, callHeadPhoto,false);
                name = VoipFunc.getIns().getCurrentCallPersonName();
                number = VoipFunc.getIns().getCurrentCallNumber();
            }
        }
//        String name = VoipFunc.getIns().getCurrentCallPersonName();
//        String number = VoipFunc.getIns().getCurrentCallNumber();
        if (!TextUtils.isEmpty(name))
        {
            displayName = name;
        }
        if (!TextUtils.isEmpty(number))
        {
            callNumber = number;
        }
        tvDisplayName.setText(displayName);
        tvNumber.setText(callNumber);
    }

    private void initView()
    {
        ivCallLog = (ImageView) findViewById(R.id.iv_callLog);

        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnAccept = (Button) findViewById(R.id.btn_accept);
        btnRefuse = (Button) findViewById(R.id.btn_refuse);

        btnCallKeeping = (ImageButton) findViewById(R.id.btn_callkeeping);
        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        // btnLoadVoice = (ImageButton) findViewById(R.id.btn_loadvice);
        btnDial = (ImageButton) findViewById(R.id.btn_dial);
        btnVideo = (ImageButton) findViewById(R.id.btn_video);

        callTwinceView = findViewById(R.id.call_twince_view);

        btnCallKeeping.setVisibility(View.VISIBLE);
        btnMute.setVisibility(View.VISIBLE);
        // btnLoadVoice.setVisibility(View.VISIBLE);
        btnDial.setVisibility(View.VISIBLE);
        if (ContactLogic.getIns().getAbility().isVideoCallAbility())
        {
            btnVideo.setVisibility(View.VISIBLE);
        }
        else
        {
            btnVideo.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(this);
        btnAccept.setOnClickListener(this);
        btnRefuse.setOnClickListener(this);

        btnCallKeeping.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        // btnLoadVoice.setOnClickListener(this);
        btnDial.setOnClickListener(this);
        btnVideo.setOnClickListener(this);

        initNumber();

        updateView();

    }

    private void registerSipCallback()
    {
        VoipFunc.getIns().registerBroadcast(this, sipBroadcast);
    }

    private void unRegisterSipCallback()
    {
        VoipFunc.getIns().unRegisterBroadcast(this, sipBroadcast);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_cancel:

                callTwinceView.setVisibility(View.GONE);
                VoipFunc.getIns().hangup();
                VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_INIT);
                finish();
                break;

            case R.id.btn_accept:

                callTwinceView.setVisibility(View.GONE);
                VoipFunc.getIns().answer(VoipFunc.getIns().isVideo());
                break;

            case R.id.btn_refuse:

                callTwinceView.setVisibility(View.GONE);
                VoipFunc.getIns().hangup();
                VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_INIT);
                finish();
                break;

            case R.id.btn_callkeeping:

                callTwinceView.setVisibility(View.GONE);
                VoipFunc.getIns().hold(!VoipFunc.getIns().isHold());

                break;

            case R.id.btn_mute:

                callTwinceView.setVisibility(View.GONE);

                if (VoipFunc.getIns().mute(VoipFunc.MICROPHONE,
                        !VoipFunc.getIns().isMute()))
                {
                    VoipFunc.getIns()
                            .setMuteStatus(!VoipFunc.getIns().isMute());
                    updateView();
                }
                break;

            // case R.id.btn_loadvice:

            // // 调用系统接口，所以此处接口不可以

            // callTwinceView.setVisibility(View.GONE);
            // VoipFunc.getIns().switchAudioRoute();
            // updateView();
            //
            // break;

            case R.id.btn_dial:

                if (callTwinceView.getVisibility() == View.GONE)
                {
                    callTwinceView.setVisibility(View.VISIBLE);
                }
                else
                {
                    callTwinceView.setVisibility(View.GONE);
                }
                break;

            case R.id.btn_video:

                callTwinceView.setVisibility(View.GONE);

                if (VideoFunc.getIns().openVideo())
                {
                    btnVideo.setEnabled(false);
                }

                break;

            default:
                break;
        }
    }

    private final OnClickListener call_numberListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // MediaUtil.getInstance().playSound(v.getId());
            int index = StringUtil.queryElement(twicePhoneIds, v.getId(),
                    twicePhoneIds.length);
            if (index != -1)
            {
                showNumber(index, twiceCmdCode[index]);
            }
        }

        private void showNumber(int cmd, String show)
        {
            StringBuffer number = new StringBuffer(edTwiceCodeNumber.getText()
                    .toString());
            number.append(show);
            edTwiceCodeNumber.setText(number.toString());
            edTwiceCodeNumber.setSelection(number.length());
            VoipFunc.getIns().dialpadInTalking(cmd);
        }
    };

    private void initNumber()
    {
        edTwiceCodeNumber = (EditText) findViewById(R.id.edit_number);
        edTwiceCodeNumber.setFocusable(false);

        RelativeLayout[] layouts = new RelativeLayout[twicePhoneIds.length];

        for (int i = 0; i < twicePhoneIds.length; i++)
        {
            layouts[i] = (RelativeLayout) findViewById(twicePhoneIds[i]);
            layouts[i].setOnClickListener(call_numberListener);
        }
    }

    private void updateView()
    {
        updateCallLogoView();
        updateCallbutton();
        updateFuncImageButton();
    }

    private void updateCallLogoView()
    {
        if (VoipFunc.getIns().getVoipStatus() == VoipFunc.STATUS_CALLING)
        {
            if (VoipFunc.getIns().isConf())
            {
                ivCallLog.setImageResource(R.drawable.conference_logo_4);
            }
            else if (VoipFunc.getIns().isVideo())
            {
                ivCallLog.setImageResource(R.drawable.video_call_4);
            }
            else
            {
                ivCallLog.setImageResource(R.drawable.call_logo4);
            }
        }
        else if (VoipFunc.getIns().getVoipStatus() == VoipFunc.STATUS_TALKING)
        {
            if (VoipFunc.getIns().isHold())
            {
                ivCallLog.setImageResource(R.drawable.call_logo_callkeep);
            }
            else if (VoipFunc.getIns().isMute())
            {
                ivCallLog.setImageResource(R.drawable.call_logo_mute);
            }
            else if (VoipFunc.getIns().isVideo())
            {
                ivCallLog.setImageResource(R.drawable.video_call_4);
            }
            else if (VoipFunc.getIns().isSpeaker())
            {
                ivCallLog.setImageResource(R.drawable.call_logo_loadvoice);
            }
            else
            {
                ivCallLog.setImageResource(R.drawable.call_logo4);
            }
        }

    }

    private void updateCallbutton()
    {
        switch (VoipFunc.getIns().getVoipStatus())
        {
            case VoipFunc.STATUS_CALLING:

                if (VoipFunc.CALL_OUT == VoipFunc.getIns().getCallMode())
                {
                    btnCancel.setText(R.string.btn_cancel);
                    btnCancel.setVisibility(View.VISIBLE);
                    btnAccept.setVisibility(View.GONE);
                    btnRefuse.setVisibility(View.GONE);
                }
                else if (VoipFunc.CALL_COME == VoipFunc.getIns().getCallMode())
                {
                    btnCancel.setVisibility(View.GONE);
                    btnAccept.setVisibility(View.VISIBLE);
                    btnRefuse.setVisibility(View.VISIBLE);
                }

                break;
            case VoipFunc.STATUS_TALKING:

                btnCancel.setText(R.string.call_down);
                btnCancel.setVisibility(View.VISIBLE);
                btnAccept.setVisibility(View.GONE);
                btnRefuse.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    private void updateFuncImageButton()
    {
        if (VoipFunc.CALL_COME == VoipFunc.getIns().getCallMode())
        {
            btnVideo.setVisibility(View.GONE);
        }
        switch (VoipFunc.getIns().getVoipStatus())
        {
            case VoipFunc.STATUS_CALLING:

                setFuncBtnEnable(false);
                break;

            case VoipFunc.STATUS_TALKING:

                setFuncBtnEnable(true);

                if (!VoipFunc.getIns().isHold())
                {
                    btnCallKeeping
                            .setImageResource(R.drawable.icon_callkeep_normal);
                }
                else
                {
                    btnCallKeeping
                            .setImageResource(R.drawable.icon_resume_callkeep_normal);
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
                break;

            default:
                break;
        }
    }

    private void setFuncBtnEnable(boolean enable)
    {
        btnCallKeeping.setEnabled(enable);
        btnMute.setEnabled(enable);
        // btnLoadVoice.setEnabled(enable);
        btnDial.setEnabled(enable);
        btnVideo.setEnabled(enable);
        if (enable && VoipFunc.CTD != VoipFunc.getIns().getCallType())
        {
            btnVideo.setEnabled(!VoipFunc.getIns().isVideo());
        }
    }

    private void skipActivity(String confId)
    {
        Intent intent = new Intent(this, ConferenceManageActivity.class);
        intent.putExtra("confId", confId);
        startActivity(intent);
        finish();
    }

    private void skipVideoActivity()
    {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("makeCallTag", needMakeCall);
        startActivity(intent);
        finish();
    }

    private void showSkipVideoDialog()
    {
        if (videoInviteDialog == null)
        {
            videoInviteDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.video_invite)
                    .setNegativeButton(R.string.call_refuse,
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which)
                                {
                                    closeVideoInviteDialog();
                                    VideoFunc.getIns().declineVideoInvite();
                                    VoipFunc.getIns().setVideo(false);
                                }
                            })
                    .setPositiveButton(R.string.agree,
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which)
                                {
                                    VideoFunc.getIns().agreeVideoUpgradte();
                                    skipVideoActivity();
                                    closeVideoInviteDialog();
                                }
                            }).setCancelable(false).show();
        }
        // 延时30秒,时间不到关闭对话框.
        handler.sendEmptyMessageDelayed(DECLINE_VIDEO, TIME_OUT);
    }

    protected void closeVideoInviteDialog()
    {
        if (videoInviteDialog != null)
        {
            videoInviteDialog.dismiss();
            videoInviteDialog = null;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unRegisterSipCallback();
    }

	@Override
	public void initializeData()
    {
		// TODO Auto-generated method stub
	}

	@Override
	public void initializeComposition()
    {
		// TODO Auto-generated method stub
	}

	@Override
	public void clearData()
    {
		// TODO Auto-generated method stub
	}

    /**
     * 避免拨打界面点击系统返回按键后，无法再次回到拨打界面
     */
    @Override
    public void onBackPressed()
    {
//        super.onBackPressed();
    }
}
