package com.huawei.esdk.uc.function;

import java.io.File;
import java.util.List;

import com.huawei.common.CommonVariables;
import com.huawei.common.res.LocContext;
import com.huawei.conference.entity.ConfURLData;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.device.DeviceManager;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.meeting.ConfDefines;
import com.huawei.meeting.ConfExtendPhoneInfoMsg;
import com.huawei.meeting.ConfMsg.VideoDeviceChange;
import com.huawei.meeting.ConferenceDataNotification;
import com.huawei.meeting.DataConference;
import com.huawei.meeting.common.MeetingCommonParam.ConfOsType;
import com.huawei.meeting.message.AudioCodecNotifyMsg;
import com.huawei.meeting.message.CommonMemberInfoMsg;
import com.huawei.meeting.message.ConfInstantNotifyMsg;
import com.huawei.meeting.message.MemberAudioStatusNotifyMsg;
import com.huawei.meeting.message.MicMaxVoiceNotifyMsg;
import com.huawei.meeting.message.NewConfMsg;
import com.huawei.meeting.message.PhoneCallStatusNotifyMsg;
import com.huawei.meeting.message.SendUserDataNotifyMsg;
import com.huawei.meeting.message.UpdateParamNotifyMsg;
import com.huawei.meeting.message.VideoDeviceInfo;
import com.huawei.meeting.message.VideoNotifyMsg;
import com.huawei.meeting.message.VideoSwitchOnNotifyMsg;
import com.huawei.utils.StringUtil;
import com.huawei.utils.io.FileUtil;

import android.text.TextUtils;
import android.util.Log;

public class DataConfFunc implements ConferenceDataNotification
{

    private static final String TAG = DataConfFunc.class.getSimpleName();

    private DataConference multiConfService;

    private ConferenceEntity curMultiConf;

    private static final String CONFERENCE_LOG_PATH = LocContext.getContext()
            .getFilesDir() + "/eSpaceAppLog/conference";

    private static final String CONFERENCE_TEMPDATA_PATH = FileUtil
            .getSdcardPath() + "/eSpace/conference";

