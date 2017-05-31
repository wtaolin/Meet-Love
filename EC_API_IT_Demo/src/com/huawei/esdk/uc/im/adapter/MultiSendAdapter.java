package com.huawei.esdk.uc.im.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.huawei.common.ui.SwitchPager;
import com.huawei.common.ui.ZoomAdapter;
import com.huawei.common.ui.ZoomImageView;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.module.um.MediaRetriever;
import com.huawei.utils.img.BitmapUtil;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 多选发送的图片预览adapter
 *
 * Created by h00203586 on 2014/8/12.
 */
public class MultiSendAdapter extends PagerAdapter implements ZoomAdapter
{
	private static final String TAG = MultiSendAdapter.class.getSimpleName();
	
    private Activity context;

    /**
     * 图片文件夹
     */
    List<MediaRetriever.Item> files;
    private Map<Integer, SwitchPager> pagers = null;

    /**
     * inflater
     */
    private LayoutInflater inflater;

    private View.OnClickListener listener;

    public MultiSendAdapter(Activity context, List<MediaRetriever.Item> files)
    {
        if (files == null || files.size() <= 0)
        {
            Logger.error(TAG, "files length, error!");
            files = new ArrayList<MediaRetriever.Item>();
        }

        this.context = context;
        inflater = LayoutInflater.from(context);
        pagers = new HashMap<Integer, SwitchPager>();

        this.files = files;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        //构造view。
        final View pictureScan = inflater.inflate(R.layout.picture_scan, null);

        final SendHolder holder = new SendHolder();
        pictureScan.setTag(holder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            holder.ivContent = (ZoomImageView)pictureScan.findViewById(R.id.iv_content);
            holder.ivContent.setVisibility(View.GONE);

            holder.ivContent = (ZoomImageView) pictureScan.findViewById(R.id.iv_content2);
        }
        else
        {
            holder.ivContent = (ZoomImageView) pictureScan.findViewById(R.id.iv_content2);
            holder.ivContent.setVisibility(View.GONE);

            holder.ivContent = (ZoomImageView)pictureScan.findViewById(R.id.iv_content);
        }
        holder.ivContent.setVisibility(View.VISIBLE);

        holder.ivContent.setMaxScaleMultiple(3.f);
        holder.ivContent.setMinScaleMultiple(0.3f);
        pagers.put(position, holder.ivContent);

        showImage(holder.ivContent, position);

        holder.ivContent.setSingleClick(new ZoomImageView.SingleClick()
        {
            @Override
            public void onSingleClick()
            {
                if (listener != null) listener.onClick(holder.ivContent);
            }

            @Override
            public boolean onLongClick()
            {
                return false;
            }
        });

        pictureScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (listener != null) listener.onClick(pictureScan);
            }
        });


        container.addView(pictureScan);
        return pictureScan;
    }

    public MediaRetriever.Item getFile(int pos)
    {
        try
        {
            return files.get(pos);
        }
        catch(Exception e)
        {
            return null;
        }
    }

    /**
     * 显示图片
     * @param ivContent
     * @param position
     */
    private void showImage(ZoomImageView ivContent, int position)
    {
        //获取手机分辨率高宽
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int reqWidth = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;
        int height = metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;

        MediaRetriever.Item item = getFile(position);
        Bitmap bitmap = decodeBitmap(item.getFilePath(), reqWidth, height);
        ivContent.setImageBitmap(bitmap);

        ivContent.rotateAction(item.getOri());
    }

    /**
     * 解析bitamp，由于bitmap经常会导致outofmemoryerror，每次出现OutOfMemoryError
     * 后，将获取的bitmap缩小4倍，再取。
     * @param absolutePath
     * @param reqWidth
     * @param height
     * @return
     */
    private Bitmap decodeBitmap(String absolutePath, int reqWidth, int height)
    {
        Bitmap bitmap;
        try
        {
            bitmap = BitmapUtil.decodeBitmapFromFile(absolutePath, reqWidth, height);
        }
        catch (OutOfMemoryError error)
        {
            Logger.warn(TAG, error.toString());
            bitmap = decodeBitmap(absolutePath, reqWidth/2, height/2);
        }
        return bitmap;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        pagers.remove(position);

        View view = (View)object;
        container.removeView(view);

        SendHolder holder = (SendHolder) view.getTag();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.ivContent.getDrawable();
        if (bitmapDrawable == null || bitmapDrawable.getBitmap() == null)
        {
            return;
        }

        //如果图片还未回收，先强制回收该图片
        holder.ivContent.setImageBitmap(null);
        if(!(bitmapDrawable.getBitmap().isRecycled()))
        {
            bitmapDrawable.getBitmap().recycle();
        }
    }

    @Override
    public int getCount()
    {
        return files.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    public void setListener(View.OnClickListener listener)
    {
        this.listener = listener;
    }

    @Override
    public SwitchPager currentPager(int location)
    {
        return pagers.get(location);
    }

    private static class SendHolder
    {
        public ZoomImageView ivContent;
    }

    /**
     * 清除资源
     * @param position
     */
    public void notifyDataSetChanged(int position)
    {
        ZoomImageView page=(ZoomImageView)pagers.get(position);
        pagers.remove(position);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) page.getDrawable();
        if (bitmapDrawable == null || bitmapDrawable.getBitmap() == null)
        {
            return;
        }

        //如果图片还未回收，先强制回收该图片
        page.setImageBitmap(null);
        if(!(bitmapDrawable.getBitmap().isRecycled()))
        {
            bitmapDrawable.getBitmap().recycle();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object obj)
    {
        return POSITION_NONE;
    }
}
