package com.huawei.esdk.uc.im.adapter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.data.topic.Topic;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.module.topic.WorkCircleFunc;
import com.huawei.module.um.UmUtil;
import com.huawei.utils.StringUtil;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by h00203586 on 2014/10/13.
 */
public class CirclePictureScanAdapter extends SimplePictureScanAdapter<MediaResource>
{
    private static final int UPDATE_DATA = 1001;
    private static final int UPDATE_DATA_FAIL = 1002;

    private final Topic topic;

    Map<String, ScanHolder> holderMap;

    private final String[] broadcasts;

    private BaseReceiver receiver;

    private Handler handler;

    private LongClick listener;

    public CirclePictureScanAdapter(Topic topic, Activity context)
    {
        super(context);

        this.topic = topic;
        holderMap = new HashMap<String, ScanHolder>();
        broadcasts = new String[] {WorkCircleFunc.UPDATE_DATA,
            WorkCircleFunc.UPDATE_DATA_FAIL
        };

        initHandler();
        receiver = new BaseReceiver()
        {
            @Override
            public void onReceive(String broadcastName, BaseData d)
            {
                WorkCircleFunc.WorkCircleReceiveData data;
                Message msg = new Message();

                if (d != null && (d instanceof WorkCircleFunc.WorkCircleReceiveData))
                {
                    data = (WorkCircleFunc.WorkCircleReceiveData) d;
                    msg.obj = data.getObj();
                }

                if (WorkCircleFunc.UPDATE_DATA.equals(broadcastName))
                {
                    msg.what = UPDATE_DATA;
                }
                else
                {
                    msg.what = UPDATE_DATA_FAIL;
                }
                handler.sendMessage(msg);
            }
        };
        WorkCircleFunc.getIns().registerBroadcast(receiver, broadcasts);
    }

    public void unRegisterBroadcast()
    {
        WorkCircleFunc.getIns().unRegisterBroadcast(receiver, broadcasts);
    }

    private void initHandler()
    {
        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                MediaResource resource = (MediaResource)msg.obj;
                if (resource == null)
                {
                    return;
                }
// by lwx302895
//                ScanHolder holder = holderMap.get(resource.getResUrl());
                ScanHolder holder = holderMap.get(resource.getRemotePath());
                if (holder == null)
                {
                    return;
                }

                if (msg.what == UPDATE_DATA_FAIL)
                {
                    refreshFail(holder, false);
                }

                if (msg.what == UPDATE_DATA)
                {
                    refreshSuccess(holder, resource);
                }
            }
        };
    }

    private void refreshSuccess(ScanHolder holder, MediaResource resource)
    {
        String path = UmUtil.createCircleResPath(
            topic.getOwnerID(), topic.getTopicId(), resource.getName());
        refreshSuccess(holder, path);

        holder.progress.setVisibility(View.GONE);
    }

    public boolean isCanSave(int pos)
    {
        String path = getPath(pos);
        return new File(path).exists();
    }

    /**
     * 获取图片存储的路径
     * @param pos
     * @return
     */
    public String getPath(int pos)
    {
        MediaResource resource = datas.get(pos);

        String path;
        if (!TextUtils.isEmpty(resource.getLocalPath()))
        {
            path = resource.getLocalPath();
        }
        else
        {
            path = UmUtil.createCircleResPath(
                topic.getOwnerID(), topic.getTopicId(), resource.getName());
        }
        return path;
    }

    @Override
    protected void doDownload(ScanHolder holder, MediaResource msg)
    {
        if (WorkCircleFunc.getIns().downloadPic(topic, msg))
        {
            holder.loadTv.setText(context.getString(R.string.updating));

            holder.progress.setVisibility(View.VISIBLE);
        }
        else
        {
            refreshFail(holder, false);
        }
    }

    @Override
    protected void refreshFail(ScanHolder holder, boolean deleted)
    {
        if (holder.loadLayout == null)
        {
            return;
        }

        holder.progress.setVisibility(View.GONE);
        ToastUtil.showToast(context, R.string.contact_load_fail);
    }

    @Override
    protected void fillInData(MediaResource mediaRes, final ScanHolder holder)
    {
        holder.ivContent.setImageResource(R.drawable.circle_pic_default_big);
        holder.ivContent.setTag(false);
        holder.loadLayout.setVisibility(View.GONE);
//by lwx302895
        //判断文件是否存在，不存在显示下载失败按钮。
        String path;
        if (mediaRes.getRemotePath() != null)
        {
            holderMap.put(mediaRes.getRemotePath(), holder);
//
            //判断是否正在下载，正在下载则转圈。
            path = getPath(mediaRes.getName());
            if (WorkCircleFunc.getIns().isTopicMediaInDownload(topic.getTopicId(),mediaRes, false))
            {
                holder.progress.setVisibility(View.VISIBLE);
                return;
            }
//            String path = getPath(mediaRes);
//            checkPathForShow(path, holder);
        }
        else
        {
            path = mediaRes.getLocalPath();
        }

        holder.ivContent.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                listener.onLongClick(holder.position, holder.ivContent.getMode());

                return false;
            }
        });

        //文件不存在时，解析缩略图
        File file = new File(path);
        if (!file.exists())
        {
            path = UmUtil.getThumbnailPath(path);
            doDownload(holder, mediaRes);
        }
        decodeBitmapForShow(path, holder);
    }

    private String getPath(MediaResource mediaRes)
    {
        String path;
        if (mediaRes.getRemotePath() != null)
        {
            path = getPath(mediaRes.getName());
        }
        else
        {
            path = mediaRes.getLocalPath();
        }
        return path;
    }

    private String getPath(String name)
    {
        return UmUtil.createCircleResPath(topic.getOwnerID(), topic.getTopicId(), name);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        holderMap.remove(datas.get(position));

        super.destroyItem(container, position, object);
    }

    public static interface LongClick
    {
        public void onLongClick(int position, int mode);
    }

    public void setLongClickListener(LongClick longClick)
    {
        this.listener = longClick;
    }
}
