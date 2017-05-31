package com.huawei.esdk.uc.conf;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.conference.CtcMemberEntity;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.data.entity.People;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.esdk.uc.contact.MemberAddHandler;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.group.ContactAddView;
import com.huawei.esdk.uc.widget.SlippingViewGroup;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ConferenceAddMemberActivity extends Activity implements MemberAddHandler
{

    private Button btnDone;

    private LinearLayout llMember;

    private SlippingViewGroup viewGroup;

    private List<String> inConfContacts = new ArrayList<String>();

    private List<String> addConfContacts = new ArrayList<String>();

    private List<ConferenceMemberEntity> inConfMembers = new ArrayList<ConferenceMemberEntity>();

    private ArrayList<ConferenceMemberEntity> addConfMembers = new ArrayList<ConferenceMemberEntity>();

    private String confId;

    private IntentFilter filter;

    private ContactAddView contactAddView;
    
    /**已经加入会议的人，李越添加，方便ContactAddView使用*/
    private List<PersonalContact> memberContacts = new ArrayList<PersonalContact>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add_member);

        /**
         * 李越添加，为接收广播，获取已经存在的会议成员
         * */
        filter = new IntentFilter();
        filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
        filter.addAction(CustomBroadcastConst.ACTION_ADD_FRIEND_RESPONSE);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        //添加结束
        
//        filter = new IntentFilter();
//        filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
//
//        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        
        
        //registerReceiver(receiver, filter);

        initView();
    }
    
    /**广播接收搜索结果，李越添加*/
    private BroadcastReceiver receiver= new BroadcastReceiver()
    {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

            if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
                    .equals(action))
            {
                contactAddView.updateSearchContactList(intent);
            }
		}
    	
    };
    
    private void initView()
    {
        btnDone = (Button) findViewById(R.id.done);
        llMember = (LinearLayout) findViewById(R.id.members);
        viewGroup = (SlippingViewGroup) findViewById(R.id.slippingGroup);
        
        /**
         * 李越修改，使用ContactAddView.注释掉的是原版
         * */
        /*searchContactView = new SearchContactView(this);
        searchContactView.setIsFromConf(true);
        searchContactView.setListItemClickListener(itemClickListener);
        viewGroup.addView(searchContactView);*/
        
        contactAddView = new ContactAddView(this);
//        contactAddView.setListItemClickListener(itemClickListener);
        viewGroup.addView(contactAddView);
       //修改结束
        
        
        btnDone.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (confId != null)
                {
                    List<CtcMemberEntity> ctcMemberEntities = ConferenceDataHandler
                            .getIns().transToCtcMember(addConfMembers);
                    ConferenceFunc.getIns().requestAddMember(confId,
                            ctcMemberEntities);
                    setResult(RESULT_OK);
                }
                else
                {
                    Intent data = new Intent();
                    data.putExtra("member", addConfMembers);
                    setResult(RESULT_OK, data);
                }
                finish();
            }
        });
        
        //获取会议成员，李越
        confId = getIntent().getStringExtra("confId");
        if (confId != null)
        {
            inConfMembers = (List<ConferenceMemberEntity>) getIntent().getSerializableExtra("members");
            if (!inConfMembers.isEmpty())
            {
                for (ConferenceMemberEntity member : inConfMembers)
                {
                	//已经加入会议的人，李越添加
                	memberContacts.add(member.getPerson());
                	contactAddView.setMemberContacts(memberContacts);
                	//添加完毕
                	
                    inConfContacts.add(member.getConfMemEspaceNumber());
                    addConfContacts.add(member.getConfMemEspaceNumber());
                    addMemberView(member);
                }
                
                /**传入ContactAddView，李越添加*/
                //contactAddView.setEspaceNumber(espaceNumber);
                contactAddView.setMemberContacts(memberContacts);
            }
        }
        
        
        
    }


