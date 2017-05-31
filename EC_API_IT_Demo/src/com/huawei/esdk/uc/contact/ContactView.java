package com.huawei.esdk.uc.contact;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.huawei.contacts.PersonalContact;
import com.huawei.data.ConstGroup;
import com.huawei.data.PersonalTeam;
import com.huawei.data.entity.InstantMessage;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.esdk.uc.utils.NotifyUpdateUi;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.espace.framework.common.ThreadManager;

public class ContactView extends LinearLayout implements OnClickListener,NotifyUpdateUi
{
    private TextView tvTeamTitle;

    private ImageView ivContactTriangle;

    private GridView gvTeams;

    private ListView lvContact;

    private TeamsAdapter teamsAdapter;

    private ContactAdapter contactAdapter;

    private ArrayList<PersonalContact> personalContacts = new ArrayList<PersonalContact>();

    private List<PersonalTeam> personalTeams = new ArrayList<PersonalTeam>();

    private int index;

    private boolean flag = false;

    private Context context;

    private ImFunc.LocalImReceiver localImReceiver;

    private static final int REFRESH_CONTACTVIEW = 0x01;

    private Handler contactHandler;

    public ContactView(Context context)
    {
        this(context, null);
    }



    public ContactView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;

        View view = LayoutInflater.from(context).inflate(R.layout.view_contact,
                null);
        addView(view);

        getContactsTeams();

        tvTeamTitle = (TextView) findViewById(R.id.title);
        ivContactTriangle = (ImageView) findViewById(R.id.contact_triangle_image);

        gvTeams = (GridView) findViewById(R.id.teams);
        teamsAdapter = new TeamsAdapter(context, personalTeams);
        gvTeams.setAdapter(teamsAdapter);

        lvContact = (ListView) view.findViewById(R.id.list_contact);
        contactAdapter = new ContactAdapter(context, personalContacts);
        lvContact.setAdapter(contactAdapter);

        ivContactTriangle.setOnClickListener(this);

        tvTeamTitle.setText(R.string.all_contacts);
        gvTeams.setVisibility(View.GONE);

        setOnItemClickListener();
        
        ThreadManager.getInstance().addToSingleThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				updateContactsList();
			}
		});

        
    }

    private void initHandler()
    {
        contactHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case REFRESH_CONTACTVIEW:
                        contactAdapter.notifyDataSetChanged();
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
//                contactAdapter.notifyDataSetChanged();
                contactHandler.sendEmptyMessage(REFRESH_CONTACTVIEW);
            }

			@Override
			public void onRequestHistoryBack(List<InstantMessage> msgList,
					GetHistoryMessageInfo info) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void refreshDisplayAfterSendMessage(InstantMessage msg) {
				// TODO Auto-generated method stub
				
			}
        };
        // 注册
        ImFunc.getIns().registerBroadcast(localImReceiver);
    }

    private void getContactsTeams()
    {
        personalTeams.clear();

        personalTeams.addAll(ContactFunc.getIns().getAllTeams());

        PersonalTeam allContactsTeam = ContactFunc.getIns() 
                .getAllContactsTeam();
        allContactsTeam.setTeamName(context.getString(R.string.all_contacts));

        personalTeams.add(0, allContactsTeam);
    }

    private void setOnItemClickListener()
    {
        gvTeams.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3)
            {
                if (index != arg2)
                {
                    index = arg2;
                    updateContactsList();
                }
            }
        });
    }

    public void updateContactsList()
    {
    	//获取分组列表，返回List<PersonalTeam>，不甚了解
        getContactsTeams();
        
        //如果上一步取得的personalTeams不为空....
        if (null != personalTeams && personalTeams.size() > 0)
        {
            PersonalTeam team = personalTeams.get(index);
            tvTeamTitle.setText(team.getTeamName());
            personalContacts.clear();
            personalContacts.addAll(team.getContactList());

            if (contactAdapter != null)
            {
                contactAdapter.notifyDataSetChanged();
            }
            if (teamsAdapter != null)
            {
                teamsAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.contact_triangle_image:

                if (flag)
                {
                    ivContactTriangle
                            .setBackgroundResource(R.drawable.contact_triangle_up);
                    gvTeams.setVisibility(View.VISIBLE);
                }
                else
                {
                    ivContactTriangle
                            .setBackgroundResource(R.drawable.contact_triangle_down);
                    gvTeams.setVisibility(View.GONE);
                }
                flag = !flag;

                break;

            default:
                break;
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        registerIM();
        UnreadMessageManager.getIns().postUnreadNumNotify();
        initHandler();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        ImFunc.getIns().unRegisterBroadcast(localImReceiver);
    }

    @Override
    public void notifyUpdate()
    {
        if(contactAdapter != null)
        {
            contactAdapter.notifyDataSetChanged();
        }

    }
}
