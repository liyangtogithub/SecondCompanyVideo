package com.adayo.mediaScanner.fileScanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MountStorageReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Log.i("MountStorageReceiver", "BroadcastReceiver,action:" + intent.getAction());
			MediaScanner.getMediaScanner(context).mountReceiverHandle(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
