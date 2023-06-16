package com.adayo.videoplayer.receivers;

import com.adayo.midware.constant.CustomerIDConstantDef;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.CustomerIDConstantDef.CUSTOMER_ID;
import com.adayo.midware.constant.channelManagerDef.MEDIA_SOURCE_ID;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.core.VideoPlayController;
import com.adayo.videoplayer.fragments.VideoPlayFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SWCKeyReceiver extends BroadcastReceiver {

    private static final String TAG = SWCKeyReceiver.class.getSimpleName();

    protected static final int MSG_NEXT = 1;

    protected static final int MSG_PREV = 2;
    protected static final int TIME_ONE_THOUSAND = 1000;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEXT:

                    break;
                case MSG_PREV:
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Trace.d(TAG, "[SWCKeyReceivers]:  " + intent.getAction());
        if (SettingConstantsDef.MCU_KEY_IND_ACTION.equals(intent.getAction())) {
            String keyState = intent.getStringExtra(SettingConstantsDef.EXTRA_KEY_STATE);
            String keyValue = intent.getStringExtra(SettingConstantsDef.EXTRA_KEY_VALUE);
            Trace.d(TAG, "keyState: " + keyState + ",keyValue: " + keyValue);
            String sourceID = intent.getStringExtra(SettingConstantsDef.EXTRA_SOURCE_ID);
            Log.e(TAG, "source ID: " + sourceID);
            if (!MEDIA_SOURCE_ID.VIDEO.name().equals(sourceID)) {
                return;
            }
            VideoPlayController videoPlayController = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
            if ("PRESS_UP".equals(keyState) || "NONE".equals(keyState)) {
                context.sendBroadcast(new Intent(VideoPlayFragment.REFRESH_FRAGMENT_ACTION));
                if ("EQ".equals(keyValue)) {
                    Intent mIntent = new Intent();
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mIntent.setClassName("com.android.settings", "com.android.settings.eq.MainActivity");
                    context.startActivity(mIntent);
                } else if ("NEXT".equals(keyValue)) {
                    if (!mHandler.hasMessages(MSG_NEXT)) {
                        AdayoVideoPlayerApplication.instance().getVideoPlayControler().next();
                        mHandler.sendEmptyMessageDelayed(MSG_NEXT, 1 * TIME_ONE_THOUSAND);
                    }
                } else if ("PREV".equals(keyValue)) {
                    if (!mHandler.hasMessages(MSG_PREV)) {
                        AdayoVideoPlayerApplication.instance().getVideoPlayControler().prev();
                        mHandler.sendEmptyMessageDelayed(MSG_PREV, 1 * TIME_ONE_THOUSAND);
                    }
                } else if ("PLAY".equals(keyValue)) {
                    if (videoPlayController.isPaused()) {
                        videoPlayController.play();
                    } else if (videoPlayController.isPlaying()) {
                        videoPlayController.pause();
                    }
                } else if ("REPEAT".equals(keyValue)) {
                    videoPlayController.switchRepeatMode();
                    Trace.d(TAG, "[SWCKeyReceivers]:  showSecondPage keyValue==" + keyValue);
                    showSecondPage(context);
                }
            }

            if ("LONG_EVENT".equals(keyState)) {
                if ("PREV".equals(keyValue)) {
                    videoPlayController.playRW();
                } else if ("NEXT".equals(keyValue)) {
                    videoPlayController.playFF();
                }
                if (CustomerIDConstantDef.supportStarToRepeat()) {
                    if (keyValue.equals("STAR")) {
                        videoPlayController.switchRepeatMode();
                        Trace.d(TAG, "[SWCKeyReceivers]:  showSecondPage keyValue==" + keyValue);
                        showSecondPage(context);
                    } else if (keyValue.equals("NUMBER")) {
                        videoPlayController.switchShuffleMode();
                        Trace.d(TAG, "[SWCKeyReceivers]:  showSecondPage keyValue==" + keyValue);
                        showSecondPage(context);
                    }
                }
            }
            if ("LONG_UP".equals(keyState)) {
                if ("PREV".equals(keyValue) || "NEXT".equals(keyValue)) {
                    videoPlayController.setRealPlay();
                }
            }
        }
    }

    private void showSecondPage(Context context) {
        Intent repeatIntent = new Intent(VideoPlayFragment.REFRESH_FRAGMENT_ACTION);
        repeatIntent.putExtra(VideoPlayFragment.SHOW_SECOND_PAGE, true);
        context.sendBroadcast(repeatIntent);
    }

}
