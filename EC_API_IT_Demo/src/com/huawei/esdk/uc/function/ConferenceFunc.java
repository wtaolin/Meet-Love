package com.huawei.esdk.uc.function;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.huawei.common.BaseBroadcast;
import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.conference.CtcMemberEntity;
import com.huawei.conference.entity.CTCFieldNotifyData;
import com.huawei.conference.entity.ConfURLData;
import com.huawei.conference.entity.CreateConferenceMsgAck;
import com.huawei.conference.entity.CtcEntity;
import com.huawei.conference.entity.GetConfListMsgAck;
import com.huawei.conference.entity.GetMemberMsgAck;
import com.huawei.conference.logic.CTCInterface;
import com.huawei.conference.logic.CTCUtil;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.ExecuteResult;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.utils.StringUtil;

public class ConferenceFunc extends BaseBroadcast
{

    private static ConferenceFunc ins;

    public static final String DATACONF_RES_PATH = LocContext.getContext()
            .getFilesDir() + "/AnnoRes";

    /** 与会者与会状态——加入会议成功 **/
    public static final int STATUS_SUCCESS = 1;

    /** 与会者与会状态——离开会议 **/
    public static final int STATUS_LEAVE = 2;

    /** 与会者与会状态——会议初始状态 **/
    public static final int STATUS_INVITE = 5;

    // //////////////// 会议状态 //////////////////
    public static final int STATUS_CREATED = 0;

    public static final int STATUS_TO_ATTEND = 1;

    public static final int STATUS_IN_PROGRESS = 2;

    public static final int STATUS_END = 3;

    public static final String GET_BOOKCONFRENCE_DETAIL  = "local_bookconf_detail";
    public static final String UPDATE_CONFERENCE_NOTIFY  = "local_conference_status";

    private CTCInterface confInterface;

    private int confListWithMemId;

    private int referCreateConfId;

    private final Map<Integer, String> getConfMemId = new HashMap<Integer, String>();

    private final Map<Integer, String> joinConfId = new HashMap<Integer, String>();

    private final Map<Integer, String> bookDetailConfId = new HashMap<Integer, String>();

    private final Map<Integer, String> endingConfId = new HashMap<Integer, String>();

    private final Map<Integer, ConferenceEntity> creatingConf = new HashMap<Integer, ConferenceEntity>();

    private final Map<Integer, String> updateConfId = new HashMap<Integer, String>();

    private final Map<String, ConfURLData> dataConfUrlMap = new HashMap<String, ConfURLData>();

    public static final int NORMAL_CREATE_CONF = 0;

    public static final int REFER_CREATE_CONF = 1;

    public static final String UPDATE_CONFLIST_NOTIFY = "local_conference_conflist";

    public static final String UPDATA_CONFERENCE_MEMBER = "local_conference_member";

    public static final String CREATE_CONFERENCE_NOTIFY = "conference_create";

    public static final String CONF_FULL_INFO = "conference_full_info";

    public static final String CONF_MEMBER_STATUS = "conference_member_status";

    public static final String CONF_END = "conference_end";

    private ConferenceEntity curConfEntity;

    private DataConfFunc dataConfFunc;

    private ConferenceEntity confEntity;

    private ConferenceFunc()
    {
        initBroadcasts();
    }

    /**创建会议的次数*/
    public int CREATE_CONF_NUM = 0;

    public static ConferenceFunc getIns()
    {
        if (ins == null)
        {
            ins = new ConferenceFunc();
        }
        return ins;
    }

    public ConferenceEntity getCurConfEntity()
    {
        return curConfEntity;
    }

    public void setCurConfEntity(ConferenceEntity conferenceEntity)
    {
        this.curConfEntity = conferenceEntity;
    }

    private void initBroadcasts()
    {
        broadcasts.put(UPDATE_CONFLIST_NOTIFY, new LinkedList<BaseReceiver>());
        broadcasts
                .put(UPDATA_CONFERENCE_MEMBER, new LinkedList<BaseReceiver>());
        broadcasts
                .put(CREATE_CONFERENCE_NOTIFY, new LinkedList<BaseReceiver>());
        broadcasts.put(CONF_FULL_INFO, new LinkedList<BaseReceiver>());
        broadcasts.put(CONF_MEMBER_STATUS, new LinkedList<BaseReceiver>());
        broadcasts.put(CONF_END, new LinkedList<BaseReceiver>());
    }

