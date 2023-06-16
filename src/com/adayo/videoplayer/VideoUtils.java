package com.adayo.videoplayer;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

import com.adayo.an6v.ui.Utils;
import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;

public class VideoUtils {

    public static final int TIME_1000 = 1000;
    public static final int TIME_60 = 60;
    public static final int TIME_10 = 10;
    public static boolean isArrayEmpty(Object[] list) {
        return list == null || list.length == 0;
    }

    public static String toDeviceText(STORAGE_PORT storage) {
        return Utils.getStorageName(storage, AdayoVideoPlayerApplication.instance());
    }

    /**
     * 
     * @param time
     *            单位是MS
     * @return
     */
    public static String toTimeFormat(long timeMs) {

        StringBuilder sb = new StringBuilder();
        timeMs /= TIME_1000;
        long s = timeMs % TIME_60;
        timeMs /= TIME_60;
        long minute = timeMs % TIME_60;
        long hour = timeMs / TIME_60;
        if (hour != 0) {

            sb.append(hour);
            sb.append(":");
        }
        if (minute != 0) {
            if (minute < TIME_10){
                sb.append("0");
            }
            sb.append(minute);
            sb.append(":");
        } else {
            sb.append("00:");
        }
        if (s < TIME_10){
            sb.append("0");
        }
        sb.append(s);
        return sb.toString();
    }

    public static String getTitle(String path) {

        if (path == null){
            return path;
        }
        int idx = path.lastIndexOf(File.separator);

        if (idx == -1){
            return path;
        }
        return path.substring(idx + 1);

    }

}
