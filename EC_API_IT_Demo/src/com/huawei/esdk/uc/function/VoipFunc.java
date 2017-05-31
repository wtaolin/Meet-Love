package com.huawei.esdk.uc.function;

import java.util.ArrayList;
import java.util.LinkedList;

import com.huawei.common.BaseBroadcast;
import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.conference.CtcMemberEntity;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.CtdData;
import com.huawei.data.ExecuteResult;
import com.huawei.dispatcher.Dispatcher;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.call.MediaActivity;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.service.EspaceService;
import com.huawei.service.ServiceProxy;
import com.huawei.utils.StringUtil;
import com.huawei.voip.CallManager;
import com.huawei.voip.CallSession;
import com.huawei.voip.TupHelper;
import com.huawei.voip.data.EarpieceMode;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

public class VoipFunc extends BaseBroadcast
{

    private static final String TAG = VoipFunc.class.getSimpleName();

    private static VoipFunc ins;

    // ////////////// 通知类型 ///////////////////////
    /** 呼叫连接成功 **/
    public static final String CALL_CONNECTED = "call_connected";

    /** 呼叫保持 **/
    public static final String CALL_HOLD = "call_hold";

    public static final String CALL_HOLD_FAILED = "call_hold_failed";

    /** 呼叫连接成功 **/
    public static final String CALL_CLOSED = "call_closed";

    public static final String VIDEO_INVITE = "call_video_invite";

    public static final String VIDEO_ADD_SUCESS = "call_video_add_sucess";

    public static final String VIDEO_ADD_FAILED = "call_video_add_failed";

    public static final String VIDEO_REMOVE = "call_video_remove";

    public static final String VIDEO_CHANGE_ORIENT = "call_video_change_orient";

    public static final String REFRESH_LOCAL_VIEW = "call_refresh_local_view";
    public static final String REFRESH_REMOTE_VIEW = "call_refresh_remote_view";

    public static final String GOTO_CONF_VIEW = "call_transto_conference";

    public static final String AUDIOSTOPNOTIFY = "local_audio_stop_notify";

    // ////////////////////// Voip 状态 //////////////////////////////
    /** 初始状态 **/
    public static final int STATUS_INIT = 0;

    /** 呼叫状态 **/
    public static final int STATUS_CALLING = 1;

    /** 通话状态 **/
    public static final int STATUS_TALKING = 2;

    // //////////////////////// 呼叫类型 //////////////////////////////////////
    /** 来电 **/
    public static final int CALL_COME = 1;

    /** 呼出 **/
    public static final int CALL_OUT = 2;

    // //////////////// 呼叫类型 /////////////////////////////
    public static final int CTD = 1;

    public static final int VOIP = 2;

    public static final int VIDEO = 3;

    public static final int MICROPHONE = 0;

    public static final int SPEAKER = 1;

    private CallManager callManager;

    private CallSession callSession;

    private PersonalContact currentCallPerson;

    private PersonalContact callPersonal;

    private String currentCallNum;

    private String confId;

    /** 是否是会议 **/
    private boolean isConf = false;

    /** 是否是视频通话 **/
    private boolean isVideo = false;

    /** Voip 状态 **/
    private int voipStatus = VoipFunc.STATUS_INIT;

    /** 呼叫模式 **/
    private int callMode = VoipFunc.CALL_OUT;

    /** 呼叫类型 **/
    private int callType = VoipFunc.VOIP;

    private boolean mute = false;

    private boolean hold = false;

    /**
     * 播放彩铃的句柄
     */
    private int callRingHandle = -1;

    /**
     * 添加“/”后缀；否则fast在创建路径下的文件时会异常。
     */
    public static final String VOIP_LOG_FOLDER_NAME = "/voipLog/";

    private VoipFunc()
    {
        initBroadcasts();
    }

    public static VoipFunc getIns()
    {
        if (ins == null)
        {
            ins = new VoipFunc();
        }
        return ins;
    }

    private void initBroadcasts()
    {
        broadcasts.put(CALL_CONNECTED, new LinkedList<BaseReceiver>());
        broadcasts.put(CALL_HOLD, new LinkedList<BaseReceiver>());
        broadcasts.put(CALL_CLOSED, new LinkedList<BaseReceiver>());
    }

    private ServiceProxy getService()
    {
        return UCAPIApp.getApp().getService();
    }

