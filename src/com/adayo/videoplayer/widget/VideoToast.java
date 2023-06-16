package com.adayo.videoplayer.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;

public class VideoToast {

    private static final String TAG = VideoToast.class.getSimpleName();
    private Toast mToast;
    Context mContext;

    public VideoToast(Context context) {
        mContext = context;
        mToast = new Toast(context);

        View view = LayoutInflater.from(context).inflate(R.layout.toast, null);

        mToast.setView(view);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.setDuration(Toast.LENGTH_LONG);
    }

    public void showText(String text) {
        // mToast.cancel();
        mToast.setText(text);
        mToast.show();
    }

    public void cancel() {
        mToast.cancel();
    }

    public void showText(int id) {
        String text = mContext.getString(id);
        Trace.i(TAG, "[showText]:" + text);
        showText(text);
    }

}
