package com.huawei.esdk.uc.headphoto;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import com.huawei.common.library.asyncimage.ImageWorker;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.log.TagInfo;
import com.huawei.utils.img.BitmapUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

/**
 * ��ͷ���ȡʹ�á�
 * Created by h00203586 on 2015/6/16.
 */
public abstract class HeadFetcher extends ImageWorker
{
    protected final Bitmap outlineBitmap;

    protected File sysFile = null;

    protected HeadFetcher(Context context, int defaultRes)
    {
        super(context);

        // ��ȡ����ļ��ĸ�Ŀ¼
        sysFile = context.getFilesDir();
        Logger.debug(LocalLog.APPTAG, "" + sysFile);

        //�������ڼ���ʱ��ʾ��ͷ��
        outlineBitmap = HeadPhotoUtil.getIns().getRoundCornerBgSmall();

        //����Ĭ��ͷ��
        setDefaultHead(defaultRes);

        setImageFadeIn(false);
        setForHeadShow(true);

        setImageCache(HeadPhotoUtil.getIns().getHeadCache());
    }


    private void setDefaultHead(int res)
    {
        Bitmap bitmap = HeadPhotoUtil.getIns().getDefaultBitmap(String.valueOf(res));
        if (bitmap == null)
        {
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), res);
            bitmap = BitmapUtil.getRoundCornerBitmap(bitmap, outlineBitmap);
            HeadPhotoUtil.getIns().setDefaultBitmap(String.valueOf(res), bitmap);
        }
        setLoadingImage(bitmap);
    }

    protected void execute(AsyncTask task, Object data)
    {
        try
        {
            // ��һ����ʱ����
            task.execute(data);
        }
        catch (RejectedExecutionException e)
        {
            Logger.warn(TagInfo.TAG, e);
        }
    }



}
