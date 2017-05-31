package com.huawei.esdk.uc.im;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.CommonVariables;
import com.huawei.contacts.ContactLogic;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.topic.Topic;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.widget.ConfirmTitleDialog;
import com.huawei.module.topic.TopicCache;
import com.huawei.module.topic.WorkCircleFunc;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.module.um.UmUtil;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;
import com.huawei.utils.io.FileUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Video;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 类描述： 视频预览播放
 * 作者： l00170942
 * 创建时间：2014-5-22
 * 版权声明 : Copyright (C) 2013-2014 华为技术有限公司(Huawei Tech.Co.,Ltd)
 * 修改时间：2014-6-5
 */
public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback
{
	private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    //已下载
    public static final int FROM_ADAPTER_DOWNLOAD = 0x01;
    //未下载
    public static final int FROM_ADAPTER_NOT_DOWNLOAD = 0x02;
    //录制预览
    public static final int FROM_CHATACTIVITY = 0x04;

    public static final int VIEW_CIRCLE_PICTURE = 0x70;

    //更新播放进度
    private static final int UPDATE_PLAYER_PROGRESS = 0x08;
    //更新下载进度
    private static final int UPDATE_DOWNLOAD_PROGRESS = 0x10;
    //下载成功
    private static final int DOWNLOAD_SUCCESS = 0x20;
    //下载失败
    private static final int DOWNLOAD_FAILED = 0x40;
    //播放
    private static final int PLAY = 0x50;
    //隐藏标题栏
    private static final int HIDE_TITLE_BUTTOM = 0x60;
    //一秒钟
    private static final int ONE_SECOND = 1000;


    // 发送按钮
    private TextView sendBtn;

    //开始区域
    private RelativeLayout startLayout;

    //播放按钮
    private Button startBtn;

    // 显示视频的控件
    private SurfaceView playerSv;

    private SurfaceHolder surfaceHolder;

    private RelativeLayout seekbarLayout;

    //当前时间
    private TextView currTimeTv;

    // 播放进度条
    private ProgressBar playProgressBar;

    //总时间
    private TextView totalTimeTv;

    // 播放视频类
    private MediaPlayer player=null;

    //视频路径
    private String videoPath;

    //视频时间,初始化为1秒。
    private int videoTime = 1000;

    //缩略图
    private BitmapDrawable thumbnailDrawable;

    //当前视频是否已prepared过一次
    private boolean isPrepared;

    //当前视频是否播放完成，为了解决部分机型上player.getDuration()与player.getCurrentPosition()不一致所导致的进度条填不满的情况
    private boolean isCompleted;

    //下载进度条
    private ProgressBar downloadProgressBar;

    //播放区域
    private RelativeLayout playerLayout;

    //加载区域
    private RelativeLayout loadLayout;

    //加载文本
    private TextView loadTv;

    //加载图片
    private ImageView loadIv;

    //加载按钮
    private Button loadBtn;

    //传递状态
    private int status;

    //及时消息对象
    private InstantMessage im;

    //视频资源对象
    private MediaResource mediaRes;

//    private MediaMetadataRetriever mediaMetadataRetriever;

    //下载更新进度注册广播
    private String[] mediaBroadcast =
            {
                    UmConstant.DOWNLOADFILEFINISH,
                    UmConstant.DOWNLOADPROCESSUPDATE
            };

    private String[] circleBroadcast =
        {
            WorkCircleFunc.UPDATE_DATA,
            WorkCircleFunc.UPDATE_DATA_FAIL,
            WorkCircleFunc.UPDATE_PROCESS
        };

    //是否正在播放
    private boolean playing;

    //标题栏
    private View titleLayout;

    //开始按钮
    private ImageView startBtnBig;

    //视频大小
    private TextView videoSize;

    private int fromActivity;
    private boolean deleteFlag;
//    private ImageView doneImg;
    //Activity是否是从后台唤起
    private boolean isBackground = false;
    //Activity调入后台时视频是否正在播放的，如果是，从后台切回后自动继续播放。
//    private boolean isPlayingBeforeStop;

    private  int mVideoHeight=480;

    private  int mVideoWidth=640;
    private boolean focusChangePause;
    /**
     * 播放完成监听
     */
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            Logger.debug(TAG, "player onCompletion...");
            isCompleted = true;
            playing = false;
            //进度条重置
            if (null != player)
            {
                playProgressBar.setProgress(0);
            }
            //播放按钮可以见
            startLayout.setVisibility(View.VISIBLE);
            //设置缩略图
            startLayout.setBackgroundDrawable(thumbnailDrawable);
            startBtnBig.setVisibility(View.VISIBLE);
            startBtn.setBackgroundResource(R.drawable.icon_video_play_select);
            //播放时间重置
            currTimeTv.setText(formatTime(0));
            handler.removeMessages(HIDE_TITLE_BUTTOM);
            titleLayout.setVisibility(View.VISIBLE);
            seekbarLayout.setVisibility(View.VISIBLE);

            abandonAudioFocus();
            focusChangePause = false;
        }
    };
    private AudioManager.OnAudioFocusChangeListener focusChangeListener
            = new AudioManager.OnAudioFocusChangeListener()
    {
        @Override
        public void onAudioFocusChange(int focusChange)
        {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            {
                pausePlayer();
                focusChangePause = !playing;
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
            {
                if (focusChangePause)
                {
                    startPlayer();
                    focusChangePause = false;
                }
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
            {
                //do nothing
                pausePlayer();
                focusChangePause = !playing;
            }
        }
    };

    private void abandonAudioFocus()
    {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        manager.abandonAudioFocus(focusChangeListener);
    }

    /**
     * 视频准备完成监听
     */
    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            Logger.debug(TAG, "player prepare onPrepared...");

            //设置进度条长度以及视频长度按钮
            initTotalTime(player.getDuration());   //videoTime = player.getDuration();
            playProgressBar.setMax(player.getDuration());
            isPrepared = true;

            View root = findViewById(R.id.rlRoot);
            root.setBackgroundColor(Color.BLACK);

            focusChangePause = false;
        }
    };

    /**
     * 按钮点击事件监听
     */
    private View.OnClickListener listener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.right_btn:
                    //保存视频
                    if (status == FROM_ADAPTER_NOT_DOWNLOAD
                        || FROM_ADAPTER_DOWNLOAD == status
                        || status == VIEW_CIRCLE_PICTURE)
                    {
                        String oldPath = videoPath;
                        String path = FileUtil.saveVideoToMovie(oldPath,
                            Uri.parse(oldPath).getLastPathSegment());

                        String msg;

                        if (path != null)
                        {
                            //保存成功提示
                            msg = getString(R.string.prompt_video_save, path);
                            DeviceUtil.notifyAlbum(VideoPlayerActivity.this, path);
                        }
                        else
                        {
                            //保存失败
                            msg = getString(R.string.savefail);
                        }

                        ToastUtil.showToast(VideoPlayerActivity.this, msg);
                    }
                    //发送视频
                    else if (status == FROM_CHATACTIVITY)
                    {
                        //聊天界面发送视频 和 发表主题时预览已选择视频
                        if(isFromTopicSelected())
                        {
                            //确认删除对话框
                            ConfirmTitleDialog confirmDialog=new ConfirmTitleDialog(VideoPlayerActivity.this,
                                    getString(R.string.sure_delet_video));
                            confirmDialog.setRightButtonListener(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    videoPath = null;
                                    backOrSendVideo();//返回前一个页面
                                }
                            });
                            confirmDialog.show();
                        }
                        else
                        {
                            backOrSendVideo();//发送视频
                        }
                    }
                    break;
