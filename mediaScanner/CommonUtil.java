package com.adayo.mediaScanner;

import android.text.TextUtils;
import android.util.Log;

import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.midware.constant.CustomerIDConstantDef;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

public class CommonUtil {
	private static final String TAG = "CommonUtil";
	public static final String DEVID_NULL = "DEVID.NULL";
	public static final String ALBUM_PICTURE_PATH = "/mnt/sdcard/Android/picture";
	public static int THMUBNAIL_PIC_WIDTH = 250;
	public static int THMUBNAIL_PIC_HEIGHT = 240;
	public static int getTHMUBNAIL_PIC_WIDTH() {
		return THMUBNAIL_PIC_WIDTH;
	}

	public static void setTHMUBNAIL_PIC_WIDTH(int tHMUBNAIL_PIC_WIDTH) {
		THMUBNAIL_PIC_WIDTH = tHMUBNAIL_PIC_WIDTH;
	}

	public static int getTHMUBNAIL_PIC_HEIGHT() {
		return THMUBNAIL_PIC_HEIGHT;
	}

	public static void setTHMUBNAIL_PIC_HEIGHT(int tHMUBNAIL_PIC_HEIGHT) {
		THMUBNAIL_PIC_HEIGHT = tHMUBNAIL_PIC_HEIGHT;
	}

	private static LinkedList<String> AUDIO_FILE_FLAG=new LinkedList<String>();
	private static HashMap<String, STORAGE_PORT> mMountedPathToStorageMap = new HashMap<String, STORAGE_PORT>();
	private static List<STORAGE_PORT> SUPPORTED_STORAGE_PORTS = new ArrayList<STORAGE_PORT>();
	private static final String MOUNT_PATH_USB1 = "/mnt/udisk1";
	private static final String MOUNT_PATH_USB2 = "/mnt/udisk2";
	private static final String MOUNT_PATH_USB3 = "/mnt/udisk3";
	private static final String MOUNT_PATH_USB4 = "/mnt/udisk4";
	private static final String MOUNT_PATH_USB5 = "/mnt/udisk5";
	private static final String MOUNT_PATH_SD1 = "/mnt/ext_sdcard1";
	private static final String MOUNT_PATH_SD2 = "/mnt/ext_sdcard2";
	private static final String MOUNT_PATH_TF = "/mnt/sdcard/external_sd";
	static{
		
		mMountedPathToStorageMap.put(MOUNT_PATH_SD1, STORAGE_PORT.STORAGE_SD);
		mMountedPathToStorageMap.put(MOUNT_PATH_USB1, STORAGE_PORT.STORAGE_USB1);
		mMountedPathToStorageMap.put(MOUNT_PATH_USB2, STORAGE_PORT.STORAGE_USB2);
		mMountedPathToStorageMap.put(MOUNT_PATH_USB3, STORAGE_PORT.STORAGE_USB3);
		mMountedPathToStorageMap.put(MOUNT_PATH_USB4, STORAGE_PORT.STORAGE_USB4);
		mMountedPathToStorageMap.put(MOUNT_PATH_USB5, STORAGE_PORT.STORAGE_USB5);
		
		if(CustomerIDConstantDef.supportSTORAGE_TF()){
			mMountedPathToStorageMap.put(MOUNT_PATH_TF, STORAGE_PORT.STORAGE_TF);
		}
		
		SUPPORTED_STORAGE_PORTS.addAll(mMountedPathToStorageMap.values());
		
		Log.d(TAG, "has tf card " + SUPPORTED_STORAGE_PORTS.contains(STORAGE_PORT.STORAGE_TF));
		
		AUDIO_FILE_FLAG.add(".mp3");
		if(CustomerIDConstantDef.supportWMA())
			AUDIO_FILE_FLAG.add(".wma");
	    AUDIO_FILE_FLAG.add(".aac");
		AUDIO_FILE_FLAG.add(".ogg");
	    AUDIO_FILE_FLAG.add(".pcm");
		AUDIO_FILE_FLAG.add(".m4a");
		AUDIO_FILE_FLAG.add(".ac3");
		AUDIO_FILE_FLAG.add(".ec3");
		AUDIO_FILE_FLAG.add(".dtshd");
		AUDIO_FILE_FLAG.add(".ra");
		AUDIO_FILE_FLAG.add(".wav");
		AUDIO_FILE_FLAG.add(".cd");
		AUDIO_FILE_FLAG.add(".amr");
		AUDIO_FILE_FLAG.add(".mp2");
		AUDIO_FILE_FLAG.add(".ape");
		AUDIO_FILE_FLAG.add(".dts");
		AUDIO_FILE_FLAG.add(".flac");
		AUDIO_FILE_FLAG.add(".midi");
		AUDIO_FILE_FLAG.add(".mid");
		
	}
	
