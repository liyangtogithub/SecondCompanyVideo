package com.adayo.midwareproxy.binder.service;
 
import com.adayo.midwareproxy.binder.callback.IMediaScannerCallBackInterface;
import com.adayo.mediaScanner.MediaObjectName;
import com.adayo.mediaScanner.ID3Info;

interface IMediaScannerInterface {
    

	boolean registerCallBack(IMediaScannerCallBackInterface callbackInterface);

	 String [] getAllMountPaths(int storage);
	 boolean isMounted(String mountPath);
     int getScanningState(String mountPath);
     
	 //获取所有目录总数
	 int getAllPathsNum(String mountPath,int media_type);
		
	 //获取path下的文件总数，path为-1时，获取所有文件总数。
	 int getAllFilesNum(String mountPath,int media_type,int pathIndex);

	 //根据索引号获取文件名,path为-1时，索引号为所有文件中的索引号
	 String getFileName(String mountPath,int media_type,int pathIndex, int fileIndex);

	 //根据文件名获取在一个目录中的索引号,path为-1时，获取在所有文件中的索引号
	 int getFileIndex(String mountPath,int media_type, int pathIndex, String fileName);
		
	 //根据目录名获取在所有目录中的索引号
	 int getPathIndex(String mountPath,int media_type, String path);
	 
	 // favorite
	 void setFavorite(String fullFileName,int flag);
	 void setTop(String fullFileName,int flag);
	 void setDelete(String fullFileName,int flag);
	 void addPlayTimes(String fullFileName);
	 //按目录浏览媒体文件接口：
	 String[]  getPathsForBrowser(int media_type,String path);
	 String[]  getFilesForBrowser(int media_type,String path);

}
