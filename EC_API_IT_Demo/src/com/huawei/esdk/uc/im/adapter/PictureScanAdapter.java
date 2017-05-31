package com.huawei.esdk.uc.im.adapter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.http.FileTransfer.ProgressInfo;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmReceiveData;
import com.huawei.utils.io.FileUtil;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

/**
 * 图片预览的adapter，供用户预览图片
 *
 * Created by h00203586 on 2014/6/26.
 */
public class PictureScanAdapter extends SimplePictureScanAdapter<InstantMessage>
{
	private static final String TAG = PictureScanAdapter.class.getSimpleName();
	
    /**
     * handler的what消息
     */
    private static final int DOWNLOAD_UPDATE = 4;

    private static final int DOWNLOAD_SUCCESS = 5;

    private static final int DOWNLOAD_FAIL = 6;

    /**
     * key值为msgid； value值为界面的holder。
     */
    private Map<Long, ScanHolder> holderMap;

    /**
     * 广播接收器
     */
    private BaseReceiver umReceiver;

    /**
     * 广播过滤分类
     */
    private String[] mediaBroadcast;

    private Handler handler;
    private boolean isPublic = false;

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    private String account = null;

    private PictureScanAdapter(Activity context)
    {
        super(context);

        holderMap = new HashMap<Long, ScanHolder>();

        initHandler();
        regMediaBroadcast();
    }

    /**
     * 构造函数
     *
     * @param msg 即时消息
     */
    public PictureScanAdapter(Activity context, InstantMessage msg)
    {
        this(context);

        datas.add(msg);
    }

    private void initHandler()
    {
        handler = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                long msgId = msg.getData().getLong(IntentData.MSG_ID);
                ScanHolder holder = holderMap.get(msgId);

                //此处由于界面生命周期与handler消息之间的操作会导致程序的必然性异常。
                //无法判断界面数据什么时候被回收，只能catch住。
                try
                {
                    if (holder == null)
                    {
                        Logger.debug(TAG, "holder is cleared.");
                        return;
                    }

                    if (msg.what == DOWNLOAD_UPDATE)
                    {
                        int tolSize = msg.getData().getInt(IntentData.TOL_SIZE);
                        int curSize = msg.getData().getInt(IntentData.CUR_SIZE);
                        refreshProgress(holder, tolSize, curSize);
                    }
                    else if (msg.what == DOWNLOAD_SUCCESS)
                    {
                        String path = msg.getData().getString(IntentData.PATH);
                        refreshSuccess(holder, path);
                    }
                    else if (msg.what == DOWNLOAD_FAIL)
                    {
                        refreshFail(holder, false);
                    }

                }
                catch (NullPointerException exception)
                {
                    Logger.error(TAG, exception.toString());
                }
            }

