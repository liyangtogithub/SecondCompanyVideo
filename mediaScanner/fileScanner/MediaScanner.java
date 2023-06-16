package com.adayo.mediaScanner.fileScanner;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.SCANNING_STATE;
import com.adayo.mediaScanner.db.MediaDB;
import com.adayo.mediaScanner.service.RunService;

import android.R.string;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

public class MediaScanner { 
	private static final String TAG = "MediaScanner";
	private FilesStateChanged mFilesStateChangedRegisterCB = null;
	private FilesStateChangedCB mFilesStateChangedCB = null;
	private ConcurrentHashMap<String, FileScanner> mSannerMap = null;
	private ConcurrentHashMap<String, String> mPathDevidMap = null;
	private ConcurrentHashMap<STORAGE_PORT, LinkedList<String>> mMountPathMap = null;
	public static MediaDB mMediaDB = null;
	private Context mContext = null;
	private static MediaScanner mForyouMediaScanner = null;
	
	static{
		File nomedia = new File(CommonUtil.ALBUM_PICTURE_PATH + "/.nomedia");
		if (!nomedia.exists()){
			try {
		        if(!nomedia.getParentFile().exists()) {  
		            if(!nomedia.getParentFile().mkdirs()) {  
		            }  
		        } 
				nomedia.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private MediaScanner(Context context) {
		if (mContext == null)
			mContext = context;
		mSannerMap = new ConcurrentHashMap<String, FileScanner>();
		mPathDevidMap = new ConcurrentHashMap<String, String>();
		mMountPathMap = new ConcurrentHashMap<STORAGE_PORT, LinkedList<String>>();
		mFilesStateChangedCB = new FilesStateChangedCB();
	}

	public void startScanAfterCreate(){
		Log.d(TAG,"[startScanAfterCreate]");
		String[] paths = getVolumePaths(mContext);
		if(paths == null){
			Log.e(TAG, "no mounted path.");
			return;
		}
		for(int i=0;i<paths.length;i++){
			//Log.e(TAG, "mounted path:" + paths[i]);
			if(!CommonUtil.isNeedScanPath(paths[i]))
				continue;
			if(!CommonUtil.isDeviceExsit(paths[i]))
				continue;
			Log.e(TAG, "mounted path need scan:" + paths[i]);
			
			synchronized (TAG){
				String devID = CommonUtil.getDevID(paths[i]);
				FileScanner scanner = mSannerMap.get(devID);
				if(scanner==null){
					scanner = new FileScanner(mContext,mFilesStateChangedCB, paths[i], devID);
					mSannerMap.put(devID, scanner);
					mPathDevidMap.put(paths[i], devID);
					setMediaDB(scanner.getMediaDB());
					addMountPath(paths[i]);
					scanner.startScan();
				}else{
					Log.e(TAG, "device scanned when MediaScanner create: " + paths[i]);
				}
				
			}
		}
	}
	
	
	public static MediaScanner getMediaScanner(Context context) {
		synchronized (TAG) {
		    if(mForyouMediaScanner ==null){
				Intent intent1 = new Intent(RunService.SERVICECLASS);
				context.startService(intent1);
		    	mForyouMediaScanner = new MediaScanner(context);
		    }
		    return mForyouMediaScanner;
		}
	}

	public static MediaScanner getAdayoMediaScanner() {
		synchronized (TAG) {
			if(mForyouMediaScanner == null){
			    mForyouMediaScanner = new MediaScanner(RunService.getRunService().getContext());
			}
			return mForyouMediaScanner;
		}
	}

	public void register(FilesStateChanged cb) {
		mFilesStateChangedRegisterCB = cb;
	}
	
	private void setMediaDB(MediaDB db){
		synchronized (TAG) {
			if(mMediaDB == null){
				mMediaDB = db;
			}
		}
	}


	private class FilesStateChangedCB implements FilesStateChanged {
		@Override
		public void mediaStorageMounted(STORAGE_PORT s) {
            
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.mediaStorageMounted(s);
			}else{
				Log.e(TAG, "mediaStorageMounted mFilesStateChangedRegisterCB =null:");
			}
		}

		public void mediaStorageUnmounted(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.mediaStorageUnmounted(s);
			}else{
				Log.e(TAG, "mediaStorageUnmounted mFilesStateChangedRegisterCB =null:");
			}
		}

		@Override
		public void fileScanStart(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.fileScanStart(s);
			}else{
				Log.e(TAG, "fileScanStart mFilesStateChangedRegisterCB =null:");
			}
		}

		@Override
		public void fileScanEnd(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.fileScanEnd(s);
			}else{
				Log.e(TAG, "fileScanEnd mFilesStateChangedRegisterCB =null:");
			}
		}
		
