package com.huawei.esdk.uc.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 以宽为标准，将高度设置为与宽一样。
 *
 * Created by h00203586 on 2014/8/20.
 * 
 * 移植自espace源码
 */
public class CubicImageView extends ImageView
{
    // private int mMaxHeight;

    public CubicImageView(Context context)
    {
        super(context);
    }

    public CubicImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public CubicImageView(Context context, AttributeSet attrs, int defStyle)
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
        // mMaxHeight = maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        if (hMode == MeasureSpec.EXACTLY)
        {
            return;
        }

        // 计算最合适的大小；考虑minWidth, minHeight, maxWith, maxHeigh 和
        // allowed size
        int width = getMeasuredWidth();

        int mHeight = width;

        setMeasuredDimension(width, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

    }
}
