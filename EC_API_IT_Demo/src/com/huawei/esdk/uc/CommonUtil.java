package com.huawei.esdk.uc;

import android.text.TextUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.huawei.ecs.mtk.log.Logger;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;


public final class CommonUtil
{

    public static final String APPTAG = "UCAPIDEMO";
    
    /**
     * 0:正常的时间格式-20120830165150  1：秒格式  2： 毫秒格式
     */
    public static final int NORMAL_TIME_TYPE = 0;
    public static final int SECOND_TIME_TYPE = 1;
    public static final int MILLISECOND_TIME_TYPE = 2;
    
    
    /**
     * 处理不同String格式的时间
     *
     * @param time
     * @param timeType
     * @return
     */
    public static Timestamp getTimestamp(String time, int timeType)
    {
        return getTimestamp(time, timeType, null);
    }

    public static Timestamp getTimestamp(String time, int timeType, String tz)
    {
        if (TextUtils.isEmpty(time) || "0".equals(time))
        {
            return new Timestamp(0);
        }
        Timestamp timestamp = null;
        if (SECOND_TIME_TYPE == timeType)
        {
            timestamp = new Timestamp(Long.valueOf(time + "000"));
        }
        else if (MILLISECOND_TIME_TYPE == timeType)
        {
            timestamp = new Timestamp(Long.valueOf(time));
        }
        else if (NORMAL_TIME_TYPE == timeType)
        {
            //正常的时间格式，不需要特殊处理（如补位数等）
            timestamp = parseTimestamp(time.replaceAll("[\\D]", ""), tz);
        }
        return timestamp;
    }

    private static Timestamp parseTimestamp(String time, String tz)
    {
        DateFormat dateFormat = new SimpleDateFormat(DateUtil.FMT_YMDHMSMS_STRING);

        if (TextUtils.isEmpty(tz))
        {
            tz = "GMT+00:00";
        }

        TimeZone tZone = TimeZone.getTimeZone(tz);
        dateFormat.setTimeZone(tZone);

        Date date = null;

        try
        {
            date = dateFormat.parse(time);
        }
        catch (ParseException e)
        {
            Logger.error(APPTAG, "e :" + e.toString());
        }

        return null == date ? null : new Timestamp(date.getTime());
    }

}
