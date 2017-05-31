package com.huawei.esdk.uc.im;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import com.huawei.common.CommonVariables;
import com.huawei.common.ui.ZoomImageView;
import com.huawei.contacts.ContactLogic;
import com.huawei.dao.impl.PublicAccountMsgDao;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.entity.RecentChatContact;
import com.huawei.data.publicno.message.PubAccInstantMessage;
import com.huawei.data.publicno.message.PublicAccountMsg;
import com.huawei.data.topic.Topic;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.ems.publicservice.data.MsgContent;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ImFunc;
import com.huawei.esdk.uc.im.adapter.CirclePictureScanAdapter;
import com.huawei.esdk.uc.im.adapter.ImageAdapter;
import com.huawei.esdk.uc.im.adapter.MultiSendAdapter;
import com.huawei.esdk.uc.im.adapter.PictureScanAdapter;
import com.huawei.esdk.uc.im.adapter.SimplePictureScanAdapter;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.FileUtil;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.widget.ConfirmTitleDialog;
import com.huawei.esdk.uc.widget.ProcessDialog;
import com.huawei.module.topic.TopicCache;
import com.huawei.module.um.ImageRetriever;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.SystemMediaManager;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmUtil;
import com.huawei.utils.StringUtil;
import com.huawei.utils.img.BitmapUtil;
import com.huawei.utils.img.ExifOriUtil;
import com.huawei.utils.img.PhotoUtil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 图片预览界面
 */
