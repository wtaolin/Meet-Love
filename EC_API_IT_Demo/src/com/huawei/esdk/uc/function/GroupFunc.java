package com.huawei.esdk.uc.function;

import java.util.ArrayList;
import java.util.List;

import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.group.ConstGroupManager;
import com.huawei.data.ConstGroup;
import com.huawei.data.ConstGroupContact;
import com.huawei.data.ExecuteResult;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.service.ServiceProxy;

public class GroupFunc
{
    private static final String TAG = GroupFunc.class.getSimpleName();

    private static GroupFunc instance;

    private GroupFunc()
    {

    }

    public static GroupFunc getIns()
    {
        if (null == instance)
        {
            instance = new GroupFunc();
        }
        return instance;
    }

    /**
     * 创建讨论组
     * @param contactList
     * @return
     */
    public ExecuteResult createDiscussGroup(List<PersonalContact> contactList)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        if (service == null)
        {
            return null;
        }
        return service.createDiscussGroup(contactList);
    }

    /**
     * 邀请联系人加入讨论组
     * @param group
     * @param contactList
     * @return
     */
    public ExecuteResult inviteToGroup(ConstGroup group,
            List<PersonalContact> contactList)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        if (service == null)
        {
            return null;
        }
        // 邀人
        return service.inviteToGroup(group, contactList);
    }

    /**
     * 离开讨论组
     * @param groupId
     * @param groupType ConstGroup.DISCUSSION:讨论组
     * @return
     */
    public ExecuteResult leaveGroup(String groupId, int groupType)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        if (service == null)
        {
            return null;
        }
        return service.leaveGroup(groupId, groupType);
    }

    /**
     * 固化讨论组
     * @param groupId
     * @param isFixed
     * @return
     */
    public ExecuteResult fixGroup(String groupId, boolean isFixed)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        if (service == null)
        {
            return null;
        }
        return service.fixGroup(groupId, isFixed);
    }

    /**
     * 修改讨论组名称
     * @param groupId
     * @param newText
     * @return
     */
    public ExecuteResult modifyGroup(String groupId, String newText)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        ConstGroup group = findConstGroupById(groupId);

        if (service == null || group == null)
        {
            return null;
        }
        ConstGroup newgroup = group;
        newgroup.setGroupId(groupId);
        newgroup.setName(newText);

        // 修改讨论组信息
        return service.manageGroup(newgroup);
    }

    /**
     * 将某成员请出讨论组
     * @param groupId
     * @param groupType ConstGroup.DISCUSSION：讨论组
     * @param member
     * @return
     */
    public ExecuteResult kickFromGroup(String groupId, int groupType,
            PersonalContact member)
    {
        ServiceProxy service = UCAPIApp.getApp().getService();
        if (service == null)
        {
            return null;
        }
        return service.kickFromGroup(groupId, groupType, member);
    }

    /**
     * 获取讨论组列表
     * @return
     */
    public List<ConstGroup> getDiscussionGroups()
    {
        // 该方法获取到的包括固定群，需要根据groupType过滤处理下
        List<ConstGroup> groups = ConstGroupManager.ins().getConstGroups();
        List<ConstGroup> disGroups = new ArrayList<ConstGroup>();
        for (ConstGroup constGroup : groups)
        {
            //if (ConstGroup.DISCUSSION == constGroup.getGroupType()) /** 注释掉这句代码就将获取讨论组跟固定群组*/
            {
                disGroups.add(constGroup);
            }
          
        }
        return disGroups;
    }

    /**
     * 通过groupId和groupType获取讨论组信息
     * @param groupId
     * @return
     */
    public ConstGroup findConstGroupById(String groupId)
    {
        return ConstGroupManager.ins().findConstGroupById(groupId);
    }

    /**
     * 通过groupId和groupType查寻讨论组成员
     * @param groupId
     * @param groupType ConstGroup.DISCUSSION:讨论组;ConstGroup.FIXED:固定群组
     * @return
     */
    public void queryGroupMembersByGroupId(String groupId, int groupType)
    {
        ConstGroupManager.ins().queryGroupMembersByGroupId(groupId, groupType);
    }

    /**
     * 通过groupId和groupType获取讨论组成员列表，首次获取需要先调用方法查询，即queryGroupMembersByGroupId
     * @param groupId
     * @param groupType ConstGroup.DISCUSSION:讨论组;ConstGroup.FIXED:固定群组
     * @return
     */
    public List<ConstGroupContact> getGroupMembers(String groupId, int groupType)
    {
        return ConstGroupManager.ins().getGroupMembers(groupId, groupType);
    }

    /**
     * 获取讨论组容量
     * @param groupId
     * @return
     */
    public int getGroupCapacity(String groupId)
    {
        ConstGroup group = findConstGroupById(groupId);
        if (group != null && group.getCapacity() > 0)
        {
            return group.getCapacity();
        }
        else
        {
            return ContactLogic.getIns().getMyOtherInfo().getGroupCapacity();
        }
    }