    public CallManager getCallManager()
    {
    	//by lwx302895
//        ServiceProxy service = getService();
//        callManager = null;
//
//        if (service != null && callManager == null)
//        {
//            callManager = service.getCallManager(); // 获取CallManager对象
//        }
//
//        Log.d(CommonUtil.APPTAG, TAG + " | callManager ：" + callManager);
//
//        return callManager;
    	return EspaceService.getCallManager();
    }

    public void setCallSession(CallSession callSession)
    {
        this.callSession = callSession;
    }

    public CallSession getCallSession()
    {
        return callSession;
    }

    public boolean makeCall(String calledNumber)
    {
        PersonalContact contact = ContactFunc.getIns().getContactByNumber(
                calledNumber);
        String displayName = calledNumber;
        if (contact != null)
        {
            displayName = ContactFunc.getIns().getDisplayName(contact);
        }
        boolean ret = false;

        setCallType(SelfDataHandler.getIns().getSelfData().getCallType());

        switch (SelfDataHandler.getIns().getSelfData().getCallType())
        {
        case VoipFunc.CTD:

            ret = doCTDCall(calledNumber);
            break;
        case VoipFunc.VIDEO:

            VoipFunc.getIns().initVideo(true);
            VoipFunc.getIns().showMediaActivity(true, calledNumber,
                    displayName, contact, true);
            break;
        case VoipFunc.VOIP:
        default:

            VoipFunc.getIns().initVideo(false);
            VoipFunc.getIns().showMediaActivity(true, calledNumber,
                    displayName, contact, true);

            break;
        }
        return ret;
    }

    public boolean doCTDCall(String calledNum)
    {
        String callBackNumber = SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb();
        CtdData data = new CtdData();
        data.setUser(CommonVariables.getIns().getUserAccount());
        data.setCallNumber(callBackNumber);
        data.setOppoNumber(calledNum);

        ServiceProxy service = getService();
        ExecuteResult ctdResult = null;
        if (null != service)
        {
            ctdResult = service.ctdCall(data);

            return ctdResult.isResult();
        }
        return false;
    }

    public boolean doVoipCall(String calledNum, boolean isVideo)
    {
        CallManager cm = getCallManager(); // 获取CallManager对象
        PersonalContact pc = ContactCache.getIns()
                .getContactByNumber(calledNum);
        String domain = (pc == null) ? null : pc.getDomain();

        CallSession callSession = null;
        if (cm != null)
        {
            callSession = cm.makeCall(calledNum, domain, isVideo);
        }

        setCallSession(callSession);
        if (isVideo && callSession != null)
        {
            VideoFunc.getIns().setCallId(callSession.getSessionId());
            VideoFunc.getIns().deployGlobalVideoCaps();
        }

        if (callSession != null)
        {
            SelfInfoUtil.getIns().setStatus(ContactClientStatus.BUSY);
            VoipFunc.getIns().setCallMode(CALL_OUT);
            VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_CALLING);
            return true;
        }

