package com.huawei.esdk.uc.conf;

import java.util.ArrayList;
import java.util.List;

import com.huawei.conference.CtcMemberEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;
import com.huawei.utils.StringUtil;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ConfManageItemAdapter extends BaseAdapter
{

    private LayoutInflater mInflater;

    private List<ConferenceMemberEntity> members = new ArrayList<ConferenceMemberEntity>();
    
    private ContactHeadFetcher headFetcher;

    public ConfManageItemAdapter(Context context)
    {
        mInflater = LayoutInflater.from(context);
        headFetcher = new ContactHeadFetcher(context);

    }

    public void setData(List<ConferenceMemberEntity> members)
    {
        if (!members.isEmpty())
        {
            this.members = members;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return members.size();
    }

    @Override
    public Object getItem(int position)
    {
        return members.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        convertView = mInflater.inflate(R.layout.conf_manage_list_item, null);
        TextView tvName = (TextView) convertView.findViewById(R.id.name);
        TextView tvNumber = (TextView) convertView.findViewById(R.id.number);
        ImageView ivStatus = (ImageView) convertView.findViewById(R.id.stauts);
        
        /**
         * 会议头像
         */
        ImageView ivHead = (ImageView) convertView.findViewById(R.id.head);

        ConferenceMemberEntity member = members.get(position);

        tvName.setText(member.getConfMemEspaceNumber());
        tvNumber.setText(member.getNumber());
        
        if (!TextUtils.isEmpty(member.getConfMemEspaceNumber()) && ivHead != null)
        {
        	//换成以下加载头像方式 bylwx302895
//        	HeadPhotoUtil.getInstance().loadHeadPhoto(member.getEspaceNumber(), ivHead);
        	headFetcher.loadHead(member.getConfMemEspaceNumber(), ivHead);
		}

        if (member.getStatus() == CtcMemberEntity.STATUS_MUTE)
        {
            ivStatus.setBackgroundResource(R.drawable.conf_calling_mute);
        }
        else if (member.getStatus() == CtcMemberEntity.STATUS_JOIN_CONF_SUCCESS)
        {
            ivStatus.setBackgroundResource(R.drawable.conf_calling_success);
        }
        else if (member.getStatus() == CtcMemberEntity.STATUS_LEAVE_CONF_SUCCESS)
        {
            ivStatus.setBackgroundResource(R.drawable.conf_calling_leave);
        }
        else 
        {
            ivStatus.setBackgroundResource(R.drawable.conf_calling_3);
        }

        return convertView;
    }

}
