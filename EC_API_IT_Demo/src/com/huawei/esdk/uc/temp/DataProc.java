package com.huawei.esdk.uc.temp;

import android.content.Intent;

import com.huawei.common.LocalBroadcast;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.UCResource;
import com.huawei.data.base.BaseResponseData;
import com.huawei.esdk.uc.CommonProc;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lWX303895 on 2016/3/15.
 */
public class DataProc
{
    private final static DataProc INS = new DataProc();

    private static final Map<String, LocalBroadcast.LocalBroadcastProc> PROCESSOR =
            new HashMap<String, LocalBroadcast.LocalBroadcastProc>();

    static
    {
        ContactProc    contactProc    = new ContactProc();
        CommonProc commonProc     = new CommonProc();

        PROCESSOR.put(CustomBroadcastConst.UPDATE_CONTACT_VIEW, contactProc);
        PROCESSOR.put(CustomBroadcastConst.KICKED_BY_SVN_GATEWAY, commonProc);
    }

    private final LocalBroadcast.LocalBroadcastProc proc = new LocalBroadcast.LocalBroadcastProc()
    {
        @Override
        public boolean onProc(Intent intent, LocalBroadcast.ReceiveData rd)
        {
            LocalBroadcast.LocalBroadcastProc proc = PROCESSOR.get(intent.getAction());

            if (proc != null)
            {
                return proc.onProc(intent, rd);
            }
            else
            {
                return onCommonProc(intent, rd);
            }
        }
    };

    private DataProc()
    {
    }

    public static DataProc getIns()
    {
        return INS;
    }

    public LocalBroadcast.LocalBroadcastProc getProc()
    {
        return proc;
    }

    private boolean onCommonProc(Intent intent, LocalBroadcast.ReceiveData rd)
    {
        rd.action = intent.getAction();
        String name = UCResource.SERVICE_RESPONSE_RESULT;
        rd.result = intent.getIntExtra(name, UCResource.REQUEST_OK);
        name = UCResource.SERVICE_RESPONSE_DATA;
        Object obj = intent.getSerializableExtra(name);
        if (obj instanceof BaseResponseData)
        {
            rd.data = (BaseResponseData) obj;
        }
        return true;
    }

}
