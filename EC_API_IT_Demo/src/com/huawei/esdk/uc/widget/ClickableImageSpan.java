package com.huawei.esdk.uc.widget;

import java.lang.ref.WeakReference;

import com.huawei.common.os.EventHandler;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class ClickableImageSpan extends DynamicDrawableSpan
{
    private WeakReference<Drawable> mDrawableRef;
    private WeakReference<Drawable> mDrawableHead;
    
    private boolean click = false;
    
    private Runnable runner;
    
    private Context context;
    
    /**
     * 点击,正常时显示的图片id;
     */
    private int normalPic;
    private int pressPic;
    
    public ClickableImageSpan(Context context, int normal, int press)
    {
        this.context = context;
        
        normalPic = normal;
        pressPic = press;
    }
    
    /**
     * @param tx The TextView this span attached to.
     */
    public void updateDrawState(final TextView tx)
    {
        click = true;
        tx.invalidate();
        
        runner = new Runnable()
        {
            @Override
            public void run()
            {
                click = false;
                tx.invalidate();
            }
        };
        EventHandler.getIns().postDelayed(runner,
                ViewConfiguration.getPressedStateDuration());
    }
    
    @Override
    public Drawable getDrawable()
    {
        Drawable pic;
        
        Resources res = context.getResources();
        if (click)
        {
            pic = res.getDrawable(pressPic);
            mDrawableRef = new WeakReference<Drawable>(pic);
        }
        else
        {
            pic = res.getDrawable(normalPic);
            mDrawableHead = new WeakReference<Drawable>(pic);
        }
        pic.setBounds(0, 0, pic.getIntrinsicWidth(),
                pic.getIntrinsicHeight());
        return pic;
    }
    
    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x, 
                     int top, int y, int bottom, Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();
        
        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }

    private WeakReference<Drawable> getWeakRef()
    {
        WeakReference<Drawable> wr;
        if (click)
        {
            wr = mDrawableRef;
        }
        else
        {
            wr = mDrawableHead;
        }
        
        return wr;
    }
    
    private Drawable getCachedDrawable()
    {
        WeakReference<Drawable> wr = getWeakRef();
        
        Drawable d = null;
        if (wr != null)
        {
            d = wr.get();
        }
        
        if (d == null)
        {
            d = getDrawable();
        }
        return d;
    }
}