//    /**
//     * 处理创建讨论组响应
//     * @param intent
//     */
//    public void handleGroupCreate(Intent intent)
//    {
//        Intent intentCreateGroup = new Intent(ACTION.ACTION_CREATE_GROUP);
//
//        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
//
//        Log.d(CommonUtil.APPTAG, TAG + " | ACTION_CREATE_GROUP | result = "
//                + result);
//
//        intentCreateGroup.putExtra(IntentData.RESULT, result);
//
//        if (UCResource.REQUEST_OK == result)
//        {
//            BaseResponseData data = (BaseResponseData) intent
//                    .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
//
//            intentCreateGroup.putExtra(IntentData.BASERESPONSEDATA, data);
//        }
//
//        UCAPIApp.getApp().sendBroadcast(intentCreateGroup);
//    }
//
//    /**
//     * 处理邀请联系人加入讨论组响应
//     * @param intent
//     */
//    public void handleGroupInvite(Intent intent)
//    {
//        Intent intentGroupInvite = new Intent(ACTION.ACTION_INVITETO_JOIN_GROUP);
//
//        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
//
//        Log.d(CommonUtil.APPTAG, TAG
//                + " | ACTION_INVITETO_JOIN_GROUP | result = " + result);
//
//        intentGroupInvite.putExtra(IntentData.RESULT, result);
//
//        if (UCResource.REQUEST_OK == result)
//        {
//            BaseResponseData data = (BaseResponseData) intent
//                    .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
//            intentGroupInvite.putExtra(IntentData.BASERESPONSEDATA, data);
//        }
//
//        UCAPIApp.getApp().sendBroadcast(intentGroupInvite);
//    }
//
//    /**
//     * 处理自己被创建者请出讨论组的通知
//     * @param intent
//     */
//    public void handleGroupDelete(Intent intent)
//    {
//        Intent intentGroupDelete = new Intent(ACTION.ACTION_GROUP_DELETE);
//
//        Log.d(CommonUtil.APPTAG, TAG + " | ACTION_GROUP_DELETE ");
//
//        BaseResponseData data = (BaseResponseData) intent
//                .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
//        intentGroupDelete.putExtra(IntentData.BASERESPONSEDATA, data);
//
//        UCAPIApp.getApp().sendBroadcast(intentGroupDelete);
//    }
//
//    /**
//     * 处理讨论组更改的通知
//     * @param intent
//     */
//    public void handleGroupChange(Intent intent)
//    {
//        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
//        if (UCResource.REQUEST_OK == result)
//        {
//            GroupChangeNotifyData changeNotify = (GroupChangeNotifyData) intent
//                    .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
//            String groupId = changeNotify.getGroupId();
//            int groupType = changeNotify.getGroupType();
//            String newGroupName = changeNotify.getName();
//
//            ConstGroup group = GroupFunc.getIns().findConstGroupById(groupId);
//
//            if (group != null && !group.getName().equals(newGroupName))
//            {
//                group.setName(newGroupName);
//            }
//        }
//
//        UCAPIApp.getApp().sendBroadcast(
//                new Intent(ACTION.ACTION_GROUP_UPDATE));
//    }
//
//    /**
//     * 处理自己离开讨论组和将某人请出讨论组的通知
//     * @param intent
//     */
//    public void handleGroupLeave(Intent intent)
//    {
//        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
//
//        if (UCResource.REQUEST_OK == result)
//        {
//            BaseResponseData data = (BaseResponseData) intent
//                    .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
//            if (ResponseCode.REQUEST_SUCCESS == data.getStatus())
//            {
//                LeaveGroupResp resp = (LeaveGroupResp) data;
//                if (LeaveGroupRequester.LEAVE == resp.getLeaveFlag())
//                {
//                    ConstGroupManager.ins().deleteGroup(resp.getGroupId());
//                    Intent intentLeave = new Intent(ACTION.ACTION_LEAVE_GROUP);
//                    UCAPIApp.getApp().sendBroadcast(intentLeave);
//                }
//                else
//                {
//                    Intent intentDelete = new Intent(
//                            ACTION.ACTION_DELETE_MEMBER_GROUP);
//                    UCAPIApp.getApp().sendBroadcast(intentDelete);
//                }
//            }
//        }
//    }

}