	private static LinkedList<String> VIDEO_FILE_FLAG=new LinkedList<String>();
	static{
		VIDEO_FILE_FLAG.add(".mpeg");
		VIDEO_FILE_FLAG.add(".mpg");
		VIDEO_FILE_FLAG.add(".mp4");
		VIDEO_FILE_FLAG.add(".m4v");
		VIDEO_FILE_FLAG.add(".3gp");
		VIDEO_FILE_FLAG.add(".3gpp");
		VIDEO_FILE_FLAG.add(".3g2");
		VIDEO_FILE_FLAG.add(".3gpp2");
		VIDEO_FILE_FLAG.add(".mkv");
		VIDEO_FILE_FLAG.add(".webm");
		VIDEO_FILE_FLAG.add(".ts");
		VIDEO_FILE_FLAG.add(".avi");
//		VIDEO_FILE_FLAG.add(".wmv");
//		VIDEO_FILE_FLAG.add(".asf");
		VIDEO_FILE_FLAG.add(".rm");
		VIDEO_FILE_FLAG.add(".rv");
		VIDEO_FILE_FLAG.add(".rmvb");
		VIDEO_FILE_FLAG.add(".mov");
		VIDEO_FILE_FLAG.add(".asx");
		VIDEO_FILE_FLAG.add(".mjpeg");
		VIDEO_FILE_FLAG.add(".f4v");
		VIDEO_FILE_FLAG.add(".flv");
//		VIDEO_FILE_FLAG.add(".divx");
		VIDEO_FILE_FLAG.add(".ogm");
		VIDEO_FILE_FLAG.add(".vob");
		VIDEO_FILE_FLAG.add(".xvid");
	}
	
	private static LinkedList<String> IMAGE_FILE_FLAG=new LinkedList<String>();
	static{
		IMAGE_FILE_FLAG.add(".jpg");
		IMAGE_FILE_FLAG.add(".jpeg");
		IMAGE_FILE_FLAG.add(".gif");
		IMAGE_FILE_FLAG.add(".png");
		IMAGE_FILE_FLAG.add(".bmp");
		IMAGE_FILE_FLAG.add(".wbmp");
		IMAGE_FILE_FLAG.add(".webp");
		IMAGE_FILE_FLAG.add(".mpg");
	}
	
	/**
	 * 获取支持的设备，有的客户不支持TF卡做为媒体卡
	 * @return
	 */
	public static STORAGE_PORT[] getSupportStoragePorts(){
		return SUPPORTED_STORAGE_PORTS.toArray(new STORAGE_PORT[0]);
	}
	
	/**
	 * 获取文件 的媒体类型
	 * @param fileName 文件全路径 
	 * @return 媒体类型
	 */
	public static  MEDIA_TYPE getMediaType(String fileName) {
		if(fileName == null)
			return MEDIA_TYPE.INVALID;
		
		int flagIndex = fileName.lastIndexOf(".");
		if(flagIndex<0)
			return MEDIA_TYPE.INVALID;
		
		String prefix=fileName.substring(flagIndex);
		if(AUDIO_FILE_FLAG.contains(prefix.toLowerCase(Locale.US))){
			return MEDIA_TYPE.AUDIO;
		}
		if(VIDEO_FILE_FLAG.contains(prefix.toLowerCase(Locale.US))){
			return MEDIA_TYPE.VIDEO;
		}
		if(IMAGE_FILE_FLAG.contains(prefix.toLowerCase(Locale.US))){
			return MEDIA_TYPE.IMAGE;
		}
		return MEDIA_TYPE.INVALID;
	}
	
