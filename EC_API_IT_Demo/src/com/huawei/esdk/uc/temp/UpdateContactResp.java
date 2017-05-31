package com.huawei.esdk.uc.temp;

import com.huawei.common.constant.UCResource;
import com.huawei.data.base.BaseResponseData;
import com.huawei.ecs.mip.common.BaseMsg;

import java.util.ArrayList;

/**
 * Created by lWX303895 on 2016/3/15.
 */
public class UpdateContactResp extends BaseResponseData
{

    private static final long serialVersionUID = -8106914275242383328L;

    private int contactSynced;
    private String account;

    private ArrayList<String> statusChangeAccounts;

    /**
     * ���췽��
     * @param msg BaseMsg
     */
    public UpdateContactResp(BaseMsg msg)
    {
        super(msg);
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public int getContactSynced()
    {
        return contactSynced;
    }

    public void setContactSynced(int synced)
    {
        contactSynced = synced;
    }

    @Override
    public String toString()
    {
        return "UpdateContactResp{" +
                "contactSynced=" + contactSynced +
                ", account='" + account + '\'' +
                '}';
    }

    public void setStatusChangeAccounts(ArrayList<String> statusChangeAccounts)
    {
        this.statusChangeAccounts = statusChangeAccounts;
    }

    public ArrayList<String> getStatusChangeAccounts()
    {
        return statusChangeAccounts;
    }

    public boolean isContactSynced()
    {
        return getContactSynced() == UCResource.CONTACT_SYNCED;
    }

    public boolean isStateChanged()
    {
        return getContactSynced() == UCResource.FRIEND_STATE_CHANGED;
    }
}
