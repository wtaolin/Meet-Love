package com.huawei.esdk.uc.utils;

/**
 * Created by h00203586 on 2014/10/21.
 */

import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;
import com.huawei.ecs.mtk.log.Logger;

/**
 * LinkMovementMethod事件与长按事件冲突，通过覆盖这个方法解决长按事件后还会
 * 进入LinkMovementMethod中事件的问题。
 */
public class MyLinkMovementMethod extends LinkMovementMethod
{
	private static final String TAG = MyLinkMovementMethod.class.getSimpleName();
    private int TIME_OUT = ViewConfiguration.getLongPressTimeout();

    private long oldTime = 0;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            oldTime = System.currentTimeMillis();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            long cur = System.currentTimeMillis();
            if (cur - oldTime >= TIME_OUT)
            {
                Logger.debug(TAG, "long click, not handle this.");
                return false;
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    public synchronized static LinkMovementMethod getMyInstance()
    {
        if (sInstance == null)
        {
            sInstance = new MyLinkMovementMethod();
        }
        return sInstance;
    }

    private static LinkMovementMethod sInstance;
}