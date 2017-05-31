package com.huawei.esdk.uc.group;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.contacts.PersonalContact;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;

public class MemberAddAdapter extends BaseAdapter
{
    private List<PersonalContact> memberList;

    private LayoutInflater inflater;

    private OnMemberClickListener onMemberClickListener;
    
    private ContactHeadFetcher headFetcher;

    public MemberAddAdapter(Context context, List<PersonalContact> contactList)
    {
        inflater = LayoutInflater.from(context);
        memberList = contactList;
        
        headFetcher = new ContactHeadFetcher(context);
    }

    @Override
    public int getCount()
    {
        return memberList.size() + 1;
    }

    @Override
    public Object getItem(int position)
    {
        return memberList.get(position + 1);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        MemberViewHolder viewHolder;

        if (null == convertView)
        {
            viewHolder = new MemberViewHolder();
            convertView = inflater.inflate(R.layout.group_member_item, null);
            viewHolder.imageView = (ImageView) convertView
                    .findViewById(R.id.contact_head);
            viewHolder.textView = (TextView) convertView
                    .findViewById(R.id.contact_name);
            viewHolder.cancelImg = (ImageView) convertView
                    .findViewById(R.id.cancel_img);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (MemberViewHolder) convertView.getTag();
        }

        if (position < memberList.size())
        {
            PersonalContact contact = memberList.get(position);
            viewHolder.textView.setText(ContactFunc.getIns().getDisplayName(
                    contact));
          //加载头像用以下方法 by lwx302895
//            HeadPhotoUtil.getInstance().loadHeadPhoto(contact,
//                    viewHolder.imageView, false);
            headFetcher.loadHead(contact, viewHolder.imageView, false);
            
            viewHolder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            viewHolder.cancelImg.setVisibility(View.VISIBLE);
        }
        else
        {
            viewHolder.textView.setText(null);
            viewHolder.imageView.setImageResource(R.drawable.group_add_enable);
            viewHolder.cancelImg.setVisibility(View.GONE);
        }
        convertView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (onMemberClickListener != null)
                {
                    onMemberClickListener.onMemberClick(position);
                }
            }
        });

        return convertView;
    }

    public void setOnMemberClickListener(OnMemberClickListener lsn)
    {
        this.onMemberClickListener = lsn;
    }

    private class MemberViewHolder
    {
        public ImageView cancelImg;

        public TextView textView;

        public ImageView imageView;
    }

    public interface OnMemberClickListener
    {
        void onMemberClick(int position);
    }
}
