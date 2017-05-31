package com.huawei.esdk.uc.headphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;

import com.huawei.contacts.MyOtherInfo;
import com.huawei.utils.img.BitmapUtil;
import com.huawei.utils.StringUtil;

import java.io.File;

/**
 * ��ϵ�˱���ͷ����ء�
 * Created by h00203586 on 2015/10/27.
 */
public class ContactHeadLocalLoader
{
    private Context mContext;

    private android.graphics.Bitmap outlineBitmap;

    private File sysFile;

    public ContactHeadLocalLoader(Context context, Bitmap outlineBitmap, File sysFile)
    {
        this.mContext = context;
        this.outlineBitmap = outlineBitmap;
        this.sysFile = sysFile;
    }

    public BitmapDrawable load(HeadPhoto headPhoto)
    {
        String account = headPhoto.getAccount();
        if (TextUtils.isEmpty(account))
        {
            return null;
        }
        Bitmap bitmap = getBitmap(account, headPhoto.getId());
        if (bitmap == null)
        {
            return null;
        }

        return new BitmapDrawable(mContext.getResources(),
                BitmapUtil.getRoundCornerBitmap(bitmap, outlineBitmap));
    }

    private Bitmap getBitmapFromFile(String account, String headid)
    {
        File file = getPhotoFile(account, headid);
        if (file.exists())
        {
            HeadPhotoUtil.getIns().addAccount(account, file.getName());
            return BitmapUtil.decodeBitmapFromFile(file.getAbsolutePath(),
                    MyOtherInfo.PICTURE_DEFAULT_WIDTH, MyOtherInfo.PICTURE_DEFAULT_WIDTH);
        }

        return null;
    }

    /**
     * ��ȡͷ��bitmap
     * @param account
     * @param headid
     * @return
     */
    private Bitmap getBitmap(String account, String headid)
    {
        if (TextUtils.isEmpty(headid))
        {
            return readUnknownHeadPhoto(account);
        }

        Bitmap bitmap = HeadPhotoUtil.getDefaultHeadImg(headid);
        if (bitmap != null)
        {
            //��ӵ��������Ҫˢ�¡�
            HeadPhotoUtil.getIns().addAccount(account, headid);
            return bitmap;
        }

        return getBitmapFromFile(account, headid);
    }


    private File getPhotoFile(String account, String headId)
    {
        //�����˺ź�ͷ��id��ȡͷ��
        String fileName = HeadPhotoUtil.createHeadFileName(account, headId);
        return new File(sysFile, fileName);
    }



    /**
     * ����eSpaceNum��ȡİ����ͷ��
     * @param eSpaceNum
     * @return
     */
    protected Bitmap readUnknownHeadPhoto(String eSpaceNum)
    {
        Bitmap bitmap = null;
        File[] files = sysFile.listFiles(new ContactHeadFilter(eSpaceNum));
        if (files != null && files.length > 0)
        {
            File file = files[0];
            HeadPhotoUtil.getIns().addAccount(eSpaceNum, file.getName());
            bitmap = BitmapUtil.decodeBitmapFromFile(file.getAbsolutePath(),
                    MyOtherInfo.PICTURE_DEFAULT_WIDTH, MyOtherInfo.PICTURE_DEFAULT_WIDTH);
        }

        return bitmap;
    }
}