        return false;
    }

    public boolean answer(boolean isVideo)
    {
        if (callSession != null)
        {
            return callSession.answer(isVideo);
        }
        return false;
    }

    public void answerConf()
    {
        if (callSession != null)
        {
            if (getConfId() != null)
            {
                ConferenceFunc.getIns().reportTerminalType(getConfId());
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    answer(isVideo);
                }
            });
        }
    }

    public void hangup()
    {
        if (callSession != null)
        {
            callSession.hangUp(false);
        }
    }

    public boolean hold(boolean hold)
    {
        if (callSession != null)
        {
            if (hold)
            {
                return callSession.holding();
            }
            else
            {
                return callSession.resume();
            }
        }
        return false;
    }

    /**
     * 
     * @param type
     *            类型 0：麦克风, 1：扬声器
     * @param mute
     * @return
     */
    public boolean mute(int type, boolean mute)
    {
        if (callSession != null)
        {
            return callSession.mute(type, mute);
        }
        return false;
    }

    /**
     * @param audioSwitch
     *            只能取<code>EarpieceMode.TYPE_LOUD_SPEAKER(扬声器)</code> 或
     *            <code>EarpieceMode.TYPE_AUTO(听筒、有线耳机、蓝牙耳机自动切换)</code>
     */
    public boolean setAudioRoute(int audioSwitch)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | audioSwitch : " + audioSwitch);
        boolean result = false;
        CallManager cm = getCallManager();

        if (cm != null)
        {
            TupHelper tupHelper = cm.getTupHelper();
            if (tupHelper != null)
            {
                tupHelper.setAudioRoute(audioSwitch);
                result = true;
            }
        }
        return result;
    }

    /**
     * 切换音频路由，当前为扬声器状态，则切为非扬声器状态； 若当前为非扬声器状态，则切为扬声器状态
     */
    public void switchAudioRoute()
    {
        if (!isSpeaker())
        {
            setAudioRoute(EarpieceMode.TYPE_LOUD_SPEAKER);
        }
        else
        {
            setAudioRoute(EarpieceMode.TYPE_AUTO);
        }
    }

    /**
     * 获取音频路由
     * 
     * @return 只可能存在五种返回值 <li>获取失败时返回EarpieceMode.TYPE_AUTO</li> <li>
     *         蓝牙：EarpieceMode.TYPE_BLUETOOTH</li> <li>
     *         有线耳机：EarpieceMode.TYPE_EARPHONE</li> <li>
     *         听筒：EarpieceMode.TYPE_TELRECEIVER</li> <li>
     *         扬声器：EarpieceMode.TYPE_LOUD_SPEAKER</li>
     */
    private int getAudioRoute()
    {
        int rote = EarpieceMode.TYPE_AUTO;
        CallManager cm = getCallManager();

        if (cm != null)
        {
            TupHelper tupHelper = cm.getTupHelper();
            if (tupHelper != null)
            {
                rote = tupHelper.getAudioRoute();
            }
        }
        return rote;
    }

    /**
     * 是否是扬声器
     * 
     * @return
     */
    public boolean isSpeaker()
    {
        return VoipFunc.getIns().getAudioRoute() == EarpieceMode.TYPE_LOUD_SPEAKER;
    }

    public boolean dialpadInTalking(int code)
    {
        boolean ret = false;
        if (null != callSession)
        {
            ret = callSession.reDial(code);
        }
        return ret;
    }

    public boolean closeVideo()
    {
        boolean ret = false;
        if (callSession != null)
        {
            ret = callSession.removeVideo();
            VoipFunc.getIns().setVideo(false);
        }
        return ret;
    }

    public boolean upgradeVideo()
    {
        boolean ret = false;
        if (callSession != null)
        {
            ret = callSession.addVideo();
        }

        if (ret)
        {
            setVideo(true);
        }
        return ret;
    }

    public void declineVideoInvite()
    {
        if (callSession != null)
        {
            callSession.disagreeVideoUpdate();
        }
    }

    public void agreeVideoUpgradte()
    {
        if (callSession != null)
        {
            callSession.agreeVideoUpdate();
        }
    }

    public void registerVoip(VoipNotification ipVoipNotification)
    {
        CallManager cm = getCallManager(); // 获取CallManager对象

        if (cm != null && ipVoipNotification != null)
        {
            cm.registerNofitication(ipVoipNotification); // 注册回调消息
        }
    }
    
    
    /**
     * 注销VOIP
     * @param needWaitResult 需要等待注销的响应回来才反初始化组件
     */
    public void unRegisterVoip(boolean needWaitResult)
    {
        CallManager cm = getCallManager(); // 获取CallManager对象

        if (cm != null)
        {
            cm.unRegister();
        }
        
//        unRegNofitication();
    }

    public void postSipNotification(String id)
    {
        sendBroadcast(id, null);
    }
    public void postNotification(String id ,BaseData data)
    {
        sendBroadcast(id, data);
    }

    public void showMediaActivity(boolean needMakeCall, String callNum,
            String displayName, PersonalContact contact, boolean showHeader)
    {
        Intent intent = new Intent(UCAPIApp.getApp(), MediaActivity.class);
        intent.putExtra("needMakeCall", needMakeCall);
        intent.putExtra("callNumber", callNum);
        intent.putExtra("displayName", displayName);
        intent.putExtra("contact", contact);
        intent.putExtra("isVideo", isVideo);
        //添加一个标识符来判断头像显示，从什么地方取头像
        intent.putExtra("showHeader", showHeader);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        LocContext.getContext().startActivity(intent);
    }

    public void clear()
    {
        if (currentCallNum != null)
        {
            currentCallNum = null;
        }
        if (currentCallPerson != null)
        {
            currentCallPerson = null;
        }
        if (isVideo)
        {
            clearAfterVideoCallEnd();
            isVideo = false;
        }

        isConf = false;
    }

    public void saveCurrentCallPerson()
    {
        String tel = callSession.getCallerNumber();
        String backNumber = callSession.getCallerNumber();

        if (TextUtils.isEmpty(tel))
        {
            return;
        }
        setCurrentCallNumber(tel);
        currentCallPerson = ContactFunc.getIns().getContactByNumber(tel);

        if (currentCallPerson == null && !TextUtils.isEmpty(backNumber))
        {
            currentCallPerson = ContactFunc.getIns().getContactByNumber(
                    backNumber);
        }
    }

    /**
     * 拨打电话加载头像，用户信息从ContactAdapter获取
     * @param callPersonal
     */
    public void setCallPersonal(PersonalContact callPersonal)
    {
        this.callPersonal = callPersonal;
    }

    /**
     *获取通话用户的信息
     * @return
     */
    public PersonalContact getCallPersonal()
    {
        return  callPersonal;
    }

    /**
     * 接听电话获取来电的用户信息
     * @return
     */
    public PersonalContact getCurrentCallPerson()
    {
        return currentCallPerson;
    }

    public String getCurrentCallPersonName()
    {
        return ContactFunc.getIns().getDisplayName(currentCallPerson);
    }

    public void setCurrentCallNumber(String callNumber)
    {
        currentCallNum = callNumber;
    }

    public String getCurrentCallNumber()
    {
        return currentCallNum;
    }

    public boolean isConf()
    {
        return isConf;
    }

    public void setConf(boolean isConf)
    {
        this.isConf = isConf;
    }

    public boolean isVideo()
    {
        return isVideo;
    }

    public void setVideo(boolean isVideo)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | isVideo = " + isVideo);
        if (!ContactLogic.getIns().getAbility().isVideoCallAbility())
        {
            this.isVideo = false;
            return;
        }

        if (isVideo && callSession != null)
        {
            VideoFunc.getIns().setCallId(callSession.getSessionId());
        }

        if (isVideo == this.isVideo)
        {
            return;
        }

        if (isVideo)
        {
            prepareVideoCall();
        }
        else
        {
            clearAfterVideoCallEnd();
        }

        this.isVideo = isVideo;
    }

    /**
     * 用户在发起视频呼叫时初始化视频部件
     * 
     * @param isVideo
     */
    public void initVideo(boolean isVideo)
    {
        if (!ContactLogic.getIns().getAbility().isVideoCallAbility())
        {
            this.isVideo = false;
            return;
        }

        if (isVideo == this.isVideo)
        {
            return;
        }

        if (isVideo)
        {
            VideoFunc.getIns().deploySessionVideoCaps();
        }
        else
        {
            clearAfterVideoCallEnd();
        }

        this.isVideo = isVideo;
    }

    private void clearAfterVideoCallEnd()
    {
        VideoFunc.getIns().clearSurfaceView();
    }

    /**
     * 初始化视频界面面的view
     */
    public void prepareVideoCall()
    {
        // 必须放到ui线程来执行.
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                if (callSession != null)
                {
                    VideoFunc.getIns().deploySessionVideoCaps();
                }
            }
        });
    }

    public String getConfId()
    {
        return confId;
    }

    public void setConfId(String confId)
    {
        this.confId = confId;
    }

    public int getVoipStatus()
    {
        return voipStatus;
    }

    public void setVoipStatus(int status)
    {
        voipStatus = status;
    }

    public int getCallMode()
    {
        return callMode;
    }

    public void setCallMode(int mode)
    {
        callMode = mode;
    }

    public int getCallType()
    {
        return callType;
    }

    public void setCallType(int type)
    {
        callType = type;
    }

    public boolean isMute()
    {
        return mute;
    }

    public void setMuteStatus(boolean mute)
    {
        this.mute = mute;
    }

    public boolean isHold()
    {
        return hold;
    }

    public void setHoldStatus(boolean hold)
    {
        this.hold = hold;
    }

    /**
     * 播放音频文件
     * 
     * @param path
     *            路径
     * @param loop
     *            -1(0xFFFFFFFF)：无限循环 ；其他：循环(loop+1)次
     * @return
     */
    public int playSound(String path, int loop)
    {
        CallManager cm = getCallManager();

        if (null == cm || isVoipCalling())
        {
            return -1;
        }

        selectAudioRoute();
        callRingHandle = cm.getTupHelper().startPlay(path, loop);

        return callRingHandle;
    }

    public boolean stopSound()
    {
        int result = -1;

        if (callRingHandle != -1)
        {
            CallManager cm = getCallManager();

            if (cm != null)
            {
                result = cm.getTupHelper().stopPlay(callRingHandle);
            }
        }

        boolean isSuccess = (result == 0);

        if (isSuccess)
        {
            VoipFunc.getIns().setAudioRoute(EarpieceMode.TYPE_AUTO);
            callRingHandle = -1;
        }

        return isSuccess;
    }

    /**
     * 根据当前的手机状态判断要选用哪种路由播放声音，如果是连接耳机设备的话就从耳机播放，否则用扬声器
     * 
     * @return
     */
    public int selectAudioRoute()
    {
        int result = -1;
        int audioRoute = getAudioRoute();

        boolean success = false;
        if (audioRoute != EarpieceMode.TYPE_EARPHONE
                && audioRoute != EarpieceMode.TYPE_BLUETOOTH)
        {
            if (audioRoute != EarpieceMode.TYPE_LOUD_SPEAKER)
            {
                result = EarpieceMode.TYPE_LOUD_SPEAKER;
                success = VoipFunc.getIns().setAudioRoute(result);
            }
        }
        else
        {
            result = EarpieceMode.TYPE_AUTO;
            success = VoipFunc.getIns().setAudioRoute(result);
        }

        return success ? result : -1;
    }

    public boolean isVoipCalling()
    {
        return isVoipCalling(false);
    }

    /**
     * 检查当前是否在通话中
     * 
     * @param isContainPstn
     *            是否检查pstn通话
     * @return
     */
    public boolean isVoipCalling(boolean isContainPstn)
    {
        // 如果考虑pstn通话的情况下，pstn通话中直接返回通话中
        if (isContainPstn && !DeviceUtil.isCallStateIdle())
        {
            return true;
        }

        if (getVoipStatus() != STATUS_INIT)
        {
            return true;
        }

        return false;
    }

    public boolean startRecord(String path)
    {
        CallManager cm = getCallManager();

        if (cm == null || isVoipCalling())
        {
            return false;
        }

        cm.getTupHelper().startRecord(path);
        return true;

    }

    public boolean stopRecord()
    {
        CallManager cm = getCallManager();

        if (cm == null)
        {
            return false;
        }
        cm.getTupHelper().stopRecord();
        return true;
    }

    public int getCurMircoVol()
    {
        CallManager cm = getCallManager();

        if (cm == null)
        {
            return -1;
        }

        return cm.getTupHelper().getMircoVol();
    }

    public void setVoipLog(boolean isOpen)
    {
    	//by lwx302895
//        setVoipLogSwitch(isOpen, LocalLog.LOG_PATH_RELATIVE
//                + VOIP_LOG_FOLDER_NAME);
    	LocalLog.setVoipLog(isOpen);
    }

