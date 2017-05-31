package com.huawei.esdk.uc.headphoto;

import android.text.TextUtils;

import com.huawei.utils.StringUtil;

/**
 * ��������ȡͷ��Ĳ���,ֻ֧�ַ��͵���ͷ��.
 * @author h00203586
 */
public class HeadPhoto
{
    private String account = "";
    private String id = "";

    public HeadPhoto(String eSpaceNumber, String headId)
    {
        if (!TextUtils.isEmpty(eSpaceNumber))
        {
            account = eSpaceNumber;
        }
        
        if (!TextUtils.isEmpty(headId))
        {
            id = headId;
        }
    }

    public String getAccount()
    {
        return account;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof HeadPhoto)
        {
            HeadPhoto mPhoto = (HeadPhoto)o;
            return account.equals(mPhoto.getAccount());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return account.length();
    }

    @Override
    public String toString()
    {
        return account;
    }
}
