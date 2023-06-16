package com.adayo.mediaScanner.test;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.AdayoMediaScanner;
import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.MediaObjectQueryInterface;
import com.adayo.mediaScanner.fileScanner.PingYingTool;
import com.adayo.scanner.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class mainActivity extends Activity {

	private static final String TAG = "mainActivity";
	Button mBtnStartLog=null;
    /** Called when the activity is first created. */
    //HashMap<STORAGE_PORT, MediaObjectQueryInterface> mQuerys = new HashMap<STORAGE_PORT, MediaObjectQueryInterface>() ;
	AdayoMediaScanner mScanner = AdayoMediaScanner.getAdayoMediaScanner();

	public  static  native  int  jni_hdmi_update(byte[] data,int len);
	
    private class FilesStateChangedCB implements FilesStateChanged{

		@Override
		public void mediaStorageMounted(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "mediaStorageMounted:" + s);
		}

		@Override
		public void mediaStorageUnmounted(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "mediaStorageUnmounted:" +s);
		
		}

		@Override
		public void fileScanStart(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "fileScanStart:" + s);
		}

		@Override
		public void fileScanEnd(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "fileScanEnd:" + s);
		}
		
		@Override
		public void fileSaveEnd(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "fileSaveEnd:" + s);
		}

		@Override
		public void fileScanID3End(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "fileScanID3End:" + s);
		}

		@Override
		public void fileParseThumbnailEnd(STORAGE_PORT s) {
			// TODO Auto-generated method stub
			Log.i(TAG, "fileParseThumbnailEnd:" + s);
		}

    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScanner.registerCallBack(new FilesStateChangedCB());
        
		Intent intent1 = new Intent("com.adayo.mediaScanner.service.RunService");
		startService(intent1);
		
        setContentView(R.layout.main);
        mBtnStartLog = (Button) findViewById(R.id.startLog);
        mBtnStartLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String path = "/mnt/udisk1";
				
				 com.adayo.mediaScanner.fileScanner.MediaScanner
				   .getMediaScanner(getApplicationContext()).testScan(path);
				
				return ;
				
//				for (Iterator iter = mQuerys.entrySet().iterator(); iter.hasNext();) {
//					Map.Entry entry = (Map.Entry) (iter.next());
//					MediaObjectQueryInfterface query = (MediaObjectQueryInfterface) entry.getValue();
//					//testQuery(query);
//				}
//				
//				String[] pathsStrings =mScanner.getMountedPaths();
//				if(null!=pathsStrings)
//				for(int i=0;i<pathsStrings.length;i++){
//					Log.i(TAG, "getMountedPaths: " + pathsStrings[i]);
//					MediaObjectQueryInfterface query = mScanner.getQueryer(pathsStrings[i]);
//				}
			}
				 
        });
        
        Button btnstopLog = (Button) findViewById(R.id.stopLog);
        btnstopLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				jni_hdmi_update(null,0);
				String path = "/mnt/sdcard/external_usb/sda1/scan_test";
//				File startPath = new File(path);
//				startPath.mkdir();
//				Log.i(TAG, "new startPath: " + startPath);
//				for(int i=0;i<100000;i++){
//					File f = new File(path+"/"+ i + ".mp3");
//					try {
//						f.createNewFile();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				Log.e(TAG, "scanOneDirectory start " );
//				LinkedList<File> subFolderList = new LinkedList<File>();
//				LinkedList<File> audioFileList = new LinkedList<File>();
//				CommonUtil.scanOneDirectory(new File(path), subFolderList, audioFileList);
//				Log.e(TAG, "scanOneDirectory end " );
				
				//String[] pinyin = PinyinHelper.toHanyuPinyinStringArray('重');
				
			    
			    Log.i(TAG, "parseString: " + PingYingTool.parseString("重"));
			    Log.i(TAG, "parseString: " + PingYingTool.parseString("1重"));
			    Log.i(TAG, "parseString: " + PingYingTool.parseString("重1"));
			    Log.i(TAG, "parseString: " + PingYingTool.parseString("a重1"));
			    Log.i(TAG, "parseString: " + PingYingTool.parseString("重a"));
			}
		});
        
        Button btncopyLog = (Button) findViewById(R.id.copyLog);
        btncopyLog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//mScanner.registerCallBack(new FilesStateChangedCB());
				String[] paths = mScanner.getMountedPaths(STORAGE_PORT.STORAGE_USB1);
				if(paths==null){
					Log.i(TAG, "mScanner.getMountedPaths" );
					return;
				}
				MediaObjectQueryInterface query = mScanner.getQueryer(CommonUtil.getStoragePort("/mnt/udisk1"));
				String[] mountPaths =mScanner.getMountedPaths(STORAGE_PORT.STORAGE_USB1);
				if(null!=mountPaths)
				for(int i=0;i<mountPaths.length;i++){
					Log.i(TAG, "getMountedPaths: " + mountPaths[i]);
				}
				if(query==null){
					Log.e(TAG, "query==null" );
					return;
				}
				
				String[] pathsStrings = query.getPathsForBrowser( MEDIA_TYPE.AUDIO, mountPaths[0]);
				Log.i(TAG, "pathsStrings: " );
				printStrings(pathsStrings);
				
				String[] filesStrings = query.getFilesForBrowser(MEDIA_TYPE.AUDIO, mountPaths[0]);
				Log.i(TAG, "filesStrings: " );
				printStrings(filesStrings);
				
				String fullFileName = mountPaths[0]+"/1.mp3";
				query.setDelete(fullFileName, 1);
				query.setFavorite(fullFileName, 1);
				query.setTop(fullFileName, 1);
				
				fullFileName = mountPaths[0]+"/谭咏麟 - 讲不出再见.mp3";
				query.setDelete(fullFileName, 1);
				query.setFavorite(fullFileName, 1);
				query.setTop(fullFileName, 1);
			}
		});
    }
	
    private void printStrings(String[] strings){
    	if(strings==null){
    		Log.i(TAG, "strings==null " );
    		return;
    	}
		for(int i=0;i<strings.length;i++){
			Log.i(TAG, "printStrings: " + strings[i]);
		}
    }
    
	@Override
    protected void onDestroy() {
		super.onDestroy();
	
		Log.i(TAG, "onDestroy:task id: " + getTaskId());
    }
	
	@Override
    protected void onStart() {
		super.onStart();
		//Log.i(TAG, "onStart:task id: " + getTaskId());
    }
	
	@Override
    protected void onStop() {
		super.onStop();
		//Log.i(TAG, "onStop:task id: " + getTaskId());
    }
	
	@Override
    protected void onPause() {
		super.onPause();
		//Log.i(TAG, "onPause:task id: " + getTaskId());
    }
	
	@Override
    protected void onResume() {
		super.onResume();
		//Log.i(TAG, "onResume:task id: " + getTaskId());
    }
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		//Log.i(TAG, "onNewIntent:task id: " + getTaskId());
	/*
	         <activity
            android:name="com.foryou.mediaScanner.test.mainActivity"
            android:label="@string/app_name" >
            <intent-filter>
                <category android:name="android.intent.category.DEFAULT" />
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>    
         </activity>
         
           android:sharedUserId="android.uid.system"
           
            <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />

	 */
    }

}