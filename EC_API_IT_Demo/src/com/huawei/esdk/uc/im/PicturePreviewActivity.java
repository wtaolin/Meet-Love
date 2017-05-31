package com.huawei.esdk.uc.im;

import java.util.ArrayList;

import com.huawei.common.os.EventHandler;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.im.adapter.ImageAdapter;
import com.huawei.esdk.uc.utils.ChatUtil;
import com.huawei.module.um.ImageRetriever;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.SystemMediaManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

/**
 * 图片选择界面
 * 
 * 移植自espace
 */
public class PicturePreviewActivity extends BaseActivity
    implements AdapterView.OnItemClickListener, View.OnClickListener
{
	private static final String TAG = PicturePreviewActivity.class.getSimpleName();
	
    public static final String DIRECTORY = "directory";

    public static final String ACCOUNT = "account_name";

    private ImageAdapter mImageAdapter;

    private TextView numberTextView;

    private String account;

    private int fromActivity;
    private int resId;
    private int topicPicSelected;

    private MediaRetriever.Item directory;

    ImageRetriever retriver;

    /**
     * 选择的路径
     */
    private ArrayList<MediaRetriever.Item> selectPaths;

    private String mediaPath;

    private boolean isVideo = false;

    private int i = 0;

    private Runnable run =  new Runnable()
    {
        @Override
        public void run()
        {
            i++;
            mImageAdapter.notifyDataSetChanged();
            if (i <= 3)
            {
                EventHandler.getIns().postDelayed(run, 300L);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initializeData();
    }

    public void initializeData()
    {
        account = getIntent().getStringExtra(ACCOUNT);
        isVideo = getIntent().getBooleanExtra(IntentData.IS_VIDEO, false);
        int source = IntentData.SourceAct.IM_CHAT.ordinal();
        fromActivity = getIntent().getIntExtra(IntentData.FROM_ACTIVITY, source);
        topicPicSelected = getIntent().getIntExtra(IntentData.TOPIC_PIC_SELECTED, 0);
        directory = (MediaRetriever.Item)getIntent().getSerializableExtra(DIRECTORY);
        selectPaths = (ArrayList<MediaRetriever.Item>)getIntent().getSerializableExtra(IntentData.SELECT_PATHS);
        if (selectPaths == null)
        {
            selectPaths = new ArrayList<MediaRetriever.Item>();
        }

        retriver = SystemMediaManager.getIns().getRetriver(getContentResolver(), isVideo);
    }

    public void initializeComposition()
    {
        setContentView(R.layout.select_sd_card_image);

        TextView titleText = (TextView)findViewById(R.id.btn_back);
        titleText.setText(directory.getBucketName());
        titleText.setVisibility(View.VISIBLE);
        titleText.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                UCAPIApp.getApp().popActivity(PicturePreviewActivity.this);
            }
        });

        numberTextView = (TextView)findViewById(R.id.right_btn);
        numberTextView.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        if (IntentData.SourceAct.IM_CHAT.ordinal() == fromActivity)
        {
            resId = R.string.btn_send;
        }
        if (IntentData.SourceAct.TOPIC_LIST.ordinal() == fromActivity)
        {
            resId = R.string.btn_done;
        }
        numberTextView.setText(getString(resId));

        numberTextView.setOnClickListener(this);
        updateSelected(selectPaths.size());

        GridView mGridView = (GridView)findViewById(R.id.gridView);
        ArrayList<MediaRetriever.Item> items = new ArrayList<MediaRetriever.Item>();
        MediaRetriever.Item item = new MediaRetriever.Item(0,"",0,"",0,0);
        // 第一个item的路径赋值为空，则界面上展示为拍照图标
        item.setFilePath("");
        items.add(item);
        items.addAll(retriver.getItems(directory.getBucketId()));
        mImageAdapter = new ImageAdapter(this,items,
            SystemMediaManager.getIns().getImageCache(),topicPicSelected);

        mImageAdapter.setIsVideo(isVideo);
        mImageAdapter.setSelectPaths(selectPaths);

        mGridView.setAdapter(mImageAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                {
                    // Before Honeycomb pause image loading on scroll to help with performance
//                    if (!DeviceUtil.hasHoneycomb())
                    {
                        mImageAdapter.getFetcher().setPauseWork(true);
                    }
                }
                else
                {
                    mImageAdapter.getFetcher().setPauseWork(false);
                }

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {

            }
        });
    }

    protected void onBack()
    {
        Intent data = new Intent();
        data.putExtra(IntentData.CHOOSE, true);
        data.putExtra(IntentData.SELECT_PATHS, selectPaths);

        setResult(RESULT_OK, data);

        //返回上一页面
        UCAPIApp.getApp().popActivity(PicturePreviewActivity.this);
    }

    protected void onResume()
    {
        super.onResume();

        EventHandler.getIns().postDelayed(run, 300);

        if (mImageAdapter != null)
        {
            mImageAdapter.getFetcher().setExitTasksEarly(false);
            mImageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (mImageAdapter != null)
        {
            mImageAdapter.getFetcher().setExitTasksEarly(true);
            mImageAdapter.getFetcher().setPauseWork(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (RESULT_OK != resultCode)
        {
            Logger.debug(TAG, "result not ok , result code = " + resultCode);
            return;
        }

        switch (requestCode)
        {
            case ChatActivity.PROCESS_UM_CAMERA:
            case ChatActivity.PROCESS_UM_VIDEO:
                takePhotoOrVideoResult();
                break;
            case ChatActivity.PROCESS_UM_VIDEO_SCAN:
            case ChatActivity.PROCESS_UM_CAMERA_SCAN:
                previewResult(data);
                break;
            case ChatActivity.PROCESS_UM_CHOOSE_VIDEO:

                break;
        }
    }

    /**
     * 预览图片后的返回
     *
     * @param data intent数据。
     */
    private void previewResult(Intent data)
    {
        if (data == null)
        {
            Logger.debug(TAG, "intent data.");
            return;
        }

        boolean send = data.getBooleanExtra(IntentData.SEND_MESSAGE, false);
        if (send)
        {
            setResult(RESULT_OK, data);
            UCAPIApp.getApp().popActivity(this);
        }
        else
        {
            //预览图片时选中的图片要在返回时添加进来。
            ArrayList<MediaRetriever.Item> selPaths = (ArrayList<MediaRetriever.Item>)
                data.getSerializableExtra(IntentData.SELECT_PATHS);
            selectPaths = selPaths;

            //刷新界面选中显示
            mImageAdapter.setSelectPaths(selPaths);
            mImageAdapter.notifyDataSetChanged();

            //刷新图片被选择的张数
            updateSelected(selectPaths.size());
        }
    }

    /**
     * 拍照响应
     */
    private void takePhotoOrVideoResult()
    {
        Intent data = new Intent();
        data.putExtra(IntentData.CHOOSE, false);
        data.putExtra(IntentData.PATH, mediaPath);

        setResult(RESULT_OK, data);
        UCAPIApp.getApp().popActivity(this);
    }

    public void clearData()
    {
        EventHandler.getIns().removeCallbacks(run);
    }

    /**
     * 更新选中照片个数
     *
     * @param num
     */
    public void updateSelected(int num)
    {
        if (isVideo)
        {
            return;
        }

        if (num <= 0)
        {
            numberTextView.setText(getString(resId));
            numberTextView.setTextColor(getResources().getColor(R.color.textLoginDisable));
            numberTextView.setEnabled(false);
            numberTextView.setClickable(false);
        }
        else
        {
            numberTextView.setEnabled(true);
            numberTextView.setClickable(true);
            numberTextView.setTextColor(getResources().getColor(R.color.white));
            numberTextView.setText(getString(resId) + "(" + String.valueOf(num) + ")");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if (0 == position)
        {
            mediaPath = ChatUtil.startCamera(this, isVideo);
            return;
        }

        MediaRetriever.Item file = (MediaRetriever.Item)parent.getItemAtPosition(position);
        if (isVideo)
        {
            ChatUtil.startVideoPlayerActivity(this, file.getFilePath(), true, fromActivity, false);
            return;
        }

        skipToScanActivity(this, account, file.getFilePath(), selectPaths, directory);
       /* mImageAdapter.onSelect(file);*/
    }

    /**
     * 跳到预览界面
     *
     * @param oldPath
     * @param directory
     */
    public void skipToScanActivity(Activity context, String account,
        String oldPath, ArrayList<MediaRetriever.Item> selectPaths, MediaRetriever.Item directory)
    {
        Intent intent = new Intent(context, PictureScanActivity.class);
        intent.putExtra(IntentData.PATH, oldPath);
        intent.putExtra(IntentData.SELECT_PATHS, selectPaths);
        intent.putExtra(IntentData.CHOOSE, true);
        intent.putExtra(IntentData.OPPOACCOUNT, account);
        intent.putExtra(IntentData.STATUS, PictureScanActivity.COMMON_VIEW_UNSEND);
        intent.putExtra(IntentData.DIRECTORY, directory);
        intent.putExtra(IntentData.FROM_ACTIVITY, fromActivity);
        intent.putExtra(IntentData.TOPIC_PIC_SELECTED, topicPicSelected);

        context.startActivityForResult(intent, ChatActivity.PROCESS_UM_CAMERA_SCAN);
    }

    @Override
    public void onClick(View v)
    {
        Intent intent = new Intent();
        intent.putExtra(IntentData.SELECT_PATHS, selectPaths);
        intent.putExtra(IntentData.CHOOSE, true);
        intent.putExtra(IntentData.SEND_MESSAGE, true);
        setResult(RESULT_OK, intent);

        UCAPIApp.getApp().popActivity(PicturePreviewActivity.this);
    }
}
