package com.huawei.esdk.uc;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.UCResource;
import com.huawei.common.res.LocContext;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.device.DeviceManager;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.conf.ConfCreateActivity;
import com.huawei.esdk.uc.conf.ConfListView;
import com.huawei.esdk.uc.contact.ContactView;
import com.huawei.esdk.uc.contact.SearchContactView;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.LoginFunc;
import com.huawei.esdk.uc.group.GroupListView;
import com.huawei.esdk.uc.group.GroupMemberAddActivity;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.recent.RecentChatView;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.esdk.uc.utils.NotifyUpdateUi;
import com.huawei.esdk.uc.widget.SlippingViewGroup;

public class MainActivityOld extends BaseActivity implements OnClickListener
{

    private static final String TAG = MainActivityOld.class.getSimpleName();

    private ImageView ivSelfHead;

    private ImageView ivStatus;

    private TextView tvUserName;

    private ImageView ivSearch;

    private ImageView ivCreate;

    private ImageView ivSetting;

    private SlippingViewGroup viewGroup;

    private ContactView contactView;

    private SearchContactView searchContactView;

    private GroupListView groupListView;

    private RecentChatView recentChatView;

    private ConfListView confListView;

    private PopupWindow popupWindow;

    private PopupWindow createPopupWindow;

    private RadioGroup rgStatus;

    private TextView tvLogout;

    private Dialog exitDialog;

    private ProgressDialog progressDialog;

    private IntentFilter filter;

    private int status;

    private boolean currIsSearch = false;

    private Animation animationDownGONE;

    private Animation animationDownVISIBLE;

    private Animation animationUpGONE;

    private Animation animationUpVISIBLE;

    private Context mContext;

    private ContactHeadFetcher headFetcher;

    private NotifyUpdateUi views[];

    /**同步所有联系人*/
    public static int ASY_ALL = 2;

    /**为了加载离线未读消息后刷新ui by wx303895*/
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case ImFunc.UNREAD_MSG_OFFLINE:

                    for(NotifyUpdateUi view : views)
                    {
                        if(view != null)
                        {
                            view.notifyUpdate();
                        }
                    }

                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();

        initAnimation();

        getUserPhotoHead();

        filter = new IntentFilter();
        filter.addAction(CustomBroadcastConst.ACTION_LOGINOUT_SUCCESS);
        filter.addAction(CustomBroadcastConst.ACTION_SET_STATUS_RESPONSE);
        filter.addAction(CustomBroadcastConst.UPDATE_CONTACT_VIEW);
        //filter.addAction(CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE);
        filter.addAction(CustomBroadcastConst.ACTION_CONNECT_TO_SERVER);


//        filter.addAction(ACTION.ACTION_GROUP_UPDATE);
//        filter.addAction(ACTION.ACTION_LEAVE_GROUP);


//        filter.addAction(CustomBroadcastConst.ACTION_CREATE_GROUP);
//        filter.addAction(CustomBroadcastConst.ACTION_GET_GROUP_PIC);
//        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER);
//        filter.addAction(CustomBroadcastConst.ACTION_LEAVE_GROUP);
//        filter.addAction(CustomBroadcastConst.ACTION_GROUP_CHANGE);
//        filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_CHAT);
//        filter.addAction(CustomBroadcastConst.ACTION_FIX_GROUP);
//        filter.addAction(CustomBroadcastConst.ACTION_MODIFY_GROUP);
//        filter.addAction(CustomBroadcastConst.ACTION_INVITETO_JOIN_GROUP);
//        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_MEMBERCHANGE);
//        filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE);


        registerRec();

        ContactFunc.getIns().loadContact(ASY_ALL);

