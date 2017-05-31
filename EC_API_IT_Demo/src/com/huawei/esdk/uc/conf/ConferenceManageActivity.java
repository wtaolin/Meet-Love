package com.huawei.esdk.uc.conf;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.conference.CtcMemberEntity;
import com.huawei.conference.entity.CreateConferenceMsgAck;
import com.huawei.conference.entity.CtcEntity;
import com.huawei.conference.entity.GetMemberMsgAck;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ConferenceFunc.ConferenceReceiveData;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.utils.StringUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ConferenceManageActivity extends Activity implements
        OnClickListener
{

    private TextView tvSubject;

    private ListView listView;

    private ImageView btnAdd;

    private ImageView btnShare;

    private ImageView btnVideo;

    private ImageView btnMute;

    // private ImageView btnLoadvoice;

    private ImageView btnExit;

    private LinearLayout llFuncBottom;

    private LinearLayout llJoinBottom;

    private Button btnJoin;

    private Dialog exitDialog;

    private Dialog onLongClickDialog;

    private ConfManageItemAdapter adapter;

    private int resultId;

    private String subject;

    private ConferenceEntity confEntity;

    //防止主持人退出会议后，再次进入会议信息不全，缺少number
    private ConferenceEntity saveConfHostEntity;

    private List<ConferenceMemberEntity> members = new ArrayList<ConferenceMemberEntity>();

    private String confId;

    private static final int CONF_CREAT_FAIL = 1;

    private static final int CONF_FULL_INFO = 2;

    private static final int UPDATE_MEMBER = 3;

    private static final int CONF_END = 4;

    private static final int CONF_BOTTOM_UPDATE = 5;

    private String[] actions =
    { ConferenceFunc.CREATE_CONFERENCE_NOTIFY,
            ConferenceFunc.UPDATA_CONFERENCE_MEMBER,
            ConferenceFunc.CONF_FULL_INFO, ConferenceFunc.CONF_MEMBER_STATUS,
            ConferenceFunc.CONF_END };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_conference_manage);

        initView();
        initData();
    }

    private void initView()
    {
        tvSubject = (TextView) findViewById(R.id.subject);
        listView = (ListView) findViewById(R.id.list_members);

        btnAdd = (ImageView) findViewById(R.id.add);
        btnShare = (ImageView) findViewById(R.id.share);
        btnVideo = (ImageView) findViewById(R.id.video);
        btnMute = (ImageView) findViewById(R.id.mute);
        // btnLoadvoice = (ImageView) findViewById(R.id.loadvoice);
        btnExit = (ImageView) findViewById(R.id.exit);
        llFuncBottom = (LinearLayout) findViewById(R.id.bottom);
        llJoinBottom = (LinearLayout) findViewById(R.id.bottom_join);
        llFuncBottom.setVisibility(View.GONE);
        llJoinBottom.setVisibility(View.GONE);
        btnJoin = (Button) findViewById(R.id.join);
        btnJoin.setOnClickListener(this);

        btnAdd.setOnClickListener(this);
        btnShare.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        // btnLoadvoice.setOnClickListener(this);
        btnExit.setOnClickListener(this);

        listView.setOnItemLongClickListener(new OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3)
            {
                if (isHost())
                {

                    ConferenceMemberEntity memberEntity = (ConferenceMemberEntity) arg0
                            .getItemAtPosition(arg2);
                    showOnLongClickDialog(memberEntity);
                }
                return false;
            }
        });
    }

    private void initData()
    {
        confId = getIntent().getStringExtra(IntentData.CONFERENCE_ID);

        if (TextUtils.isEmpty(confId))
        {
            resultId = getIntent().getIntExtra("result_id", 0);
            subject = getIntent().getStringExtra("subject");
        }

        confEntity = (ConferenceEntity) getIntent().getSerializableExtra(
                IntentData.CTCENTITY);
        if (confEntity != null)
        {
            confId = confEntity.getConfId();
            ConferenceFunc.getIns().reportTerminalType(confId);
            ConferenceFunc.getIns().requestConferenceMem(confId);
            if (confEntity.getConfMemberList() != null)
            {
                members = confEntity.getConfMemberList();
            }
        }

        adapter = new ConfManageItemAdapter(this);
        listView.setAdapter(adapter);
        adapter.setData(members);

        tvSubject.setText(subject);

        ConferenceFunc.getIns().registerBroadcast(receiver, actions);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.add:

                if (confEntity != null)
                {
                    Intent intent = new Intent(ConferenceManageActivity.this,
                            ConferenceAddMemberActivity.class);
                    intent.putExtra("confId", confEntity.getConfId());
                    ArrayList<ConferenceMemberEntity> confMembers = new ArrayList<ConferenceMemberEntity>();
                    confMembers.addAll(members);
                    intent.putExtra("members", confMembers);
                    startActivityForResult(intent, 0);
                }

                break;
            case R.id.share:

                ConferenceFunc.getIns().requestUpgradeDataConf(confId);

                break;
            case R.id.video:

                // ConferenceFunc.getIns().requestUpgradeDataConf(confId);

                break;
            case R.id.mute:

                if (VoipFunc.getIns().mute(VoipFunc.MICROPHONE,
                        !VoipFunc.getIns().isMute()))
                {
                    VoipFunc.getIns()
                            .setMuteStatus(!VoipFunc.getIns().isMute());
                    if (VoipFunc.getIns().isMute())
                    {
                        btnMute.setImageResource(R.drawable.media_phonic_select);
                    }
                    else
                    {
                        btnMute.setImageResource(R.drawable.media_mute_select);
                    }
                }
                break;
            // case R.id.loadvoice:

            // VoipFunc.getIns().switchAudioRoute();
            // break;
            case R.id.exit:

                showExitDialog();
                break;
            case R.id.join:

                ConferenceFunc.getIns().requestJoinConf(confEntity);
                //lwx302895一键入会后要把当前的Activity销毁，接听来电后会重新跳转到该Activity
                ConferenceManageActivity.this.finish();
                break;

            default:
                break;
        }
    }

    private void showOnLongClickDialog(final ConferenceMemberEntity memberEntity)
    {
        final List<String> items = new ArrayList<String>();
        int status = memberEntity.getStatus();
        switch (status)
        {
            case ConferenceFunc.STATUS_SUCCESS:
                items.add(getString(R.string.hangup));
                break;
            case ConferenceFunc.STATUS_INVITE:
                items.add(getString(R.string.hangup));
                break;
            default:
                items.add(getString(R.string.Join));
                break;
        }

        onLongClickDialog = new Dialog(this, R.style.Theme_dialog);
        onLongClickDialog.setContentView(R.layout.dialog_list_view);
        ListView listView = (ListView) onLongClickDialog
                .findViewById(R.id.dialogList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.dialog_list_item_view, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3)
            {
                onLongClickDialog.dismiss();
                String selectItem = (String) items.get(arg2);
                if (getString(R.string.hangup).equals(selectItem))
                {
                    ConferenceFunc.getIns().requestDelConfMember(
                            confEntity.getConfId(), memberEntity.getNumber());
                }
                else if (getString(R.string.Join).equals(selectItem))
                {
                    List<ConferenceMemberEntity> l = new ArrayList<ConferenceMemberEntity>();
                    l.add(memberEntity);
                    ConferenceFunc.getIns().requestAddMember(
                            confEntity.getConfId(),
                            ConferenceDataHandler.getIns().transToCtcMember(l));
                }
            }
        });
        onLongClickDialog.show();
    }

    private void showExitDialog()
    {
        if (exitDialog == null)
        {
            exitDialog = new Dialog(this, R.style.Theme_dialog);
            exitDialog.setContentView(R.layout.exit_dialog_view);
            Button endConf = (Button) exitDialog.findViewById(R.id.end);
            Button exitConf = (Button) exitDialog.findViewById(R.id.exit);
            Button cancel = (Button) exitDialog.findViewById(R.id.cancel);
            if (!isHost())
            {
                endConf.setVisibility(View.GONE);
            }
            endConf.setOnClickListener(exitClickListener);
            exitConf.setOnClickListener(exitClickListener);
            cancel.setOnClickListener(exitClickListener);
        }
        exitDialog.show();
    }

    private OnClickListener exitClickListener = new OnClickListener()
    {

        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.end:
                    ConferenceFunc.getIns().requestEndConference(confId);
                    exitDialog.dismiss();
                    break;
                case R.id.exit:

                    if (confEntity != null && isHost())
                    {
                        // 主持人离会
                        ConferenceFunc.getIns().requestDelConfMember(confId,
                                confEntity.getHost());
                    }
                    else
                    {
                        // 一般与会者离会
                        VoipFunc.getIns().hangup();
                    }
                    exitDialog.dismiss();
                    break;
                case R.id.cancel:
                    exitDialog.dismiss();
                    break;
                default:
                    break;
            }
        }
    };

    private boolean isHost()
    {
        if (confEntity != null)
        {
            return confEntity.getHostAccount().equals(
                    CommonVariables.getIns().getUserAccount());
        }
        return false;
    }

    protected boolean isInDataConf()
    {
        if (confEntity == null)
        {
            return false;
        }

        ConferenceMemberEntity self = confEntity.getSelfInConf();

        return self != null
                && self.isInDataConference()
                && (confEntity.getMediaType() == ConferenceEntity.MULTI_CONFERENCE_TYPE);
    }

    private BaseReceiver receiver = new BaseReceiver()
    {
        @Override
        public void onReceive(String action, BaseData baseData)
        {
            ConferenceReceiveData data = (ConferenceReceiveData) baseData;
            if (ConferenceFunc.CREATE_CONFERENCE_NOTIFY.equals(action))
            {
                BaseResponseData responseData = data.responseData;
                if (resultId == responseData.getBaseId())
                {
                    if (UCResource.REQUEST_OK == data.result
                            && responseData.getStatus() == ResponseCode.REQUEST_SUCCESS)
                    {
                        CreateConferenceMsgAck ack = (CreateConferenceMsgAck) responseData;
                        confId = ack.getConfId();
                    }
                    else
                    {
                        sendMessage(CONF_CREAT_FAIL, null);
                    }
                }
            }
            if (ConferenceFunc.UPDATA_CONFERENCE_MEMBER.equals(action))
            {
                BaseResponseData responseData = data.responseData;

                if (resultId == UCResource.REQUEST_OK && confEntity != null)
                {
                    // -30处理
                    if (responseData.getStatus() == ResponseCode.REQUEST_SUCCESS)
                    {
                        if (!(responseData instanceof GetMemberMsgAck))
                        {
                            return;
                        }

                        List<CtcMemberEntity> ctcMemberEntities = ((GetMemberMsgAck) responseData)
                                .getUsers();

                        if (null != ctcMemberEntities)
                        {
                            updateMemberStatus(ConferenceDataHandler.getIns()
                                    .transToConfMember(ctcMemberEntities));
                        }
                    }
                }
                if (updateBottomView())
                {
                    return;
                }
            }
            else if (ConferenceFunc.CONF_FULL_INFO.equals(action))
            {
                BaseResponseData responseData = data.responseData;
                if (UCResource.REQUEST_OK == data.result)
                {
                    if (responseData instanceof CtcEntity)
                    {
                        CtcEntity ctc = (CtcEntity) responseData;
                        sendMessage(
                                CONF_FULL_INFO,
                                ConferenceDataHandler.getIns()
                                        .transToConference(ctc,
                                                CommonUtil.NORMAL_TIME_TYPE));
                    }
                }
            }
            else if (ConferenceFunc.CONF_MEMBER_STATUS.equals(action))
            {
                sendMessage(UPDATE_MEMBER, ConferenceDataHandler.getIns()
                        .transToConfMember(data.ctcMemberEntities));
            }
            else if (ConferenceFunc.CONF_END.equals(action))
            {
                sendMessage(CONF_END, null);
            }
        }
    };

    private void sendMessage(int what, Object obj)
    {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;

        handler.sendMessage(msg);

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case CONF_CREAT_FAIL:
                    Toast.makeText(getBaseContext(),
                            getString(R.string.create_fail), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case CONF_FULL_INFO:
                    confEntity = (ConferenceEntity) msg.obj;
                    confId = confEntity.getConfId();
                    tvSubject.setText(confEntity.getSubject());
                    if (!TextUtils.isEmpty(confEntity.getHost()))
                    {
                        saveConfHostEntity = confEntity;
                        ConferenceFunc.getIns().setHostEntity(saveConfHostEntity);
                        updateMemberStatus(confEntity.getConfMemberList());
                    }
                    else
                    {
                        saveConfHostEntity = ConferenceFunc.getIns().getHostEntity();
                        //第二次之后进入如果没有主持人信息，何以获取第一次推送过来的信息
                        if (saveConfHostEntity != null && !TextUtils.isEmpty(saveConfHostEntity.getHost()) && TextUtils.isEmpty(confEntity.getHost()))
                        {
                            updateMemberStatus(saveConfHostEntity.getConfMemberList());
                        }
                        else
                        {
                            updateMemberStatus(confEntity.getConfMemberList());
                        }
                    }

                    break;
                case UPDATE_MEMBER:
                    updateMemberStatus((List<ConferenceMemberEntity>) (msg.obj));
                    if (updateBottomView())
                    {
                        return;
                    }
                    break;
                case CONF_END:
                    Toast.makeText(getBaseContext(),
                            getString(R.string.conf_end), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case CONF_BOTTOM_UPDATE:
                    updateBottomView();
                    boolean updateView = confEntity != null && members != null && members.size() > 0;
                    if (updateView)
                    {
                        adapter.setData(members);
                    }
                    break;
                default:
                    break;
            }
        };
    };

    private void updateMemberStatus(List<ConferenceMemberEntity> entities)
    {
        for (ConferenceMemberEntity member1 : entities)
        {
            boolean flag = true;
            ConferenceMemberEntity member2;
            for (int i = 0; i < members.size(); i++)
            {
                member2 = members.get(i);
                if ((member1.getConfMemEspaceNumber() != null && member1
                        .getConfMemEspaceNumber().equals(member2.getConfMemEspaceNumber()))
                        || member1.getNumber().equals(member2.getNumber()))
                {
                    member2.setStatus(member1.getStatus());
                    // members.set(i, member1);
                    members.set(i, member2);
                    flag = false;
                    break;
                }
            }
            if (flag)
            {
                members.add(member1);
            }
        }
        adapter.setData(members);

        if (confEntity != null)
        {
            ConferenceFunc.getIns().setCurConfEntity(confEntity);
            updateBottomView();
        }
    }

    @Override
    protected void onResume()
    {
//        updateBottomView();

        super.onResume();
        Message msg = new Message();
        msg.what = CONF_BOTTOM_UPDATE;
        handler.sendMessageDelayed(msg ,5000);
    }

    private boolean updateBottomView()
    {
        if (confEntity != null)
        {
            confEntity.setConfMemberList(members);
            ConferenceMemberEntity self = confEntity.getSelfInConf();
            if (!isConfControlEnable() && self != null &&
                    ConferenceMemberEntity.STATUS_LEAVE_CONF == self.getStatus())
            {
                llFuncBottom.setVisibility(View.GONE);
                llJoinBottom.setVisibility(View.VISIBLE);
                return true;
            }
            else
            {
                llFuncBottom.setVisibility(View.VISIBLE);
                llJoinBottom.setVisibility(View.GONE);
                return false;
            }

        }
        return true;
    }
    protected boolean isConfControlEnable()
    {
        return confEntity != null && confEntity.isConfControlEnable();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        ConferenceFunc.getIns().unRegisterBroadcast(receiver, actions);
    }

}
