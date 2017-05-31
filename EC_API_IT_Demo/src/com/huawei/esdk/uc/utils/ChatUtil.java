package com.huawei.esdk.uc.utils;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

import com.huawei.common.CommonVariables;
import com.huawei.common.constant.Constant;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.im.ChatActivity;
import com.huawei.esdk.uc.im.VideoPlayerActivity;
import com.huawei.esdk.uc.im.VideoRecorderActivity;
import com.huawei.module.um.UmConstant;
import com.huawei.module.um.UmFunc;
import com.huawei.module.um.UmUtil;
import com.huawei.utils.StringUtil;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

/**
 * IM的工具类
 * 
 * */
public class ChatUtil {
	
	private static final String TAG = ChatUtil.class.getSimpleName();

	/**
     * 发送启动摄像头的intent
     */
    private static String startCamera(Activity context)
    {
        String picPath = UmUtil.createPhotoPath(UmConstant.JPG);

        Intent iCam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        iCam.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(picPath)));
        context.startActivityForResult(iCam, ChatActivity.PROCESS_UM_CAMERA);

        return picPath;
    }

    /**
     * 启动录制视频页面。
     */
    private static String startRecordVideo(Activity context)
    {
        String path = UmUtil.createPhotoPath(UmConstant.MP4);

        //判断支持自定义录制时，使用自定义录制。
        if (isSupportCustomVideoRecord())
        {
            Intent intent = new Intent(context, VideoRecorderActivity.class);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, path);
            context.startActivityForResult(intent, ChatActivity.PROCESS_UM_VIDEO);
        }
        else
        {
            File f = new File(path);
            Intent iVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            iVideo.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            iVideo.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            iVideo.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
            iVideo.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Integer.toString(1024 * 1024 * 2));//限制在2MB

            context.startActivityForResult(iVideo, ChatActivity.PROCESS_UM_VIDEO);
        }
        return path;
    }
    
    /**
     * 判断支持自定义拍摄视频
     * @return
     */
    public static boolean isSupportCustomVideoRecord()
    {
        //android 3.0版本以上。
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    
    public static String startCamera(Activity activity, boolean isVideo)
    {
        if (isVideo)
        {
            return startRecordVideo(activity);
        }
        else
        {
            return startCamera(activity);
        }
    }
    
    /**
     * 启动视屏预览界面
     * @param oldPath 被预览的视频所在的目录
     *@param isChoose 判断是否选择的视频
     */
    public static void startVideoPlayerActivity(Activity activity, String oldPath, boolean isChoose,
                                                int fromActivity, boolean deleteFlag)
    {
        Intent intent = new Intent(activity, VideoPlayerActivity.class);
        intent.putExtra(IntentData.VIDEO_PATH, oldPath);
        intent.putExtra(IntentData.CHOOSE, isChoose);
        intent.putExtra(IntentData.STATUS, VideoPlayerActivity.FROM_CHATACTIVITY);
        intent.putExtra(IntentData.FROM_ACTIVITY, fromActivity);
        intent.putExtra(IntentData.DELETE_FLAG, deleteFlag);
        activity.startActivityForResult(intent, ChatActivity.PROCESS_UM_VIDEO_SCAN);
    }
    
    /**
    * 清除UM缓存数据。
    */
   public static void clearTempUmData(int requestCode)
   {
       if (!isReadyToSend(requestCode))
       {
           return;
       }

       Logger.debug(TAG, "requestcode = " + requestCode);
       File file = new File(UmUtil.TEMP_PATH);
       com.huawei.utils.io.FileUtil.deleteFile(file);
   }
   
   /**
    * 判断是否准备好发送消息了。
    * @param requestCode
    * @return
    */
   private static boolean isReadyToSend(int requestCode)
   {
       return requestCode == ChatActivity.PROCESS_UM_CAMERA_SCAN
           || requestCode == ChatActivity.PROCESS_UM_VIDEO_SCAN;
   }
   
   /**
    * 判断发送内容是否包含敏感词
    * @param content 待发送内容
    * @return 包含敏感词，返回true，否则返回false
    */
   public static boolean containSensitiveWords(String content)
   {
       if (CommonVariables.getIns().getSensitive() == Constant.Sensitive.INSENSITIVE)
       {
           return false;
       }

       String[] arr = CommonVariables.getIns().getSensitiveContent();
       
       for (String sensitiveWord : arr)
       {
           if (!TextUtils.isEmpty(sensitiveWord)
                   && content.toLowerCase(Locale.getDefault()).contains(sensitiveWord.toLowerCase(Locale.getDefault())))
           {
               return true;
           }
       }
       return false;
   }

   /**
    * 敏感词过滤
    * @param content 未过滤文本
    * @return 过滤后的文本
    */
   public static String sensitiveFilter(String content)
   {
       String[] arr = CommonVariables.getIns().getSensitiveContent();
       
       char[] originalCharArr = content.toCharArray();
       
       for (String w : arr)
       {
           if (!TextUtils.isEmpty(w))
           {
               content = filter(content,w);
           }
       }
       char[] convertedCharArr = content.toCharArray();
       
       for (int i = 0; i < convertedCharArr.length; i++)
       {
           if (convertedCharArr[i] == '*' && originalCharArr[i] != '*')
           {
               originalCharArr[i] = '*';
           }
       }
       
       content = new String(originalCharArr);
       return content;
   }

   /**
    * 过滤敏感词汇（不区分大小写）
    * @param content 待过滤内容
    * @param sensitiveWord 敏感词
    */
   private static String filter(String content, String sensitiveWord)
   {
       String stars = getStars(sensitiveWord);
       content = content.toLowerCase(Locale.getDefault()).replaceAll(
               Pattern.quote(sensitiveWord.toLowerCase(Locale.getDefault())), stars);
       return content;
   }

   /**
    * 根据敏感词长度得到'*'串
    * @param sensitiveWord 敏感词
    */
   private static String getStars(String sensitiveWord)
   {
       StringBuffer sb = new StringBuffer("");
       for (int i = 0; i < sensitiveWord.length(); i++)
       {
           sb.append("\\*");
       }
       return sb.toString();
   }


}
