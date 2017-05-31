package com.huawei.esdk.uc.function;

import android.text.TextUtils;

import com.huawei.utils.StringUtil;

import java.io.Serializable;
import java.util.*;

/**
 * 该类用来管理漫游聊天记录合并到聊天界面
 * Created by s00200335 on 11/8/14.
 */
public class HistoryMsgMergerManager
{
    private Map<String, List<String>> downloadHistoryMap = new HashMap<String, List<String>>();

    /**
     * 判断本地记录是否为空，如果为空需要请求服务器
     * @param account 联系人帐号
     * @return
     */
    public synchronized boolean isNeedFirstRequest(String account)
    {
        List<String> recordList = downloadHistoryMap.get(account);

        //todo 除了存在这个记录以外最好还要判断下是否超过10个，如果不到的话最好再请求一次

        // 如果本地没有记录，需要请求服务器
        return recordList == null;
    }

    /**
     * 判断某个联系人是否还需要请求更多的历史纪录
     * @param account 联系人帐号
     * @param lastMsgId 当前界面显示的最后一个消息ID
     * @return
     */
    public synchronized boolean isNeedRequestNext(String account, String lastMsgId)
    {
        List<String> recordList = downloadHistoryMap.get(account);

        //如果本地没有记录，需要请求服务器
        if (recordList == null)
        {
            return true;
        }

        int curIndex = recordList.indexOf(lastMsgId);

        //如果当前显示的最后一条消息ID不在记录列表里，默认需要继续请求服务器
        if (curIndex < 0)
        {
            return true;
        }

        int length = recordList.size();

        //当余下记录不足每次请求条数的话就需要去服务器请求了
        return length - curIndex <= ImFunc.MAX_MSGS;
    }

    /**
     * 获取当前记录列表里最后一个消息ID，以便继续去服务器做请求
     * @param account 联系人帐号
     * @return
     */
    public synchronized String getLastRecordId(String account)
    {
        List<String> recordList = downloadHistoryMap.get(account);

        if (recordList == null || recordList.isEmpty())
        {
            return "";
        }

        return recordList.get(recordList.size() - 1);
    }

    /**
     * 将从服务器请求的记录存到记录列表中
     * @param messageIdList
     * @param account
     */
    public synchronized void addRecordList(List<String> messageIdList, String account)
    {
        if (messageIdList == null || messageIdList.isEmpty() || TextUtils.isEmpty(account))
        {
            return;
        }

        List<String> recordList = downloadHistoryMap.get(account);

        if (recordList == null)
        {
            recordList = new ArrayList<String>();
            downloadHistoryMap.put(account, recordList);
        }

        for (String messageId : messageIdList)
        {
            if (!TextUtils.isEmpty(messageId) && !recordList.contains(messageId))
            {
                recordList.add(messageId);
            }
        }

        Collections.sort(recordList, new MyComparator());
    }

    /**
     * 清空记录，需要在断网的时候调用
     */
    public synchronized void cleanUp()
    {
        downloadHistoryMap.clear();
    }

    private static class MyComparator implements Comparator<String>, Serializable
    {
        private static final long serialVersionUID = -2764216822712682485L;

        @Override public int compare(String lhs, String rhs)
        {
            return -1 * lhs.compareTo(rhs);
        }
    }

}
