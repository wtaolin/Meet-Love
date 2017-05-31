package com.huawei.esdk.uc.group;

import java.util.List;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.data.ConstGroup;
import com.huawei.data.ConstGroupContact;
import com.huawei.data.GroupChangeNotifyData;
import com.huawei.data.LeaveGroupResp;
import com.huawei.data.base.BaseResponseData;
import com.huawei.device.DeviceManager;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.msghandler.maabusiness.LeaveGroupRequester;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GroupDetailActivity extends BaseActivity implements
        OnClickListener
{
    private static final String TAG = GroupDetailActivity.class.getSimpleName();

    private ImageView imgBack;

    private TextView tvFinish;

    private GridView membersGv;

    private RelativeLayout rlDiscussName;

    private TextView tvDiscussName;

    private RelativeLayout rlAnnounce;

    private TextView tvAnnounce;

    private RelativeLayout rlInfo;

    private TextView tvInfo;

    //private ImageView imgSaveToContact;

    private Button btnEixt;

    private ProgressDialog progressDialog;

    private DiscussionMemberAdpater memberAdpater;

    // 删除模式
    private boolean isDeleteMode;

    private String groupId;

    private int groupType;

    private ConstGroup constGroup;

    private IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_detail);

        initView();

        initData();

        refreshGroupMembers();

        filter = new IntentFilter();
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
        registerRec();

    }

    private void initView()
    {
        imgBack = (ImageView) findViewById(R.id.back_img);
        tvFinish = (TextView) findViewById(R.id.finish_tv);

        membersGv = (GridView) findViewById(R.id.contacts_added_gv);

        rlDiscussName = (RelativeLayout) findViewById(R.id.discussion_name);
        tvDiscussName = (TextView) findViewById(R.id.name);
        rlAnnounce = (RelativeLayout) findViewById(R.id.group_announce);
        tvAnnounce = (TextView) findViewById(R.id.announce);
        rlInfo = (RelativeLayout) findViewById(R.id.group_info);
        tvInfo = (TextView) findViewById(R.id.info);

//        imgSaveToContact = (ImageView) findViewById(R.id.save_img);

        btnEixt = (Button) findViewById(R.id.quit);

        imgBack.setOnClickListener(this);
        tvFinish.setOnClickListener(this);
        btnEixt.setOnClickListener(this);

//        imgSaveToContact.setOnClickListener(this);

        rlDiscussName.setOnClickListener(this);
        rlAnnounce.setOnClickListener(this);
        rlInfo.setOnClickListener(this);
    }

    private void initData()
    {
        groupId = getIntent().getStringExtra(IntentData.GROUPID);
        groupType = getIntent().getIntExtra(IntentData.GROUPTYPE,
                ConstGroup.DISCUSSION);

        constGroup = GroupFunc.getIns().findConstGroupById(groupId);

        tvDiscussName.setText(constGroup.getName());
        tvAnnounce.setText(constGroup.getAnnounce());
        tvInfo.setText(constGroup.getIntro());

        if (!constGroup.getOwner().equals(
                CommonVariables.getIns().getUserAccount()))
        {
            tvFinish.setVisibility(View.GONE);
        }

//        imgSaveToContact.setSelected(constGroup.getDiscussionFixed() == 1);
    }

    private void refreshGroupMembers()
    {
        if (membersGv == null || constGroup == null)
        {
            return;
        }
        final List<ConstGroupContact> memberList = GroupFunc.getIns()
                .getGroupMembers(groupId, groupType);
        if (memberList.size() <= 0)
        {
            GroupFunc.getIns().queryGroupMembersByGroupId(groupId, groupType);
        }

        int columWidth = getResources().getDimensionPixelSize(
                R.dimen.grid_item_wight);
        // 添加一个空白列
        int colums = memberList.size() + 1;
        // 总宽度
        int width = colums * columWidth;
        // 宽度为总宽度，高度自适应
        LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
        membersGv.setLayoutParams(params);
        // 设定列宽
        membersGv.setColumnWidth(columWidth);
        // 每列间隔
        membersGv.setHorizontalSpacing(0);
        membersGv.setStretchMode(GridView.NO_STRETCH);
        // 设定列数
        membersGv.setNumColumns(colums);

        memberAdpater = new DiscussionMemberAdpater(this, memberList,
                constGroup.getOwner(), GroupFunc.getIns().getGroupCapacity(
                        groupId));
        membersGv.setAdapter(memberAdpater);

        // 固定群不能加人和删人
        if (constGroup.getGroupType() == ConstGroup.DISCUSSION)
        {
            memberAdpater.setDiscussionFlag(true);
        }
        else
        {
            memberAdpater.setDiscussionFlag(false);
        }

        memberAdpater.setDeleteMode(isDeleteMode);
        memberAdpater
                .setOnMemberClickListener(new DiscussionMemberAdpater.OnMemberClickListener()
                {
                    @Override
                    public void onMemberClick(int position)
                    {
                        if (position < memberList.size())
                        {
                            ConstGroupContact clickContact = memberList
                                    .get(position);
                            if (isDeleteMode
                                    && !clickContact.getEspaceNumber()
                                            .equalsIgnoreCase(
                                                    CommonVariables.getIns()
                                                            .getUserAccount()))
                            {
                                GroupFunc.getIns().kickFromGroup(groupId,
                                        groupType, memberList.get(position));
                            }
                        }
                        else
                        {
                            if (constGroup.getGroupType() == ConstGroup.DISCUSSION)
                            {
                                if (position == memberList.size()
                                        && memberList.size() < GroupFunc
                                                .getIns().getGroupCapacity(
                                                        groupId))
                                {
                                    if (DeviceManager.isFastClick())
                                    {
                                        return;
                                    }
                                    Intent intent = new Intent(
                                            GroupDetailActivity.this,
                                            GroupMemberAddActivity.class);
                                    intent.putExtra(IntentData.GROUPID, groupId);
                                    intent.putExtra(IntentData.GROUPTYPE,
                                            groupType);
                                    startActivity(intent);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        case R.id.back_img:

            UCAPIApp.getApp().popActivity(GroupDetailActivity.this);
            break;
        case R.id.finish_tv:

            isDeleteMode = !isDeleteMode;
            memberAdpater.setDeleteMode(isDeleteMode);
            memberAdpater.notifyDataSetChanged();
            if (isDeleteMode)
            {
                tvFinish.setText(R.string.btn_done);
            }
            else
            {
                tvFinish.setText(R.string.edit);
            }

            break;
        case R.id.quit:

            showProgerssDlg();
            GroupFunc.getIns().leaveGroup(groupId, groupType);
            break;
//        case R.id.save_img:
//
//            GroupFunc.getIns()
//                    .fixGroup(groupId, !imgSaveToContact.isSelected());
//
//            break;
        case R.id.discussion_name:

            showGroupNameDialog();

            break;

        default:
            break;
        }
    }

    /**
     * 等待对话框
     */
    private void showProgerssDlg()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在操作，请稍后······");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeDialog()
    {
        if (null != progressDialog && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    private void showGroupNameDialog()
    {
        int maxLen = ConstGroup.DISCUSSION_NAME_MAXLEN;
        String originText = constGroup.getName();

        final EditText editText = new EditText(this);
        editText.setText(originText);
        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(
                maxLen)});
        new AlertDialog.Builder(this).setView(editText)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String newText = editText.getText().toString();
                        GroupFunc.getIns().modifyGroup(groupId, newText);
                    }
                }).show();
    }

    private void registerRec()
    {
        // registerReceiver(receiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                filter);
    }

    private void unRegisterRec()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        // unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(CommonUtil.APPTAG, TAG + " | action = " + action);

            if (CustomBroadcastConst.ACTION_LEAVE_GROUP.equals(action))
            {

                int result = intent.getIntExtra(
                        UCResource.SERVICE_RESPONSE_RESULT, 0);

                if (UCResource.REQUEST_OK == result)
                {
                    BaseResponseData data = (BaseResponseData) intent
                            .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    if (ResponseCode.REQUEST_SUCCESS == data.getStatus())
                    {
                        LeaveGroupResp resp = (LeaveGroupResp) data;
                        if (LeaveGroupRequester.LEAVE == resp.getLeaveFlag())
                        {
                            closeDialog();
                            UCAPIApp.getApp().popActivity(
                                    GroupDetailActivity.this);
                        }
                        else
                        {
                            refreshGroupMembers();
                        }
                    }
                }

            }
            // else if (ACTION.ACTION_DELETE_MEMBER_GROUP.equals(action))
            // {
            // refreshGroupMembers();
            // }
            else if (CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP
                    .equals(action))
            {
                refreshGroupMembers();
            }
            
            
            else if (CustomBroadcastConst.ACTION_GROUP_CHANGE.equals(action))
            {
                int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
                if (UCResource.REQUEST_OK == result)
                {
                    GroupChangeNotifyData changeNotify = (GroupChangeNotifyData) intent
                            .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    String groupId = changeNotify.getGroupId();
                    int groupType = changeNotify.getGroupType();
                    String newGroupName = changeNotify.getName();

                    ConstGroup group = GroupFunc.getIns().findConstGroupById(groupId);

                    if (group != null && !group.getName().equals(newGroupName))
                    {
                        group.setName(newGroupName);
                  
                    }
                    
                    constGroup = GroupFunc.getIns().findConstGroupById(groupId);

                    if (constGroup != null)
                    {
                        if (tvDiscussName != null)
                        {
                            tvDiscussName.setText(constGroup.getName());
                        }
//                        if (imgSaveToContact != null)
//                        {
//                            imgSaveToContact.setSelected(constGroup
//                                    .getDiscussionFixed() == 1);
//                        }
                    }
                    refreshGroupMembers();
                }
            }
    
            else if (CustomBroadcastConst.ACTION_GET_GROUP_PIC.equals(action)||CustomBroadcastConst.ACTION_FIX_GROUP.equals(action) || CustomBroadcastConst.ACTION_MODIFY_GROUP.equals(action))
            {
                constGroup = GroupFunc.getIns().findConstGroupById(groupId);

                if (constGroup != null)
                {
                    if (tvDiscussName != null)
                    {
                        tvDiscussName.setText(constGroup.getName());
                    }
//                    if (imgSaveToContact != null)
//                    {
//                        imgSaveToContact.setSelected(constGroup
//                                .getDiscussionFixed() == 1);
//                    }
                }
                refreshGroupMembers();
            }
            else if (CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE
                    .equals(action))
            {
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

                GroupChangeNotifyData changeData = (GroupChangeNotifyData) data;
                if (null == changeData)
                {
                    return;
                }
                String deleteGroupId = changeData.getGroupId();

                if (groupId.equals(deleteGroupId))
                {
                    UCAPIApp.getApp().popActivity(GroupDetailActivity.this);
                }
            }
            else if (CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER
                    .equals(action))
            {
                refreshGroupMembers();
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unRegisterRec();
    }

    @Override
    public void initializeData()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void initializeComposition()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearData()
    {
        // TODO Auto-generated method stub

    }

}