            ;
        };
    }

    /**
     * 刷新界面显示进度
     *
     * @param holder
     * @param tolSize
     * @param curSize
     */
    private void refreshProgress(ScanHolder holder, long tolSize, long curSize)
    {
        if (holder.loadTv == null)
        {
            return;
        }

        holder.loadTv.setText(context.getString(R.string.downloding) +
            "\n(" + FileUtil.getShowFileSize(curSize) + "/" + FileUtil.getShowFileSize(tolSize) + ")");
        holder.progressBar.setMax((int)tolSize);
        holder.progressBar.setProgress((int) curSize);
    }

    /**
     * 判断是否是当前富媒体
     *
     * @param d
     * @return
     */
    private InstantMessage getMediaMessage(UmReceiveData d)
    {
        //如果没有im，media对象；直接返回false。
        if (datas == null || datas.isEmpty())
        {
            return null;
        }

        for (InstantMessage msg : datas)
        {
            //如果传输过来的msg不为null，且id与im的一致，返回true。
            if (d.msg != null && d.msg.getId() == msg.getId())
            {
                return msg;
            }
        }

        return null;
    }

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
                if (data == null || !(data instanceof UmReceiveData))
                {
                    return;
                }

                UmReceiveData d = (UmReceiveData)data;
                if (d.isThumbNail)  //如果是缩略图下载，一律不处理。
                {
                    Logger.debug(TAG, "d.isThumbNail = " + d.isThumbNail);
                    return;
                }

                //收到的消息非当前消息，返回。
                if (getMediaMessage(d) == null)
                {
                    return;
                }

                if (ID.equals(UmConstant.DOWNLOADPROCESSUPDATE))
                {
                    updateDownloadProcess(d);
                }
                else if (ID.equals(UmConstant.DOWNLOADFILEFINISH))
                {
                    updateDownloadResult(d);
                }
            }
        };

        mediaBroadcast = new String[] {UmConstant.DOWNLOADFILEFINISH, UmConstant.DOWNLOADPROCESSUPDATE};
        UmFunc.getIns().registerBroadcast(umReceiver, mediaBroadcast);
    }

    /**
     * 更新下载结果
     *
     * @param d 收到的um数据
     */
    private void updateDownloadResult(UmReceiveData d)
    {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        if (UmReceiveData.FINISH_FAIL == d.status)
        {
            bundle.putLong(IntentData.MSG_ID, d.msg.getId());
            msg.what = DOWNLOAD_FAIL;
        }
        else if (UmReceiveData.FINISH_SUCCESS == d.status)
        {
            updateMsg(d.msg, d.media);

            bundle.putLong(IntentData.MSG_ID, d.msg.getId());
            bundle.putString(IntentData.PATH, d.media.getLocalPath());
            msg.what = DOWNLOAD_SUCCESS;
        }

        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    /**
     * 更新消息的内容
     *
     * @param msg   即时消息
     * @param media 媒体消息
     */
    private void updateMsg(InstantMessage msg, MediaResource media)
    {
        if (msg == null || media == null)
        {
            return;
        }

        for (InstantMessage message : datas)
        {
            if (message.getId() == msg.getId())
            {
                //更新消息数据。
                msg.setContent(message.getContent());
                msg.setMediaRes(media);
                break;
            }
        }
    }

    /**
     * 更新界面进度。
     *
     * @param receiveData
     */
    private void updateDownloadProcess(UmReceiveData receiveData)
    {
        Message msg = new Message();
        msg.what = DOWNLOAD_UPDATE;

        if (null != receiveData && null != receiveData.process && null != receiveData.msg)
        {
            Bundle b = new Bundle();
            b.putInt(IntentData.CUR_SIZE, receiveData.process.getCurSize());
            b.putInt(IntentData.TOL_SIZE, receiveData.process.getTotalSize());
            b.putLong(IntentData.MSG_ID, receiveData.msg.getId());
            msg.setData(b);
        }
        handler.sendMessage(msg);
    }

    /**
     * 注销媒体消息。
     */
    public void unRegMediaBroadCast()
    {
        UmFunc.getIns().unRegisterBroadcast(umReceiver, mediaBroadcast);
    }

    /**
     * 刷新数据
     *
     * @param datas
     */
    public void setData(List<InstantMessage> datas)
    {
        this.datas = datas;

        notifyDataSetChanged();
    }

    /**
     * 填充数据到对应的界面，显示。
     *
     * @param instantMessage 即时消息。
     * @param holder         view的容器
     */
    @Override
    protected void fillInData(InstantMessage instantMessage, ScanHolder holder)
    {
        saveTempHolder(instantMessage, holder);

        MediaResource mediaRes = instantMessage.getMediaRes();
        if (mediaRes == null)
        {
            Logger.error(TAG, "res null.");
            return;
        }

        if (isSupportDownload(mediaRes))
        {
            doDownload(holder, instantMessage);
        }
        else
        {
            holder.progressBar.setVisibility(View.GONE);

            decodeBitmapForShow(mediaRes.getLocalPath(), holder);
        }
    }

    private boolean isSupportDownload(MediaResource mediaRes)
    {
        return mediaRes.getResourceType() == MediaResource.RES_URL
                || isPublicNeedDownload(mediaRes.getLocalPath());
    }

    private boolean isPublicNeedDownload(String path)
    {
        if (path == null)
        {
            return false;
        }

        if (new File(path).exists())
        {
            return false;
        }

        return isPublic;
    }

    /**
     * 下载富媒体消息
     *
     * @param holder         界面的holder
     * @param instantMessage 即时消息
     */
    protected void doDownload(ScanHolder holder, InstantMessage instantMessage)
    {
        holder.ivContent.setVisibility(View.GONE);
        holder.loadLayout.setVisibility(View.VISIBLE);
        holder.loadTv.setText(context.getString(R.string.updating));
        holder.loadLogo.setBackgroundResource(R.drawable.um_load_pic_normal);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.loadBtn.setVisibility(View.GONE);

        MediaResource mediaRes = instantMessage.getMediaRes();
        if (mediaRes == null)
        {
            Logger.warn(TAG, "mediares is null.");
            return;
        }

        //正在下载，刷新下载进度
        if (isInTransFile(instantMessage.getId(), mediaRes.getMediaId()))
        {
            Logger.debug(TAG, "is downing!");
            ProgressInfo processInfo = UmFunc.getIns().getTransProcess(
                    instantMessage.getId(), mediaRes.getMediaId(), isPublic);
            if (processInfo != null)
            {
                refreshProgress(holder, processInfo.getTotalSize(), processInfo.getCurSize());
            }
            return;
        }

        boolean result = download(instantMessage, mediaRes);
        if (!result)
        {
            refreshFail(holder, false);
        }
    }

    private boolean download(InstantMessage message, MediaResource resource)
    {
        if (isPublic)
        {
            UmFunc.getIns().downloadPublic(message, resource, false);
            return true;
        }

        //by lwx302895 第三个参数为 false 否则点击图片无法查看
        return UmFunc.getIns().downloadFile(message, resource,false);
    }

    /**
     * 判断是否在传输
     * @param id
     * @param mediaId
     * @return
     */
    private boolean isInTransFile(long id, int mediaId)
    {
        if (isPublic)
        {
            return UmFunc.getIns().isPublicInTransFile(id, mediaId, false);
        }
        else
        {
            return UmFunc.getIns().isInTransFile(id, mediaId, false);
        }
    }

    /**
     * 判断当前富媒体消息是否支持保存。
     *
     * @return
     */
    public boolean isSupportSave(int position)
    {
        MediaResource res = datas.get(position).getMediaRes();
        if (res == null)
        {
            return false;
        }

        return !isSupportDownload(res);
    }

    /**
     * 存储缓存界面信息的holder
     *
     * @param instantMessage
     * @param holder
     */
    private void saveTempHolder(InstantMessage instantMessage, ScanHolder holder)
    {
        MediaResource mediaResource = instantMessage.getMediaRes();
        if (mediaResource == null)
        {
            return;
        }

        if (isSupportDownload(mediaResource))
        {
            holderMap.put(instantMessage.getId(), holder);
        }
    }

    /**
     * 移除界面缓存的holder
     *
     * @param id
     */
    private void removeTempHolder(Long id)
    {
        holderMap.remove(id);
    }

    /**
     * 获取对应位置的message
     *
     * @param pos
     * @return
     */
    public InstantMessage getMessage(int pos)
    {
        if (pos < 0 || pos >= getCount())
        {
            return null;
        }

        return datas.get(pos);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        removeTempHolder(datas.get(position).getId());

        super.destroyItem(container, position, object);
    }

    public boolean isPublic()
    {
        return isPublic;
    }

    public void setPublic(boolean isPublic)
    {
        this.isPublic = isPublic;
    }


}
