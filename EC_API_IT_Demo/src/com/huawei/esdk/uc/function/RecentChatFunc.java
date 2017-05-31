package com.huawei.esdk.uc.function;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import com.huawei.dao.impl.InstantMessageDao;
import com.huawei.dao.impl.RecentChatContactDao;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.utils.StringUtil;

public class RecentChatFunc
{

    private static final String TAG = RecentChatFunc.class.getSimpleName();

    private static RecentChatFunc ins;

    private RecentChatFunc()
    {

    }

    public static RecentChatFunc getIns()
    {
        if (ins == null)
        {
            ins = new RecentChatFunc();
        }
        return ins;
    }

    public static List<RecentChatContact> getAllRecentChatContact()
    {
        List<RecentChatContact> recentContacts = RecentChatContactDao.query();

        if (null == recentContacts)
        {
            return new ArrayList<RecentChatContact>();
        }

        String account;
        for (RecentChatContact lc : recentContacts)
        {
            account = lc.getContactAccount();
            if (TextUtils.isEmpty(account))
            {
                continue;
            }

            int msgType = lc.getType();
            // 获得未读消息条数
            lc.setUnReadMsgsCount(InstantMessageDao.getUnReadMsgCount(account,
                    msgType));

            InstantMessage msg = null;
            List<InstantMessage> lastMsgs = InstantMessageDao.queryRecord(
                    account, 1, msgType, 0);
            if (lastMsgs != null && lastMsgs.size() > 0)
            {
                msg = lastMsgs.get(0);
            }
            if (null == msg)
            {
                continue;
            }
            lc.setInstantMsg(msg);
            lc.setEndTime(msg.getTimestamp().getTime());
        }

        return recentContacts;
    }

    public boolean delete(RecentChatContact chatContact)
    {
        return RecentChatContactDao.delete(chatContact);
    }

}
