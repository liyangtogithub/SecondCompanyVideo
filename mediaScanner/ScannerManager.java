package com.adayo.mediaScanner;

import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.SCANNING_STATE;
import com.adayo.midware.constant.ServiceConstants;
import com.adayo.midwareproxy.serviceConnection.ServiceConnection;
import com.adayo.midwareproxy.binder.callback.IMediaScannerCallBackInterface;
import com.adayo.midwareproxy.binder.service.IMediaScannerInterface;

public class ScannerManager extends ServiceConnection {
	private static final String TAG = "ScannerManager";
	private static ScannerManager mScannerManager = null;
	private static IMediaScannerInterface mService = null;
	private IBinder getServiceObj = null;
	private  List<FilesStateChanged> mFilesStateChangedCallBacks = new ArrayList<FilesStateChanged>();
	
	private static final int MSG_STORAGE_MOUNTED = 0;
	private static final int MSG_STORAGE_UNMOUNTED = 1;
	private static final int MSG_FILE_SCAN_START = 2;
	private static final int MSG_SCAN_FILE_END = 3;
	private static final int MSG_SAVE_FILE_END = 4;
	private static final int MSG_SCAN_ID3_END = 5;
	private static final int MSG_PARSE_THUMBNAIL_END = 6;
	
	private ScannerManager() {
		super();
		getServiceConnection();
	}

	public static ScannerManager getScannerManager() {
		if (mScannerManager == null) {
			mScannerManager = new ScannerManager();
		}
	
		return mScannerManager;
	}

	@Override
	public boolean getServiceConnection() {
		getServiceObj = connectService();
		if (null != getServiceObj) {
			Log.i(TAG, "getServiceConnection success");
			mService = IMediaScannerInterface.Stub.asInterface((IBinder) getServiceObj);
			try {
				if(mService!=null){
					Log.e(TAG, "scanner registerCallBack success");
					mService.registerCallBack(new IMediaScannerCallBackInterfaceImpl());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		} else {
			mService = null;
			return false;
		}
	}

	@Override
	public String getServiceName() {
		return ServiceConstants.SERVICE_NAME_MEDIASCANNER;
	}

	@Override
	public void serviceReConnected() {
		super.serviceReConnected();

	}

	@Override
	public void serviceDied() {
		mService = null;
		Log.e(TAG, "scanner died...");
		super.serviceDied();
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "handleMessage  cb msg.");
			
			if(mFilesStateChangedCallBacks.size()<=0)
				return;
			
			switch (msg.what) {
			case MSG_STORAGE_MOUNTED:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.mediaStorageMounted((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_STORAGE_UNMOUNTED:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.mediaStorageUnmounted((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_FILE_SCAN_START:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.fileScanStart((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_SCAN_FILE_END:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.fileScanEnd((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_SAVE_FILE_END:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.fileSaveEnd((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_SCAN_ID3_END:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.fileScanID3End((STORAGE_PORT)msg.obj);
				}
				break;
			case MSG_PARSE_THUMBNAIL_END:
				for(FilesStateChanged fs:mFilesStateChangedCallBacks){
					fs.fileParseThumbnailEnd((STORAGE_PORT)msg.obj);
				}
				break;
			}
		}
	};
	
	public boolean registerCallBack(FilesStateChanged cb) {
		if(mFilesStateChangedCallBacks.contains(cb)){
			return true;
		}
		mFilesStateChangedCallBacks.add(cb);
		return true;
	}

	public boolean unRegisterCallBack(FilesStateChanged cb) {
		try {
			mFilesStateChangedCallBacks.remove(cb);
			return true;//mService.unregisterCallBack();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}


	public final class IMediaScannerCallBackInterfaceImpl extends IMediaScannerCallBackInterface.Stub {

		@Override
		public void mediaStorageMounted(int storage)
				throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STORAGE_MOUNTED, s));
		}

		@Override
		public void mediaStorageUnmounted(int storage)
				throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STORAGE_UNMOUNTED, s));
		}

		@Override
		public void fileScanStart(int storage)
				throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_FILE_SCAN_START, s));
		}

		@Override
		public void fileScanFileEnd(int storage) throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_FILE_END, s));
		}
		
		@Override
		public void fileSaveFileEnd(int storage) throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_SAVE_FILE_END, s));
		}

		@Override
		public void fileScanID3End(int storage) throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_ID3_END, s));
		}

		@Override
		public void parseThumbnailEnd(int storage) throws RemoteException {
			STORAGE_PORT s = STORAGE_PORT.values()[storage];
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PARSE_THUMBNAIL_END, s));
		}

	}

	public String [] getAllMountPaths(STORAGE_PORT storage){
		try {
			return mService.getAllMountPaths(storage.ordinal());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public  boolean isMounted(String mountPath) {
		try {
			return mService.isMounted(mountPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public  SCANNING_STATE getScanningState(String mountPath) {
		try {
			return SCANNING_STATE.values()[mService.getScanningState(mountPath)];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return SCANNING_STATE.NOT_START;
		}
	}
	
	public int getAllPathsNum(String mountPath,MEDIA_TYPE mediaType) {
		try {
			return mService.getAllPathsNum(mountPath,mediaType.ordinal());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	
	public int getAllFilesNum(String mountPath,MEDIA_TYPE mediaType,int pathIndex) {
		try {
			return mService.getAllFilesNum(mountPath,mediaType.ordinal(),pathIndex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	
	public String getFileName(String mountPath,MEDIA_TYPE mediaType,int pathIndex, int fileIndex) {
		try {
			return mService.getFileName(mountPath,mediaType.ordinal(),pathIndex,fileIndex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	
	public int getFileIndex(String mountPath,MEDIA_TYPE mediaType,int pathIndex, String fileName) {
		try {
			return mService.getFileIndex(mountPath,mediaType.ordinal(),pathIndex,fileName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}


	public int getPathIndex(String mountPath,MEDIA_TYPE mediaType,String path) {
		try {
			return mService.getPathIndex(mountPath,mediaType.ordinal(),path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}


	// favorite
	public void setFavorite(String fullFileName,int flag) {
		try {
			 mService.setFavorite(fullFileName,flag);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
	}
	
	public void setTop(String fullFileName,int flag) {
		try {
			 mService.setTop(fullFileName,flag);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
	}
	
	public void setDelete(String fullFileName,int flag) {
		try {
			 mService.setDelete(fullFileName,flag);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
	}
	
	public void addPlayTimes(String fullFileName) {
		try {
			 mService.addPlayTimes(fullFileName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
	}
	
	public String[] getPathsForBrowser(MEDIA_TYPE mediaType, String path) {
		try {
			 return mService.getPathsForBrowser(mediaType.ordinal(),path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public String[] getFilesForBrowser(MEDIA_TYPE mediaType, String path) {
		try {
			 return mService.getFilesForBrowser(mediaType.ordinal(),path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