    private void postBroadcast(String broadcastName)
    {
        sendBroadcast(broadcastName, null);
    }

    private CTCInterface getCtcService()
    {
    	// by lwx302895
        if (null == confInterface)
        {
//            ServiceProxy service = UCAPIApp.getApp().getService();
//            if (null != service)
//            {
//                confInterface = service.getCTCService();
//            }
        	confInterface = CTCUtil.getInstance();
        }
        return confInterface;
    }

    public ExecuteResult requestCreateConference(String emcee, String subject,
            int mediaType, String myEspaceAccount, int beginTime, int endTime,
            int confType, boolean sendMail,
            ArrayList<ConferenceMemberEntity> members)
    {
        if (getCtcService() == null)
        {
            return null;
        }

        List<CtcMemberEntity> ctcMemberEntities = ConferenceDataHandler
                .getIns().transToCtcMember(members);

        ExecuteResult result = getCtcService().createConferenceReq(emcee,
                subject, mediaType, myEspaceAccount, beginTime, endTime,
                confType, sendMail, ctcMemberEntities);

        if (result.isResult())
        {
            ConferenceEntity conf = new ConferenceEntity();
            conf.setHost(emcee);
            conf.setSubject(subject);
            conf.setMediaType(mediaType);
            conf.setBeginTime(new Timestamp(System.currentTimeMillis()));
            conf.setEndTime(new Timestamp(endTime));
            conf.setConfType(confType);
            conf.setConfMemberList(members);
            conf.setFullInfo(true);

            setCurConfEntity(conf);

            creatingConf.put(result.getId(), conf);
        }

        return result;
    }

    /**
     * 添加入会者
     * @param confId 会议Id
     * @param members 与会者集合
     * @return
     */
    public ExecuteResult requestAddMember(String confId,
            List<CtcMemberEntity> members)
    {
        if (getCtcService() == null || TextUtils.isEmpty(confId))
        {
            return null;
        }

        // 会议类型都传0 不做区分
        ExecuteResult result = getCtcService().addMemberReq(
                CommonVariables.getIns().getUserAccount(), confId, members, 0);

        return result;
    }

    /**
     * 一键入会
     * @param conf 会议
     * @return 操作信息
     */
    public ExecuteResult requestJoinConf(ConferenceEntity conf)
    {
        String joinNumber = SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb();
        if (TextUtils.isEmpty(joinNumber) || conf == null)
        {
            return null;
        }

        String confId = conf.getConfId();

        if (TextUtils.isEmpty(confId))
        {
            return null;
        }

        String account = CommonVariables.getIns().getUserAccount();
        String name = ContactLogic.getIns().getMyContact().getName();
        String host = conf.getHostAccount();

        ExecuteResult result = getCtcService().joinConference(account, confId,
                joinNumber, name, conf.getType(), host, conf.getConfType());

        joinConfId.put(result.getId(), confId);

        return result;
    }

    /**
     * 删除与会人员
     * @param confId 会议ID
     * @param number 与会号码
     * @return
     */
    public ExecuteResult requestDelConfMember(String confId, String number)
    {
        if (getCtcService() == null)
        {
            return null;
        }
        ExecuteResult result = getCtcService().delMemberReq(
                CommonVariables.getIns().getUserAccount(), confId, number,
                null, 0);

        return result;
    }

    public ExecuteResult requestConfListWithMem()
    {
        ExecuteResult result = requestConferenceList();

        if (result != null)
        {
            confListWithMemId = result.getId();
        }

        return result;
    }

    public ExecuteResult requestConferenceList()
    {
        if (getCtcService() == null)
        {
            return null;
        }

        // 999是获取的一页中条目数，1是页面，0获取所有会议
        ExecuteResult result = getCtcService().getConfListReq(
                CommonVariables.getIns().getUserAccount(), 999, 1, 0);

        return result;
    }

    public ExecuteResult requestConferenceMem(String confId)
    {
        if (getCtcService() == null)
        {
            return null;
        }

        ExecuteResult result = getCtcService().getMemberReq(
                CommonVariables.getIns().getUserAccount(), confId);

        getConfMemId.put(result.getId(), confId);

        return result;
    }

