package com.huawei.esdk.uc.im.data;

/**
 * 获取漫游消息的结构体
 * 
 * Created by s00200335 on 11/20/14.
 */
public class GetHistoryMessageInfo
{
    private String account; //账号
    private long msgDaoId; //数据库存储id
    private String messageId; //消息id
    private int type; //消息类型，个人or 群组
    private boolean isFirst; //是否第一次获取漫游消息
    private long time;
    private boolean isHaveHistory;

    public GetHistoryMessageInfo(String account, long msgDaoId, String messageId, int type, boolean isFirst, long time)
    {
        setAccount(account);
        setMsgDaoId(msgDaoId);
        setMessageId(messageId);
        setType(type);
        setFirst(isFirst);
        setTime(time);
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public long getMsgDaoId()
    {
        return msgDaoId;
    }

    public void setMsgDaoId(long msgDaoId)
    {
        this.msgDaoId = msgDaoId;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public boolean isFirst()
    {
        return isFirst;
    }

    public void setFirst(boolean isFirst)
    {
        this.isFirst = isFirst;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public boolean isHaveHistory()
    {
        return isHaveHistory;
    }

    public void setHaveHistory(boolean isNoHistory)
    {
        this.isHaveHistory = isNoHistory;
    }
}
