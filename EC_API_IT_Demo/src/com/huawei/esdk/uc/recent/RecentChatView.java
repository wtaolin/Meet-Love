package com.huawei.esdk.uc.recent;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.huawei.contacts.PersonalContact;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.ImFunc.LocalImReceiver;
import com.huawei.esdk.uc.function.RecentChatFunc;
import com.huawei.esdk.uc.im.ChatActivity;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.esdk.uc.utils.NotifyUpdateUi;

public class RecentChatView extends LinearLayout implements NotifyUpdateUi
{

    private ListView lvRecentChat;

    private RecentChatAdapter recentChatAdapter;

    private List<RecentChatContact> recentChatContacts = new ArrayList<RecentChatContact>();

    private LocalImReceiver localImReceiver;

    private Dialog dialog;

    private static final int REFRESH_CHATVIEW = 0x03;

    private Handler recentChatHandler;

    public RecentChatView(Context context)
    {
        this(context, null);
    }

    public RecentChatView(final Context context, AttributeSet attrs)
    {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.view_recent,
                null);
        addView(view);

        lvRecentChat = (ListView) view.findViewById(R.id.list_recent);
        recentChatAdapter = new RecentChatAdapter(context);
        lvRecentChat.setAdapter(recentChatAdapter);

        updateRecentChat();

        lvRecentChat.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> paramAdapterView,
                                    View paramView, int paramInt, long paramLong) {
                RecentChatContact chatContact = recentChatContacts
                        .get(paramInt);
                PersonalContact contact = ContactFunc.getIns()
                        .getContactByAccount(chatContact.getContactAccount());

                if (contact == null) {
                    contact = new PersonalContact();
                    contact.setEspaceNumber(chatContact.getContactAccount());
                    contact.setNickname(chatContact.getNickname());
                }

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(IntentData.ESPACENUMBER, contact);
                context.startActivity(intent);
            }
        });

        lvRecentChat.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                RecentChatContact chatContact = recentChatContacts.get(arg2);
                showDeletDialog(chatContact);

                return false;
            }
        });

    }

    private void showDeletDialog(final RecentChatContact chatContact)
    {
        dialog = new Dialog(getContext(), R.style.Theme_dialog);
        dialog.setContentView(R.layout.delete_dialog_layout);
        TextView textView = (TextView) dialog.findViewById(R.id.delete);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RecentChatFunc.getIns().delete(chatContact);

                updateRecentChat();

                dialog.dismiss();
                dialog = null;
            }
        });
        dialog.show();
    }

    public void updateRecentChat()
    {
        recentChatContacts = RecentChatFunc.getAllRecentChatContact();
        if (recentChatAdapter != null)
        {
            recentChatAdapter.setRecentChatList(recentChatContacts);
        }
    }

    private void initHandler()
    {
        recentChatHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case REFRESH_CHATVIEW:
                        updateRecentChat();
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
                recentChatHandler.sendEmptyMessage(REFRESH_CHATVIEW);
//                updateRecentChat();
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

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        registerIM();
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
        if(recentChatAdapter != null)
        {
            recentChatAdapter.notifyDataSetChanged();
        }
    }
}
