package com.adayo.videoplayer;

import com.adayo.videoplayer.core.VideoPlayController;

import android.view.View;

public class Constants {

    public static final int PLAYING = VideoPlayController.STARTED;
    public static final int PAUSE = VideoPlayController.PAUSED;
    public static final int STOP = VideoPlayController.STOPPED;

    public enum ShuffleMode {
        ON, OFF
    }

    public enum RepeateMode {
        ONE, ALL
    }

    enum DeviceType {
        ALL, SD, USB1, USB2, USB3, INVAILD,
    }

    public static final String SP_NAME = "video_play_save";
    public static final String SP_KEY_PLAY_PATH = "play_path";
    public static final String SP_KEY_PLAY_POSITION = "play_position";
    public static final String SP_KEY_SHUFFLE_MODE = "shuffle_mode";
    public static final String SP_KEY_REPEAT_MODE = "repeat_mode";
    public static final String SP_KEY_DEVICE = "device";

    public static int[] FIRST_PAGE_BUTTONS_IDS = new int[] { R.id.btn_goto_list_id, R.id.btn_play_prev,
            R.id.btn_play_pause_id, R.id.btn_play_next, R.id.btn_show_second_page_buttons_id };
    public static int[] SECOND_PAGE_BUTTONS_IDS = new int[] { R.id.btn_shuffle_id, R.id.btn_repeat_id,
    // R.id.btn_eq_id
    };
    public static int[] FIRST_PAGE_BUTTONS_RESIDS = new int[] { R.drawable.selector_goto_list_btn,
            R.drawable.selector_play_prev_btn, R.drawable.selector_play_btn, R.drawable.selector_play_next_btn,
            R.drawable.selector_show_second_page_buttons, };
    public static int[] SECOND_PAGE_BUTTONS_RESIDS = new int[] { R.drawable.selector_shuffle_none_btn,
            R.drawable.selector_repeat_all_btn,
    // R.drawable.selector_eq_btn
    };

    public static int[] FIRST_PAGE_BUTTONS_TITLES = new int[] { R.string.goto_list_text, View.NO_ID, View.NO_ID,
            View.NO_ID, R.string.show_more_buttons_text };
    public static int[] SECOND_PAGE_BUTTONS_TITLES = new int[] { R.string.shuffle_text, R.string.repeat_text,
    // R.string.eq_text,
    };
}