public class PictureScanActivity extends BaseActivity
        implements ViewPager.OnPageChangeListener
{
	private static final String TAG = PictureScanActivity.class.getSimpleName();
	
    private static final String DEVIDER = " / ";

    private static final String TAB = " ";

    /**
     * 本地文件, 预览
     */
    public static final int COMMON_VIEW = 1;

    /**
     * 选择图片或拍照后，预览
     */
    public static final int COMMON_VIEW_UNSEND = 2;

    /**
     * 点击下载，预览
     */
    public static final int DOWNLOAD_VIEW = 3;

    /**
     * 查看自己头像
     */
    public static final int VIEW_HEAD = 4;

    /**
     * 查看公众号Logo
     */
    public static final int VIEW_LOGO = 6;

    public static final String PUBLIC_NO_NUM = "publicNoNum";

    /**
     * 查看同事圈的照片
     */
    public static final int VIEW_CIRCLE_PICTURE = 5;

    private static final int MAX_IMAGE_COUNT = 9;

    private String picturePath = "";

    private String newPath;

    private int status;

    private int pictureIndex;

    private TextView doneBtn;

    //    private ImageView doneImg;
    private int resId;

    private View titleLayout;

    private View rootView;

    private PhotoUtil photoUtil;

    private Bitmap bitmap;

    private boolean isChoose = false;

    private int fromActivity;

    private int topicPicSelected;

    private boolean selectedPreview;

    private ViewPager viewPager;

    private MultiSendAdapter multiSendAdapter;

    private LinearLayout imageCounterLayout;

    private List<ImageView> countPointList;

    /**
     * 图片显示的imageView
     */
    private ZoomImageView bigHead;

    private ArrayList<MediaRetriever.Item> selectedPath = null;

    private View buttomLayout;

    private ImageView buttomCheck;

    public void initializeData()
    {
        status = getIntent().getIntExtra(IntentData.STATUS, 0);

        int source = IntentData.SourceAct.IM_CHAT.ordinal();
        fromActivity = getIntent()
                .getIntExtra(IntentData.FROM_ACTIVITY, source);
        topicPicSelected = getIntent()
                .getIntExtra(IntentData.TOPIC_PIC_SELECTED, 0);
        selectedPreview = getIntent()
                .getBooleanExtra(IntentData.SELECTED_PREVIEW, false);
//代码注释 by lwx302895
//        photoUtil = new PhotoUtil(this);
    }

    public void initializeComposition()
    {
        setContentView(R.layout.picture_scan_list);

        //根节点
        rootView = findViewById(R.id.picture_scan_root_list);

        //初始化title栏显示。
        titleLayout = findViewById(R.id.title_layout);
        titleLayout.setBackgroundColor(
                getResources().getColor(R.color.title_video));
        if (status == COMMON_VIEW || status == DOWNLOAD_VIEW)
        {
            titleLayout.setVisibility(View.INVISIBLE);
        }
        else
        {
            titleLayout.setVisibility(View.VISIBLE);
        }
        setTitle(getString(R.string.um_pric_look));

        //右上角按钮
        doneBtn = (TextView) findViewById(R.id.right_btn);
        doneBtn.setOnClickListener(listener);
        if (!isFromTopicSelected())
        {
            buttomLayout = findViewById(R.id.buttom_layout);
            buttomCheck = (ImageView) findViewById(R.id.chooseCheck);
        }

        if (DeviceUtil.hasHoneycomb())
        {
            bigHead = (ZoomImageView) findViewById(R.id.big_head);
        }
        else
        {
            bigHead = (ZoomImageView) findViewById(R.id.big_head1);
        }
        bigHead.setMaxScaleMultiple(3.f);
        bigHead.setMinScaleMultiple(0.3f);

        viewPager = (ViewPager) findViewById(R.id.picture_viewpager);
        imageCounterLayout = (LinearLayout) findViewById(
                R.id.image_counter_layout);
        initByStatus();

//        initReconnect(R.id.picture_scan_root_list);
    }

   /* @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation ==Configuration.ORIENTATION_LANDSCAPE )
        {
            int selectedPos = getIntent().getIntExtra(ShowAllPicActivity.PICTURE_SELECTED, 0);
            viewPager.setCurrentItem(selectedPos);
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ChatActivity.PROCESS_UM_MEDIA && resultCode == RESULT_OK)
        {
            int selectedPos =  data.getIntExtra(ShowAllPicActivity.PICTURE_SELECTED, 0);
            viewPager.setCurrentItem(selectedPos);
        }
    }

    /**
     * 通过状态来初始化页面。
     */
    private void initByStatus()
    {
        //查看头像
        if (status == VIEW_HEAD || status == VIEW_LOGO)
        {
            viewHead(status);
            return;
        }

        //发送图片
        if (status == COMMON_VIEW_UNSEND)
        {

            prepareViewForSend();
            return;
        }

        if (status == VIEW_CIRCLE_PICTURE)
        {
            prepareCirclePictureShow();
            return;
        }

        prepareViewForOther();
    }

    /**
     * 初始化同事圈照片显示的页面
     */
    private void prepareCirclePictureShow()
    {
        titleLayout.setVisibility(View.GONE);
        buttomLayout.setVisibility(View.GONE);
        imageCounterLayout.setVisibility(View.VISIBLE);

        //初始化图片显示界面
        String topicId = getIntent().getStringExtra(IntentData.TOPIC_ID);
        Topic topic = TopicCache.getIns().getTopicByTopicId(topicId);
        if (topic == null)
        {
            int id = getIntent().getIntExtra(IntentData.TOPIC_NO, 0);
            topic = TopicCache.getIns().getFailTopicById(id);
        }

        if (topic == null)
        {
            Logger.error(TAG, "Don't find topic, quit.");
            UCAPIApp.getApp().popActivity(this);
            return;
        }

        int pos = getIntent().getIntExtra(IntentData.POSITION, 0);
        final CirclePictureScanAdapter scanAdapter = new CirclePictureScanAdapter(
                topic, this);
        scanAdapter.setData(topic.getContents());
        viewPager.setAdapter(scanAdapter);
        if (scanAdapter.isCanSave(pos))
        {
            showSaveBtn();
        }
        else
        {
            doneBtn.setVisibility(View.GONE);
        }

        viewPager.setCurrentItem(pos);
        viewPager.setOnPageChangeListener(this);
        setMyTitle(scanAdapter.getCount(), pos + 1);
        CirclePictureScanAdapter adapter = (CirclePictureScanAdapter) viewPager
                .getAdapter();
        if (adapter != null)
        {
            picturePath = adapter
                    .getPath(viewPager.getCurrentItem());
        }
        scanAdapter.registerDownLoadListener(
                new SimplePictureScanAdapter.PictureListener()
                {
                    @Override
                    public void onDownSuccess(int position)
                    {
                        //do nothing
                        if (viewPager.getCurrentItem() == position)
                        {
                            showSaveBtn();
                        }
                    }

                    @Override
                    public void onClick()
                    {
                    	UCAPIApp.getApp().popActivity(PictureScanActivity.this);
                    }

                    @Override
                    public boolean onLongClick()
                    {
                        return onLongClickImage(picturePath);
                    }
                }
        );

        int count = topic.getContents().size();
        countPointList = new ArrayList<ImageView>();
        if (count > 0 && count <= MAX_IMAGE_COUNT)
        {
            for (int i = 0; i < count; i++)
            {
                ImageView countPoint = (ImageView) LayoutInflater.from(this)
                        .inflate(R.layout.count_point, null);
                imageCounterLayout.addView(countPoint);
                countPointList.add(i, countPoint);
            }
        }

        if (countPointList.size() >= pos)
        {
            countPointList.get(pos).setSelected(true);
        }
    }

    private boolean onLongClickImage(final String picturePath)
    {
        if (!ContactLogic.getIns().getAbility().isAllowCopy())
        {
            return false;
        }
        showSaveImageDialog(picturePath);

        return true;
    }

    //保存图片的功能，先注释
    private void showSaveImageDialog(final String picturePath)
    {
//        final List<Object> data = new ArrayList<Object>();
//        data.add(getString(R.string.save));
//
//        final SimpleListDialog dialog = new SimpleListDialog(this, data);
//        dialog.setOnItemClickListener(new AdapterView.OnItemClickListener()
//        {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                    int position, long id)
//            {
//                dialog.dismiss();
//                if (data.get(position).equals(getString(R.string.save)))
//                {
//                      savePicture(picturePath);
//                }
//            }
//        });
//        dialog.show();
    }

    /**
     * 准备其他的页面
     * 1 DOWNLOAD_VIEW
     * 2 COMMON_VIEW
     */
    private void prepareViewForOther()
    {
        if (null == getIntent().getExtras())
        {
            Logger.error(TAG, "not have extra!");
            return;
        }
        InstantMessage im = (InstantMessage) getIntent().getExtras()
                .get(IntentData.IM);
        boolean isPublic = getIntent().getExtras()
                .getBoolean(IntentData.ISPUBLIC);

        //初始化图片显示界面
        final PictureScanAdapter scanAdapter = new PictureScanAdapter(this, im);
        viewPager.setAdapter(scanAdapter);
        scanAdapter.setPublic(isPublic);

        setMyTitle(scanAdapter.getCount(), -1);
        PictureScanAdapter sa = (PictureScanAdapter) viewPager
                .getAdapter();
        if (sa != null)
        {
            InstantMessage tempInsMessage = sa.getMessage(0);
            if (tempInsMessage != null)
            {
                picturePath = tempInsMessage.getMediaRes().getLocalPath();
            }
        }
        scanAdapter.registerDownLoadListener(
                new PictureScanAdapter.PictureListener()
                {
                    @Override
                    public void onDownSuccess(int position)
                    {
                        if (viewPager.getCurrentItem() == position)
                        {
                            showSaveBtn();
                        }
                    }

                    @Override
                    public void onClick()
                    {
                        touchScrean(false);
                    }

                    @Override
                    public boolean onLongClick()
                    {
                        return onLongClickImage(picturePath);
                    }
                }
        );

        viewPager.setOnPageChangeListener(this);

        final List<InstantMessage> msgs = getAllLikePics(scanAdapter, im, isPublic);

        if (status == COMMON_VIEW)
        {
            showSaveBtn();
        }
        else if (status == DOWNLOAD_VIEW)
        {
            doneBtn.setVisibility(View.GONE);
        }

        ImageView showAllImages = (ImageView) findViewById(R.id.show_all_image);
        showAllImages.setVisibility(View.VISIBLE);
        showAllImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                InstantMessage im = null;
                android.os.Bundle tempBundle = getIntent().getExtras();
                if (tempBundle != null)
                {
                    im = (InstantMessage) tempBundle.get(IntentData.IM);
                }

                ArrayList<MediaRetriever.Item> items = new ArrayList<MediaRetriever.Item>();
                if (null != msgs && !msgs.isEmpty())
                {
                    MediaRetriever.Item item;
                    for (InstantMessage instantMessage : msgs)
                    {
                        item = new MediaRetriever.Item(instantMessage.getId(),"",0,"",0,0);
                        item.setFilePath(instantMessage.getMediaRes().getFilePath());
                        items.add(item);
                    }
                }

                final Intent intent = new Intent(PictureScanActivity.this, ShowAllPicActivity.class);
                if (im != null)
                {
                    intent.putExtra(ShowAllPicActivity.ACCOUNT, getAccount(im));
                }
                intent.putExtra(IntentData.IS_VIDEO, false);
                intent.putExtra(IntentData.FROM_ACTIVITY, fromActivity);
                intent.putExtra(IntentData.TOPIC_PIC_SELECTED, topicPicSelected);
                intent.putExtra(ShowAllPicActivity.PICTURE_LIST, items);
                intent.putExtra(ShowAllPicActivity.PICTURE_INDEX, pictureIndex - 1);

                startActivityForResult(intent, ChatActivity.PROCESS_UM_MEDIA);

            }
        });


    }

    private void touchScrean(boolean showButtom)
    {
        if (status == COMMON_VIEW || status == DOWNLOAD_VIEW)
        {
            toggleImageView(showButtom);
        }
        else
        {
            toggleTitle(showButtom);
        }
    }

    /**
     * 显示或隐藏大图浏览
     *
     * @param showButtom 判断是否显示底部栏
     */
    private void toggleTitle(boolean showButtom)
    {
        int visi=titleLayout.getVisibility() == View.VISIBLE?View.INVISIBLE:View.VISIBLE;
        titleLayout.setVisibility(visi);
        if(showButtom && !isFromTopicSelected())
        {
            buttomLayout.setVisibility(visi);
        }
    }


    /**
     * 显示或隐藏title栏
     *
     * @param showButtom 判断是否显示底部栏
     */
    private void toggleImageView(boolean showButtom)
    {
        //隐藏时点击后显示，显示时点击则隐藏
        int visi = View.INVISIBLE;
        titleLayout.setVisibility(visi);
        if (showButtom && !isFromTopicSelected())
        {
            buttomLayout.setVisibility(visi);
        }
        UCAPIApp.getApp().popActivity(this);
    }

    /**
     * 刷新显示这个聊天联系对象的所有图片。
     *
     * @param im
     * @param isPublic
     */
    private List<InstantMessage> getAllLikePics(PictureScanAdapter adapter, InstantMessage im, boolean isPublic)
    {
        List<InstantMessage> msgs = new ArrayList<InstantMessage>();
        if (im == null)
        {
            return msgs;
        }

        //获取查询的账号
        String account = getAccount(im, isPublic);

        adapter.setAccount(account);

        List<InstantMessage> instantMessages = getAllLikeMsgs(im, account, isPublic);
        if (/*null != instantMessages && */!instantMessages.isEmpty())
        {
            msgs.addAll(instantMessages);
        }

        if (msgs.size() <= 1)
        {
            Logger.debug(TAG, "can't find messages.");
            return msgs;
        }

        //刷新数据
        adapter.setData(msgs);

        int index = indexOf(msgs, im);
        viewPager.setCurrentItem(index);
        setMyTitle(adapter.getCount(), index + 1);
        return msgs;
    }

    /**
     * 获取查询的账号
     * @param im InstantMessage
     * @return 账号
     */
    private String getAccount(InstantMessage im)
    {
        String account;
        if (im.getMsgType() == RecentChatContact.GROUPCHATTER
                || im.getMsgType() == RecentChatContact.DISCUSSIONCHATTER)
        {
            account = im.getToId();
        }
        else
        {
            if (CommonVariables.getIns().getUserAccount()
                    .equalsIgnoreCase(im.getFromId()))
            {
                account = im.getToId();
            }
            else
            {
                account = im.getFromId();
            }
        }
        return account;
    }

    private String getAccount(InstantMessage im, boolean isPublic)
    {
        String account;
        if (isPublic || !(im.getMsgType() == RecentChatContact.GROUPCHATTER
                || im.getMsgType() == RecentChatContact.DISCUSSIONCHATTER))
        {
            if (CommonVariables.getIns().getUserAccount()
                    .equalsIgnoreCase(im.getFromId()))
            {
                account = im.getToId();
            }
            else
            {
                account = im.getFromId();
            }
        }
        else
        {
            account = im.getToId();
        }
        return account;
    }

    protected List<InstantMessage> getAllLikeMsgs(InstantMessage im,
            String account, boolean isPublic)
    {
        List<InstantMessage> instantMessages = new ArrayList<InstantMessage>();
        if (isPublic)
        {
            //  公众号
            List<PublicAccountMsg> publicAccountMsgs = PublicAccountMsgDao.queryAllByAccountAndType(account, MsgContent.MsgType.PIC);
            if (null != publicAccountMsgs)
            {
                for (PublicAccountMsg msg : publicAccountMsgs)
                {
                    instantMessages.add(new PubAccInstantMessage(msg));
                }
            }

            for (int i = 0;i < instantMessages.size();i++)
            {
            	//by lwx302895
//                MediaResource mr = instantMessages.get(i).getMediaRes();
//                if (StringUtil.isStringEmpty(mr.getFilePath()))
//                {
//                    String path = UmFunc.getIns().createPath(account, mr, true);
//                    mr.setFilePath(path);
//                }
            	MediaResource mr = instantMessages.get(i).getMediaRes();
                if (mr != null && TextUtils.isEmpty(mr.getFilePath()))
                {
                    String path = UmUtil.createPublicPath(account, mr.getName(), true);
                    mr.setFilePath(path);
                }
            }

        }
        else {
            List<InstantMessage> messages = ImFunc.getIns().getAllMediaMessage(
                    account, im.getMsgType(), im.getMediaType());
            if (messages != null)
            {
                instantMessages.addAll(messages);
                for (int i = 0; i < instantMessages.size(); i++) {
                    MediaResource mr = instantMessages.get(i).getMediaRes();
                    if (TextUtils.isEmpty(mr.getFilePath())) {
                        String path = UmUtil.getThumbnailPath(UmUtil.createResPath(
                                mr.getMediaType(), mr.getName(), null));
                        mr.setFilePath(path);
                    }
                }
            }
        }
        return instantMessages;
    }

    /**
     * 设置自己显示的标题
     *
     * @param total 总条数
     * @param cur
     */
    private void setMyTitle(int total, int cur)
    {
        if (cur <= 0)
        {
            cur = total;
        }
        pictureIndex = cur;

        setTitle(
                getString(R.string.um_pric_look) + TAB + cur + DEVIDER + total);
    }

    /**
     * 判断im消息在list中的位置。
     *
     * @param msgs 消息队列
     * @param im   im消息
     * @return 返回index位置
     */
    private int indexOf(List<InstantMessage> msgs, InstantMessage im)
    {
        int index = -1;
        for (int i = 0; i < msgs.size(); i++)
        {
            if (im.getId() == msgs.get(i).getId())
            {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * 初始化发送界面
     */
    private void prepareViewForSend()
    {
        String oldPath = getIntent().getStringExtra(IntentData.PATH);
        MediaRetriever.Item directory = (MediaRetriever.Item) getIntent()
                .getSerializableExtra(IntentData.DIRECTORY);
        selectedPath = (ArrayList<MediaRetriever.Item>) getIntent()
                .getSerializableExtra(IntentData.SELECT_PATHS);
        isChoose = getIntent().getBooleanExtra(IntentData.CHOOSE, false);

        newPath = UmUtil.createTempResPath(UmConstant.JPG);
        updateSendButton();
        //图片旋转压缩后显示。
        if (isChoose)
        {
            if (!isFromTopicSelected())
            {
                buttomLayout.setVisibility(View.VISIBLE);
                buttomCheck.setOnClickListener(listener);
                prepareMultiSend(directory, oldPath);
            }
            else
            {
                prepareMultiSend(selectedPath, oldPath);
            }
        }
        else
        {
            bigHead.setVisibility(View.VISIBLE);
            bigHead.setSingleClick(new ZoomImageView.SingleClick()
            {
                @Override
                public void onSingleClick()
                {
                    touchScrean(false);
                }

                @Override
                public boolean onLongClick()
                {
                    return false;
                }
            });
            operatePicture(oldPath, newPath);

            DeviceUtil.notifyAlbum(this, oldPath);
            listener.onClick(buttomCheck);
            buttomLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 更新发送按钮显示信息
     */
    private void updateSendButton()
    {
        //发送按钮的显示处理
        doneBtn.setVisibility(View.VISIBLE);

        if (IntentData.SourceAct.IM_CHAT.ordinal() == fromActivity)
        {
            resId = R.string.btn_send;
        }
        if (IntentData.SourceAct.TOPIC_LIST.ordinal() == fromActivity)
        {
            resId = R.string.btn_done;
        }
        if (isFromTopicSelected())
        {
            resId = R.string.delete;
        }
        doneBtn.setText(getString(resId));

        if (selectedPath == null)
        {
            selectedPath = new ArrayList<MediaRetriever.Item>();
        }
        if (!isFromTopicSelected())
        {
            updateSelect(selectedPath.size());
        }
    }

    /**
     * 准备多张图片发送的页面
     */
    private void prepareMultiSend(MediaRetriever.Item directory, String oldPath)
    {
        if (directory == null || oldPath == null)
        {
            Logger.debug(TAG, "null data.");
            return;
        }

        //多选发送adapter初始化
        ImageRetriever retriever = SystemMediaManager.getIns()
                .getRetriver(getContentResolver(), false);
        List<MediaRetriever.Item> files = retriever
                .getItems(directory.getBucketId());
        prepareMultiSend(files, oldPath);
    }

    private void prepareMultiSend(List<MediaRetriever.Item> files,
            String oldPath)
    {
        if (files == null || oldPath == null)
        {
            Logger.debug(TAG, "null data.");
            return;
        }

        //多选发送adapter初始化
        multiSendAdapter = new MultiSendAdapter(this, files);
        multiSendAdapter.setListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                touchScrean(true);
            }
        });

        //设置adapter
        viewPager.setAdapter(multiSendAdapter);

        //设置当前显示的图片位置
        int index = getIndex(oldPath, files);
        if (index >= 0)
        {
            viewPager.setCurrentItem(index);
        }

        //viewPager.getCurrentItem() 在index小于0时有用
        setMyTitle(files.size(), viewPager.getCurrentItem() + 1);
        onPageSelected(viewPager.getCurrentItem());
        viewPager.setOnPageChangeListener(this);
    }

    private int getIndex(String path, List<MediaRetriever.Item> items)
    {
        if (items == null || path == null)
        {
            return -1;
        }

        for (int i = 0; i < items.size(); i++)
        {
            if (path.equals(items.get(i).getFilePath()))
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * 更新选择的图片个数
     *
     * @param size
     */
    private void updateSelect(int size)
    {
        if (size <= 0)
        {
            doneBtn.setText(getString(resId));
        }
        else
        {
            doneBtn.setText(
                    getString(resId) + "(" + String.valueOf(size) + ")");
        }
    }

    /**
     * 显示保存按钮。
     */
    private void showSaveBtn()
    {
        if (ContactLogic.getIns().getAbility().isAllowCopy())
        {
            doneBtn.setVisibility(View.VISIBLE);
            doneBtn.setText(getString(R.string.save));
        }
        else
        {
            doneBtn.setVisibility(View.GONE);
        }
    }

    /**
     * 未下载完成图片之前，先显示缩略图。
     */
    /*private void showThunbNail(InstantMessage im, MediaResource mediaRes)
    {
        String path = UmUtil.createResPath(im.getFromId(), mediaRes.getMediaType(),
            mediaRes.getName(), null);
        path = UmUtil.getThumbnailPath(path);
        if (path != null)
        {
            if (new File(path).exists())
            {
                Uri uri = Uri.parse(path);
                loadLogo.setImageURI(uri);
                loadLogo.setBackgroundResource(0);
            }
        }
    }*/

    /**
     * 操作图片，将图片处理后存如新的目录。
     *
     * @param oldPath
     * @param newPath
     */
    private void operatePicture(final String oldPath, final String newPath)
    {
        //纠正可能的图片拍照后方向显示与拍照方向偏转90°的问题（三星note2， 三星sIII）
        final ProcessDialog dialog = new ProcessDialog(this,
                R.string.setting_processing);
        dialog.show();
        try
        {
            new AsyncTask<Integer, Integer, Object>()
            {
                @Override
                protected Object doInBackground(Integer... integers)
                {
                    PhotoUtil.zoomPicture(oldPath, newPath, UmUtil.isSavePng(oldPath),UmConstant.MAXPICSIZE);
                    return null;
                }

                @Override
                protected void onPostExecute(Object o)
                {
                    dialog.cancel();
                    initView(newPath, bigHead);
                }
            }.execute();
        }
        catch (RejectedExecutionException exception)
        {
            Logger.beginWarn(TAG).p("RejectedExecutionException: ")
                    .p(exception).end();
        }
    }

    /**
     * 查看联系人的头像
     */
    private void viewHead(int view)
    {
        //显示大图的imageview置为可见。
        bigHead.setVisibility(View.VISIBLE);

        titleLayout.setVisibility(View.GONE);
        rootView.setBackgroundResource(
                R.drawable.bg_corner_square_no_shadow_black);

        if (view == VIEW_HEAD)
        {
            showPersonalHead();
        }
        else if (view == VIEW_LOGO)
        {
            showPublicNoHead();
        }

        bigHead.setSingleClick(new ZoomImageView.SingleClick()
        {
            @Override
            public void onSingleClick()
            {
                UCAPIApp.getApp().popActivity(PictureScanActivity.this);
            }

            @Override
            public boolean onLongClick()
            {
                return false;
            }
        });
    }

    /**
     * 获取联系人头像信息
     */
    private void showPersonalHead()
    {
//        String espacenumber = getIntent().getStringExtra("espacenumber");
//        String head = getIntent().getStringExtra("head");
//
//        if (StringUtil.isStringEmpty(espacenumber))
//        {
//            bigHead.setImageResource(
//                    ContactUtil.getLocalDefaultHead());
//        }
//        else if (StringUtil.isStringEmpty(head))
//        {
//            //先设置一个默认头像，然后去取已下载的头像的头像。
//            bigHead.setImageResource(ContactUtil.getDefaultHead());
//            HeadPhotoUtil.getInstance().loadHeadPhoto(espacenumber, bigHead);
//        }
//        else
//        {
//            bitmap = ImageFetcher.getDefaultHeadImg(head);
//            if (bitmap == null)
//            {
//                bigHead.setImageResource(
//                        ContactUtil.getDefaultHead());
//
//                String fileName = ImageFetcher
//                        .createFileName(espacenumber, head);
//                File file = new File(UCAPIApp.getApp().getFilesDir(),
//                        fileName);
//                if (!file.exists())
//                {
//                    Logger.warn(TAG, "file not exit!");
//                }
//
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inJustDecodeBounds = true;
//                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
//                int realHeight = options.outHeight;
//                int realWidth = options.outWidth;
//
//                //反向检测inSampleSize（即将图片的长宽设置为服务器下发大小，要求的长宽用图片的正常长宽），
//                // 来判断是否需要下载图片。
//                int sideLength = ContactLogic.getIns().getMyOtherInfo()
//                        .getPictureSideLength();
//                options.outHeight = sideLength;
//                options.outWidth = sideLength;
//                int inSampleSize = BitmapUtil
//                        .calculateInSampleSize(options, realWidth, realHeight);
//                if (inSampleSize > 1)
//                {
//                    loadBigPic(file.getAbsolutePath(), espacenumber, head,
//                            bigHead);
//                }
//
//                initView(file.getAbsolutePath(), bigHead);
//            }
//            else
//            {
//                bigHead.setImageBitmap(bitmap);
//            }
//        }
    }

    /**
     * 获取公众号LOGO信息  目前不需要，先注释
     */
    private void showPublicNoHead()
    {
//        String publicNoNum = getIntent().getStringExtra(PUBLIC_NO_NUM);
//        final PublicAccount pa = PublicAccountCache.getIns()
//                .findPublicAccount(publicNoNum);
//
//        if (pa != null)
//        {
//            File file = new File(pa.getBigLogoPath());
//            if(file.exists())
//            {
//                Bitmap bitmapBig = BitmapFactory.decodeFile(file.getAbsolutePath());
//                if (null != bitmapBig)
//                {
//                    bigHead.setImageBitmap(bitmapBig);
//                    return;
//                }
//            }
//        }
//
//        //加载Small Logo
////        HeadPhotoUtil.getInstance().loadSmallPubAccLogo(pa, bigHead); //目前不考虑公众号，先注释
//        final LinearLayout progressLayout = (LinearLayout) findViewById(
//                R.id.progress_layout);
//        progressLayout.setVisibility(View.VISIBLE);
//
//        if (pa != null)
//        {
//            new AsyncTask<PublicAccount,Void,BitmapDrawable>(){
//
//                @Override
//                protected BitmapDrawable doInBackground(
//                        PublicAccount... publicAccounts)
//                {
//                    PublicAccount pa = publicAccounts[0];
//                    return HeadPhotoUtil.getInstance().loadBigPubAccLogo(pa);
//                }
//
//                @Override
//                protected void onPostExecute(BitmapDrawable bitmapDrawable)
//                {
//                    if(new File(pa.getBigLogoPath()).exists())
//                    {
//                        decodeBitmapForShow(pa.getBigLogoPath(),bigHead);
//                    }
//                    else
//                    {
//                        Logger.error(TAG, "download fail.");
//                        bigHead.setImageResource(R.drawable.default_public);
//                    }
//                    progressLayout.setVisibility(View.GONE);
//                }
//            }.execute(pa);
//        }
    }

    /**
     * 加载大图片
     *
     * @param absolutePath
     * @param espaceNumber
     * @param head
     * @param bigHead
     */
    private void loadBigPic(final String absolutePath,
            final String espaceNumber, String head, final ZoomImageView bigHead)
    {
//        if (!SelfDataHandler.getIns().getSelfData().isConnect())
//        {
//            Logger.debug(TAG, "not connect!");
//            return;
//        }
//
//        //加载大图片前显示大图片框
//        final LinearLayout progressLayout = (LinearLayout) findViewById(
//                R.id.progress_layout);
//        progressLayout.setVisibility(View.VISIBLE);
//        final HeadPhoto photo = new HeadPhoto(espaceNumber, head);
//        try
//        {
//            new AsyncTask<HeadPhoto, Integer, Bitmap>()
//            {
//                @Override
//                protected Bitmap doInBackground(HeadPhoto... headPhotos)
//                {
//                    HeadPhoto photo = headPhotos[0];
//                    HeadPhotoUtil.getInstance().loadBigPhoto(photo);
//                    return null;
//                }
//
//                @Override
//                protected void onPostExecute(Bitmap bitmap1)
//                {
//                    if (new File(absolutePath).exists())
//                    {
//                        decodeBitmapForShow(absolutePath, bigHead);
//                    }
//                    else
//                    {
//                        Logger.error(TAG, "download fail.");
//                        bigHead.setImageResource(
//                                ContactUtil.getDefaultHead());
//                    }
//                    progressLayout.setVisibility(View.GONE);
//                }
//            }.execute(photo);
//        }
//        catch (RejectedExecutionException exception)
//        {
//            Logger.beginWarn(TAG).p("RejectedExecutionException: ")
//                    .p(exception).end();
//        }
    }

    private void decodeBitmapForShow(String path, ZoomImageView imageView)
    {
        //获取手机分辨率高宽
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int reqWidth = metrics.widthPixels > metrics.heightPixels ?
                metrics.heightPixels :
                metrics.widthPixels;
        int height = metrics.widthPixels > metrics.heightPixels ?
                metrics.widthPixels :
                metrics.heightPixels;

        bitmap = decodeBitmap(path, reqWidth, height);

        //onPause时会回收bitamp
        if (bitmap != null && !bitmap.isRecycled())
        {
            imageView.setImageBitmap(bitmap);

            //以下方法更改
//            int ori = PhotoUtil.getExifOrientation(path);
            int ori = ExifOriUtil.getExifOrientation(path);
            imageView.rotateAction(ori);
        }
    }

    /**
     * 解析bitamp，由于bitmap经常会导致outofmemoryerror，每次出现OutOfMemoryError
     * 后，将获取的bitmap缩小4倍，再取。
     *
     * @param absolutePath
     * @param reqWidth
     * @param height
     * @return
     */
    private Bitmap decodeBitmap(String absolutePath, int reqWidth, int height)
    {
        try
        {
            bitmap = BitmapUtil
                    .decodeBitmapFromFile(absolutePath, reqWidth, height);
        }
        catch (OutOfMemoryError error)
        {
            Logger.warn(TAG, error.toString());
            bitmap = decodeBitmap(absolutePath, reqWidth / 2, height / 2);
        }
        return bitmap;
    }

    private View.OnClickListener listener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (v.getId() == R.id.right_btn)
            {
//                if (VIEW_CIRCLE_PICTURE == status)
//                {
//                    CirclePictureScanAdapter adapter = (CirclePictureScanAdapter)viewPager.getAdapter();
//                    if (adapter != null)
//                    {
//                        String path = adapter.getPath(viewPager.getCurrentItem());
//                        savePicture(path);
//                    }
//                }
                if (COMMON_VIEW == status || DOWNLOAD_VIEW == status)
                {
                    PictureScanAdapter scanAdapter = (PictureScanAdapter) viewPager
                            .getAdapter();
                    if (scanAdapter == null)
                    {
                        return;
                    }

                    int position = viewPager.getCurrentItem();
                    InstantMessage message = scanAdapter.getMessage(position);
                    if (null != message)
                    {
                        MediaResource mediaRes = message.getMediaRes();
                        savePicture(mediaRes.getLocalPath());
                    }
                }
                else if (COMMON_VIEW_UNSEND == status)
                {
                    if (isFromTopicSelected())
                    {
                        //确认删除对话框
                        ConfirmTitleDialog confirmDialog = new ConfirmTitleDialog(
                                PictureScanActivity.this,
                                getString(R.string.sure_delet_pic));
                        confirmDialog.setRightButtonListener(
                                new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        int pos = viewPager.getCurrentItem();
                                        selectedPath
                                                .remove(getSendPath());//删除当前预览图片 //TODO 出过一次异常
                                        multiSendAdapter
                                                .notifyDataSetChanged(pos);

                                        if (selectedPath.size() > 0)
                                        {
                                            setMyTitle(selectedPath.size(),
                                                    viewPager.getCurrentItem() +
                                                            1
                                            );
                                        }
                                        else
                                        {
                                            backToOrigin();
                                            UCAPIApp.getApp()
                                                    .popActivity(PictureScanActivity.this);
                                        }
                                    }
                                }
                        );
                        confirmDialog.show();
                    }
                    else
                    {
                        Intent intent = new Intent();
                        if (selectedPath == null || selectedPath.size() <= 0)
                        {
                            selectedPath = new ArrayList<MediaRetriever.Item>();
                            if (isChoose)
                            {
                                MediaRetriever.Item path = getSendPath();
                                if (path != null)
                                {
                                    selectedPath.add(path);
                                }
                            }
                            else
                            {
                                MediaRetriever.Item item = new MediaRetriever.Item(
                                        -1, "", -1, "", -1, 0);
                                item.setFilePath(newPath);
                                selectedPath.add(item);
                            }
                        }

                        intent.putExtra(IntentData.SELECT_PATHS, selectedPath);
                        intent.putExtra(IntentData.SEND_MESSAGE, true);
                        intent.putExtra(IntentData.CHOOSE, isChoose);
                        setResult(RESULT_OK, intent);
                        UCAPIApp.getApp().popActivity(PictureScanActivity.this);
                    }
                }
            }
            else if (v.getId() == R.id.chooseCheck)
            {
                clickChoose();
            }
        }
    };

    private void savePicture(String localPath)
    {
        if (TextUtils.isEmpty(localPath))
        {
            return;
        }

        File file = new File(localPath);
        if (!file.exists())
        {
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(
                localPath, new BitmapFactory.Options());
        String path = FileUtil.savePictureToAlbum(this, bitmap,
                Uri.parse(localPath).getLastPathSegment());
        String msg;
        if (path != null)
        {
            msg = getString(R.string.prompt_pic_save, path);
        }
        else
        {
            msg = getString(R.string.savefail);
        }

        ToastUtil.showToast(PictureScanActivity.this, msg);
    }

    protected void onBack()
    {
        backToOrigin();
        UCAPIApp.getApp().popActivity(PictureScanActivity.this);
    }

    private void backToOrigin()
    {
        //只有当发送图片且返回的时候，才进行处理。
        if (status == COMMON_VIEW_UNSEND && isChoose)
        {
            Intent data = new Intent();
            data.putExtra(IntentData.CHOOSE, true);
            data.putExtra(IntentData.SELECT_PATHS, selectedPath);

            setResult(RESULT_OK, data);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (KeyEvent.ACTION_DOWN == event.getAction())
        {
            if (KeyEvent.KEYCODE_BACK == keyCode)
            {
                backToOrigin();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 点击选择框时选中与取消选中的操作。
     */
    private void clickChoose()
    {
        if (selectedPath == null)
        {
            selectedPath = new ArrayList<MediaRetriever.Item>();
        }

        MediaRetriever.Item file = getSelectPath();
        if (file == null)
        {
            Logger.error(TAG, "don't have file path.");
            return;
        }

        //如果已选中则取消，并刷新选中框
        if (selectedPath.contains(file))
        {
            selectedPath.remove(file);
        }
        //如果未选中，就添加。并刷新选中框
        else
        {
            if (selectedPath.size() >=
                    ImageAdapter.MAX_NUMBER - topicPicSelected)
            {
            	ToastUtil.showToast(this, R.string.greatest_picture_count);
                return;
            }

            selectedPath.add(file);
        }

        if (!isFromTopicSelected())
        {
            updateSelect(selectedPath.size());
        }
        toggleCheck(file);
    }

    private boolean isFromTopicSelected()
    {
        return IntentData.SourceAct.TOPIC_LIST.ordinal() == fromActivity &&
                selectedPreview;
    }

    /**
     * 设置选中框选中效果
     *
     * @param path
     */
    private void toggleCheck(MediaRetriever.Item path)
    {
        if (path == null)
        {
            return;
        }

        //包含时设置为选中
        if (selectedPath.contains(path))
        {
            buttomCheck.setImageResource(R.drawable.pic_select_click);
            return;
        }

        buttomCheck.setImageResource(R.drawable.pic_select_normal);
    }

    /**
     * @param path
     * @param imageView
     */
    private void initView(String path, ZoomImageView imageView)
    {
        if (null != path)
        {
            decodeBitmapForShow(path, imageView);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (bitmap != null && !bitmap.isRecycled())
        {
            bitmap.recycle();
        }
        clearData();
        super.onDestroy();
    }

	public void clearData()
    {
        //低内存情况下activity重新拉起，会出现这个问题。
        if (viewPager == null)
        {
            Logger.error(TAG, "low memory。");
            return;
        }

        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null)
        {
            return;
        }

        if (adapter instanceof PictureScanAdapter)
        {
            PictureScanAdapter scanAdapter = (PictureScanAdapter) adapter;
            scanAdapter.unRegMediaBroadCast();
            scanAdapter.unRegisterDownloadListener();
        }
        else if (adapter instanceof MultiSendAdapter)
        {
            ((MultiSendAdapter) adapter).setListener(null);
        }
        else if (adapter instanceof CirclePictureScanAdapter)
        {
            CirclePictureScanAdapter scanAdapter = (CirclePictureScanAdapter) adapter;
            scanAdapter.unRegisterBroadcast();
            scanAdapter.unRegisterDownloadListener();
        }

    }

    /**
     * 获取被选中的图片的路径
     *
     * @return
     */
    public MediaRetriever.Item getSelectPath()
    {
        //非选择图片预览，即拍照预览时直接返回路径。
        if (!isChoose)
        {
            MediaRetriever.Item item = new MediaRetriever.Item(-1, "", -1, "",
                    -1, 0);
            item.setFilePath(newPath);
            return item;
        }

        //查看相册预览的时候，点击选中框需要获取被选中的图片的文件路径
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null || !(adapter instanceof MultiSendAdapter))
        {
            return null;
        }

        MultiSendAdapter mAdapter = (MultiSendAdapter) adapter;
        int pos = viewPager.getCurrentItem();
        MediaRetriever.Item mFile = mAdapter.getFile(pos);
        if (mFile == null)
        {
            Logger.error(TAG, "file not exist.");
            return null;
        }

        return mFile;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels)
    {

    }

    @Override
    public void onPageSelected(int position)
    {
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null)
        {
            return;
        }

        if (adapter instanceof PictureScanAdapter)
        {
            PictureScanAdapter scanAdapter = (PictureScanAdapter) adapter;

            if (scanAdapter.isSupportSave(position))
            {
                showSaveBtn();
            }
            else
            {
                doneBtn.setVisibility(View.GONE);
            }

            setMyTitle(scanAdapter.getCount(), position + 1);
        }
        else if (adapter instanceof MultiSendAdapter)
        {
            MultiSendAdapter mAdapter = (MultiSendAdapter) adapter;
            MediaRetriever.Item file = mAdapter.getFile(position);

            if (!isFromTopicSelected())
            {
                toggleCheck(file);
            }

            setMyTitle(mAdapter.getCount(), position + 1);
        }
        else if (adapter instanceof CirclePictureScanAdapter)
        {
//            CirclePictureScanAdapter sAdapter = (CirclePictureScanAdapter)adapter;
//            if (sAdapter.isCanSave(position))
//            {
//                showSaveBtn();
//            }
//            else
//            {
//                doneBtn.setVisibility(View.GONE);
//            }
//
//            setMyTitle(sAdapter.getCount(), position + 1);
            if (countPointList != null && position <= countPointList.size())
            {
                int total = countPointList.size();
                if (position > 0)
                {
                    countPointList.get((position - 1) % total)
                            .setSelected(false);
                }
                if (position < total)
                {
                    countPointList.get((position + 1) % total)
                            .setSelected(false);
                }

                countPointList.get(position).setSelected(true);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {

    }

    /**
     * 用户未勾选图片时，发送当前预览的图片。
     *
     * @return
     */
    public MediaRetriever.Item getSendPath()
    {
        if (!(viewPager.getAdapter() instanceof MultiSendAdapter))
        {
            Logger.error(TAG, "error adapter");
            return null;
        }

        MultiSendAdapter adapter = (MultiSendAdapter) viewPager.getAdapter();
        MediaRetriever.Item file = adapter.getFile(viewPager.getCurrentItem());

        if (file == null)
        {
            Logger.error(TAG, "error file");
            return null;
        }

        return file;
    }
}
