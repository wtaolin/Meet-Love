package com.huawei.esdk.uc.widget;

/**
 * Created by h00203586 on 2014/5/28.
 */
import com.huawei.esdk.uc.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircleProgressBar extends ImageView
{
    private static final int SWEEP_INC = 10;

    private final Paint mFramePaint;

    private int maxProgress = 100;

    private int progress = -1;

    private int mSweep = -90;

    private float stokeWidth;
    private float stokeOutWidth;

    //画圆所在的距形区域
    RectF oval;

    //外框所在区域
    RectF outOval;

    Paint paint;


    public CircleProgressBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        stokeWidth = getResources().getDimension(R.dimen.circle_stoke_width);
        stokeOutWidth = getResources().getDimension(R.dimen.circle_stoke_out);

        paint = new Paint();
        paint.setAntiAlias(true); // 设置画笔为抗锯齿
        paint.setStyle(Style.STROKE);

        mFramePaint = new Paint();
        mFramePaint.setAntiAlias(true);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(0);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT); // 透明

        prepareRecF();

        //绘制圆形半透明背景
        drawCircle(canvas);

        //绘制外层框
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(stokeOutWidth); //线宽
        paint.setColor(getResources().getColor(R.color.gray));
//        canvas.drawRect(outOval, mFramePaint);
        canvas.drawArc(outOval, -90, 360, false, paint);

        paint.setStrokeWidth(stokeWidth); //线宽
        paint.setColor(Color.WHITE); // 设置画笔颜色
//        canvas.drawRect(oval, mFramePaint);
        canvas.drawArc(oval, -90, 360, false, paint); // 绘制白色圆圈，即进度条背景

        if (progress <= 0)
        {
            paint.setColor(getResources().getColor(R.color.gray));
            canvas.drawArc(oval, mSweep, 60, false, paint);

            mSweep += SWEEP_INC;
            if (mSweep >= 360)
            {
                mSweep -= 360;
            }
            invalidate();
        }
        else
        {
            mSweep = -90;

            paint.setColor(getResources().getColor(R.color.main_conf_item_red));
            canvas.drawArc(oval, -90, ((float)progress / maxProgress) * 360, false, paint); // 绘制进度圆弧，这里是浅绿
        }
    }

    private void drawCircle(Canvas canvas)
    {
        float width = this.getWidth();

        paint.setStyle(Style.FILL);
        paint.setColor(getResources().getColor(R.color.half_transparent));
        canvas.drawCircle(outOval.centerX(), outOval.centerY(), width/2, paint);
    }

    private void prepareRecF()
    {
        int width = this.getWidth();
        int height = this.getHeight();

        if (width != height)
        {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }

        if (outOval == null)
        {
            outOval = new RectF(stokeOutWidth/2, stokeOutWidth/2,
                width - stokeOutWidth/2, height - stokeOutWidth/2);
        }

        float mWidth = (stokeWidth + stokeOutWidth) / 2;
        if (oval == null)
        {
            oval = new RectF(mWidth, mWidth, width - mWidth, height - mWidth);
        }


    }

    public int getMaxProgress()
    {
        return maxProgress;
    }

    public void setMaxProgress(int maxProgress)
    {
        this.maxProgress = maxProgress;
    }

    public void setProgress(int progress)
    {
        this.progress = progress;
        this.invalidate();
    }

    /**
     * 非ＵＩ线程调用
     */
    public void setProgressNotInUiThread(int progress)
    {
        this.progress = progress;
        this.postInvalidate();
    }

}
