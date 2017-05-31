package com.huawei.esdk.uc.conf.data;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class AddMemberEntity
{
    private int resource;

    private String name;

    // private String pingyinName;

    private String account;

    private boolean isFirstMember;

    public int getResource()
    {
        return resource;
    }

    public void setResource(int resource)
    {
        this.resource = resource;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /*public String getPingyinName()
    {
        return pingyinName;
    }

    public void setPingyinName(String pingyinName)
    {
        this.pingyinName = pingyinName;
    }*/

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public boolean isFirstMember()
    {
        return isFirstMember;
    }

    public void setIsFirstMember(boolean isFirstMember)
    {
        this.isFirstMember = isFirstMember;
    }
}
