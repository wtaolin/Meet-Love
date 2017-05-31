package com.huawei.esdk.uc.group;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.ConstGroup;
import com.huawei.data.ExecuteResult;
import com.huawei.data.ManageGroupResp;
import com.huawei.data.base.BaseResponseData;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.contact.MemberAddHandler;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.esdk.uc.im.ChatActivity;
import com.huawei.utils.StringUtil;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GroupMemberAddActivity extends BaseActivity implements
        MemberAddHandler, OnClickListener
{

    private static final String TAG = GroupMemberAddActivity.class
            .getSimpleName();

    private ImageView ivBack;

    private TextView tvCreate;

    // 已添加成员的GridView
    private GridView contactsToAddGv;

    // 已添加成员的适配器
    private MemberAddAdapter addAdpater;

    private RelativeLayout rlContactListView;

    private ContactAddView contactAddView;

    private ProgressDialog progressDialog;

    private IntentFilter filter;

    private String groupId;

    private int groupType;

    private List<PersonalContact> groupContacts = new ArrayList<PersonalContact>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member_add);

        initView();

        initData();

        refreshGridView();

        filter = new IntentFilter();
        filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
        filter.addAction(CustomBroadcastConst.ACTION_CREATE_GROUP);
        filter.addAction(CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP);
        registerRec();
    }

    private void initView()
    {
        ivBack = (ImageView) findViewById(R.id.back_img);
        tvCreate = (TextView) findViewById(R.id.create_tv);

        contactsToAddGv = (GridView) findViewById(R.id.contacts_added_gv);

        rlContactListView = (RelativeLayout) findViewById(R.id.contact_list_rl);
        contactAddView = new ContactAddView(this);
        rlContactListView.addView(contactAddView);

        ivBack.setOnClickListener(this);
        tvCreate.setOnClickListener(this);

    }

    private void initData()
    {
        groupId = getIntent().getStringExtra(IntentData.GROUPID);
        groupType = getIntent().getIntExtra(IntentData.GROUPTYPE,
                ConstGroup.DISCUSSION);

        if (!TextUtils.isEmpty(groupId))
        {
            groupContacts.addAll(GroupFunc.getIns().getGroupMembers(groupId,
                    groupType));
            
            
            contactAddView.setMemberContacts(groupContacts);

            tvCreate.setText("邀请");
        }
        
    }

    /**
     * 刷新已添加成员
     */
    public void refreshGridView()
    {
        int columWidth = getResources().getDimensionPixelSize(
                R.dimen.grid_item_wight);
        // 添加一个空白列
        int colums = getAddList().size() + 1;
        // 总宽度
        int width = colums * columWidth;
        // 宽度为总宽度，高度自适应
        LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
        contactsToAddGv.setLayoutParams(params);
        // 设定列宽
        contactsToAddGv.setColumnWidth(columWidth);
        // 每列间隔
        contactsToAddGv.setHorizontalSpacing(0);
        contactsToAddGv.setStretchMode(GridView.NO_STRETCH);
        // 设定列数
        contactsToAddGv.setNumColumns(colums);
        final List<PersonalContact> addList = getAddList();
        addAdpater = new MemberAddAdapter(this, addList);
        // 已添加成员点击事件
        addAdpater
                .setOnMemberClickListener(new MemberAddAdapter.OnMemberClickListener()
                {
                    @Override
                    public void onMemberClick(int position)
                    {
                        // 移除成员
                        if (position < addList.size())
                        {
                        	PersonalContact contact = addList.get(position);
                            contactAddView.removeContact(contact);
                            //addAdpater.notifyDataSetChanged();
                        }
                    }
                });
        contactsToAddGv.setAdapter(addAdpater);
        // 成员为空时隐藏完成按钮，否则显示
        if (addList.isEmpty())
        {
            tvCreate.setVisibility(View.GONE);
        }
        else
        {
            tvCreate.setVisibility(View.VISIBLE);
        }
    }

    // 获取已添加成员的列表，需要与添加的数据分开保存，否则会引起界面错误
    private List<PersonalContact> getAddList()
    {
        return contactAddView.getAddContacts();
    }

    private void createGroup(List<PersonalContact> contactList)
    {
        ExecuteResult result = null;
        if (!TextUtils.isEmpty(groupId))
        {
            ConstGroup group = GroupFunc.getIns().findConstGroupById(groupId);
            result = GroupFunc.getIns().inviteToGroup(group, contactList);
        }
        else
        {
            // 创建讨论组
            result = GroupFunc.getIns().createDiscussGroup(contactList);
        }
        if (result.isResult())
        {
            showProgressDlg();
        }
    }

    private void showProgressDlg()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            if (!TextUtils.isEmpty(groupId))
            {
                progressDialog.setMessage("正在发送邀请，请稍后······");
            }
            else
            {
                progressDialog.setMessage("正在创建，请稍后······");
            }
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

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.back_img:

                UCAPIApp.getApp().popActivity(GroupMemberAddActivity.this);
                break;
            case R.id.create_tv:

                // if (getAddList().size() == 1 &&
                // StringUtil.isStringEmpty(groupId))
                // {
                // gotoChat(false);
                // }
                // else
                // {
                createGroup(getAddList());
                // }

                break;

            default:
                break;
        }

    }

    private void gotoChat(boolean isGroup)
    {
        Intent intentChat = new Intent(this, ChatActivity.class);
        if (isGroup)
        {
            intentChat.putExtra(IntentData.GROUPID, groupId);
            intentChat.putExtra(IntentData.GROUPTYPE, groupType);
        }
        else
        {
            intentChat.putExtra(IntentData.ESPACENUMBER, getAddList().get(0));
        }
        startActivity(intentChat);

        UCAPIApp.getApp().popActivity(this);
    }

    private void registerRec()
    {
        //registerReceiver(receiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        
    }

    private void unRegisterRec()
    {
        //unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(CommonUtil.APPTAG, TAG + " | action = " + action);

            if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
                    .equals(action))
            {
                contactAddView.updateSearchContactList(intent);
            }
            else if (CustomBroadcastConst.ACTION_CREATE_GROUP.equals(action))
            {
                closeDialog();

                int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);

                Log.d(CommonUtil.APPTAG, TAG + " | ACTION_CREATE_GROUP | result = "
                        + result);

                if (UCResource.REQUEST_OK == result)
                {
                    BaseResponseData data = (BaseResponseData) intent
                            .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    if (ResponseCode.REQUEST_SUCCESS != data.getStatus())
                    {
                        // 响应错误
                        Toast.makeText(
                                GroupMemberAddActivity.this,
                                "create discuss group filed | ResponseCode = "
                                        + data.getStatus() + "["
                                        + data.getDesc() + "]",
                                Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        // 成功响应
                        ManageGroupResp groupRtn = (ManageGroupResp) data;
                        groupId = groupRtn.getGroupId();
                        groupType = groupRtn.getGroupType();

                        gotoChat(true);
                    }
                }
                else
                {
                    // 请求错误
                    Toast.makeText(GroupMemberAddActivity.this,
                            "create discuss group filed ", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            else if (CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP.equals(action))
            {
                closeDialog();

                int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);

                Log.d(CommonUtil.APPTAG, TAG
                        + " | ACTION_INVITETO_JOIN_GROUP | result = " + result);
                if (UCResource.REQUEST_OK == result)
                {
                    BaseResponseData data = (BaseResponseData) intent
                            .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
                    if (ResponseCode.REQUEST_SUCCESS != data.getStatus())
                    {
                        // 响应错误
                        Toast.makeText(
                                GroupMemberAddActivity.this,
                                "invite group members response filed | ResponseCode = "
                                        + data.getStatus() + "["
                                        + data.getDesc() + "]",
                                Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        // 成功响应
                        UCAPIApp.getApp().popActivity(
                                GroupMemberAddActivity.this);
                    }
                }
                else
                {
                    // 请求错误
                    Toast.makeText(GroupMemberAddActivity.this,
                            "invite group members request filed",
                            Toast.LENGTH_SHORT).show();
                }
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
	public void initializeData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeComposition() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMemberChanged() {
		// TODO Auto-generated method stub
		this.refreshGridView();
	}

}
