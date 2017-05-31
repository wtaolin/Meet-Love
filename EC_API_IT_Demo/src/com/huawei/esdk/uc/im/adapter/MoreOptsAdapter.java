package com.huawei.esdk.uc.im.adapter;


import com.huawei.common.res.LocContext;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * More Options Adapter
 */
public class MoreOptsAdapter extends BaseAdapter
{
    public static final int MORE_BTN_GALLERY    = 0;
    public static final int MORE_BTN_VIDEO      = 1;

    private static final int[][] MOREBTNRES =
            {
                    {R.string.media_picture, R.drawable.selector_icon_more_picture},
                    {R.string.media_video, R.drawable.selector_icon_more_video},
            };

    private int[] moreBtnArray =
            {MORE_BTN_GALLERY,
             MORE_BTN_VIDEO};
    
    private LayoutInflater inflater;
    
    public MoreOptsAdapter(Context context)
    {
        inflater = LayoutInflater.from(context);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder vh;
        
        if (null == convertView)
        {
            vh = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_more_opts_chat, null);
            vh.optLayout = (LinearLayout) convertView.findViewById(R.id.item_ll);
            vh.optImage = (ImageView) convertView.findViewById(R.id.item_image);
            vh.optDesc = (TextView) convertView.findViewById(R.id.item_desc);
            convertView.setTag(vh);
        }
        else
        {
            vh = (ViewHolder) convertView.getTag();
        }
        
        if (moreBtnArray[position] == -1)
        {
            vh.optDesc.setText("");
            vh.optImage.setImageResource(R.drawable.icon_more_transparent);
            vh.optLayout.setBackgroundResource(0);
            return convertView;
        }
        
        int[] res = MOREBTNRES[moreBtnArray[position]];
        
        vh.optDesc.setText(LocContext.getContext().getString(res[0]));
        vh.optImage.setImageResource(res[1]);
        
        return convertView;
    }
    
    @Override
    public int getCount()
    {
        return moreBtnArray.length;
    }
    
    @Override
    public Object getItem(int position)
    {
        return moreBtnArray[position];
    }
    
    @Override
    public long getItemId(int position)
    {
        return position;
    }
    
    private static class ViewHolder
    {
        private LinearLayout optLayout;
        private ImageView optImage;
        private TextView optDesc;
    }

    /**
     * 设置更多的按钮显示，如果没有按钮位置需要用-1代替
     */
    public int[] getMoreBtnArray()
    {
        return (moreBtnArray == null)? null: moreBtnArray.clone();
    }
    
    /**
     * 设置更多的按钮显示，如果没有按钮位置需要用-1代替
     * @param moreBtnArray int[]
     */
    public void setMoreBtnArray(int[] moreBtnArray)
    {
        this.moreBtnArray = (moreBtnArray == null)? null: moreBtnArray.clone();
    }
}