//                case R.id.right_img:
//                    //TODO 确认删除对话框
//                    videoPath=null;
//                    backOrSendVideo();
//                    break;
                //播放
                case R.id.btn_play:
                case R.id.play_btn_big:
                    //正在播放则暂停，否则播放
                    if (playing)
                    {
                        pausePlayer();
                    }
                    else
                    {
                        startPlayer();
                    }
                    focusChangePause = false;
                    break;
                //暂停
                case R.id.svPlayer:
                    handler.removeMessages(HIDE_TITLE_BUTTOM);
                    titleLayout.setVisibility(titleLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    seekbarLayout.setVisibility(seekbarLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    break;
                //下载视频
                case R.id.resume_load_btn:
                    loadTv.setText(getString(R.string.updating));
                    loadIv.setBackgroundResource(R.drawable.um_load_video_normal);
                    loadBtn.setVisibility(View.GONE);
                    downloadProgressBar.setVisibility(View.VISIBLE);

                    if (!reDownload())
                    {
                        handler.sendEmptyMessage(DOWNLOAD_FAILED);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Topic topic;

    /**
     * 重新下载。
     * @return
     */
    private boolean reDownload()
    {
        if (VIEW_CIRCLE_PICTURE == status)
        {
            return WorkCircleFunc.getIns().downloadPic(topic, mediaRes);
        }

        return downloadFile(im, mediaRes);
    }

    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                //开始播放
                case PLAY:
                    startPlayer();
                    break;
                //阴藏标题以及播放进度
                case HIDE_TITLE_BUTTOM:
                    titleLayout.setVisibility(View.GONE);
                    seekbarLayout.setVisibility(View.GONE);
                    break;
                //更新播放进度
                case UPDATE_PLAYER_PROGRESS:
                    if (player != null && !isCompleted)
                    {
                        int currPos = player.getCurrentPosition();
                        playProgressBar.setProgress(currPos);
                        currTimeTv.setText(formatTime(currPos));
                        //正在播放时每秒钟更新一次进度
                        if (player.isPlaying() && player.getDuration()-currPos > ONE_SECOND)
                        {
                            updateProgress(ONE_SECOND);
                        }
                    }
                    break;
                //更新下载进度条
                case UPDATE_DOWNLOAD_PROGRESS:
                    int tolSize = msg.getData().getInt(IntentData.TOL_SIZE);
                    int curSize = msg.getData().getInt(IntentData.CUR_SIZE);
                    loadTv.setText(getString(R.string.downloding)
                            + "\n(" + FileUtil.getShowFileSize(curSize)
                            + "/" + FileUtil.getShowFileSize(tolSize) + ")");
                    downloadProgressBar.setMax(tolSize);
                    downloadProgressBar.setProgress(curSize);
                    downloadProgressBar.setProgress(msg.getData().getInt(IntentData.CUR_SIZE));
                    break;
                //下载成功显示播放按钮，生成缩略图
                case DOWNLOAD_SUCCESS:
                    loadLayout.setVisibility(View.GONE);
                    playerLayout.setVisibility(View.VISIBLE);
                    showSaveBtn();
                    mediaRes = (MediaResource) msg.obj;
                    videoPath = status == VIEW_CIRCLE_PICTURE ? videoPath : mediaRes.getLocalPath();
                    startBtnBig.setVisibility(View.VISIBLE);
                    seekbarLayout.setVisibility(View.VISIBLE);
                    processThumbnail();
                    break;
                //下载失败显示失败图标，重新加载按钮
                case DOWNLOAD_FAILED:
                    refreshWhenFail(false);
                    break;
                default:
                    break;
            }
        }
    };

    private boolean isChoose = false;
    private boolean isPublic = false;

    /**
     * 获取界面初始化数据，注册广播接受
     */
    public void initializeData()
    {
        status = getIntent().getIntExtra(IntentData.STATUS, -1);

        int source = IntentData.SourceAct.IM_CHAT.ordinal();
        fromActivity = getIntent().getIntExtra(IntentData.FROM_ACTIVITY, source);
        isPublic = getIntent().getBooleanExtra(IntentData.ISPUBLIC, false);
        deleteFlag = getIntent().getBooleanExtra(IntentData.DELETE_FLAG, false);

        regMediaBroadcast();
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		initializeData();
		initializeComposition();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		clearData();
	}

	private boolean isFromTopicSelected()
    {
        return IntentData.SourceAct.TOPIC_LIST.ordinal() == fromActivity && deleteFlag;
    }

    /**
     * 界面初始化
     */
    public void initializeComposition()
    {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.video_player);
        setTitle(getString(R.string.um_video_look));

        TextView tvBack = (TextView)findViewById(R.id.btn_back);
        tvBack.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                UCAPIApp.getApp().popActivity(VideoPlayerActivity.this);
            }
        });

        //右上角按钮
        sendBtn = (TextView) findViewById(R.id.right_btn);
        sendBtn.setOnClickListener(listener);
        if(isFromTopicSelected())
        {
            setRightBtn(R.string.delete, listener);
        }
        else
        {
            setRightBtn(R.string.save, listener);
        }

        // 标题栏
        int backColor = getResources().getColor(R.color.title_video);
        titleLayout = findViewById(R.id.title_layout);
        titleLayout.setBackgroundColor(backColor);

        startLayout = (RelativeLayout) findViewById(R.id.rlPlayerStart);
        downloadProgressBar = (SeekBar) findViewById(R.id.pb_picturescan);
        startBtn = (Button) findViewById(R.id.btn_play);
        startBtnBig = (ImageView) findViewById(R.id.play_btn_big);
        playerSv = (SurfaceView) findViewById(R.id.svPlayer);
        videoSize = (TextView) findViewById(R.id.videoSize);
        playProgressBar = (ProgressBar) findViewById(R.id.sbVideo);
        playerLayout = (RelativeLayout) findViewById(R.id.rlPlayer);
        loadLayout = (RelativeLayout) findViewById(R.id.layout_load);
        loadTv = (TextView) findViewById(R.id.load_pic_process_txt);
        loadIv = (ImageView) findViewById(R.id.load_pic_logo);
        loadBtn = (Button) findViewById(R.id.resume_load_btn);
        loadIv.setBackgroundResource(R.drawable.um_load_video_normal);

        seekbarLayout = (RelativeLayout) findViewById(R.id.rlSeekBar);
        currTimeTv = (TextView) findViewById(R.id.tvCurrTime);
        totalTimeTv = (TextView) findViewById(R.id.tvTotalTime);

        //设置点击事件监听
        startBtn.setOnClickListener(listener);
        startBtnBig.setOnClickListener(listener);
        playerSv.setOnClickListener(listener);
        loadBtn.setOnClickListener(listener);

        // 取得holder
        surfaceHolder = playerSv.getHolder();
        // holder加入回调接口
        surfaceHolder.addCallback(this);
        // setType必须设置，要不出错.
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//        mediaMetadataRetriever = new MediaMetadataRetriever();

        initUI();

