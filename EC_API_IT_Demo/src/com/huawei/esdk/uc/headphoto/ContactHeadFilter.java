package com.huawei.esdk.uc.headphoto;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by h00203586 on 2015/8/26.
 */
public class ContactHeadFilter implements FilenameFilter
{
    String filter;

    ContactHeadFilter(String filter)
    {
        this.filter = filter;
    }

    @Override
    public boolean accept(File dir, String filename)
    {
        if (HeadPhotoUtil.SUFFIX.equalsIgnoreCase(filter))
        {
            return filename.endsWith(HeadPhotoUtil.SUFFIX);
        }
        else
        {
            return matchEspaceNumber(filename, filter);
        }
    }

    private boolean matchEspaceNumber(String fileName, String eSpaceNum)
    {
        int index = fileName.indexOf(HeadPhotoUtil.SEPARATOR);
        if (-1 != index)
        {
            String str = fileName.substring(0, index);
            return str.equalsIgnoreCase(eSpaceNum);
        }
        return false;
    }
}
