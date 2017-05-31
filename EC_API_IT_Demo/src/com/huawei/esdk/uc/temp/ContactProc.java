package com.huawei.esdk.uc.temp;

import android.content.Intent;

import com.huawei.common.LocalBroadcast;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.UCResource;

/**
 * Created by lWX303895 on 2016/3/15.
 */
public class ContactProc implements LocalBroadcast.LocalBroadcastProc
{

    public ContactProc()
    {

    }

    @Override
    public boolean onProc(Intent intent, LocalBroadcast.ReceiveData rd)
    {
        String action = intent.getAction();
        rd.action = action;

        if (CustomBroadcastConst.UPDATE_CONTACT_VIEW.equals(action))
        {
            return onUpdateContactView(intent, rd);
        }

        return true;
    }

    private boolean onUpdateContactView(Intent intent, LocalBroadcast.ReceiveData rd)
    {
        UpdateContactResp resp = getUpateContactResp(
                intent.getIntExtra(UCResource.SERVICE_RESPONSE_DATA, UCResource.FRIEND_STATE_CHANGED));
        resp.setAccount(intent.getStringExtra(UCResource.UPDATE_CONTACT_ACCOUNT));
        resp.setStatusChangeAccounts(intent.getStringArrayListExtra(UCResource.UPDATE_STATUS_ACCOUNT));
        rd.data = resp;
        rd.result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, UCResource.REQUEST_OK);
        return true;
    }

    private UpdateContactResp getUpateContactResp(int contactSync)
    {
        UpdateContactResp resp = new UpdateContactResp(null);
        resp.setContactSynced(contactSync);
        return resp;
    }
}