//	private OnItemClickListener itemClickListener = new OnItemClickListener()
//    {
//
//        @Override
//        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
//                long arg3)
//        {
//            PersonalContact contact = (PersonalContact) arg0.getAdapter()
//                    .getItem(arg2);
//            if (!contact.getEspaceNumber().equals(
//                    CommonVariables.getIns().getUserAccount()))
//            {
//                if (!addConfContacts.contains(contact.getEspaceNumber()))
//                {
//                	//获取加入会议的人员，给contacAddtview，李越添加
//                	memberContacts.add(contact);
//                	contactAddView.setMemberContacts(memberContacts);
//                	
//                    addConfContacts.add(contact.getEspaceNumber());
//                    ConferenceMemberEntity member = getConfMemberEntity(contact);
//                    addConfMembers.add(member);
//                    addMemberView(member);
//                }
//            }
//        }
//    };

    

    
    private void addMemberView(final ConferenceMemberEntity member)
    {
        final View child = getLayoutInflater().inflate(
                R.layout.create_member_item, null);
        child.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        TextView name = (TextView) child.findViewById(R.id.name);
        name.setText(member.getConfMemEspaceNumber());
        ImageView operate = (ImageView) child.findViewById(R.id.operate);
        operate.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                // TODO Auto-generated method stub
                if (!inConfContacts.contains(member.getConfMemEspaceNumber()))
                {
                    llMember.removeView(child);

                    addConfContacts.remove(member.getConfMemEspaceNumber());
                    addConfMembers.remove(member);
                    
                    //传入ContactAddView
                    memberContacts.remove(member.getPerson());
                    contactAddView.setMemberContacts(memberContacts);
                    contactAddView.removeContact(member.getPerson());
                }
            }
        });
        llMember.addView(child);
    }

    public ConferenceMemberEntity getConfMemberEntity(PersonalContact contact)
    {
        String account = CommonVariables.getIns().getUserAccount();

        PersonalContact pContact = ContactLogic.getIns().getMyContact();
        String number = SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb();
        String name = ContactFunc.getIns().getDisplayName(pContact);

        People p = new People(account, null, null);

        ConferenceMemberEntity mem = new ConferenceMemberEntity(p, name, number);

        mem.setAccount(contact.getEspaceNumber());
        mem.setConfMemEspaceNumber(contact.getEspaceNumber());
        mem.setNumber(contact.getBinderNumber());
        mem.setDisplayName(contact.getDisplayName());
        mem.setEmail(contact.getEmail());
        mem.setRole(CtcMemberEntity.ROLE_MEMBER);
        return mem;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // searchContactView.registerContactBroadcast();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        //unregisterReceiver(receiver);
    }

//    private BroadcastReceiver receiver = new BroadcastReceiver()
//    {
//        @Override
//        public void onReceive(Context context, Intent intent)
//        {
//            String action = intent.getAction();
//            if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
//                    .equals(action))
//            {
//                Log.d(CommonUtil.APPTAG, "ConferenceAddMemberActivity"
//                        + " | ACTION_SEARCE_CONTACT_RESPONSE ");
//
//                searchContactView.updateSearchContactList(intent);
//            }
//        }
//    };
    
	@Override
	public void onMemberChanged() {
		// TODO Auto-generated method stub
		this.refreshMemberView();
		
		
         
	}

	private void refreshMemberView() {
		// TODO Auto-generated method stub
		
		List<PersonalContact> contacts = contactAddView.getAddContacts();//这里获取新加入的成员
		
		for (PersonalContact contact : contacts) {
			 if (!contact.getEspaceNumber().equals(
		             CommonVariables.getIns().getUserAccount()))
		     {
		         if (!addConfContacts.contains(contact.getEspaceNumber()))
		         {
		         	//获取新加入会议的人员，给contacAddtview，李越添加
//		         	memberContacts.add(contact);
//		         	contactAddView.setMemberContacts(memberContacts);//有问题，这里应该是新加入的
		         	
		            addConfContacts.add(contact.getEspaceNumber());
		            ConferenceMemberEntity member = getConfMemberEntity(contact);
		             addConfMembers.add(member);
		             addMemberView(member);
		         }
		     }
		}
		
//		 PersonalContact contact = (PersonalContact) arg0.getAdapter()
//	             .getItem(arg2);
//	     if (!contact.getEspaceNumber().equals(
//	             CommonVariables.getIns().getUserAccount()))
//	     {
//	         if (!addConfContacts.contains(contact.getEspaceNumber()))
//	         {
//	         	//获取加入会议的人员，给contacAddtview，李越添加
//	         	memberContacts.add(contact);
//	         	contactAddView.setMemberContacts(memberContacts);
//	         	
//	             addConfContacts.add(contact.getEspaceNumber());
//	             ConferenceMemberEntity member = getConfMemberEntity(contact);
//	             addConfMembers.add(member);
//	             addMemberView(member);
//	         }
//	     }
	     
	}

}