    public ExecuteResult requestBookConfDetail(String confId)
    {
        if (TextUtils.isEmpty(confId) || getCtcService() == null)
        {
            return null;
        }

        ExecuteResult result = getCtcService().getConferenceInfo(
                CommonVariables.getIns().getUserAccount(), confId,
                ConferenceEntity.TYPE_BOOKING);

        if (result != null && result.isResult())
        {
            bookDetailConfId.put(result.getId(), confId);
        }

        return result;
    }

    /**
     * 升级语音会场为多媒体会场
     * @param confId 会议ID
     * @return
     */
    public ExecuteResult requestUpgradeDataConf(String confId)
    {
        if (TextUtils.isEmpty(confId) || getCtcService() == null)
        {
            return null;
        }

        ExecuteResult result = getCtcService().upgradeConference(null, confId,
                CommonVariables.getIns().getUserAccount());

        if (result != null)
        {
            updateConfId.put(result.getId(), confId);
        }

        return result;
    }

    /**
     * 结束会议
     * @param confId 会议Id
     * @return
     */
    public ExecuteResult requestEndConference(String confId)
    {
        if (TextUtils.isEmpty(confId) || getCtcService() == null)
        {
            return null;
        }

        // 会议类型都传0 不做区分
        ExecuteResult result = getCtcService().stopConferenceReq(
                CommonVariables.getIns().getUserAccount(), confId, 0, 0);
        if (result != null)
        {
            endingConfId.put(result.getId(), confId);
        }
        return result;
    }

    /**
     * 上报终端
     * @param confId
     * @return
     */
    public ExecuteResult reportTerminalType(String confId)
    {
        if (getCtcService() == null)
        {
            return null;
        }

        // 会话ID传null true是用来订阅的
        ExecuteResult result = getCtcService().reportTerminalType(null,
                CommonVariables.getIns().getUserAccount(), confId, true);

        return result;
    }

    public void handleConferenceListResp(int result, BaseResponseData d)
    {
        if (result == UCResource.REQUEST_OK)
        {
            if (d.getStatus() == ResponseCode.REQUEST_SUCCESS
                    && d instanceof GetConfListMsgAck)
            {
                List<CtcEntity> rspList = ((GetConfListMsgAck) d).getConfList();
                //会议列表 start lwx302895
//                if (rspList != null)
//                {
//                    for (CtcEntity ctcEntity : rspList)
//                    {
//                        if (ConferenceEntity.STATUS_IN_PROGRESS == ctcEntity
//                                .getCtcStatus())
//                        {
//                            requestConferenceMem(ctcEntity.getConfId());
//                        }
//                        if (ConferenceEntity.STATUS_TO_ATTEND == ctcEntity
//                                .getCtcStatus())
//                        {
//                            requestBookConfDetail(ctcEntity.getConfId());
//                        }
//                    }
//                }

                ConferenceDataHandler.getIns().updateAllConf(rspList);

                 ConferenceDataHandler.getIns().updateConfList(rspList);

                 // 该响应是需要请求与会人列表的请求回来的响应
                 if (confListWithMemId == d.getBaseId())
                 {
                    List<ConferenceEntity> confInPro = ConferenceDataHandler
                            .getIns().getConfInPro();

                    for (ConferenceEntity conf : confInPro)
                    {
                        requestConferenceMem(conf.getConfId());
                    }
                 }

                 List<ConferenceEntity> confToAttend = ConferenceDataHandler
                 .getIns().getConfToAttend();
                 for (ConferenceEntity conf : confToAttend)
                 {
                 requestBookConfDetail(conf.getConfId());
                 }
                //会议列表 end

            }
        }

        ConferenceReceiveData data = new ConferenceReceiveData();
        data.result = result;
        data.responseData = d;
        sendBroadcast(UPDATE_CONFLIST_NOTIFY, data);
    }

