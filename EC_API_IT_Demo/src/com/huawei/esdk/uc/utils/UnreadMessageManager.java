package com.huawei.esdk.uc.utils;

import android.os.Handler;

import com.huawei.data.Message;
import com.huawei.esdk.uc.function.ImFunc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lWX303895 on 2016/4/11.
 *
 * 为了存储离线未读消息
 */
public class UnreadMessageManager
{

    public Map<String, Message> unReadMsg = new ConcurrentHashMap<String, Message>();

    private Handler unReadHandler;

    public void setUnReadHandler(Handler handler)
    {
        unReadHandler = handler;
    }



    private Map<String, Integer> unreadNumberMap = new HashMap<String, Integer>();

    private static UnreadMessageManager manager;

    public static  UnreadMessageManager getIns()
    {
        if(manager == null)
        {
            manager = new UnreadMessageManager();
        }

        return  manager;
    }

    private UnreadMessageManager ()
    {

    }

    public synchronized boolean saveUnreadNumber(String account, int number)
    {
        unreadNumberMap.put(account, Integer.valueOf(number));

        return true;
    }

    public synchronized int clearUnreadNumber(String account)
    {
        Integer number = unreadNumberMap.remove(account);

        if (number == null)
        {
            return 0;
        }

        return number;
    }

    /**这个方法被更改过 */
    public synchronized int getUnreadNumber(String account)
    {
        Integer number = unreadNumberMap.get(account);

        if (number == null)
        {
            return 0;
        }

        return number/*.intValue()*/;
    }

    /**
     * 存放未读消息
     * @param msgId
     * @param msg
     */
    public synchronized void saveUnreadMsg(String msgId, Message msg)
    {
        if (unReadMsg != null && !unReadMsg.containsKey(msgId))
        {
            unReadMsg.put(msgId, msg);
        }
    }

    /**
     * 通过联系人account删除消息
     * @param account
     */
    public synchronized void delMsgByAccount(String account)
    {
        Iterator<Map.Entry<String, Message>> it = unReadMsg.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Message> mapEntry = it.next();
            if (ImFunc.getIns().isImMessage(mapEntry.getValue().getType()) && mapEntry.getValue().getFrom().equals(account))
            {
                unReadMsg.remove(mapEntry.getKey());
            }
        }

//        for (String key : unReadMsg.keySet())
//        {
//            if (account.equals(unReadMsg.get(key).getFrom()))
//            {
//                unReadMsg.remove(key);
//            }
//        }
    }

    /**
     * 通过groupId删除消息
     * @param groupId
     */
    public synchronized void delMsgByGroupId(String groupId)
    {
        Iterator<Map.Entry<String, Message>> it = unReadMsg.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Message> mapEntry = it.next();
            if (ImFunc.getIns().isGroupMessage(mapEntry.getValue().getType()) && mapEntry.getValue().getFrom().equals(groupId))
            {
                unReadMsg.remove(mapEntry.getKey());
            }
        }
    }

    /**
     * 通过groupId获取未读消息数
     * @param groupId
     * @return
     */
    public synchronized int getUnreadMsgNumByGroupId(String groupId)
    {
        int num = 0;
        Iterator<Map.Entry<String, Message>> it = unReadMsg.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Message> mapEntry = it.next();
            if (ImFunc.getIns().isGroupMessage(mapEntry.getValue().getType()) && mapEntry.getValue().getFrom().equals(groupId))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * 通过联系人信息获取未读消息数
     * @param account
     * @return
     */
    public synchronized int getUnreadMsgNumByAccount(String account)
    {
        int num = 0;
        Iterator<Map.Entry<String, Message>> it = unReadMsg.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Message> mapEntry = it.next();
            if (ImFunc.getIns().isImMessage(mapEntry.getValue().getType()) && mapEntry.getValue().getFrom().equals(account))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * 刷新界面
     */
    public void postUnreadNumNotify()
    {
        if(unReadHandler != null)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = ImFunc.UNREAD_MSG_OFFLINE;
            unReadHandler.sendMessage(msg);
        }
    }

    /**
     * 清空未读消息容器
     */
    public synchronized void clearUnreadManager()
    {
        unReadMsg.clear();
    }

    /**
     * 根据chatId标记群组或某个聊天全部置为已读
     * @param chatId
     * @param msgType
     */
    public void setMsgMarked(String chatId, int msgType)
    {
        for (String key : unReadMsg.keySet())
        {
            if (chatId.equals(unReadMsg.get(key).getFrom()))
            {
                ImFunc.getIns().markRead(ImFunc.getIns().getMarkType(msgType), chatId, key, ImFunc.getIns().getMarkTag(msgType));
            }
        }
    }

    /**
     * 通过mesageId标记消息已读
     * @param chatId
     * @param messageId
     * @param msgType
     */
    public void setMsgMarked(String chatId, String messageId,int msgType)
    {
        for (String key : unReadMsg.keySet())
        {
            if (key.equals(messageId))
            {
                ImFunc.getIns().markRead(ImFunc.getIns().getMarkType(msgType), chatId, messageId, ImFunc.getIns().getMarkTag(msgType));
            }
        }
    }
}
