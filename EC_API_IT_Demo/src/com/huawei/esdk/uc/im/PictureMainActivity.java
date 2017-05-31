package com.huawei.esdk.uc.im;

import java.io.File;
import java.util.ArrayList;

import com.huawei.common.os.EventHandler;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.im.adapter.PictureMainAdapter;
import com.huawei.esdk.uc.utils.ChatUtil;
import com.huawei.module.um.ImageRetriever;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.SystemMediaManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

/**
 * 图片预览主页面
 *
 * Created by cWX198123 on 2014/7/30.
 */
public class PictureMainActivity extends BaseActivity
    implements View.OnClickListener, AdapterView.OnItemClickListener
{
	private static final String TAG = PictureMainActivity.class.getSimpleName();
	
    public static final java.lang.String ACCOUNT = "account_name";

    private String account;

    private ArrayList<MediaRetriever.Item> selectPaths = new ArrayList<MediaRetriever.Item>();

    private String mediaPath;

    private PictureMainAdapter pictureMainAdapter;

    private boolean isVideo = false;

    private int fromActivity;
    private int resId;
    private int topicPicSelected;

    private ImageRetriever manager;

    private int i = 0;

    private Runnable run = new Runnable()
    {
        @Override
        public void run()
        {
            i++;
            pictureMainAdapter.notifyDataSetChanged();
            if (i <= 3)
            {
                EventHandler.getIns().postDelayed(run, 300L);
            }

        }
    };

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void initializeData()
    {
        account = getIntent().getStringExtra(ACCOUNT);
        isVideo = getIntent().getBooleanExtra(IntentData.IS_VIDEO, false);

        int source = IntentData.SourceAct.IM_CHAT.ordinal();
        fromActivity = getIntent().getIntExtra(IntentData.FROM_ACTIVITY, source);
        topicPicSelected = getIntent().getIntExtra(IntentData.TOPIC_PIC_SELECTED, 0);

        manager = SystemMediaManager.getIns().getRetriver(getContentResolver(), isVideo);
    }

    @Override
    public void initializeComposition()
    {
        setContentView(R.layout.select_image_directory);

        TextView title = (TextView) findViewById(R.id.btn_back);
        title.setText(getMyTitle());

        if (IntentData.SourceAct.IM_CHAT.ordinal() == fromActivity
                || IntentData.SourceAct.PUBNO_CHAT.ordinal() == fromActivity)
        {
            resId = R.string.btn_send;
            setRightBtn(resId, this);
        }

        if (IntentData.SourceAct.TOPIC_LIST.ordinal() == fromActivity)
        {
            resId = R.string.btn_done;
            setRightBtn(resId, this);
        }

        updateSelect(0);

        GridView listView = (GridView) findViewById(R.id.image_directory_list);
        pictureMainAdapter = new PictureMainAdapter(this, manager, isVideo);
        listView.setAdapter(pictureMainAdapter);
        listView.setOnItemClickListener(this);
    }

    private String getMyTitle()
    {
        if (isVideo)
        {
            return getString(R.string.video_home);
        }

        return getString(R.string.album);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        EventHandler.getIns().postDelayed(run, 300);

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
            case ChatActivity.PROCESS_UM_CAMERA_SCAN:
            case ChatActivity.PROCESS_UM_VIDEO_SCAN:
                takePhotoPreviewResult(data);
                break;
            case ChatActivity.PROCESS_UM_MEDIA:
                choosePhotoOrVideoResult(data);
                break;
            default:
                break;
        }
    }

    /**
     * 选择图片的返回结果
     * @param data Intent
     */
    private void choosePhotoOrVideoResult(Intent data)
    {
        boolean choose = data.getBooleanExtra(IntentData.CHOOSE, true);
        if (!choose)
        {
            mediaPath = data.getStringExtra(IntentData.PATH);
            takePhotoOrVideoResult();
            return;
        }

        //如果发送图片，则直接返回到聊天页面
        boolean send = data.getBooleanExtra(IntentData.SEND_MESSAGE, false);
        if (send)
        {
            data.putExtra(IntentData.IS_VIDEO, isVideo);
            data.putExtra(IntentData.CHOOSE, true);
            setResult(RESULT_OK, data);

            UCAPIApp.getApp().popActivity(this);
        }
        else
        {
            //将选择的图片添加到主预览页面中
            ArrayList<MediaRetriever.Item> select = (ArrayList<MediaRetriever.Item>)
                data.getSerializableExtra(IntentData.SELECT_PATHS);
            if (select == null || select.size() <= 0)
            {
                selectPaths.clear();
            }
            else
            {
                selectPaths = select;
            }

            updateSelect(selectPaths.size());
            notifyAdapter(selectPaths);
        }
    }

    /**
     * 通知界面Adapter更新
     * @param selectPaths selectPaths
     */
    private void notifyAdapter(ArrayList<MediaRetriever.Item> selectPaths)
    {
        pictureMainAdapter.notify(selectPaths);
    }

    /**
     * 刷新选择图片的个数
     * @param size 数量
     */
    private void updateSelect(int size)
    {
        TextView rightBtn = (TextView) findViewById(R.id.right_btn);
        if (isVideo)
        {
            rightBtn.setVisibility(View.GONE);
            return;
        }

        if (size <= 0)
        {
            rightBtn.setText(getString(resId));
            rightBtn.setTextColor(getResources().getColor(R.color.textLoginDisable));
            rightBtn.setEnabled(false);
            rightBtn.setClickable(false);
        }
        else
        {
            rightBtn.setEnabled(true);
            rightBtn.setClickable(true);
            rightBtn.setTextColor(getResources().getColor(R.color.white));
            rightBtn.setText(getString(resId) + "(" + String.valueOf(size) + ")");
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //应在onPause里面调用，onDestory很可能延时。
        if (isFinishing())
        {
            SystemMediaManager.getIns().clearRetriver(isVideo);
        }
    }

    /**
     * 图片预览返回ChatActivity
     * @param data Intent
     */
    private void takePhotoPreviewResult(Intent data)
    {
        if (data == null)
        {
            Logger.warn(TAG, "something error.");
            return;
        }

        data.putExtra(IntentData.PATH, mediaPath);
        data.putExtra(IntentData.IS_VIDEO, isVideo);
        setResult(RESULT_OK, data);

        //收到拍照响应，直接popup
        UCAPIApp.getApp().popActivity(this);
    }

    /**
     * 接收到拍照片的响应
     */
    private void takePhotoOrVideoResult()
    {
        if (mediaPath == null)
        {
            Logger.error(TAG, "mediaPath null, please check.");
            return;
        }

        String path = mediaPath;
        File file = new File(path);
        if (!file.exists())
        {
            Logger.debug(TAG, "file not exist");
            return;
        }
        else if (file.length() == 0)
        {
            Logger.debug(TAG, "file is empty");
            return;
        }

        if (isVideo)
        {
            ChatUtil.startVideoPlayerActivity(this, mediaPath, false, fromActivity, false);
            return;
        }

        //跳到预览页面
        startPictureScanActivity(account, mediaPath, false);
    }

    /**
     * 跳到预览界面
     * @param oldPath oldPath
     * @param choose choose
     */
    private void startPictureScanActivity(String account, String oldPath, boolean choose)
    {
        Intent intent = new Intent(this, PictureScanActivity.class);
        intent.putExtra(IntentData.PATH, oldPath);
        intent.putExtra(IntentData.CHOOSE, choose);
        intent.putExtra(IntentData.OPPOACCOUNT, account);
        intent.putExtra(IntentData.STATUS, PictureScanActivity.COMMON_VIEW_UNSEND);

        intent.putExtra(IntentData.FROM_ACTIVITY, fromActivity);
        intent.putExtra(IntentData.TOPIC_PIC_SELECTED, topicPicSelected);

        startActivityForResult(intent, ChatActivity.PROCESS_UM_CAMERA_SCAN);
    }

    public void clearData()
    {
        EventHandler.getIns().removeCallbacks(run);
    }

    @Override
    public void onClick(View v)
    {
        //点击发送时返回ChatActivity页面
        //不需要判断视频，视频时已隐藏按钮
        if (v.getId() == R.id.right_btn)
        {
            Intent intent = new Intent();
            intent.putExtra(IntentData.CHOOSE, true);
            intent.putExtra(IntentData.SELECT_PATHS, selectPaths);
            setResult(RESULT_OK, intent);

            UCAPIApp.getApp().popActivity(this);
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

        MediaRetriever.Item directory =  (MediaRetriever.Item)parent.getItemAtPosition(position);
        startPreviewMedia(directory);
    }

    /**
     * 启动相册的预览页面
     * @param directory directory
     */
    private void startPreviewMedia(MediaRetriever.Item directory)
    {
        final Intent intent = new Intent(this, PicturePreviewActivity.class);
        intent.putExtra(PicturePreviewActivity.ACCOUNT, account);
        intent.putExtra(IntentData.SELECT_PATHS, selectPaths);
        intent.putExtra(IntentData.IS_VIDEO, isVideo);
        intent.putExtra(PicturePreviewActivity.DIRECTORY, directory);
        intent.putExtra(IntentData.FROM_ACTIVITY, fromActivity);
        intent.putExtra(IntentData.TOPIC_PIC_SELECTED, topicPicSelected);

        startActivityForResult(intent, ChatActivity.PROCESS_UM_MEDIA);
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

    @Override
    protected void onBack()
    {
        UCAPIApp.getApp().popActivity(PictureMainActivity.this);
        super.onBack();
    }
}
