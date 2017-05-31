package com.huawei.esdk.uc.group;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.common.CommonVariables;
import com.huawei.contacts.PersonalContact;
import com.huawei.data.ConstGroupContact;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;

/**
 * 讨论组
 * 
 * @author l00170942
 * @since eSpace Mobile V200R001C50 2014/5/20 Copyright (C) 2008-2010
 *        华为技术有限公司(Huawei Tech.Co.,Ltd)
 */
public class DiscussionMemberAdpater extends BaseAdapter
{
    private List<ConstGroupContact> memberList;

    private String owner;

    private LayoutInflater inflater;

    private boolean deleteMode;

    private OnMemberClickListener onMemberClickListener;

    private int capacity;

    private boolean discussionFlag = false;
    
    private ContactHeadFetcher headFetcher;

    public DiscussionMemberAdpater(Context context,
            List<ConstGroupContact> contactList, String owner, int capacity)
    {
        inflater = LayoutInflater.from(context);
        memberList = contactList;
        this.owner = owner;
        this.capacity = capacity;
        
        headFetcher = new ContactHeadFetcher(context);
    }

    @Override
    public int getCount()
    {
        int size = memberList.size();

        if (!discussionFlag)
        {
            return size;
        }
        // 当成员数量已经达到容量上限，不再出现加号；这种场景下，如果是管理员，出现减号
        if (isOwner())
        {
            if (size < capacity)
            {
                return size + 2;
            }
            else
            {
                return size + 1;
            }
        }
        else
        {
            if (size < capacity)
            {
                return size + 1;
            }
            else
            {
                return size;
            }

        }
    }

    @Override
    public Object getItem(int position)
    {
        return memberList.get(position);
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
            viewHolder.deleteImg = (ImageView) convertView
                    .findViewById(R.id.delete_img);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (MemberViewHolder) convertView.getTag();
        }

        viewHolder.deleteImg.setVisibility(View.GONE);
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

        if (position < memberList.size())
        {
            ConstGroupContact contact = memberList.get(position);
            PersonalContact pContact = ContactFunc.getIns()
                    .getContactByAccount(contact.getEspaceNumber());
            if (pContact != null)
            {
                viewHolder.textView.setText(ContactFunc.getIns()
                        .getDisplayName(pContact));
            }
            else
            {
                viewHolder.textView.setText(ContactFunc.getIns()
                        .getDisplayName(contact));
            }

            //加载头像用以下方法 by lwx302895
//            HeadPhotoUtil.getInstance().loadHeadPhoto(contact,
//                    viewHolder.imageView, false);
            headFetcher.loadHead(pContact, viewHolder.imageView, false);
            
            viewHolder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            if (!contact.isSelf() && deleteMode)
            {
                viewHolder.deleteImg.setVisibility(View.VISIBLE);
            }
        }
        // 添加按钮
        else
        {
            if (position == memberList.size() && memberList.size() < capacity)
            {
                if (deleteMode)
                {
                    convertView.setVisibility(View.GONE);
                }
                else
                {
                    viewHolder.textView.setText(null);
                    viewHolder.imageView
                            .setImageResource(R.drawable.group_add_selector);
                    convertView.setVisibility(View.VISIBLE);
                }
            }
            else
            {
                if (deleteMode)
                {
                    convertView.setVisibility(View.GONE);
                }
                else
                {
                    viewHolder.textView.setText(null);
                    viewHolder.imageView
                            .setImageResource(R.drawable.group_delete_selector);
                    convertView.setVisibility(View.VISIBLE);
                }
            }
        }

        return convertView;
    }

    private class MemberViewHolder
    {
        public ImageView deleteImg;

        public TextView textView;

        public ImageView imageView;

    }

    private boolean isOwner()
    {
        return CommonVariables.getIns().getUserAccount().equals(owner);
    }

    public void setDeleteMode(boolean deleteMode)
    {
        this.deleteMode = deleteMode;
    }

    public void setDiscussionFlag(boolean discussionFlag)
    {
        this.discussionFlag = discussionFlag;
    }

    public void setOnMemberClickListener(OnMemberClickListener lsn)
    {
        this.onMemberClickListener = lsn;
    }

    public interface OnMemberClickListener
    {
        void onMemberClick(int position);
    }
}
