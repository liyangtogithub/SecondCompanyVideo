package com.adayo.mediaScanner;

public interface MediaScannerInterface {
	public static  enum STORAGE_PORT{
		STORAGE_ALL,
		STORAGE_SD,
		STORAGE_USB1,
		STORAGE_USB2,
		STORAGE_USB3,
		STORAGE_USB4,
		STORAGE_USB5,
		STORAGE_TF,
		INVALID,
	}
	
	public static  enum MEDIA_TYPE{
		AUDIO,
		VIDEO,
		IMAGE,
		INVALID,
	}
	
	public static enum SCANNING_STATE {
		NOT_START, 
		SCANNING_FILE, 
		SCANNING_ID3, 
		SCANNED_FINISH, 
	};
	
	public void registerCallBack(FilesStateChanged cb);
	public String[] getMountedPaths(STORAGE_PORT storage);
	public MediaObjectQueryInterface getQueryer(STORAGE_PORT storage);
	public interface MediaObjectQueryInterface {
		SCANNING_STATE getScanningState();
		
		//获取所有目录总数
		int getAllPathsNum(MEDIA_TYPE mediaType);
		
		//获取path下的文件总数，path为-1时，获取所有文件总数。
		int getAllFilesNum(MEDIA_TYPE mediaType,int pathIndex);

		//根据索引号获取文件名,path为-1时，索引号为所有文件中的索引号
		String getFileName(MEDIA_TYPE mediaType,int pathIndex, int fileIndex);

		//根据文件名获取在一个目录中的索引号,path为-1时，获取在所有文件中的索引号
		int getFileIndex(MEDIA_TYPE mediaType,int pathIndex, String fileName);
		
		//根据目录名获取在所有目录中的索引号
		int getPathIndex(MEDIA_TYPE mediaType,String path);
		
		//按目录浏览媒体文件接口：
		String[]  getPathsForBrowser(MEDIA_TYPE mediaType,String path);
		String[]  getFilesForBrowser(MEDIA_TYPE mediaType,String path);
		
		void setFavorite(String fullFileName,int flag);
		void setTop(String fullFileName,int flag);
		void setDelete(String fullFileName,int flag);
		void addPlayTimes(String fullFileName);
	}
	
	public interface FilesStateChanged {
		public void mediaStorageMounted(STORAGE_PORT storage);
		public void mediaStorageUnmounted(STORAGE_PORT storage);
		public void fileScanStart(STORAGE_PORT storage);
		public void fileScanEnd(STORAGE_PORT storage);
		public void fileSaveEnd(STORAGE_PORT storage);
		public void fileScanID3End(STORAGE_PORT storage);
		public void fileParseThumbnailEnd(STORAGE_PORT storage);
	}
}