//        initReconnect(R.id.rlRoot);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (player == null)
            initializePlayer();
        int wid = player.getVideoWidth();
        int hig = player.getVideoHeight();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int mSurfaceViewWidth = dm.widthPixels;
        int mSurfaceViewHeight = dm.heightPixels;
        // 根据视频的属性调整其显示的模式
        Logger.debug(TAG, "SCREEN_ORIENTATION_LANDSCAPE video width(" + wid + ") or height(" + hig + ")");
        if (wid > mSurfaceViewWidth || hig > mSurfaceViewHeight)
        {
            // 计算出宽高的倍数
            float vWidth = (float) wid / (float) mSurfaceViewWidth;
            float vHeight = (float) hig / (float) mSurfaceViewHeight;
            // 获取最大的倍数值，按大数值进行缩放
            float max = Math.max(vWidth, vHeight);
            // 计算出缩放大小,取接近的正值
            wid = (int) Math.ceil((float) wid / max);
            hig = (int) Math.ceil((float) hig / max);
        }
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(wid,
                hig);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        playerSv.setLayoutParams(layoutParams);

    }

    /**
     * 下载时候或下载的视频呗删除后刷新。
     */
    private void refreshWhenVideoNotExist()
    {
        sendBtn.setVisibility(View.GONE);
        startBtnBig.setVisibility(View.GONE);
        seekbarLayout.setVisibility(View.GONE);
        playerLayout.setVisibility(View.GONE);
        loadLayout.setVisibility(View.VISIBLE);

        loadTv.setText(getString(R.string.updating));
        loadIv.setVisibility(View.VISIBLE);
    }

    /**
     * 根据不同的人口初始化界面
     */
    private void initUI()
    {
        Bundle b = getIntent().getExtras();
        switch (status)
        {
            //正在下载时刷新下载进度
            case FROM_ADAPTER_DOWNLOAD:
                refreshWhenVideoNotExist();
                if (b == null)
                {
                    return;
                }
                im = (InstantMessage) b.get(IntentData.IM);
                mediaRes = im.getMediaRes();
                if (!isInTransFile(im, mediaRes))
                {
                    boolean result = downloadFile(im, mediaRes);
                    if (!result)
                    {
                        handler.sendEmptyMessage(DOWNLOAD_FAILED);
                    }
                }
                break;
            case FROM_ADAPTER_NOT_DOWNLOAD:
                showSaveBtn();
                if(null != getIntent().getExtras())
                {
                    mediaRes = (MediaResource) getIntent().getExtras().get(IntentData.MEDIA_RESOURCE);
                    videoPath = mediaRes.getLocalPath();

                    showVideoPath();
                }
                break;
            //录制完成则生成缩略图，刷新文件大小
            case FROM_CHATACTIVITY:

                if(!isFromTopicSelected())
                {
                    if (IntentData.SourceAct.IM_CHAT.ordinal() == fromActivity)
                    {
                        sendBtn.setText(R.string.btn_done);
                    }
                    else
                    {
                        sendBtn.setText(R.string.btn_send);
                    }
                }

                videoPath = getIntent().getStringExtra(IntentData.VIDEO_PATH);
                isChoose = getIntent().getBooleanExtra(IntentData.CHOOSE, false);
                processThumbnail();
                refreshFileSize(videoPath);

                DeviceUtil.notifyAlbum(this, videoPath);
                break;
            case VIEW_CIRCLE_PICTURE:
//                intent.putExtra(IntentData.TOPIC_ID, topic.getTopicID());
//                intent.putExtra(IntentData.TOPIC_NO, topic.getId());
                String topicId = getIntent().getStringExtra(IntentData.TOPIC_ID);
                topic = TopicCache.getIns().getTopicByTopicId(topicId);
                if (topic == null)
                {
                    int id = getIntent().getIntExtra(IntentData.TOPIC_NO, 0);
                    topic = TopicCache.getIns().getFailTopicById(id);
                }

                MediaResource resource = null;
                if (topic != null)
                {
                    List<MediaResource> resourceList = topic.getContents();
                    if (resourceList != null && !(resourceList.isEmpty()))
                    {
                        resource = resourceList.get(0);
                    }
                }

                if (resource == null)
                {
                    Logger.error(TAG, " resource null, please check.");
                    return;
                }

                if (resource.getLocalPath() != null)
                {
                    showSaveBtn();
                    videoPath = resource.getLocalPath();
                }
                else
                {
                    WorkCircleFunc.getIns().registerBroadcast(circleReceiver, circleBroadcast);
                    //判断是否正在下载，正在下载则转圈。
                    videoPath = UmUtil.createCircleResPath(
                        topic.getOwnerID(), topic.getTopicId(), resource.getName());
                    if (!WorkCircleFunc.getIns().isTopicMediaInDownload(topic.getTopicId(),resource, false))
                    {
                        File file = new File(videoPath);
                        if (!file.exists())
                        {
                            refreshWhenVideoNotExist();
                            if (!WorkCircleFunc.getIns().downloadPic(topic, resource))
                            {
                                handler.sendEmptyMessage(DOWNLOAD_FAILED);
                            }
                            return;
                        }
                        else
                        {
                            showSaveBtn();
                        }
                    }
                    else
                    {
                        refreshWhenVideoNotExist();
                    }
                }
                showVideoPath();
                break;
            default:
                break;
        }
    }

    private boolean downloadFile(InstantMessage im, MediaResource mediaRes)
    {
    	//by lwx302895
//        if (isPublic)
//        {
//            String account;
//            if (CommonVariables.getIns().getUserAccount()
//                    .equalsIgnoreCase(im.getFromId()))
//            {
//                account = im.getToId();
//            }
//            else
//            {
//                account = im.getFromId();
//            }
//
//            UmFunc.getIns().downloadPublic(account, im, (int)im.getId(), mediaRes, false);
//            return true;
//        }
//
//        return UmFunc.getIns().downloadFile(im, mediaRes);
    	
    	if (isPublic)
        {

            UmFunc.getIns().downloadPublic(im, mediaRes, false);
            return true;
        }

        return UmFunc.getIns().downloadFile(im, mediaRes,false);
    }

    private boolean isInTransFile(InstantMessage im, MediaResource mediaRes)
    {
        if (isPublic)
        {
            return UmFunc.getIns().isPublicInTransFile(im.getId(), mediaRes.getMediaId(), false);
        }

        return UmFunc.getIns().isInTransFile(im.getId(), mediaRes.getMediaId(), false);
    }

    private void showVideoPath()
    {
        if (videoPath == null || !(new File(videoPath).exists()))
        {
            refreshWhenVideoNotExist();
            refreshWhenFail(true);
        }
        else
        {
            processThumbnail();
        }
    }

    /**
     * 发送失败的时候刷新界面
     * @param deleted
     */
    private void refreshWhenFail(boolean deleted)
    {
        loadIv.setBackgroundResource(R.drawable.um_load_video_fail);
        loadTv.setText(getString(R.string.um_load_video_fail));
        downloadProgressBar.setVisibility(View.GONE);
        if (deleted)
        {
            loadBtn.setVisibility(View.INVISIBLE);
        }
        else
        {
            loadBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 判断是否显示保存按钮。
     */
    private void showSaveBtn()
    {
        //禁止拷贝时隐藏保存按钮
        if (ContactLogic.getIns().getAbility().isAllowCopy())
        {
            sendBtn.setText(getString(R.string.save));
            sendBtn.setVisibility(View.VISIBLE);
        }
        else
        {
            sendBtn.setVisibility(View.GONE);
        }
    }

    /**
     * 刷新显示文件大小
     *
     * @param videoPath 文件路径
     */
    private void refreshFileSize(String videoPath)
    {
        if (TextUtils.isEmpty(videoPath))
        {
            Logger.debug(TAG, "video path is empty");
            return;
        }

        File file = new File(videoPath);
        if (!file.exists())
        {
            Logger.debug(TAG, "file not exist");
            return;
        }

        long length = file.length();
        videoSize.setText(FileUtil.getShowFileSize(length));
        videoSize.setVisibility(View.VISIBLE);

    }

    /**
     * 处理视频缩略图
     */
    private void processThumbnail()
    {
        thumbnailDrawable = new BitmapDrawable(ThumbnailUtils.createVideoThumbnail(videoPath,
                Video.Thumbnails.MINI_KIND));
        startLayout.setBackgroundDrawable(thumbnailDrawable);
         //宽高以手机竖屏为准
         setPreviewSize(thumbnailDrawable.getIntrinsicWidth(), thumbnailDrawable.getIntrinsicHeight());
    }

    /**
     * 关闭界面时注销广播，停止正在播放的视频
     */
    public void clearData()
    {
        UmFunc.getIns().unRegisterBroadcast(umReceiver, mediaBroadcast);
        WorkCircleFunc.getIns().unRegisterBroadcast(circleReceiver, circleBroadcast);

        if (player != null)
        {
            if (player.isPlaying())
            {
                player.stop();
            }
            player.release();
            player = null;
        }
    }

    /**
     * 格式化时间
     *
     * @param millis 毫秒数
     * @return 格式化后的时间串
     */
    private String formatTime(int millis)
    {
        return DateUtil.formatSimple(new Date(millis * 1L), DateUtil.FMT_MS);
    }

    /**
     * 初始化MediaPlayer对象
     */
    private void initializePlayer()
    {
        FileInputStream fis = null;
        try
        {
            player = new MediaPlayer();
            player.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener()
            {
                @Override
                public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height)
                {
                    Logger.debug(TAG, "width = " + width + "/height = " + height);
                    if (width == 0 || height == 0) {
                         Logger.debug(TAG, "invalid video width(" + width + ") or height(" + height + ")");
                        return;
                    }
                    setPreviewSize(width, height);

                }
            });

            initTotalTime(videoTime);
            player.setOnPreparedListener(onPreparedListener);
            player.setOnCompletionListener(onCompletionListener);

            // 设置流的类型
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 播放时保持屏幕常亮
            player.setScreenOnWhilePlaying(true);
            // 设置要预览的视频
//            player.setDataSource(videoPath);

            //由于视频目录移到程序目录，4.1以下版本都不能直接setDataSource路径播放
            File file = new File(videoPath);
            fis = new FileInputStream(file);
            player.setDataSource(fis.getFD());
            player.prepare();

//            mediaMetadataRetriever.setDataSource(file.getAbsolutePath());
        }
        catch (FileNotFoundException e)
        {
            Logger.error(TAG, e.toString());
        }
        catch (IOException e)
        {
            Logger.error(TAG, e.toString());
        }
        finally
        {
            try
            {
                if (null != fis)
                {
                    fis.close();
                }
            }
            catch (IOException e)
            {
                fis = null;
                Logger.error(TAG, e.toString());
            }
        }
    }

    private void initTotalTime(int mTime)
    {
        videoTime = mTime < 1000 ? 1000 : mTime;
        String totalTime = formatTime(videoTime);
        totalTimeTv.setText(totalTime);
    }

    private boolean requestAudioFocus()
    {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int durationHint;
        if (DeviceUtil.hasKitKat())
        {
            durationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
        }
        else
        {
            durationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
        }

        int result = manager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, durationHint);

        Logger.debug(TAG, "result = " + result);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * 开始播放
     */
    private void startPlayer()
    {
        try
        {
            if (player != null && !player.isPlaying())
            {
                requestAudioFocus();

                //视频未准备好时重新初始化
                if (!isPrepared)
                {
                    initializePlayer();
                }
                player.start();

                //启动计时，更新进度条
                updateProgress(0);

                playing = true;
                isCompleted = false;
                startLayout.setVisibility(View.GONE);
                startBtnBig.setVisibility(View.GONE);
                startBtn.setBackgroundResource(R.drawable.icon_video_pause_select);

                DeviceUtil.setKeepScreenOn(this);
            }
        }
        catch (Exception e)
        {
            Logger.error(TAG, e.getMessage());
        }
    }

    /**
     * 设置缩略图大小
     *  @param width
     * @param height
     */
    private void setPreviewSize(int width, int height)
    {
        if (width == 0)
        {
            Logger.error(TAG, "height = " + height);
            return;
        }

        float mRatio = 1.f * height / width;

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        Logger.debug(TAG, "metrics.widthPixels = " + metrics.widthPixels
            + "metrics.widthPixels = " + metrics.heightPixels);

        int reqWidth = getLess(metrics.widthPixels, metrics.heightPixels);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.setFixedSize(reqWidth, (int) (reqWidth * mRatio));

            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1)
            {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                int dimensionWidth;
                int dimensionHeight;
                try
                {
                    retriever.setDataSource(videoPath);

                    //这里的width与height的参照物因为不同的手机显示会不一致。iphone横屏发的图片在这里显示会有问题。
                    dimensionWidth =
                        Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    dimensionHeight =
                        Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

                    Logger.debug(TAG, "width = " + dimensionWidth);
                    Logger.debug(TAG, "height = " + dimensionHeight);
                    //注意这里的宽度其实就是手机最长的一边
                    float ratio;
                    if (dimensionWidth > dimensionHeight)
                    {
                        ratio = 1.f * dimensionWidth / dimensionHeight;
                    }
                    else
                    {
                        ratio = 1.f * dimensionHeight / dimensionWidth;
                    }

                }
                catch (NumberFormatException e)
                {
                    Logger.debug(TAG, "exception" + e.toString());
                }
                catch (IllegalArgumentException e)
                {
                    Logger.error(TAG, "IllegalArgumentException," + e.toString());
                }
            }*/
    }

    private int getLess(int length1, int length2)
    {
        return length1 > length2 ? length2 : length1;
    }

