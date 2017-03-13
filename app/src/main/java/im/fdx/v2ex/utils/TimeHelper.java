package im.fdx.v2ex.utils;

import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import im.fdx.v2ex.MyApp;
import im.fdx.v2ex.R;

/**
 * Created by a708 on 15-9-9.
 * 获取相对时间
 */
public class TimeHelper {


    /**
     * @param created 若等-1 （目前设定），则为刚刚。
     * @return
     */
    public static String getRelativeTime(long created) {
        if (created == -1L) {
            return "";
        }

        created = created * 1000;
        long now = System.currentTimeMillis();
        long difference = now - created;
        CharSequence text = (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                MyApp.getInstance().getString(R.string.just_now) :
                DateUtils.getRelativeTimeSpanString(
                        created,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);

        return text.toString();
    }


    public static String getAbsoluteTime(long created) {
        created *= 1000;

        DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.MEDIUM,Locale.CHINA);
        return format.format(created);


    }

    /**
     * 遗憾的是只能通过这样得到一个不准确的时间。
     * 算出绝对时间是为了保存如缓存，不然可以直接用得到的时间展示。
     *
     * @param timeStr
     * @return
     */
    public static long toLong(String timeStr) {

//       String timeStr = time.replace("&nbsp", "");
//        44 分钟前用 iPhone 发布
//         · 1 小时 34 分钟前 · 775 次点击
//         · 100 天前 · 775 次点击
        //1992.02.03 12:22:22 +0800
        //其中可能出现一些奇怪的字符，你可能以为是空格。
        long created = System.currentTimeMillis() / 1000; // ms -> second

        int second = timeStr.indexOf("秒");
        int hour = timeStr.indexOf("小时");
        int minute = timeStr.indexOf("分钟");
        int day = timeStr.indexOf("天");

        if (second != -1) {
            return created;
        } else if (hour != -1) {
            created -= Long.parseLong(timeStr.substring(0, hour).split(" ")[1]) * 60 * 60 +
                    Long.parseLong(timeStr.substring(hour + 2, minute).trim()) * 60;
        } else if (day != -1) {
            created -= Long.parseLong(timeStr.substring(0, day).split(" ")[1]) * 60 * 60 * 24;
        } else if (minute != -1) {
            created -= Long.parseLong(timeStr.substring(0, minute).split(" ")[1]) * 60;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
                Date date = sdf.parse(timeStr.split("\\+")[0]);
                created = date.getTime() / 1000;
            } catch (Exception ignored) {
            }
        }

        return created;
    }
}