    public void handleConferenceMemResp(int result, BaseResponseData d)
    {
        String confId = getConfMemId.remove(d.getBaseId());

        if (confId == null)
        {
            return;
        }

        // ConferenceEntity conf = ConferenceDataHandler.getIns().getConference(
        // confId);

        if (result == UCResource.REQUEST_OK /* && conf != null */)
        {
            // -30处理
            if (d.getStatus() == ResponseCode.REQUEST_SUCCESS)
            {
                // conf.setFullInfo(true);

                if (!(d instanceof GetMemberMsgAck))
                {
                    return;
                }

                // ConferenceDataHandler.getIns().updateConfMember(
                // ((GetMemberMsgAck) d).getUsers(), conf);

                // 断网重练后：可能没有与会人状态推送，此时收到与会人列表响应，则要通知与会人列表刷新
                if (null != ((GetMemberMsgAck) d).getUsers())
                {
                    ConferenceReceiveData data = new ConferenceReceiveData();
                    data.result = UCResource.REQUEST_OK;
                    data.responseData = d;
                    sendBroadcast(UPDATA_CONFERENCE_MEMBER, data);
                }
            }
            else if (ResponseCode.CONF_NOT_EXIST == d.getStatus())
            {
                // conf.setFullInfo(false);
            }
        }
    }

    public void handleCreateConferenceResp(int result, BaseResponseData d)
    {
        if (d == null)
        {
            return;
        }

        boolean isRefer = false;

        // 如果是referTo的会议是要通知VOIP的
        if (referCreateConfId == d.getBaseId())
        {
            isRefer = true;

            if (result == UCResource.REQUEST_OK
                    && d.getStatus() == ResponseCode.REQUEST_SUCCESS
                    && d instanceof CreateConferenceMsgAck)
            {
                CreateConferenceMsgAck ack = (CreateConferenceMsgAck) d;
                // VoipFunc.getIns().createVoipToConfResp(true, ack.getConfId(),
                // ack.isNewProcess());
            }
            else
            {
                // VoipFunc.getIns().createVoipToConfResp(false, null, false);
            }
        }

        ConferenceEntity conf = creatingConf.remove(d.getBaseId());
        ConferenceReceiveData data = new ConferenceReceiveData();
        data.result = result;
        data.responseData = d;

        if (result == UCResource.REQUEST_OK)
        {
            if (d.getStatus() == ResponseCode.REQUEST_SUCCESS
                    && d instanceof CreateConferenceMsgAck)
            {
                CreateConferenceMsgAck ack = (CreateConferenceMsgAck) d;
                conf.setConfId(ack.getConfId());

                setCurConfEntity(conf);

                //会议列表
                if (conf.getType() == ConferenceEntity.TYPE_INSTANT)
                {
                    ConferenceDataHandler.getIns().addConfCreated(conf);
                }
                else
                {
                    ConferenceDataHandler.getIns().addConfToAttend(conf);
                }

                ConferenceDataHandler.getIns().addConfToDb(conf);

                boolean loadMember = ack.isNewProcess() && !isRefer
                        && conf.getType() == ConferenceEntity.TYPE_INSTANT;

                // refer会场的需要等收到bye才能重邀
                if (loadMember)
                {
                    List<ConferenceMemberEntity> memberList = conf.getConfMemberList();
                    if (memberList.size() > 0)
                    {
                        List<CtcMemberEntity> ctcMemberEntities = ConferenceDataHandler
                                .getIns()
                                .transToCtcMember(memberList);
                        Log.d("ConferenceFunc", "ctcMemberEntities.size = " + ctcMemberEntities.size() + ",confId = " + conf.getConfId());
                        ExecuteResult result1 = requestAddMember(conf.getConfId(), ctcMemberEntities);

                        Log.d("ConferenceFunc", "+++++++++++++++++++++++++++++++" + result1.isResult());
                        Logger.debug("+++++++++++++++++++++++++++1111111111111");
                    }
                    else
                    {
                        Logger.debug("------------------------222222222222222");
                        Log.d("ConferenceFunc", "----------------------------");
                    }


                }

                sendBroadcast(UPDATE_CONFLIST_NOTIFY, data);
            }
        }
        sendBroadcast(CREATE_CONFERENCE_NOTIFY, data);
    }

