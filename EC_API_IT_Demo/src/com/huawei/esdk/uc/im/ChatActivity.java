package com.huawei.esdk.uc.im;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler;
import com.huawei.common.constant.UCResource;
import com.huawei.contacts.ContactClientStatus;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.dao.impl.InstantMessageDao;
import com.huawei.data.ConstGroup;
import com.huawei.data.ConstGroupContact;
import com.huawei.data.GroupChangeNotifyData;
import com.huawei.data.LeaveGroupResp;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.data.entity.RecentChatter;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.esdk.uc.function.GroupFunc;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.im.adapter.ChatAdapter;
import com.huawei.esdk.uc.im.adapter.MoreOptsAdapter;
import com.huawei.esdk.uc.im.data.GetHistoryMessageInfo;
import com.huawei.esdk.uc.utils.ChatUtil;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.utils.UnreadMessageManager;
import com.huawei.esdk.uc.widget.SoundWaveView;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.module.um.UmUtil;
import com.huawei.msghandler.maabusiness.LeaveGroupRequester;
import com.huawei.reportstatistics.controller.EventReporter;
import com.huawei.reportstatistics.data.StatsEvent;
import com.huawei.utils.DateUtil;
import com.huawei.utils.SoftInputUtil;
import com.huawei.utils.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends BaseActivity implements OnClickListener {

	private static final String TAG = ChatActivity.class.getSimpleName();

	private TextView tvBack;

	private ImageView ivStatus;

	private TextView tvUserName;

	private EditText edContent;

	private Button btnSend;

	private ListView listView;

	private ArrayList<InstantMessage> datalist = new ArrayList<InstantMessage>();

	private ChatAdapter adapter;

	private PersonalContact curContact;

	private String groupId;

	private int groupType;

	private ConstGroup constGroup;

	private boolean isGroup;

	private List<ConstGroupContact> constGroupContacts = new ArrayList<ConstGroupContact>();

	private ImFunc.LocalImReceiver localImReceiver;

	private IntentFilter filter;

	private Dialog dialog;

	private Button btnGetHistoryMsg;

	private Button btnMoreType;// 输入的+号，选择更多的消息发送类型

	private String curAccount; // 当前界面的用户ID，可以是个人的ID也可以是群组的ID
	private int msgType; // 需要获取的漫游消息类型，根据用户所在的聊天场景来确定是p2p的聊天消息，还是群组消息
	private ProgressDialog progressDialog;
	private static final int CHAT_LIST_FIRST = 0; // 第一次获取漫游消息刷新列表
	private static final int CHAT_LIST_NOT_FIRST = 1;
	/** 表情和常用回复 */
	private RelativeLayout moreLayout = null;
	/** 更多操作GridView */
	private GridView moreOptsGridView = null;
	private MoreOptsAdapter moreAdapter = null;
	/** 聊天界面底部标题 */
	private ViewGroup bottomArea = null;
	/** 输入框（包括语音图标的父布局） */
	private RelativeLayout editLayout = null;
	/** 聊天输入框 */
	private EditText editText = null;
	private TextView wordCount = null;
	private ImageView emotionButton = null;
	/** 发送按钮 */
	private ImageView sendButton = null;
	/** 输入图标按钮 */
	private Button audioStart = null;
	private SoundWaveView audioLayout = null;
	/** 录音提示框布局 */
	private TextView recordPrompt = null;
	/** 录音提示布局 */
	private LinearLayout audioHintLayout = null;
	/** 录音时间View */
	private TextView timePromptView = null;
	/** 说话提示 */
	private TextView speakHint = null;
	/**
	 * 图片标志
	 */
	public static final int PROCESS_UM_MEDIA = 0x80;
	public static final int PROCESS_UM_CHOOSE_VIDEO = 0x81;

	/**
	 * 页面加载数据
	 */
	public static final int CHAT_LOAD_DATA = 0x01;

	public static final int MSG_PREPEND = 0x02;

	/**
	 * 拍照标志
	 */
	public static final int PROCESS_UM_CAMERA = 0xC0;

	/**
	 * 视频标志
	 */
	public static final int PROCESS_UM_VIDEO = 0xFE;

	/**
	 * 录制视频，跳转到预览页面，再回到当前页面的标志
	 */
	public static final int PROCESS_UM_VIDEO_SCAN = 0xFF;

	/**
	 * 选择图片/拍照后跳转到预览页面，从预览页面回到当前页面的标志
	 */
	public static final int PROCESS_UM_CAMERA_SCAN = 0xF0;

	/**
	 * 录音时间变更
	 */
	public static final int RECORD_TIME_PROMPT = 0xF8;

	/**
	 * 录音音量/及时间变更
	 */
	public static final int VOLUMEANDTIMECHANGE = 0xFC;

	/**
	 * 录音时间太短或倒计时结束
	 */
	public static final int RECORD_TIMELIMIT_DELAY = 1003;

	/**
	 * 录音超过限制
	 */
	public static final int RECORD_TIMELIMIT_BEYOND = 1004;

	/**
	 * 标志是否开始录音
	 */
	private boolean startRecord;

	/**
	 * 录音时间
	 */
	private int timeCount;

	/**
	 * 录音计数器
	 */
	private Timer timer;

	/** 按住说话按钮 */
	private ImageView recordBtn = null;

	private String recordPath;

	// 富媒体消息状态更新
	public static final int UPLOAD_PROCESSING = 1000;

	public static final int TRANS_FINISH = 1001;

	public static final int DOWNLOAD_PROCESSING = 1002;

	/**
	 * 更新显示timestamp， 隐藏TIMESTAMP
	 */
	private static final int UPDATE_TIMESTAMP = 1005;
	private static final int HIDE_TIMESTAMP = 1006;

	/**
	 * 录音时间计数频率.
	 */
	public static final int VOICE_RECORD_TIME_RATE = 100;

	public static final int ONE_SECOND = 1000;

	private static final int INIT_STATE = 0;
	private static final int AUDIO_STARTED = 1;
	private static final int KEYBOARD_STARTED = 2;

	private static RecentChatter curChatter;

	private Context mContext;

	/**
	 * 聊天类型，个人or群组
	 * */
	private int chatType;

	/**
	 * 当前聊天人发送的消息
	 */
	public static final int MSG_SEND = 0x023;

	/**
	 * 收到当前聊天人的消息
	 */
	public static final int MSG_CURRENT = 0x03;

	private boolean noHistory = false;
	
	/**
     * um广播action
     */
    private String[] umBroadcast;

	//保存groupId
	private String unReadMsgGroupId;

    /**
     * um消息接收器
     */
    private BaseReceiver umReceiver;

	private Handler handler = new Handler() {

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(android.os.Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);

			switch (msg.what) {
			case CHAT_LOAD_DATA:
				List<InstantMessage> msgList = (List<InstantMessage>) msg.obj;

				if(msgList == null)
				{
					adapter.notifyDataSetChanged();
					return;
				}

				Logger.info(TAG, "CHAT_LOAD_DATA  msgList size ========= "
						+ msgList.size());

				updateChatDataList(msgList, msg.arg1);
				break;

			case MSG_PREPEND:
				// List<InstantMessage> msgs = (List<InstantMessage>) msg.obj;
				InstantMessage rsvMsg = (InstantMessage) msg.obj;
				adapter.setMessageData(rsvMsg);
				listView.setSelection(datalist.size() - 1);
				//未读消息标记为已读 bylwx302895
				markUnreadMsgByMsgId(rsvMsg.getMessageId());
				rsvMsg.setStatus(InstantMessage.STATUS_READ);
//				InstantMessageDao.updateMessageIdAndStatus(rsvMsg);
				// updateChatDataList(msgs);
				break;

			case MSG_CURRENT:
				InstantMessage rsvMsg1 = (InstantMessage) msg.obj;
				adapter.setMessageData(rsvMsg1);
				listView.setSelection(datalist.size() - 1);
				//未读消息标记为已读 bylwx302895
				markUnreadMsgByMsgId(rsvMsg1.getMessageId());
				delMsg();
				ImFunc.getIns().updateStatus(rsvMsg1, InstantMessage.STATUS_READ);
				
				
//				rsvMsg1.setStatus(InstantMessage.STATUS_READ);
//				InstantMessageDao.updateMessageIdAndStatus(rsvMsg1);
				break;

			case MSG_SEND:
				InstantMessage message = (InstantMessage) msg.obj;
				// datalist.add(message);
				adapter.setMessageData(message);
				adapter.notifyDataSetChanged();
				setListSelection(adapter.getCount() - 1, 0);
				break;

			case UPLOAD_PROCESSING:
				adapter.onUploadProgress(msg.arg1, msg.arg2);
				break;
			case TRANS_FINISH:
				adapter.onTransFinish((UmReceiveData) msg.obj,
						UmConstant.getFinishAction(msg.arg1));
				break;
			case DOWNLOAD_PROCESSING:
				adapter.onDownloadProgress(msg.arg1, msg.arg2,
						(Integer) msg.obj);
				break;

			case VOLUMEANDTIMECHANGE:
				if (startRecord) // 开始录音时才需要刷新view显示
				{
					audioLayout.invalidateView(VoipFunc.getIns()
							.getCurMircoVol());
				}
				timePromptView.setText(getTimeBytimeCount(timeCount));
				break;

			case RECORD_TIMELIMIT_DELAY:
				audioHintLayout.setVisibility(View.GONE);
				break;
			case RECORD_TIMELIMIT_BEYOND:
				recordPrompt.setText(getString(R.string.record_end));
				handler.sendEmptyMessageDelayed(RECORD_TIMELIMIT_DELAY, 500);

				refreshWhenStop();
				stopRecord();
				ImFunc.getIns().prepareAudioToSend(
						recordPath,
						ContactLogic.getIns().getMyOtherInfo()
								.getUmVoiceRecordLength(), getChatType());
				break;

			default:
				break;
			}
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_im);

		mContext = this;

		filter = new IntentFilter();
		filter.addAction(CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER);
		filter.addAction(CustomBroadcastConst.UPDATE_CONTACT_VIEW);
		filter.addAction(CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE);

		ImFunc.getIns().setHandler(handler); // 设置聊天界面的消息回调


		initViewComponent();

		initData();

		//registerReceiver(receiver, filter);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
		
		registerUM();
	}

	private void initViewComponent() {
		tvBack = (TextView) findViewById(R.id.btn_back);
		ivStatus = (ImageView) findViewById(R.id.cur_status);
		tvUserName = (TextView) findViewById(R.id.username);

		edContent = (EditText) findViewById(R.id.content);
		btnSend = (Button) findViewById(R.id.btn_sent);

		listView = (ListView) findViewById(R.id.list_im);
		adapter = new ChatAdapter(this, datalist);
		listView.setAdapter(adapter);
		adapter.setListView(listView);

		tvBack.setOnClickListener(this);
		btnSend.setOnClickListener(this);

		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				InstantMessage msg = datalist.get(arg2);
				showDeletDialog(msg);
				return false;
			}
		});

		btnGetHistoryMsg = (Button) findViewById(R.id.btn_get_history_msg);
		btnGetHistoryMsg.setOnClickListener(this);

		moreLayout = (RelativeLayout) findViewById(R.id.more_layout);

		btnMoreType = (Button) findViewById(R.id.btn_more);
		btnMoreType.setOnClickListener(this);

		// 更多的选项: 包括 表情/快速回复/语音通话/视频通话/图库等.
		moreAdapter = new MoreOptsAdapter(this);
		moreOptsGridView = (GridView) findViewById(R.id.grid_more_opts);
		moreOptsGridView.setAdapter(moreAdapter);
		moreOptsGridView.setOnItemClickListener(moreItemClickListener);

		emotionButton = (ImageView) findViewById(R.id.emotion_button);
		emotionButton.setOnClickListener(this);

		// 处理输入框时间.
		editLayout = (RelativeLayout) findViewById(R.id.txt_input_area);
		editText = (EditText) findViewById(R.id.et_txt_input);
		editText.setOnClickListener(this);
		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
				ContactLogic.getIns().getMyOtherInfo().getMaxMessageLength()) });
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean isFocused) {
				if (isFocused) {
					// hideSetting();
					editLayout
							.setBackgroundResource(R.drawable.chat_input_background_select);
					if (!emotionButton.isSelected()) {
						hideButtomWholeLayout();
					}
				} else {
					editLayout
							.setBackgroundResource(R.drawable.chat_input_background);
				}
			}
		});
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
									  int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				int length = StringUtil.sizeOf(s.toString());
				// setWordCount(length);

				// if (!chatLogic.isUmBtnVisible())
				// {
				// return;
				// }
				switchSendMsgBtn(length > 0);
			}
		});

		initAudioRecordLayout();

		bottomArea = (ViewGroup) findViewById(R.id.chat_bottom_area);

	}

	private void showDeletDialog(final InstantMessage msg) {
		dialog = new Dialog(this, R.style.Theme_dialog);
		dialog.setContentView(R.layout.delete_dialog_layout_2);
		TextView delete = (TextView) dialog.findViewById(R.id.delete);
		TextView deleteAll = (TextView) dialog.findViewById(R.id.deleteall);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				InstantMessageDao.deleteInstantMessage(msg.getId());

				getHistoryMsg();

				dialog.dismiss();
				dialog = null;
			}
		});
		deleteAll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String chatId;
				int msgType;

				if (isGroup) {
					chatId = constGroup.getGroupId();
					if (groupType == ConstGroup.FIXED) {
						msgType = RecentChatContact.GROUPCHATTER;
					} else {
						msgType = RecentChatContact.DISCUSSIONCHATTER;
					}
				} else {
					chatId = curContact.getEspaceNumber();
					msgType = RecentChatContact.ESPACECHATTER;
				}
				InstantMessageDao.delete(chatId,
						RecentChatContact.ESPACECHATTER);

				getHistoryMsg();

				//by lwx302895 start 修复“删除所有”选项崩溃，因为没有及时刷新UI
				adapter.notifyDataSetChanged();
				//by lwx302895 end

				dialog.dismiss();
				dialog = null;
			}
		});
		dialog.show();
	}

	private void initData() {
		groupId = getIntent().getStringExtra(IntentData.GROUPID);
		groupType = getIntent().getIntExtra(IntentData.GROUPTYPE,
				ConstGroup.DISCUSSION);

		curContact = (PersonalContact) getIntent().getSerializableExtra(
				IntentData.ESPACENUMBER);

		if (!TextUtils.isEmpty(groupId)) {
			isGroup = true;
			constGroup = GroupFunc.getIns().findConstGroupById(groupId);
			tvUserName.setText(constGroup.getName());
			updateGroupMember();
			ivStatus.setVisibility(View.INVISIBLE);

			if (groupType == ConstGroup.DISCUSSION) {
				msgType = RecentChatContact.DISCUSSIONCHATTER;
				chatType = RecentChatContact.DISCUSSIONCHATTER;
			} else {
				msgType = RecentChatContact.GROUPCHATTER;
				chatType = RecentChatContact.GROUPCHATTER;
			}

			curAccount = constGroup.getGroupId();
			curChatter = new RecentChatter(curAccount, msgType);

//			UnreadMessageManager.getIns().clearUnreadNumber(groupId); //清除离线未读消息
//			UnreadMessageManager.getIns().delMsgByGroupId(groupId);

			unReadMsgGroupId = groupId;

		} else {
			isGroup = false;

			tvUserName.setText(ContactFunc.getIns().getDisplayName(curContact));

			setUserStatus(ivStatus, curContact.getStatus(false));

			msgType = RecentChatContact.ESPACECHATTER;

			chatType = RecentChatContact.ESPACECHATTER;

			curAccount = curContact.getEspaceNumber();

			curChatter = new RecentChatter(curAccount, msgType);

//			UnreadMessageManager.getIns().clearUnreadNumber(curContact.getEspaceNumber()); //清除离线未读消息
//			UnreadMessageManager.getIns().delMsgByAccount(curContact.getEspaceNumber());

		}

		registerIM();

		getHistoryMsg();

		if (ImFunc.getIns().getUnReadChatCount() > 0) {
			tvBack.setText("对话(" + ImFunc.getIns().getUnReadChatCount() + ")");
		}

//		if(curContact != null)
//		{
//			UnreadMessageManager.getIns().clearUnreadNumber(curContact.getEspaceNumber()); //清除离线未读消息
//		}
	}

	private void getHistoryMsg() {
		Logger.info(TAG, "getHistoryMsg datalist size =======1======== "
				+ datalist.size());

		datalist.clear();

		Logger.info(TAG, "getHistoryMsg datalist size =======2======== "
				+ datalist.size());

		// String chatId;
		// int msgType;
		// if (isGroup)
		// {
		// chatId = groupId;
		// msgType = RecentChatContact.DISCUSSIONCHATTER;
		// }
		// else
		// {
		// chatId = curContact.getEspaceNumber();
		// msgType = RecentChatContact.ESPACECHATTER;
		// }
		// List<InstantMessage> messages =
		// ImFunc.getIns().getLastHistoryMassages(
		// chatId, 40, msgType);
		//
		// if (messages != null)
		// {
		// datalist.addAll(messages);
		//
		// adapter.notifyDataSetChanged();
		// listView.setSelection(datalist.size() - 1);
		// }
		// else
		// {
		// adapter.notifyDataSetChanged();
		// }

		Logger.info(TAG, "getHistoryMsg datalist size =======3======== "
				+ datalist.size());

		// 获取漫游消息记录
		showGetMsgDlg();
		ThreadManager.getInstance().addToFixedThreadPool(
				new MyRunnable(getCurAccount(), getMsgType()));
	}

	private static class MyRunnable implements Runnable {
		private String key;
		private int type;

		public MyRunnable(String key, int type) {
			this.key = key;
			this.type = type;
		}

		@Override
		public void run() {
			// 获取历史消息
			GetHistoryMessageInfo info = new GetHistoryMessageInfo(key, -1, "",
					type, true, 0);
			ImFunc.getIns().requestHistoryMessage(info);
		}
	}

	/**
	 * 在onResume中将未读消息标记为已读
	 */
	private void markUnreadMsg()
	{
		String chatId;
		int msgType;
		if (isGroup)
		{
			chatId = unReadMsgGroupId;
			msgType = RecentChatContact.DISCUSSIONCHATTER;
		}
		else
		{
			chatId = curContact.getEspaceNumber();
			msgType = RecentChatContact.ESPACECHATTER;
		}
		UnreadMessageManager.getIns().setMsgMarked(chatId, msgType);
		ImFunc.getIns().getUnReadMsg(chatId, msgType);

//		ImFunc.getIns().markRead(ImFunc.getIns().getMarkType(msgType), chatId , , ImFunc.getIns().getMarkTag(msgType));
	}

	/**
	 * 如果是在当前界面每次接收到消息都会把消息置为已读
	 * @param messageId
	 */
	private void markUnreadMsgByMsgId(String messageId)
	{
		String chatId;
		int msgType;
		if (isGroup)
		{
			chatId = unReadMsgGroupId;
			msgType = RecentChatContact.DISCUSSIONCHATTER;
		}
		else
		{
			chatId = curContact.getEspaceNumber();
			msgType = RecentChatContact.ESPACECHATTER;
		}
		UnreadMessageManager.getIns().setMsgMarked(chatId, messageId, msgType);
	}

	private void setUserStatus(ImageView ivStatus, int status) {
		switch (status) {
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

	private void updateGroupMember() {
		constGroupContacts.clear();
		constGroupContacts.addAll(GroupFunc.getIns().getGroupMembers(groupId,
				groupType));
		if (constGroupContacts.size() <= 0) {
			GroupFunc.getIns().queryGroupMembersByGroupId(groupId, groupType);
		}
		tvUserName.setText(constGroup.getName() + "("
				+ constGroupContacts.size() + ")");
	}

	// 接收到漫游消息之后去重并更新消息列表
	private void updateChatDataList(List<InstantMessage> msgList) {
		if (msgList == null) {
			return;
		}

		boolean isSameId;

		for (InstantMessage message : msgList) {
			// 只有在数据库id不为0的时候再做此判断，沙特的漫游记录不入库，没有数据库ID
			if (message.getId() != 0) {
				isSameId = false;

				// 如果消息数据库ID相同，则不添加到界面
				for (InstantMessage item : datalist) {
					if (item != null) {
						if (message.getId() == item.getId()) {
							isSameId = true;
							break;
						}
					}
				}

				if (isSameId) {
					continue;
				}
			}

			appendNormalMessage(message, 1);
		}
	}

	// 接收到漫游消息之后将消息插入消息列表中
	@SuppressWarnings("unchecked")
	private void updateChatDataList(List<InstantMessage> msgList, int isFirst) {

		Logger.info(TAG, "updateChatDataList datalist size =====1====== "
				+ datalist.size());

		if (msgList == null) {
			return;
		}

		Comparator<InstantMessage> comparator = new Comparator<InstantMessage>() { //依据时间排序
			public int compare(InstantMessage s1, InstantMessage s2) {
				if (s1.getTimestamp().getTime() != s2.getTimestamp().getTime()) {
					return (int) (s1.getTimestamp().getTime() - s2
							.getTimestamp().getTime());
				} else {
					return 0;
				}

			}
		};

//		ArrayList<InstantMessage> temp = new ArrayList<InstantMessage>();
//		datalist.addAll(msgList);

//		temp = (ArrayList<InstantMessage>) removeDuplicateWithOrder(datalist);

//		adapter.setDataList(temp);
		
		updateChatDataList(msgList);

		Logger.info(TAG, "updateChatDataList datalist size =====2====== "
				+ datalist.size());

		Collections.sort(datalist, comparator);
		adapter.notifyDataSetChanged();

		if (isFirst == CHAT_LIST_NOT_FIRST) {
			listView.setSelection(0);
		} else {
			listView.setSelection(datalist.size() - 1);
		}

	}

	// 去重
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List removeDuplicateWithOrder(List list) {
		Set set = new HashSet();
		List newList = new ArrayList();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (set.add(element)) {
				newList.add(element);
			}
		}
		return newList;
	}

	/**
	 * 加载普通的消息.
	 * 
	 * @param im
	 * @param index
	 * @return
	 */
	private int appendNormalMessage(InstantMessage im, int index) {
		int count = 0;

		// 内容敏感词检查; containSensitiveWords保留，以兼容老版本。
		boolean sensitive = im.isContainSensitive()
				|| ChatUtil.containSensitiveWords(im.getContent());

		// 如果是普通消息，才进行敏感词过滤。富媒体消息不进行过滤
		sensitive = sensitive && im.getMediaType() == MediaResource.TYPE_NORMAL;
		if (sensitive) {
			im.setContent(ChatUtil.sensitiveFilter(im.getContent()));
		}

		count++;

		datalist.add(im);

		return count;
	}

	/**
	 * 初始化接收聊天消息,并注册
	 */
	private void registerIM() {           
		localImReceiver = new ImFunc.LocalImReceiver() {
			public void onReceive(InstantMessage msg, boolean update) {
				if (null == msg) {
					return;
				}
				if ((isGroup && ImFunc.getIns()
						.isCurrentGroupChat(msg, groupId))
						|| (!isGroup && ImFunc.getIns().isCurrentIMChat(msg,
								curContact.getEspaceNumber())) || update) {
					// 发送消息通知到界面更新
					android.os.Message notify = new android.os.Message();
					notify.what = ChatActivity.MSG_CURRENT;
					notify.obj = msg;
					handler.sendMessage(notify);
				} else {
					if (ImFunc.getIns().getUnReadChatCount() > 0) {
						tvBack.setText("对话("
								+ ImFunc.getIns().getUnReadChatCount() + ")");
					}
				}
			}

			@Override
			public void onRequestHistoryBack(List<InstantMessage> msgList,
					GetHistoryMessageInfo info) {
				// TODO Auto-generated method stub
				closeGetMsgDlg();
				String account = getCurAccount();

				if ((info == null) || (msgList == null))
				{
					Logger.error(TAG, "param null!!!!");

					/**剩最后一行删除后也要刷新 ui by wx303895*/
					android.os.Message msg = new android.os.Message();
					msg.what = CHAT_LOAD_DATA;
					handler.sendMessage(msg);
				    /**end */

					return;
				}

				Logger.info(TAG, "onRequestHistoryBack msgList size ======== "
						+ msgList.size());

				if (account != null)
				{
					if (!account.equals(info.getAccount()))
					{
						return;
					}
				}
				else
				{
					return;
				}

				List<InstantMessage> localList = new ArrayList<InstantMessage>();

				if (msgList != null && !msgList.isEmpty())
				{
					localList.addAll(msgList);
				}

				if (info.isFirst())
				{
					Collections.reverse(localList);
					android.os.Message msg = new android.os.Message();
					msg.what = CHAT_LOAD_DATA;
					msg.obj = localList;
					msg.arg1 = CHAT_LIST_FIRST;
					handler.sendMessage(msg);

				}
				else
				{
					if (info.isHaveHistory())
					{
						Collections.reverse(localList);
						android.os.Message msg = new android.os.Message();
						msg.what = CHAT_LOAD_DATA;
						msg.obj = localList;
						msg.arg1 = CHAT_LIST_NOT_FIRST;
						handler.sendMessageDelayed(msg, 400);
					}

					noHistory = !info.isHaveHistory();
				}

			}

			@Override
			public void refreshDisplayAfterSendMessage(InstantMessage msg)
			{
				// TODO Auto-generated method stub
				String toAccount = msg.getToId();
				if (toAccount.equals(getCurAccount()))
				{
					android.os.Message message = new android.os.Message();
					message.what = MSG_SEND;
					message.obj = msg;
					handler.sendMessage(message);
				}
			}

		};

		// 注册
		ImFunc.getIns().registerBroadcast(localImReceiver);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_back:
			UCAPIApp.getApp().popActivity(ChatActivity.this);
			break;

		case R.id.btn_chat_send: // 发送文本消息

			sendIM();
			editText.setText("");
			break;

		case R.id.btn_get_history_msg: // 获取消息漫游记录
			showGetMsgDlg();
			loadMoreChatData();
			break;

		case R.id.btn_more: // 选择更多的消息发送类型
			openMoreOptsLayout(false);
			break;

		case R.id.btn_audio_start: // 打开录音界面
			toggleAudioView();
			if (getChatType() == RecentChatContact.ESPACECHATTER)
			{
				EventReporter.getIns().report(StatsEvent.CLICK_PTOP_MICROPHONE,
						ChatActivity.class.getName());
			}
			else
			{
				EventReporter.getIns().report(
						StatsEvent.CLICK_GROUP_MICROPHONE,
						ChatActivity.class.getName());
			}
			break;

		default:
			break;
		}
	}

	// 更多类型消息选择
	private AdapterView.OnItemClickListener moreItemClickListener = new OnItemClickListener()
	{

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id)
		{
			// TODO Auto-generated method stub
			int[] btn = moreAdapter.getMoreBtnArray();
			if (null == btn)
			{
				return;
			}
			int index = btn[position];
			switch (index)
			{
			case MoreOptsAdapter.MORE_BTN_GALLERY:// 图片
				showMediaActivity(false);
				break;
			case MoreOptsAdapter.MORE_BTN_VIDEO:// 视频
				showMediaActivity(true);
				break;
			}
		}

	};

	private void sendIM()
	{
		String contentStr = editText.getText().toString().trim();

		if (isGroup)
		{
			sendGroupMsg(contentStr);
		}
		else
		{
			sendChatIM(contentStr);
		}
	}

	private void sendChatIM(String contentStr)
	{
		String eSpaceNumStr = curContact.getEspaceNumber();
		if ((!TextUtils.isEmpty(eSpaceNumStr))
				&& (!TextUtils.isEmpty(contentStr)))
		{
			boolean result = ImFunc.getIns().sendMessage(contentStr,
					eSpaceNumStr);
			if (result)
			{
				editText.setText("");
			}
		}
	}

	private void sendGroupMsg(String msg)
	{

		ImFunc.getIns().sendGroupMsg(groupId, groupType, msg);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(CommonUtil.APPTAG, TAG + " |action = " + action);
			if (CustomBroadcastConst.ACTION_GROUPSEND_QUERYMEMBER.equals(action))
			{
				updateGroupMember();
			}
			else  if(CustomBroadcastConst.UPDATE_CONTACT_VIEW
					.equals(action))
			{
				if(curContact != null)
				{
					setUserStatus(ivStatus, curContact.getStatus(false));
				}

			}
			else if (CustomBroadcastConst.ACTION_GROUPNOTIFY_GROUPDELTE.equals(action))
			{
					GroupChangeNotifyData data = (GroupChangeNotifyData) intent
							.getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);
					if (ResponseCodeHandler.ResponseCode.REQUEST_SUCCESS.value() == data.getStatus().value() && data.getGroupId().equals(groupId))
					{
						Toast.makeText(ChatActivity.this, "您已被请出讨论组", Toast.LENGTH_SHORT).show();
						UCAPIApp.getApp().popActivity(ChatActivity.this);
					}
				}
			}
	};

	private void delMsg()
	{
		if (!TextUtils.isEmpty(unReadMsgGroupId))
		{
			UnreadMessageManager.getIns().clearUnreadNumber(unReadMsgGroupId); //清除离线未读消息
			UnreadMessageManager.getIns().delMsgByGroupId(unReadMsgGroupId);
		}
		else
		{
			UnreadMessageManager.getIns().clearUnreadNumber(curContact.getEspaceNumber()); //清除离线未读消息
			UnreadMessageManager.getIns().delMsgByAccount(curContact.getEspaceNumber());
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		markUnreadMsg();

		delMsg();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		//unregisterReceiver(receiver);
		ImFunc.getIns().unRegisterBroadcast(localImReceiver);
		UmFunc.getIns().unRegisterBroadcast(umReceiver, umBroadcast);
	}

	/**
	 * 加载更多聊天数据。
	 */
	private void loadMoreChatData()
	{

		Logger.info(TAG, "loadMoreChatData datalist size =======1======== "
				+ datalist.size());
		ThreadManager.getInstance().addToFixedThreadPool(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				InstantMessage message = getFirstMessage();

				GetHistoryMessageInfo info;

				if (message == null)
				{
					info = new GetHistoryMessageInfo(getCurAccount(), -1, "",
							getMsgType(), false, 0);
				} else {
					long time = 0;

					time = message.getTimestamp().getTime();
					info = new GetHistoryMessageInfo(getCurAccount(), message
							.getId(), message.getMessageId(), getMsgType(),
							false, time);
				}

				ImFunc.getIns().requestHistoryMessage(info);
			}

		});

	}

	/**
	 * 返回账号
	 * */
	private String getCurAccount() {
		return curAccount;
	}

	public static RecentChatter getCurChatter() {
		return curChatter;
	}

	/**
	 * 返回需要获取的漫游消息类型
	 * */
	private int getMsgType() {
		return msgType;
	}

	// 获取消息列表中的第一条消息
	private InstantMessage getFirstMessage()
	{
		if ((datalist == null) || (datalist.isEmpty()))
		{
			return null;
		}

		for (InstantMessage msg : datalist)
		{
			return msg;
		}

		return null;
	}

	/**
	 * 等待登录对话框
	 */
	private void showGetMsgDlg()
	{
		if (null == progressDialog)
		{
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在获取漫游消息，请稍后······");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}

	private void closeGetMsgDlg()
	{
		if (null != progressDialog && progressDialog.isShowing())
		{
			progressDialog.dismiss();
		}
	}

	/**
	 * 显示图片选择Activity
	 * 
	 * @param isVideo
	 *            判断是否为视频
	 */
	private void showMediaActivity(boolean isVideo)
	{
		final Intent intent = new Intent(this, PictureMainActivity.class);
		intent.putExtra(IntentData.IS_VIDEO, isVideo);
		String account = getCurAccount();
		intent.putExtra(PicturePreviewActivity.ACCOUNT, account);
		int source = IntentData.SourceAct.IM_CHAT.ordinal();
		intent.putExtra(IntentData.FROM_ACTIVITY, source);

		startActivityForResult(intent, PROCESS_UM_MEDIA);
	}

	/**
	 * 打开更多选项的操作界面
	 */
	private void openMoreOptsLayout(final boolean isEmotion)
	{
		if (moreLayout.getVisibility() == View.VISIBLE
				&& moreOptsGridView.getVisibility() == View.VISIBLE
				&& !isEmotion) {
			return;
		}

		// 先隐藏键盘,进行一点时延后再进行布局弹出.
		SoftInputUtil.hideSoftInput(ChatActivity.this, editText);
		handler.postDelayed(new Runnable() {
			@Override
			public void run()
			{
				moreLayout.setVisibility(View.VISIBLE);

				if (isEmotion)
				{
					moreOptsGridView.setVisibility(View.INVISIBLE);
					editText.requestFocus();
				}
				else
				{
					moreOptsGridView.setVisibility(View.VISIBLE);
					editText.clearFocus();
				}

				audioLayout.setVisibility(View.GONE);
				audioStart.setSelected(false);
				audioStart.setTag(INIT_STATE);
				forceLayoutForButtomLayout(false);

				setListSelection(adapter.getCount() - 1, 0);
			}
		}, 100L);
	}

	/**
	 * 隐藏底部界面显示: 1 更多操作; 2 语音录音界面.
	 */
	private void hideButtomWholeLayout()
	{
		if (moreLayout.getVisibility() == View.VISIBLE)
		{
			moreLayout.setVisibility(View.GONE);
			forceLayoutForButtomLayout(true);

			emotionButton.setSelected(false);
			audioStart.setSelected(false);
			audioStart.setTag(INIT_STATE);

			setListSelection(adapter.getCount() - 1, 0);
		}
	}

	private void setListSelection(int pos, int top)
	{
		if (top <= 0)
		{
			listView.setSelection(pos);
		}
		else
		{
			listView.setSelectionFromTop(pos, top);
		}
	}

	private void switchSendMsgBtn(boolean show)
	{
		if (show)
		{
			audioStart.setVisibility(View.GONE);
			sendButton.setVisibility(View.VISIBLE);
			sendButton.setImageResource(R.drawable.send_selector);
		}
		else
		{
			audioStart.setVisibility(View.VISIBLE);
			sendButton.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void initializeData()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeComposition()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void clearData()
	{
		// TODO Auto-generated method stub

	}

	/**
	 * 初始化与语音相关的控件.
	 */
	private void initAudioRecordLayout()
	{
		/**
		 * 打开底部语音录音界面的按钮
		 */
		audioStart = (Button) findViewById(R.id.btn_audio_start);
		audioStart.setTag(INIT_STATE);
		audioStart.setOnClickListener(this);

		sendButton = (ImageView) findViewById(R.id.btn_chat_send);
		sendButton.setOnClickListener(this);

		// 录音父控件.
		audioLayout = (SoundWaveView) findViewById(R.id.record_audio);
		audioLayout.setRecordCallBack(new SoundWaveView.RecordCallBack()
		{
			@Override
			public void onEndRecord(boolean normal) {
				handleRecordEnd(normal);
			}

			@Override
			public void onTouchRegionChange(boolean in) {
				handleTouchRegionChange(in);
			}
		});

		// 聊天界面顶部提示text.
		recordPrompt = (TextView) findViewById(R.id.prompt_cancel_send);
		audioHintLayout = (LinearLayout) findViewById(R.id.audio_hint);
		speakHint = (TextView) findViewById(R.id.speak_hint);

		// 录音时用到的按钮.
		recordBtn = (ImageView) findViewById(R.id.btn_say_pressed);
		recordBtn.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (VoipFunc.getIns().isVoipCalling(true))
				{
					ToastUtil.showToast(mContext,
							getString(R.string.call_in_progress));
					return false;
				}

				startRecord();
				return false;
			}
		});

		// 提示录音时间.
		timePromptView = (TextView) findViewById(R.id.prompt_time);

	}

	private void forceLayoutForButtomLayout(boolean alignButtom)
	{
		final RelativeLayout.LayoutParams mLayoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		if (alignButtom)
		{
			mLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
					RelativeLayout.TRUE);
			bottomArea.setLayoutParams(mLayoutParams);
			bottomArea.requestFocus();
		}
		else
		{
			mLayoutParams.addRule(RelativeLayout.ABOVE, R.id.more_layout);
			bottomArea.setLayoutParams(mLayoutParams);
		}
	}

	/**
	 * Override onActivityResult
	 * 
	 * @param requestCode
	 *            Request Code
	 * @param resultCode
	 *            Result Code
	 * @param data
	 *            Intent
	 * @see android.app.Activity#onActivityResult(int, int,
	 *      android.content.Intent)
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (RESULT_OK != resultCode)
		{
			Logger.debug(TAG, "result not ok , result code = " + resultCode);
			ChatUtil.clearTempUmData(requestCode);
			return;
		}

		switch (requestCode)
		{
		case PROCESS_UM_MEDIA:
			ImFunc.getIns().handleChooseMedia(data, getChatType());
			break;
		default:
			break;
		}
	}

	/**
	 * 语音录音界面的开和关
	 */
	private void toggleAudioView()
	{
		if (!DeviceUtil.isEnableSave(this))
		{
			Logger.warn(TAG, "no support save");
			return;
		}

		int tag = (Integer) audioStart.getTag();
		if (tag == INIT_STATE || tag == KEYBOARD_STARTED)
		{
			openAudioLayout();
		}
		else
		{
			hideButtomWholeLayout();
			SoftInputUtil.showSoftInput(ChatActivity.this);
		}
	}

	/**
	 * 打开语音录音界面
	 */
	private void openAudioLayout()
	{

		SoftInputUtil.hideSoftInput(ChatActivity.this, editText);
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				moreLayout.setVisibility(View.VISIBLE);
				audioLayout.setVisibility(View.VISIBLE);
				editText.clearFocus();
				audioStart.setSelected(true);
				audioStart.setTag(AUDIO_STARTED);

				emotionButton.setSelected(false);
				moreOptsGridView.setVisibility(View.INVISIBLE);

				forceLayoutForButtomLayout(false);

				// if (View.VISIBLE == chatSettingLayout.getVisibility())
				// {
				// showSetting();
				// }

				setListSelection(adapter.getCount() - 1, 0);
			}
		}, 100L);
	}

	/**
	 * 开始录音
	 */
	private void startRecord()
	{
		startRecord = true;

		recordBtn.setBackgroundResource(R.drawable.voice_input_click);
		prepareSoundWave();

		adapter.stopAudio();
		startRecordTimeChange();

		handler.removeMessages(RECORD_TIMELIMIT_DELAY);

		// 开始录制的时候震动一下
		// DeviceUtil.vibratorUnlimited();

		String codec = ContactLogic.getIns().getMyOtherInfo()
				.getUmVoiceCodecs();

		if ("AMR-WB".equals(codec))
		{
			recordPath = UmUtil.createTempResPath(UmConstant.AMR);
		}
		else
		{
			recordPath = UmUtil.createTempResPath(UmConstant.WAV);
		}

		VoipFunc.getIns().startRecord(recordPath);

		DeviceUtil.setKeepScreenOn(this);
		audioHintLayout.setVisibility(View.VISIBLE);
		audioHintLayout.setBackgroundColor(getResources().getColor(
				R.color.main_conf_item_red));
		recordPrompt.setText(getString(R.string.slip_out_to_cancel_send));
		speakHint.setVisibility(View.INVISIBLE);
	}

	/**
	 * 停止语音录制
	 */
	private void stopRecord()
	{
		startRecord = false;

		stopSoundWave();
		timePromptView.setVisibility(View.GONE);
		btnGetHistoryMsg.setVisibility(View.VISIBLE);
		VoipFunc.getIns().stopRecord();

		DeviceUtil.releaseKeepScreen();
		stopRecordTimeChange();
	}

	/**
	 * 停止声音波纹效果。
	 */
	private void stopSoundWave()
	{
		audioLayout.invalidateView((int) SoundWaveView.INIT_VALUE);
	}

	/**
	 * 模拟声音波纹。
	 */
	private void prepareSoundWave()
	{
		int[] btnLocation1 = new int[2];
		recordBtn.getLocationInWindow(btnLocation1);
		int btnY = btnLocation1[1];

		int[] btnLocation2 = new int[2];
		audioLayout.getLocationInWindow(btnLocation2);

		Logger.debug(TAG, "x = " + btnLocation2[0] + "/y = " + btnLocation2[1]);
		audioLayout.setCx((recordBtn.getRight() + recordBtn.getLeft()) / 2
				- btnLocation2[0]);
		audioLayout.setCy((btnY + btnY + recordBtn.getHeight()) / 2
				- btnLocation2[1]);
		audioLayout.setMinRadius(recordBtn.getHeight() / 2);
		audioLayout.setMaxRadius((audioLayout.getHeight() + audioLayout
				.getWidth()) / 4);
	}

	/**
	 * 处理录音结束。
	 * 
	 * @param normal
	 *            是否正常结束。
	 * @return
	 */
	private boolean handleRecordEnd(boolean normal)
	{
		if (!startRecord)
		{
			stopSoundWave();
			return false;
		}

		recordBtn.setBackgroundResource(R.drawable.voice_input_selector);
		speakHint.setVisibility(View.VISIBLE);
		stopRecord();

		// 判断是否已滑动到输入框上面
		int time = timeCount / (1000 / VOICE_RECORD_TIME_RATE);
		if (!normal) {
			delRecordFile();
		}
		else if (time < 1) // 录音时间小于1秒.
		{
			audioHintLayout.setVisibility(View.VISIBLE);
			audioHintLayout.setBackgroundColor(getResources().getColor(
					R.color.chat_red));
			recordPrompt.setText(R.string.record_time_tooshort);
			delRecordFile();
		}
		else
		{
			ImFunc.getIns().prepareAudioToSend(recordPath, time, getChatType());
		}
		handler.sendEmptyMessageDelayed(RECORD_TIMELIMIT_DELAY, 500);

		return false;
	}

	/**
	 * 录音开始时， 处理时间的变化
	 */
	private void startRecordTimeChange()
	{
		stopSoundWave();
		timePromptView.setVisibility(View.VISIBLE);
		btnGetHistoryMsg.setVisibility(View.GONE);
		audioHintLayout.setVisibility(View.VISIBLE);

		timeCount = 0;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				int seconds = timeCount * VOICE_RECORD_TIME_RATE / ONE_SECOND;

				if (seconds >= ContactLogic.getIns().getMyOtherInfo()
						.getUmVoiceRecordLength())
				{
					handler.sendEmptyMessage(RECORD_TIMELIMIT_BEYOND);
				}

				handler.sendEmptyMessage(VOLUMEANDTIMECHANGE);

				timeCount++;
			}
		}, 0, VOICE_RECORD_TIME_RATE);

	}

	private void stopRecordTimeChange()
	{
		if (null != timer) {
			handler.removeMessages(VOLUMEANDTIMECHANGE);

			timer.purge();
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * 判断触摸区域改变。
	 * 
	 * @param in
	 *            是否在识别录音的触摸区域里面。
	 * @return
	 */
	private boolean handleTouchRegionChange(boolean in)
	{
		if (!startRecord) {
			return false;
		}

		// 判断滑动到上部
		if (!in)
		{
			audioHintLayout.setVisibility(View.VISIBLE);
			audioHintLayout.setBackgroundColor(getResources().getColor(
					R.color.chat_red));
			recordPrompt.setText(R.string.release_to_cancel_send);
		}
		else
		{
			audioHintLayout.setBackgroundColor(getResources().getColor(
					R.color.main_conf_item_red));
			recordPrompt.setText(R.string.slip_out_to_cancel_send);
		}

		return false;
	}

	private void delRecordFile()
	{
		// 删除之前录制的语音
		File file = new File(recordPath);
		if (file.exists())
		{
			if (!file.delete())
			{
				Logger.error(TAG, "delete file error");
			}
		}
	}

	/**
	 * 返回类型有两种: 联系人, 群.
	 */
	private int getChatType()
	{
	    //0-IM 5-Group
	    
	    Logger.info(CommonUtil.APPTAG, TAG + "| chatType:" + chatType);
	    
		return chatType;
	}

	private String getTimeBytimeCount(int timeCount)
	{
		int seconds = VOICE_RECORD_TIME_RATE * timeCount / ONE_SECOND;

		return DateUtil.getTimeString(seconds);
	}

	private void refreshWhenStop()
	{
		// 刷新界面显示
		recordBtn.setBackgroundResource(R.drawable.voice_input_normal);
		speakHint.setVisibility(View.VISIBLE);
		handler.sendEmptyMessage(RECORD_TIMELIMIT_DELAY);
	}
	
	 //上传、下载富媒体消息
    private void registerUM()
    {
        umBroadcast = new String[] {UmConstant.UPLOADPROCESSUPDATE,
        		UmConstant.UPLOADFILEFINISH,
        		UmConstant.DOWNLOADFILEFINISH,
        		UmConstant.DOWNLOADPROCESSUPDATE};

        umReceiver = new BaseReceiver()
        {
            @Override
            public void onReceive(String id, BaseData data)
            {
            	Log.d("UM", TAG + "------onReceive-------");
                if (data == null || !(data instanceof UmReceiveData))
                {
                    return;
                }
                
                Log.d("UM", TAG + " | action = " + id);
                UmReceiveData d = (UmReceiveData) data;
                if (UmConstant.UPLOADPROCESSUPDATE.equals(id)) //富媒体消息发送中
                {
                	ImFunc.getIns().handleUmMsgUploadProcessUpdate(d);
                }
                else if (UmConstant.DOWNLOADFILEFINISH.equals(id)
                    || UmConstant.UPLOADFILEFINISH.equals(id))
                {
                	
                	ImFunc.getIns().handleUmMsgUploadFileFinish(d);
                }
                else if (UmConstant.DOWNLOADPROCESSUPDATE.equals(id)) //富媒体消息接收中
                {
                	ImFunc.getIns().handleUmMsgDownloadProcessUpdate(d);
                }
            }
        };

        //注册媒体信息更新.
        UmFunc.getIns().registerBroadcast(umReceiver, umBroadcast);
    }

}
