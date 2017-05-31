package com.huawei.esdk.uc.function;

import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.utils.StringUtil;
import com.huawei.voip.CallSession;
import com.huawei.voip.IpCallNotification;
import com.huawei.voip.TupHelper;
import com.huawei.voip.data.AudioMediaEvent;
import com.huawei.voip.data.EarpieceMode;
import com.huawei.voip.data.EventData;
import com.huawei.voip.data.OrientChange;
import com.huawei.voip.data.VoiceMailNotifyData;
import com.huawei.voip.data.VoiceQuality;

import android.text.TextUtils;
import android.util.Log;
import common.TupCallParam;
import imssdk.FastParam;
import tupsdk.TupCall;

public class VoipNotification implements IpCallNotification
{

    private static final String TAG = VoipNotification.class.getSimpleName();
    
    public static final int REFRESH_LOCAL_ADD = 1;
    public static final int REFRESH_LOCAL_DEL = 3;
    public static final int REFRESH_REMOTE_ADD = 2;
    public static final int REFRESH_REMOTE_DEL = 4;
    public static final int REFRESH_UNKNOWN = 0;
    

    @Override
    public void onAudioPlayEnd(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onAudioPlayEnd ");
        VoipFunc.getIns().setAudioRoute(EarpieceMode.TYPE_AUTO);
        AudioMediaEvent param = new AudioMediaEvent(arg0);
        VoipFunc.getIns().postNotification(VoipFunc.AUDIOSTOPNOTIFY, param);

    }

    @Override
    public void onCallAddVideo(CallSession callSession)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallAddVideo ");

        VoipFunc.getIns().setCallSession(callSession);

        VoipFunc.getIns().setVideo(true);
        //by lwx302895
//        saveOrient(callSession.getTupCall().getOrientType());
        saveOrient(callSession.getOrientType());

