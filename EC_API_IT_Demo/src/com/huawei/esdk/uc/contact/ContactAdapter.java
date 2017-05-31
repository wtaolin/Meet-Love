package com.huawei.esdk.uc.contact;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huawei.common.CommonVariables;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.ExecuteResult;
import com.huawei.data.PersonalTeam;
import com.huawei.esdk.uc.FakeVideoActivity;
import com.huawei.esdk.uc.InteractVideoActivity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.service.ServiceProxy;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends BaseAdapter
{

    private Context context;

    private LayoutInflater mInflater;

    private ArrayList<PersonalContact> personalContacts = new ArrayList<PersonalContact>();
    
    private boolean isFromConf = false;
    
    private boolean isFromSearch = false;
    
    private ContactHeadFetcher headFetcher;

    public ContactAdapter(Context context,
            ArrayList<PersonalContact> personalContacts)
    {
        this.context = context;

        this.personalContacts = personalContacts;

        mInflater = LayoutInflater.from(context);
        headFetcher = new ContactHeadFetcher(this.context);
    }
    
    public void setIsFromConf(boolean isFromConf)
    {
        this.isFromConf = isFromConf;
        notifyDataSetChanged();
    }
    
    
    public void setIsFromSearch(boolean isSearch)
    {
        this.isFromSearch = isSearch;
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return personalContacts.size();
    }

    @Override
    public Object getItem(int position)
    {
        return personalContacts.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {

        convertView = mInflater.inflate(R.layout.contact_list_item, null);

        ImageView ivHead = (ImageView) convertView.findViewById(R.id.head);
        ImageView ivStatus = (ImageView) convertView
                .findViewById(R.id.status_image);
        TextView tvName = (TextView) convertView.findViewById(R.id.name);
        ImageView ivCall = (ImageView) convertView.findViewById(R.id.call);
        ImageView ivMsg = (ImageView) convertView.findViewById(R.id.msg);
        TextView tvUnRead = (TextView) convertView.findViewById(R.id.unread);
        ImageView ivAdd = (ImageView) convertView.findViewById(R.id.add);
        RelativeLayout rlContactDetail = (RelativeLayout)convertView.findViewById(R.id.contact_detail);

        final PersonalContact contact = personalContacts.get(position);
        rlContactDetail.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent();
                intent.setClass(context, ContactDetailActivity.class);
                intent.putExtra("CONTACT_DETAIL", contact);
                context.startActivity(intent);
            }
        });
        
        ivMsg.setVisibility(View.GONE);
        ivCall.setVisibility(View.GONE);
        ivAdd.setVisibility(View.GONE);

        if (contact.getEspaceNumber().equals(
                CommonVariables.getIns().getUserAccount()))
        {
            ivMsg.setVisibility(View.GONE);
            ivCall.setVisibility(View.GONE);
            ivAdd.setVisibility(View.GONE);
            
        }
        else 
        {
            if(isFromConf)
            {
                ivMsg.setVisibility(View.GONE);
                ivCall.setVisibility(View.GONE);
                ivAdd.setVisibility(View.VISIBLE);
            }
            else
            {
                ivMsg.setVisibility(View.VISIBLE);
                ivCall.setVisibility(View.VISIBLE);
                
                
                ivMsg.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
//                        Intent intent = new Intent(context, ChatActivity.class);
//                        intent.putExtra(IntentData.ESPACENUMBER, contact);
//                        context.startActivity(intent);
                        Intent in = new Intent(context.getApplicationContext(), InteractVideoActivity.class);
                        context.startActivity(in);
                    }
                });
                
                ivCall.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
//                        if (VoipFunc.STATUS_INIT == VoipFunc.getIns().getVoipStatus())
//                        {
//                            VoipFunc.getIns().makeCall(contact.getBinderNumber());
//                            //为获取头像方便 lwx302895
//                            VoipFunc.getIns().setCallPersonal(contact);
//
//                        }
//                        else
//                        {
//                            Toast.makeText(context, R.string.tip_taking, Toast.LENGTH_SHORT).show();
//                        }
                        Intent in = new Intent(context.getApplicationContext(), FakeVideoActivity.class);
                        context.startActivity(in);
                    }
                });
                
            
                
                //添加按钮修改为添加联系人
                
                if(isFromSearch)
                    
                {
                    ivAdd.setVisibility(View.VISIBLE);
                    
                    ivAdd.setOnClickListener(new OnClickListener()
                    {

                        @Override
                        public void onClick(View v)
                        {
                            addFriend(contact);
                        }
                    });
                }
                
                else
                {
                    ivAdd.setVisibility(View.GONE);
                }
            }
        }

        tvName.setText(ContactFunc.getIns().getDisplayName(contact));
        
        int status = contact.getStatus(false);
        setUserStatus(ivStatus, status);

//        int unRead = ImFunc.getIns().getUnReadMsgCount(
//                contact.getEspaceNumber(), RecentChatContact.ESPACECHATTER);
		
		//通过帐号信息未读消息容器中取出未读消息数目
        int unRead = UnreadMessageManager.getIns().getUnreadMsgNumByAccount(contact.getEspaceNumber());

        if (unRead <= 0)
        {
            tvUnRead.setVisibility(View.GONE);
        }
        else if (unRead >= 99)
        {
            tvUnRead.setVisibility(View.VISIBLE);
            tvUnRead.setBackgroundResource(R.drawable.red_shape_max);
        }
        else
        {
            tvUnRead.setVisibility(View.VISIBLE);
            tvUnRead.setBackgroundResource(R.drawable.red_shape);
            tvUnRead.setText(String.valueOf(unRead));
        }
        
        //这里开始加载头像
//        HeadPhotoUtil.getInstance().loadHeadPhoto(contact, ivHead, false);
        headFetcher.loadHead(contact, ivHead, false);

        return convertView;
    }

    private void setUserStatus(ImageView ivStatus, int status)
    {
        
        switch (status)
        {
            case ContactClientStatus.ON_LINE:

                ivStatus.setImageResource(R.drawable.recent_online_small);
                break;

            case ContactClientStatus.BUSY:

                ivStatus.setImageResource(R.drawable.recent_busy_small);
                break;

            case ContactClientStatus.XA:

                ivStatus.setImageResource(R.drawable.recent_away_small);
                break;

            case ContactClientStatus.AWAY:

                ivStatus.setImageResource(R.drawable.recent_offline_small);
                break;

            default:

                ivStatus.setImageResource(R.drawable.recent_offline_small);
                break;
        }
    }
    
    
    /**
     * 添加联系人好友
     *
     * @param contact 是否请求访问同事圈,请求则不通知
     */
    public void addFriend(PersonalContact contact)
    {
        if(contact == null)
        {
            return;
        }
        List<PersonalTeam> serviceTeams = ContactLogic.getIns().getTeams();
        if(serviceTeams.size() == 0)
        {
            return;
        }
    
        ServiceProxy mService = UCAPIApp.getApp().getService();

        ExecuteResult r = null;

        if (mService != null)
        {
            r = mService.addFriend(contact.getEspaceNumber(), null, serviceTeams.get(0).getTeamId(), false, false);
           
        }
       
    }

}
