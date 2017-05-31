package com.huawei.esdk.uc.conf;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.constant.ResponseCodeHandler;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.conference.entity.CTCFieldNotifyData;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.ExecuteResult;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.conf.data.AddMemberEntity;
import com.huawei.esdk.uc.conf.data.MemberContant;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.log.TagInfo;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class ConferenceDetailActivity extends Activity
{
    /**与会者框*/
    private RelativeLayout detailArea;

    private Button joinConf;

    /**与会者相关*/
    private RelativeLayout confDetailMemberView;

    private DisplayMemberWidget memberWidget;

    private static final int UPDATE_CONF_MEM_VIEW = 5;

    private Handler detailHandler = new Handler()
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
//                case MemberContant.SKIP_MORE:
//                    Bundle bundle = new Bundle();
//                    bundle.putSerializable(
//                            ConferenceUtil.CONFERENCE_MEMBER_LIST,
//                            (Serializable) conference.getConfMemberList());
//                    bundle.putBoolean("fromConfDetail", true);
//                    ConferenceUtil.skipActivity(ConferenceDetailActivity.this,
//                            MoreAddMemberActivity.class, bundle);
//                    break;
//                case RES_ATTEND_CONFERENCE_FAILED:
//                    updateFirstLayout(true, false);
//                    break;
//                case RES_ATTEND_CONFERENCE_SUCCESS:
//                    updateFirstLayout(true, true);
//                    break;
                case UPDATE_CONF_MEM_VIEW:
                    updateMemberView();
                    break;
//                case RESPONSE_ERROR:
//                    ResponseErrorCodeHandler.getIns().handleError(
//                            (ResponseCodeHandler.ResponseCode) msg.getData().getSerializable(
//                                    ConferenceUtil.KEY_RESPONSE_CODE),
//                            msg.getData().getString(
//                                    ConferenceUtil.KEY_RESPONSE_DESC));
//                    break;
//                case REQUEST_ERROR:
//                    RequestErrorCodeHandler.getIns().handleReqErrorCode(
//                            (Integer) msg.getData().getSerializable(
//                                    CommonUtil.MESSAGE_TRANSFER_KEY));
//                    break;
                default:
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_detail);
        regBroadcast();

        initializeData();
        initializeComposition();
    }

    /** 会议列表页面接收的会议实体 */
    private ConferenceEntity conference;
    private DetailInfo info;

    public void initializeData()
    {
        conference = (ConferenceEntity) getIntent().getSerializableExtra(IntentData.CTCENTITY);
        info = new DetailInfo(conference);
    }

    private void updateMemberView()
    {
        if (!conference.getConfMemberList().isEmpty())
        {
            detailArea.setVisibility(View.VISIBLE);
            memberWidget.notifyView(getAddMemberEntities());
        }
        else
        {
            confDetailMemberView.setVisibility(View.GONE);
        }
    }

    public void initializeComposition()
    {
        if (conference == null)
        {
            finish();
            return;
        }

        TextView confDetailTime = (TextView) findViewById(R.id.conf_time_tv);
        TextView confTimeLength=(TextView) findViewById(R.id.conference_time_length);

        /**添加与会者 by wx303895*/
        detailArea = (RelativeLayout) findViewById(R.id.auto_location_detail);
        confDetailMemberView = (RelativeLayout) findViewById(R.id.conf_member_view);

        ExecuteResult result = ConferenceFunc.getIns().requestBookConfDetail(getConfId());

        if (conference.getConfMemberList().isEmpty())
        {
            if (ConferenceEntity.TYPE_BOOKING == info.type
                    && ConferenceEntity.STATUS_END != info.state)
            {
                if (result != null && result.isResult())
                {
                    detailArea.setVisibility(View.GONE);
                }
                else
                {
                    confDetailMemberView.setVisibility(View.GONE);
                }
            }
            else
            {
                confDetailMemberView.setVisibility(View.GONE);
            }
        }

        memberWidget = new DisplayMemberWidget(detailArea, this,getAddMemberEntities(),
                MemberContant.CONF_DITAIL_ACTIVITY, ConferenceHelper.compute());
        memberWidget.setHandler(detailHandler);

        //初始化时间，会议时长，与会号码，与会者
        setTitle(info.subject);
        confDetailTime.setText(DateUtil.formatSimple(conference.getBeginTime(),
                DateUtil.FMT_YMDHM_3));
        confTimeLength.setText(getConfTimeLength());

        joinConf = (Button) findViewById(R.id.join_conf_button);
        if (conference != null)
        {
            if (conference.getState() == ConferenceEntity.STATUS_TO_ATTEND || conference.getState() == ConferenceEntity.STATUS_END)
            {
                joinConf.setVisibility(View.GONE);
            }
        }
        joinConf.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                handleJoinConfClick();
            }
        });
        initAccessCodeView();


    }

    private String[] confBroadcast = new String[]
            {  ConferenceFunc.GET_BOOKCONFRENCE_DETAIL,ConferenceFunc.UPDATE_CONFERENCE_NOTIFY
            };

    public void regBroadcast()
    {
        ConferenceFunc.getIns().registerBroadcast(conferenceFuncReceiver,
                confBroadcast);
    }

    private BaseReceiver conferenceFuncReceiver = new BaseReceiver()
    {
//        }

        void handleSuccess(String action, ConferenceFunc.ConferenceReceiveData cr)
        {
            if (cr.respCode == null || ResponseCodeHandler.ResponseCode.REQUEST_SUCCESS.equals(cr.respCode))
            {
//                if (ConferenceFunc.JOIN_CONFERENCE.equals(action))
//                {
//                    detailHandler.sendEmptyMessage(RES_ATTEND_CONFERENCE_SUCCESS);
//                }
//                else if (ConferenceFunc.UPDATE_CONFERENCE_NOTIFY.equals(action))
//                {
//                    if (CTCFieldNotifyData.CONF_END == cr.confStatus)
//                    {
//                        ActivityStack.getIns().popup(ConferenceDetailActivity.this);
//                    }
//                }
                if (ConferenceFunc.GET_BOOKCONFRENCE_DETAIL.equals(action))
                {
                    detailHandler.sendEmptyMessage(UPDATE_CONF_MEM_VIEW);
                }
//                else if (ConferenceFunc.CONF_INFO_FULL_NOTIFY.equals(action))
//                {
//                    Logger.debug(TagInfo.APPTAG, "CONF_INFO_FULL_NOTIFY");
//                    gotoConfView();
//                }
            }
//            else if (!ConferenceFunc.GET_BOOKCONFRENCE_DETAIL.equals(action))
//            {
//                if (ConferenceFunc.JOIN_CONFERENCE.equals(action))
//                {
//                    detailHandler.sendEmptyMessage(RES_ATTEND_CONFERENCE_FAILED);
//                }
//
//                detailHandler.sendMessage(ConferenceUtil.getMessageByTransfer(
//                        cr.respCode, cr.describe, RESPONSE_ERROR));
//            }
        }

//        void handleFail(String action, ConferenceReceiveData cr)
//        {
//            if (ConferenceFunc.JOIN_CONFERENCE.equals(action))
//            {
//                detailHandler.sendEmptyMessage(RES_ATTEND_CONFERENCE_FAILED);
//            }
//
//            detailHandler.sendMessage(ConferenceUtil.getMessageByTransfer(
//                    cr.result, REQUEST_ERROR));
//        }

        @Override
        public void onReceive(String action, BaseData data)
        {
//            if (checkInvalid(data))
//            {
//                return;
//            }

            ConferenceFunc.ConferenceReceiveData cr = (ConferenceFunc.ConferenceReceiveData) data;
//            if (checkNoInvalid(cr))
//            {
//                return;
//            }

//            DialogCache.getIns().close();

            if (UCResource.REQUEST_OK == cr.result)
            {
                handleSuccess(action, cr);
            }
//            else if (!ConferenceFunc.GET_BOOKCONFRENCE_DETAIL.equals(action))
//            {
//                handleFail(action, cr);
//            }
        }
    };

    private List<AddMemberEntity> getAddMemberEntities()
    {
        List<ConferenceMemberEntity> members = conference.getConfMemberList();
        List<ConferenceMemberEntity> ms = members;
        return ConferenceHelper.transfer(members);
    }


    static class DetailInfo
    {
        final String no;
        final String subject;

        final int type;
        final int state;

        final String accessCode;
        final String outerAccessCode;

        final String menCode;
        final String hostCode;

        DetailInfo(ConferenceEntity conference)
        {
            no = conference.getConfId();
            subject = conference.getSubject();

            type = conference.getType();
            state = conference.getState();

            accessCode = conference.getAccessCode();
            outerAccessCode = conference.getOuterAccessCode();

            menCode = conference.getMemberCode();
            hostCode = conference.getHostCode();
        }
    }

    private void initAccessCodeView()
    {
        if (ConferenceEntity.STATUS_END == info.state)
        {
            return;
        }

        String accessCode = info.accessCode;
        String outerAccessCode = info.outerAccessCode;

        String menCode = info.menCode;
        String hostCode = info.hostCode;

        boolean isIntraVisible = !TextUtils.isEmpty(accessCode);
        boolean isInterVisible = !TextUtils.isEmpty(outerAccessCode);

        //接入码布局
        if (isIntraVisible && isInterVisible)
        {
            TextView accessCodeText = (TextView) findViewById(R.id.conference_accesscode_text);
            accessCodeText.setVisibility(View.VISIBLE);
        }

        if (isIntraVisible)
        {
            RelativeLayout accessCodeLayout = (RelativeLayout) findViewById(R.id.conference_accesscode_view);
            accessCodeLayout.setVisibility(View.VISIBLE);
            TextView accesscodeText = (TextView) findViewById(R.id.conference_accesscode_content);
            accesscodeText.setText(accessCode);

            if (!isInterVisible)
            {
                accessCodeLayout.setBackgroundResource(R.drawable.bg_row_single_normal);

                TextView accessCodeTag = (TextView) findViewById(R.id.conference_accesscode_tag);
                accessCodeTag.setText(R.string.conf_access_code);
            }
        }

        if (isInterVisible)
        {
            RelativeLayout accessCodeLayout = (RelativeLayout) findViewById(R.id.conference_outeraccesscode_view);
            accessCodeLayout.setVisibility(View.VISIBLE);
            TextView accesscodeText = (TextView) findViewById(R.id.conference_outeraccesscode_content);
            accesscodeText.setText(outerAccessCode);

            if (!isIntraVisible)
            {
                accessCodeLayout.setBackgroundResource(R.drawable.bg_row_single_normal);

                TextView accessCodeTag = (TextView) findViewById(R.id.conference_outeraccesscode_tag);
                accessCodeTag.setText(R.string.conf_access_code);
            }
        }

        //密码布局

        if (TextUtils.isEmpty(menCode) && TextUtils.isEmpty(hostCode))
        {
            return;
        }

        boolean isHost = conference.isSelfHost();
        boolean isHostVisible = !TextUtils.isEmpty(hostCode) && isHost && !conference.isMcu();
        boolean isMemVisible = !TextUtils.isEmpty(menCode);

        if (!isHostVisible && !isMemVisible)
        {
            return;
        }

        RelativeLayout memAccessPasswordLayout;
        RelativeLayout hostAccessPasswordLayout;

        TextView accessPasswordText = (TextView) findViewById(R.id.conference_password_text);
        accessPasswordText.setVisibility(View.VISIBLE);

        if (isHostVisible)
        {
            hostAccessPasswordLayout = (RelativeLayout) findViewById(R.id.conference_host_password_view);
            hostAccessPasswordLayout.setVisibility(View.VISIBLE);
            TextView hostAccessPasswordText = (TextView) findViewById(R.id.conference_host_password_content);
            hostAccessPasswordText.setText(hostCode);

            if (!isMemVisible)
            {
                hostAccessPasswordLayout
                        .setBackgroundResource(R.drawable.bg_row_single_normal);
            }
        }

        if (isMemVisible)
        {
            memAccessPasswordLayout = (RelativeLayout) findViewById(R.id.conference_member_password_view);
            memAccessPasswordLayout.setVisibility(View.VISIBLE);
            TextView memAccessPasswordText = (TextView) findViewById(R.id.conference_member_password_content);
            memAccessPasswordText.setText(menCode);

            if (!isHostVisible)
            {
                memAccessPasswordLayout
                        .setBackgroundResource(R.drawable.bg_row_single_normal);
            }
        }
    }

    public String getJoinConfInviteInfo()
    {
        String access;

        if (!TextUtils.isEmpty(conference.getAccessCode()))
        {
            access = conference.getAccessCode();
        }
        else
        {
            access = conference.getOuterAccessCode();
        }

        if (TextUtils.isEmpty(access) || TextUtils.isEmpty(conference.getMemberCode()))
        {
            return null;
        }

        return access + "*" + conference.getMemberCode();
    }

    private void handleJoinConfClick()
    {
        //与会号码不为空
        String joinNumber = SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb();
        if (!TextUtils.isEmpty(joinNumber))
        {
            ConferenceFunc.getIns().requestJoinConf(conference);
        }
        else
        {
            Toast.makeText(ConferenceDetailActivity.this, "The access number cannot be empty", Toast.LENGTH_SHORT).show();
        }

        UCAPIApp.getApp().popActivity(ConferenceDetailActivity.this);
    }

    /**
     * [方法的功能描述]获取会议时长
     * @return
     */
    private String getConfTimeLength()
    {
        String timeLength;
        int count = 0;

        conference.checkEndTime();

        Timestamp tempTimestamp = conference.getEndTime();
        if (tempTimestamp != null)
        {
            count = (int) (tempTimestamp.getTime() - conference.getBeginTime().getTime());
        }

        Resources resources = LocContext.getResources();
        timeLength = DateUtil.getConfCallTime(resources, count / 1000);

        return timeLength;
    }

    private String getConfId()
    {
        return info == null ? null : info.no;
    }
}
