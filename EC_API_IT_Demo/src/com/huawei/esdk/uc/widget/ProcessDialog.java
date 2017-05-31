package com.huawei.esdk.uc.widget;

import com.huawei.esdk.uc.R;

import android.content.Context;

/**
 * 进度条对话框
 *
 * @author l00170942
 * @since eSpace Mobile V200R001C05 14-1-29
 * Copyright (C) 2008-2010 华为技术有限公司(Huawei Tech.Co.,Ltd)
 */
public class ProcessDialog extends BaseDialog
{
    public ProcessDialog(Context context, int resId)
    {
        this(context, context.getString(resId));
    }

    public ProcessDialog(Context context, String message)
    {
        super(context);
        setContentView(R.layout.dialog_process);
        setMessage(message);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void show()
    {
        super.show();
    }
}
