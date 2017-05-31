package com.huawei.esdk.uc.headphoto;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.huawei.application.BaseApp;
import com.huawei.common.library.asyncimage.ImageCache;
import com.huawei.common.res.LocContext;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.msghandler.maabusiness.GetHeadImageRequest;
import com.huawei.utils.StringUtil;
import com.huawei.utils.img.BitmapUtil;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * 类名称：HeadPhotoUtil.java
 * 作者： z00189563
 * 创建时间：2011-11-22
 * 类描述：
 * 版权声明 : Copyright (C) 2008-2010 华为技术有限公司(Huawei Tech.Co.,Ltd)
 * 修改时间：下午3:01:46
 *
 *
 * 采用单例模式，给所有模块提供获取头像接口。并提供头像缓存，使对所有同一个人的头像在不
 * 同地方的显示，不会导致头像缓存变大。
 */
public final class HeadPhotoUtil
{
    public static final String SUFFIX = ".jpg";

    /**
     * 分隔符
     */
    public static final String SEPARATOR = "_";

    private static final int MAX_SIZE = 20;

    private static HeadPhotoUtil instance = new HeadPhotoUtil();

    private ImageCache imageCache;

    private ImageCache localCache;

    private ImageCache publicCache;

    private Bitmap circleBgBig = null;
    private Bitmap roundCornerBgBig = null;
    private Bitmap roundCornerBgSmall = null;

    /** 存储已下载的联系账号，用于过滤 */
    private Map<String, String> accounts;

    private LruCache<String, Bitmap> bitmapMap;

    private HeadPhotoUtil()
    {
        init();
    }

    public static HeadPhotoUtil getIns()
    {
        return instance;
    }


    public static void loadBgHeadPhoto(PersonalContact pContact, ImageView imageView)
    {
        if (pContact == null)
        {
            //联系人为空,设置为默认头像
            imageView.setImageResource(R.drawable.default_head);
            return;
        }

        String eSpaceNumber = pContact.getEspaceNumber();
        if (TextUtils.isEmpty(eSpaceNumber))
        {
            //联系人账号为空,设置为自定义头像.
            imageView.setImageResource(R.drawable.default_head_local);
            return;
        }

        String headId = pContact.getHead();
        if (TextUtils.isEmpty(headId))
        {
            imageView.setImageResource(R.drawable.default_head);
        }
        else
        {
            loadBgHeadPhoto(imageView, eSpaceNumber, headId);
        }
    }

    private static void loadBgHeadPhoto(ImageView imageView, String eSpaceNumber, String headId)
    {
        Bitmap defaultHeadBitmap = HeadPhotoUtil.getDefaultHeadImg(headId);
        if (defaultHeadBitmap == null)
        {
            imageView.setImageResource(R.drawable.default_head);

            String fileName = HeadPhotoUtil.createHeadFileName(eSpaceNumber, headId);
            File file = new File(LocContext.getContext().getFilesDir(), fileName);
            if (!file.exists())
            {
                Logger.warn(LocalLog.APPTAG, fileName + "file not exit!");
            }

            int sideLength = ContactLogic.getIns().getMyOtherInfo().getPictureSideLength();
            imageView.setImageBitmap(BitmapUtil.decodeBitmapFromFile(file.getAbsolutePath(), sideLength, sideLength));
        }
        else
        {
            imageView.setImageBitmap(defaultHeadBitmap);
        }
    }

    @SuppressLint("NewApi")
	public void setDefaultBitmap(String key, Bitmap defaultBitmap)
    {
        bitmapMap.put(key, defaultBitmap);
    }

    @SuppressLint("NewApi")
	public Bitmap getDefaultBitmap(String key)
    {
        return bitmapMap.get(key);
    }

    public Bitmap getCircleBgBig()
    {
        if (circleBgBig == null)
        {
            int resId = R.drawable.bg_call_head;
            Resources resources = LocContext.getContext().getResources();
            circleBgBig = BitmapFactory.decodeResource(resources, resId);
        }

        return circleBgBig;
    }

    public Bitmap getRoundCornerBgBig()
    {
        if (roundCornerBgBig == null)
        {
            int resId = R.drawable.head_bg_big;
            Resources resources = LocContext.getContext().getResources();
            roundCornerBgBig = BitmapFactory.decodeResource(resources, resId);
        }

        return roundCornerBgBig;
    }

