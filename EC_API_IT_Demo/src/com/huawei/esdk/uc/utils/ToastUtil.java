package com.huawei.esdk.uc.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.common.res.LocContext;
import com.huawei.esdk.uc.R;

/**
 * toast 操作类 
 * */
public class ToastUtil {

	private ToastUtil()
    {
		
    }
	
	/**
     * Show Toast
     * @param context Context
     * @param resId Message Resource ID
     */
    public static void showToast(Context context, int resId)
    {
        showToast(context, context.getString(resId));
    }

    /**
     * Show toast
     * @param context Context
     * @param message Message to show
     */
    public static void showToast(Context context, String message)
    {
        if (message == null)
        {
            return;
        }

        LayoutInflater inflater = (LayoutInflater) LocContext.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Toast toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        View contentView = inflater.inflate(R.layout.dialog_toast, null);
        toast.setView(contentView);
        toast.setGravity(Gravity.BOTTOM, 0, 185);
        TextView textView = (TextView) contentView.findViewById(R.id.dialog_message);
        textView.setText(message);
        toast.show();
    }
}
