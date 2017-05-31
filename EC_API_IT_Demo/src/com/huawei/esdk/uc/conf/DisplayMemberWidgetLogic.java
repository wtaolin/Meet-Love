package com.huawei.esdk.uc.conf;

import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;

import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.esdk.uc.utils.StatusUtil;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class DisplayMemberWidgetLogic
{
    public boolean getStatusResource(String account, ImageView view)
    {
        PersonalContact pc = ContactCache.getIns().getContactByAccount(account);
        if (pc == null)
        {
            //����û�е���ϵ��,Ҳ������״̬.
            pc = new PersonalContact();
            pc.setEspaceNumber(account);
        }

        handleStatusView(pc, view, false, false);
        return view.getVisibility() == View.VISIBLE;
    }

    public static void handleStatusView(PersonalContact pc,
                                        ImageView imageView, boolean isSetStatusAway, boolean search)
    {
        if (!ContactLogic.getIns().getAbility().isPresenceAbility()
                || pc == null || !pc.hasStatus())
        {
            imageView.setVisibility(View.INVISIBLE);
            return;
        }

        int status = getStatus(pc, search, isSetStatusAway);  //search ? pc.getEnterpriseStatus() : pc.status(false);
        int clientType = pc.getClientType();

        int resId = StatusUtil.getStatusResource(status, clientType);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageResource(resId);
    }

    private static int getStatus(PersonalContact pc, boolean search, boolean isSetStatusAway)
    {
        int status = pc.getShowStatus(search, false);
        if (isSetStatusAway && status == ContactClientStatus.DEF)
        {
            status = ContactClientStatus.AWAY;
        }
        return status;
    }


}
