package com.adayo.an6v.ui;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;

import com.adayo.mediaScanner.AdayoMediaScanner;
import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;

public class Utils {

	public static String getStoragePath(STORAGE_PORT device) {

		return CommonUtil.getStorageMountPath(device);
	}

	public static STORAGE_PORT[] getMountedStorage() {
		return CommonUtil.getMountedStorage();
	}

	public static String STORAGE_PORT_To_String(STORAGE_PORT storage,Context context){
		return getStorageName(storage,context);
	}
	
	public static boolean isArrayEmpty(Object[] array){
		return array == null || array.length == 0;
	}

	public static STORAGE_PORT getWhichStorageIn(String path) {
		return CommonUtil.getStoragePort(path);
	}
	
	private static STORAGE_PORT getStorageByPath(String fullPath){
		if(TextUtils.isEmpty(fullPath))
			return STORAGE_PORT.INVALID;
		
		String path = fullPath.endsWith(File.separator)?fullPath:fullPath + File.separator;
		
		for (STORAGE_PORT storage : STORAGE_PORT.values()) {
			if(path.startsWith(getStoragePath(storage)))
					return storage;
		}
		return STORAGE_PORT.INVALID;
	}
	
	public static boolean isRootDir(String fullPathString){
		for (STORAGE_PORT storage : STORAGE_PORT.values()) {
			if(getStoragePath(storage).equals(fullPathString))
				return true;
		}
		return false;
	}
	
	
	/**
	 * 把STORAGE_PORT转换成显示在界面的字符串
	 * @param storage
	 * @return
	 */
	public static String getStorageName(STORAGE_PORT storage,Context context){
		
		if(storage == null)
			return "";
		
		String name = "";
		
		switch (storage) {
		case STORAGE_USB1:
			return context.getResources().getString(R.string.usb1_text);
		case STORAGE_USB2:
			return context.getResources().getString(R.string.usb2_text);
		case STORAGE_USB3:
			return context.getResources().getString(R.string.usb3_text);
		case STORAGE_USB4:
			return context.getResources().getString(R.string.usb4_text);
		case STORAGE_USB5:
			return context.getResources().getString(R.string.usb5_text);
		case STORAGE_TF:
			return "TF";
		case STORAGE_SD:
			return context.getResources().getString(R.string.sd_text);
		case STORAGE_ALL:
			return context.getResources().getString(R.string.all_text);
		default:
			return "";
		}
	}
}
