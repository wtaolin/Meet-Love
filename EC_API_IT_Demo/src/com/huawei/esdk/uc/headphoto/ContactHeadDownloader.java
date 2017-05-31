package com.huawei.esdk.uc.headphoto;

import java.util.ArrayList;
import java.util.List;

import com.huawei.contacts.MyOtherInfo;
import com.huawei.data.ViewHeadPhotoData;
import com.huawei.data.ViewHeadPhotoParam;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.msghandler.maabusiness.GetHeadImageRequest;
import com.huawei.utils.StringUtil;
import com.huawei.utils.img.BitmapUtil;
import com.huawei.utils.io.FileUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;

/**
 * ��ϵ��ͷ������
 * Created by h00203586 on 2015/10/27.
 */
public class ContactHeadDownloader
{
    private final Context mContext;

    private final Bitmap outlineBitmap;

    public ContactHeadDownloader(Context context, Bitmap outlineBitmap)
    {
        this.mContext =context;
        this.outlineBitmap = outlineBitmap;
    }

    /**
     * �ӷ���������ͼƬ
     *
     * @param headPhoto
     * @param sideLength
     * @return
     */
    protected BitmapDrawable loadBitmapFromServer(HeadPhoto headPhoto,
            int sideLength, ServerPhotoLoadedListener listener)
    {
        String account = headPhoto.getAccount();
        String id = headPhoto.getId();
        if (isInValidParam(account, id))
        {
            return null;
        }

        Bitmap bitmap = requestBitmap(account, id, sideLength);
        if (bitmap == null)
        {
            return null;
        }

        //֪ͨ����
        if (listener != null)
        {
            listener.onLoadSuccess();
        }

        return new BitmapDrawable(mContext.getResources(),
                BitmapUtil.getRoundCornerBitmap(bitmap, outlineBitmap));
    }

    /**
     * �����Ƿ���Ч
     * @param account
     * @param id
     * @return
     */
    private boolean isInValidParam(String account, String id)
    {
        return TextUtils.isEmpty(account) || TextUtils.isEmpty(id);
    }

    protected Bitmap doRequest(List<ViewHeadPhotoParam> list)
    {
        GetHeadImageRequest request = new GetHeadImageRequest();
        request.setWaitTime(15000);
        List<ViewHeadPhotoData> dataList = request.requestPhoto(list);
        if (dataList == null)
        {
            return null;
        }

        return saveHeadPhoto(dataList, list);
    }

    /**
     * ����ͼƬ����
     * @param account ��ϵ���˺�
     * @param headId ��ϵ��ͷ��id
     * @param sideLength ��ϵ�������С���߳���
     * @return
     */
    private Bitmap requestBitmap(String account, String headId, int sideLength)
    {
        Logger.debug(LocalLog.APPTAG, "account=" + account + "/id=" + headId);
        List<ViewHeadPhotoParam> list = getParam(account, headId, sideLength);

        return doRequest(list);
    }

    private List<ViewHeadPhotoParam> getParam(String account, String id, int sideLength)
    {
        List<ViewHeadPhotoParam> list = new ArrayList<ViewHeadPhotoParam>();
        ViewHeadPhotoParam param = new ViewHeadPhotoParam();
        param.setJid(account);
        param.setHeadId(id);
        param.setH(getSideLength(sideLength));
        param.setW(getSideLength(sideLength));
        list.add(param);

        return list;
    }



    /**
     * ��ȡͷ��ı߳���
     * @param sideLength
     * @return
     */
    private String getSideLength(int sideLength)
    {
        return (sideLength < 0 ? MyOtherInfo.PICTURE_DEFAULT_HEIGHT : sideLength) + "";
    }


    /**
     * �յ���Ӧʱ����ͷ��
     * @param photoDatas
     * @param headPhoto
     * @return resp Ϊnull,ֱ�ӷ���.
     */
    public Bitmap saveHeadPhoto(List<ViewHeadPhotoData> photoDatas,
            List<ViewHeadPhotoParam> headPhoto)
    {
        if (!isInValidParam(photoDatas, headPhoto))
        {
            return null;
        }

        ViewHeadPhotoData photoData = photoDatas.get(0);
        ViewHeadPhotoParam mHeadPhoto = headPhoto.get(0);
        return saveBytes(photoData, mHeadPhoto);
    }

    private boolean isInValidParam(List<ViewHeadPhotoData> photoDatas, List<ViewHeadPhotoParam> headPhoto)
    {
        return headPhoto != null && headPhoto.size() == 1 && photoDatas.size() == 1;
    }

    private Bitmap saveBytes(ViewHeadPhotoData photoData, ViewHeadPhotoParam mHeadPhoto)
    {
        String account = photoData.getEspaceNumber();
        if (TextUtils.isEmpty(account))
        {
            Logger.debug(LocalLog.APPTAG, "eSpaceNumber = null or \"\"");
            return null;
        }

        //ɾ���ϴ�ʹ�õ�ͷ��
        HeadPhotoUtil.deletePhoto(mContext, account);

        byte[] data = photoData.getData();
        String fileName = save(account, mHeadPhoto.getHeadId(), data);
        HeadPhotoUtil.getIns().addAccount(account, fileName);

        //�����ڴ�
        return BitmapUtil.decodeByteArray(data, MyOtherInfo.PICTURE_DEFAULT_HEIGHT);
    }

    private String save(String account, String headId, byte[] data)
    {
        if (data == null)
        {
            Logger.debug(LocalLog.APPTAG, "headId = null");
            return null;
        }

        String fileName = HeadPhotoUtil.createHeadFileName(account, headId);
        FileUtil.saveBytes(mContext, fileName, data, true);

        return fileName;
    }


    public interface ServerPhotoLoadedListener
    {
        void onLoadSuccess();
    }
}