		@Override
		public void fileSaveEnd(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.fileSaveEnd(s);
			}else{
				Log.e(TAG, "fileSaveEnd mFilesStateChangedRegisterCB =null:");
			}
		}

		@Override
		public void fileScanID3End(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.fileScanID3End(s);
			}else{
				Log.e(TAG, "fileScanID3End mFilesStateChangedRegisterCB =null:");
			}
		}

		@Override
		public void fileParseThumbnailEnd(STORAGE_PORT s) {
			if (mFilesStateChangedRegisterCB != null) {
				mFilesStateChangedRegisterCB.fileParseThumbnailEnd(s);
			}else{
				Log.e(TAG, "fileParseThumbnailEnd mFilesStateChangedRegisterCB =null:");
			}
		}
	}
	
	private String[] getVolumePaths(Context context){
		StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE); 
		String[] paths = null;
		try {
			paths = (String[]) sm.getClass().getMethod("getVolumePaths", null).invoke(sm, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return paths;
	}
	
	private String getMountState(String path){
		StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE); 
		String state = "unmounted";
		try {
		    Class[] argsClass = new Class[1];    
		    argsClass[0] = path.getClass();  
			state = (String) sm.getClass().getMethod("getVolumeState", argsClass).invoke(sm, path);
			if(!CommonUtil.isDeviceExsit(path))
				return "unmounted";
			else 
				return state;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return state;
	}

	public synchronized String[] getAllMountPaths(STORAGE_PORT s) {
		if (mSannerMap.size() == 0)
			return null;
		LinkedList<String> pathList = mMountPathMap.get(s);
		if(pathList == null)
			return null;
		return pathList.toArray(new String[0]);
	}
	
	public String getMountPath(String fullFileName) {
		if(fullFileName==null)
			return null;
		String[] paths = getAllMountPaths(STORAGE_PORT.STORAGE_ALL);
		for(String path:paths){
			if(fullFileName.startsWith(path))
				return path;
		}
		return null;
	}
	
	private synchronized void addMountPath(String mountPath){
		STORAGE_PORT s = CommonUtil.getStoragePort(mountPath);
		LinkedList<String> pathList = mMountPathMap.get(s);
		if(pathList ==null){
			pathList = new LinkedList<String>();
			mMountPathMap.put(s, pathList);
		}
		if(!pathList.contains(mountPath))
		    pathList.addLast(mountPath);
		
		LinkedList<String> all = mMountPathMap.get(STORAGE_PORT.STORAGE_ALL);
		if(all ==null){
			all = new LinkedList<String>();
			mMountPathMap.put(STORAGE_PORT.STORAGE_ALL, all);
		}
		if(!all.contains(mountPath))
			all.addLast(mountPath);
	}
	
	private synchronized void removeMountPath(String mountPath){
		STORAGE_PORT s = CommonUtil.getStoragePort(mountPath);
		LinkedList<String> pathList = mMountPathMap.get(s);
		if(pathList ==null){
			pathList = new LinkedList<String>();
			mMountPathMap.put(s, pathList);
		}
		pathList.remove(mountPath);
		
		LinkedList<String> all = mMountPathMap.get(STORAGE_PORT.STORAGE_ALL);
		if(all ==null){
			all = new LinkedList<String>();
			mMountPathMap.put(s, all);
		}
		all.remove(mountPath);
	}
	
	public boolean isMounted(String mountPath) {
		if(null==mountPath)
			return false;
		
		if(Environment.MEDIA_MOUNTED.equals(getMountState(mountPath))){
			Log.e(TAG, "isMounted return true. path: " + mountPath);
			return true;
		}
		
		String[] allPaths = getAllMountPaths(STORAGE_PORT.STORAGE_ALL);
		if(null!=allPaths){
			for(String path:allPaths){
				if(mountPath.startsWith(path) ||
				mountPath.equals(path)){
					Log.e(TAG, "isMounted return true. mount path: " + mountPath);
				    return true;
				}
			}
		}
		
		Log.e(TAG, "isMounted return false. path: " + mountPath);
		return false;
	}
	
	public SCANNING_STATE getScanningState(String mountPath){
		if(null==mountPath)
			return SCANNING_STATE.NOT_START;
		FileScanner scanner=mSannerMap.get(CommonUtil.getDevID(mountPath));
		if(scanner==null)
			return SCANNING_STATE.NOT_START;
		return scanner.getScanningState();
	}
	
	public FileScanner getFileScanner(String mountPath) {
		//Log.i(TAG, "getFileScanner,action:" + " path:" + path);
		return mSannerMap.get(CommonUtil.getDevID(mountPath));
	}

	public void mountReceiverHandle(Intent intent) {
		synchronized (TAG){
			String action = intent.getAction();
			if (action == Intent.ACTION_SCREEN_OFF||
					action == "com.device.poweroff" ) {
				Iterator<String> iterator = mSannerMap.keySet().iterator();
				while(iterator.hasNext()) {
					FileScanner scanner = mSannerMap.get(iterator.next());
					scanner.stopScanWhenSleep();
				}
				return;
			}  
			if (action == Intent.ACTION_SCREEN_ON||
					action == "com.device.poweron" ) {
				Iterator<String> iterator = mSannerMap.keySet().iterator();
				while(iterator.hasNext()) {
					FileScanner scanner = mSannerMap.get(iterator.next());
					scanner.startScanAfterSleep();
				}
				return;
			}  
			
			Uri uri = intent.getData();
			String path = uri.getPath();
	        STORAGE_PORT s = CommonUtil.getStoragePort(path);
			Log.i(TAG, "MountBroadcastReceiver,action:" + action + " path:" + path);
			if (action == Intent.ACTION_MEDIA_MOUNTED && path.equals("/mnt/sdcard")) {
				File nomedia = new File(CommonUtil.ALBUM_PICTURE_PATH + "/.nomedia");
				if (!nomedia.exists()) {
					try {
						nomedia.createNewFile();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			if (!CommonUtil.isNeedScanPathWhenMount(path)) {
				Log.i(TAG, "not scan for:" + path);
				return;
			}
			if (action == Intent.ACTION_MEDIA_MOUNTED) {
				if (!CommonUtil.isDeviceExsit(path)) {
					Log.i(TAG, "device not exits, not scan.");
					return;
				}
				
				String devID = CommonUtil.getDevID(path);
				FileScanner scanner = mSannerMap.get(devID);
				if(scanner==null){
					mFilesStateChangedCB.mediaStorageMounted(s);
					scanner = new FileScanner(mContext,mFilesStateChangedCB, path, devID);
					mSannerMap.put(devID, scanner);
					mPathDevidMap.put(path, devID);
					addMountPath(path);
					scanner.startScan();
					setMediaDB(scanner.getMediaDB());
				}else{
					Log.e(TAG, "device scanned before mount: " + path);
				}
			} else if (action == Intent.ACTION_MEDIA_UNMOUNTED 
					||action == Intent.ACTION_MEDIA_EJECT 
					||action == Intent.ACTION_MEDIA_REMOVED
					) {
				String devID = mPathDevidMap.get(path);
				if (devID!= null)
                {
					FileScanner scanner = mSannerMap.get(devID);
					if (scanner != null) {
						scanner.mediaUnmounted();
					} else {
						Log.e(TAG, "scanner null");
						// return;
					}
					mSannerMap.remove(devID);
				}
				mPathDevidMap.remove(path);
				removeMountPath(path);
				mFilesStateChangedCB.mediaStorageUnmounted(s);
			}
		}
	}

	public void testScan(String path){
		FileScanner  scanner = new FileScanner(mContext,mFilesStateChangedCB, path, CommonUtil.getDevID(path));
		if(scanner!=null)
			scanner.startScan();
		else {
			Log.i(TAG, "scanner null");
		}
	}
}
