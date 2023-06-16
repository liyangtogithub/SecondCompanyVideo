package com.adayo.mediaScanner.fileScanner;

import java.lang.reflect.Method;
import java.util.LinkedList;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.midwareproxy.binder.callback.IMediaScannerCallBackInterface;
import com.adayo.midwareproxy.binder.service.IMediaScannerInterface;

public class ScannerService extends IMediaScannerInterface.Stub {
    private static final String TAG = "ScannerService";

    private static ScannerService mScannerService = null;

    public static LinkedList<IMediaScannerCallBackInterface> mMediaScannerCallBack = new LinkedList<IMediaScannerCallBackInterface>();
  
    private ScannerService() {
    	MediaScanner.getAdayoMediaScanner().register(new FilesStateChangedCB());
    }

    public static ScannerService getService() {
        if (mScannerService == null) {
        	mScannerService = new ScannerService();
        }
        return mScannerService;
    }

    public  void addServiceToServiceManager(String serviceName) {
        Object object = new Object();
        Method addService;
        ScannerService serviceImpl;
        try {
            serviceImpl = getService();
            addService = Class.forName("android.os.ServiceManager").getMethod("addService", String.class, IBinder.class);
            addService.invoke(object, serviceName, serviceImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private synchronized void addMediaScannerCallBack(IMediaScannerCallBackInterface cb) {
    	mMediaScannerCallBack.addLast(cb);
	}
    
    private synchronized void removeMediaScannerCallBack(IMediaScannerCallBackInterface cb) {
    	mMediaScannerCallBack.remove(cb);
    	
	}
    private class FilesStateChangedCB implements FilesStateChanged{

		@Override
		public void mediaStorageMounted(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: mediaStorageMounted");
					return;
				}
				Log.i(TAG, "callback: mediaStorageMounted");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.mediaStorageMounted(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void mediaStorageUnmounted(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: mediaStorageUnmounted");
					return;
				}
				Log.i(TAG, "callback: mediaStorageUnmounted");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.mediaStorageUnmounted(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void fileScanStart(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: fileScanStart");
					return;
				}
				Log.i(TAG, "callback: fileScanStart");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.fileScanStart(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void fileScanEnd(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: fileScanEnd");
					return;
				}
				Log.i(TAG, "callback: fileScanPathEnd");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.fileScanFileEnd(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void fileSaveEnd(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: fileSaveEnd");
					return;
				}
				Log.i(TAG, "callback: fileSaveFileEnd");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.fileSaveFileEnd(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void fileScanID3End(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: fileScanID3End");
					return;
				}
				Log.i(TAG, "callback: fileScanID3End");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.fileScanID3End(s.ordinal());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void fileParseThumbnailEnd(STORAGE_PORT s) {
			try {
				if(mMediaScannerCallBack.size()==0){
					Log.e(TAG, "not callback: fileParseThumbnailEnd");
					return;
				}
				Log.i(TAG, "callback: fileParseThumbnailEnd");
				for(IMediaScannerCallBackInterface cb:mMediaScannerCallBack)
					cb.parseThumbnailEnd(s.ordinal());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

    	
    }
    
	private final class MediaScannerCallBacDeathNotifier implements IBinder.DeathRecipient {
		IMediaScannerCallBackInterface mIMediaScannerCallBackInterface;
		@Override
		public void binderDied() {
			Log.e(TAG, "[binderDied]: " + mIMediaScannerCallBackInterface.toString());
			//mMediaScannerCallBack.asBinder().unlinkToDeath(arg0, arg1)
			removeMediaScannerCallBack(mIMediaScannerCallBackInterface);
		}
		MediaScannerCallBacDeathNotifier(IMediaScannerCallBackInterface cb){
			mIMediaScannerCallBackInterface = cb;
		}
	}

	@Override
	public boolean registerCallBack(IMediaScannerCallBackInterface callbackInterface)
			throws RemoteException {
		Log.e(TAG, "[registerCallBack]: " + callbackInterface.toString());
		addMediaScannerCallBack(callbackInterface);
		callbackInterface.asBinder().linkToDeath(new MediaScannerCallBacDeathNotifier(callbackInterface), 0);
		return false;
	}

	@Override
	public String[] getAllMountPaths(int storage) throws RemoteException {
		STORAGE_PORT s= STORAGE_PORT.values()[storage];
		return MediaScanner.getAdayoMediaScanner().getAllMountPaths(s);
	}

	@Override
	public boolean isMounted(String mountPath) throws RemoteException {
		return MediaScanner.getAdayoMediaScanner().isMounted(mountPath);
	}

	@Override
	public int getScanningState(String mountPath) throws RemoteException {
		return MediaScanner.getAdayoMediaScanner().getScanningState(mountPath).ordinal();
	}
	
	@Override
	public int getAllPathsNum(String mountPath,int media_type) {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return 0;
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getAllPathsNum(m);
	}

	@Override
	public int getAllFilesNum(String mountPath, int media_type,int pathIndex) {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return 0;
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getAllFilesNum(m,pathIndex);
	}

	@Override
	public String getFileName(String mountPath, int media_type,int pathIndex, int fileIndex) {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return null;
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getFileName(m,pathIndex,fileIndex);
	}

	@Override
	public int getFileIndex(String mountPath, int media_type,int pathIndex, String fileName) {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return 0;
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getFileIndex(m,pathIndex,fileName);
	}

	@Override
	public int getPathIndex(String mountPath, int media_type,String path) {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return 0;
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getPathIndex(m,path);
	}

	@Override
	public void setFavorite(String fullFileName,int flag) throws RemoteException {
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(fullFileName);
		if(mountPath==null)
			return;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return ;
		MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).setFavorite(fullFileName,flag);
	}
	
	@Override
	public void setTop(String fullFileName,int flag) throws RemoteException {
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(fullFileName);
		if(mountPath==null)
			return;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return ;
		MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).setTop(fullFileName,flag);
	}
	@Override
	public void setDelete(String fullFileName,int flag) throws RemoteException {
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(fullFileName);
		if(mountPath==null)
			return;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return ;
		MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).setDelete(fullFileName,flag);
	}
	@Override
	public void addPlayTimes(String fullFileName) throws RemoteException {
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(fullFileName);
		if(mountPath==null)
			return;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return ;
		MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).addPlayTimes(fullFileName);
	}

	@Override
	public String[] getPathsForBrowser(int media_type,String path) throws RemoteException {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(path);
		if(mountPath==null)
			return null;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return null;
		
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getPathsForBrowser(m,path);
	}

	@Override
	public String[] getFilesForBrowser(int media_type,String path) throws RemoteException {
		MEDIA_TYPE m = MEDIA_TYPE.values()[media_type];
		String mountPath = MediaScanner.getAdayoMediaScanner().getMountPath(path);
		if(mountPath==null)
			return null;
		if(MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath)==null)
			return null;
		
		return MediaScanner.getAdayoMediaScanner().getFileScanner(mountPath).getFilesForBrowser(m,path);
	}

}
