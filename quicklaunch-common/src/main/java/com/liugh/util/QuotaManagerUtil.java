package com.liugh.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class QuotaManagerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaManagerUtil.class);

    private QuotaManagerUtil(){
        
    }
    
    public static boolean checkInstance(String checkScript) {
        boolean instanceDuplicate = false;
        Runtime run = Runtime.getRuntime();
        BufferedReader br = null;
        Process pro = null;
        try {
            pro = run.exec(checkScript);
            br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String str = "";
            while ((str = br.readLine()) != null) {
                if (Integer.parseInt(str.trim()) >= 2) {
                    instanceDuplicate = true;
                }
            }

        } catch (IOException e) {
            LOGGER.warn("checkInstance, ioexception", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (pro != null) {
                    pro.destroy();
                }

            } catch (IOException e) {
                LOGGER.warn("checkInstance,finally, ioexception", e);
            }
        }

        return instanceDuplicate;
    }



    public static long getMsFromFormatTimeWithTimeZone(String formatTime,
            String timeFormat, String timeZone) {  
    	long mstime = 0L;
        try {
        	SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
            Date date = null;            
            date = sdf.parse(formatTime);
            mstime = date.getTime();
        } catch (ParseException e) {
            LOGGER.warn("getMsFromFormatTimeWithTimeZone, ParseException", e);
        }

        return mstime;
    }

    public static String getFormatTimeFromMsWithTimeZone(long timems,
            String timeFormat, String timeZone) {
        Date d = new Date(timems);
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        return sdf.format(d);
    }
    
    public static String getMonthEnd(String strdate) {
        if (strdate.length() < 7){
            return null;
        }
        Calendar time = Calendar.getInstance();
        time.setTimeZone(TimeZone.getTimeZone("GMT"));
        time.clear();
        time.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        int month = Integer.parseInt(strdate.substring(5, 7));
        time.set(Calendar.MONTH, month - 1);
        int day = time.getActualMaximum(Calendar.DAY_OF_MONTH);
        return strdate + "-" + day;
    }

    public static String getCalendarIntervalDay(String strdate, int interval,
            String timezone) {
        if (strdate.length() < 10){
            return null;
        }
        Calendar time = Calendar.getInstance();
        time.setTimeZone(TimeZone.getTimeZone(timezone));
        time.clear();
        time.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        int month = Integer.parseInt(strdate.substring(5, 7));
        time.set(Calendar.MONTH, month - 1);
        int day = Integer.parseInt(strdate.substring(8, 10));
        time.set(Calendar.DATE, day);
        time.add(Calendar.DAY_OF_MONTH, interval - 1);
        return getFormatStringDay(time, "yyyy-MM-dd", timezone);
    }

    public static String getFormatStringDay(Calendar dayTime,
            String timeFormat, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.format(dayTime.getTime());
    }

    public static String getNextMonthDay(String strdate, String timezone) {
        if (strdate.length() < 10){
            return null;
        }
        Calendar time = Calendar.getInstance();
        time.setTimeZone(TimeZone.getTimeZone(timezone));
        time.clear();
        time.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        int month = Integer.parseInt(strdate.substring(5, 7));
        time.set(Calendar.MONTH, month - 1);
        int day = Integer.parseInt(strdate.substring(8, 10));
        time.set(Calendar.DATE, day);
        time.add(Calendar.MONTH, 1);
        time.add(Calendar.DATE, -1);
        return getFormatStringDay(time, "yyyy-MM-dd", timezone);
    }

    @SuppressWarnings("rawtypes")
    public static boolean judgeListIsNotNull(List list) {
        boolean listIsNull = false;
        if (list != null && !list.isEmpty()) {
            listIsNull = true;
        }
        return listIsNull;
    }

    @SuppressWarnings("rawtypes")
    public static boolean judgeMapIsNotNull(Map map) {
        boolean listIsNull = false;
        if (map != null && !map.isEmpty()) {
            listIsNull = true;
        }
        return listIsNull;
    }

    public static String getNextMonthString(String strdate, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar nextTime = Calendar
                .getInstance(TimeZone.getTimeZone(timezone));
        nextTime.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        nextTime.set(Calendar.MONTH,
                Integer.parseInt(strdate.substring(4, 6)) - 1);
        nextTime.add(Calendar.MONTH, 1);

        return formatter.format(nextTime.getTime());
    }

    public static String getNextIntervalMonthString(String strdate,
            String timezone, int interval) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar nextTime = Calendar
                .getInstance(TimeZone.getTimeZone(timezone));
        nextTime.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        nextTime.set(Calendar.MONTH,
                Integer.parseInt(strdate.substring(4, 6)) - 1);
        nextTime.add(Calendar.MONTH, interval);

        return formatter.format(nextTime.getTime());
    }
    
    /**
     * get first day of a month
     * current time is 01/09/2015,10:00
     * the result is 01/01/2015,10:00
     * return: void
     */
    public static long getFirstDayofMonth(String timeZone, long currentTime) {
        long firstDayLong=0L;
        TimeZone tz= TimeZone.getTimeZone(timeZone);
        Calendar cal = Calendar.getInstance(tz);
        cal.set(Calendar.DAY_OF_MONTH, Calendar.getInstance(tz).getActualMinimum(Calendar.DAY_OF_MONTH));
        firstDayLong=cal.getTimeInMillis();
        return firstDayLong;
    }
    
    public static String getPreviousDayString(String timezone, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar previousTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        previousTime.add(Calendar.DAY_OF_MONTH, -1);
        
        return formatter.format(previousTime.getTime());
    }
    
    public static String getPreDayString(String timezone, String format, String yyyyMMddString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        
        Date date = formatter.parse(yyyyMMddString);
        
        Calendar preDayTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        preDayTime.setTime(date);
        preDayTime.add(Calendar.DAY_OF_MONTH, -1);
        
        return formatter.format(preDayTime.getTime());
    }
    
    public static String getPreCertainDayString(String timezone, String format, String yyyyMMddString, int preDay) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        
        Date date = formatter.parse(yyyyMMddString);
        
        Calendar preDayTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        preDayTime.setTime(date);
        preDayTime.add(Calendar.DAY_OF_MONTH, -preDay);
        
        return formatter.format(preDayTime.getTime());
    }
    
    public static String getNextDayString(String timezone, String format, String yyyyMMddString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        
        Date date = formatter.parse(yyyyMMddString);
        
        Calendar nextDayTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        nextDayTime.setTime(date);
        nextDayTime.add(Calendar.DAY_OF_MONTH, 1);
        
        return formatter.format(nextDayTime.getTime());
    }
    
    
    public static String getCurrentDayString(String timezone, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        //previousTime.add(Calendar.DAY_OF_MONTH, -1);
        
        return formatter.format(currentTime.getTime());
    }
    
    public static String getPreMonthString(String strdate, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar nextTime = Calendar
                .getInstance(TimeZone.getTimeZone(timezone));
        nextTime.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        nextTime.set(Calendar.MONTH,
                Integer.parseInt(strdate.substring(4, 6)) - 1);
        nextTime.add(Calendar.MONTH, -1);

        return formatter.format(nextTime.getTime());
    }
    
    public static long getFirstDayLongOfCertainMonth(String strdate, String timezone, int count) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar nextTime = Calendar
                .getInstance(TimeZone.getTimeZone(timezone));
        nextTime.set(Calendar.YEAR, Integer.parseInt(strdate.substring(0, 4)));
        nextTime.set(Calendar.MONTH,
                Integer.parseInt(strdate.substring(4, 6)) - 1);
        nextTime.add(Calendar.MONTH, count);        
        String yyyyMM = formatter.format(nextTime.getTime());
        Date date;
        long result = 0L;
		try {
			date = formatter.parse(yyyyMM);
			result = date.getTime();
		} catch (ParseException e) {
			result=0L;
		}
        
        return result;
    }
    
    public static void main(String[] args) throws Exception {
		System.err.println(new Date());
	}
}
