package com.huawei.esdk.uc.contact;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.LocalBroadcast.ReceiveData;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.AddFriendResp;
import com.huawei.data.ExecuteResult;
import com.huawei.data.SearchContactsResp;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.InstantMessage;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.utils.StringUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class SearchContactView extends LinearLayout implements OnClickListener
{

    private EditText edCondition;

    private ImageView ivClear;

    private Button btnSearch;

    private ListView lvContact;

    private ContactAdapter contactAdapter;

    private ArrayList<PersonalContact> personalContacts = new ArrayList<PersonalContact>();

    private int requsetId;

    private ImFunc.LocalImReceiver localImReceiver;
    
    private IntentFilter filter;

    /**
     * 注册广播名称
     */
    private String[] actionNames;

    private static final int ADD_FRIEND_SUCCESS = 0x0;

    /*
     * 更新UI界面
     */
    private final Handler myHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            // 刷新UI前都更新一下联系人
            // currentContact =
            // contactDetailLogic.getContact(currentContact.getEspaceNumber());
            switch (msg.what)
            {
   
            case ADD_FRIEND_SUCCESS:
                // 添加联系人成功回调
                handleAddFriendSuccess((AddFriendResp) msg.obj);
                break;
            }
        }
    };
    


    public SearchContactView(Context context)
    {
        this(context, null);
    }

    public SearchContactView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.view_search,
                null);
        addView(view);

        edCondition = (EditText) view.findViewById(R.id.condition);
        ivClear = (ImageView) findViewById(R.id.clear);
        btnSearch = (Button) view.findViewById(R.id.search);
        
        
        lvContact = (ListView) view.findViewById(R.id.list_contact);
        contactAdapter = new ContactAdapter(context, personalContacts);
        contactAdapter.setIsFromSearch(true);
        lvContact.setAdapter(contactAdapter);

        ivClear.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
     
        // 2015/8/13李越添加，为使创建会议时有本地联系人
        //初始化personalContacts
        getAllContact();
        // 添加结束
        
        edCondition.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (s.length() == 0)
                {
                    ivClear.setVisibility(View.GONE);
                    personalContacts.clear();
                    contactAdapter.notifyDataSetChanged();
                }
                else
                {
                    ivClear.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    
    private void getAllContact()
    {
        personalContacts.addAll(ContactFunc.getIns().getAllContacts());
        contactAdapter.notifyDataSetChanged();
    }
    public void setIsFromConf(boolean isFromConf)
    {
        contactAdapter.setIsFromConf(isFromConf);
    }

    public void setListItemClickListener(OnItemClickListener listener)
    {
        lvContact.setOnItemClickListener(listener);
    }

    public void updateSearchContactList(Intent intent)
    {
        int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
        if (UCResource.REQUEST_OK != result)
        {
            return;
        }
        BaseResponseData data = (BaseResponseData) intent
                .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

        int id = data.getBaseId();

        if (data != null && data.getBaseId() == requsetId
                && data instanceof SearchContactsResp)
        {
            personalContacts.clear();

            SearchContactsResp contactsResp = (SearchContactsResp) data;

            if (contactsResp.getContacts() != null)
            {
                personalContacts.addAll(contactsResp.getContacts());
                contactAdapter.notifyDataSetChanged();
                if (contactsResp.getContacts().size() <= 0)
                {
                    Toast.makeText(LocContext.getContext(), "用户不存在", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        case R.id.clear:
            edCondition.setText("");
            break;
        case R.id.search:
            String condition = edCondition.getText().toString();
            if (TextUtils.isEmpty(condition))
            {
                break;
            }

            ExecuteResult result = ContactFunc.getIns()
                    .searchContact(condition);
            if (null != result && result.isResult())
            {
                requsetId = result.getId();

            }
            break;

        default:
            break;
        }
    }

    /**
     * 添加联系人回调
     */
    private void handleAddFriendSuccess(AddFriendResp data)
    {
        if (data == null)
        {
            Logger.debug(CommonUtil.APPTAG, "handleAddFriendSuccess receiveData is null");
            return;
        }

        Toast.makeText(this.getContext(), "add friend response：" + data.getAnswer(), Toast.LENGTH_SHORT).show();
        
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
                contactAdapter.notifyDataSetChanged();
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

    public void updateContacts()
    {
        if (contactAdapter != null)
        {
            contactAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        registerIM(); 

        
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
        
        LocalBroadcastManager.getInstance(this.getContext()).unregisterReceiver(receiver);
        
    }

    /**
     * 处理增加联系人广播消息
     * 
     * @param resp
     *            data
     */
    private void handleAddFriendResponse(ReceiveData resp)
    {
        if ((null == resp) || (null == resp.data))
        {
            return;
        }
        BaseResponseData data = resp.data;
        int result = resp.result;
        if (UCResource.REQUEST_OK == result)
        {
            AddFriendResp afr = null;
            if (data instanceof AddFriendResp)
            {
                afr = (AddFriendResp) data;
            }
            else
            {
                return;
            }
            if (ResponseCode.REQUEST_SUCCESS.equals(afr.getStatus()))
            {
                sendMessage(ADD_FRIEND_SUCCESS, resp);
            }
        }
    }

    private void sendMessage(int what, Object object)
    {
        if (object != null)
        {
            Message msg = new Message();
            msg.what = what;
            msg.obj = object;
            myHandler.sendMessage(msg);
        }
        else
        {
            myHandler.sendEmptyMessage(what);
        }
    }

//    @Override
//    public void onReceive(String action, BaseData data)
//    {
//        if (!(data instanceof ReceiveData))
//        {
//            return;
//        }
//        ReceiveData resp = (ReceiveData) data;
//
//        String action = intent.getAction();
//        if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
//                .equals(action))
//        {
//            Log.d(CommonUtil.APPTAG, "ConferenceAddMemberActivity"
//                    + " | ACTION_SEARCE_CONTACT_RESPONSE ");
//
//            searchContactView.updateSearchContactList(intent);
//        }
//        
//        
//    }
    
    
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
                    .equals(action))
            {
                Log.d(CommonUtil.APPTAG, "SearchContactView"
                        + " | ACTION_SEARCE_CONTACT_RESPONSE ");

                updateSearchContactList(intent);
            }
            
            else if (CustomBroadcastConst.ACTION_ADD_FRIEND_RESPONSE
                    .equals(action))
            {
                Log.d(CommonUtil.APPTAG, "SearchContactView"
                        + " | ACTION_ADD_FRIEND_RESPONSE ");
                int result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT, 0);
                if (UCResource.REQUEST_OK != result)
                {
                    return;
                }
                BaseResponseData data = (BaseResponseData) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

               

                if (data != null &&  data instanceof AddFriendResp)
                {
                    AddFriendResp afr = (AddFriendResp)data ;
                    if (ResponseCode.REQUEST_SUCCESS.equals(afr.getStatus()))
                    {
                        sendMessage(ADD_FRIEND_SUCCESS, afr);
                    }
                }
            }
        }
    };

}