	/**
	 * 比较两个字符串
	 * @param lData
	 * @param rData
	 * @return
	 */
	public static int compareStr(String lData, String rData) {
		String lName = lData;
		String rName = rData;
		if (lName != null && rName != null) {
			Collator collator = Collator.getInstance(Locale.getDefault());
			return collator.compare(lName.toLowerCase(Locale.getDefault()),
					                rName.toLowerCase(Locale.getDefault()));
		} else {
			return 0;
		}
	}
	
	public static Comparator<File> mComparator = new Comparator<File>() {
		@Override
		public int compare(File lData, File rData) {
			String lName = lData.getName();
			String rName = rData.getName();
			return compareStr(lName,rName);
		}
	};
	
	public static Comparator<String> mComparatorStr = new Comparator<String>() {
		@Override
		public int compare(String lData, String rData) {
			String lName = lData;
			String rName = rData;
			return compareStr(lName,rName);
		}
	};
	/**
	 * 获取目录下的所有音乐文件
	 * @param directory
	 * @param subFolderList 存放目录下所有的子目录
	 * @param audioFileList 存放目录下所有的音频文件
	 */
	public static void scanOneDirectory(final File directory,LinkedList<File> subFolderList,LinkedList<File> audioFileList) {
		if (!directory.isDirectory()) {
			//Log.w(TAG, "[scanOneDirectory]directory - " + directory.getAbsolutePath() + " is not a directory!");
			return;
		}
		if(subFolderList==null&&audioFileList==null){
			return;
		}
		if(subFolderList!=null){
			subFolderList.clear();
		}
		if(audioFileList!=null){
			audioFileList.clear();
		}
		
		File[] files = directory.listFiles();
		if (files == null || files.length <= 0) {
			//Log.w(TAG, "[scanOneDirectory]directory - " + directory.getAbsolutePath() + " has no sub file!");
			return;
		}

		for (int i = 0 ;i < files.length;i++) {
			File file = files[i];
			if (subFolderList!=null&&file.isDirectory()) {
				subFolderList.add(file);
			} else if (audioFileList!=null&&file.isFile()&&CommonUtil.getMediaType(file.getName())==MEDIA_TYPE.AUDIO) {
				audioFileList.add(file);
			}
		}

		if (subFolderList!=null&&subFolderList.size() > 0) {
			Collections.sort(subFolderList, mComparator);
		}
		if (audioFileList!=null&&audioFileList.size() > 0) {
			Collections.sort(audioFileList, mComparator);
		}
		return;
	}
	
	/**
	 * 获取目录下所有的音频文件
	 * @param path 目录路径
	 * @param audioFileList 存放目录下所有的音频
	 * @return
	 */
	public static int scanOneDirectory(final String path,LinkedList<String> audioFileList) {
		File directory = new File(path);
		if (!directory.isDirectory()) {
			//Log.w(TAG, "[scanOneDirectory]directory - " + directory.getAbsolutePath() + " is not a directory!");
			return 0; 
		}
		if(audioFileList==null){
			return 0;
		}
		if(audioFileList!=null){
			audioFileList.clear();
		}
		
		File[] files = directory.listFiles();
		if (files == null || files.length <= 0) {
			//Log.w(TAG, "[scanOneDirectory]directory - " + directory.getAbsolutePath() + " has no sub file!");
			return 0;
		}

		for (int i = 0 ;i < files.length;i++) {
			File file = files[i];
			if(audioFileList!=null&&file.isFile()&&CommonUtil.getMediaType(file.getName())==MEDIA_TYPE.AUDIO) {
				audioFileList.addLast(file.getName());
			}
		}

		if (audioFileList!=null&&audioFileList.size() > 0) {
			Collections.sort(audioFileList, mComparatorStr);
		}
		return audioFileList.size();
	}
	/**
	 * 获取设备的序列号
	 * @param mountPath 挂载路径
	 * @return 设备序号号
	 */
	public static String getDevID(String mountPath) {
		if(mountPath == null)
			return null;
		//String id =  "DevID_"+ mountPath;//getAndroidOSProperties("DEVID." + mountPath);
		String id =  getAndroidOSProperties("DEVID." + mountPath);
		if(id ==null || "".equals(id) || " ".equals(id)){
			return DEVID_NULL;//id =  "DevID_"+ mountPath;
		}
		id = id.replaceAll("/", "_");
		//Log.i(TAG, "get devid:" + id + " for path:" + mountPath);
		return id;
	}
	
