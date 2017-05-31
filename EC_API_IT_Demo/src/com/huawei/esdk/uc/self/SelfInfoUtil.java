package com.huawei.esdk.uc.self;

import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.ExecuteResult;
import com.huawei.data.base.BaseResponseData;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.service.ServiceProxy;

import android.content.Intent;
import android.util.Log;

/**
 * 个人设置接口
 */
public final class SelfInfoUtil
{
    private static final String TAG = SelfInfoUtil.class.getSimpleName();

    private static SelfInfoUtil ins = new SelfInfoUtil();

    private int settingStatus = ContactClientStatus.DEF;

    private SelfInfoUtil()
    {
    }

    public static SelfInfoUtil getIns()
    {
        return ins;
    }
    
    /**
     * 清空该账号所有data数据
     */
    public void clear()
    {
        SelfDataHandler.getIns().clear();
    }

    /**
     * 获取用户自己的状态
     * @return 用户状态
     */
    public int getStatus()
    {
        return SelfDataHandler.getIns().getSelfData().getStatus();
    }

    private void saveStatusToSelfData(int status, boolean isLogout)
    {
        SelfDataHandler.getIns().getSelfData().setStatus(status, isLogout);
    }

    /**
     * 设置状态
     * @param status <p>
     *        PersonalContact.ON_LINE;
     *        <p>
     *        PersonalContact.BUSY;
     *        <p>
     *        PersonalContact.AWAY
     * @return
     */
    public ExecuteResult setStatus(int status)
    {
        if (status == ContactClientStatus.DEF)
        {
            return null;
        }

        // 低内存的时候可能会把状态设置回默认值away，然后在恢复的时候设置该状态到服务器，所以要做下拦截。
        if (status == ContactClientStatus.AWAY)
        {
            status = ContactClientStatus.ON_LINE;
        }

        settingStatus = status;

        ServiceProxy service = UCAPIApp.getApp().getService();

        if (service != null)
        {
            ExecuteResult result = service.setStatus(status);

            return result;
        }

        return null;
    }

    public void setToLoginStatus()
    {
        // 因为是登录操作后，默认是在线状态，所以不需要发请求，本地存储即可
        saveStatusToSelfData(ContactClientStatus.ON_LINE, false);
        
    }

    public void setToLogoutStatus()
    {
        // 因为是注销操作后，要保证本地显示的是离线状态，所以只需要在本地存就可以
        saveStatusToSelfData(ContactClientStatus.AWAY, true);
    }

    public void onStatusRespProc(Intent intent)
    {
        if (settingStatus == ContactClientStatus.DEF)
        {
            return;
        }

        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
        if (UCResource.REQUEST_OK != result)
        {
            return;
        }
        BaseResponseData data = (BaseResponseData) intent
                .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
        Log.d(CommonUtil.APPTAG, TAG + " |ACTION_SET_STATUS_RESPONSE | result = "
                + result);
        if (data.getStatus() == ResponseCode.REQUEST_SUCCESS)
        {
            saveStatusToSelfData(settingStatus, false);
        }
    }

}
