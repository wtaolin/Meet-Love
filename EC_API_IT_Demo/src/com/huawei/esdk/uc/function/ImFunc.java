package com.huawei.esdk.uc.function;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.common.LocalBroadcast;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.ContactTools;
import com.huawei.contacts.MyAbility;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.group.ConstGroupManager;
import com.huawei.dao.impl.InstantMessageDao;
import com.huawei.dao.impl.RecentChatContactDao;
import com.huawei.data.ChatResp;
import com.huawei.data.ConstGroup;
import com.huawei.data.ExecuteResult;
import com.huawei.data.GetRoamingMessageData;
import com.huawei.data.Message;
import com.huawei.data.Messages;
import com.huawei.data.UnReadMessageNotifyDataList;
import com.huawei.data.UnreadMessageNotifyData;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.ConversationEntity;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.InstantMessageFactory;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.data.entity.RecentChatter;
import com.huawei.data.unifiedmessage.JsonMultiUniMessage;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.FuncResultCode;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.im.ChatActivity;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.esdk.uc.utils.ChatUtil;
import com.huawei.esdk.uc.utils.FileUtil;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.factory.ResourceGenerator;
import com.huawei.log.TagInfo;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.MessageSender;
import com.huawei.module.um.ResourcesParser;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.module.um.UmUtil;
import com.huawei.service.ServiceProxy;
import com.huawei.utils.AndroidLogger;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;
import com.huawei.utils.Tools;
import com.huawei.utils.img.PhotoUtil;
import com.huawei.utils.security.FileSHA1;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImFunc
{

    private static final String TAG = ImFunc.class.getSimpleName();

    /**
     * 消息类型：发送消息
     */
    public static final int SENDMESSAGE_TYPE = 1;

    /**
     * 消息类型：接收消息
     */
    public static final int RECEIVEMESSAGE = 2;

    /**
     * 置消息已读：IM
     */
    public static final int MARK_TYPE_IM = 1;

    /**
     * 置消息已读：固定群
     */
    public static final int MARK_TYPE_GROUP = 2;

    /**
     * 置消息已读类型：IM
     */
    public static final int MARK_MESSAGE_TAG_IM = 0;
    /**
     * 置消息已读类型：固定群/讨论组
     */
    public static final int MARK_MESSAGE_TAG_GROUP = 1;

    /**
     * 未读离线消息
     */
    public static final int UNREAD_MSG_OFFLINE = -1;

    private static ImFunc instance;

    private Object imLock = new Object();

    private final Object imbroadcastLock = new Object();

    @SuppressLint("UseSparseArrays")
    private Map<Integer, InstantMessage> imRequestIDMap = new HashMap<Integer, InstantMessage>();

    private final List<LocalImReceiver> imReceivers = new LinkedList<LocalImReceiver>();

    private HistoryMsgMergerManager historyMsgMergerManager;

    private Handler handler; // 处理聊天界面的数据刷新

    public Handler getHandler()
    {
        return handler;
    }

    public void setHandler(Handler handler)
    {
        this.handler = handler;
    }


    /**为了添加完未读消息后刷新页面 by wx303895*/

    private  Handler viewHandler ;

    /***/

    /**
     * 界面每次刷新消息条数.
     */
    public static final int MAX_MSGS = 10;

    @SuppressLint("UseSparseArrays")
    private final Map<Integer, GetHistoryMessageInfo> getHistoryMessageInfoMap = new HashMap<Integer, GetHistoryMessageInfo>();

    private ImFunc()
    {

    }

    // 这个是仿照espace的实现逻辑来做的消息接收,by wx303895
     private final BaseReceiver localReceiver = new BaseReceiver()
     {


        @Override
        public void onReceive(String action, BaseData data)
        {
            if (data == null || !(data instanceof LocalBroadcast.ReceiveData))
            {
                return;
            }
            final LocalBroadcast.ReceiveData d = (LocalBroadcast.ReceiveData) data;

            ThreadManager.getInstance().addLargeMessageThread(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (CustomBroadcastConst.ACTION_UNREAD_MESSAGE_NOTIFY.equals(d.action))
                            {
                                handleUnreadMessageNotify(d);
                            }
                        }
                    }
            );
        }
    };

    /**
     * 以下方法为了解决注销后接收的消息提醒问题 by303895
     * start
     * */
    public void handleUnreadMessageNotify(LocalBroadcast.ReceiveData resp)
    {
        if (!(resp.data instanceof UnReadMessageNotifyDataList))
        {
            Logger.error(TagInfo.APPTAG, "no data");
            return;
        }
        if (!ContactLogic.getIns().getAbility().isIMAbility())
        {
            return;
        }

        UnReadMessageNotifyDataList mData = (UnReadMessageNotifyDataList) resp.data;
        List<UnreadMessageNotifyData> datas = mData.getDataList();
        if (datas == null)
        {
            return;
        }
        for (UnreadMessageNotifyData data : datas)
        {
            parseUnreadMessageNotifyData(data);
        }

        if(viewHandler != null)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = UNREAD_MSG_OFFLINE;
            viewHandler.sendMessage(msg);
        }
    }

    /**读取完未读消息后刷新一下ui by wx303895*/
    public void setViewHandler(Handler viewHandler)
    {
        /**添加广播为了接受注销后发来的消息 bywx303895*/
        String[] broadcast = new String[]
                {
                        CustomBroadcastConst.ACTION_UNREAD_MESSAGE_NOTIFY
                };

        LocalBroadcast.getIns().registerBroadcast(localReceiver, broadcast);

        historyMsgMergerManager = new HistoryMsgMergerManager();

        this.viewHandler = viewHandler;
		//注册handler
        UnreadMessageManager.getIns().setUnReadHandler(viewHandler);
    }

    private void parseUnreadMessageNotifyData(UnreadMessageNotifyData data)
    {

        if (!data.getMessageList().isEmpty())
        {
            List<Message> messageList = data.getMessageList();
//            if (messageList == null || messageList.size() <= 0)
//            {
//                return;
//            }
//            for (Message msg : messageList)
//            {
//                UnreadMessageManager.getIns().saveUnreadMsg(msg.getMessageId(), msg);
//            }
            //每次登录时推送过来的消息按接收在线消息流程来，否则会出现离线群组消息接收后无法显示
            handleInComingMessage(messageList);

            int unreadNumber = getNotifyUnreadNumber(data.getMessageList(), data.getUnreadNumber());
            UnreadMessageManager.getIns().saveUnreadNumber(data.getOppoAccount(), unreadNumber);
        }
    }


    private int getNotifyUnreadNumber(List<Message> messages, int oldUnreadNumber)
    {
        int unreadNumber = oldUnreadNumber;

        String[] idArray = new String[messages.size()];

        for (int i = 0; i < idArray.length; i++)
        {
            idArray[i] = messages.get(i).getMessageId();
        }

        Arrays.sort(idArray, Collections.reverseOrder());

        String[] readIdArray = InstantMessageDao.pickAlreadyReadMsgIds(idArray);

        if (readIdArray != null && readIdArray.length > 0)
        {
            Arrays.sort(readIdArray, Collections.reverseOrder());

            String lastReadId = readIdArray[0];

            int index = StringUtil.queryElement(idArray, lastReadId);

            // ֻ���������δ����Ϣ�����ڱ��ػ�ȡ�������ݿ����δ���������ñ������ݿ���߼�ȡ������Ȼ�����Է�����Ϊ׼
            if (index >= 0 && oldUnreadNumber > index)
            {
                unreadNumber = index;
//                    markRead();
            }
        }

        return unreadNumber;
    }

    /** end */

    public static ImFunc getIns()
    {
        if (null == instance)
        {
            instance = new ImFunc();
        }
        return instance;
    }

    /**
     * 发送单人聊天消息
     * 
     * @param msg
     * @param espaceNum
     * @return
     */
    public boolean sendMessage(String msg, String espaceNum)
    {
        ServiceProxy serviceProxy = UCAPIApp.getApp().getService();
        if (null == serviceProxy)
        {
            return false;
        }

//        MessageSender sender = getMessageSender(espaceNum, false);
//        InstantMessage message = sender.sendIm(msg, true);

        InstantMessage message = ImFunc.getIns().genMsg(msg, null,
                InstantMessage.STATUS_SEND, true,
                RecentChatContact.ESPACECHATTER);

        ExecuteResult result = sendMessage(message, false,
                MediaResource.TYPE_NORMAL);

        if (result.isResult())
        {
            return true;
        }
        return false;
    }

    private MessageSender getMessageSender(String groupId, boolean isGroup)
    {
        return new MessageSender(groupId, isGroup)
        {
            @Override
            protected void onFail()
            {
                InstantMessage message = getMessage();

//                handleNeedToastResult(message, false);
                updateStatus(message,message.getStatus());
            }

            @Override
            protected void onSuccess()
            {
                InstantMessage message = getMessage();
//                handleNeedToastResult(message, true);

                updateStatus(message,message.getStatus());
            }
        };
    }


    /**
     * 注册消息广播接收
     * 
     * @param receiver
     *            消息接收监听
     * @return 是否注册成功
     */
    public boolean registerBroadcast(LocalImReceiver receiver)
    {
        if (receiver == null)
        {
            return false;
        }

        synchronized (imbroadcastLock)
        {
            imReceivers.add(receiver);
        }

        return true;
    }

    /**
     * 注销消息广播接收
     * 
     * @param receiver
     *            消息接收监听
     * @return 是否注销成功
     */
    public boolean unRegisterBroadcast(LocalImReceiver receiver)
    {
        if (receiver == null)
        {
            return false;
        }

        synchronized (imbroadcastLock)
        {
            imReceivers.remove(receiver);
        }

        return true;
    }

    /**
     * 判断是否是单人IM消息
     * 
     * @param type
     * @return
     */
    public boolean isImMessage(int type)
    {
        return type == Message.IM_CHAT || type == Message.IM_UNDELIVER
                || type == Message.IM_OVERSIZE
                || type == Message.IM_FILE_TRANSFER;
    }

    /**
     * 判断是否是讨论组消息
     * 
     * @param type
     * @return
     */
    public boolean isGroupMessage(int type)
    {
        return type == Message.IM_GROUPCHAT
                || type == Message.IM_GROUPCHATUNDELIVER
                || type == Message.IM_GROUPCHATOVERSIZE
                || type == InstantMessage.TYPE_GROUP_MEM_JOIN
                || type == InstantMessage.TYPE_GROUP_MEM_LEAVE
                || type == InstantMessage.TYPE_GROUP_NAME_MODIFIED;
    }

    /**
     * 判断消息是否是来自某人的IM消息
     * 
     * @param msg
     * @param espaceNumber
     * @return
     */
    public boolean isCurrentIMChat(InstantMessage msg, String espaceNumber)
    {
        if (isImMessage(msg.getType()) && espaceNumber.equals(msg.getFromId()))
        {
            return true;
        }

        return false;
    }

    /**
     * 判断消息是否是来自某个讨论组的IM消息
     * 
     * @param msg
     * @param groupId
     * @return
     */
    public boolean isCurrentGroupChat(InstantMessage msg, String groupId)
    {
        if (isGroupMessage(msg.getType()) && groupId.equals(msg.getToId()))
        {
            return true;
        }
        return false;
    }

    /**
     * 发送群组消息
     * 
     * @param groupId
     * @param groupType
     *            ConstGroup.DISCUSSION:讨论组
     * @param im
     */
    // public void sendGroupMsg(String groupId, int groupType, InstantMessage
    // im)
    // {
    // saveMessage(im, SENDMESSAGE_TYPE);
    //
    // String from = im.getFromId();
    // String type = "groupchat";
    // ConstGroup group = GroupFunc.getIns().findConstGroupById(groupId);
    // if (null == group)
    // {
    // return;
    // }
    //
    // String owner = group.getOwner();
    // ExecuteResult result = ConstGroupManager.ins().sendGroupMessage(
    // groupId, from, type, owner, im.getContent(), groupType,
    // im.getMediaType(), im.getMessageId());
    // synchronized (imLock)
    // {
    // if (result != null)
    // {
    // imRequestIDMap.put(result.getId(), im);
    // }
    // }
    // }

    // public InstantMessage generateMsg(String msg, MediaResource mediaRes,
    // String status, int type)
    // {
    // InstantMessage im = new InstantMessage();
    // im.setContent(msg);
    // im.setFromId(CommonVariables.getIns().getUserAccount());
    // im.setTimestamp(new Timestamp(System.currentTimeMillis()));
    // im.setStatus(status);
    // im.setMediaRes(mediaRes);
    // im.setMsgType(type);
    // return im;
    // }

    /**
     * 处理发消息响应，包括单人，讨论组消息。
     * 
     * @param intent
     */
    public void handleSendMessageResponse(Intent intent)
    {
        Logger.info(CommonUtil.APPTAG, TAG + " | handleSendMessageResponse");

        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
        BaseResponseData data = (BaseResponseData) intent
                .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
        InstantMessage im;
        synchronized (imLock)
        {
            Logger.info(CommonUtil.APPTAG,
                    TAG + " | handleSendMessageResponse------baseId = "
                            + data.getBaseId());
            im = imRequestIDMap.remove(data.getBaseId());
        }

        if (im == null)
        {
            Logger.info(CommonUtil.APPTAG, TAG
                    + " | response, the Im is not existed!");
            return;
        }

        if (UCResource.REQUEST_OK == result)
        {
            if (ResponseCode.REQUEST_SUCCESS == data.getStatus())
            {
                ChatResp chatResp = (ChatResp) data;
                im.setStatus(InstantMessage.STATUS_SEND_SUCCESS);
                Timestamp timeStamp = new Timestamp(chatResp.getUtcTime());
                im.setTimestamp(timeStamp);

                Logger.info(CommonUtil.APPTAG, TAG
                        + " | handleSendMessageResponse------getMsgId = "
                        + chatResp.getMsgId());

                if (!TextUtils.isEmpty(chatResp.getMsgId()))
                {
                    im.setMessageId(chatResp.getMsgId());
                    InstantMessageDao.updateMessageIdAndStatus(im);
                    InstantMessageDao.update(im, InstantMessageDao.TIME, im
                            .getTimestamp().getTime()); // 更新消息发送时间，与服务器保持一致
                }
                else
                {
                    // 仅更新状态
                    InstantMessageDao.updateMessageIdAndStatus(im);
                }

                if (chatResp.getUtcTime() > 0)
                {
                    im.setTimestamp(new Timestamp(chatResp.getUtcTime()));
                    InstantMessageDao.update(im, InstantMessageDao.TIME, im
                            .getTimestamp().getTime());
                }

                // 刷新chatActivity;
                onImReceiver(im, true);
                return;
            }
        }

        updateStatus(im, InstantMessage.STATUS_SEND_FAILED);
    }

    /**
     * 处理获取漫游消息请求返回的数据
     * 
     * @param resp
     *            返回的结果
     * */
    public void handlerHistoryMessageResponse(Intent resp)
    {
        if (resp == null)
        {
            Logger.warn(TAG, "some is null");
            return;
        }

        BaseResponseData rspData = (BaseResponseData) resp
                .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

        GetRoamingMessageData msgData = null;
        if (rspData instanceof GetRoamingMessageData)
        {
            msgData = (GetRoamingMessageData) rspData;
        }

        // GetRoamingMessageData msgData =
        // (GetRoamingMessageData)resp.getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

        if (msgData == null)
        {
            return;
        }

        Logger.debug(TAG, "history message back");
        GetHistoryMessageInfo info = getHistoryMessageInfoMap.remove(msgData
                .getBaseId());

        if (info == null)
        {
            return;
        }

        if (rspData.getStatus() == ResponseCode.REQUEST_SUCCESS)
        {
            handleHistoryMessageList(msgData, info);
        }

        // 无论是否响应成功，都需要返回数据
        requestHistoryFromLocal(info);
    }

    private void requestHistoryFromLocal(GetHistoryMessageInfo info)
    {

        Logger.info(TAG,
                "requestHistoryFromLocal ======== " + info.getAccount() + ","
                        + info.getType() + "," + info.getMsgDaoId() + ","
                        + info.getTime());
        List<InstantMessage> messageList = getHistoryById(info.getAccount(),
                MAX_MSGS, info.getType(), info.getMsgDaoId(), info.getTime());
        
        if(messageList == null || messageList.size() == 0)
        {
         Logger.info(TAG, "messageList size empty ");
         onRequestHistoryBack(messageList, info);
         return;
        }

        Logger.info(TAG, "messageList size ======== " + messageList.size());

        //SimpleDateFormat sd = new SimpleDateFormat("MM-dd HH:mm:ss");
        
        for (InstantMessage item : messageList)
        {

            Timestamp time = item.getTimestamp();
            

            Calendar calendar;
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time.getTime());

            Logger.info(TAG, "id:" + item.getMessageId() + ", time ======== "
                    + DateUtil.format(item.getTimestamp(), DateUtil.FMT_HMS));
        }

        if (!info.isHaveHistory())
        {
            info.setHaveHistory(messageList != null
                    && messageList.size() == MAX_MSGS);
        }

        onRequestHistoryBack(messageList, info);
    }

    /**
     * 通过某条消息获取前面指定的消息记录
     * 
     * @param account
     *            账号
     * @param msgType
     *            消息类型 群聊天/单聊天
     * @param id
     *            消息id
     * @return 通过消息id获取前40条聊天记录.
     */
    public List<InstantMessage> getHistoryById(String account, int number,
            int msgType, long id, long time)
    {
        List<InstantMessage> msgs;

        boolean isHistoryMsgMerger = ContactLogic.getIns().getAbility()
                .isHistoryMsgMerger();

        Logger.debug(TAG, "sort history message by TIME : "
                + isHistoryMsgMerger);

        if (isHistoryMsgMerger)
        {
            msgs = InstantMessageDao.queryRecordByTime(account, number,
                    msgType, id, time);

        }
        else
        {
            msgs = InstantMessageDao.queryRecord(account, number, msgType, id);
        }

        return msgs;
    }

    /**
     * 处理接收到的IM消息，包括单人，讨论组消息。
     * 
     * @param //intent
     */
    public void handleInComingMessage(List<Message> messages)
    {
//        Log.d(CommonUtil.APPTAG, TAG + " | handleInComingMessage");
//
//        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
//
//        if (UCResource.REQUEST_OK == result)
//        {
//            Messages messages = (Messages) intent
//                    .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

            //by lwx302895 未读消息小红点显示 start
            //每次来的未读消息都存放到map中
            List<Message> messageList = messages;
            if (messageList == null || messageList.size() <= 0)
            {
                return;
            }
            for (Message msg : messageList)
            {
                UnreadMessageManager.getIns().saveUnreadMsg(msg.getMessageId(), msg);
            }
            //end

            List<Message> msgList = messages;

            MyAbility ability = ContactLogic.getIns().getAbility();

            if (msgList == null || msgList.isEmpty() || !ability.isIMAbility())
            {
                return;
            }

            Message msg;

            List<ConversationEntity> conversationEntities = new ArrayList<ConversationEntity>();

            for (int i = 0; i < msgList.size(); i++)
            {
                msg = msgList.get(i);

                matchPersonalContact(msg);

                switch (msg.getType())
                {
                case Message.IM_SYSTEM_NOTIFY:
                    // handleInComingSystemMsg(msg, conversationEntities);
                    break;
                case Message.IM_CHAT:
                case Message.IM_OVERSIZE:
                case Message.IM_UNDELIVER:
                case Message.IM_FILE_TRANSFER:
                    handleInComingMsg(msg, RecentChatContact.ESPACECHATTER);
                    break;
                case Message.IM_GROUPCHAT:
                case Message.IM_GROUPCHATOVERSIZE:
                case Message.IM_GROUPCHATUNDELIVER:
                    if (ability.isConstGroupAbility()
                            && msg.getGroupType() == ConstGroup.FIXED)
                    {
                        handleInComingMsg(msg, RecentChatContact.GROUPCHATTER);
                    }
                    else if (ability.isDiscussGroupAbility()
                            && msg.getGroupType() == ConstGroup.DISCUSSION)
                    {
                        handleInComingMsg(msg,
                                RecentChatContact.DISCUSSIONCHATTER);
                    }
                    break;
                default:
                    break;
                }
                msg.sendReply();
            }
//        }
    }

    private void handleInComingMsg(Message msg, int msgType)
    {
        if (!preReceiveMessage(msg, msgType))
        {
            return;
        }

        markRead(getMarkType(msgType), msg.getFrom(), msg.getMessageId(),
                RECEIVEMESSAGE);
//by lwx302895
//        InstantMessage myMsg = new InstantMessage(msg, msgType);
        InstantMessage myMsg = InstantMessageFactory.createInstantMessage(msg, msgType);
        // json 类型的消息直接构造MediaResource
        if (msg.getContentType() == MediaResource.MEDIA_JSON_MUTLI)
        {
            myMsg.setMediaRes(JsonMultiUniMessage.newJsonMultiUniMessage(msg
                    .getBody(),MediaResource.MEDIA_JSON_MUTLI));
        }
        else
        {
        	//by lwx302895
            // 解析媒体资源
//            myMsg.setMediaRes(UmUtil.parseMedia(myMsg.getContent()));
        	ResourcesParser parser = new ResourcesParser(myMsg.getContent());
        	myMsg.setMediaResList(parser.getResources());
        }

        if (msg.getContentType() == MediaResource.MEDIA_FILE
                || myMsg.getMediaType() == MediaResource.MEDIA_FILE)
        {
            myMsg.setType(Message.IM_FILE_TRANSFER);
            msg.setType(Message.IM_FILE_TRANSFER);
        }

        if (!preReceiveMessage(msg, msgType))
        {
            return;
        }

        myMsg.setContent(myMsg.getContent());

        myMsg.setStatus(InstantMessage.STATUS_UNREAD);

        // saveReceiveMessage(myMsg);

        saveMessage(myMsg, RECEIVEMESSAGE);

        onImReceiver(myMsg, false);

        // 收到消息时，自动下载语音。
        if (myMsg.getMediaType() == MediaResource.MEDIA_AUDIO)
        {
        	//by lwx302895
            UmFunc.getIns().downloadFile(myMsg, myMsg.getMediaRes(),false);
        }

        if (myMsg.getMediaType() == MediaResource.MEDIA_PICTURE)
        {
            UmFunc.getIns().downloadFile(myMsg, myMsg.getMediaRes(), true);
        }

    }

    /**
     * 保存收到的消息
     * 
     */
    // private void saveReceiveMessage(InstantMessage message,
    // List<ConversationEntity> conversationEntities)
    // {
    // Logger.debug(TAG, "server msg id=" + message.getMessageId());
    // saveMessageNotNotifyRecent(message, RECEIVEMESSAGE,
    // conversationEntities);
    // onImReceiver(message, false);
    // }
    //
    // private void saveMessageNotNotifyRecent(final InstantMessage msg, int
    // sendType, final List<ConversationEntity> conversationEntities)
    // {
    // add(msg, sendType);
    //
    // final String account = getAccount(msg, sendType);
    // if (StringUtil.isStringEmpty(account))
    // {
    // return;
    // }
    // //在ack里面处理
    // if (sendType == RECEIVEMESSAGE && isRead(msg))
    // {
    // markRead(getMarkType(msg.getMsgType()), account, msg.getMessageId(),
    // getMarkTag(msg.getMsgType()));
    // }
    //
    // RecentChatContact recentChatContact = RecentConversationFunc.getIns()
    // .addRecentContact(account, msg.getMsgType(), msg.getNickname(), false);
    //
    // if (conversationEntities != null)
    // {
    // conversationEntities.add(recentChatContact);
    // }
    // }

    // private void handleInComingMsg(Message msg, int msgType,
    // List<ConversationEntity> conversationEntities,
    // boolean needUpdateNotice, boolean isAlwaysRead)
    // {
    // // TODO if transmit content parse to transmit message
    // InstantMessage myMsg = new InstantMessage(msg, msgType);
    //
    // // json 类型的消息直接构造MediaResource
    // if (msg.getContentType() == MediaResource.MEDIA_JSON_MUTLI)
    // {
    // myMsg.setMediaRes(JsonMultiUniMessage.newJsonMultiUniMessage(msg.getBody()));
    // }
    // else
    // {
    // //解析媒体资源
    // myMsg.setMediaRes(UmUtil.parseMedia(myMsg.getContent()));
    // }
    //
    // if (msg.getContentType() == MediaResource.MEDIA_FILE
    // || myMsg.getMediaType() == MediaResource.MEDIA_FILE)
    // {
    // myMsg.setType(Message.IM_FILE_TRANSFER);
    // msg.setType(Message.IM_FILE_TRANSFER);
    // }
    //
    // if (!preReceiveMessage(msg, msgType))
    // {
    // return;
    // }
    //
    // if (needUpdateNotice)
    // {
    // // playMedia(msg, msgType);
    // }
    //
    // myMsg.setStatus(getMessageStatus(myMsg, RECEIVEMESSAGE));
    //
    // if (!isUninterruptable())
    // {
    // //通过通知栏通知界面
    // notify(myMsg, RECEIVEMESSAGE, needUpdateNotice);
    // }
    //
    // // String account = getAccount(myMsg, RECEIVEMESSAGE);
    // // chatterManager.addChatterToFront(account, msgType);
    //
    // // AlwaysRead来自未读消息，已经有一次计数了，不需要再记一次
    // if (!isAlwaysRead && !isRead(myMsg))
    // {
    // unreadMessageManager.addUnreadNumber(account);
    // }
    //
    // saveReceiveMessage(myMsg, conversationEntities);
    //
    // //收到消息时，自动下载语音。
    // if (myMsg.getMediaType() == MediaResource.MEDIA_AUDIO)
    // {
    // UmFunc.getIns().downloadFile(myMsg, myMsg.getMediaRes());
    // }
    //
    // if (myMsg.getMediaType() == MediaResource.MEDIA_PICTURE)
    // {
    // UmFunc.getIns().downloadFile(myMsg, myMsg.getMediaRes(), true);
    // }
    // }

    private void saveMessage(InstantMessage msg, int sendType)
    {
        InstantMessageDao.insert(msg, (sendType == SENDMESSAGE_TYPE));

        String userName = CommonVariables.getIns().getUserAccount();
        String account = "";// 对方账号
        if (msg.getMsgType() == RecentChatContact.GROUPCHATTER
                || msg.getMsgType() == RecentChatContact.DISCUSSIONCHATTER)
        {
            // 群账号
            account = msg.getToId();
        }
        else if (!TextUtils.isEmpty(userName))
        {
            if (userName.equalsIgnoreCase(msg.getFromId()))
            {
                account = msg.getToId();
            }
            else
            {
                account = msg.getFromId();
            }
        }

        if (TextUtils.isEmpty(account))
        {
            return;
        }

        /**判断条件为了讨论组消息 不以 recentcontact 标签插入数据库 ，
         * 这样就不会再最近消息中显示了
         * by wx303895
         * */
        if(msg.getMsgType() !=  RecentChatContact.DISCUSSIONCHATTER)
        {
            RecentChatContact contact = new RecentChatContact(getAccount(msg,
                    sendType), msg.getMsgType(), msg.getNickname());

            contact.setEndTime(System.currentTimeMillis());
            RecentChatContactDao.replace(contact);
        }

    }

    public String getAccount(InstantMessage msg, int sendType)
    {
        String account = null;
        if (msg == null)
        {
            return account;
        }

        if (msg.getMsgType() == RecentChatContact.GROUPCHATTER
                || msg.getMsgType() == RecentChatContact.DISCUSSIONCHATTER)
        {
            account = msg.getToId(); // 固定群消息是群ID
        }
        else
        {
            if (sendType == SENDMESSAGE_TYPE)
            {
                // 发送别人的消息，toid表示对方id
                account = msg.getToId();
            }
            else
            {
                account = msg.getFromId();
            }
        }
        return account;
    }

    /**
     * 匹配联系人
     */
    private void matchPersonalContact(Message msg)
    {
        String account = msg.getFrom();
        if (Message.IM_GROUPCHAT == msg.getType()
                || Message.IM_GROUPCHATOVERSIZE == msg.getType()
                || Message.IM_GROUPCHATUNDELIVER == msg.getType())
        {
            account = msg.getJid();
        }

        PersonalContact pc = ContactCache.getIns().getContactByAccount(account);

        if (null != pc && TextUtils.isEmpty(pc.getName()))
        {
            pc.setName(msg.getNickname());
        }

        String displayName = ContactTools.getDisplayName(pc, msg.getNickname(),
                null);
        if (!TextUtils.isEmpty(displayName))
        {
            msg.setNickname(displayName);
        }
        if (TextUtils.isEmpty(msg.getNickname()))
        {
            msg.setNickname(account);
        }
        // 未匹配到，则加入缓存
        if (null == pc)
        {
            pc = new PersonalContact();
            pc.setEspaceNumber(account);
            pc.setName(msg.getNickname());
            ContactCache.getIns().addStranger(pc);
        }
    }

    /**
     * 预处理接受消息
     * 
     * @return status 返回 本地数据库已存在数据.或是null
     */
    private boolean preReceiveMessage(Message msg, int type)
    {
        if (TextUtils.isEmpty(msg.getFrom())
                || TextUtils.isEmpty(msg.getTo()))
        {
            return false;
        }
        if (!TextUtils.isEmpty(msg.getMessageId()))
        {
      //  	by lwx302895
//            String oldStatus = InstantMessageDao.isRead(msg.getMessageId(),
//                    type);
        	String oldStatus = InstantMessageDao.getMessageStatus(msg.getMessageId(), type);
            if (InstantMessage.STATUS_UNREAD.equals(oldStatus))
            {
                Logger.debug(CommonUtil.APPTAG,
                        TAG + " | " + msg.getMessageId() + " msg is unread!");
                return false;
            }
            else if (InstantMessage.STATUS_READ.equals(oldStatus)
                    || InstantMessage.STATUS_AUDIO_UNREAD.equals(oldStatus))
            {
                Logger.debug(CommonUtil.APPTAG,
                        TAG + " | " + msg.getMessageId() + " msg is read!");
                if (msg.getType() != Message.IM_FILE_TRANSFER)
                {
                    markRead(getMarkType(type), msg.getFrom(),
                            msg.getMessageId(), RECEIVEMESSAGE);
                }
                return false;
            }
        }
        return true;
    }

    public int getMarkType(int msgType)
    {
        return RecentChatContact.ESPACECHATTER == msgType ? MARK_TYPE_IM
                : MARK_TYPE_GROUP;
    }

    /**
     * 置消息已读
     * 
     * @param markType
     *            1: IM; 2: 固定群; 6: 其它消息（系统公告、部门通知、固定群邀请、固定群邀请回复通知
     *            、固定群主动加入申请、固定群主动加入申请回复通知、UMS传真通知、好友提示）
     * @param account
     *            登录的eSpace账号
     * @param msgId
     *            消息ID
     */
    public void markRead(int markType, String account, String msgId, int msgTag)
    {
        ServiceProxy proxy = UCAPIApp.getApp().getService();
        if (null != proxy && !TextUtils.isEmpty(msgId))
        {
            Logger.debug(CommonUtil.APPTAG, TAG + " | MessageId = " + msgId);
            proxy.markRead(markType, account, msgId, msgTag);
            // proxy.markRead(markType, account, mId);
        }
    }

    public void updateStatus(InstantMessage im, String status)
    {
        if (im == null)
        {
            Log.d(CommonUtil.APPTAG, TAG + " | updateStatus");
            return;
        }

        im.setStatus(status);
        InstantMessageDao.updateMessageIdAndStatus(im);

        // 刷新chatActivity;
        // onImReceiver(im, true);
    }

    /**
     * 获取聊天界面显示的未读条数
     * 
     * @return
     */
    public int getUnReadChatCount()
    {
        return InstantMessageDao.getUnreadCount();
    }

    /**
     * 获取某个聊天的未读消息条数
     * 
     * @param chatId
     *            fromId or toId
     * @param msgType
     *            RecentChatContact.ESPACECHATTER or
     *            RecentChatContact.DISCUSSIONCHATTER
     *
     *            为了读取注销后收到的消息，增加判断语句 by wx303895
     * @return
     */
    public int getUnReadMsgCount(String chatId, int msgType)
    {
        int unReadCount_offline = UnreadMessageManager.getIns().getUnreadNumber(chatId);
        int unReadCount_online = InstantMessageDao.getUnReadMsgCount(chatId, msgType);

        return  unReadCount_offline + unReadCount_online;
    }

    /**
     * 为了RecentChatAdapter也可以加载离线未读消息
     * */
    public int getUnreadMsgOffline(String chatId)
    {
        return UnreadMessageManager.getIns().getUnreadNumber(chatId);

    }