        ImFunc.getIns().setViewHandler(handler);
    }

    private void initView()
    {
        ivSelfHead = (ImageView) findViewById(R.id.self_head);
        ivStatus = (ImageView) findViewById(R.id.cur_status);
        tvUserName = (TextView) findViewById(R.id.username);
        ivSearch = (ImageView) findViewById(R.id.search_img);
        ivCreate = (ImageView) findViewById(R.id.create_img);
        ivSetting = (ImageView) findViewById(R.id.setting_img);

        //滑动视图
        viewGroup = (SlippingViewGroup) findViewById(R.id.slippingGroup);

        //联系人
        contactView = new ContactView(this);
        viewGroup.addView(contactView);

        //头像加载
        headFetcher = new ContactHeadFetcher(MainActivityOld.this);

        searchContactView = (SearchContactView) findViewById(R.id.search_view);
        searchContactView.setVisibility(View.GONE);

        //讨论组
        if (ContactLogic.getIns().getAbility().isDiscussGroupAbility())
        {
            ivCreate.setVisibility(View.VISIBLE);
            groupListView = new GroupListView(this);
            viewGroup.addView(groupListView);
        }
        else
        {
            ivCreate.setVisibility(View.GONE);
        }

        //对话
        recentChatView = new RecentChatView(this);
        viewGroup.addView(recentChatView);

        views = new NotifyUpdateUi[]{ recentChatView , contactView };

        //会议
        confListView = new ConfListView(this);
        viewGroup.addView(confListView);

        ivSelfHead.setOnClickListener(this);
        ivSearch.setOnClickListener(this);
        ivCreate.setOnClickListener(this);
        ivSetting.setOnClickListener(this);

        tvUserName.setText(ContactFunc.getIns().getDisplayName(
                ContactFunc.getIns().getMySelf()));

        setUserStatus();
    }

    private AnimationListener animationListener = new AnimationListener()
    {
        @Override
        public void onAnimationStart(Animation animation)
        {
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {
        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            searchContactView.setVisibility(currIsSearch ? View.VISIBLE
                    : View.GONE);
            viewGroup.setVisibility(!currIsSearch ? View.VISIBLE : View.GONE);
        }
    };

    private void initAnimation()
    {
        animationDownGONE = AnimationUtils.loadAnimation(this,
                R.anim.translate_down_gone);
        animationDownGONE.setAnimationListener(animationListener);

        animationDownVISIBLE = AnimationUtils.loadAnimation(this,
                R.anim.translate_down_visible);
        animationDownVISIBLE.setAnimationListener(animationListener);

        animationUpGONE = AnimationUtils.loadAnimation(this,
                R.anim.translate_up_gone);
        animationUpGONE.setAnimationListener(animationListener);

        animationUpVISIBLE = AnimationUtils.loadAnimation(this,
                R.anim.translate_up_visible);
        animationUpVISIBLE.setAnimationListener(animationListener);
    }

    /**
     * 每次登陆, 获取自己的头像
     */
    private void getUserPhotoHead()
    {
    	//头像加载换成以下方法
//        HeadPhotoUtil.getInstance().loadSelfHeadPhoto(ivSelfHead);
    	headFetcher.loadHead(CommonVariables.getIns().getUserAccount(), ivSelfHead,false);

    }

    private void setUserStatus()
    {
        PersonalContact myContact = ContactFunc.getIns().getMySelf();
        int staut = myContact.getStatus(false);
        switch (staut)
        {
            case ContactClientStatus.ON_LINE:

                status = R.id.online;
                ivStatus.setImageResource(R.drawable.recent_online_small);
                break;

            case ContactClientStatus.BUSY:

                status = R.id.busy;
                ivStatus.setImageResource(R.drawable.recent_busy_small);
                break;

            case ContactClientStatus.XA:

                status = R.id.away;
                ivStatus.setImageResource(R.drawable.recent_away_small);
                break;

            case ContactClientStatus.AWAY:

                status = 0;
                ivStatus.setImageResource(R.drawable.recent_offline_small);
                break;

            default:
                status = 0;
                ivStatus.setImageResource(R.drawable.recent_offline_small);
                break;
        }
    }

    private void showStatusPopup(View anchor)
    {
        if (null == popupWindow)
        {
            View view = getLayoutInflater()
                    .inflate(R.layout.popup_status, null);

            rgStatus = (RadioGroup) view.findViewById(R.id.group_status);
            tvLogout = (TextView) view.findViewById(R.id.logout);

            rgStatus.setOnCheckedChangeListener(new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId)
                {
                    switch (checkedId)
                    {
                        case R.id.online:
                            SelfInfoUtil.getIns().setStatus(
                            		ContactClientStatus.ON_LINE);
                            break;
                        case R.id.busy:
                            SelfInfoUtil.getIns().setStatus(
                            		ContactClientStatus.BUSY);
                            break;
                        case R.id.away:
                            SelfInfoUtil.getIns().setStatus(ContactClientStatus.XA);
                            break;
                        default:
                            break;
                    }
                }
            });

            tvLogout.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    showExitDialog();
                    popupWindow.dismiss();
                }
            });

            popupWindow = new PopupWindow(view, LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.bg_dialog));
        }

        if (!popupWindow.isShowing())
        {
            rgStatus.check(status);
            popupWindow.showAsDropDown(anchor);
        }
    }

    private void showCreatePopup(View anchor)
    {
        if (null == createPopupWindow)
        {
            View view = getLayoutInflater()
                    .inflate(R.layout.create_popup, null);
            TextView createConf = (TextView) view
                    .findViewById(R.id.create_conf);
            TextView createDiscuss = (TextView) view
                    .findViewById(R.id.create_discuss);

            createConf.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    toCreateConf();
                    createPopupWindow.dismiss();
                }
            });

            createDiscuss.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    toCreateDiscussGroup();
                    createPopupWindow.dismiss();
                }
            });

            createPopupWindow = new PopupWindow(view,
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            createPopupWindow.setOutsideTouchable(true);
        }

        if (!createPopupWindow.isShowing())
        {
            createPopupWindow.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.bg_dialog));
            createPopupWindow.showAsDropDown(anchor);
        }
    }

    private void toCreateConf()
    {
        Intent intent = new Intent(MainActivityOld.this, ConfCreateActivity.class);
        startActivity(intent);
    }

    private void toCreateDiscussGroup()
    {
        Intent intent = new Intent(MainActivityOld.this,
                GroupMemberAddActivity.class);
        startActivity(intent);
    }

    private void showExitDialog()
    {
        if (exitDialog == null)
        {
            exitDialog = new Dialog(this, R.style.Theme_dialog);
            exitDialog.setContentView(R.layout.dialog_common);

            ((TextView) exitDialog.findViewById(R.id.dialog_message))
                    .setText(R.string.suretologout);
            exitDialog.setCanceledOnTouchOutside(true);

            ((Button) exitDialog.findViewById(R.id.dialog_leftbutton))
                    .setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (exitDialog.isShowing())
                            {
                                exitDialog.dismiss();
                            }
                        }
                    });

            ((Button) exitDialog.findViewById(R.id.dialog_rightbutton))
                    .setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (LoginFunc.getIns().isLogin())
                            {
                                exitDialog.dismiss();
//                                new Handler().postDelayed(new Runnable()
//                                {
//                                    @Override
//                                    public void run()
//                                    {
//                                        closeDialog();
//
//                                        LoginFunc.getIns().setLogin(false);
//                                        SelfInfoUtil.getIns()
//                                                .setToLogoutStatus();
//                                        UCAPIApp.getApp().getService().logout(false);
//                                        UCAPIApp.getApp().stopImService(true);
//
//                                        if (ContactLogic.getIns().getAbility().isAllUmAbility())
//                                        {
//                                            HttpCloudHandler.ins().clear();
//                                        }
//
//                                        UCAPIApp.getApp().popAllExcept(null);
//
//                                        DeviceManager.killProcess();
//                                    }
//                                }, 5 * 1000);

                                ApplicationHandler.getIns().exitOrLogout();

                                if (LoginFunc.getIns().logout())
                                {
                                    showProgerssDlg();
                                    return;
                                }
                            }
                            LoginFunc.getIns().setLogin(false);
                            SelfInfoUtil.getIns().setToLogoutStatus();
                            UCAPIApp.getApp().stopImService(true);
                            UCAPIApp.getApp().popAllExcept(null);

                            DeviceManager.killProcess();
                        }
                    });
        }

        exitDialog.show();
    }

    /**
     * 等待对话框
     */
    private void showProgerssDlg()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在登出，请稍后······");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeDialog()
    {
        if (null != progressDialog && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.self_head:

                showStatusPopup(v);

                break;
            case R.id.search_img:

                currIsSearch = !currIsSearch;
                int res = currIsSearch ? R.drawable.search_light
                        : R.drawable.search;
                ivSearch.setImageResource(res);

                if (currIsSearch)
                {
                    searchContactView.startAnimation(animationDownVISIBLE);
                    viewGroup.startAnimation(animationDownGONE);
                }
                else
                {
                    searchContactView.startAnimation(animationUpGONE);
                    viewGroup.startAnimation(animationUpVISIBLE);
                }

                break;
            case R.id.create_img:
            {
                showCreatePopup(v);
            }
                break;
            case R.id.setting_img:
            {
                Intent intent = new Intent(MainActivityOld.this, SetActivity.class);
                startActivity(intent);
            }
                break;

            default:
                break;
        }
    }

    private void registerRec()
    {
//        registerReceiver(receiver, filter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);
    }

    private void unRegisterRec()
    {
//        unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(CommonUtil.APPTAG, TAG + " | action = " + action);

            if (CustomBroadcastConst.ACTION_LOGINOUT_SUCCESS.equals(action))
            {
                closeDialog();

                int result = intent.getIntExtra(
                        UCResource.SERVICE_RESPONSE_RESULT, 0);
                Log.d(CommonUtil.APPTAG, TAG
                        + " | ACTION_LOGINOUT_SUCCESS | result = " + result);
                if (UCResource.REQUEST_OK == result)
                {
                    LoginFunc.getIns().setLogin(false);
                    SelfInfoUtil.getIns().setToLogoutStatus();
                    UCAPIApp.getApp().stopImService(true);
                    UCAPIApp.getApp().popAllExcept(null);

                    DeviceManager.killProcess();
                }
                else
                {
                    Toast.makeText(MainActivityOld.this, "登出失败", Toast.LENGTH_SHORT).show();
                }
            }
            else if (CustomBroadcastConst.ACTION_SET_STATUS_RESPONSE
                    .equals(action))
            {
                Log.d(CommonUtil.APPTAG, TAG + " | ACTION_SET_STATUS_RESPONSE ");
                SelfInfoUtil.getIns().onStatusRespProc(intent);
                setUserStatus();
            }
            else if (CustomBroadcastConst.UPDATE_CONTACT_VIEW.equals(action))
            {
                Log.d(CommonUtil.APPTAG, TAG + " | UPDATE_CONTACT_VIEW ");

                contactView.updateContactsList();

                if (groupListView != null)
                {
                    groupListView.updateGroups();
                }

                recentChatView.updateRecentChat();
            }
//            else if (CustomBroadcastConst.ACTION_SEARCE_CONTACT_RESPONSE
//                    .equals(action))
//            {
//                Log.d(CommonUtil.APPTAG, TAG
//                        + " | ACTION_SEARCE_CONTACT_RESPONSE ");
//
//                searchContactView.updateSearchContactList(intent);
//            }
            
            else if(CustomBroadcastConst.ACTION_CONNECT_TO_SERVER
                    .equals(action))
            {
            	 Log.d(CommonUtil.APPTAG, TAG
                         + " | ACTION_CONNECT_TO_SERVER ");
            	 
            	 boolean connectStatus = intent.getBooleanExtra(UCResource.SERVICE_RESPONSE_DATA,
                         false);
            	 Log.d(CommonUtil.APPTAG, TAG
                         + " | ACTION_CONNECT_TO_SERVER " + connectStatus);
            }
//            else if (ACTION.ACTION_GROUP_UPDATE.equals(action))
//            {
//                Log.d(CommonUtil.APPTAG, TAG + " | ACTION_GROUP_UPDATE ");
//
//                if (groupListView != null)
//                {
//                    groupListView.updateGroups();
//                }
//            }
//            else if (ACTION.ACTION_LEAVE_GROUP.equals(action))
//            {
//                Log.d(CommonUtil.APPTAG, TAG + " | ACTION_LEAVE_GROUP ");
//
//                if (groupListView != null)
//                {
//                    groupListView.updateGroups();
//                }
//            }
        }
    };

    @Override
    public void onBackPressed()
    {
        showExitDialog();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.d(CommonUtil.APPTAG, TAG + " | onResume");

        if (contactView != null)
        {
            contactView.updateContactsList();
        }
        if (groupListView != null)
        {
            groupListView.updateGroups();
        }
        if (searchContactView != null)
        {
            searchContactView.updateContacts();
        }
        if (recentChatView != null)
        {
            recentChatView.updateRecentChat();
        }

        ConferenceFunc.getIns().requestConferenceList();

        UCAPIApp.getApp().popAllExcept(this);
    }

    @Override
    protected void onDestroy()
    {
        unRegisterRec();
        /**强杀进程后销毁服务 by wx303895 start*/
        cancelAllNotification();
        UCAPIApp.getApp().stopImService();
        /**end*/
        Log.d(CommonUtil.APPTAG, TAG + " | kill process");
        Log.i("********************","kill kill");
        super.onDestroy();

    }

    /**
     * 清除所有通知
     */
    private void cancelAllNotification()
    {
        Context context = LocContext.getContext();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }

	@Override
	public void initializeData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeComposition() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearData() {
		// TODO Auto-generated method stub
		
	}

}