    public void JoinDataConf(ConferenceEntity conf, ConfURLData dataUrl)
    {
        curMultiConf = conf;

        NewConfMsg newConfInfo = new NewConfMsg();

        int componentFlag = ConfDefines.IID_COMPONENT_AS
                | ConfDefines.IID_COMPONENT_CHAT | ConfDefines.IID_COMPONENT_WB
                | ConfDefines.IID_COMPONENT_DS;

        // 融合会议不能加载视频模块，以免与FAST视频模块冲突，导致视频异常
        if (!curMultiConf.isMcu())
        {
            componentFlag |= ConfDefines.IID_COMPONENT_VIDEO;
        }

        newConfInfo.setComponentFlag(componentFlag);
        newConfInfo.setConfLog(CONFERENCE_LOG_PATH);
        newConfInfo.setConfTempDir(CONFERENCE_TEMPDATA_PATH);
        newConfInfo.setAnnoPath(ConferenceFunc.DATACONF_RES_PATH
                + File.separator); // 数据共享时的资源

        int option = ConfDefines.CONF_OPTION_USERLIST;
        // multiple server first
        option = option | ConfDefines.CONF_OPTION_LOAD_BALANCING;
        // phone user join conference
        option = option | ConfDefines.CONF_OPTION_PHONE;
        option = option | ConfDefines.CONF_OPTION_QOS;
        option = option | ConfDefines.CONF_OPTION_HOST_NO_GRAB;
        newConfInfo.setOption(option);

        multiConfService = new DataConference(this);

        int userType;

        if (curMultiConf.isDataConfControlEnable())
        {
            userType = DataConference.CONF_ROLE_HOST;
        }
        else
        {
            userType = DataConference.CONF_ROLE_GENERAL;
        }

        PersonalContact self = ContactLogic.getIns().getMyContact();

        String userName = "";

        if (self != null && !TextUtils.isEmpty(self.getName()))
        {
            userName += self.getName() + " " + self.getEspaceNumber();
        }
        else
        {
            userName = CommonVariables.getIns().getUserAccount();
        }

        long userId = ContactLogic.getIns().getMyOtherInfo().getUserId();

        if (self != null && 0 == userId)
        {
            Long tempLong = Long.getLong(self.getBinderNumber());
            if (tempLong != null)
            {
                userId = tempLong;
            }
        }

        String dataConfID = StringUtil.findStringElement(dataUrl.getConfId(),
                "sip:", "@");

        if (TextUtils.isEmpty(dataConfID))
        {
            dataConfID = dataUrl.getConfId();
        }

        String serverIP = dataUrl.getDataConfURL();
        String confkey = dataUrl.getToken();
        String siteID = dataUrl.getSiteId();
        String userLogUri = dataUrl.getAttendNum();
        // String strHostRole = dataUrl.getHostKey();
        String siteUrl = dataUrl.getCmAddress();

        // IPT2.2新加字段
        if (!TextUtils.isEmpty(siteUrl))
        {
            newConfInfo.setSiteUrl(siteUrl);
        }

        // 或者为纯数字，或者为sip:***@domain
        /** makeUpLogUri参数添加了domain参数 by lwx302895 time2016-1-29*/
//        userLogUri = NewConfMsg.makeUpLogUri(userLogUri);
        String domain = ContactLogic.getIns().getMyContact().getDomain();
		userLogUri = NewConfMsg.makeUpLogUri(userLogUri,domain);
        // 如果为空，就返回bindNumber
        if (self != null && (TextUtils.isEmpty(userLogUri)))
        {
            userLogUri = self.getBinderNumber();
        }

        if (0 == userId || TextUtils.isEmpty(dataConfID)
                || TextUtils.isEmpty(serverIP)
                || TextUtils.isEmpty(confkey)
                || TextUtils.isEmpty(siteID))
        {
            return;
        }

        newConfInfo.setUserType(userType);
        newConfInfo.setConfId(dataConfID);
        newConfInfo.setUserId(userId);
        newConfInfo.setDeviceType(3);
        newConfInfo.setOsType(ConfOsType.CONF_OS_ANDROID.value());
        newConfInfo.setSiteId(siteID);
        newConfInfo.setUserName(userName);
        newConfInfo.setConfTitle(curMultiConf.getSubject());
        newConfInfo.setServerIp(serverIP);
        newConfInfo.setEncrytionKey(confkey);
        // newConfInfo.setStrHostRole(strHostRole);
        newConfInfo.setUserLogUri(userLogUri);

        multiConfService.joinConf(newConfInfo);
    }

    @Override
    public void onAudioCodecResponse(AudioCodecNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onAudioCodecResponse ");

    }

    @Override
    public void onBindPhoneUser(ConfExtendPhoneInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onBindPhoneUser ");
    }

    @Override
    public void onComponentChange(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onComponentChange ");
    }

    @Override
    public void onComponentLoaded(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onComponentLoaded ");
    }

    @Override
    public void onConfLock(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onConfLock ");
    }