//代码修改 by lwx302895  用下面的getUnReadMsg(String , int)方法
    
//    /**
//     * 获取某个聊天的未读消息
//     * 
//     * @param chatId
//     *            fromId or toId
//     * @param msgType
//     *            RecentChatContact.ESPACECHATTER or
//     *            RecentChatContact.DISCUSSIONCHATTER
//     * @return
//     */
//    public List<InstantMessage> getUnReadMsg(String chatId, int msgType)
//    {
//        List<InstantMessage> unReads = InstantMessageDao.getUnReadMsgs(chatId,
//                msgType);
//        List<InstantMessage> messages = new ArrayList<InstantMessage>();
//        for (InstantMessage msg : unReads)
//        {
//            messages.add(InstantMessageDao.getImById(msg.getId()));
//            msg.setStatus(InstantMessage.STATUS_READ);
//            InstantMessageDao.updateMessageIdAndStatus(msg);
//        }
//
//        if (!unReads.isEmpty())
//        {
//            // 批量置已读时，需要取最后一条非离线文件消息的messageId
//            String lastMid = findLastMid(unReads);
//            if (StringUtil.isNotEmpty(lastMid))
//            {
//                markRead(getMarkType(msgType), chatId, lastMid, RECEIVEMESSAGE);
//            }
//        }
//        return messages;
//    }
    
    public void getUnReadMsg(String chatId, int msgType)
    {
    	 int number = InstantMessageDao.getUnReadMsgCount(chatId, msgType);

         if (number <= 0)
         {
//             Logger.info(TagInfo.FLUENT, "no unread");
             return;
         }

         InstantMessageDao.transUnReadMsgs2Read(chatId, "", msgType);

         String lastMessageId = InstantMessageDao
                 .getMaxMsgId(chatId, msgType, CommonVariables.getIns().getUserAccount());
         markRead(getMarkType(msgType), chatId, lastMessageId, getMarkTag(msgType));

    }

    public List<InstantMessage> getLastHistoryMassages(String account,
            int number, int msgType)
    {
        List<InstantMessage> historyMessages = InstantMessageDao.queryRecord(
                account, number, msgType, 0);

        // List<InstantMessage> temp = new ArrayList<InstantMessage>();
        // if (historyMessages != null && historyMessages.size() > 0)
        // {
        //
        // for (int i = historyMessages.size() - 1; i >= 0; i--)
        // {
        // temp.add(historyMessages.get(i));
        // }
        // historyMessages.clear();
        // historyMessages.addAll(temp);
        // }

        return historyMessages;
    }

    /**
     * 找到最后一条非离线文件消息的messageId
     * 
     * @param unReads
     *            未读消息列表
     * @return 最后一条非离线文件消息ID
     */
    private String findLastMid(List<InstantMessage> unReads)
    {
        int size = unReads.size();
        InstantMessage msg;
        for (int i = size - 1; i >= 0; i--)
        {
            msg = unReads.get(i);
            if (msg.getType() != Message.IM_FILE_TRANSFER)
            {
                return msg.getMessageId();
            }
        }
        return null;
    }

    private void onImReceiver(InstantMessage msg, boolean update)
    {
        synchronized (imbroadcastLock)
        {
            for (LocalImReceiver receiver : imReceivers)
            {
                receiver.onReceive(msg, update);
            }
        }
    }

    // 回调界面里面注册的漫游消息接收处理方法
    private void onRequestHistoryBack(List<InstantMessage> msgList,
            GetHistoryMessageInfo info)
    {
        synchronized (imbroadcastLock)
        {
            for (LocalImReceiver receiver : imReceivers)
            {
                receiver.onRequestHistoryBack(msgList, info);
            }
        }
    }

    /**
     * 消息接收接口
     */
    public interface LocalImReceiver
    {
        /**
         * 消息接收
         * 
         * @param msg
         *            消息内容
         * @param update
         *            更新消息
         */
        void onReceive(InstantMessage msg, boolean update);

        void refreshDisplayAfterSendMessage(InstantMessage msg);

        void onRequestHistoryBack(List<InstantMessage> msgList,
                GetHistoryMessageInfo info);
    }

    /**
     * 获取漫游消息
     * 
     * @param info
     *            获取漫游消息的结构体
     * */
    public FuncResultCode requestHistoryMessage(GetHistoryMessageInfo info)
    {
        if (info == null)
        {
            return FuncResultCode.RESULT_NULL_PARAM;
        }

        boolean isMerger = ContactLogic.getIns().getAbility()
                .isHistoryMsgMerger();
        boolean getFromLocal = true;
        Logger.info(TAG, "GetHistoryMessageInfo isMerger:" + isMerger);
        if (isMerger)
        {
            ExecuteResult result = null;

            ServiceProxy proxy = UCAPIApp.getApp().getService();

            boolean isNeedRequest;

            if (info.isFirst())
            {
                isNeedRequest = historyMsgMergerManager.isNeedFirstRequest(info
                        .getAccount());
            }
            else
            {
                isNeedRequest = historyMsgMergerManager.isNeedRequestNext(
                        info.getAccount(), info.getMessageId());
            }

            Logger.info(TAG, "GetHistoryMessageInfo isFirst:" + info.isFirst()
                    + ", isNeedRequest:" + isNeedRequest);

            if (isNeedRequest && proxy != null)
            {
                String lastRecordId = historyMsgMergerManager
                        .getLastRecordId(info.getAccount());
                int requestType;

                switch (info.getType())
                {
                case RecentChatContact.GROUPCHATTER:
                case RecentChatContact.DISCUSSIONCHATTER:
                    requestType = GetRoamingMessageData.GROUP_CHAT_MESSAGE;
                    break;
                default:
                    requestType = GetRoamingMessageData.P2P_CHAT_MESSAGE;
                    break;
                }

                info.setMessageId(lastRecordId);

                Logger.info(TAG, "getRoamingMessage requestType: "
                        + requestType + ", lastRecordId:" + lastRecordId
                        + ", Account:" + info.getAccount());

                result = proxy.getRoamingMessage(requestType, lastRecordId,
                        info.getAccount(), MAX_MSGS);
            }

            if (result != null && result.isResult())
            {
                final int requestId = result.getId();
                getHistoryMessageInfoMap.put(requestId, info);

                // 非登录后首次进入聊天界面,并且需要从服务器请求的时候,不需要从本地数据库直接读取
                if (!info.isFirst())
                {
                    getFromLocal = false;
                }
            }
        }

        if (getFromLocal)
        {
            requestHistoryFromLocal(info);
        }

        return FuncResultCode.RESULT_SUCCESS;
    }

    /**
     * 处理接收到漫游消息列表
     * 
     * */
    private void handleHistoryMessageList(GetRoamingMessageData data,
            GetHistoryMessageInfo info)
    {
        List<String> messageIdList = new ArrayList<String>();

        info.setHaveHistory(data.getMessageList().size() == MAX_MSGS);

        for (Message msg : data.getMessageList())
        {
            if (msg != null)
            {
                Logger.info(
                        TAG,
                        "msg id: "
                                + msg.getMessageId()
                                + ", time:"
                                + DateUtil.format(msg.getDateTime(),
                                        DateUtil.FMT_YMDHMS));
                messageIdList.add(msg.getMessageId());
            }
        }

        // 如果首次请求，请求id为空，则不替换startMessageId，兼容服务器入库延迟问题。
        if (!TextUtils.isEmpty(info.getMessageId()))
        {
            data.setStartMsgId(info.getMessageId());
        }

        if (messageIdList.isEmpty())
        {
            data.setEndMsgId("");
        }

        // 删除返回范围内除了有效值外的所有记录（针对某个帐号）
        InstantMessageDao.deleteMessageInScope(info.getAccount(),
                info.getType(), data.getStartMsgId(), data.getEndMsgId(),
                messageIdList);

        // 数据库中查询哪些需要插入 needInsertMessages
        List<String> localExistMessages = InstantMessageDao.getExistMessages(
                messageIdList, false);
        List<String> localExistOldMessages = InstantMessageDao
                .getExistMessages(messageIdList, true);

        if (localExistMessages == null)
        {
            localExistMessages = new ArrayList<String>();
        }
        else
        {
            for (String msgId : localExistMessages)
            {

                Logger.info(TAG, "localExistMessages msg id: " + msgId);

            }
        }

        if (localExistOldMessages == null)
        {
            localExistOldMessages = new ArrayList<String>();
        }

        List<InstantMessage> messageList = new ArrayList<InstantMessage>();

        InstantMessage im;
        String selfAccount = ContactLogic.getIns().getMyContact()
                .getEspaceNumber();

        if (TextUtils.isEmpty(selfAccount))
        {
            selfAccount = CommonVariables.getIns().getUserAccount();
        }

        if (selfAccount == null)
        {
            selfAccount = "";
        }

        for (Message message : data.getMessageList())
        {
            if (message != null
                    && !localExistMessages.contains(message.getMessageId())
                    && !localExistOldMessages.contains(message.getMessageId()))
            {
                im = InstantMessageFactory.createInstantMessage(message, info.getType());/*new InstantMessage(message, info.getType());*/

                // json 类型的消息直接构造MediaResource
                if (message.getContentType() == MediaResource.MEDIA_JSON_MUTLI)
                {
                    im.setMediaRes(JsonMultiUniMessage
                            .newJsonMultiUniMessage(message.getBody(),MediaResource.MEDIA_JSON_MUTLI));
                }
                else
                {
                    // 解析媒体资源
//                    im.setMediaRes(UmUtil.parseMedia(im.getContent()));
                    ResourcesParser parser = new ResourcesParser(im.getContent());
                    im.setMediaResList(parser.getResources());
                }

                if (message.getContentType() == MediaResource.MEDIA_FILE
                        || im.getMediaType() == MediaResource.MEDIA_FILE)
                {
                    im.setType(Message.IM_FILE_TRANSFER);
                }

                // 对方帐号等于ToId就说明是发送消息，相反则是接收消息
                if (selfAccount.equals(im.getFromId()))
                {
                    // 同步下来的消息肯定是发送成功的
                    im.setStatus(InstantMessage.STATUS_SEND_SUCCESS);
                }
                else
                {
                    im.setStatus(InstantMessage.STATUS_READ);
                }

                // 收到消息时，自动下载语音。
                if (im.getMediaType() == MediaResource.MEDIA_AUDIO)
                {
                    UmFunc.getIns().downloadFile(im, im.getMediaRes(), false);
                }

                if (im.getMediaType() == MediaResource.MEDIA_PICTURE)
                {
                    UmFunc.getIns().downloadFile(im, im.getMediaRes(), true);
                }

                messageList.add(im);
            }
        }

        // 插入返回的有效值（已有不替换）messageList
        InstantMessageDao.insertMessageList(messageList, false);
        historyMsgMergerManager.addRecordList(messageIdList, info.getAccount());
    }

    /**
     * 获取所有的富媒体消息。
     * 
     * @param account
     * @param msgType
     * @param mediaType
     * @return
     */
    public List<InstantMessage> getAllMediaMessage(String account, int msgType,
            int mediaType)
    {
        boolean isTimeSort = ContactLogic.getIns().getAbility()
                .isHistoryMsgMerger();

        return InstantMessageDao.queryRecordByMediaType(account, msgType,
                mediaType, isTimeSort);
    }

    /**
     * 处理选择到的媒体资源
     * 
     * @param data
     *            选择媒体（图片，视频）后返回的data；包含文件路径
     * @param chatType
     *            聊天类型，讨论组，固定群，点对点
     */
    public void handleChooseMedia(Intent data, int chatType)
    {
        if (data == null)
        {
            Logger.debug(TAG, "data is null");
            return;
        }

        boolean isVideo = data.getBooleanExtra(IntentData.IS_VIDEO, false);
        if (isVideo)
        {
            sendVideo(data, chatType);
            return;
        }

        sendPicture(data, chatType);
    }

    /**
     * 发送视频
     * 
     * @param data
     */
    private void sendVideo(Intent data, int chatType)
    {
        String newPath = data.getStringExtra(IntentData.VIDEO_PATH);
        int time = data.getIntExtra(IntentData.VIDEO_TIME, 0);

        prepareVideoToSend(newPath, time, chatType);
    }

    /**
     * 发送图片
     * 
     * @param data
     */
    private void sendPicture(Intent data, final int chatType)
    {
        final List<MediaRetriever.Item> paths = (ArrayList<MediaRetriever.Item>) data
                .getSerializableExtra(IntentData.SELECT_PATHS);
        if (paths == null || paths.size() <= 0)
        {
            Logger.debug(TAG, "Don't choose picture.");
            return;
        }

        final boolean choose = data.getBooleanExtra(IntentData.CHOOSE, false);
        ThreadManager.getInstance().addToFixedThreadPool(new Runnable()
        {
            @Override
            public void run()
            {
                for (MediaRetriever.Item item : paths)
                {
                    preparePicMsgToSend(item.getFilePath(), choose, item,
                            chatType);
                }
            }
        });
    }

    /**
     * 准备发送图片消息
     * 
     * @param newPath
     * @param choose
     * @param item
     */
    public void preparePicMsgToSend(String newPath, boolean choose,
            MediaRetriever.Item item, int chatType)
    {
        // 选择图片时走另外的流程
        String mPath;
//        if (choose)
//        {
//            // 如果是图片，先进行压缩,再旋转。
//            mPath = processPicture(newPath);
//        }
//        else
//        {
            String dir = UmUtil.createDir(MediaResource.MEDIA_PICTURE);
            mPath = FileUtil.renameWithSha1(newPath, dir);
//        }

        if (mPath == null)
        {
            Logger.error(TAG, "mPath is null, espace may logout。");
            return;
        }
        Uri uri = Uri.parse(mPath);
        processMediaMsgSend(uri, MediaResource.MEDIA_PICTURE, 0, null, chatType);
    }

    /**
     * 发送图片前，对图片进行处理
     * 
     * @param path
     * @return
     */
    public String processPicture(String path)
    {
        if (TextUtils.isEmpty(path))
        {
            return null;
        }

        String sha1 = FileSHA1.getFileSha1(path);
        String fileName = com.huawei.utils.io.FileUtil.getFileNameWithSha1(sha1,
                path);
        String thePath = UmUtil.createResPath(MediaResource.MEDIA_PICTURE,
                fileName, UmConstant.JPG);

        if (TextUtils.isEmpty(sha1) || TextUtils.isEmpty(thePath))
        {
            return null;
        }

        // 压缩图片,压缩不成功，可能图片很小不需要压缩。直接返回path。
        if (PhotoUtil.zoomPicture(path, thePath,UmUtil.isSavePng(path), UmConstant.MAXPICSIZE)
                && !(thePath.toLowerCase(Locale.ENGLISH).endsWith(UmConstant.JPG)))
        {
            // 对于不是jpg结尾的图片，需要重新进行处理
            String jpgFileName = sha1 + UmConstant.DOT + UmConstant.JPG;
            File oldFile = new File(thePath);
            File newFile = new File(oldFile.getParent(), jpgFileName);
            if (oldFile.renameTo(newFile))
            {
                return newFile.getAbsolutePath();
            }
        }

        // rename fail
        return thePath;
    }

    /**
     * 准备语音发送。
     * 
     * @param filePath
     * @param seconds
     */
    public void prepareAudioToSend(String filePath, int seconds, int chatType)
    {
        if (filePath == null)
        {
            Logger.warn(TAG, "filePath null.");
            return;
        }

        // 如果文件不存在，则返回。处理一些异常情况，比如sd卡已满，录音的底层库异常。
        File file = new File(filePath);
        if (!file.exists())
        {
            Logger.warn(TAG, "file not exist.");
            return;
        }

        // 将文件移动到新的目录下面，并用sha1值重命名
        String dirPath = UmUtil.createDir(MediaResource.MEDIA_AUDIO);
        String newFilePath = FileUtil.renameWithSha1(filePath, dirPath);

        // 发送消息
        processMediaMsgSend(Uri.parse(newFilePath), MediaResource.MEDIA_AUDIO,
                seconds, null, chatType);
    }

    /**
     * 准备发送富媒体消息
     * 
     * @param newPath
     *            文件路径
     * @param time
     *            视频录制的时长
     */
    public void prepareVideoToSend(String newPath, int time, int chatType)
    {
        File sendFile = new File(newPath);
        long length = sendFile.length();
        if (length > ContactLogic.getIns().getMyOtherInfo().getUmVideoSize())
        {
            showVideoExceedDialog();
            return;
        }

        sendVideo(newPath, time, chatType);
    }

    /**
     * 发送视频或图片
     * 
     * @param newPath
     * @param time
     */
    private void sendVideo(String newPath, int time, int chatType)
    {
        // 首先创建文件夹。
        UmUtil.createDir(MediaResource.MEDIA_VIDEO);

        String sha1 = FileSHA1.getFileSha1(newPath);
        String fileName = com.huawei.utils.io.FileUtil.getFileNameWithSha1(sha1,
                newPath);
        String mPath = newPath;

        if (mPath == null)
        {
            Logger.error(TAG, "mPath is null, espace may logout。");
            return;
        }

        Uri uri = Uri.parse(mPath);
        InstantMessage msg = processMediaMsgSend(uri,
                MediaResource.MEDIA_VIDEO, time / 1000, fileName, chatType);

        // r如果是选择视频，则需要做视频移动处理。
        moveVideo(msg, mPath);
    }

    /**
     * 显示视频超过限制的对话框。
     */
    private void showVideoExceedDialog()
    {
        ToastUtil.showToast(LocContext.getContext(), R.string.video_max_tip);
    }

    /**
     * 处理多媒体消息发送
     *
     * @param uri
     *            多媒体URI
     * @param mediaType
     *            媒体类型
     * @param time
     *            时间
     */
    public InstantMessage processMediaMsgSend(Uri uri, int mediaType, int time,
            String fileName, int chatType)
    {
        return sendMediaMsg(uri, mediaType, time, fileName, chatType);
    }

    /**
     * 转发已有的MediaResource 此种不需要再上传resource
     * 
     * @param mediaResource
     * @return
     */
    public void processMediaMsgSend(MediaResource mediaResource, int chatType)
    {
        sendIMMsg(mediaResource, chatType);
    }

    /**
     * 发送媒体消息
     * 
     * @param uri
     * @param mediaType
     * @param time
     * @param fileName
     * @param isNeedToast
     *            发送结果需要弹出toast
     * @return
     */
    public InstantMessage sendMediaMsg(Uri uri, int mediaType, int time,
            String fileName, boolean isNeedToast, int chatType)
    {
        Logger.setLogger(new AndroidLogger());
        String extraParam = "";
        if (mediaType == MediaResource.MEDIA_PICTURE)
        {
        	//代码修改 by lwx302895
//            extraParam = UmUtil.genExtraParam(uri);
            extraParam = UmUtil.genExtraParam(uri.getPath());
        }

        String path = uri.getPath();
        if (path == null || !(new File(path).exists()))
        {
            Logger.warn(TAG, "file not exit, path = " + path);
            return null;
        }

        //代码修改 bylwx302895
        String name = TextUtils.isEmpty(fileName) ? uri
                .getLastPathSegment() : fileName;

        ResourceGenerator generator = new ResourceGenerator(uri.getPath(), mediaType);
        MediaResource mediaRes = generator.parseMediaResource(CommonVariables.getIns().getUserAccount(), -1);

        if (null == mediaRes)
        {
            Logger.error(TAG, "mediaRes is null");
            return null;
        }

        if (time > 0)
        {
            mediaRes.setDuration(time);
        }

//        MediaResource mediaRes = new MediaResource(path,
//                     MediaResource.RES_LOCAL, mediaType, FileUtil.getFileSize(path),
//                      time, name, extraParam) {
//
//					@Override
//					public Bitmap getThumbnail() {
//						// TODO Auto-generated method stub
//						return null;
//					}
//				};
//        MediaResource mediaRes = MediaResource.generateMediaResource(path,
//                MediaResource.RES_LOCAL, mediaType, FileUtil.getFileSize(path),
//                time, name, extraParam);

//代码修改 bylwx302895
//        String msg = UmUtil.createResLocal(mediaRes, path);
        String msg = mediaRes.toLocalString(path);
        mediaRes.toLocalString(path);
        InstantMessage im = genMsg(msg, mediaRes, InstantMessage.STATUS_SEND,
                false, chatType);
        im.setNeedToastResult(isNeedToast);

        ImFunc.getIns().saveSendMessage(im);
        UmFunc.getIns().uploadFile(im, mediaRes);
        ImFunc.getIns().refreshDisplayAfterSendMessage(im);

        return im;
    }

    public InstantMessage sendMediaMsg(Uri uri, int mediaType, int time,
            String fileName, int chatType)
    {
        return sendMediaMsg(uri, mediaType, time, fileName, false, chatType);
    }

    /**
     * 生成消息实体
     * 
     * @param msg
     *            发送的消息字符串.
     * @param mediaRes
     *            媒体资源
     * @param status
     *            消息的当前状态.
     * @param isCommonMessage
     *            是否普通消息. ture为普通消息.
     * @return 构造的InstantMessage实体.
     */
    public InstantMessage genMsg(String msg, MediaResource mediaRes,
            String status, boolean isCommonMessage, int chatType)
    {
        InstantMessage im = new InstantMessage();

        // 需要在发送时做敏感词过滤。（规格）
        if (isCommonMessage && ChatUtil.containSensitiveWords(msg))
        {
            msg = ChatUtil.sensitiveFilter(msg);
            im.setContainSensitive(true);
        }

        im.setContent(msg);
        im.setFromId(CommonVariables.getIns().getUserAccount());
        im.setTimestamp(new Timestamp(System.currentTimeMillis()));
        im.setStatus(status);
        im.setMediaRes(mediaRes);
        im.setMsgType(chatType);

        im.setToId(ChatActivity.getCurChatter().getChatAccount());
        
        
        im.setType(chatType == RecentChatContact.ESPACECHATTER ? com.huawei.data.Message.IM_CHAT:com.huawei.data.Message.IM_GROUPCHAT);
  
        PersonalContact curContact = ContactCache.getIns().getContactByAccount(
                CommonVariables.getIns().getUserAccount());
        if (curContact != null)
        {
            im.setNickname(ContactFunc.getIns().getDisplayName(curContact));
        }

        return im;
    }

    /**
     * 直接发送已经存在的MediaResource
     * 
     * @param resource
     *            需要发送的MediaResource
     */
    public void sendIMMsg(MediaResource resource, int chatType)
    {
        sendIMMsg(resource, false, chatType);
    }

    public void sendIMMsg(MediaResource resource, boolean isNeedToast,
            int chatType)
    {
        // 如果转发的是没有下载到本地的资源的话，直接转发原本的url
    	//	by lwx302895
//        String msg = UmUtil.createResUrl(resource, resource.getRemotePath());
    	String remoteUrl = resource.getRemoteUrl();
    	String msg = resource.toString(remoteUrl);

        InstantMessage im = genMsg(msg, resource, InstantMessage.STATUS_SEND,
                false, chatType);
        im.setNeedToastResult(isNeedToast);

        // ImFunc.getIns().refreshDisplayAfterSendMessage(im);
        ImFunc.getIns().sendMessage(im, false, resource.getMediaType());
    }

    /**
     * 发送即时消息
     * 
     * @param im
     *            消息内容
     * @param resend
     *            是否是重新发送
     * @return 发送结果
     */
    public ExecuteResult sendMessage(InstantMessage im, boolean resend,
            int mediaType)
    {
        ServiceProxy proxy = UCAPIApp.getApp().getService();

        if (!resend)
        {
            // 先存.再发送消息.
            im.setMsgType(RecentChatContact.ESPACECHATTER);
            saveSendMessage(im);
        }
        else
        {
            updateStatus(im, InstantMessage.STATUS_SEND);
        }

        ExecuteResult eResult = proxy.sendMessage(im.getContent(),
                im.getToId(), null, mediaType, im.getMessageId());

        synchronized (imLock)
        {
            imRequestIDMap.put(eResult.getId(), im);
        }
        return eResult;
    }

    /**
     * 保存发送消息
     * 
     * @param msg
     *            消息
     */
    public void saveSendMessage(InstantMessage msg)
    {
        // 发送message的时候需要添加消息ID，为服务器重复消息过滤

        if (msg != null)
        {
        	//代码修改 by lwx302895
//            msg.setMessageId(StringUtil.generateSendMessageId());
        	msg.setMessageId(Tools.generateSendMessageId(CommonVariables.getIns().getUserAccount()));
            
            saveMessageAndNotifyRecent(msg, SENDMESSAGE_TYPE);
        }
    }

    /**
     *
     * 添加聊天记录
     * 
     * @param msg
     * @param sendType
     */
    public void add(InstantMessage msg, int sendType)
    {
        // 和iphone一致，离线文件消息存未读
        if (msg.getType() == Message.IM_FILE_TRANSFER)
        {
            msg.setStatus(InstantMessage.STATUS_UNREAD);
            String from = msg.getFromId();
            RecentChatter currentChatter = ImFunc.getIns().getLastChatter();
            if (!TextUtils.isEmpty(from)
                    && currentChatter != null
                    && from.equals(currentChatter.getChatAccount())
                    && msg.getMsgType() == currentChatter.getChatterType()
                    && UCAPIApp.getApp().getCurActivity() instanceof ChatActivity)
            {
                msg.setStatus(InstantMessage.STATUS_READ);
            }
        }

        //Logger.info(CommonUtil.APPTAG, TAG + "insert message:" + msg.toString());
        InstantMessageDao.insert(msg, sendType == ImFunc.SENDMESSAGE_TYPE);

        /**为了能够 发送消息也在对话中看到 by 303895*/
        if(msg.getMsgType() !=  RecentChatContact.DISCUSSIONCHATTER)
        {
            RecentChatContact contact = new RecentChatContact(getAccount(msg,
                    sendType), msg.getMsgType(), msg.getNickname());
            contact.setEndTime(System.currentTimeMillis());

            RecentChatContactDao.replace(contact);
        }
        /** end */
    }

    /**
     * 获得正在聊天人或群
     * 
     * @return 如果返回值为null，说明不存在正在聊天的人
     */
    public RecentChatter getLastChatter()
    {
        return ChatActivity.getCurChatter();
    }

    /**
     * 保存消息
     */
    private void saveMessageAndNotifyRecent(InstantMessage msg, int sendType)
    {
        add(msg, sendType);

        String account = getAccount(msg, sendType);

        if (TextUtils.isEmpty(account))
        {
            return;
        }

        // 在ack里面处理
        if (sendType == RECEIVEMESSAGE && isRead(msg))
        {
            markRead(getMarkType(msg.getMsgType()), account,
                    msg.getMessageId(), getMarkTag(msg.getMsgType()));
        }


        // ThreadManager.getInstance().addToFixedThreadPool(new
        // TempRunnable(account, msg.getMsgType(), msg.getNickname(), true));

    }

    private boolean isRead(InstantMessage message)
    {
        return message != null
                && (InstantMessage.STATUS_READ.equals(message.getStatus()) || InstantMessage.STATUS_AUDIO_UNREAD
                        .equals(message.getStatus()));
    }

    public int getMarkTag(int msgType)
    {
        return RecentChatContact.ESPACECHATTER == msgType ? MARK_MESSAGE_TAG_IM
                : MARK_MESSAGE_TAG_GROUP;
    }

    // private static class TempRunnable implements Runnable
    // {
    // private String account;
    // private int type;
    // private String nickName;
    // private boolean notifyViewUpdate;
    // public TempRunnable(String account, int type, String nickName, boolean
    // notifyViewUpdate)
    // {
    // this.account = account;
    // this.type = type;
    // this.nickName = nickName;
    // this.notifyViewUpdate = notifyViewUpdate;
    // }
    //
    // @Override
    // public void run()
    // {
    // RecentConversationFunc.getIns()
    // .addRecentContact(account, type, nickName, notifyViewUpdate);
    // }
    // }

    private int getType()
    {
        return RecentChatContact.ESPACECHATTER;
    }

    /**
     * 将文件移动到um的专用文件夹，并跳到预览界面显示
     *
     * @param msg
     * @param oldPath
     * @return
     */
    public void moveVideo(final InstantMessage msg, final String oldPath)
    {
        // 放到多线程里面执行更新操作。
        ThreadManager.getInstance().addToFixedThreadPool(new Runnable()
        {
            @Override
            public void run()
            {
                if (msg == null || msg.getId() <= 0
                        || msg.getMediaRes() == null)
                {
                    Logger.debug(TAG, "msg null, send fail.");
                    return;
                }

                File file = new File(oldPath);
                if (!file.exists()) // 文件不存在， 返回
                {
                    Logger.error(TAG, "File you choose do not exist.");
                    return;
                }

                String sha1 = FileSHA1.getFileSha1(oldPath);
                if (sha1 == null) // sha1不存在， 返回
                {
                    Logger.error(TAG, "Get file sha1 is null.");
                    return;
                }

                String suffix = UmConstant.MP4; // ChatUtil.getMeidaSuffix(mediaType
                                            // == MediaResource.MEDIA_VIDEO);
                String fileName = sha1 + UmConstant.DOT + suffix;
                final String path = UmUtil.createResPath(
                        MediaResource.MEDIA_VIDEO, fileName, suffix);
                if (path == null) // 获取不到目录
                {
                    Logger.error(TAG, "can not get path.");
                    return;
                }

                file = new File(path);
                if (file.exists()) // 如果该文件已经存在，直接返回。
                {
                    Logger.debug(TAG, "sended file is exist.");
                    updateMediaRes(msg, path);
                    return;
                }

                // 移动文件到新创建的目录
                boolean move = com.huawei.utils.io.FileUtil
                        .copyFile(oldPath, path);

                // 移动文件成功，跳到视频播放页面
                if (!move)
                {
                    Logger.error(TAG, "move file fail, please check!");
                    return;
                }

                updateMediaRes(msg, path);
            }
        });
    }

    /**
     * 更新媒体资源
     * 
     * @param msg
     * @param path
     */
    private void updateMediaRes(InstantMessage msg, String path)
    {
        // 目录
    	//代码修改
//        String content = UmUtil.createResLocal(msg.getMediaRes(), path);
        String content = msg.getMediaRes().toLocalString(path);
        InstantMessageDao.updateContent(content, msg.getId());

        msg.setContent(content);
        msg.getMediaRes().initResource(MediaResource.RES_LOCAL, path);
    }

    /**
     * 将发送的消息插入界面的消息列表中
     * 
     * */
    public void refreshDisplayAfterSendMessage(final InstantMessage msg)
    {
        synchronized (imbroadcastLock)
        {
            for (LocalImReceiver receiver : imReceivers)
            {
                receiver.refreshDisplayAfterSendMessage(msg);
            }
        }
    }

    /**
     *
     * 如果当前页面可以显示消息(当前聊天对象与消息发送方相同), 则调用此方法, 每增加一条数据后及时刷新页面
     * 
     * @param im
     *            消息
     */
    public void appendIMMessage(InstantMessage im)
    {
        // android.os.Message msg = new android.os.Message();
        // msg.what = ChatActivity.MSG_SEND;
        // msg.obj = im;
        // handler.sendMessage(msg);
    }

    /**
     * 处理富媒体消息的上传进度
     * */
    public void handleUmMsgUploadProcessUpdate(UmReceiveData data)
    {
        if (data == null)
        {
            return;
        }

        android.os.Message msg = new android.os.Message();
        msg.arg1 = (int) data.msg.getId();
        msg.arg2 = data.media.getMediaId();
        msg.what = ChatActivity.UPLOAD_PROCESSING;
        handler.sendMessage(msg);

    }

    /**
     * 处理富媒体消息上传结束
     * 
     * */
    public void handleUmMsgUploadFileFinish(UmReceiveData data)
    {
        android.os.Message msg = new android.os.Message();
        msg.arg1 = 0;
        msg.what = ChatActivity.TRANS_FINISH;
        msg.obj = data;
        handler.sendMessage(msg);
    }

    /**
     * 处理富媒体消息的下载进度
     * 
     * @param data
     *            下载进度消息返回的数据
     * 
     * */
    public void handleUmMsgDownloadProcessUpdate(UmReceiveData data)
    {
        if (data == null)
        {
            return;
        }

        android.os.Message msg = android.os.Message.obtain(handler,
                ChatActivity.DOWNLOAD_PROCESSING);
        msg.arg1 = (int) data.msg.getId();
        msg.arg2 = data.media.getMediaId();

        if (data.process != null)
        {
            msg.obj = data.process.getProgress();
        }
        handler.sendMessage(msg);
    }

    /**
     * 处理富媒体消息下载结束
     * 
     * */
    public void handleUmMsgDownloadFileFinish()
    {
        android.os.Message msg = new android.os.Message();
        msg.arg1 = 1;
        msg.what = ChatActivity.TRANS_FINISH;
        handler.sendMessage(msg);
    }

    // /**
    // * listView条目类
    // */
    // public static final class ListItem
    // {
    // public enum ItemType
    // {
    // PromptNormal, PromptDate, PromptGroup, MsgSendText, MsgSendAudio,
    // MsgSendVideo, MsgSendFile, MsgSendImage, MsgRecvText, MsgRecvAudio,
    // MsgRecvVideo, MsgRecvFile, MsgRecvImage, MsgSendTrans, MsgRecvTrans,
    // MsgRecvMulti
    // }
    //
    // /** 当前条目所包含消息内容集合 */
    // public InstantMessage insMsg = null;
    //
    // public boolean isFirst = false;
    //
    // /**
    // * 判断是否被选中。
    // * 选中时可进行相应的多选删除处理。
    // */
    // // public boolean isChecked = false;
    //
    // /**
    // * 条目类型，包括正常消息类型，超大消息类型，敏感消息提示类型等
    // * 类型与InstantMessage里定义的类型并非一一对应
    // */
    // public ItemType itemType = null;
    //
    // /** 提示信息(可选) */
    // public String prompt = null;
    //
    // /**
    // * 消息被解析后的内容，含SpannableString等。
    // * 内容放在此处有两个原因：
    // * 1 InstantMessage里面不能添加一个UIContent项，因为SpannableString不是
    // * Serialization类型.这样不能通过Intent传输。
    // * 2 SpannableString在Adapter里面解析耗时太大，严重影响滑动的体验。
    // */
    // public CharSequence content = null;
    //
    // /** 仅对ItemType为PromptDate时可用 */
    // public Timestamp timeStamp = null;
    //
    // private ListItem()
    // {
    // }
    //
    // @Override
    // public String toString()
    // {
    // return "type = " + itemType + ", prompt = " + prompt;
    // }
    // }

    /**
     * 发送群组消息
     * 
     * @param groupId
     *            群组ID
     * @param groupType
     *            Group Type
     * @param msg
     *            消息
     */
    public void sendGroupMsg(String groupId, int groupType, String msg)
    {

        InstantMessage im = ImFunc
                .getIns()
                .genMsg(msg,
                        null,
                        InstantMessage.STATUS_SEND,
                        true,
                        groupType == ConstGroup.DISCUSSION ? RecentChatContact.DISCUSSIONCHATTER
                                : RecentChatContact.GROUPCHATTER);

        im.setType(Message.IM_GROUPCHAT);
        im.setToId(groupId);

        sendGroupMsg(groupId, groupType, im, false, MediaResource.TYPE_NORMAL);
    }

    /**
     * 发送群组消息
     * 
     * @param groupId
     *            群组ID
     * @param groupType
     *            Group Type
     * @param im
     *            消息
     */
    public void sendGroupMsg(String groupId, int groupType, InstantMessage im)
    {
        sendGroupMsg(groupId, groupType, im, false, MediaResource.TYPE_NORMAL);
    }

    /**
     * 发送群组消息
     * 
     * @param groupId
     *            群组ID
     * @param im
     *            消息
     * @param resend
     *            是否是重新发送
     */
    public void sendGroupMsg(String groupId, int groupType, InstantMessage im,
            boolean resend, int mediaType)
    {
        if (!resend)
        {
            saveSendMessage(im);
        }
        else
        {
            updateStatus(im, InstantMessage.STATUS_SEND);
        }
        String from = im.getFromId();
        String type = "groupchat";
        ConstGroup group = ConstGroupManager.ins().findConstGroupById(groupId);
        if (null == group)
        {
            updateStatus(im, InstantMessage.STATUS_SEND_FAILED);
            return;
        }

        String owner = group.getOwner();
        ExecuteResult result = ConstGroupManager.ins().sendGroupMessage(
                groupId, from, type, owner, im.getContent(), groupType,
                mediaType, im.getMessageId());

        synchronized (imLock)
        {
            if (result != null)
            {
                Logger.info(CommonUtil.APPTAG,
                        TAG + " | sendGroupMsg------result id = "
                                + result.getId());
                imRequestIDMap.put(result.getId(), im);
            }
        }
    }

    /**
     * 清楚所有聊天资料
     */
    public void clear()
    {
        // TODO 考虑ImFunc的生命周期
        // TODO 此处只清数据，不清逻辑
        // endAllChat();
        // imNotify.clear();

        synchronized (imLock)
        {
            imRequestIDMap.clear();
        }

        // synchronized (kickGroupIds)
        // {
        // kickGroupIds.clear();
        // }
        //
        // synchronized (removeGroupIds)
        // {
        // removeGroupIds.clear();
        // }
        //
        // if (memberAddBySelfSave != null)
        // {
        // memberAddBySelfSave.clear();
        // memberAddBySelfSave = null;
        // }
        //
        // if (memberDelBySelfSave != null)
        // {
        // memberDelBySelfSave.clear();
        // memberDelBySelfSave = null;
        // }

        historyMsgMergerManager.cleanUp();
        // unreadMessageManager.cleanUp();
        //
        // Logger.debug(LocalLog.APPTAG, "clear data");
    }
}
