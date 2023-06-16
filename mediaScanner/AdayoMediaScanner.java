package com.adayo.mediaScanner;

import android.util.Log;

import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;

public class AdayoMediaScanner implements MediaScannerInterface 
{
	private static final String TAG = "AdayoMediaScanner";
	private ScannerManager mScannerManager;
	private static AdayoMediaScanner mAdayoMediaScanner = new AdayoMediaScanner();
	private AdayoMediaScanner(){
		mScannerManager = ScannerManager.getScannerManager();
	}
	
	public static AdayoMediaScanner getAdayoMediaScanner(){
		return mAdayoMediaScanner;
	}
	
	public void registerCallBack(FilesStateChanged cb) {
		mScannerManager.registerCallBack(cb);
	}
	
	public void unRegisterCallBack(FilesStateChanged cb) {
		mScannerManager.unRegisterCallBack(cb);
	}
	
	public boolean isMounted(String mountPath) {
		return mScannerManager.isMounted(mountPath);
	}
	
	public String[] getMountedPaths(STORAGE_PORT torage) {
		return mScannerManager.getAllMountPaths(torage);
	}
	
	public MediaObjectQueryInterface getQueryer(STORAGE_PORT storage){
		String[] paths = mScannerManager.getAllMountPaths(storage);
		if(paths==null){
			Log.e(TAG, "getQueryer return null. mScannerManager.getAllMountPaths(storage)==null");
		    return null;
		}
		for(String path:paths){
		   if(mScannerManager.isMounted(path))
			   return new MediaObjectQuery(storage);
		}
		
		Log.e(TAG, "getQueryer return null. no mounted path for:" + storage.toString());
		return null;
		
	}
	
	private class MediaObjectQuery implements MediaObjectQueryInterface{
       // private STORAGE_PORT mStorage;
        private String mMountPath = null;
		public MediaObjectQuery(STORAGE_PORT storage){
			//mStorage = storage;
			String[] paths = mScannerManager.getAllMountPaths(storage);
			for(String path:paths){
			   if(mScannerManager.isMounted(path)){
				   mMountPath=path;
			       break;
			   }
			}
		}
		
		@Override
		public SCANNING_STATE getScanningState(){
			return mScannerManager.getScanningState(mMountPath);
		}
		
		@Override
		public int getAllPathsNum(MEDIA_TYPE mediaType) {
			return mScannerManager.getAllPathsNum(mMountPath,mediaType);
		}

		@Override
		public int getAllFilesNum(MEDIA_TYPE mediaType,int pathIndex) {
			return mScannerManager.getAllFilesNum(mMountPath,mediaType,pathIndex);
		}

		@Override
		public String getFileName(MEDIA_TYPE mediaType,int pathIndex, int fileIndex) {
			return mScannerManager.getFileName(mMountPath,mediaType,pathIndex,fileIndex);
		}

		@Override
		public int getFileIndex(MEDIA_TYPE mediaType,int pathIndex, String fileName) {
			return mScannerManager.getFileIndex(mMountPath,mediaType,pathIndex,fileName);
		}

		@Override
		public int getPathIndex(MEDIA_TYPE mediaType,String path) {
			return mScannerManager.getPathIndex(mMountPath,mediaType,path);
		}

		@Override
		public String[] getPathsForBrowser(MEDIA_TYPE mediaType, String path) {
			return mScannerManager.getPathsForBrowser(mediaType,path);
		}

		@Override
		public String[] getFilesForBrowser(MEDIA_TYPE mediaType, String path) {
			return mScannerManager.getFilesForBrowser(mediaType,path);
		}

		@Override
		public void setFavorite(String fullFileName,int flag) {
			mScannerManager.setFavorite(fullFileName,flag);
		}

		@Override
		public void setTop(String fullFileName,int flag) {
			mScannerManager.setTop(fullFileName,flag);
		}

		@Override
		public void setDelete(String fullFileName,int flag) {
			mScannerManager.setDelete(fullFileName,flag);
		}

		@Override
		public void addPlayTimes(String fullFileName) {
			mScannerManager.addPlayTimes(fullFileName);
		}
	}


}
