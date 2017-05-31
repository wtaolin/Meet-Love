package com.huawei.esdk.uc.function;

import android.text.TextUtils;

import java.util.List;

import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.ContactTools;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.ExecuteResult;
import com.huawei.data.PersonalTeam;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.service.ServiceProxy;
import com.huawei.utils.StringUtil;

public class ContactFunc
{

    private static final String TAG = ContactFunc.class.getSimpleName();

    private static ContactFunc instance;

    private ContactFunc()
    {

    }

    public static ContactFunc getIns()
    {
        if (null == instance)
        {
            instance = new ContactFunc();
        }
        return instance;
    }

    /**
     * 加载联系人
     * @param syncMode 同步模式： <li>0：不同步 <li>1：增量同步 <li>2：完全同步
     */
    public void loadContact(int syncMode)
    {
        ServiceProxy serviceProxy = UCAPIApp.getApp().getService();
        if (null == serviceProxy)
        {
            return;
        }
        serviceProxy.loadContacts(syncMode);
    }

    /**
     * 搜索企业通讯录
     * @param condition
     * @return
     */
    public ExecuteResult searchContact(String condition)
    {
        ServiceProxy serviceProxy = UCAPIApp.getApp().getService();
        if (null == serviceProxy)
        {
            return null;
        }
        // 这里只搜索前50条记录，分页处理暂时不做了
        return UCAPIApp.getApp().getService()
                .searchContact(condition, true, 1, 50, true);
    }

    /**
     * 获取所有联系人好友
     * @return
     */
    public List<PersonalContact> getAllContacts()
    {
        return ContactCache.getIns().getFriends().getAllContacts();
    }

    /**
     * 通过账号获取联系人信息
     * @param account
     * @return
     */
    public PersonalContact getContactByAccount(String account)
    {
        if (TextUtils.isEmpty(account))
        {
            return null;
        }

        PersonalContact contact = ContactCache.getIns().getContactByAccount(
                account);
        if (contact == null)
        {
            contact = ContactLogic.getIns().getContactByEspaceAccount(account);
        }
        return contact;
    }

    /**
     * 通过号码获取联系人信息
     * @param number
     * @return
     */
    public PersonalContact getContactByNumber(String number)
    {
        if (TextUtils.isEmpty(number))
        {
            return null;
        }

        PersonalContact contact = ContactCache.getIns().getContactByNumber(
                number);
        if (contact == null)
        {
            contact = ContactLogic.getIns().getContactByPhoneNumber(number);
        }
        return contact;
    }

    /**
     * 获取联系人显示的名称
     * @param pc
     * @return
     */
    public String getDisplayName(PersonalContact pc)
    {
        return ContactTools.getDisplayName(pc, null, null);
    }

    /**
     * 获取联系人显示的名称
     * @param pc
     * @param originalName
     * @return
     */
    public String getDisplayName(PersonalContact pc, String originalName)
    {
        return ContactTools.getDisplayName(pc, originalName, null);
    }

    /**
     * 获取自己的信息
     * @return
     */
    public PersonalContact getMySelf()
    {
        return ContactLogic.getIns().getMyContact();
    }

    /**
     * 获取联系人分组
     * @return
     */
    public List<PersonalTeam> getAllTeams()
    {
        return ContactLogic.getIns().getTeams();
    }

    /**
     * 获取所有联系人分组的信息
     * @return
     */
    public PersonalTeam getAllContactsTeam()
    {
        return ContactLogic.getIns().getAllContactsTeam();
    }

}
