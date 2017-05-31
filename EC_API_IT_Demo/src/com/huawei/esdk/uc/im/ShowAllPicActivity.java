package com.huawei.esdk.uc.im;

import java.util.ArrayList;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.os.EventHandler;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.im.adapter.ImageAdapter;
import com.huawei.module.um.ImageRetriever;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.SystemMediaManager;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

/**
 * 图片显示页面
 * Created by z00289327 on 2014/1/7.
 */
public class ShowAllPicActivity extends BaseActivity
    implements AdapterView.OnItemClickListener, View.OnClickListener
{
	private static final String TAG = ShowAllPicActivity.class.getSimpleName();
	
    public static final String DIRECTORY = "directory";

    public static final String ACCOUNT = "account_name";

    public static final String PICTURE_LIST = "picture_list";

    public static final String PICTURE_SELECTED = "picture_selected";

    public static final String PICTURE_INDEX = "picture_index";

    private int pictureIndex;

    private ImageAdapter mImageAdapter;

    private TextView numberTextView;

    private ArrayList<MediaRetriever.Item> items;

    private int fromActivity;
    private int resId;
    private int topicPicSelected;

    private MediaRetriever.Item directory;

    private ImageRetriever retriver;
    private String[] mediaBroadcast;
    private BaseReceiver umReceiver;

    /**
     * 注册广播
     */
    private void regMediaBroadcast()
    {
        umReceiver = new BaseReceiver()
        {
            @Override
            public void onReceive(String ID, BaseData data)
            {
                if (data != null && data instanceof UmReceiveData && null != items)
                {
                    // UmFunc.UmReceiveData  umReceiveData = (UmFunc.UmReceiveData) data;
                    // MediaRetriever.Item item;
                    for (int i = 0; i < items.size(); i++)
                    {
                        // item = items.get(i);
                        // if (item.getId() == umReceiveData.msg.getId() || item.getId() ==
                        // umReceiveData.media.getMediaId())
                        // {
                        // item.setFilePath(umReceiveData.media.getLocalPath());
                        EventHandler.getIns().postDelayed(runReloadPic, 300L);
                        // }
                        //  if (null == umReceiveData.media.getLocalPath())
                        // {
                        // EventHandler.getIns().postDelayed(runReloadPic, 300L);
                        // }
                    }

                }
            }
        };

        mediaBroadcast = new String[] {UmConstant.DOWNLOADFILEFINISH};
        UmFunc.getIns().registerBroadcast(umReceiver, mediaBroadcast);
    }

    private Runnable runReloadPic =  new Runnable()
    {
        @Override
        public void run()
        {
            mImageAdapter.notifyDataSetChanged(items);
        }
    };


    /**
     * 选择的路径
     */
    private ArrayList<MediaRetriever.Item> selectPaths;

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
    }

    @Override
    public void initializeData()
    {
        regMediaBroadcast();

        if (getIntent().hasExtra(PICTURE_INDEX))
        {
            pictureIndex = getIntent().getIntExtra(PICTURE_INDEX,0);
        }
        if (getIntent().hasExtra(PICTURE_LIST))
        {
            items = (ArrayList<MediaRetriever.Item>)getIntent().getSerializableExtra(PICTURE_LIST);
        }

        isVideo = getIntent().getBooleanExtra(IntentData.IS_VIDEO, false);
        int source = IntentData.SourceAct.IM_CHAT.ordinal();
        fromActivity = getIntent().getIntExtra(IntentData.FROM_ACTIVITY, source);
        topicPicSelected = getIntent().getIntExtra(IntentData.TOPIC_PIC_SELECTED, 0);
        if (getIntent().hasExtra(DIRECTORY))
        {
            directory = (MediaRetriever.Item) getIntent().getSerializableExtra(DIRECTORY);
        }
        if (getIntent().hasExtra(IntentData.SELECT_PATHS))
        {
            selectPaths = (ArrayList<MediaRetriever.Item>) getIntent().getSerializableExtra(IntentData.SELECT_PATHS);
        }
        if (selectPaths == null)
        {
            selectPaths = new ArrayList<MediaRetriever.Item>();
        }

        retriver = SystemMediaManager.getIns().getRetriver(getContentResolver(), isVideo);
    }

    @Override
    public void initializeComposition()
    {
        setContentView(R.layout.select_sd_card_image);

        TextView titleText = (TextView)findViewById(R.id.username);
        titleText.setText(R.string.all_sd_card_picture);
        titleText.setVisibility(View.VISIBLE);

        numberTextView = (TextView)findViewById(R.id.right_btn);
        numberTextView.setVisibility(View.GONE);
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

        if (null == items)
        {
            items = retriver.getItems(directory.getBucketId());
        }
        mImageAdapter = new ImageAdapter(this,items,
                SystemMediaManager.getIns().getImageCache(), topicPicSelected, true);
        mImageAdapter.setIsVideo(isVideo);
        mImageAdapter.setSelectPaths(selectPaths);

        mGridView.setAdapter(mImageAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setSelection(pictureIndex);
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

    @Override
    protected void onBack()
    {
        Intent data = new Intent();
        data.putExtra(IntentData.CHOOSE, true);
        data.putExtra(IntentData.SELECT_PATHS, selectPaths);
        data.putExtra(PICTURE_SELECTED, pictureIndex);

        setResult(RESULT_OK, data);

        super.onBack();
    }

    @Override
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
    @Override
    public void clearData()
    {
        UmFunc.getIns().unRegisterBroadcast(umReceiver, mediaBroadcast);
        EventHandler.getIns().removeCallbacks(run);
    }

    /**
     * 更新选中照片个数
     *
     * @param num 数量
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
        Intent intent=new Intent();
        intent.putExtra(PICTURE_SELECTED, position);
        setResult(RESULT_OK, intent);
        finish();
        UCAPIApp.getApp().popActivity(this);
    }

    @Override
    public void onClick(View v)
    {
        Intent intent = new Intent();
        intent.putExtra(IntentData.SELECT_PATHS, selectPaths);
        intent.putExtra(IntentData.CHOOSE, true);
        intent.putExtra(IntentData.SEND_MESSAGE, true);
        setResult(RESULT_OK, intent);

        UCAPIApp.getApp().popActivity(ShowAllPicActivity.this);
    }
}
