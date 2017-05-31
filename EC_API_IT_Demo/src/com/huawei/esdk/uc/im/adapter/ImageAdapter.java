package com.huawei.esdk.uc.im.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.huawei.common.library.asyncimage.ImageCache;
import com.huawei.common.res.LocContext;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.im.PicturePreviewActivity;
import com.huawei.esdk.uc.utils.SDCardPhotoFetcher;
import com.huawei.esdk.uc.utils.ToastUtil;
import com.huawei.esdk.uc.widget.ImageViewHolder;
import com.huawei.module.um.MediaRetriever;
import com.huawei.utils.StringUtil;

/**
 * Created by cWX198123 on 2014/7/15.
 * 
 * 移植自espace源码
 */
public class ImageAdapter extends BaseAdapter
{
    public static final int MAX_NUMBER = 9;

    private Activity mContext;

    private int topicPicSelected;

    /**
     * 显示的所有图片文件
     */
    private List<MediaRetriever.Item> files;

    private SDCardPhotoFetcher sdCardPhotoFetcher;

    private LayoutInflater inflater;

    private ArrayList<MediaRetriever.Item> selectPaths;

    private boolean isVideo = false;

    //如果只显示为图片，则第一项不显示拍照，而且不能勾选（只用于图片展示）。
    private boolean justShowPics = false;

    public ImageAdapter(Activity context, List<MediaRetriever.Item> list, ImageCache cache, int topicPicSelected, Boolean justShowPics)
    {
        this(context, list, cache, topicPicSelected);
        this.justShowPics = justShowPics;
    }

    public ImageAdapter(Activity context, List<MediaRetriever.Item> list, ImageCache cache, int topicPicSelected)
    {
        mContext = context;

        inflater = LayoutInflater.from(mContext);
        this.topicPicSelected=topicPicSelected;

        files = list == null ? new ArrayList<MediaRetriever.Item>() : list;

        sdCardPhotoFetcher = new SDCardPhotoFetcher(LocContext.getContext());
        sdCardPhotoFetcher.setImageCache(cache);
    }

    public void notifyDataSetChanged(List<MediaRetriever.Item> list)
    {
        this.files = list == null ? new ArrayList<MediaRetriever.Item>() : list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
//        return files.size() + 1;
        return files.size();
    }

    @Override
    public Object getItem(int position)
    {
//        if (position == 0)
//        {
//            return position;
//        }
//        return files.get(position - 1);

        return files.get(position);
    }

    public SDCardPhotoFetcher getFetcher()
    {
        return sdCardPhotoFetcher;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        final ImageViewHolder holder;
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.grid_item, null);

            holder = new ImageViewHolder(convertView);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ImageViewHolder) convertView.getTag();
        }

        //将第一张预留给拍照
        if (0 == position && !justShowPics)
        {
            holder.checkBoxImage.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(R.color.primary);

            holder.takePhotoImage.setVisibility(View.VISIBLE);
            holder.checkBoxImage.setVisibility(View.GONE);
            holder.videoPlay.setVisibility(View.GONE);
        }
        else
        {
            //holder是重用的，下次getView要再设置回来
            holder.imageView.setVisibility(View.VISIBLE);
            if (justShowPics)
            {
                holder.checkBoxImage.setVisibility(View.GONE);
                holder.selectLayout.setVisibility(View.GONE);
            }
            else
            {
                holder.checkBoxImage.setVisibility(View.VISIBLE);
            }
            holder.takePhotoImage.setVisibility(View.GONE);

            final MediaRetriever.Item mFile = (MediaRetriever.Item)getItem(position);

            final String path = mFile.getFilePath();

            if (isVideo)
            {
                handleVideo(path, holder);
            }
            else
            {
                handlePicture(mFile, holder);
            }

        }
        return convertView;
    }

    /**
     * 处理视频的显示
     * @param path
     * @param holder
     */
    private void handleVideo(String path, ImageViewHolder holder)
    {
        sdCardPhotoFetcher.loadImage(path, holder.imageView);

        //设置cover界面图片
        holder.imageView.setBackgroundColor(Color.BLACK);

        holder.checkBoxImage.setVisibility(View.GONE);

        holder.videoPlay.setVisibility(View.VISIBLE);
    }

    /**
     * 处理图片的显示
     * @param item
     * @param holder
     */
    private void handlePicture(final MediaRetriever.Item item, ImageViewHolder holder)
    {
        holder.imageView.setVisibility(View.VISIBLE);
        holder.imageView.setImageResource(0);
        sdCardPhotoFetcher.loadImage(item.getFilePath(), holder.imageView);
        holder.selectLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onSelect(item);
            }
        });

        holder.checkBoxImage.setImageResource(isSelected(item) ?
                R.drawable.pic_select_click :
            R.drawable.pic_select_normal);

        holder.videoPlay.setVisibility(View.GONE);
    }

    public void onSelect(MediaRetriever.Item item)
    {
        if (selectPaths == null)
        {
            selectPaths = new ArrayList<MediaRetriever.Item>();
        }

        if (selectPaths.contains(item))
        {
            selectPaths.remove(item);
        }
        else
        {
            if (selectPaths.size() >= MAX_NUMBER-topicPicSelected)
            {
                //显示选择超出9张提示
                popupAlarmWindow();
                return;
            }

            selectPaths.add(item);
        }

        ((PicturePreviewActivity)mContext).updateSelected(selectPaths.size());

        notifyDataSetChanged();
    }


    private boolean isSelected(MediaRetriever.Item path)
    {
        if (selectPaths == null)
        {
            return false;
        }

        return selectPaths.contains(path);
    }

    /**
     *  显示选择超出9张提示
     */
    private void popupAlarmWindow()
    {
        ToastUtil.showToast(mContext, R.string.greatest_picture_count);
    }

    public void setSelectPaths(ArrayList<MediaRetriever.Item> selectPaths)
    {
        this.selectPaths = selectPaths;
    }

    public void setIsVideo(boolean isVideo)
    {
        this.isVideo = isVideo;
        sdCardPhotoFetcher.setVideo(isVideo);
    }

}