    @Override
    public void onConfMemberAudioDeviceStatusChange(
            MemberAudioStatusNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG
                + " | onConfMemberAudioDeviceStatusChange ");
    }

    @Override
    public void onConfModelUpdate(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onConfModelUpdate ");
    }

    @Override
    public void onConfTerminate()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onConfTerminate ");
    }

    @Override
    public void onDesktopDataRecv()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onDesktopDataRecv ");
    }

    @Override
    public void onDocPageChange(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onDocPageChange ");
    }

    @Override
    public void onGetVideoDeviceInfo(VideoDeviceInfo arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onGetVideoDeviceInfo ");
    }

    @Override
    public void onGetVideoDeviceNum(int arg0, long arg1, String arg2)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onGetVideoDeviceNum ");

    }

    @Override
    public void onGetVideoParam(int arg0, long arg1, String arg2)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onGetVideoParam ");

    }

    @Override
    public void onHosterChanged(long arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onHosterChanged ");

    }

    @Override
    public void onJoinConfResponse(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onJoinConfResponse ");

    }

    @Override
    public void onJoinPhoneSession(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onJoinPhoneSession ");

    }

    @Override
    public void onKickout(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onKickout ");

    }

    @Override
    public void onMemberEnter(CommonMemberInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMemberEnter ");

    }

    @Override
    public void onMemberInfoModified(CommonMemberInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMemberInfoModified ");

    }

    @Override
    public void onMemberLeave(CommonMemberInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMemberLeave ");

    }

    @Override
    public void onMemberOffLine(CommonMemberInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMemberOffLine ");

    }

    @Override
    public void onMemberReConnected(CommonMemberInfoMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMemberReConnected ");

    }

    @Override
    public void onMuteAllConfPhone(boolean arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onMuteAllConfPhone ");

    }

    @Override
    public void onNetBroken()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onNetBroken ");

    }

    @Override
    public void onNetReconnect()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onNetReconnect ");

    }

    @Override
    public void onOpenMic(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onOpenMic ");

    }

    @Override
    public void onPauseShareDesktop()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPauseShareDesktop ");

    }

    @Override
    public void onPhoneCallMute(PhoneCallStatusNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPhoneCallMute ");

    }

    @Override
    public void onPhoneCallVideoCapableChange(PhoneCallStatusNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPhoneCallVideoCapableChange ");

    }

    @Override
    public void onPhoneUserEnter(PhoneCallStatusNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPhoneUserEnter ");

    }

    @Override
    public void onPhoneUserLeave(PhoneCallStatusNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPhoneUserLeave ");

    }

    @Override
    public void onPresenterChanged(long arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onPresenterChanged ");

    }

    @Override
    public void onReceiveAudioMuteAction(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveAudioMuteAction ");

    }

    @Override
    public void onReceiveChatMsg(ConfInstantNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveChatMsg ");

    }

    @Override
    public void onReceiveMaxVoice(List<MicMaxVoiceNotifyMsg> arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveMaxVoice ");

    }

    @Override
    public void onReceiveUserMsgData(SendUserDataNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveUserMsgData ");

    }

    @Override
    public void onReceiveVideoMaxOpenNumber(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveVideoMaxOpenNumber ");

    }

    @Override
    public void onReceiveVideoOperRequest(VideoNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onReceiveVideoOperRequest ");

    }

    @Override
    public void onSetMaxOpenAudioDevice(int arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onSetMaxOpenAudioDevice ");

    }

    @Override
    public void onSharedMemberChanged(int arg0, int arg1, long arg2)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onSharedMemberChanged ");

    }

    @Override
    public void onStartDocShare()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStartDocShare ");

    }

    @Override
    public void onStartShareDesktop()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStartShareDesktop ");

    }

    @Override
    public void onStartWhiteBoard()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStartWhiteBoard ");

    }

    @Override
    public void onStopDocShare()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStopDocShare ");

    }

    @Override
    public void onStopShareDesktop()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStopShareDesktop ");

    }

    @Override
    public void onStopWhiteBoard()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onStopWhiteBoard ");

    }

    @Override
    public void onUpdateParam(UpdateParamNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onUpdateParam ");

    }

    @Override
    public void onVideoDeviceInfoChange(VideoDeviceChange arg0,
            VideoDeviceInfo arg1)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onVideoDeviceInfoChange ");

    }

    @Override
    public void onVideoSwitchOn(VideoSwitchOnNotifyMsg arg0)
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onVideoSwitchOn ");

    }

    @Override
    public void onWhiteBoardChange()
    {
        // TODO Auto-generated method stub
        Log.d(CommonUtil.APPTAG, TAG + " | onWhiteBoardChange ");

    }

}