    public Bitmap getRoundCornerBgSmall()
    {
        if (roundCornerBgSmall == null)
        {
            int resId = R.drawable.head_bg;
            Resources resources = LocContext.getContext().getResources();
            roundCornerBgSmall = BitmapFactory.decodeResource(resources, resId);
        }

        return roundCornerBgSmall;
    }

//    private void initBigLogoFetch(PublicAccount publicAccount, ImageView imageView)
//    {
//        int length = ContactLogic.getIns().getMyOtherInfo().getPictureHeight();
//        File file = new File(publicAccount.getBigLogoPath());
//        Bitmap bitmapBig = BitmapUtil.decodeBitmapFromFile(file.getAbsolutePath(),
//                length, length);
//
//        String loadingImagePath = publicAccount.getSmallLogoPath();
//        publicBigLogoFetcher = new JsonImageFetcher(EspaceApp.getApp(), false, loadingImagePath);
//
//        // 加载公众号logo大图时，由于未缓存，所以每次都先从硬盘加载一下。
//
//        BitmapDrawable bd ;
//
//        if (bitmapBig != null && imageView != null) {
//            bd = new BitmapDrawable(EspaceApp.getIns().getResources(),bitmapBig);
//            imageView.setImageDrawable(bd);
//            return;
//        }
//
//        if (imageView != null)
//        {
//            publicBigLogoFetcher.loadImage(publicAccount, imageView);
//        }
//    }

    public void addAccount(String account, String fileName)
    {
        if (account == null || fileName == null)
        {
            return;
        }

        synchronized (accounts)
        {
            accounts.put(account, fileName);
        }
    }

    public String getFileName(String account)
    {
        if (account == null)
        {
            return null;
        }

        synchronized (accounts)
        {
            return accounts.get(account);
        }
    }

    /**
     *  1、初始化头像保存目录
     *  2、缓存默认头像Bitmap
     *  注意 : 比较
     */
    @SuppressLint("NewApi")
	public void init()
    {
        Logger.debug(LocalLog.APPTAG, "init");

        accounts = new HashMap<String, String>();
        imageCache = new ImageCache();

        localCache = new ImageCache();

        bitmapMap = new LruCache<String, Bitmap>(MAX_SIZE);
    }

    /**
     *清空图片缓存
     */
    @SuppressLint("NewApi")
	public void cleanPhotos()
    {
        imageCache.clearCaches();
        localCache.clearCaches();
        if (publicCache != null)
        {
            publicCache.clearCaches();
        }
        bitmapMap.evictAll();

        synchronized (accounts)
        {
            accounts.clear();
        }
    }



    /**
     * 删除所有.png图片
     */
    public void deletePhotoDir()
    {
        deletePhoto(LocContext.getContext(), SUFFIX);
    }

    /**
     * 删除用户的所有头像
     * @author hute
     * @param filter 过滤字符串
     * @return [说明]
     */
    public static void deletePhoto(Context context, String filter)
    {
        File sysFile = context.getFilesDir();
        File[] files = sysFile.listFiles(new ContactHeadFilter(filter));
        if (files != null && files.length > 0)
        {
            for (File sFile : files)
            {
                deleteFile(sFile);
            }
        }
    }

    private static void deleteFile(File sFile)
    {
        if (sFile.isFile())
        {
            if(!sFile.delete())
            {
                Logger.debug(LocalLog.APPTAG, "Delete photo " +
                        "file fail, File is " + sFile.getPath());
            }
        }
    }

    /**
     * 获取默认头像，默认头像id为0~9
     * @param headid 为0时采用统一默认头像。
     * @return
     */
    public static Bitmap getDefaultHeadImg(String headid)
    {
        int head = StringUtil.stringToInt(headid);
        if (head == GetHeadImageRequest.DEFAULT_HEAD_ID_LITTLE)
        {
            int mResource = R.drawable.default_head;
            Resources r = LocContext.getContext().getResources();
            return BitmapFactory.decodeResource(r, mResource);
        }

        if (head > GetHeadImageRequest.DEFAULT_HEAD_ID_LITTLE
                && head <= GetHeadImageRequest.DEFAULT_HEAD_ID_LARGE)
        {
            int resource = R.drawable.head0;
            resource += head;
            Resources r = LocContext.getContext().getResources();
            return BitmapFactory.decodeResource(r, resource);
        }
        return null;
    }

    /**
     * 对应parse函数
     * @see //com.huawei.espace.module.headphoto.HeadPhotoUtil#parseHeadId
     * @param account
     * @param id
     * @return
     */
    public static String createHeadFileName(String account, String id)
    {
        return account + SEPARATOR + id + SUFFIX;
    }

    /**
     * @see com.huawei.espace.module.headphoto.HeadPhotoUtil#createHeadFileName
     * @param account
     * @param fileName
     * @return
     */
    public static String parseHeadId(String account, String fileName)
    {
        if (fileName == null || account == null)
        {
            return "";
        }

        String headId = fileName.replace(account + SEPARATOR, "");
        return headId.replace(SUFFIX, "");
    }

    public ImageCache getHeadCache()
    {
        return imageCache;
    }

    public ImageCache getPublicHeadCache()
    {
        if (publicCache == null)
        {
            publicCache = new ImageCache();
        }

        return publicCache;
    }

    public ImageCache getLocalHeadCache()
    {
        return localCache;
    }
}
