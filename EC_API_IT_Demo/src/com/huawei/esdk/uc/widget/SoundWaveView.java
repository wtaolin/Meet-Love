package com.huawei.esdk.uc.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;

/**
 * Created by h00203586 on 2014/5/28.
 */
public class SoundWaveView extends LinearLayout
{
	private static final String TAG = SoundWaveView.class.getSimpleName();
	
    private static final int MAX_VOLUME = 10;

    private static final int PLUS_VALUE = 30;

    // private static final int PLUS_SCOPE = 20;

    public static final float INIT_VALUE = -1f;

    private Paint paint;

    private int scopeValue = -1;

    private float cx = INIT_VALUE;
    private float cy = INIT_VALUE;

    private int maxRadius;
    private int minRadius;

    private float radius = INIT_VALUE;
    private float currentRadius;

    private boolean isInit = true;

    private RecordCallBack callBack;

    public SoundWaveView(Context context)
    {
        this(context, null, 0);
    }

    public SoundWaveView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        paint = new Paint();
        paint.setAntiAlias(true);                       //设置画笔为无锯齿
    }

    public SoundWaveView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (radius > 0 && cx > 0 && cy > 0)
        {
            paint.setColor(getResources().getColor(R.color.soundwave));         //设置画笔颜色

            //用户按住但没有声音的时候做一个循环动画
            refreshCurrentRadius();

            if (Math.abs(currentRadius - radius) > .1)
            {
                canvas.drawCircle(cx, cy, currentRadius, paint);
            }
            else
            {
                canvas.drawCircle(cx, cy, radius - getScopeValue(), paint);
            }

            invalidate();
        }
        else
        {
            paint.setColor(Color.TRANSPARENT);
            canvas.drawCircle(cx, cy, radius, paint);
        }
    }

    /**
     * 刷新动画范围值
     */
    private void refreshCurrentRadius()
    {
        //检测当前画圆半径是否符合规范
        checkCurrentRadius();

        if (currentRadius < radius)
        {
            currentRadius += PLUS_VALUE;
            if (currentRadius > radius)
            {
                currentRadius = radius;
            }
        }
        else if (currentRadius > radius)
        {
            currentRadius -= (PLUS_VALUE / 2);
            if (currentRadius < radius)
            {
                currentRadius = radius;
            }
        }
    }

    private int getScopeValue()
    {
        if (isInit)
        {
            scopeValue++;
            if (scopeValue > PLUS_VALUE)
            {
                isInit = false;
            }
        }
        else
        {
            scopeValue--;
            if (scopeValue < (-1 * PLUS_VALUE))
            {
                isInit = true;
            }
        }
        return scopeValue;
    }

    private void checkCurrentRadius()
    {
        if (currentRadius < minRadius)
        {
            currentRadius = minRadius;
        }

        if (currentRadius > maxRadius)
        {
            currentRadius = maxRadius;
        }
    }

    public void setCx(float cx)
    {
        this.cx = cx;
    }

    public void setCy(float cy)
    {
        this.cy = cy;
    }

    /**
     * 刷新声音波纹效果
     * @param volume
     */
    public void invalidateView(int volume)
    {
        //如果声音值传负数，则将半径置为初始值。
        if (volume < 0)
        {
            radius = INIT_VALUE;
            Logger.debug(TAG, "radius = " + radius);
            invalidate();
            return;
        }

        radius = calculateRadius(volume);
        isInit = true;
        scopeValue = 0;

        invalidate();
        Logger.debug(TAG, "radius = " + radius);
    }

    /**
     * 计算当前半径
     * @param volume 声音的大小
     * @return
     */
    private float calculateRadius(int volume)
    {
        //y = ax2 + b; paramA = a;
        float paramA = (float)(maxRadius - minRadius) / MAX_VOLUME;

        Logger.debug(TAG, "max = " + maxRadius + "/min = " + minRadius + "/volume=" + volume);
        float radius = paramA * volume + minRadius;
        if (radius > maxRadius)
        {
            radius = maxRadius;
        }

        return radius;
    }

    public void invalidateNotUiThread(float radius)
    {
        this.radius = radius;
        postInvalidate();
    }
    
    public void setHeight(int height)
    {
        getLayoutParams().height = height;
    }

    public void setMaxRadius(int maxRadius)
    {
        this.maxRadius = maxRadius + PLUS_VALUE;
    }

    public int getMaxRadius()
    {
        return maxRadius;
    }

    public int getMinRadius()
    {
        return minRadius;
    }

    public void setMinRadius(int minRadius)
    {
        this.minRadius = minRadius + PLUS_VALUE;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if (callBack == null)
        {
            return super.onInterceptTouchEvent(ev);
        }

        //y方向上，y小于0时。代表点击区域已离开此控件。
        int y = (int) ev.getY();
        if (MotionEvent.ACTION_UP == ev.getAction())
        {
            callBack.onEndRecord(y >= 0);
        }
        else if (MotionEvent.ACTION_MOVE == ev.getAction())
        {
            callBack.onTouchRegionChange(y >= 0);
        }

        return super.onInterceptTouchEvent(ev);
    }

    public void setRecordCallBack(RecordCallBack callBack)
    {
        this.callBack = callBack;
    }

    public static interface RecordCallBack
    {
        /**
         * 结束录音事件
         * @param normal ture正常结束
         */
        public void onEndRecord(boolean normal);

        /**
         * 判断触摸区域是否在接受触摸区域里面。
         * @param in
         */
        public void onTouchRegionChange(boolean in);
    }
}
