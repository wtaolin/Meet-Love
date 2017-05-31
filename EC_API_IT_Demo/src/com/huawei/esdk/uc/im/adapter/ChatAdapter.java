package com.huawei.esdk.uc.im.adapter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.common.os.EventHandler;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.dao.impl.InstantMessageDao;
import com.huawei.data.ConstGroup;
import com.huawei.data.Message;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.device.DeviceManager;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.function.VoipFunc;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;
import com.huawei.esdk.uc.im.PictureScanActivity;
import com.huawei.esdk.uc.im.VideoPlayerActivity;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.widget.CircleProgressBar;
import com.huawei.esdk.uc.widget.ConfirmTitleDialog;
import com.huawei.http.TransFileCallbackProcessor;
import com.huawei.module.um.SystemMediaManager;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.module.um.UmUtil;
import com.huawei.service.ServiceProxy;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;
import com.huawei.utils.img.ExifOriUtil;
import com.huawei.utils.io.FileUtil;
import com.huawei.voip.data.AudioMediaEvent;
import com.huawei.widget.FluentAdapter;
import com.huawei.widget.RecyclingRotateBitmapDrawable;

import android.annotation.SuppressLint;
import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChatAdapter extends FluentAdapter//SingleUpdateAdapter
{
    private static final String TAG = ChatAdapter.class.getSimpleName();
	
    private LayoutInflater mInflater;

    private ArrayList<InstantMessage> datalist = new ArrayList<InstantMessage>();
    private ArrayList<ItemType> layoutList = new ArrayList<ItemType>();

    private Calendar calendar;
    
    private Context mContext;
    
    private static final String PROCESS_SUFFIX = "%";
    
    private static final int SEND_FAIL = -1;
    
    private static final int MAX_LENGTH = 60;
    
    private boolean audioPlaying;

    private InstantMessage lastItem;

    private int audioHandle = -1;

    private static  final  int LOCAL_AUDIO_STOP = 1;


    Handler handler = new Handler()
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case LOCAL_AUDIO_STOP:
                    EventHandler.getIns().post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            stopAudio();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };
    
    /**
     * 一秒所代表的的毫秒数.
     */
    private static final long ONE_SECOND = 1000L;

    private String[] sipBroadcast = { VoipFunc.AUDIOSTOPNOTIFY };
    private void regBroadcast()
    {
        VoipFunc.getIns().registerBroadcast(sipReceiver, sipBroadcast);
    }


    private final BaseReceiver sipReceiver = new BaseReceiver()
    {
        @Override
        public void onReceive(String id, BaseData d)
        {
            if (VoipFunc.AUDIOSTOPNOTIFY.equals(id))
            {
                if (d == null)
                {
                    return;
                }
                if (!(d instanceof AudioMediaEvent))
                {
                    return;
                }
                int handle = ((AudioMediaEvent) d).getHandle();
                if (handle != audioHandle)
                {
                    return;
                }

                handler.sendEmptyMessage(LOCAL_AUDIO_STOP);
            }
        }
    };


    
    /**
     * 时间格式化
     */
    private SimpleDateFormat formatter;
    
    private ContactHeadFetcher headFetcher;
    
    public enum ItemType
    {
        MsgSendText, MsgSendAudio,
        MsgSendVideo, MsgSendImage, 
        MsgRecvText, MsgRecvAudio,
        MsgRecvVideo, MsgRecvImage, 
    }
    
    /** 存入的itemMap，供查找富媒体对应的item使用 */
    protected Map<String, InstantMessage> itemMap = new HashMap<String, InstantMessage>();
    
    public ChatAdapter(Context context, ArrayList<InstantMessage> datalist)
    {
        this.datalist = datalist;
        mInflater = LayoutInflater.from(context);
        mContext = context;
        formatter = new SimpleDateFormat();

        regBroadcast();
        
        layoutList = new ArrayList<ItemType>();
        layoutList.add(ItemType.MsgRecvText);
        layoutList.add(ItemType.MsgSendText);
        layoutList.add(ItemType.MsgRecvImage);
        layoutList.add(ItemType.MsgSendImage);
        layoutList.add(ItemType.MsgRecvAudio);
        layoutList.add(ItemType.MsgSendAudio);
        layoutList.add(ItemType.MsgRecvVideo);
        layoutList.add(ItemType.MsgSendVideo);
        
        headFetcher = new ContactHeadFetcher(context);

    }

    @Override
    public int getCount()
    {
        return datalist.size();
    }

    @Override
    public Object getItem(int position)
    {
        return datalist.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }
    
    @Override
    public int getItemViewType(int position)
    {
        InstantMessage item = (InstantMessage) getItem(position);
        
        int itemType = -1;
        
        if(isSendMsg(item))
        {
        	switch (item.getMediaType()) {
			case MediaResource.TYPE_NORMAL:
				itemType = ItemType.MsgSendText.ordinal();
				break;
				
			case MediaResource.MEDIA_PICTURE:
				itemType = ItemType.MsgSendImage.ordinal();
				break;
				
			case MediaResource.MEDIA_AUDIO:
				itemType = ItemType.MsgSendAudio.ordinal();
				break;
				
			case MediaResource.MEDIA_VIDEO:
				itemType = ItemType.MsgSendVideo.ordinal();
				break;

			default:
				break;
			}
        }
        else
        {
        	switch (item.getMediaType()) {
			case MediaResource.TYPE_NORMAL:
				itemType = ItemType.MsgRecvText.ordinal();
				break;
				
			case MediaResource.MEDIA_PICTURE:
				itemType = ItemType.MsgRecvImage.ordinal();
				break;
				
			case MediaResource.MEDIA_AUDIO:
				itemType = ItemType.MsgRecvAudio.ordinal();
				break;
				
			case MediaResource.MEDIA_VIDEO:
				itemType = ItemType.MsgRecvVideo.ordinal();
				break;

			default:
				break;
			}
        }

        return itemType;
    }

    @Override
    public int getViewTypeCount()
    {
        
    	return layoutList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        InstantMessage message = datalist.get(position);
        
        if(convertView == null)
        {
        	View tempView = getViewByMsgType(message.getMediaType(), 
        			isSendMsg(message));
            if (tempView != null)
            {
            	convertView = tempView;
            }
        }
        
        if(convertView != null)
        {
        	loadDataByMsgType(convertView, message);
        }

        return convertView;
    }
    
    /**
     * 上传进度更新
     */
    public void onUploadProgress(long msgId, int mediaId)
    {
    	InstantMessage item = peekItem(msgId, mediaId);
        if (item == null)
        {
            return;
        }

        updateSingle(datalist.indexOf(item));
    }
    
    /**
     * 下载进度更新
     */
    public void onDownloadProgress(long msgId, int mediaId, int process)
    {
    	InstantMessage item = peekItem(msgId, mediaId);
        if (item == null )
        {
            return;
        }

        updateSingle(datalist.indexOf(item));
    }

    /**
     * 根据消息类型获取要添加的view
     * 
     * @param msgType 消息类型
     * @param isSend 是发送的消息还是接收的消息
     * */
    private View getViewByMsgType(int msgType, boolean isSend){
    	View view = null;
        SimpleHolder viewHolder = null;
    	
    	switch (msgType) {
		case MediaResource.TYPE_NORMAL:
			
			if(isSend){
				view = mInflater.inflate(R.layout.chat_list_item_right, null);
	            viewHolder = getTextHolder(view);
			}else{
				view = mInflater.inflate(R.layout.chat_list_item_left, null);
	            viewHolder = getTextHolder(view);
			}
			
			break;
			
		case MediaResource.MEDIA_AUDIO:
			if(isSend){
				view = mInflater.inflate(R.layout.chat_item_send_audio, null);
	            viewHolder = getAudioHolder(view);
			}else{
				view = mInflater.inflate(R.layout.chat_item_recv_audio, null);
	            viewHolder = getAudioHolder(view);
			}
			break;
			
		case MediaResource.MEDIA_PICTURE:
			if(isSend){
				view = mInflater.inflate(R.layout.chat_item_send_image, null);
	            viewHolder = getImageHolder(view);
			}else{
				view = mInflater.inflate(R.layout.chat_item_recv_image, null);
	            viewHolder = getImageHolder(view);
			}
			break;
			
		case MediaResource.MEDIA_VIDEO:
			if(isSend){
				view = mInflater.inflate(R.layout.chat_item_send_video, null);
	            viewHolder = getVideoHolder(view);
			}else{
				view = mInflater.inflate(R.layout.chat_item_recv_video, null);
	            viewHolder = getVideoHolder(view);
			}
			break;

		default: //默认用文本消息显示
			if(isSend){
				view = mInflater.inflate(R.layout.chat_list_item_right, null);
	            viewHolder = getTextHolder(view);
			}else{
				view = mInflater.inflate(R.layout.chat_list_item_left, null);
	            viewHolder = getTextHolder(view);
			}
			break;
		}
    	
    	if (viewHolder == null)
        {
            viewHolder = new SimpleHolder();
        }

        if (view != null)
        {
            view.setTag(viewHolder);
        }
    	
    	return view;
    }
    
    private TextMessageHolder getTextHolder(View oldView)
    {
        TextMessageHolder holder = new TextMessageHolder();

        modifyMsgHolder(holder, oldView);

        holder.contentText = (TextView) oldView.findViewById(R.id.content);
        setTextSize(holder.contentText);

        holder.clickLayout = holder.contentText;
        return holder;
    }
    
    private void modifyMsgHolder(MessageHolder holder, View oldView)
    {
        holder.mainText = (TextView) oldView.findViewById(R.id.name);
        holder.secondaryText = (TextView) oldView.findViewById(R.id.time);
        holder.headImage = (ImageView) oldView.findViewById(R.id.head);
        holder.failTipImage = (ImageView) oldView.findViewById(R.id.fail_tip_head);
        holder.sendProgressbar = (ProgressBar) oldView.findViewById(R.id.send_progressbar);
    }
    
    private void setTextSize(TextView textView)
    {
        textView.setTextSize(SelfDataHandler.getIns().getSelfData().getImFontSize());
    }
    
    private SimpleHolder getImageHolder(View view)
    {
        ImageHolder holder = new ImageHolder();
        modifyMsgHolder(holder, view);
        holder.loadImageView = (ImageView) view.findViewById(R.id.imageContent);
        holder.backgroundNormal = (ImageView) view.findViewById(R.id.background_normal);
        holder.progressBar = (CircleProgressBar) view
                .findViewById(R.id.upload_progressbar);

        holder.reSendBtn = (TextView) view.findViewById(R.id.play_btn);
        holder.clickLayout = view.findViewById(R.id.framelayout);
        return holder;
    }
    
    private SimpleHolder getAudioHolder(View view)
    {
        AudioMessageHolder holder = new AudioMessageHolder();
        modifyMsgHolder(holder, view);

        holder.playImage = (ImageView) view.findViewById(R.id.chat_audio_image);

        holder.progressBar = (ProgressBar) view.findViewById(R.id.audio_progress);

        holder.timeLength = (TextView) view.findViewById(R.id.chat_audio_tx2);

        holder.clickLayout = view.findViewById(R.id.rl_chat_audio_play);
        return holder;
    }
    
    private SimpleHolder getVideoHolder(View view)
    {
        VideoViewHolder viewHolder = new VideoViewHolder();
        modifyMsgHolder(viewHolder, view);

        viewHolder.clickLayout = view.findViewById(R.id.video_layout);

        viewHolder.videoThumbnail = (ImageView) view.findViewById(R.id.thumbnail);
        viewHolder.playBtn = (TextView) view.findViewById(R.id.play_btn);
        viewHolder.timeTv = (TextView) view.findViewById(R.id.item_video_tv2);
        viewHolder.progressBar = (CircleProgressBar) view
                .findViewById(R.id.item_video_pb);

        return viewHolder;
    }


    private void loadDataByMsgType(View view, InstantMessage msg)
    {
    	SimpleHolder viewHolder = (SimpleHolder) view.getTag();
    	int type = msg.getMediaType();
    	
    	switch (type) {
		case MediaResource.TYPE_NORMAL: //文本消息
			loadTextMsg(msg, (TextMessageHolder)viewHolder);
			break;
			
		case MediaResource.MEDIA_PICTURE: //图片消息
			loadPictureMsg(msg, (ImageHolder)viewHolder);
			break;
			
		case MediaResource.MEDIA_AUDIO: //Audio消息
            loadAudioMsg(msg, (AudioMessageHolder)viewHolder);
			break;

		case MediaResource.MEDIA_VIDEO: //video消息
			loadVideoMsg(msg, (VideoViewHolder)viewHolder);
			break;
		default:
			break;
		}
    	
    }
    //加载头像、昵称、消息发送时间等数据
    private void loadHeadInfo(InstantMessage message, SimpleHolder viewHolder)
    {
    	//头像方法更改 by lwx302895
//		HeadPhotoUtil.getInstance().loadHeadPhoto(message.getFromId(), viewHolder.headImage);
    	headFetcher.loadHead(message.getFromId(), viewHolder.headImage);
    	
		viewHolder.mainText.setText(message.getFromId());
		Timestamp time = message.getTimestamp();
		SimpleDateFormat sd = new SimpleDateFormat("MM-dd HH:mm:ss");

		calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time.getTime());
		viewHolder.secondaryText.setText(sd.format(calendar.getTime()));
    }
    
    //加载文本消息数据
    private void loadTextMsg(InstantMessage message, TextMessageHolder viewHolder)
    {
    	loadHeadInfo(message, viewHolder);
    	viewHolder.contentText.setText(message.getContent());

	}
    
    //加载图片消息数据
    private void loadPictureMsg(InstantMessage message, ImageHolder viewHolder)
    {
    	loadHeadInfo(message, viewHolder);
    	
    	 MediaResource mediaRes = message.getMediaRes();
    	 
    	 itemMap.put(getCacheKey(message.getId(), mediaRes.getMediaId()), message);
    	 
    	 loadPictureView(message, mediaRes, viewHolder);
    }
    
    //加载Audio消息数据
    private void loadAudioMsg(InstantMessage message, AudioMessageHolder viewHolder)
    {
    	
    	loadHeadInfo(message, viewHolder);
    	
    	MediaResource mediaRes = message.getMediaRes();
    	
    	itemMap.put(getCacheKey(message.getId(), mediaRes.getMediaId()), message);
   	    
   	    loadAudioView(message, viewHolder);
    }
    
    public static class PlayButtonStatus
    {
        public static final int STATUS_UPLOAD_FAIL = 0;

        public static final int STATUS_DOWNLOAD_FAIL = 3;

        public static final int STATUS_SUCCESS = 1;

        public static final int STATUS_IN_PROGRESS = 2;
    }
    
    /**
     * 注册图片相关控件及其点击事件
     *
     * @param im
     * @param mediaRes
     * @return
     */
    private void loadPictureView(final InstantMessage im, final MediaResource mediaRes,
            final ImageHolder container)
    {
        String path = loadAndPath(im, mediaRes, container);
        picTransProgress(container, im, mediaRes);

        container.reSendBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int status = (Integer) container.reSendBtn.getTag();
                if (status == PlayButtonStatus.STATUS_SUCCESS)
                {
                	goToVideoOrPicActivity(im, mediaRes, false);
                }
                else if (status == PlayButtonStatus.STATUS_UPLOAD_FAIL)
                {
                    resend(container, im, mediaRes);
                }
                else if (status == PlayButtonStatus.STATUS_IN_PROGRESS)
                {
                    // TODO
                    cancelTransFile(im, mediaRes);
                    picTransProgress(container, im, mediaRes);
                }
            }
        });

        final String thumbnailPath = path;
        container.clickLayout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mediaRes.getResourceType() == MediaResource.RES_URL)
                {
                    if (preDownloadCheck(im, mediaRes))
                    {
                        return;
                    }
                }
                // 下载缩略图
                if (thumbnailPath == null || !new File(thumbnailPath).exists())
                {
                    downloadMediaRes(im, mediaRes);
                }

                // 启动预览界面
                startActivity(im, mediaRes);
            }
        });

        container.failTipImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (InstantMessage.STATUS_SEND_FAILED.equals(im.getStatus()))
                {
                    int process = getProgress(im, mediaRes);

                    if (process == SEND_FAIL)
                    {
                        showResendDialog(container, im, mediaRes);
                    }
                    else
                    {
                        cancelTransFile(im, mediaRes);
                        picTransProgress(container, im, mediaRes);
                    }
                }
            }
        });
    }
    
    protected String loadAndPath(InstantMessage im, MediaResource resource,
            ImageHolder container)
    {
        String path = null;
        if (MediaResource.RES_URL == resource.getResourceType())
        {
            path = UmUtil.getThumbnailPath(UmUtil.createResPath(
                    resource.getMediaType(), resource.getName(), null));
            handlePicture(path, container, true, false);

            if (!(new File(path).exists()))
            {
                // 此处如果下周图片资源很快失败的话，会导致登录与此处循环。
                downloadMediaRes(im, resource);
            }
        }
        else if (MediaResource.RES_LOCAL == resource.getResourceType())
        {
            //这个路径在手机4.4.2上不对，无法显示缩略图
//            path = resource.getThumbnailPath();
            path = resource.getLocalPath();

            handlePicture(path, container, false, isSendMsg(im));
        }
        return path;
    }
    
    /**
     * 处理图片显示， 如果图片不存在，则显示默认图片
     *
     * @param path
     * @param holder
     * @param isUrl         判断是否是URl
     * @param isSend        判断是否是发送消息
     */
    private void handlePicture(String path, ImageHolder holder,
            boolean isUrl, boolean isSend)
    {
        holder.backgroundNormal.setVisibility(View.GONE);
        if (isHuaweiUcNoUmAbility())
        {
            holder.loadImageView.setImageResource(getUmDisableBackground());
            return;
        }

        if (path == null)
        {
            holder.loadImageView.setImageResource(getPicNormalBackground(isSend));
            return;
        }

        if (new File(path).exists())
        {
            loadPicture(holder.loadImageView, holder.backgroundNormal, path, isSend);
            return;
        }

        if (isUrl)
        {
            holder.loadImageView.setImageResource(getPicNormalBackground(isSend));
        }
        else
        {
            holder.loadImageView.setImageResource(getPicDisableBackground(isSend));
        }

    }
    
    private void loadPicture(ImageView loadImageView, ImageView background, String path,
            boolean isSend)
    {
        Drawable drawable = SystemMediaManager.getIns().getDrawable(path);
        if (drawable != null)
        {
            loadImageView.setImageDrawable(drawable);
            return;
        }

        BitmapWorkerTask task = new BitmapWorkerTask(loadImageView,
                mContext.getResources());
        final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(),
                getBitmap(path), task);
        loadImageView.setImageDrawable(asyncDrawable);

        if (background != null)
        {
            background.setVisibility(View.VISIBLE);
            background.setImageResource(getPicNormalBackground(isSend));
        }

        task.execute(path);
    }
    
    private Bitmap getBitmap(String path)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int ori = ExifOriUtil.getExifOrientation(path);
        Bitmap bitmap;
        if (ori % 180 == 0)
        {
            bitmap = Bitmap.createBitmap(getLength(options.outWidth),
                    getLength(options.outHeight), Bitmap.Config.ALPHA_8);
        }
        else
        {
            bitmap = Bitmap.createBitmap(getLength(options.outHeight),
                    getLength(options.outWidth), Bitmap.Config.ALPHA_8);
        }
        return bitmap;
    }
    
    private int getLength(int length)
    {
        return length <= 0 ? 1 : length;
    }
    
    protected void cancelTransFile(InstantMessage im, MediaResource mediaResource)
    {
        UmFunc.getIns().cancelTransFile(im.getId(), mediaResource.getMediaId(), false);
    }
    
    private void picTransProgress(ImageHolder holder, InstantMessage im,
            MediaResource mediaRes)
    {
        picTransProgress(holder.failTipImage, holder.progressBar, holder.reSendBtn,
                holder.sendProgressbar, im, mediaRes);
    }
    
    /**
     * 图片上传进度及界面标志显示处理
     *
     * @param failBtn
     * @param progressBar
     * @param reSendBtn
     * @param im
     * @param mediaResource
     */
    private void picTransProgress(ImageView failBtn, CircleProgressBar progressBar,
            TextView reSendBtn, ProgressBar progress, InstantMessage im,
            MediaResource mediaResource)
    {
        transProgress(failBtn, progressBar, reSendBtn, progress, im, mediaResource, false);
    }
    
    /**
     * 图片与视频的进度处理
     *
     * @param failBtn
     * @param progressBar
     * @param playBtn
     * @param im
     * @param mediaResource
     * @param isVideo
     */
    private void transProgress(ImageView failBtn, CircleProgressBar progressBar,
            TextView playBtn, ProgressBar sendProgress, InstantMessage im,
            MediaResource mediaResource, boolean isVideo)
    {
        String status = im.getStatus();

        playBtn.setText("");

        boolean isSend = isSendMsg(im);
        boolean isUrl = mediaResource.getResourceType() == MediaResource.RES_URL;
        if (status.equals(InstantMessage.STATUS_SEND) && isUrl)
        {
            failBtn.setVisibility(View.GONE);
            sendProgress.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            playBtn.setVisibility(View.GONE);
            playBtn.setTag(PlayButtonStatus.STATUS_IN_PROGRESS);
            playBtn.setBackgroundResource(R.drawable.video_play_small_selector);
        }
        else if (status.equals(InstantMessage.STATUS_SEND)
                || status.equals(InstantMessage.STATUS_SEND_FAILED)
                || (isUrl && isNotSendSuccess(status))) //
        {
            int process = getProgress(im, mediaResource);
            if (process == -1)
            {
                sendProgress.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                if (status.equals(InstantMessage.STATUS_SEND_FAILED) && isSend)
                {
                    failBtn.setVisibility(View.VISIBLE);
                    playBtn.setVisibility(View.VISIBLE);
                }
                else
                {
                    failBtn.setVisibility(View.GONE);
                    playBtn.setVisibility(View.GONE);
                }
                failBtn.setImageResource(R.drawable.selector_icon_pic_upload_fail);
                playBtn.setBackgroundResource(R.drawable.um_resend_selector);
                playBtn.setTag(!isSend ? PlayButtonStatus.STATUS_DOWNLOAD_FAIL
                        : PlayButtonStatus.STATUS_UPLOAD_FAIL);
            }
            else
            {
                // 失败按钮隐藏
                failBtn.setVisibility(View.GONE);
                failBtn.setImageResource(R.drawable.selector_icon_pic_upload_cancel);

                // 无限旋转的小圈圈
                sendProgress.setVisibility(isSend ? View.VISIBLE : View.GONE);

                // 转圈显示发送比率
                progressBar.setVisibility((isSend || isVideo) ? View.VISIBLE : View.GONE);
                process = process >= 95 ? 95 : process;
                progressBar.setProgress(process);

                // 发送中显示发送比率process
                playBtn.setVisibility((isVideo || isSend) ? View.VISIBLE : View.GONE);
                playBtn.setText(process + PROCESS_SUFFIX);
                playBtn.setBackgroundResource(0);
                playBtn.setTag(PlayButtonStatus.STATUS_IN_PROGRESS);
            }
        }
        else
        {
            failBtn.setVisibility(View.GONE);
            sendProgress.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);

            // 视频，而且不是url的时候，隐藏视频按钮（转发可能直接转发url）
            playBtn.setVisibility((isVideo && !isUrl) ? View.VISIBLE : View.GONE);
            playBtn.setTag(PlayButtonStatus.STATUS_SUCCESS);
            playBtn.setBackgroundResource(R.drawable.video_play_small_selector);
        }
    }
    
    /**
     * 下载媒体资源.
     *  @param im
     * @param mediaRes
     */
    private void downloadMediaRes(InstantMessage im, MediaResource mediaRes)
    {
        if (!DeviceManager.isNetActive())
        {
            Logger.warn(TAG, "net error, not download thumbnail.");
            return;
        }

        ServiceProxy proxy = UCAPIApp.getApp().getService();
        boolean isPic = mediaRes.getMediaType() == MediaResource.MEDIA_PICTURE;
        //注释 bylwx302895
        if (proxy != null /*&& proxy.isConnected() */&& checkDownload(im, mediaRes, isPic))
        {
            boolean result = UmFunc.getIns().downloadFile(im, mediaRes, isPic);

            Logger.debug(TAG, "picture download result : " + result);
        }
    }
    
    private boolean checkDownload(InstantMessage im, MediaResource mediaRes, boolean isPic)
    {
        return isTransFile(im, mediaRes, isPic) || downloadFile(im, mediaRes, isPic);
    }

    protected boolean isTransFile(InstantMessage im, MediaResource mediaRes, boolean isPic)
    {
        return UmFunc.getIns().isInTransFile(im.getId(), mediaRes.getMediaId(), isPic);
    }
    
    protected boolean downloadFile(InstantMessage im, MediaResource mediaRes, boolean isPic)
    {
        return UmFunc.getIns().downloadFile(im, mediaRes, isPic);
    }
    
    /**
     * 获取当前资源的传输进度
     *
     * @param im
     * @param mediaRes
     * @return 如果返回值是null 说明不在下载，其他返回值应在0-100之间
     */
    protected int getProgress(InstantMessage im, MediaResource mediaRes)
    {
        return UmFunc.getIns().getProgress(im.getId(), mediaRes.getMediaId(),false);
    }
    
    protected static class SimpleHolder
    {
        /**
         * 名称 /为消息时显示名字.
         */
        public TextView mainText;

        /**
         * 时间 /正常消息显示时间
         */
        public TextView secondaryText;
        
        /**
         * 头像
         */
        public ImageView headImage;

        
     }
    
    public static class MessageHolder extends SimpleHolder
    {
        /**
         * 失败提示
         */
        public ImageView failTipImage; 

        public ProgressBar sendProgressbar; 
        /**
         * 长按layout
         */
        public View clickLayout;

        /**
         * 选中框，每个数据项对应都有
         */
        public ImageView checkBox; //目前demo里面没用到
    }
    
    
    protected class ImageHolder extends MessageHolder
    {
        public ImageView backgroundNormal;

        public ImageView loadImageView;

        public CircleProgressBar progressBar;

        public TextView reSendBtn;
    }
    
    private static final class TextMessageHolder extends MessageHolder
    {
        /**
         * 消息内容
         */
        public TextView contentText;

    }

    private class AudioMessageHolder extends MessageHolder
    {
        // 播放按钮图标
        public ImageView playImage;

        // 下载时候显示进度按钮
        public ProgressBar progressBar;

        // 时长
        public TextView timeLength;
    }

    private class VideoViewHolder extends MessageHolder
    {

        public ImageView videoThumbnail;

        public TextView playBtn;

        public TextView timeTv;

        public CircleProgressBar progressBar;
    }
    
    private static class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable>
    {
        private final Resources resources;

        private String data;

        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView, Resources resources)
        {
            imageViewReference = new WeakReference<ImageView>(imageView);

            this.resources = resources;
        }

        @SuppressLint("NewApi")
		@Override
        protected BitmapDrawable doInBackground(String... params)
        {
            data = params[0];

            BitmapDrawable drawable = SystemMediaManager.getIns().getDrawable(data);
            if (drawable == null)
            {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferQualityOverSpeed = false;
                Bitmap bitmap = null;

                try
                {
                    bitmap = BitmapFactory.decodeFile(data, options);
                }
                catch (OutOfMemoryError error)
                {
                    Logger.error(TAG, "error = " + error.toString());
                }

                if (bitmap != null)
                {
                    int ori = ExifOriUtil.getExifOrientation(data);

                    drawable = new RecyclingRotateBitmapDrawable(resources, bitmap, ori);
                    SystemMediaManager.getIns().addDrawable(data, drawable);
                }
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable bitmap)
        {
            final ImageView imageView = getAttachedImageView();
            if (bitmap != null && imageView != null)
            {
                imageView.setImageDrawable(bitmap);
            }
        }

        private ImageView getAttachedImageView()
        {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask)
            {
                return imageView;
            }

            return null;
        }

    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView)
    {
        if (imageView != null)
        {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable)
            {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static class AsyncDrawable extends BitmapDrawable
    {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap,
                BitmapWorkerTask bitmapWorkerTask)
        {
            super(resources, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
                    bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask()
        {
            return bitmapWorkerTaskReference.get();
        }
    }
    
    protected void goToVideoOrPicActivity(InstantMessage im, MediaResource mediaRes, boolean isPublic)
    {
        boolean isUrl = mediaRes.getResourceType() == MediaResource.RES_URL;

        Intent intent = new Intent();
        if (mediaRes.getMediaType() == MediaResource.MEDIA_PICTURE)
        {
            intent.setClass(mContext, PictureScanActivity.class);
            intent.putExtra(IntentData.STATUS, isUrl ? PictureScanActivity.DOWNLOAD_VIEW
                    : PictureScanActivity.COMMON_VIEW);
        }
        else if (mediaRes.getMediaType() == MediaResource.MEDIA_VIDEO)
        {
            intent.setClass(mContext, VideoPlayerActivity.class);
            intent.putExtra(IntentData.STATUS,
                    isUrl ? VideoPlayerActivity.FROM_ADAPTER_DOWNLOAD
                            : VideoPlayerActivity.FROM_ADAPTER_NOT_DOWNLOAD);
        }
        intent.putExtra(IntentData.ISPUBLIC, isPublic);

        intent.putExtra(IntentData.IM, im);
        intent.putExtra(IntentData.MEDIA_RESOURCE, mediaRes);
        mContext.startActivity(intent);
    }
    
    /**
     * 判断是否为自己发送的消息。
     *
     * @param im 聊天消息
     * @return
     */
    protected boolean isSendMsg(InstantMessage im)
    {
        if (im == null)
        {
            return false;
        }

        return CommonVariables.getIns().getUserAccount().equalsIgnoreCase(im.getFromId());
    }
    
    private boolean isNotSendSuccess(String status)
    {
        return !(status.equals(InstantMessage.STATUS_SEND_SUCCESS));
    }
    
    /**
     * 判断是否华为uc下没有um权限。
     * @return
     */
    private boolean isHuaweiUcNoUmAbility()
    {
        return CommonVariables.getIns().isHWUC()
                && !ContactLogic.getIns().getAbility().isUmAbility();
    }
    
    private void showResendDialog(final InstantMessage msg)
    {
        // final Activity context = ActivityStack.getIns().getCurActivity();
        ConfirmTitleDialog resendDialog = new ConfirmTitleDialog(mContext,
                R.string.prompt_resend_message);

        OnClickListener rightListener = new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                resend(mContext, msg);
            }
        };

        resendDialog.setRightButtonListener(rightListener);
        resendDialog.show();
    }
    
    private void showResendDialog(final MessageHolder holder, final InstantMessage im,
            final MediaResource mediaRes)
    {
    	ConfirmTitleDialog dialog = new ConfirmTitleDialog(mContext, R.string.prompt_resend_message);

        OnClickListener rightListener = new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                resend(holder, im, mediaRes);
            }
        };

        dialog.setRightButtonListener(rightListener);
        dialog.show();
    }
    
    /**
     * 重新发送消息
     * @param msg
     */
    protected void resend(Context context, InstantMessage msg)
    {
        if (!SelfDataHandler.getIns().getSelfData().isConnect())
        {
            ToastUtil.showToast(context, R.string.im_disabled);
            return;
        }
        // send chat message again
        if (msg.getType() == Message.IM_GROUPCHAT)
        {
            ImFunc.getIns().sendGroupMsg(
                    msg.getToId(),
                    msg.getMsgType() == RecentChatContact.GROUPCHATTER ? ConstGroup.FIXED
                            : ConstGroup.DISCUSSION, msg, true, msg.getMediaType());
        }
        else
        {
            ImFunc.getIns().sendMessage(msg, true,
                    msg.getMediaType());
        }
    }
    
    protected boolean resend(final InstantMessage im, final MediaResource mediaRes)
    {
        ImFunc.getIns().updateStatus(im, InstantMessage.STATUS_SEND);
        boolean result = UmFunc.getIns().uploadFile(im, mediaRes);

        return result;
    }
    
    private void resend(MessageHolder holder, final InstantMessage im,
            final MediaResource mediaRes)
    {
        if (mediaRes.getResourceType() == MediaResource.RES_URL)
        {
            Logger.debug(TAG, "url, send by normal pass.");

            resend(mContext, im);
            return;
        }

        //如果没有发送成功，直接返回。
        if (!resend(im, mediaRes))
        {
            return;
        }

        if (mediaRes.getMediaType() == MediaResource.MEDIA_VIDEO)
        {
            VideoViewHolder curVideo;
            if(holder instanceof VideoViewHolder)
            {
                curVideo = (VideoViewHolder) holder;
            }
            else
            {
                return;
            }
//            videoTransProgress(curVideo, im, mediaRes);
        }
        else if (mediaRes.getMediaType() == MediaResource.MEDIA_PICTURE)
        {
            ImageHolder curImage = null;
            if(holder instanceof ImageHolder)
            {
                curImage = (ImageHolder) holder;
            }
            else
            {
                return;
            }
            picTransProgress(curImage, im, mediaRes);
        }
        else if (mediaRes.getMediaType() == MediaResource.MEDIA_AUDIO)
        {
            notifyDataSetChanged();
        }
    }
    
    private boolean preDownloadCheck(InstantMessage im, MediaResource mediaRes)
    {
        return storageCheck() || showDataLimitTip(im, mediaRes);
    }

    private boolean storageCheck()
    {
        if (!FileUtil.isSaveFileEnable())
        {
            ToastUtil.showToast(mContext, R.string.feedback_sdcard_prompt);
            return true;
        }
        return false;
    }

    /**
     * 无wifi网络且文件大小超过1MB时弹框提示.
     *
     * @param im
     * @param mediaRes 媒体消息
     * @return
     */
    private boolean showDataLimitTip(final InstantMessage im, final MediaResource mediaRes)
    {
        boolean hasWifi = DeviceUtil.isWifiConnect();
        if (hasWifi)
        {
            return false;
        }

        // 3G情况下提示size大小1M。
        if (mediaRes.getSize() > UmConstant.DOWNLOAD_PROMPT_SIZE)
        {
            ConfirmTitleDialog dialog = new ConfirmTitleDialog(mContext,
                    R.string.file_too_large_tip);
            dialog.setRightText(R.string.res_continue);
            dialog.setLeftText(R.string.btn_cancel);

            dialog.setRightButtonListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    goToVideoOrPicActivity(im, mediaRes, false);
                }
            });
            dialog.show();
            return true;
        }
        return false;
    }
    
    /**
     * 获取默认的正常图片
     *
     * @param isSelf
     * @return
     */
    protected int getPicNormalBackground(boolean isSelf)
    {
        return isSelf ? R.drawable.um_right_pic_normal : R.drawable.um_left_pic_normal;
    }

    private int getUmDisableBackground()
    {
        return DeviceManager.isChinese() ? R.drawable.um_pic_disable_ch : R.drawable.um_pic_disable_eg;
    }
    
    /**
     * 获取图片无效背景图片
     *
     * @param isSelf 是否是发送消息
     * @return
     */
    private int getPicDisableBackground(boolean isSelf)
    {
        return isSelf ? R.drawable.um_right_pic_disable : R.drawable.um_left_pic_disable;
    }
    
    
    /**
     * 注册语音相关控件及其点击事件
     *
     * @param item 存储item数据的item。
     */
    private void loadAudioView(final InstantMessage item, final AudioMessageHolder holder)
    {
        final InstantMessage im = item;
        final MediaResource mediaRes = im.getMediaRes();

        holder.timeLength.setText(DateUtil.getTimeString(mediaRes.getDuration()));
        optViewLength(holder, mediaRes.getDuration());

        audioTransProgress(holder, item);

        holder.clickLayout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                audioLayoutClick(item, holder);
            }
        });

        holder.failTipImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (InstantMessage.STATUS_SEND_FAILED.equals(im.getStatus()))
                {
                    showResendDialog(holder, im, mediaRes);
                }
            }
        });
    }

    /**
     * 语音的layout点击。
     *
     * @param item
     * @param holder
     */
    private void audioLayoutClick(InstantMessage item, AudioMessageHolder holder)
    {
        final InstantMessage im = item;
        final MediaResource mediaRes = im.getMediaRes();

        // 判断是否是自己
        String from = im.getFromId();
        final boolean isSelf = CommonVariables.getIns().getUserAccount()
                .equalsIgnoreCase(from);

        if (mediaRes.getResourceType() == MediaResource.RES_LOCAL)
        {
            if (!preparePlay(item))
            {
                return;
            }

            // 获取文件路径
            String pathFile = mediaRes.getLocalPath();
            if (pathFile == null)
            {
                Logger.debug(TAG, "path file is empty.");
                return;
            }

            // 播放声音
            audioHandle = playSound(mContext, pathFile);
            if (audioHandle == -1)
            {
                Logger.debug(TAG, "play sound fail, please check.");
                return;
            }

            // 更新状态为已读；并更新
            updateAudioRead(isSelf, im);

            audioPlaying = true;

            lastItem = item;

            notifyDataSetChanged();
        }
        else if (mediaRes.getResourceType() == MediaResource.RES_URL)
        {
            // 先检测是否可以存储
            if (storageCheck())
            {
                return;
            }

            // 在下载中 则置相应的值
            if (checkDownload(im, mediaRes, false))
            {
                if (!preparePlay(item))
                {
                    return;
                }

                lastItem = item;

                notifyDataSetChanged();
            }
            else
            {
                // 下载失败，刷新界面显示。
                if (holder != null && holder.progressBar != null)
                {
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.playImage.setVisibility(View.GONE);
                    holder.progressBar.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            notifyDataSetChanged();
                        }
                    }, 500L);
                }
            }

        }
    }
    
    // 计算语音消息在界面显示的长度
    private void optViewLength(AudioMessageHolder holder, int duration)
    {

        int timeLength = duration > MAX_LENGTH ? MAX_LENGTH : duration;

        StringBuffer str = new StringBuffer(" ");
        for (int i = 0; i < timeLength; i++)
        {
            str.append(" ");
        }
        holder.timeLength.append(str.toString());
    }
    
    /**
     * audio进度更新
     *
     * @param holder 语音消息的view载体
     * @param item
     */
    private void audioTransProgress(AudioMessageHolder holder, InstantMessage item)
    {
        InstantMessage im = item;
        MediaResource res = im.getMediaRes();

        // 判断是否是自己
        String from = im.getFromId();
        final boolean isSelf = CommonVariables.getIns().getUserAccount()
                .equalsIgnoreCase(from);

        String status = im.getStatus();

        // 设置默认值： 重发按钮与进度隐藏， 播放按钮显示。
        holder.failTipImage.setVisibility(View.GONE);
        holder.sendProgressbar.setVisibility(View.GONE);
        holder.playImage.setVisibility(View.VISIBLE);
        if (holder.progressBar != null)
        {
            holder.progressBar.setVisibility(View.GONE);
        }

        if (lastItem != null && lastItem == item)
        {
            // 如果是已经点击的语音消息，正在播放则显示播放
            if (audioPlaying)
            {
                animPlayBtn(holder.playImage, isSelf);
            }
            else
            {
                // 如果暂时未播放，则显示进度框。
                holder.playImage.setVisibility(View.GONE);
                if (holder.progressBar != null)
                {
                    holder.progressBar.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            // 如果没有点击过该item，设置播放的背景图片。
            holder.playImage.setImageResource(R.drawable.left_miss);
        }

        if (status.equals(InstantMessage.STATUS_SEND_FAILED))
        {
            // 语音上传进度
            if (getProgress(im, res) == UmConstant.NOT_DOWNLOAD)
            {
                // 发送失败的消息，显示发送失败重发按钮
                holder.failTipImage.setVisibility(View.VISIBLE);
                holder.failTipImage
                        .setImageResource(R.drawable.selector_icon_pic_upload_fail);
            }
        }
        else if (status.equals(InstantMessage.STATUS_SEND))
        {
            holder.sendProgressbar.setVisibility(View.VISIBLE);
        }

        // 消息未读时，显示未读红点
        if (InstantMessage.STATUS_AUDIO_UNREAD.equals(status)
                || InstantMessage.STATUS_UNREAD.equals(status))
        {
            holder.failTipImage.setImageResource(R.drawable.um_unread_audio_notify);
            holder.failTipImage.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 为本次播放做准备。
     *
     * @param item
     * @return
     */
    private boolean preparePlay(InstantMessage item)
    {
        if (audioPlaying)
        {
            // action !此判断操作要放在前面，
            // 因为stopAudio里面把lastItem置为null
            if (lastItem == item)
            {
                stopAudio();
                return false;
            }

            stopAudio();
        }
        return true;
    }
    
    /**
     * 停止语音
     */
    public void stopAudio()
    {
//        releaseSensor();

        VoipFunc.getIns().stopSound();
        resetAudioView();
    }
    
    /**
     * 播放声音
     * @param context
     * @param path
     * @return
     */
    private int playSound(Context context, String path)
    {
        Logger.debug(TAG, "path = " + path);

        if (path == null)
        {
            Logger.error(TAG, "path error null.");
            return -1;
        }

        if (!(new File(path).exists()))
        {
            ToastUtil.showToast(context, R.string.audio_fail);
            return -1;
        }

        // 设置屏幕感光
//        registerSensor();

        return VoipFunc.getIns().playSound(path, 1);
    }
    
    /**
     * 更新语音消息为未读状态。
     *
     * @param isSelf
     * @param im
     */
    protected void updateAudioRead(boolean isSelf, final InstantMessage im)
    {
        if (isSelf || InstantMessage.STATUS_READ.equals(im.getStatus()))
        {
            return;
        }

        im.setStatus(InstantMessage.STATUS_READ);
        InstantMessageDao.update(im, InstantMessageDao.STATUS,
                InstantMessage.STATUS_READ);
    }

    /**
     * 重置语音的view显示
     */
    private void resetAudioView()
    {
        if (audioHandle == -1)
        {
            return;
        }

        audioPlaying = false;
        audioHandle = -1;

        lastItem = null;
        notifyDataSetChanged();
        
    }
    
    private void animPlayBtn(ImageView playImage, boolean isSelf)
    {
        playImage.setImageResource(getAudioPlayImage(isSelf));
        final AnimationDrawable ad = (AnimationDrawable) playImage.getDrawable();
        ad.start();
    }
    
    private int getAudioPlayImage(boolean isSelf)
    {
        return isSelf ? R.drawable.right_audio_play_selector
                : R.drawable.left_audio_play_selector;
    }
    
    private void loadVideoMsg(final InstantMessage item, VideoViewHolder viewHolder)
    {
        if (item == null || item.getMediaRes() == null)
        {
            return;
        }

        MediaResource mediaRes = item.getMediaRes();
        itemMap.put(getCacheKey(item.getId(), mediaRes.getMediaId()), item);

//        if (supportLongClick)
//        {
//            viewHolder.playBtn.setOnLongClickListener(handleOnLongClickListener(item));
//        }

        loadVideoView(item, mediaRes, viewHolder);
        loadHeadInfo(item, viewHolder);
    }
    
    /**
     * 加载视频到view.
     *
     * @param im
     * @param mediaRes
     */
    private void loadVideoView(final InstantMessage im, final MediaResource mediaRes,
            final VideoViewHolder holder)
    {
        // 存入map对象中，供回调接口中查找对应消息
        holder.timeTv.setText(format(new Date(mediaRes.getDuration() * ONE_SECOND),
                DateUtil.FMT_MS));

        loadVideoThumbNail(im, mediaRes, holder);

        // 当前video下载、上传状态的更新
        videoTransProgress(holder, im, mediaRes);

        holder.playBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int status = (Integer) holder.playBtn.getTag();
                if (status == PlayButtonStatus.STATUS_SUCCESS)
                {
                    startActivity(im, mediaRes);
                }
                else if (status == PlayButtonStatus.STATUS_UPLOAD_FAIL)
                {
                    resend(holder, im, mediaRes);
                }
                else if (status == PlayButtonStatus.STATUS_IN_PROGRESS)
                {
                    // cancelTrans();
                    cancelTransFile(im, mediaRes);
                    videoTransProgress(holder, im, mediaRes);
                }
            }
        });

        holder.clickLayout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                boolean isUrl = mediaRes.getResourceType() == MediaResource.RES_URL;
                if (isUrl)
                {
                    // 没有下载中的时候，才需要检测并提示。
                    if (!isTransFile(im, mediaRes, false))
                    {
                        if (preDownloadCheck(im, mediaRes))
                        {
                            return;
                        }
                    }
                }

                startActivity(im, mediaRes);
            }
        });

        holder.failTipImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int process = getProgress(im, mediaRes);

                if (process == SEND_FAIL)
                {
                    showResendDialog(holder, im, mediaRes);
                }
            }
        });
    }
    
    private void loadVideoThumbNail(InstantMessage im, MediaResource mediaRes, VideoViewHolder holder)
    {
        if (isHuaweiUcNoUmAbility())
        {
            holder.videoThumbnail.setImageResource(getUmDisableBackground());
            holder.timeTv.setVisibility(View.INVISIBLE);
            return;
        }

        // 检测文件是否存在
        if (mediaRes.getResourceType() == MediaResource.RES_URL)
        {
            holder.videoThumbnail.setImageResource(R.drawable.um_left_video_normal);
        }
        else if (mediaRes.getResourceType() == MediaResource.RES_LOCAL)
        {
            Bitmap bitmap = null;
            if (!TextUtils.isEmpty(mediaRes.getLocalPath()))
            {
                File file = new File(mediaRes.getLocalPath());
                if (file.exists())
                {
                    bitmap = mediaRes.getThumbnail();
                }
            }

            if (bitmap != null)
            {
                holder.videoThumbnail.setImageBitmap(bitmap);
            }
            else
            {
                holder.videoThumbnail
                        .setImageResource(getVideoDisableBackground(isSendMsg(im)));
            }
        }
    }
    
    private void videoTransProgress(VideoViewHolder holder, InstantMessage im,
            MediaResource mediaRes)
    {
        videoTransProgress(holder.failTipImage, holder.progressBar,
                holder.playBtn, holder.sendProgressbar, im, mediaRes);
    }
    
    private void videoTransProgress(ImageView failBtn, CircleProgressBar progressBar,
            TextView playBtn, ProgressBar progress, InstantMessage im,
            MediaResource mediaResource)
    {
        transProgress(failBtn, progressBar, playBtn, progress, im, mediaResource, true);
    }
    
    protected void startActivity(InstantMessage im, MediaResource mediaRes)
    {
        if (isHuaweiUcNoUmAbility())
        {
            Logger.debug(TAG, "hwuc and not have umability. not start the activity.");
            return;
        }

        goToVideoOrPicActivity(im, mediaRes, false);
    }
    
    /**
     * 将format函数独立在这里
     * 避免每次使用的时候new SimpleDateFormat。
     * SimpleDateFormat的初始化是很耗时间的。
     * 而且format函数只在ui线程使用，不会出现异步调用问题。
     * @param date
     * @param pattern
     * @return
     */
    private String format(Date date, String pattern)
    {
        synchronized (formatter) // 加同步，避免报fority警告。
        {
            formatter.applyPattern(pattern);

            return formatter.format(date);
        }
    }
    
    /**
     * 获取视频无效背景图片
     *
     * @param isSelf 是否是发送消息
     * @return
     */
    private int getVideoDisableBackground(boolean isSelf)
    {
        return isSelf ? R.drawable.um_right_video_disable
                : R.drawable.um_left_video_disable;
    }
    
    /**
     * 设置数据.
     *
     * //@param messages 聊天消息更新.
     */
    public void setMessageData(InstantMessage message)
    {
        if (message == null)
        {
            return;
        }

        if (message != null)
        {
            datalist.add(message);
        }

        notifyDataSetChanged();
        
    }
    
    public void onTransFinish(UmReceiveData d, String transType)
    {
        if (d == null || d.media == null || d.msg == null)
        {
            Logger.warn(TAG, "something error!");
            return;
        }

        if (d.media.getMediaType() == MediaResource.MEDIA_AUDIO)
        {
            audioTransFinish(d, transType);
            return;
        }

        InstantMessage item = poolItem(d.msg.getId(), d.media.getMediaId());
        if (item == null)
        {
            return;
        }

        if (d.status == UmReceiveData.FINISH_FAIL)
        {
            if (d.statusCode == TransFileCallbackProcessor.STATUS_403)
            {
                handleStatusCode403(d);
            }

            // 上传失败时需将msg状态置为failed;
            // 此种情况只在 发送后退回到主界面再进入时需要。
            if (UmConstant.UPLOADFILEFINISH.equals(transType))
            {
                item.setStatus(InstantMessage.STATUS_SEND_FAILED);
            }
            // 删除操作中会出现越界
            if (datalist.contains(item))
            {
                updateSingle(datalist.indexOf(item));
            }
            return;
        }

        if (UmConstant.UPLOADFILEFINISH.equals(transType))
        {
            itemMap.remove(getCacheKey(d.msg.getId(), d.media.getMediaId()));
            item.setStatus(InstantMessage.STATUS_SEND_SUCCESS);
        }
        else
        {
            // 非缩略图，才更新数据。
            if (!d.isThumbNail)
            {
                item.setContent(d.msg.getContent());

                MediaResource resource = item.getMediaRes();

                //Attention: 判断是否一个内存对象，注意不能用equals。是一个对象时不需要刷新
                if (resource != null && resource != d.media)
                {
                    resource.initResource(d.media.getResourceType(), d.media.getLocalPath());
                }
            }
        }

        updateSingle(datalist.indexOf(item));
    }

    /**
    * uploadHolders用到的key
    */
   protected String getCacheKey(long msgId, int mediaId)
   {
       return String.valueOf(msgId) + String.valueOf(mediaId);
   }
   
   private InstantMessage peekItem(long msgId, int mediaId)
   {
       String key = getCacheKey(msgId, mediaId);
       return itemMap.get(key);
   }

   private InstantMessage poolItem(long msgId, int mediaId)
   {
       String key = getCacheKey(msgId, mediaId);
       return itemMap.remove(key);
   }
   
   /**
    * 语音数据传输完成。
    *
    * @param d Um通知消息。
    * @param transType
    */
   private void audioTransFinish(UmReceiveData d, String transType)
   {
       if (d.status == UmReceiveData.FINISH_FAIL)
       {
           handleAudioTransFail(d.msg.getId(), d.media.getMediaId(), d.statusCode,
                   transType);
           return;
       }

       InstantMessage item = poolItem(d.msg.getId(), d.media.getMediaId());
       if (item == null)
       {
           return;
       }

       // 声音内容下载完后应更新界面。
       if (UmConstant.UPLOADFILEFINISH.equals(transType))
       {
           item.setStatus(InstantMessage.STATUS_SEND_SUCCESS);
       }
       else
       {
           item.setContent(d.msg.getContent());
           item.setMediaRes(d.media);

           // 收到的消息是要播放的消息，则播放。
           if (lastItem == item)
           {
               boolean isSelf = isSendMsg(item);
               audioPlaying = playAudio(d.msg.getId(), d.media.getMediaId(),
                       d.media.getLocalPath());

               updateAudioRead(isSelf, item);
           }
       }

       notifyDataSetChanged();
   }
   
   /**
    * 1 语音消息下载失败时，需要恢复本地的变量
    * 2 语音下载失败时，需要置变量。
    * @param id         消息id
    * @param mediaId    媒体消息id
    * @param statusCode
    * @param transType 传输类型
    */
   private void handleAudioTransFail(long id, int mediaId, int statusCode,
           String transType)
   {
       if (lastItem == null) // 发送失败，没有点击item。不用刷新页面；直接返回。
       {
           return;
       }

       InstantMessage msg = lastItem;
       if (msg == null)
       {
           return;
       }

       MediaResource res = msg.getMediaRes();
       if (res == null)
       {
           return;
       }

       if (msg.getId() != id || res.getMediaId() != mediaId)
       {
           return;
       }

       if (UmConstant.UPLOADFILEFINISH.equals(transType))
       {
           msg.setStatus(InstantMessage.STATUS_SEND_FAILED);
       }
       else
       {
           lastItem = null;
       }

       if (statusCode == TransFileCallbackProcessor.STATUS_403)
       {
           show403Toast();
       }
   }
   
   /**
    * 处理语音下载失败的其他情况。
    *
    * @param d
    */
   private void handleStatusCode403(UmReceiveData d)
   {
       if (d.isThumbNail)
       {
           return;
       }

       show403Toast();
   }
   
   private void show403Toast()
   {
       ToastUtil.showToast(mContext, R.string.file_limit);
   }
   
   /**
    * 播放语音消息
    *
    * @param msgId
    * @param mediaId
    * @param localPath 语音文件存放路径
    * @return 播放成功，返回true
    */
   public boolean playAudio(final long msgId, final int mediaId, String localPath)
   {
       if (lastItem == null)
       {
           return false;
       }

       if (lastItem == null) // 消息内容不存在，返回。
       {
           return false;
       }

       MediaResource media = lastItem.getMediaRes();
       if (media == null) // 富媒体资源不存在，返回。
       {
           return false;
       }

       long audioMsgId = lastItem.getId();
       int audioMediaId = media.getMediaId();
       if (msgId != audioMsgId || mediaId != audioMediaId)
       {
           return false;
       }

       audioHandle = playSound(mContext, localPath);

       Logger.debug(TAG, "audioHandle = " + audioHandle);
       return audioHandle != -1;
   }
   
   public void setDataList(ArrayList<InstantMessage> list)
   {
	   if(list == null)
	   {
	       return;   
	   }
	   
	   if(datalist != null)
	   {
		   datalist.clear();
		   datalist = null;
		   datalist = list;
	   }
	   else
	   {
		   datalist = list;
	   }
   }
}
