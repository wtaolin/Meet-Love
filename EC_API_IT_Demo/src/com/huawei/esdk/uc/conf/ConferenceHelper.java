package com.huawei.esdk.uc.conf;

import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.device.DeviceManager;
import com.huawei.esdk.uc.conf.data.AddMemberEntity;
import com.huawei.esdk.uc.utils.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class ConferenceHelper
{
    static int compute()
    {
        // ħ�����֣�������ô�㲻���
        int offset = UIUtil.dipToPx(70);
        offset += UIUtil.dipToPx(7) * 3;
        offset += UIUtil.dipToPx(19);

        return DeviceManager.getScreenWidth() - offset;
    }

    static List<AddMemberEntity> transfer(List<ConferenceMemberEntity> members)
    {
        if (members.isEmpty())
        {
            return null;
        }

        List<AddMemberEntity> entities = new ArrayList<AddMemberEntity>();
        for (ConferenceMemberEntity member : members)
        {
            AddMemberEntity entity = new AddMemberEntity();
            entity.setAccount(member.getConfMemEspaceNumber());
            entity.setName(member.getDisplayName());
            entity.setIsFirstMember(member.isHost());
            entities.add(entity);
        }

        return entities;
    }
}
