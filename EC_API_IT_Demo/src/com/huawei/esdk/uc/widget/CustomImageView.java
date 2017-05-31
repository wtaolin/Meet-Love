package com.huawei.esdk.uc.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.huawei.widget.RecyclingImageView;
import com.huawei.widget.RecyclingRotateBitmapDrawable;

/**
 * Created by h00203586 on 2014/5/31.
 *
 * view的width固定；支持minHeight，maxHeight同时使用。
 *
 * 布局如下类似即可： width固定，height有最小和最大高度。
 * <com.huawei.espace.widget.CustomImageView
 android:id="@+id/thumbnail"
 android:layout_width="120dp"
 android:layout_height="wrap_content"
 android:scaleType="centerCrop"
 android:minHeight="86.67dp"
 android:maxHeight="173.33dp"
 android:src="@drawable/icon_media_video_normal"
 android:adjustViewBounds="true"
 android:background="@color/video_bg"
 />
 *
 */
public class CustomImageView extends RecyclingImageView
{
    private int mMaxHeight;

    public CustomImageView(Context context)
    {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void setMinimumHeight(int minHeight)
    {
        super.setMinimumHeight(minHeight);
    }

    @Override
    public void setMaxHeight(int maxHeight)
    {
        super.setMaxHeight(maxHeight);
        mMaxHeight = maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Drawable drawable = getDrawable();
        if (drawable != null)
        {
            int hMode = MeasureSpec.getMode(heightMeasureSpec);
            if (hMode == MeasureSpec.EXACTLY)
            {
                return;
            }

            // 计算最合适的大小；考虑minWidth, minHeight, maxWith, maxHeigh 和
            // allowed size
            int width = getMeasuredWidth();

            int mHeight = 0;

            boolean notChange = true;
            if (drawable instanceof RecyclingRotateBitmapDrawable)
            {
                notChange = ((RecyclingRotateBitmapDrawable)drawable).canDivide180();
            }

            if (notChange)
            {
                if (drawable.getIntrinsicWidth() > 0)
                {
                    mHeight = width * drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
                }
            }
            else
            {
                if (drawable.getIntrinsicHeight() > 0)
                {
                    mHeight = width * drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
                }
            }
            int height = Math.max(mHeight, getSuggestedMinimumHeight());

            height = Math.min(height, mMaxHeight);

            setMeasuredDimension(width, height);
        }
    }



}
