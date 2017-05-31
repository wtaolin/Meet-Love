package com.huawei.esdk.uc.recent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.contacts.ContactCache;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.PersonalContact;
import com.huawei.dao.impl.RecentChatContactDao;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.headphoto.HeadPhotoUtil;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.utils.StringUtil;

public class RecentChatAdapter extends BaseAdapter
{

    private static final String TAG = RecentChatAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    private List<RecentChatContact> recentChatContacts = new ArrayList<RecentChatContact>();
    
    private ContactHeadFetcher headFetcher;

    private int TYPE_NORMAL = 0;


    public RecentChatAdapter(Context context)
    {
        mInflater = LayoutInflater.from(context);

        headFetcher = new ContactHeadFetcher(context);
    }

    public void setRecentChatList(List<RecentChatContact> recentChatContacts)
    {
        this.recentChatContacts = recentChatContacts;
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return recentChatContacts.size();
    }

    @Override
    public Object getItem(int position)
    {
        return recentChatContacts.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder = null;
        if (convertView != null)
        {
            holder = (ViewHolder) convertView.getTag();
        }
        if (holder == null)
        {
            holder = new ViewHolder();

            convertView = mInflater.inflate(R.layout.recent_list_item, null);
            holder.ivHead = (ImageView) convertView.findViewById(R.id.head);
            holder.ivStatus = (ImageView) convertView
                    .findViewById(R.id.status_image);
            holder.tvName = (TextView) convertView.findViewById(R.id.name);
            holder.tvMsg = (TextView) convertView.findViewById(R.id.msg);
            holder.tvUnRead = (TextView) convertView.findViewById(R.id.unread);

            convertView.setTag(holder);
        }

        final RecentChatContact recentChatContact = recentChatContacts
                .get(position);

        holder.tvName.setText(recentChatContact.getNickname());

        if (recentChatContact.getType() == RecentChatContact.ESPACECHATTER)
        {
            loadInstantMessage(recentChatContact, holder);
        }
        else if (recentChatContact.getType() == RecentChatContact.GROUPCHATTER
                || recentChatContact.getType() == RecentChatContact.DISCUSSIONCHATTER)
        {
            // loadGroupMessage(recentChatContact, holder);
        }

        return convertView;
    }

    class ViewHolder
    {
        ImageView ivHead;

        ImageView ivStatus;

        TextView tvName;

        TextView tvMsg;

        TextView tvUnRead;
    }

    private void loadInstantMessage(RecentChatContact o, ViewHolder holder)
    {
        PersonalContact pc = null;
        if (!TextUtils.isEmpty(o.getContactAccount()))
        {
            pc = ContactCache.getIns().getContactByAccount(
                    o.getContactAccount());
            if (pc == null)
            {
                pc = new PersonalContact();
                pc.setEspaceNumber(o.getContactAccount());
                pc.setNickname(o.getNickname());
            }
            String nameText = ContactFunc.getIns().getDisplayName(pc,
                    o.getNickname());
            if (!TextUtils.isEmpty(nameText))
            {
                // 已在匹配时存入. 出现的概率较小,不放在线程.
                if (!nameText.equals(o.getNickname()))
                {
                    o.setNickname(nameText);
                    try
                    {
                        new AsyncTask<RecentChatContact, Integer, String>()
                        {
                            @Override
                            protected String doInBackground(
                                    RecentChatContact... params)
                            {
                                RecentChatContactDao
                                        .updateContactName(params[0]);
                                return null;
                            }
                        }.execute(o);
                    }
                    catch (RejectedExecutionException exception)
                    {
                        Log.e(CommonUtil.APPTAG, TAG + " |RejectedExecutionException: " + exception);
                    }
                }
            }
        }
        if (pc == null)
        {
            return;
        }
        setUserStatus(holder.ivStatus, pc.getStatus(false));
        headFetcher.loadHead(pc, holder.ivHead, false);

        /**离线消息也需要加载*/
        int unReadMsg_offline = ImFunc.getIns().getUnreadMsgOffline(pc.getEspaceNumber());

//        int unReadMsgCount = o.getUnReadMsgsCount() + unReadMsg_offline;
        int unReadMsgCount = UnreadMessageManager.getIns().getUnreadMsgNumByAccount(pc.getEspaceNumber());

        if (unReadMsgCount <= 0)
        {
            holder.tvUnRead.setVisibility(View.GONE);
        }
        else if (unReadMsgCount >= 99)
        {
            holder.tvUnRead.setVisibility(View.VISIBLE);
            holder.tvUnRead.setBackgroundResource(R.drawable.red_shape_max);
        }
        else
        {
            holder.tvUnRead.setVisibility(View.VISIBLE);
            holder.tvUnRead.setBackgroundResource(R.drawable.red_shape);
            holder.tvUnRead.setText(String.valueOf(unReadMsgCount));
        }

        if (TextUtils.isEmpty(o.getNickname()))
        {
            holder.tvName.setText(o.getContactAccount());
        }
        else
        {
            holder.tvName.setText(o.getNickname());
        }
        InstantMessage instantMessage = o.getInstantMsg();
        if (instantMessage != null)
        {
            /**加判断为了文本消息和多媒体消息都能够正常显示  by wx303895*/
            if(instantMessage.getMediaType() == TYPE_NORMAL)
            {
                holder.tvMsg.setText(instantMessage.getContent());
            }else
            {
                holder.tvMsg.setText(R.string.media_symbol);
            }

        }
        // holder.time
        // .setText(DateUtil.getConverStringDate(o.getEndTime(), false));
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

}
