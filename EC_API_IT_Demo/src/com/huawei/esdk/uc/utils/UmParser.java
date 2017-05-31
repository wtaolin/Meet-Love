package com.huawei.esdk.uc.utils;

import com.huawei.data.entity.InstantMessage;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

/**
 * 供富媒体消息显示在会话界面时解析使用
 *
 * Created by h00203586 on 2014/6/16.
 * 
 * 移植自espace源码
 */
public class UmParser
{
	private static final String TAG = UmParser.class.getSimpleName();
	
    private static final int[] MEDIA_DRAWABLE_FAIL =
        {
            0,
            R.drawable.um_voice_fail_abstract,
            R.drawable.um_video_fail_abstract,
            R.drawable.um_picture_fail_abstract,
            R.drawable.um_file_common_abstract,
            R.drawable.um_file_common_abstract
        };

    private static final int[] MEDIA_DRAWABLE_COMMON =
        {
            0,
            R.drawable.um_voice_common_abstract,
            R.drawable.um_video_common_abstract,
            R.drawable.um_picture_common_abstract,
            R.drawable.um_file_common_abstract,
            R.drawable.um_file_common_abstract
        };

    private static final int[] MEDIA_DRAWABLE_UNREAD =
        {
            0,
            R.drawable.um_voice_unread_abstract,
            R.drawable.um_video_unread_abstract,
            R.drawable.um_picture_unread_abstract,
            R.drawable.um_file_unread_abstract,
            R.drawable.um_file_unread_abstract
        };

    /**
     * 根据消息体和多媒体资源，获取对应的drawable资源
     *
     * @param msg   消息体
     * @param media 多媒体资源
     * @return 如果返回值为-1，说明没有找到对应的资源
     */
    public int getDrawableResId(InstantMessage msg, MediaResource media)
    {
        if (msg == null || media == null)
        {
            return -1;
        }

        int mediaType = media.getMediaType();

        if (media.getResourceType() == MediaResource.RES_URL
            || InstantMessage.STATUS_AUDIO_UNREAD.equals(msg.getStatus())
            || InstantMessage.STATUS_UNREAD.equals(msg.getStatus()))
        {
            return MEDIA_DRAWABLE_UNREAD[mediaType];
        }

        if (InstantMessage.STATUS_SEND_FAILED.equals(msg.getStatus()))
        {
        	//by lwx302895
//            int process = UmFunc.getIns().getProgress(msg, media.getMediaId());
        	int process = UmFunc.getIns().getProgress(msg.getId(), media.getMediaId(),false);
        	if (process != UmConstant.NOT_DOWNLOAD)
            {
                return MEDIA_DRAWABLE_COMMON[mediaType];
            }
            else
            {
                return MEDIA_DRAWABLE_FAIL[mediaType];
            }
        }

        return MEDIA_DRAWABLE_COMMON[mediaType];
    }

    /**
     * 生成SpannableString
     * @param msg
     * @return
     */
    public SpannableString getUmSpannableString(InstantMessage msg)
    {
        //消息为空，且内容为空；则无需考虑。
        if (msg == null || msg.getContent() == null || msg.getMediaRes() == null)
        {
            return null;
        }

        SpannableString result = new SpannableString(msg.getContent());

        try
        {
            int resid = getDrawableResId(msg, msg.getMediaRes());
            ImageSpan image = new ImageSpan(UCAPIApp.getApp().getCurActivity(), resid);
            result.setSpan(image, 0, msg.getContent().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        catch (Exception e)
        {
            Logger.debug(TAG, e.toString());
            return null;
        }

        return result;
    }

    /*private class MediaPosInfo extends StringUtil.StringPosInfo
    {
        MediaResource media;
    }*/
}
