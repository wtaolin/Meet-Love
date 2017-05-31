package com.huawei.esdk.uc;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler;
import com.huawei.common.constant.UCResource;
import com.huawei.conference.CtcMemberEntity;
import com.huawei.conference.entity.ConfURLData;
import com.huawei.conference.entity.CtcEntity;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.PhoneContactCache;
import com.huawei.contacts.SearchCache;
import com.huawei.dao.DbVindicate;
import com.huawei.data.Messages;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.LoginFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;
import com.huawei.esdk.uc.login.LoginActivity;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.reportstatistics.controller.EventReporter;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class UCAPIService extends Service
{

    private static final String TAG = UCAPIService.class.getSimpleName();

    private IntentFilter filter;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");

        filter = new IntentFilter();

        filter.addAction(CustomBroadcastConst.ACTION_LOGINOUT_SUCCESS);
        filter.addAction(CustomBroadcastConst.BACK_TO_LOGIN_VIEW);
        filter.addAction(CustomBroadcastConst.ACTION_SET_STATUS_RESPONSE);
        filter.addAction(CustomBroadcastConst.UPDATE_CONTACT_VIEW);
        filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
        filter.addAction(CustomBroadcastConst.ACTION_RECEIVE_MESSAGE);
        
        filter.addAction(CustomBroadcastConst.ACTION_CREATE_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GET_GROUP_PIC);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER);
        filter.addAction(CustomBroadcastConst.ACTION_LEAVE_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GROUP_CHANGE);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_CHAT);
        filter.addAction(CustomBroadcastConst.ACTION_FIX_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_MODIFY_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_MEMBERCHANGE);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE);
        
        
        filter.addAction(CustomBroadcastConst.CTC_CREATE_CONFERENCE_RESPONSE);
        filter.addAction(CustomBroadcastConst.CTC_GET_CONFLIST_RESPONSE);
        filter.addAction(CustomBroadcastConst.CTC_PUSH_CONFINFO);
        filter.addAction(CustomBroadcastConst.CTC_UPDATE_MEMBERSTATUS_PUSH);
        filter.addAction(CustomBroadcastConst.CTC_STOP_CONFERENCE_RESPONSE);
        filter.addAction(CustomBroadcastConst.CTC_GET_MEMBER_RESPONSE);
        filter.addAction(CustomBroadcastConst.CTC_JOIN_CONF);
        filter.addAction(CustomBroadcastConst.CTC_UPGRADE_CONF);
        filter.addAction(CustomBroadcastConst.CTC_PUSH_DATACONF_INFO);
        filter.addAction(CustomBroadcastConst.CTC_UPDATE_CONF_STATUS_PUSH);
        filter.addAction(CustomBroadcastConst.ACTION_QUERY_ROAMING_MESSAGE); //接收漫游消息
        filter.addAction(CustomBroadcastConst.ACTION_CONNECT_TO_SERVER); // 连接上服务器
        filter.addAction(CustomBroadcastConst.ACTION_SEND_MESSAGE_RESPONSE); //群组富媒体消息发送完成