        // 如果设置video发现无法设置成功(无权限),则直接拒接.
        if (!VoipFunc.getIns().isVideo())
        {
            VoipFunc.getIns().declineVideoInvite();
        }
        else
        {
            
            VoipFunc.getIns().postSipNotification(VoipFunc.VIDEO_INVITE);
        }
    }
    
    
    private void saveOrient(int orient)
    {
        orient = VideoFunc.transOrient(orient);
        VideoFunc.getIns().setOrient(orient);
    }

    @Override
    public void onCallBldTransferFailed(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallBldTransferFailed ");
    }

    @Override
    public void onCallBldTransferSuccess(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallBldTransferSuccess ");
    }

    @Override
    public void onCallComing(CallSession arg0)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallComing ");

        VoipFunc.getIns().setCallSession(arg0);

        SelfInfoUtil.getIns().setStatus(ContactClientStatus.BUSY);

        VoipFunc.getIns().setCallMode(VoipFunc.CALL_COME);
        VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_CALLING);

        VoipFunc.getIns().saveCurrentCallPerson();

        TupCall tupCall;
        String number = null;
        String displayName = null;
        PersonalContact contact = null;
        if (arg0 != null)
        {
        	//by lwx302895
//            tupCall = arg0.getTupCall();

            arg0.alertingCall(); // 通知对端已振铃

            number = arg0.getCallerNumber();/*tupCall.getFromNumber();*/
			displayName = arg0.getCallerDisplayName();/*tupCall.getFromDisplayName();*/
            contact = ContactFunc.getIns().getContactByNumber(number);

            boolean isConf = arg0.getIsFoces();/* boolean isConf = tupCall.getIsFocus() == 1;*/

            if (isConf)
            {
                boolean isCtc = ContactLogic.getIns().getAbility().isCtcFlag();

                if (isCtc)
                {
                    VoipFunc.getIns().setConfId(arg0.getServerConfID());
                    VoipFunc.getIns().setConf(true);
                }
            }

//            if (TupCallParam.CALL_E_CALL_TYPE.TUP_CALLTYPE_VIDEO == tupCall
//                    .getCallType())
            if(arg0.isVideoCallType())
            {
                VoipFunc.getIns().setVideo(true);
            }
        }

        String callerNumber = "";

        if (!TextUtils.isEmpty(arg0.getTellNumber()))
        {
            callerNumber = arg0.getTellNumber();
        }
        else if (!TextUtils.isEmpty(arg0.getPaiNumber()))
        {
            callerNumber = arg0.getPaiNumber();
        }
        else
        {
            callerNumber = arg0.getCallerNumber();
        }

        if (callerNumber == null)
        {
            callerNumber = "";
        }

        // 判断callerNumber是否有; 有的话截断;后面的所有东西 判断是否是cpc=ordinary
        boolean isCpcOrdinary = false;
        String[] splitNumbers = callerNumber.split("\\;", 2);

        if (splitNumbers.length == 2 && splitNumbers[1] != null
                && splitNumbers[1].equals("cpc=ordinary"))
        {
            isCpcOrdinary = true;
            callerNumber = splitNumbers[0];
        }

        if (isCpcOrdinary)
        {
            splitNumbers = callerNumber.split("\\*", 2);

            if (!TextUtils.isEmpty(splitNumbers[0]))
            {
                callerNumber = splitNumbers[0];
            }
        }

        if (callerNumber.equals(SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb()))
        {
            if (VoipFunc.getIns().getConfId() != null)
            {
                VoipFunc.getIns().answerConf();
                return;
            }
        }

        VoipFunc.getIns()
                .showMediaActivity(false, number, displayName, contact, false);
    }

    @Override
    public void onCallConnect(CallSession call)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallConnect ");

        VoipFunc.getIns().setCallSession(call);

        if (VoipFunc.getIns().getConfId() != null)
        {
            ConferenceFunc.getIns().reportTerminalType(
                    VoipFunc.getIns().getConfId());
        }

        VoipFunc.getIns().saveCurrentCallPerson();

        VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_TALKING);
        
    
        //by lwx302895
        if (call.isVideoCallType())
        {
        	//by lwx302895
            VoipFunc.getIns().setVideo(true);
            saveOrient(call.getOrientType());
            VoipFunc.getIns().prepareVideoCall();
        }
        else
        {
            VoipFunc.getIns().setVideo(false);
        }

        if (VoipFunc.getIns().isConf())
        {
            EventData data = new EventData();
            data.setRawData(VoipFunc.getIns().getConfId());
            VoipFunc.getIns().sendBroadcast(VoipFunc.GOTO_CONF_VIEW, data);
        }
        else
        {
            VoipFunc.getIns().postSipNotification(VoipFunc.CALL_CONNECTED);
        }
    }

    @Override
    public void onCallHoldFailed(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallHoldFailed ");

        VoipFunc.getIns().setCallSession(arg0);

        VoipFunc.getIns().postSipNotification(VoipFunc.CALL_HOLD_FAILED);
    }

    @Override
    public void onCallHoldSuccess(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallHoldSuccess ");

        VoipFunc.getIns().setCallSession(arg0);

        VoipFunc.getIns().setHoldStatus(true);
        VoipFunc.getIns().postSipNotification(VoipFunc.CALL_HOLD);
    }

    @Override
    public void onCallRemoveVideo(CallSession arg0)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallRemoveVideo ");

        VoipFunc.getIns().setCallSession(arg0);

        VoipFunc.getIns().setVideo(false);

        VoipFunc.getIns().postSipNotification(VoipFunc.VIDEO_REMOVE);
    }

    @Override
    public void onCallToConf(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallToConf ");
    }

    @Override
    public void onCallVideoRemoveResult(CallSession callSession)
    {

    }

    @Override
    public void onCallUnHoldFailed(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallUnHoldFailed ");

        VoipFunc.getIns().setCallSession(arg0);

        VoipFunc.getIns().postSipNotification(VoipFunc.CALL_HOLD_FAILED);
    }

    @Override
    public void onCallUnHoldSuccess(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onCallUnHoldSuccess ");

        VoipFunc.getIns().setCallSession(arg0);

        VoipFunc.getIns().setHoldStatus(false);
        VoipFunc.getIns().postSipNotification(VoipFunc.CALL_HOLD);
    }

    @Override
    public void onCallVideoAddResult(CallSession session)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallVideoAddResult ");

