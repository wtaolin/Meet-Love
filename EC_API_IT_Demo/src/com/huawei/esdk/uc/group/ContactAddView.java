package com.huawei.esdk.uc.group;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.ExecuteResult;
import com.huawei.data.SearchContactsResp;
import com.huawei.data.base.BaseResponseData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.contact.MemberAddHandler;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.utils.StringUtil;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class ContactAddView extends LinearLayout implements OnClickListener
{
    private EditText edCondition;

    private ImageView ivClear;

    private Button btnSearch;

    private ListView lvContact;

    private ListView lvSearchContact;

    private ContactAddAdapter contactAdapter;

    private List<PersonalContact> personalContacts = new ArrayList<PersonalContact>();
    
    private List<PersonalContact> searchContacts = new ArrayList<PersonalContact>();

    private List<PersonalContact> addContacts = new ArrayList<PersonalContact>();
    
    private List<PersonalContact> memberContacts = new ArrayList<PersonalContact>();
    

    private ContactAddAdapter searchContactAdapter;



    private int requsetId;
    
   // private boolean flag = false;
    
    private MemberAddHandler memberAddActivity;

    //private GroupMemberAddActivity memberAddActivity;
//    
//    /**已经在会议中的成员列表,李越添加*/
//    private List<String> espaceNumber = new ArrayList<String>();
//    
//    /** 已经在讨论组中的成员列表 **/
//    private List<ConstGroupContact> groupContacts = new ArrayList<ConstGroupContact>();
   
    public ContactAddView(Context context)
    {
        this(context, null);
    }

    public ContactAddView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
		memberAddActivity = (MemberAddHandler) context;
		View view = LayoutInflater.from(context).inflate(
                R.layout.contact_member_add, null);
        
        //将view加入此布局，李越
        addView(view);

        edCondition = (EditText) view.findViewById(R.id.condition);
        ivClear = (ImageView) findViewById(R.id.clear);
        btnSearch = (Button) view.findViewById(R.id.search);

        lvContact = (ListView) view.findViewById(R.id.list_contact);
        lvSearchContact = (ListView) view
                .findViewById(R.id.list_search_contact);

        contactAdapter = new ContactAddAdapter(context);
        contactAdapter.setContactList(personalContacts, memberContacts, addContacts);
        lvContact.setAdapter(contactAdapter);

        searchContactAdapter = new ContactAddAdapter(context);
        
        searchContactAdapter.setContactList(searchContacts, memberContacts, addContacts);
        lvSearchContact.setAdapter(searchContactAdapter);

        ivClear.setOnClickListener(this);
        btnSearch.setOnClickListener(this);

        setOnListener();

        getAllContacts();

    }

    public void setMemberContacts(List<PersonalContact> memberContacts)
    {
//        this.groupContacts = groupContacts;
//        contactAdapter.setGroupConstList(groupContacts);
//        searchContactAdapter.setGroupConstList(groupContacts);
    	this.memberContacts.clear();
    	 
    	if(memberContacts == null)
    	{
    		return;
    	}

        this.memberContacts.addAll(memberContacts);
        
        contactAdapter.notifyDataSetChanged();
        searchContactAdapter.notifyDataSetChanged();
    }

    private void setOnListener()
    {
        lvContact.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3)
            {
                PersonalContact personalContact = personalContacts.get(arg2);
                if (isCanAdd(personalContact))
                {
                    addContacts.add(personalContact);
                    contactAdapter.notifyDataSetChanged();
                    searchContactAdapter.notifyDataSetChanged();

                    memberAddActivity.onMemberChanged();
                }
            }
        });

        lvSearchContact.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3)
            {
                PersonalContact personalContact = searchContacts.get(arg2);
                if (isCanAdd(personalContact))
                {
                    addContacts.add(personalContact);
                    contactAdapter.notifyDataSetChanged();
                    searchContactAdapter.notifyDataSetChanged();
                    memberAddActivity.onMemberChanged();
                }
            }
        });

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
                    searchContacts.clear();
                    searchContactAdapter.notifyDataSetChanged();
                    lvSearchContact.setVisibility(View.GONE);
                    lvContact.setVisibility(View.VISIBLE);
                }
                else
                {
                    ivClear.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void getAllContacts()
    {
        personalContacts.addAll(ContactFunc.getIns().getAllContacts());
        contactAdapter.notifyDataSetChanged();
    }

    private boolean isCanAdd(PersonalContact item)
    {
        if (item.getEspaceNumber().equals(
                CommonVariables.getIns().getUserAccount()))
        {
            return false;
        }
        if (isAdd(item))
        {
            return false;
        }
        if (isGroupMember(item))
        {
            return false;
        }
        return true;
    }

    private boolean isGroupMember(PersonalContact item)
    {
        for (PersonalContact contact : memberContacts)
        {
            if (contact.getEspaceNumber().equals(item.getEspaceNumber()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isAdd(PersonalContact item)
    {
        for (PersonalContact contact : addContacts)
        {
            if (contact.getEspaceNumber().equals(item.getEspaceNumber()))
            {
                return true;
            }
        }
        return false;
    }

    public List<PersonalContact> getAddContacts()
    {
        return addContacts;
    }

    public void removeContact(PersonalContact contact)
    {
    	for(int i = addContacts.size() -1 ; i>=0; i--)
    	{
    		if(contact.getEspaceNumber().equals(addContacts.get(i).getEspaceNumber()))
    		{
    			 addContacts.remove(i);
    			 break;
    		}
    	}
    	
       
        contactAdapter.notifyDataSetChanged();
        searchContactAdapter.notifyDataSetChanged();
        memberAddActivity.onMemberChanged();
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

        if (data != null && data.getBaseId() == requsetId
                && data instanceof SearchContactsResp)
        {
            searchContacts.clear();

            SearchContactsResp contactsResp = (SearchContactsResp) data;

            if (contactsResp.getContacts() != null)
            {
                searchContacts.addAll(contactsResp.getContacts());
                searchContactAdapter.notifyDataSetChanged();
                if (contactsResp.getContacts().size() <= 0)
                {
                    Toast.makeText(LocContext.getContext(), "用户不存在", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
//    /**设置搜索结果的点击事件，李越添加*/
//    public void setListItemClickListener(OnItemClickListener listener)
//    {
//        lvContact.setOnItemClickListener(listener);
//    }

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
                if (!TextUtils.isEmpty(condition))
                {
                    ExecuteResult result = ContactFunc.getIns().searchContact(
                            condition);
                    if (null != result && result.isResult())
                    {
                        requsetId = result.getId();
                        lvContact.setVisibility(View.GONE);
                        lvSearchContact.setVisibility(View.VISIBLE);
                    }
                }
                break;
            default:
                break;
        }
    }


}
