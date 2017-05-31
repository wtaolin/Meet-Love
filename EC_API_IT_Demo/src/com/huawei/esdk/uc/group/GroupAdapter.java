package com.huawei.esdk.uc.group;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.data.ConstGroup;
import com.huawei.data.ConstGroupContact;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.utils.UnreadMessageManager;

public class GroupAdapter extends BaseAdapter
{

    private LayoutInflater mInflater;

    private List<ConstGroup> constGroups = new ArrayList<ConstGroup>();

    private Context context;

    public GroupAdapter(Context context, List<ConstGroup> constGroups)
    {

        this.context = context;

        this.constGroups = constGroups;

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        return constGroups.size();
    }

    @Override
    public Object getItem(int position)
    {
        return constGroups.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        convertView = mInflater.inflate(R.layout.group_list_item, null);

        ImageView ivHead = (ImageView) convertView.findViewById(R.id.head);
        TextView tvName = (TextView) convertView.findViewById(R.id.name);
        TextView tvUnRead = (TextView) convertView.findViewById(R.id.unread);

        final ConstGroup group = constGroups.get(position);

//        List<ConstGroupContact> memberlist = GroupFunc.getIns()
//                .getGroupMembers(group.getGroupId(), group.getGroupType());
//        if (null == memberlist || memberlist.size() <= 0)
//        {
            tvName.setText(group.getName());
//        }
//        else
//        {
//            tvName.setText(group.getName() + " (" + memberlist.size() + ")");
//        }

//        int unRead = ImFunc.getIns().getUnReadMsgCount(group.getGroupId(),
//                RecentChatContact.DISCUSSIONCHATTER);
        int unRead = UnreadMessageManager.getIns().getUnreadMsgNumByGroupId(group.getGroupId());

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

        ivHead.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(context, GroupDetailActivity.class);
                intent.putExtra(IntentData.GROUPID, group.getGroupId());
                intent.putExtra(IntentData.GROUPTYPE, group.getGroupType());
                context.startActivity(intent);
            }
        });

        // HeadPhotoUtil.getInstance().loadHeadPhoto(contact, ivHead);

        return convertView;
    }

}