//        VoipFunc.getIns().setCallSession(session);
//        TupCall tupCall;
//        if (session != null)
//        {
//            tupCall = session.getTupCall();
//            if (1 == tupCall.getIsviedo())
//            {
//                // VoipFunc.getIns().setVideo(true);
//                saveOrient(tupCall.getOrientType());
//                VoipFunc.getIns().postSipNotification(VoipFunc.VIDEO_ADD_SUCESS);
//                return;
//            }
//            else
//            {
//                if (VoipFunc.getIns().isVideo())
//                {
//                    VoipFunc.getIns().setVideo(false);
//                    VoipFunc.getIns().postSipNotification(
//                            VoipFunc.VIDEO_ADD_FAILED);
//                }
//
//            }
//        }
        /** 以上方法注释掉，用新sdk by302895 2016.1.29*/
		if(session.isVideoCall())
		{
			saveOrient(session.getOrientType());
			VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_TALKING);
			VoipFunc.getIns().postSipNotification(VoipFunc.VIDEO_ADD_SUCESS);
		}
		else
		{
			VoipFunc.getIns().setVideo(false);
			VoipFunc.getIns().postSipNotification(VoipFunc.VIDEO_ADD_FAILED);
		}
    }

    @Override
    public void onCallEnd(CallSession callSession)
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onCallend ");

        VoipFunc.getIns().setCallSession(callSession);

        VoipFunc.getIns().setVoipStatus(VoipFunc.STATUS_INIT);
        VoipFunc.getIns().setHoldStatus(false);
        VoipFunc.getIns().setMuteStatus(false);
        // VoipFunc.getIns().setVideo(false);

        SelfInfoUtil.getIns().setStatus(ContactClientStatus.ON_LINE);
        VoipFunc.getIns().clear();

        VoipFunc.getIns().postSipNotification(VoipFunc.CALL_CLOSED);
    }

    @Override
    public void onFrowardNotify(String arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onFrowardNotify ");
    }

    @Override
    public void onNetLevelChange(VoiceQuality arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onNetLevelChange ");
    }

    @Override
    public void onRefreshView()
    {
        Log.d(CommonUtil.APPTAG, TAG + " | onRefreshView ");

        // 1 添加视频；0 删除视频/添加视频失败
        
//        if(REFRESH_LOCAL_ADD == eventType)
//        {
        VoipFunc.getIns().postSipNotification(VoipFunc.REFRESH_LOCAL_VIEW);
//        }
        
    }

    @Override
    public void onRegisterSuccess()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onRegisterSuccess ");

        // VideoFunc.getIns().initView();
        // VideoFunc.getIns().setVideoCaps();
        // VideoFunc.getIns().deployGlobalVideoCaps();
        VideoFunc.getIns().handleVoipRegisterSuccess();
    }

    @Override
    public void onRingBack(CallSession arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onRingBack ");
    }

    @Override
    public void onRouteChange()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onRouteChange ");
    }

    @Override
    public void onVoiceMailNotify(VoiceMailNotifyData arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onVoiceMailNotify ");
    }

    @Override
    public void onRequestHangup(CallSession arg0, int arg1)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onVoiceMailNotify ");
    }

    @Override
    public int reportNofitication(String arg0, int arg1, EventData arg2)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | reportNofitication " + arg0 + "  "
                + arg1);
        return 0;
    }

	@Override
	public void onCallBldTransferRecvSucRsp(CallSession arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOrientChange(int toOrient) {
		// TODO Auto-generated method stub
//        Logger.info(CommonUtil.APPTAG, TupHelper.TAG + "change orient");
//        OrientChange orientChange = new OrientChange();
		//by lwx302895
		OrientChange orientChange = new OrientChange(VideoFunc.transOrient(toOrient));
        orientChange.setOrient(VideoFunc.transOrient(toOrient));
        VoipFunc.getIns().sendBroadcast(VoipFunc.VIDEO_CHANGE_ORIENT, orientChange);
	}

	@Override
	public void onCallStartResult(CallSession arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCallDestroy(CallSession paramCallSession) {
		// TODO Auto-generated method stub
		
	}



}
