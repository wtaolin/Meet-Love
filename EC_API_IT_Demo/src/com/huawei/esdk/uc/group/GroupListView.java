package com.huawei.esdk.uc.group;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.data.ConstGroup;
import com.huawei.data.entity.InstantMessage;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.im.ChatActivity;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.esdk.uc.utils.UnreadMessageManager;

public class GroupListView extends LinearLayout implements OnItemClickListener
{

    private ListView listView;

    private GroupAdapter groupAdapter;

    private List<ConstGroup> constGroups = new ArrayList<ConstGroup>();

    private Context context;

    private ImFunc.LocalImReceiver localImReceiver;

    private static final int REFRESH_GROUPVIEW = 0x02;

    private Handler groupHandler;
    
    private IntentFilter filter;

    public GroupListView(Context context)
    {
        this(context, null);
        

    }

    public GroupListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;

        View view = LayoutInflater.from(context).inflate(R.layout.view_group,
                null);
        addView(view);

        updateGroups();

        listView = (ListView) view.findViewById(R.id.list_group);
        groupAdapter = new GroupAdapter(context, constGroups);
        listView.setAdapter(groupAdapter);

        listView.setOnItemClickListener(this);
        //listView.setOnItemLongClickListener(this);
    }
    
    
    

    public void updateGroups()
    {
        constGroups.clear();
        constGroups.addAll(GroupFunc.getIns().getDiscussionGroups());

        //Collections.sort(constGroups, comparator);

        if (null != groupAdapter)
        {
            groupAdapter.notifyDataSetChanged();
        }
    }

    private void gotoChat(String groupId, int groupType)
    {
        Intent intentChat = new Intent(context, ChatActivity.class);
        intentChat.putExtra(IntentData.GROUPID, groupId);
        intentChat.putExtra(IntentData.GROUPTYPE, groupType);
        context.startActivity(intentChat);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
    {
        String groupId = constGroups.get(arg2).getGroupId();
        int groupType = constGroups.get(arg2).getGroupType();
        gotoChat(groupId, groupType);
    }

    private void initHandler()
    {
        groupHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case REFRESH_GROUPVIEW:
                        groupAdapter.notifyDataSetChanged();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /**
     * 初始化接收聊天消息,并注册
     */
    private void registerIM()
    {
        localImReceiver = new ImFunc.LocalImReceiver()
        {
            public void onReceive(InstantMessage msg, boolean update)
            {
                if (null == msg)
                {
                    return;
                }
//                groupAdapter.notifyDataSetChanged();
                groupHandler.sendEmptyMessage(REFRESH_GROUPVIEW);
            }

            @Override
            public void onRequestHistoryBack(List<InstantMessage> msgList,
                    GetHistoryMessageInfo info)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void refreshDisplayAfterSendMessage(InstantMessage msg)
            {
                // TODO Auto-generated method stub

            }
        };
        // 注册
        ImFunc.getIns().registerBroadcast(localImReceiver);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        registerIM();
        initHandler();

        filter = new IntentFilter();
        filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
        filter.addAction(CustomBroadcastConst.ACTION_ADD_FRIEND_RESPONSE);

        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(receiver, filter);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        ImFunc.getIns().unRegisterBroadcast(localImReceiver);
        
        filter.addAction(CustomBroadcastConst.ACTION_CREATE_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GET_GROUP_PIC);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER);
        filter.addAction(CustomBroadcastConst.ACTION_LEAVE_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GROUP_CHANGE);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_CHAT);
        filter.addAction(CustomBroadcastConst.ACTION_FIX_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_MODIFY_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_MEMBERCHANGE);
        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE);
        
        LocalBroadcastManager.getInstance(this.getContext()).unregisterReceiver(receiver);
        
    }
    
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateGroups();
        }
    };

//    private class GroupComparator implements Comparator
//    {
//
//        public int compare(Object item1, Object item2)
//        {
//            ConstGroup group1 = (ConstGroup) item1;
//            ConstGroup group2 = (ConstGroup) item2;
//
//            return group2.getGroupId().compareTo(group1.getGroupId());
//
//        }
//
//    }

}
