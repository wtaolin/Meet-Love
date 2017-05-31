package com.huawei.esdk.uc.utils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.huawei.common.res.LocContext;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.module.um.UmConstant;
import com.huawei.utils.security.FileSHA1;

import java.io.File;


public class FileUtil {
	
	private static final String TAG = FileUtil.class.getSimpleName();
	
	public static String savePictureToAlbum(Context context, Bitmap bitmap, String name)
    {
        ContentResolver cr = LocContext.getContext().getContentResolver();
        String url = MediaStore.Images.Media.insertImage(cr, bitmap, name, "");

        if (url == null)
        {
            return null;
        }

        return getPath(context, Uri.parse((url)));
    }
	
	/**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri)
    {
        Logger.debug(TAG, " File -" +
                "Authority: " + uri.getAuthority() +
                ", Fragment: " + uri.getFragment() +
                ", Port: " + uri.getPort() +
                ", Query: " + uri.getQuery() +
                ", Scheme: " + uri.getScheme() +
                ", Host: " + uri.getHost() +
                ", Segments: " + uri.getPathSegments().toString()
        );

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri))
        {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri))
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type))
                {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri))
            {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri))
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type))
                {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type))
                {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type))
                {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                    split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
        {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
        {
            return uri.getPath();
        }

        return null;
    }
    
    /**
     * 通过URi查询文件路径
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return 返回的值为uri对应的路径值
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
        String[] selectionArgs)
    {
        if (context == null)
        {
            Logger.warn(TAG, "context is null!");
            return null;
        }

        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = { column };

        try
        {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null);
            if (cursor != null && cursor.moveToFirst())
            {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        finally
        {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
    
    /**
     * 将文件重命名并转移到新的目录里面。
     * @param filePath
     * @param newPath
     * @return
     */
    public static String renameWithSha1(String filePath, String newPath)
    {
        if (TextUtils.isEmpty(filePath))
        {
            Logger.warn(TAG, "filePath Path is null!");
            return filePath;
        }

        File file = new File(filePath);
        if (!file.exists())
        {
            Logger.warn(TAG, "file not exist!");
            return filePath;
        }

        String name = file.getName();
        int index = name.lastIndexOf(UmConstant.DOT);
        if (index <=0)
        {
            return filePath;
        }

        name = name.replaceFirst(name.substring(0, index), FileSHA1.getFileSha1(filePath));
        File newFile;
        if (newPath == null)
        {
            newFile = new File(file.getParent(), name);
        }
        else
        {
            createPath(newPath);
            newFile = new File(newPath, name);
        }

        if (newFile.exists())
        {
            Logger.debug(TAG, "new file exist, not need rename.");
            return newFile.getAbsolutePath();
        }

        if (file.renameTo(newFile))
        {
            return newFile.getAbsolutePath();
        }

        Logger.debug(TAG, "rename File fail.");
        return filePath;
    }
    
    public static boolean createPath(String path)
    {
        return createPath(path, true);
    }

    public static boolean createPath(String path, boolean isShowInAlbum)
    {
        File file = null;

        if (isShowInAlbum)
        {
            file = new File(path);
        }
        else
        {
            // 2.2不支持MediaStore.MEDIA_IGNORE_FILENAME
            file = new File(path, ".nomedia");
        }

        if (!file.exists())
        {
            return file.mkdirs();
        }

        return false;
    }
    
    /**
     * 返回文件大小
     *
     * @param path 文件路径
     * @return 文件大小
     */
    public static int getFileSize(String path)
    {
        return TextUtils.isEmpty(path) ? -1 : (int) new File(path)
            .length();
    }

}
