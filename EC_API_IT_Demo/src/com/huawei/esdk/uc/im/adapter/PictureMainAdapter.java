package com.huawei.esdk.uc.im.adapter;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.utils.SDCardPhotoFetcher;
import com.huawei.esdk.uc.widget.CubicImageView;
import com.huawei.module.um.ImageRetriever;
import com.huawei.module.um.MediaRetriever;
import com.huawei.module.um.SystemMediaManager;

/**
 * Created by cWX198123 on 2014/7/30.
 */
public class PictureMainAdapter extends BaseAdapter
{
	private static final String TAG = PictureMainAdapter.class.getSimpleName();
	
    private static final int POSITION_ALL = 1;

    private final LayoutInflater inflater;

    private final boolean mIsVideo;

    private Context mContext;
    private SDCardPhotoFetcher sdCardPhotoFetcher;

    ImageRetriever retriever;
    private ArrayList<MediaRetriever.Item> selectPaths;
    private List<MediaRetriever.Item> dirItems;

    public PictureMainAdapter(Context context,ImageRetriever retriever, boolean isVideo)
    {
        mContext = context;
        mIsVideo = isVideo;

        dirItems = retriever.getDirItems();
        this.retriever = retriever;

        inflater = LayoutInflater.from(mContext);

        sdCardPhotoFetcher = new SDCardPhotoFetcher(context);
        sdCardPhotoFetcher.setVideo(mIsVideo);
        sdCardPhotoFetcher.setImageCache(SystemMediaManager.getIns().getImageCache());
    }

    @Override
    public int getCount()
    {
        return dirItems.size() + 1;
    }

    @Override
    public Object getItem(int position)
    {
        if (position <= 0)
        {
            return position;
        }

        return dirItems.get(position - 1);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final ItemViewHolder holder;

        boolean initp;
        if (null == convertView)
        {
            convertView = inflater.inflate(R.layout.image_directory_list_item, null);

            holder = new ItemViewHolder();
            holder.imageView = (CubicImageView)convertView.findViewById(R.id.directory_recent_image);
            holder.takePhotoImage = (ImageView)convertView.findViewById(R.id.take_photo);
            holder.directoryName = (TextView)convertView.findViewById(R.id.directory_name);
            holder.capacity = (TextView)convertView.findViewById(R.id.directory_capacity);
            // holder.background = (ImageView)convertView.findViewById(R.id.select_img_background);
            holder.videoTip = (ImageView) convertView.findViewById(R.id.video_tip);
            convertView.setTag(holder);

            initp = false;
        }
        else
        {
            holder = (ItemViewHolder) convertView.getTag();

            initp = false;
        }

        //地一张留给拍照
        if (0 == position)
        {
            holder.directoryName.setText(getTakeMediaRes());
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(R.color.primary);

            holder.takePhotoImage.setVisibility(View.VISIBLE);

            holder.videoTip.setVisibility(View.GONE);

            holder.directoryName.setTextColor(mContext.getResources().getColor(R.color.textPrimary));
        }
        else
        {
            holder.takePhotoImage.setVisibility(View.GONE);

            final MediaRetriever.Item directory = (MediaRetriever.Item)getItem(position);
            holder.directoryName.setText(directory.getBucketName());
            holder.directoryName.setTextColor(getDirectoryColor(directory, position));

            holder.capacity.setText("(" + retriever.getCount(directory.getBucketId()) + ")");
            holder.capacity.setTextColor(getDirectoryColor(directory, position));

            setCover(holder, directory, initp);
        }

        return convertView;
    }

    /**
     * 获取资源
     * @return
     */
    private int getTakeMediaRes()
    {
        return mIsVideo ? R.string.video_take : R.string.take_photo;
    }

    /**
     *
     * @param directory
     * @param position
     * @return
     */
    private int getDirectoryColor(MediaRetriever.Item directory, int position)
    {
        int color;
        if (isContainSelect(directory, position))
        {
            color = R.color.primary;
        }
        else
        {
            color = R.color.textPrimary;
        }
        return mContext.getResources().getColor(color);
    }

    /**
     * 判断是否存在已选择图片的文件夹。
     */
    private boolean isContainSelect(MediaRetriever.Item directory, int position)
    {
        if (selectPaths == null || selectPaths.isEmpty())
        {
            return false;
        }

        //第一个位置为所有图片显示的相册，必包含。
        if (position == POSITION_ALL)
        {
            return true;
        }

        List<MediaRetriever.Item> items = retriever.getItems(directory.getBucketId());
        if (items == null)
        {
            return false;
        }

        //这里主要捕获files变化导致的异常。
        try
        {
            for (MediaRetriever.Item item : items)
            {
                if (selectPaths.contains(item))
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(TAG, "e = " + e.toString());
        }

        return false;
    }

    /**
     * 通知更新
     * @param selectPaths
     */
    public void notify(ArrayList<MediaRetriever.Item> selectPaths)
    {
        this.selectPaths = selectPaths;
        notifyDataSetChanged();
    }

    /**
     * 设置封面图片
     * @param holder
     * @param item
     * @param initp
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setCover(ItemViewHolder holder, MediaRetriever.Item item, boolean initp)
    {
        holder.imageView.setVisibility(View.VISIBLE);
        holder.imageView.setImageResource(0);
        if (mIsVideo)
        {
            holder.imageView.setBackgroundColor(Color.BLACK);
            sdCardPhotoFetcher.loadImage(item.getFilePath(), holder.imageView);
            holder.videoTip.setVisibility(View.VISIBLE);
            return;
        }

        sdCardPhotoFetcher.loadImage(item.getFilePath(), holder.imageView);
        holder.videoTip.setVisibility(View.GONE);

        if (initp)
        {
            notifyDataSetChanged();
        }
    }

    private static class ItemViewHolder
    {
    	// public ImageView background;
        public CubicImageView imageView;
        public ImageView takePhotoImage;
        public TextView directoryName;
        public TextView capacity;

        public ImageView videoTip;
    }
}