    public void handleJoinConfResp(int result, BaseResponseData d)
    {
        String confId = joinConfId.remove(d.getBaseId());

        if (result == UCResource.REQUEST_OK
                && d.getStatus() == ResponseCode.REQUEST_SUCCESS)
        {
            reportTerminalType(confId);
            //预约会议 start lwx302895
            ConferenceEntity conf = ConferenceDataHandler.getIns()
                    .delConference(confId, ConferenceEntity.STATUS_TO_ATTEND);

            if (conf != null)
            {
                ConferenceDataHandler.getIns().addConfInPro(conf);
            }
            //end
        }

        // ConferenceReceiveData data = new ConferenceReceiveData();
        // data.result = result;
        // sendBroadcast(JOIN_CONFERENCE, data);
    }

    public void handlePushConferenceInfo(int result, BaseResponseData d)
    {
        if (result == UCResource.REQUEST_OK)
        {
            if (d instanceof CtcEntity)
            {
                CtcEntity ctc = (CtcEntity) d;

                String confId = ctc.getConfId();

                if (confId == null)
                {
                    return;
                }
                // 会议全量信息推送过来的时候，会议一定是正在进行的
                 ctc.setCtcStatus(ConferenceEntity.STATUS_IN_PROGRESS);
                 ConferenceDataHandler.getIns().updateConference(ctc);

                ConferenceFunc.getIns().setCurConfEntity(
                        ConferenceDataHandler.getIns().transToConference(ctc,
                                CommonUtil.NORMAL_TIME_TYPE));
                requestInitDataConf(confId);

                if (confId.equals(VoipFunc.getIns().getConfId())
                        && VoipFunc.getIns().getVoipStatus() == VoipFunc.STATUS_TALKING)
                {
                    VoipFunc.getIns().transVoipToConfView();
                }

                final ConferenceReceiveData data = new ConferenceReceiveData();
                data.result = result;
                data.responseData = ctc;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("ConferenceFunc", "handlePushConferenceInfo CONF_FULL_INFO+++++++++++++++++++++++++++++++");
                        sendBroadcast(CONF_FULL_INFO, data);
                    }
                }, 1000);
            }
        }
    }

    public void handleUpdateMemberStatus(
            ArrayList<CtcMemberEntity> ctcMemberEntities)
    {
        if (ctcMemberEntities == null)
        {
            return;
        }

        ConferenceReceiveData data = new ConferenceReceiveData();
        data.ctcMemberEntities = ctcMemberEntities;
        sendBroadcast(CONF_MEMBER_STATUS, data);
    }

    public void handleUpdateConference(int result, BaseResponseData d)
    {
        if (!(d instanceof CTCFieldNotifyData))
        {
            return;
        }

        // 推送消息，不需要做状态处理
        int status = ((CTCFieldNotifyData) d).getState();
        String confId = ((CTCFieldNotifyData) d).getConfId();
        ConferenceEntity conf = null;
        switch (status)
        {
            case CTCFieldNotifyData.CREATE_CONF_FAIL:
            {
                //会议列表 start by lwx302895
                conf = ConferenceDataHandler.getIns().delConference(confId,
                        ConferenceEntity.STATUS_CREATED);

                if (conf == null)
                {
                    return;
                }
                //end

                conf.setEndTime(new Timestamp(System.currentTimeMillis()));
                ConferenceDataHandler.getIns().addConfEnded(conf);

                ConferenceReceiveData data = new ConferenceReceiveData();
                data.result = result;
                data.responseData = d;
                sendBroadcast(UPDATE_CONFLIST_NOTIFY, data);
            }
                break;
            case CTCFieldNotifyData.CREATE_CONF_SUCCESS:
            {
                //会议列表 start
                conf = ConferenceDataHandler.getIns()
                        .delConference(confId, ConferenceEntity.STATUS_CREATED);

                // 如果是推送的是预约会议的话，已经在创会响应中处理了，所以不再createdList中，不需要在这里做处理
                if (conf == null)
                {
                    return;
                }
                //end

                ConferenceDataHandler.getIns().addConfInPro(conf);
                reportTerminalType(confId);
                ConferenceReceiveData data = new ConferenceReceiveData();
                data.result = result;
                data.responseData = d;
                sendBroadcast(UPDATE_CONFLIST_NOTIFY, data);
            }
                break;
            case CTCFieldNotifyData.CONF_END:

                handleEndConferenceResp(result, d);
                //主持人结束会议，与会者退出会议 by lwx302895
                postBroadcast(CONF_END);
                break;
            case CTCFieldNotifyData.UPDATE_CONF_SUCCESS:

                handleUpdateDataConf(result, d);

                break;
            default:
                break;
        }
    }

    public void handleEndConferenceResp(int result, BaseResponseData data)
    {
        String confId = endingConfId.remove(data.getBaseId());
        if (TextUtils.isEmpty(confId))
        {
            confId = ((CTCFieldNotifyData) data).getConfId();
        }
        // 如果返回的是会议不存在也当成是结束成功
        if (ResponseCode.CONF_NOT_EXIST == data.getStatus())
        {
            data.setStatus(ResponseCode.REQUEST_SUCCESS);
        }

        if (result == UCResource.REQUEST_OK
                && data.getStatus() == ResponseCode.REQUEST_SUCCESS)
        {
             ConferenceEntity conf = ConferenceDataHandler.getIns()
                .getConference(confId);

             if (conf == null)
             {
                return;
             }

             if (conf.getState() == ConferenceEntity.STATUS_END)
             {
                return;
             }

             // 预约会议不用更新结束时间
             if (conf.getType() == ConferenceEntity.TYPE_INSTANT)
             {
                Timestamp beginTime = conf.getBeginTime();

                long t = System.currentTimeMillis();
                Timestamp time = new Timestamp(t);

                if (null != beginTime && ((time.getTime() - beginTime.getTime()) <= 0))
                {
                    // 结束会议更新时间：如果当前时间比开始时间小，则再将结束时间置为比开始时间大于2小时
                    t = beginTime.getTime() + 2 * 60 * 60 * 1000;
                    conf.setEndTime(new Timestamp(t));
                }
                else
                {
                    conf.setEndTime(new Timestamp(t));
                }
             }

             ConferenceDataHandler.getIns().delConference(confId,
             conf.getState());

             ConferenceDataHandler.getIns().addConfEnded(conf);

            setCurConfEntity(null);
            postBroadcast(CONF_END);
        }
    }

    public void handleUpdateDataConf(int result, BaseResponseData d)
    {
        String confId = updateConfId.remove(d.getBaseId());

        if (result == UCResource.REQUEST_OK
                && d.getStatus() == ResponseCode.REQUEST_SUCCESS)
        {
            ConferenceEntity conf = getCurConfEntity();

            if (conf == null)
            {
                // savedMultiConfId.add(confId);
                return;
            }

            if (conf.getMediaType() == ConferenceEntity.MULTI_CONFERENCE_TYPE)
            {
                return;
            }
            conf.setMediaType(ConferenceEntity.MULTI_CONFERENCE_TYPE);
            requestInitDataConf(confId);
        }
    }

    public void handleDataConfUrl(ConfURLData urlData)
    {
        String confId = urlData.getConfId();
        dataConfUrlMap.put(confId, urlData);
        requestInitDataConf(confId);
    }

    public void requestInitDataConf(String confId)
    {

        ConferenceEntity conf = getCurConfEntity();
        boolean isDataConf = ContactLogic.getIns().getAbility().isJoinInDataConferenceAbility()/*isPhoneDataConferenceAbility()*/;
        ConfURLData url = dataConfUrlMap.get(confId);

        if (conf == null
                || !isDataConf
                || null == url
                || conf.getMediaType() != ConferenceEntity.MULTI_CONFERENCE_TYPE)
        {
            return;
        }

        ConferenceMemberEntity mem = conf.getSelfInConf();

        // 自己是否入会
        if (mem == null || !mem.isInConference())
        {
            return;
        }

        getDataConfFunc().JoinDataConf(conf, url);
    }

    private DataConfFunc getDataConfFunc()
    {
        if (dataConfFunc == null)
        {
            dataConfFunc = new DataConfFunc();
        }
        return dataConfFunc;
    }

    public static class ConferenceReceiveData extends BaseData
    {
        private static final long serialVersionUID = -924467544639618932L;

        public int result;

        public ResponseCode respCode;

        public ArrayList<CtcMemberEntity> ctcMemberEntities;

        public BaseResponseData responseData;
    }

    public void setHostEntity(ConferenceEntity conferenceEntity)
    {
        confEntity = conferenceEntity;
    }

    public ConferenceEntity getHostEntity()
    {
        if (confEntity != null)
        {
            return confEntity;
        }

        return null;
    }
}
