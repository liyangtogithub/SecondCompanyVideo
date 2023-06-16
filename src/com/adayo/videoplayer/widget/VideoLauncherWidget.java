package com.adayo.videoplayer.widget;

import com.adayo.midware.constant.channelManagerDef;
import com.adayo.midwareproxy.sourceswitch.AdayoAppWidgetProvider;
import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.VideoMainActivity;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class VideoLauncherWidget extends AdayoAppWidgetProvider {

    private static final String TAG = VideoLauncherWidget.class.toString();

    private RemoteViews buildUpdate(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setOnClickPendingIntent(R.id.tv_widget_title, getLaunchPendingIntent(context));// PendingIntent.getActivity(context,
                                                                                             // 0,
                                                                                             // new
                                                                                             // Intent(context,
                                                                                             // VideoMainActivity.class),
                                                                                             // 0)
        return views;
    }

    private void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context);
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(new ComponentName(context.getPackageName(), getClass().getName()), views);
    }

    @Override
    public String getSourceActionName() {
        return channelManagerDef.AdayoVideoAction;
    }

    @Override
    public Bundle addDataToIntent() {
        return null;
    }

    @Override
    public void onReceiveAdayo(Context context, Intent intent) {
        Trace.d(TAG, "[onReceiveAdayo]");
        updateWidget(context);
    }

}