	/**
	 * 设备是否存在
	 * @param mountPath 设备路径 
	 * @return true存在，false不存在
	 */
	public static  boolean isDeviceExsit(String mountPath){
		if(DEVID_NULL.equals(getDevID(mountPath))){
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param storage 设备
	 * @return 设备的挂载路径
	 */
	public static String getStorageMountPath(STORAGE_PORT storage){
		
		Iterator<Entry<String, STORAGE_PORT>> iterator = mMountedPathToStorageMap.entrySet().iterator();
		
		while(iterator.hasNext()){
			Entry<String, STORAGE_PORT> entry = iterator.next();
			if(entry.getValue().equals(storage))
				return entry.getKey();
		}
		return "";
	}
	/**
	 * 根据路径返回设备
	 * @param mountPath
	 * @return
	 */
	public static STORAGE_PORT getStoragePort(String mountPath){
		
		if(TextUtils.isEmpty(mountPath))
			return STORAGE_PORT.INVALID;
		
		Iterator<Entry<String, STORAGE_PORT>> iterator = mMountedPathToStorageMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, STORAGE_PORT> entry = iterator.next();
			if(mountPath.startsWith(entry.getKey()))
				return entry.getValue();
		}
		return STORAGE_PORT.INVALID;
	}
	/**
	 * 
	 * @param fileFullPath 媒体文件的路径
	 * @return 缩略图路径（视频、图片）
	 */
	public static String getThumbnailFilePath(String fileFullPath){
		return getPicFilePath(fileFullPath,"thumb");
	}
	
	/**
	 * 
	 * @param fileFullPath 音频文件路径
	 * @return ID3图片路径 （音频）
	 */
	public static String getAlbumPicFilePath(String fileFullPath){
		return getPicFilePath(fileFullPath,"album");
	}
	
	/**
	 * 
	 * @param fileFullPath 媒体文件的路径
	 * @param picPath 文件类型（"thumb" 或者 "album"）
	 * @return 缩略图路径（视频、图片）或者 ID3图片路径 （音频） 
	 */
	private static String getPicFilePath(String fileFullPath,String picPath){
		String mountPath = null;
//		if(fileFullPath.startsWith(MOUNT_PATH_USB1))
//			mountPath = MOUNT_PATH_USB1;
//		else if(fileFullPath.startsWith(MOUNT_PATH_USB2))
//			mountPath = MOUNT_PATH_USB2;
//		else if(fileFullPath.startsWith(MOUNT_PATH_USB3))
//			mountPath = MOUNT_PATH_USB3;
//		else if(fileFullPath.startsWith(MOUNT_PATH_USB4))
//			mountPath = MOUNT_PATH_USB4;
//		else if(fileFullPath.startsWith(MOUNT_PATH_USB5))
//			mountPath = MOUNT_PATH_USB5;
//		else if(fileFullPath.startsWith(MOUNT_PATH_SD1))
//			mountPath = MOUNT_PATH_SD1;
//		else if(fileFullPath.startsWith(MOUNT_PATH_SD2))
//			mountPath = MOUNT_PATH_SD2;
//		else 
//			return null;
		
		if(TextUtils.isEmpty(fileFullPath))
			return null;
		
		Iterator<Entry<String, STORAGE_PORT>> iterator = mMountedPathToStorageMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, STORAGE_PORT> entry = iterator.next();
			if(fileFullPath.startsWith(entry.getKey()))
				mountPath = entry.getKey(); 
		}
		
		if(TextUtils.isEmpty(mountPath))
			return null;
		
		
		
		String filePathNoMount = fileFullPath.replaceFirst(mountPath, "");
		String thumbFilePath = ALBUM_PICTURE_PATH + File.separator+ picPath + File.separator
				+ getDevID(mountPath)+ File.separator ;
		String fileName = thumbFilePath  + filePathNoMount.replaceAll(File.separator, "_") + ".png";
		
		return fileName;
	}
	/**
	 * 挂载路径是否需要当成媒体卡扫描
	 * @param path 挂载路径
	 * @return true为当成媒体卡，false不当成媒体卡
	 */
	public static boolean isNeedScanPathWhenMount(String path){
		if(path==null)
			return false;
		
		if(mMountedPathToStorageMap.get(path) != null)
			return true;
		
		
		
		
//		if(//mountPath.startsWith("/mnt/ext_sdcard") ||
//				path.equals(MOUNT_PATH_SD1)||
//				path.equals(MOUNT_PATH_SD2))
//			return true;
//		
//		if(getStoragePort(path)!=null && getStoragePort(path)!=STORAGE_PORT.INVALID){
//			return true;
//		}
		return false;
	}
	/**
	 * 挂载路径是否需要当成媒体卡扫描
	 * @param path 挂载路径
	 * @return true为当成媒体卡，false不当成媒体卡
	 */
	public static boolean isNeedScanPath(String path){
		
		
		if(mMountedPathToStorageMap.containsKey(path))
			return true;
		
//		if(path==null)
//			return false;
//		if(path.equals(MOUNT_PATH_USB1))
//			return true;
//		if(path.equals(MOUNT_PATH_USB2))
//			return true;
//		if(path.equals(MOUNT_PATH_USB3))
//			return true;
//		if(path.equals(MOUNT_PATH_USB4))
//			return true;
//		if(path.equals(MOUNT_PATH_USB5))
//			return true;
//		if(path.equals(MOUNT_PATH_SD1))
//			return true;
//		if(path.equals(MOUNT_PATH_SD2))
//			return true;
//		if(//mountPath.startsWith("/mnt/ext_sdcard") ||
//				path.equals(MOUNT_PATH_SD1)||
//				path.equals(MOUNT_PATH_SD2))
//			return true;
//		
//		if(getStoragePort(path)!=null && getStoragePort(path)!=STORAGE_PORT.INVALID){
//			return true;
//		}
		return false;
	}
	
