package com.adayo.mediaScanner.service;

import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.db.MediaDB;
import com.adayo.mediaScanner.fileScanner.MediaScanner;
import com.adayo.mediaScanner.fileScanner.MountStorageReceiver;
import com.adayo.mediaScanner.fileScanner.ScannerService;
import com.adayo.midware.constant.ServiceConstants;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midwareproxy.binder.service.IServiceHeartBeat;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

 
public class RunService extends Service {
    private static final String TAG = "RunService";
    public static final String SERVICECLASS = "com.foryou.mediaScanner.service.RunService";
    protected static final int CHECK_LOCK_PASS = 1;
    private Context mContext = null;
    private static RunService mRunService = null;
    MountStorageReceiver mMountStorageReceiver;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        
        configureThumbnailWidthHeight();
        
        mRunService = this;
        ScannerService.getService().addServiceToServiceManager(ServiceConstants.SERVICE_NAME_MEDIASCANNER);
//        MountStorageReceiver mMountStorageReceiver = new MountStorageReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.device.poweroff");
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction("com.device.poweron");
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(new MountStorageReceiver(), filter);
		
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addDataScheme("file");
		registerReceiver(new MountStorageReceiver(), filter);
		MediaScanner.getMediaScanner(mContext).startScanAfterCreate();
		
		
		filter = new IntentFilter(SettingConstantsDef.ACTION_FACTORY_RESET);
		
		registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				if(SettingConstantsDef.ACTION_FACTORY_RESET.equals(intent.getAction())){
					Log.w(TAG, SettingConstantsDef.ACTION_FACTORY_RESET);
					clearData();
				}
			}
			
		}, filter);
		
        Log.e(TAG, "onCreate scaner service");
    }

    private void configureThumbnailWidthHeight() {
    	DisplayMetrics metrics = new DisplayMetrics(); 
    	WindowManager manager = (WindowManager) mContext.getSystemService(Service.WINDOW_SERVICE);
    	manager.getDefaultDisplay().getMetrics(metrics);
    	if(metrics.widthPixels == 0 || metrics.heightPixels == 0){
    		Log.e(TAG,"[configureThumbnailWidthHeight]:can't get DisplayMetrics");
    		return;
    	}
    	if(metrics.widthPixels == 800 && metrics.heightPixels == 480){
    		Log.d(TAG,"[configureThumbnailWidthHeight]:configure 800x480");
    		CommonUtil.setTHMUBNAIL_PIC_HEIGHT(191);
    		CommonUtil.setTHMUBNAIL_PIC_WIDTH(196);
    	}else if(metrics.widthPixels == 1024 && metrics.heightPixels == 600){
    		Log.d(TAG,"[configureThumbnailWidthHeight]:configure 1024x600");
    		CommonUtil.setTHMUBNAIL_PIC_HEIGHT(240);
    		CommonUtil.setTHMUBNAIL_PIC_WIDTH(250);
    	}
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        
        //return START_STICKY;
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy(){
    	Log.e(TAG, "onDestroy");
    	//MediaDB.closeWriteDB();
        return ;
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mHeartBeatBinder;
	}
    
	HeartBeatBinder mHeartBeatBinder = new HeartBeatBinder();
    private class HeartBeatBinder extends IServiceHeartBeat.Stub{
		@Override
		public int getBeatNum(int num) throws RemoteException {
			// TODO Auto-generated method stub
			return num;
		}
     }
    
    public Context getContext(){
    	return mContext;
    }
    
    public static RunService getRunService(){
    	return mRunService;
    }
    
	private void clearData() {
		try{
			Runtime.getRuntime().exec("pm clear " + mContext.getPackageName());
		}catch(Exception e){
			e.printStackTrace();
		}
	}   
   
}