//    private void setVoipLogSwitch(boolean status, String path)
//    {
//        ServiceProxy service = UCAPIApp.getApp().getService();
//        if (null != service)
//        {
//            service.setLogSwitch(path, status);
//        }
//    }

    private void sendConferenceMemberStatus(String confId, String number, boolean isInConf)
    {
        ArrayList<CtcMemberEntity> newMemberSet = new ArrayList<CtcMemberEntity>();
        CtcMemberEntity entity = new CtcMemberEntity();
        entity.setConfId(confId);
        entity.setNumber(number);
        if (isInConf)
        {
            entity.setMemberStatus(1);
        }
        else
        {
            entity.setMemberStatus(2);
        }
        entity.setRole(2);
        newMemberSet.add(entity);
        Intent intent = new Intent(CustomBroadcastConst.CTC_UPDATE_MEMBERSTATUS_PUSH);
        intent.putExtra(UCResource.SERVICE_RESPONSE_DATA, newMemberSet);
        Dispatcher.postLocBroadcast(intent);
    }

    public void transVoipToConfView()
    {
        if (!TextUtils.isEmpty(getCallSession().getServerConfType()) && getCallSession().getServerConfType().startsWith("00003"))
        {
            sendConferenceMemberStatus(getConfId(), ContactLogic.getIns().getMyContact().getBinderNumber(), true);
        }
    }

}