//    private int getLarge(int length1, int length2)
//    {
//        return length1 > length2 ? length1 : length2;
//    }


    /**
     * 暂停播放
     */
    private void pausePlayer()
    {
        try
        {
            if (player != null && player.isPlaying())
            {
                abandonAudioFocus();

                player.pause();
                startBtn.setBackgroundResource(R.drawable.icon_video_play_select);
                startBtnBig.setVisibility(View.VISIBLE);
                startLayout.setVisibility(View.VISIBLE);
                startLayout.setBackgroundDrawable(null);

                DeviceUtil.releaseKeepScreen();
                playing = false;

                //暂停时更新缩略图
//                thumbnailDrawable = new BitmapDrawable(mediaMetadataRetriever.getFrameAtTime(player.getCurrentPosition()));
            }
        }
        catch (Exception e)
        {
            Logger.error(TAG, e.getMessage());
        }

    }

    /**
     * 发送视频
     */
    private void backOrSendVideo()
    {
        Intent intent = new Intent();
        intent.putExtra(IntentData.VIDEO_PATH, videoPath);
        intent.putExtra(IntentData.VIDEO_TIME, videoTime);
        intent.putExtra(IntentData.CHOOSE, isChoose);
        intent.putExtra(IntentData.SEND_MESSAGE, true);
        setResult(RESULT_OK, intent);
        UCAPIApp.getApp().popActivity(this);
    }

    /**
     * 延迟更新进度
     *
     * @param delayMillis 延迟号毫秒数
     */
    private void updateProgress(int delayMillis)
    {
        Message msg = handler.obtainMessage(UPDATE_PLAYER_PROGRESS);
        handler.removeMessages(UPDATE_PLAYER_PROGRESS);
        handler.sendMessageDelayed(msg, delayMillis);
    }

    /**
     * Do Nothing
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Logger.debug(TAG, "format = " + format
            + "width = " + width + "/height = " + height);

    }

    /**
     * Surface Created
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            if (isFinishing())
            {
                Logger.info(TAG, "activity is finishing, will not continue.");
                return;
            }

            if (!isBackground || isCompleted)
            {
                //初始化播放器
                initializePlayer();
            }

            player.setDisplay(holder);

            //如果未下载则开始下载
            if (!isBackground && status == FROM_ADAPTER_NOT_DOWNLOAD)
            {
                handler.sendEmptyMessage(PLAY);

                handler.sendEmptyMessageDelayed(HIDE_TITLE_BUTTOM, 3000L);
            }

            isBackground = false;
        }
        catch (Exception e)
        {
            Logger.error(TAG, e.toString());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Logger.debug(TAG, "destroy");
        isBackground = true;

        pausePlayer();
        focusChangePause = false;
    }

    private BaseReceiver circleReceiver = new BaseReceiver()
    {
        @Override
        public void onReceive(String broadcastName, BaseData d)
        {
            if (!(d instanceof WorkCircleFunc.WorkCircleReceiveData))
            {
                return;
            }
            WorkCircleFunc.WorkCircleReceiveData data
                = (WorkCircleFunc.WorkCircleReceiveData)d;
            MediaResource resource = (MediaResource)data.getObj();
            String topicId = data.getTopicId();
            if (topicId != null && topic != null && topicId.equals(topic.getTopicId()))
            {
                if (WorkCircleFunc.UPDATE_DATA.equals(broadcastName) && resource != null)
                {
                    handler.sendMessage(handler.obtainMessage(DOWNLOAD_SUCCESS, resource));
                }
                else if (WorkCircleFunc.UPDATE_DATA_FAIL.equals(broadcastName))
                {
                    handler.sendEmptyMessage(DOWNLOAD_FAILED);
                }
                else if (WorkCircleFunc.UPDATE_PROCESS.equals(broadcastName))
                {
                    sendUpdateProcessMessage(data.getCurSize(), data.getTolSize());
                }
            }
        }
    };



    /**
     * 广播接受类
     */
    private BaseReceiver umReceiver = new BaseReceiver()
    {
        @Override
        public void onReceive(String ID, BaseData data)
        {
            if (data == null || !(data instanceof UmReceiveData))
            {
                return;
            }

            UmReceiveData d = (UmReceiveData) data;

            //非当前视频不处理
            if (!isCurrentVideo(d))
            {
                return;
            }

            //下载进度刷新
            if (ID.equals(UmConstant.DOWNLOADPROCESSUPDATE))
            {
                if (d.process != null)
                {
                    sendUpdateProcessMessage(
                        d.process.getCurSize(), d.process.getTotalSize());
                }
            }
            //下载结束
            else if (ID.equals(UmConstant.DOWNLOADFILEFINISH))
            {
                //下载成功
                if (UmReceiveData.FINISH_SUCCESS == d.status)
                {
                    handler.sendMessage(handler.obtainMessage(DOWNLOAD_SUCCESS, d.media));
                }
                //下载失败
                else if (UmReceiveData.FINISH_FAIL == d.status)
                {
                    handler.sendEmptyMessage(DOWNLOAD_FAILED);
                }
            }
        }
    };

    private void sendUpdateProcessMessage(int curSize, int tolSize)
    {
        if (tolSize <= 0)
        {
            return;
        }

        Message msg = new Message();
        msg.what = UPDATE_DOWNLOAD_PROGRESS;
        Bundle b = new Bundle();

        b.putInt(IntentData.CUR_SIZE, curSize);
        b.putInt(IntentData.TOL_SIZE, tolSize);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    /**
     * 判断是否是当前视频
     *
     * @param d 多媒体数据
     * @return 是当前视频是返回true，否则返回false
     */
    private boolean isCurrentVideo(UmReceiveData d)
    {
        //如果没有im，media对象；直接返回false。
        if (im == null || mediaRes == null)
        {
            return false;
        }

        //如果传输过来的msg为null，或者id与im的不一致，返回false。
        if (d.msg == null || d.msg.getId() != im.getId())
        {
            return false;
        }

        //如果媒体消息为null，或者媒体消息ID不一致，返回false
        return !(d.media == null || d.media.getMediaId() != mediaRes.getMediaId());
    }

    /**
     * 注销广播
     */
    private void regMediaBroadcast()
    {
        UmFunc.getIns().registerBroadcast(umReceiver, mediaBroadcast);
    }
    
    private void setRightBtn(int resId, OnClickListener onClickListener)
    {
        TextView rightBtn = (TextView) findViewById(R.id.right_btn);
        if (rightBtn != null)
        {
            rightBtn.setText(resId);
            rightBtn.setOnClickListener(onClickListener);
            rightBtn.setVisibility(View.VISIBLE);
        }
    }

}
