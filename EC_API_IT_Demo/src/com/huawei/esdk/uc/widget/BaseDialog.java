package com.huawei.esdk.uc.widget;

import com.huawei.esdk.uc.R;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class description
 *
 * @author l00170942
 * @since eSpace Mobile V200R001C05 14-1-24
 * Copyright (C) 2008-2010 华为技术有限公司(Huawei Tech.Co.,Ltd)
 */
public class BaseDialog extends Dialog
{
    private Button singleButton;

    private Button leftButton;

    private Button rightButton;

    private ListView listView;

    public BaseDialog(Context context)
    {
        super(context, R.style.Theme_dialog);
    }

    protected void setTitle(String title)
    {
        TextView titleTV = (TextView) findViewById(R.id.dialog_title);
        if (titleTV != null && title != null)
        {
            titleTV.setText(title);
        }
    }

    protected void setMessage(String message)
    {
        TextView messageTv = (TextView) findViewById(R.id.dialog_message);
        if (messageTv != null && message != null)
        {
            messageTv.setText(message);
        }
    }

    protected void setMessage(int resId)
    {
        setMessage(getContext().getString(resId));
    }

    /**
     * 设置对话框单个按钮点击事件
     *
     * @param lsn 点击事件
     */
    public void setSingleButtonListener(View.OnClickListener lsn)
    {
        setSingleButtonListener(lsn, true);
    }

    /**
     * 设置对话框单个按钮点击事件，同时设置是否先关闭对话框
     *
     * @param lsn          点击事件
     * @param dismissFirst 是否先关闭对话框
     */
    public void setSingleButtonListener(final View.OnClickListener lsn, boolean dismissFirst)
    {
        setButtonListener(getSingleButton(), lsn, dismissFirst);
    }

    /**
     * 设置对话框左边确认按钮点击事件
     *
     * @param lsn 点击事件
     */
    public void setLeftButtonListener(final View.OnClickListener lsn)
    {
        setLeftButtonListener(lsn, true);
    }

    /**
     * 设置对话框左边按钮点击事件，同时设置是否先关闭对话框
     *
     * @param lsn          点击事件
     * @param dismissFirst 是否先关闭对话框
     */
    public void setLeftButtonListener(final View.OnClickListener lsn, boolean dismissFirst)
    {
        setButtonListener(getLeftButton(), lsn, dismissFirst);
    }

    /**
     * 设置对话框右边按钮点击事件
     *
     * @param lsn 点击事件
     */
    public void setRightButtonListener(final View.OnClickListener lsn)
    {
        setRightButtonListener(lsn, true);
    }

    /**
     * 设置对话框右边按钮点击事件，同时设置是否先关闭对话框
     *
     * @param lsn          点击事件
     * @param dismissFirst 是否先关闭对话框
     */
    public void setRightButtonListener(final View.OnClickListener lsn, boolean dismissFirst)
    {
        setButtonListener(getRightButton(), lsn, dismissFirst);
    }

    private void setButtonListener(Button button, final View.OnClickListener lsn, boolean dismissFirst)
    {
        if (button == null)
        {
            return;
        }

        if (dismissFirst)
        {
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    dismiss();
                    if (lsn != null)
                    {
                        lsn.onClick(v);
                    }
                }
            });
        }
        else
        {
            button.setOnClickListener(lsn);
        }
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener lsn)
    {
        getListView().setOnItemClickListener(lsn);
    }

    public void setSingleButtonText(int textId)
    {
        getSingleButton().setText(textId);
    }

    public void setSingleButtonText(String text)
    {
        getSingleButton().setText(text);
    }

    public void setLeftBackgroundResource(int resId)
    {
        getLeftButton().setBackgroundResource(resId);
    }

    public void setRightBackgroundResource(int resId)
    {
        getRightButton().setBackgroundResource(resId);
    }

    public void setLeftText(String text)
    {
        getLeftButton().setText(text);
    }

    public void setLeftText(int resId)
    {
        getLeftButton().setText(resId);
    }

    public void setRightText(String text)
    {
        getRightButton().setText(text);
    }

    public void setRightText(int resId)
    {
        getRightButton().setText(resId);
    }

    public void setAdapter(BaseAdapter adapter)
    {
        getListView().setAdapter(adapter);
    }

    public void setSelection(int position)
    {
        getListView().setSelection(position);
    }

    public void dismiss()
    {
        if (isShowing())
        {
            super.dismiss();
        }
    }


    public void show()
    {
        if (!isShowing())
        {
            super.show();
        }
    }

    private Button getSingleButton()
    {
        if (singleButton == null)
        {
            singleButton = (Button) findViewById(R.id.dialog_single_button);
        }
        return singleButton;
    }

    private Button getLeftButton()
    {
        if (leftButton == null)
        {
            leftButton = (Button) findViewById(R.id.dialog_leftbutton);
        }
        return leftButton;

    }

    public Button getRightButton()
    {
        if (rightButton == null)
        {
            rightButton = (Button) findViewById(R.id.dialog_rightbutton);

        }
        return rightButton;
    }

    private ListView getListView()
    {
        if (listView == null)
        {
            listView = (ListView) findViewById(R.id.dialog_listview);
        }
        return listView;
    }
}
