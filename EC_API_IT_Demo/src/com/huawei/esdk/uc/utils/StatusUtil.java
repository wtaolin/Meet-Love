package com.huawei.esdk.uc.utils;

import android.util.SparseIntArray;

import com.huawei.contacts.ContactClientStatus;
import com.huawei.esdk.uc.R;

import java.util.HashMap;

/**
 * Created by lWX303895 on 2016/4/26.
 * 显示与会者的辅助界面
 */
public class StatusUtil
{
    private static final int ARRAY_SIZE = 4;

    private static final HashMap<Integer, SparseIntArray> statusMap = new HashMap<Integer, SparseIntArray>();

    static
    {
        SparseIntArray intArray = new SparseIntArray(ARRAY_SIZE);
        intArray.put(ContactClientStatus.MOBILEESPACE, R.drawable.recent_online_mobile);
        intArray.put(ContactClientStatus.PADESPACE, R.drawable.recent_online_mobile);
        intArray.put(ContactClientStatus.WEBESPACE, R.drawable.recent_web_online);
        intArray.put(ContactClientStatus.IPPHONEESPACE, R.drawable.recent_ipphone_online);
        statusMap.put(ContactClientStatus.ON_LINE, intArray);

        intArray = new SparseIntArray(ARRAY_SIZE);
        intArray.put(ContactClientStatus.MOBILEESPACE, R.drawable.recent_busy_mobile);
        intArray.put(ContactClientStatus.PADESPACE, R.drawable.recent_busy_mobile);
        intArray.put(ContactClientStatus.WEBESPACE, R.drawable.recent_web_busy);
        intArray.put(ContactClientStatus.IPPHONEESPACE, R.drawable.recent_ipphone_busy);
        statusMap.put(ContactClientStatus.BUSY, intArray);

        intArray = new SparseIntArray(ARRAY_SIZE);
        intArray.put(ContactClientStatus.MOBILEESPACE, R.drawable.recent_away_mobile);
        intArray.put(ContactClientStatus.PADESPACE, R.drawable.recent_away_mobile);
        intArray.put(ContactClientStatus.WEBESPACE, R.drawable.recent_web_leave);
        intArray.put(ContactClientStatus.IPPHONEESPACE, R.drawable.recent_ipphone_away);
        statusMap.put(ContactClientStatus.XA, intArray);

        intArray = new SparseIntArray(ARRAY_SIZE);
        intArray.put(ContactClientStatus.MOBILEESPACE, R.drawable.recent_uninterruptable_mobile);
        intArray.put(ContactClientStatus.PADESPACE, R.drawable.recent_uninterruptable_mobile);
        intArray.put(ContactClientStatus.WEBESPACE, R.drawable.recent_web_uninterret);
        intArray.put(ContactClientStatus.IPPHONEESPACE, R.drawable.recent_ipphone_uninterrupt);
        statusMap.put(ContactClientStatus.UNINTERRUPTABLE, intArray);

        intArray = new SparseIntArray(0);
        statusMap.put(ContactClientStatus.DEF, intArray);
    }


    /**
     * ��ȡ��Ӧ��״̬ͼ��
     * @param status ԭ״̬��
     * @param clientType �ͻ�������
     * @return ����״̬ͼ��ID
     */
    public static int getStatusResource(int status, int clientType)
    {
        SparseIntArray array = statusMap.get(status);
        if (array == null)
        {
            return R.drawable.recent_offline_small;
        }
        else if (array.size() < ARRAY_SIZE)
        {
            return R.drawable.recent_unknow_person;
        }
        else
        {
            return array.get(clientType, getDefaultStatusResource(status));
        }
    }

    private static int getDefaultStatusResource(int status)
    {
        int drwId = 0;
        switch (status)
        {
            case ContactClientStatus.ON_LINE:
                drwId = R.drawable.recent_online_big;
                break;
            case ContactClientStatus.BUSY:
                drwId = R.drawable.recent_busy_big;
                break;
            case ContactClientStatus.XA:
                drwId = R.drawable.recent_away_big;
                break;
            case ContactClientStatus.UNINTERRUPTABLE:
                drwId = R.drawable.recent_uninterrupt_big;
                break;
            default:
                break;
        }

        return drwId;
    }
}