//        filter.addAction(UmFunc.UPLOADPROCESSUPDATE); //富媒体消息发送中
//        filter.addAction(UmFunc.UPLOADFILEFINISH); //富媒体消息发送结束
//        filter.addAction(UmFunc.DOWNLOADFILEFINISH); //富媒体消息接收结束
//        filter.addAction(UmFunc.DOWNLOADPROCESSUPDATE); //富媒体消息接收进行
        filter.addAction(CustomBroadcastConst.CTC_GET_CONF_INFO_RESPONSE);
        registerRec();
        
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    private void registerRec()
    {
//        registerReceiver(broadcastReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    private void unRegistRec()
    {
//        unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void sendBroadcast(String action)
    {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @SuppressWarnings("unchecked")
		@Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
//            Log.d(CommonUtil.APPTAG, TAG + " | action = " + action);

            int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT,
                    UCResource.REQUEST_OK);

            if (CustomBroadcastConst.BACK_TO_LOGIN_VIEW.equals(action))
            {
                removeStickyBroadcast(intent);

                
                LoginFunc.getIns().setLogin(false);
                LoginFunc.getIns().backToLoginView();
                
                VoipFunc.getIns().unRegisterVoip(false);

                clearResource();

                CommonVariables.getIns().clear();//ADDED


                Activity activity = UCAPIApp.getApp().getCurActivity();
                if (activity != null)
                {
                    Intent loginIntent = new Intent(activity,
                            LoginActivity.class);
                    loginIntent.putExtra(IntentData.BEKICKEDOUT, true);
                    activity.startActivity(loginIntent);
                }
            }
//            else if (CustomBroadcastConst.ACTION_GET_GROUP_PIC.equals(action))
//            {
//                sendBroadcast(ACTION.ACTION_GROUP_UPDATE);
//            }
//            else if (CustomBroadcastConst.ACTION_CREATE_GROUP.equals(action))
//            {
//                //GroupFunc.getIns().handleGroupCreate(intent);
//            }
//            else if (CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER
//                    .equals(action))
//            {
//                //sendBroadcast(ACTION.ACTION_GROUPSEND_QUERYMEMBER);
//            }
//            else if (CustomBroadcastConst.ACTION_LEAVE_GROUP.equals(action))
//            {
//                GroupFunc.getIns().handleGroupLeave(intent);
//            }
//            else if (CustomBroadcastConst.ACTION_GROUP_CHANGE.equals(action))
//            {
//                GroupFunc.getIns().handleGroupChange(intent);
//            }
//      
//            else if (CustomBroadcastConst.ACTION_FIX_GROUP.equals(action))
//            {
//                sendBroadcast(ACTION.ACTION_GROUP_UPDATE);
//            }
//            else if (CustomBroadcastConst.ACTION_MODIFY_GROUP.equals(action))
//            {
//                sendBroadcast(ACTION.ACTION_GROUP_UPDATE);
//            }
//            
            
//            else if (CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP
//                    .equals(action))
//            {
//                GroupFunc.getIns().handleGroupInvite(intent);
//            }
//            else if (CustomBroadcastConst.ACTION_GROUPNOTIFY_MEMBERCHANGE
//                    .equals(action))
//            {
//
//            }
//            else if (CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE
//                    .equals(action))
//            {
//                //GroupFunc.getIns().handleGroupDelete(intent);
//            }
            else if (CustomBroadcastConst.ACTION_GROUPSEND_CHAT.equals(action)||(CustomBroadcastConst.ACTION_SEND_MESSAGE_RESPONSE.equals(action)))
            {
                ImFunc.getIns().handleSendMessageResponse(intent);
            }
            else if (CustomBroadcastConst.ACTION_RECEIVE_MESSAGE.equals(action)) //收到消息
            {
                if (UCResource.REQUEST_OK == result) {
                    Messages messages = (Messages) intent
                            .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    ImFunc.getIns().handleInComingMessage(messages.getMsgList());
                }
            }
            
            else if (CustomBroadcastConst.CTC_CREATE_CONFERENCE_RESPONSE
                    .equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns()
                        .handleCreateConferenceResp(result, data);
            }
            else if (CustomBroadcastConst.CTC_GET_CONFLIST_RESPONSE
                    .equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                if (data.getStatus() == ResponseCodeHandler.ResponseCode.REQUEST_SUCCESS && data instanceof CtcEntity)
                {
                    CtcEntity temp = (CtcEntity) data;
//                    ConferenceDataHandler.getIns().
                }
                ConferenceFunc.getIns().handleConferenceListResp(result, data);
            }
            else if (CustomBroadcastConst.CTC_PUSH_CONFINFO.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handlePushConferenceInfo(result, data);
            }
            else if (CustomBroadcastConst.CTC_UPDATE_MEMBERSTATUS_PUSH
                    .equals(action))
            {
                ArrayList<CtcMemberEntity> ctcMemberEntities = (ArrayList<CtcMemberEntity>) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handleUpdateMemberStatus(
                        ctcMemberEntities);
            }
            else if (CustomBroadcastConst.CTC_STOP_CONFERENCE_RESPONSE
                    .equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handleEndConferenceResp(result, data);
            }
            else if (CustomBroadcastConst.CTC_GET_MEMBER_RESPONSE
                    .equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handleConferenceMemResp(result, data);
            }
            else if (CustomBroadcastConst.CTC_JOIN_CONF.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handleJoinConfResp(result, data);
            }
            else if (CustomBroadcastConst.CTC_UPGRADE_CONF.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                ConferenceFunc.getIns().handleUpdateDataConf(result, data);
            }
            else if (CustomBroadcastConst.CTC_PUSH_DATACONF_INFO.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                if (data instanceof ConfURLData)
                {
                    ConferenceFunc.getIns().handleDataConfUrl(
                            (ConfURLData) data);
                }
            }
            else if (CustomBroadcastConst.CTC_UPDATE_CONF_STATUS_PUSH.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    ConferenceFunc.getIns().handleUpdateConference(result, data);
            }
            else if(CustomBroadcastConst.ACTION_QUERY_ROAMING_MESSAGE.equals(action)) //接收到消息漫游返回数据
            {
            	ImFunc.getIns().handlerHistoryMessageResponse(intent);
            }
            else if(UmConstant.UPLOADPROCESSUPDATE.equals(action)) //富媒体消息发送中
            {
            	Log.d("UM", TAG + " | action = " + action);
            	UmReceiveData data = (UmReceiveData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
            	ImFunc.getIns().handleUmMsgUploadProcessUpdate(data);
            }
            else if(UmConstant.DOWNLOADFILEFINISH.equals(action) || UmConstant.UPLOADFILEFINISH.equals(action)) //富媒体消息发送接收完成
            {
            	Log.d("UM", TAG + " | action = " + action);
            	UmReceiveData data = (UmReceiveData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
            	ImFunc.getIns().handleUmMsgUploadFileFinish(data);
            }
            else if(UmConstant.DOWNLOADPROCESSUPDATE.equals(action)) //富媒体消息接收中
            {
            	Log.d("UM", TAG + " | action = " + action);
            	UmReceiveData data = (UmReceiveData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
            	ImFunc.getIns().handleUmMsgDownloadProcessUpdate(data);
            }
            //返回的Data中包含预约会议与会者信息
            //by lwx302895
            else if (CustomBroadcastConst.CTC_GET_CONF_INFO_RESPONSE.equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                if (data.getStatus() == ResponseCodeHandler.ResponseCode.REQUEST_SUCCESS && data instanceof CtcEntity)
                {
                    CtcEntity temp = (CtcEntity) data;
                    //获取预约会议
                    List<CtcMemberEntity> ctcMemberEntities = temp.getParticates();
                    if (ctcMemberEntities != null)
                    {
                        String confId = temp.getConfId();
                        ConferenceDataHandler.getIns().saveBookConfMember(confId, ctcMemberEntities);
                    }
                }
            }
            
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Log.d(CommonUtil.APPTAG, TAG + " | onDestroy");

        unRegistRec();
    }
    
    
    private void logout()
    {

    }
    
    
    /**
     *清除资源
     */
    public void clearResource()
    {
        //清除线程管理器
        ThreadManager.getInstance().clearThreadResource();

        //关闭数据库
        DbVindicate.getIns().closeDb();
        
        //清除数据
        clearContactData();

        //NotificationUtil.cancelAll();
    }
    /**
     * 注销时清理联系人相关数据
     */
    private void clearContactData()
    {
    	//方法变化 by lwx302895
//        HeadPhotoUtil.getInstance().cleanPhotos();
    	HeadPhotoUtil.getIns().cleanPhotos();

        SelfInfoUtil.getIns().clear();
        VoipFunc.getIns().clear();
        UmFunc.getIns().clear();
        ImFunc.getIns().clear();
        PhoneContactCache.getIns().clear();
        ContactCache.getIns().clear();
//        VoiceMessageFunc.getIns().clear();
//        VoiceMsgHandler.getIns().clear();
        SearchCache.getIns().clear();
        EventReporter.getIns().clear();

        //DataInitLogic.getIns().unInit();
    }
}
