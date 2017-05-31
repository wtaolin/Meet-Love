package com.huawei.esdk.uc.widget;

import com.huawei.esdk.uc.R;

import android.content.Context;

/**
 * 确认对话框，左右两个按钮,带标题
 *
 * @author l00170942
 * @since eSpace Mobile V200R001C05 14-1-26
 * Copyright (C) 2008-2010 华为技术有限公司(Huawei Tech.Co.,Ltd)
 */
public class ConfirmTitleDialog extends BaseDialog
{
    public ConfirmTitleDialog(Context context, int titleResId, int messageResId)
    {
        super(context);
        setContentView(R.layout.dialog_confirm_title);
        setTitle(context.getString(titleResId));
        setMessage(messageResId);
        setCanceledOnTouchOutside(false);
        setLeftButtonListener(null);
        setRightButtonListener(null);
    }

    public ConfirmTitleDialog(Context context, int titleResId, String message)
    {
        super(context);
        setContentView(R.layout.dialog_confirm_title);
        setTitle(context.getString(titleResId));
        setMessage(message);
        setCanceledOnTouchOutside(false);
        setLeftButtonListener(null);
        setRightButtonListener(null);
    }
    
    public ConfirmTitleDialog(Context context, String message)
    {
        super(context);
        setContentView(R.layout.dialog_confirm_title);
        setTitle(context.getString(R.string.info));
        setMessage(message);
        setCanceledOnTouchOutside(false);
        setLeftButtonListener(null);
        setRightButtonListener(null);
    }
    
    public ConfirmTitleDialog(Context context, int message)
    {
        super(context);
        setContentView(R.layout.dialog_confirm_title);
        setTitle(context.getString(R.string.info));
        setMessage(message);
        setCanceledOnTouchOutside(false);
        setLeftButtonListener(null);
        setRightButtonListener(null);
    }
}
