package com.huawei.esdk.uc.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class UIUtil
{
    private UIUtil()
    {

    }


    /**
     * �������ƣ�dipToPx
     * ���ߣ�YouJun
     * ����������dip��λת��Ϊpx (����Ǿ��Ծ�ȷ����Ҫ΢��)
     * ���������@param context
     * ���������@param dipValue
     * ���������@return
     * �������ͣ�int
     * ��ע��
     */
    public static int dipToPx(float dipValue)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return Math.round(dipValue * metrics.density + 0.5f);
    }

    /**
     * �������ƣ�pxTodip
     * ���ߣ�YouJun
     * ����������px��λת��Ϊdip (����Ǿ��Ծ�ȷ����Ҫ΢��)
     * ���������@param context
     * ���������@param pxValue
     * ���������@return
     * �������ͣ�int
     * ��ע��
     */
    public static int pxToDip(float pxValue)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return Math.round(pxValue / metrics.density + 0.5f);
    }

    public static int spToPx(float spValue)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return (int) (spValue * metrics.scaledDensity + 0.5f);
    }

    public static int pxToSp(float pxValue)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return (int)(pxValue / metrics.scaledDensity + 0.5f);
    }


    private final static int UNREAD_COUNT_MAX = 99;
    public static void showUnreadCount(TextView displayTv, int unreadCount)
    {
        if (0 >= unreadCount)
        {
            displayTv.setText("");
            displayTv.setVisibility(View.GONE);
        }
        else if (UNREAD_COUNT_MAX < unreadCount)
        {
            displayTv.setText("");
            displayTv.setActivated(true);
            displayTv.setVisibility(View.VISIBLE);
        }
        else
        {
            displayTv.setActivated(false);
            displayTv.setVisibility(View.VISIBLE);
            displayTv.setText(String.valueOf(unreadCount));
        }
    }

    public static void setLayoutSize(View view, int width, int height)
    {
        if (view.getParent() instanceof FrameLayout)
        {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            set(view, width, height, lp);
        }
        else if (view.getParent() instanceof RelativeLayout)
        {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
            set(view, width, height, lp);
        }
        else if (view.getParent() instanceof LinearLayout)
        {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
            set(view, width, height, lp);
        }
    }

    private static void set(View view, int width, int height, ViewGroup.MarginLayoutParams lp)
    {
        if (width > 0)
        {
            lp.width = width;
        }
        if (height > 0)
        {
            lp.height = height;
        }
        view.setLayoutParams(lp);
        view.requestLayout();
    }

    public static boolean removeFromParent(View view)
    {
        if (view == null || view.getParent() == null)
        {
            return false;
        }

        ((ViewGroup) view.getParent()).removeView(view);
        return true;
    }

    public static boolean addViewTo(View view, ViewGroup container)
    {
        if (view == null || container == null)
        {
            return false;
        }

        ViewParent vp = view.getParent();
        if (vp == null)
        {
            container.addView(view);
            return true;
        }

        if (vp == container)
        {
            // ��ͬ�ĸ����������������
            return true;
        }

        ((ViewGroup) vp).removeView(view);
        container.addView(view);
        return true;
    }

    public static boolean reAddView(View view)
    {
        if (view == null)
        {
            return false;
        }

        if (view.getParent() == null)
        {
            return false;
        }

        ViewGroup vg = (ViewGroup) view.getParent();

        vg.removeView(view);
        vg.addView(view);
        return true;
    }
}