	private static Object invokeStaticMethod(String className,
			String methodName, Object[] args) throws Exception {
		Class ownerClass = Class.forName(className);
		Class[] argsClass = new Class[args.length];
		for (int i = 0, j = args.length; i < j; i++) {
			argsClass[i] = args[i].getClass();
		}
		Method method = ownerClass.getMethod(methodName, argsClass);
		return method.invoke(null, args);
	}

	private static String getAndroidOSProperties(String key) {
		try {
			Object[] args = new Object[1];
			args[0] = key;
			String value = (String) invokeStaticMethod(
					"android.os.SystemProperties", "get", args);
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 检查路径 是否已经挂载
	 * @param mountPath 路径
	 * @return true为已挂载，false为没有挂载
	 */
	public static boolean checkMountPath(String mountPath){
	    if(getStoragePort(mountPath)==STORAGE_PORT.INVALID){
	        return false;
	    }else{
	        return true;
	    }
	}
	/**
	 * 
	 * @return 已经挂载的设备
	 */
	public static STORAGE_PORT[] getMountedStorage() {
		ArrayList<STORAGE_PORT> ports = new ArrayList<STORAGE_PORT>();
		for (STORAGE_PORT storage : SUPPORTED_STORAGE_PORTS) {
			if (AdayoMediaScanner.getAdayoMediaScanner().isMounted(getStorageMountPath(storage))){
				ports.add(storage);
			}
		}

		if(ports.size() > 0){
			STORAGE_PORT[] p = new STORAGE_PORT[ports.size()];
			for (int j = 0; j < ports.size(); j++) {
				p[j] = ports.get(j);
			}
			return p;
		}
		return null;
	}
}
