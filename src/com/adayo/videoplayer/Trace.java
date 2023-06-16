package com.adayo.videoplayer;

import android.util.Log;

public class Trace {
    private static boolean PRINT_THRED_ID = true;

    public static void d(String tag, String log) {
        if (PRINT_THRED_ID) {
            // log += " ------ thread id " + Thread.currentThread().getId() +
            // " ------";
            Log.d(tag, log);
        }
    }

    public static void e(String tag, String log) {
        if (PRINT_THRED_ID) {
            // log += " ------ thread id " + Thread.currentThread().getId() +
            // " ------";
            Log.e(tag, log);
        }
    }

    public static void w(String tag, String log) {
        if (PRINT_THRED_ID) {
            // log += " ------ thread id " + Thread.currentThread().getId() +
            // " ------";
            Log.w(tag, log);
        }
    }

    public static void i(String tag, String log) {
        if (PRINT_THRED_ID) {
            // log += " ------ thread id " + Thread.currentThread().getId() +
            // " ------";
            Log.i(tag, log);
        }
    }
}
