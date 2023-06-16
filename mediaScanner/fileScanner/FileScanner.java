package com.adayo.mediaScanner.fileScanner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.ID3Info;
import com.adayo.mediaScanner.MediaObjectName;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.SCANNING_STATE;
import com.adayo.mediaScanner.db.MediaDB;
import com.adayo.mediaScanner.db.MediaObjectList;
import com.adayo.mediaScanner.db.MediaScannerDBHelper;

public class FileScanner {
	private static final String TAG = "Scanner";
    private static final long MilSecondsNewTime = 24*60*60*1000;
	private static final int MSG_SCAN_FILE_END = 0;
	private static final int MSG_SAVE_FILE_END = 1;
	private static final int MSG_SCAN_ID3_END = 2;
	private static final int MSG_PARSE_THUMBNAIL_END = 3;
	private static final int CAPTURE_POSITION =  5*1000*1000;
	private long mLastModified =  0;   //最近修改的文件的modified时间，用于new属性比较。
	private FilesStateChanged mFilesStateChangedCallBack = null;
	private MediaScannerDBHelper mMediaScannerDBHelper = null;
	public MediaScannerDBHelper getMediaScannerDBHelper() {
		return mMediaScannerDBHelper;
	}
  
	private String mMountPath;
	private SCANNING_STATE mScanningState = SCANNING_STATE.NOT_START;
	private Handler mHandler;
    private ScanThread mScanThread =null;
    private boolean mIsSleep = false;
    
	private static Comparator<FileEntry> mComparatorFileEntry = new Comparator<FileEntry>() {
		@Override
		public int compare(FileEntry lData, FileEntry rData) {
			String lName = lData.mFileNamePYForDB;
			String rName = rData.mFileNamePYForDB;
			return CommonUtil.compareStr(lName,rName);
		}
	};
	
	public class FileEntry {
		public String mName;
		public boolean mIsNewSearched;
		public long mModTime;
        public String mPathNameForDB;
        public String mFileNamePYForDB;
		public FileEntry(String path,String name, long modTime) {
			mName = name;
			mModTime = modTime;
			mIsNewSearched = true;
			mFileNamePYForDB = PingYingTool.parseString(name);
			mPathNameForDB = path.replace(mMountPath, "") + "/";
		}
	}
	private class AllFilesInOnePath{
		public LinkedList<FileEntry> mFilesList = null;
		public HashMap<String,FileEntry> mFilesMap =  null;
		public String mPath;
		public int mAllPreNum;
		public int mNumFileInPath;
		public AllFilesInOnePath(String path, int preNum,int num){
			mPath = path;
			mAllPreNum = preNum;
			mNumFileInPath = num;
		}
	}
	private class MediaPathList{
		public LinkedList<AllFilesInOnePath> mAllPathsList = null;
		public  int mFilesNum;
		public MediaPathList(LinkedList<AllFilesInOnePath> allPathsList){
			mAllPathsList = allPathsList;
			mFilesNum = 0;
		}
	}
    private HashMap<MEDIA_TYPE, MediaPathList> mMediaPathsList = new HashMap<MEDIA_TYPE, MediaPathList>();
    private HashMap<MEDIA_TYPE, MediaDirectory> mMediaDirectoryBrower = new HashMap<MEDIA_TYPE, MediaDirectory>();
    
	private LinkedList<AllFilesInOnePath> mAllPathListAudio ;
	private LinkedList<AllFilesInOnePath> mAllPathListVideo ;
	private LinkedList<AllFilesInOnePath> mAllPathListImage ;
	private HashMap<String, AllFilesInOnePath>  mAllPathMapAudio;
	private HashMap<String, AllFilesInOnePath>  mAllPathMapVideo;
	private HashMap<String, AllFilesInOnePath>  mAllPathMapImage;
	
	private MediaDirectory mRootMediaDirectoryAudio;
	private MediaDirectory mRootMediaDirectoryVideo;
	private MediaDirectory mRootMediaDirectoryImage;
	
	public FileScanner(Context context, FilesStateChanged cb, String path, String devID) {
		mFilesStateChangedCallBack = cb;
		mMountPath = path;
		mMediaScannerDBHelper = new MediaScannerDBHelper(context, path, devID);

		mAllPathListAudio = new LinkedList<AllFilesInOnePath>();
		mAllPathListVideo = new LinkedList<AllFilesInOnePath>();
		mAllPathListImage = new LinkedList<AllFilesInOnePath>();
		mAllPathMapAudio = new HashMap<String, AllFilesInOnePath>();
		mAllPathMapVideo = new HashMap<String, AllFilesInOnePath>();
		mAllPathMapImage = new HashMap<String, AllFilesInOnePath>();
		mMediaPathsList.put(MEDIA_TYPE.AUDIO, new MediaPathList(mAllPathListAudio));
		mMediaPathsList.put(MEDIA_TYPE.VIDEO, new MediaPathList(mAllPathListVideo));
		mMediaPathsList.put(MEDIA_TYPE.IMAGE, new MediaPathList(mAllPathListImage));
		
		mRootMediaDirectoryAudio = new MediaDirectory(mMountPath);
		mRootMediaDirectoryVideo = new MediaDirectory(mMountPath);
		mRootMediaDirectoryImage = new MediaDirectory(mMountPath);
		mMediaDirectoryBrower.put(MEDIA_TYPE.AUDIO, mRootMediaDirectoryAudio);
		mMediaDirectoryBrower.put(MEDIA_TYPE.VIDEO, mRootMediaDirectoryVideo);
		mMediaDirectoryBrower.put(MEDIA_TYPE.IMAGE, mRootMediaDirectoryImage);
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Log.i(TAG, "handleMessage:" + msg.what);
				switch (msg.what) {
				case MSG_SCAN_FILE_END:
					mFilesStateChangedCallBack.fileScanEnd(CommonUtil.getStoragePort(mMountPath));
					break;
				case MSG_SAVE_FILE_END:
					mFilesStateChangedCallBack.fileSaveEnd(CommonUtil.getStoragePort(mMountPath));
					break;
				case MSG_SCAN_ID3_END:
					mFilesStateChangedCallBack.fileScanID3End(CommonUtil.getStoragePort(mMountPath));
					break;
				case MSG_PARSE_THUMBNAIL_END:
					mFilesStateChangedCallBack.fileParseThumbnailEnd(CommonUtil.getStoragePort(mMountPath));
					break;
				default:
					Log.e(TAG, "unexpected msg.");
					break;
				}
			}
		};
	}
	
	public MediaDB getMediaDB(){
		return mMediaScannerDBHelper.getMediaDB();
	}
	
	public MediaObjectList getMediaObjectList(int index){
		return mMediaScannerDBHelper.getMediaObjectList(index);
	}

	public SCANNING_STATE getScanningState(){
	    return mScanningState;	
	}
	
	private synchronized void setScanningState(SCANNING_STATE state){
	    mScanningState = state;	
	}
	
	public int getAllPathsNum(MEDIA_TYPE media) {
		return mMediaPathsList.get(media).mAllPathsList.size();	
	}

	public int getAllFilesNum(MEDIA_TYPE media,int pathIndex) {
		int filesNum = mMediaPathsList.get(media).mFilesNum;
		LinkedList<AllFilesInOnePath> mediaPathList = mMediaPathsList.get(media).mAllPathsList;
		Log.i(TAG, "getAllFilesNum:" + pathIndex + ", mFilesNum:" + filesNum 
				+ " mScanningState:" + mScanningState);
		
		if(pathIndex<0)
			return filesNum;
		AllFilesInOnePath onePath = mediaPathList.get(pathIndex);
		if(onePath==null)
		    return 0;
		return onePath.mNumFileInPath;
	}

	public String getFileName(MEDIA_TYPE media, int pathIndex, int fileIndex) {
		LinkedList<AllFilesInOnePath> mediaPathList = mMediaPathsList.get(media).mAllPathsList;
		if (mediaPathList.size() == 0)
			return null;
		
		AllFilesInOnePath onePath = null;
		if(pathIndex<0){
			if(fileIndex<0)
				return null;

			for (Iterator<AllFilesInOnePath> iter = mediaPathList.iterator(); iter.hasNext();) {
				AllFilesInOnePath entry = (AllFilesInOnePath) (iter.next());
				if(entry.mAllPreNum<(fileIndex+1) && (fileIndex+1)<=(entry.mAllPreNum+entry.mNumFileInPath)){
					onePath = entry;
					break;
				}
			}
			if(onePath == null)
			   return null;
				
			String fileName = onePath.mFilesList.get(fileIndex - onePath.mAllPreNum).mName;
			if(fileName!=null){
				return onePath.mPath +"/" + fileName;
			}
			return null;
		}else{
			onePath = mediaPathList.get(pathIndex);
			if(onePath == null)
				return null;

			String fileName = onePath.mFilesList.get(fileIndex).mName;
			if(fileName!=null){
				return onePath.mPath +"/" + fileName;
			}
			return null;
		}
	}

	public int getFileIndex(MEDIA_TYPE media, int pathIndex, String fileName) {
		LinkedList<AllFilesInOnePath> mediaPathList = mMediaPathsList.get(media).mAllPathsList;
		
		if(fileName==null)
			return -1;
		String path = fileName.substring(0, fileName.lastIndexOf("/"));
		if(path==null)
			return -1;
		AllFilesInOnePath onePath = null;
		for (Iterator<AllFilesInOnePath> iter = mediaPathList.iterator(); iter.hasNext();) {
			AllFilesInOnePath entry = (AllFilesInOnePath) (iter.next());
			//Log.e(TAG, "getFileIndex mPath:" + entry.mPath + " path:" + path);
			if(entry.mPath.equals(path)){
				onePath = entry;
				break;
			}
		}
		if(onePath==null)
			return -1;
		String name = fileName.substring(fileName.lastIndexOf("/")+1);
		
		int index =0;
		boolean foundName = false;
		for (Iterator<FileEntry> iter = onePath.mFilesList.iterator(); iter.hasNext();) {
			String entry = (String) (iter.next().mName);
			//Log.e(TAG, "getFileIndex entry:" + entry + " name:" + name);
			if(entry.equals(name)){
				foundName = true;
				break;
			}
			index++;
		}
		if(!foundName)
			return -1;
		if(pathIndex<0){
			return index + onePath.mAllPreNum;
		}else{
			return index;
		}
	}

	public int getPathIndex(MEDIA_TYPE media, String path) {
		LinkedList<AllFilesInOnePath> mediaPathList = mMediaPathsList.get(media).mAllPathsList;
		
		if(path==null)
			return -1;
		int index =0;
		for (Iterator<AllFilesInOnePath> iter = mediaPathList.iterator(); iter.hasNext();) {
			AllFilesInOnePath entry = (AllFilesInOnePath) (iter.next());
			if(entry.mPath.equals(path)){
				return index;
			}
			index++;
		}
		return -1;
	}
	
	
	public String[] getPathsForBrowser(MEDIA_TYPE media_type,String path){
	    MediaDirectory rootDir = mMediaDirectoryBrower.get(media_type);
		MediaDirectory dir = getOneMediaDirectory(rootDir, path);
		if(dir.mSubDirectories==null)
			return null;
		int size = dir.mSubDirectories.size();
		if(size == 0)
	        return null;
		
		if(dir.mSubDirectoriesStringsForBrowser == null){
			dir.mSubDirectoriesStringsForBrowser = new String[size];
		    for(int i=0;i<size;i++){
		    	dir.mSubDirectoriesStringsForBrowser[i] = dir.mSubDirectories.get(i).mDirName;
		    }
		}
		return dir.mSubDirectoriesStringsForBrowser;
	}

	public String[] getFilesForBrowser(MEDIA_TYPE media_type,String path){
	    MediaDirectory rootDir = mMediaDirectoryBrower.get(media_type);
		MediaDirectory dir = getOneMediaDirectory(rootDir, path);
		if(dir.mFiles==null)
			return null;
		
		int size = dir.mFiles.size();
		if(size == 0)
	        return null;
		
		if(dir.mFilesStringForBrowser==null){
			dir.mFilesStringForBrowser = new String[size];
			for(int i=0;i<size;i++){
				dir.mFilesStringForBrowser[i] = dir.mFiles.get(i).mName;
			}
		}
		return dir.mFilesStringForBrowser;
	}
	
	public void setFavorite(String fullFileName,int flag){
		fullFileName=fullFileName.replace(mMountPath, "");
		mMediaScannerDBHelper.setFavorite(fullFileName, flag);
	}
	public void setTop(String fullFileName,int flag){
		fullFileName=fullFileName.replace(mMountPath, "");
		mMediaScannerDBHelper.setTop(fullFileName, flag);
	}
	public void setDelete(String fullFileName,int flag){
		fullFileName=fullFileName.replace(mMountPath, "");
		mMediaScannerDBHelper.setDelete(fullFileName, flag);
	}
	public void addPlayTimes(String fullFileName){
		fullFileName=fullFileName.replace(mMountPath, "");
		mMediaScannerDBHelper.addPlayTimes(fullFileName);
	}
	//////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized void startScan() {
		mMediaScannerDBHelper.updateAllFilesToNotExist();
		killScanThread();
		mAllPathListAudio.clear();
		mAllPathListVideo.clear();
		mAllPathListImage.clear();
	    mScanThread = new ScanThread(mMountPath);
		mScanThread.start();
		mFilesStateChangedCallBack.fileScanStart(CommonUtil.getStoragePort(mMountPath));
		
	}
	
	public synchronized void stopScanWhenSleep() {
		Log.i(TAG, "stopScanWhenSleep:" + mMountPath);
		mIsSleep = true;
	}
	public synchronized void startScanAfterSleep() {
		Log.i(TAG, "startScanAfterSleep:" + mMountPath);
		mIsSleep = false;
	}
	private void waitWhenSleep(){
		if(mIsSleep){
			while(true){
				try {
					Thread.sleep(1000);
				
				    if(!mIsSleep || Thread.interrupted()){
					    Log.i(TAG, "break wait when sleep:" + mMountPath);
					    break;
				    }
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}

		}
	}
	
	private void killScanThread() {
		if (mScanThread != null) {
			if (mScanThread.isAlive()) {
				Log.e(TAG, "kill mScanThread");
				mScanThread.interrupt();
			}
		}
		mScanThread = null;
		
		for(Thread thread:mThreadParseID3){
			if(thread.isAlive()){
				thread.interrupt();
			}
		}
	}

	private LinkedList<MediaObjectName> mFilesNeedParseID3 = new LinkedList<MediaObjectName>();
	private LinkedList<Thread> mThreadParseID3 = new LinkedList<Thread>();
	private int mParseTaskEndNum = 0;
	private int PARSEID3_TASK_NUM = 3;
	private synchronized void parseWorkEnd(){
		mParseTaskEndNum ++;
	}
	private boolean isParseWorkEnd(){
		return mParseTaskEndNum == PARSEID3_TASK_NUM?true:false;
	}
	private class ScanThread extends Thread {
		public ScanThread(String path) {
			super("ScanThread:" + path);
		}

		// 扫描过程：1.先把扫描得到的音频文件 的 path和file保存到mAllPathList；
		// 2.根据mAllPathList更新DB
		public void run() {
			try {
				Log.d(TAG,"start scan:mount path " + mMountPath);
				setScanningState(SCANNING_STATE.SCANNING_FILE);
				if (!searchPaths(mMountPath)) {// not mount when power on
					setScanningState(SCANNING_STATE.NOT_START);
					mScanThread = null;
					return;
				}
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_FILE_END));

				if (Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath)) {
					mScanThread = null;
					return;
				}
				setScanningState(SCANNING_STATE.SCANNING_FILE);
				saveSeachedMediaFiles(mMountPath);

				// Log.e(TAG, "scan file end:" + mMountPath);
				if (!CommonUtil.isDeviceExsit(mMountPath)) {
					mScanThread = null;
					return;
				}
				setScanningState(SCANNING_STATE.SCANNING_ID3);
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SAVE_FILE_END));
				
				if(startAndWaitParseID3Thread()){
					Log.e(TAG, "startAndWaitParseID3Thread break,thread exit" + mMountPath);
					return;
				}
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_ID3_END));
				parseThumbNail();
				mHandler.sendMessage(mHandler.obtainMessage(MSG_PARSE_THUMBNAIL_END));
				setScanningState(SCANNING_STATE.SCANNED_FINISH);
			} catch (Exception e) {
				if (getScanningState() == SCANNING_STATE.SCANNING_FILE)
					mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_FILE_END));
				if (getScanningState() == SCANNING_STATE.SCANNING_ID3)
					mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_ID3_END));
				setScanningState(SCANNING_STATE.SCANNED_FINISH);
				e.printStackTrace();
			}
			mScanThread = null;
		}
	}
	
	private boolean startAndWaitParseID3Thread(){
		if(null == getAllFilesNeedParseID3(mFilesNeedParseID3)){
			Log.e(TAG, "getAllFilesNeedParseID3 return null, not start to parse id3." );
			return false;
		}
		
		int filesNumAll = mFilesNeedParseID3.size();
		if(filesNumAll == 0){
			Log.e(TAG, "getAllFilesNeedParseID3 filesNumAll==0, not start to parse id3." );
			return false;
		}
		
		Log.e(TAG, "startAndWaitParseID3Thread files num: " + mFilesNeedParseID3.size());
		
		int filesNumForOneTask = filesNumAll/PARSEID3_TASK_NUM;
		int filesNumLeft = filesNumAll%PARSEID3_TASK_NUM;
		if(filesNumForOneTask==0)
			PARSEID3_TASK_NUM = 1;
		
		for(int i=0;i<PARSEID3_TASK_NUM;i++){
			int startIndex = i*filesNumForOneTask;
			int endIndex = startIndex + filesNumForOneTask-1;
			if(i==PARSEID3_TASK_NUM-1)
				endIndex += filesNumLeft;
			
			Thread taskThread = new ParseID3Task(startIndex,endIndex);
			mThreadParseID3.addLast(taskThread);
			taskThread.start();
		}
		
		while(true){
			try {
				Thread.sleep(1000);
			}catch (InterruptedException e) {
				return true;
	        }
			if(isParseWorkEnd())
				return false;
			if(Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath))
				return true;
		}
	}
	
	private class ParseID3Task extends Thread {
		private int mStartIndex;
		private int mEndIndex;
		public ParseID3Task(int startIndex,int endIndex) {
			super("ParseID3Task:" + startIndex + "," + endIndex);
			mStartIndex = startIndex;
			mEndIndex = endIndex;
		}

		public void run() {
			try {
				Log.e(TAG, "scanID3 files start:" + "ParseID3Task:" + mStartIndex + "," + mEndIndex);
				parseID3Work(mStartIndex,mEndIndex);
				parseWorkEnd();
				Log.e(TAG, "scanID3 files end:" + "ParseID3Task:" + mStartIndex + "," + mEndIndex);
				
			} catch (Exception e) {
				e.printStackTrace();
				parseWorkEnd();
			}
		}
	}

	private void parseID3Work(int startIndex,int endIndex){
		LinkedList<ID3Info> id3PasesdID3 = new LinkedList<ID3Info>();
		for (int i = startIndex; i <= endIndex; i++) {
			try {
				waitWhenSleep();
				if (!CommonUtil.isDeviceExsit(mMountPath)) {
					Log.e(TAG, "scanID3 SCAN_TERMINATED:etScanningState() == SCANNING_STATE.NOT_START." + mMountPath);
					return;
				}
				if(Thread.interrupted()){
					Log.e(TAG, "scanID3 SCAN_TERMINATED:thread.interrupted()" + mMountPath);
					return;
				}
				
				MediaObjectName fileName = mFilesNeedParseID3.get(i);
				ID3Info id3 = parseID3(fileName);
				id3PasesdID3.addLast(id3);
				if(id3PasesdID3.size()%100==0){
					mMediaScannerDBHelper.saveID3(id3PasesdID3);
					id3PasesdID3.clear();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(id3PasesdID3.size()>0){
			mMediaScannerDBHelper.saveID3(id3PasesdID3);
			id3PasesdID3.clear();
		}
		Log.e(TAG, "parse end,start to save:" + " startIndex" + "," + endIndex);
	}

	private LinkedList<MediaObjectName> getAllFilesNeedParseID3(LinkedList<MediaObjectName> filesNeedParse){
		MediaObjectList files = mMediaScannerDBHelper.getFilesID3NotExist();
		if(files==null){
			Log.e(TAG, "getAllFilesNeedParseID3 no files:" + mMountPath);
			return null;
		}
		for (int i = 0; i < files.getSize(); i++) {
			if (Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath)) {
				Log.e(TAG, "getAllFilesNeedParseID3 terminated:" + mMountPath);
				files.releaseCursor();
				return null;
			}
			MediaObjectName fileName = files.getNextMediaObject();
			if(fileName!=null)
			     filesNeedParse.addLast(fileName);
			//ID3Info id3 = parseID3(fileName);
			//mMediaScannerDBHelper.saveID3(id3);
		}
		files.releaseCursor();
		return filesNeedParse;
	}
	
	public void mediaUnmounted() {
		setScanningState(SCANNING_STATE.NOT_START);
		killScanThread();
		mMediaScannerDBHelper.updateAllFilesToNotExist();
		mMediaScannerDBHelper.releaseDB();
		//mAllPathList = null;
	}

	private void saveAlbumPcture(ID3Info id3,byte[] picData){
		if(picData==null){
			return;
		}
		String picName = CommonUtil.getAlbumPicFilePath(id3.file.getName());
		try {
			File picFile = new File(picName);
			File parent = picFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			picFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(picName);
			fos.write(picData);
			fos.close();
			id3.picPath = picName;
			//Log.i(TAG, "save one pic: " + picName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ID3Info parseID3(MediaObjectName file) {
		ID3Info id3 = new ID3Info();
		id3.file = file;
		MediaMetadataRetriever retriever = null;
		boolean id3MediaFail = false;
		try {
			retriever = new MediaMetadataRetriever();
			retriever.setDataSource(file.getName());
			//retriever.setMode(MediaMetadataRetriever.MODE_GET_METADATA_ONLY); 
			id3.title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
			id3.duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			id3.track_number = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
			id3.album = new MediaObjectName(-1,retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
			id3.artist = new MediaObjectName(-1,retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
			id3.composer = new MediaObjectName(-1,retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
			id3.genre = new MediaObjectName(-1,retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
			
		    //byte[] byteArray = retriever.getEmbeddedPicture(); 
		    //saveAlbumPcture(id3, byteArray);;
			 retriever.release();
		} catch (Exception e) {
			//Log.e(TAG, "exception in scanID3:" + id3.file.getName());
			id3MediaFail=true;
			e.printStackTrace();
		} finally {
			if (retriever != null) {
				retriever.release();
				retriever = null;
			}
		}

		try {
			if(id3MediaFail || id3.picPath==null){
				String filePath = id3.file.getName();
				File file2 = new File(filePath);
				AudioFile audioFile = null;
				audioFile = (AudioFile) AudioFileIO.read(file2);
				Tag tag = audioFile.getTag();
				if(tag != null){
					if(id3.album==null){
						String album = tag.getFirst(FieldKey.ALBUM);
						//Log.i(TAG, "get album with jaduiotagger: "+album);
						id3.album =  new MediaObjectName(-1,album);
					}
					if(id3.artist==null){
						String artist = tag.getFirst(FieldKey.ARTIST);
						//Log.i(TAG, "get artist with jaduiotagger: "+artist);
						id3.artist =  new MediaObjectName(-1,artist);
					}
					if(id3.composer==null){
						String composer = tag.getFirst(FieldKey.COMPOSER);
						//Log.i(TAG, "get composer with jaduiotagger: "+composer);
						id3.composer =  new MediaObjectName(-1,composer);
					}
					if(id3.genre==null){
						String genre = tag.getFirst(FieldKey.GENRE);
						//Log.i(TAG, "get genre with jaduiotagger: "+genre);
						id3.genre =  new MediaObjectName(-1,genre);
					}
					if(id3.title==null){
						String title = tag.getFirst(FieldKey.TITLE);
						//Log.i(TAG, "get title with jaduiotagger: "+title);
						id3.title =  title;
					}
					if(id3.picPath==null){
						Artwork artwork = tag.getFirstArtwork();
						if (artwork != null) {
						    byte[] byteArray = artwork.getBinaryData(); 
						    //Log.i(TAG, "get album-picture with jaduiotagger: "+filePath);
						    saveAlbumPcture(id3, byteArray);
					    }
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if(id3.title==null||isAllSpaces(id3.title)){
			    int flagIndex = id3.file.getName().lastIndexOf("/")+1;
			    id3.title=id3.file.getName().substring(flagIndex);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(id3.album==null||isAllSpaces(id3.album.getName()))
			id3.album=new MediaObjectName(-1,null);
		if(id3.artist==null||isAllSpaces(id3.artist.getName()))
			id3.artist=new MediaObjectName(-1,null);
		if(id3.composer==null||isAllSpaces(id3.composer.getName()))
			id3.composer=new MediaObjectName(-1,null);
		if(id3.genre==null||isAllSpaces(id3.genre.getName()))
			id3.genre=new MediaObjectName(-1,null);
		if(id3.title==null)
			id3.title= MediaDB.NOT_EXIST_NAME_STRING;
		if(id3.duration ==null)
			id3.duration="0";
		if(id3.track_number ==null)
			id3.track_number="0";
		
		id3.album_py = PingYingTool.parseString(id3.album.getName());
		id3.artist_py = PingYingTool.parseString(id3.artist.getName());
		id3.composer_py = PingYingTool.parseString(id3.composer.getName());
		id3.genre_py = PingYingTool.parseString(id3.genre.getName());
		id3.title_py = PingYingTool.parseString(id3.title);
		return id3;
	}
	
	
	//String sqlQuery = "select file_info.id,partions.mount_path, path_name.name||file_info.name  as path_name " //
      //      + " from file_info,path_name,partions where file_info.partion_id=partions.id and file_info.path_id=path_name.id and file_info.file_exist=1 and " + 
	//		"file_info.media_type=" + t + " and " 
	//		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID ;
	private void parseThumbNail(){
		Log.i(TAG, "parseThumbNail start." );
		parseThumbNail(MEDIA_TYPE.IMAGE);
		parseThumbNail(MEDIA_TYPE.VIDEO);
		Log.i(TAG, "parseThumbNail end." );
	}
	
	private void parseThumbNail(MEDIA_TYPE type){
		Cursor cursor = mMediaScannerDBHelper.getFilesImageVideoThumbNotParsed(type);
		if (cursor == null) {
			Log.e(TAG, "getFilesImageVideoThumbNotParsed file cursor == null:" + mMountPath + " type:" + type.toString());
		}else{
			while(cursor.moveToNext()){
				int fileID = cursor.getInt(0);
				String mount_path = cursor.getString(1);
				String path_name = cursor.getString(2);
				String file_name = cursor.getString(3);
				waitWhenSleep();
				if (Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath)) {
					cursor.close();
					return;
				}
				
				String filePath = mount_path + path_name + file_name;
				Log.i(TAG, "getFilesImageVideoThumbNotParsed(MEDIA_TYPE.IMAGE):"  + " fileID:" + fileID + " filePath:" + filePath);
				String picFilePath = parseMediaThumbNail(filePath,type);
				mMediaScannerDBHelper.saveThumbNailPic(fileID, picFilePath);
			}
			cursor.close();
		}
	}
	
	private String parseMediaThumbNail(String mediaFilePath,MEDIA_TYPE type) {
		try {
			Bitmap bitmap = null;
			if(type == MEDIA_TYPE.IMAGE)
			    bitmap = getImageThumbnail(mediaFilePath, CommonUtil.THMUBNAIL_PIC_WIDTH, CommonUtil.THMUBNAIL_PIC_HEIGHT);
			if(type == MEDIA_TYPE.VIDEO)
			    bitmap = getVideoThumbnail(mediaFilePath, CommonUtil.THMUBNAIL_PIC_WIDTH, CommonUtil.THMUBNAIL_PIC_HEIGHT,MediaStore.Video.Thumbnails.MINI_KIND);
			
			if(bitmap==null){
				Log.e(TAG, "parseMediaThumbNail fail: bitmap==null:" + mediaFilePath);
				return null;
			}
				
			String fileName = CommonUtil.getThumbnailFilePath(mediaFilePath);
//			Log.i(TAG, "parseThumbNailPic: " + fileName);
			File bitmapFile = new File(fileName);

	        if(!bitmapFile.getParentFile().exists()) {  
	            if(!bitmapFile.getParentFile().mkdirs()) {  
	            	Log.e(TAG, "mkdirs fail: " + fileName);
	                return null;  
	            }  
	        }  
			if(!bitmapFile.exists())
				bitmapFile.createNewFile();
			
			FileOutputStream bitmapWtriter = null;

			bitmapWtriter = new FileOutputStream(bitmapFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, bitmapWtriter);
			return fileName;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	private Bitmap getImageThumbnail(String imagePath, int width, int height) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		// 获取这个图片的宽和高，注意此处的bitmap为null
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		options.inJustDecodeBounds = false; // 设为 false
		// 计算缩放比
		int h = options.outHeight;
		int w = options.outWidth;
		
		int beWidth = w / width;
		int beHeight = h / height;
		
		int be = 1;
		if (beWidth < beHeight) {
			be = beWidth;
		} else {
			be = beHeight;
		}
		if (be <= 0) {
			be = 1;
		}
		options.inSampleSize = be;
		
		try{
			// 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
			bitmap = BitmapFactory.decodeFile(imagePath, options);
			// 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
			bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
					ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		}catch(Throwable t){
			t.printStackTrace();
		}
		if(bitmap != null && (bitmap.getWidth() > width || bitmap.getHeight() > height)){
			
			int newWidth = Math.min(bitmap.getWidth(),width);
			int newHeight = Math.min(bitmap.getHeight(),height);
			
			Bitmap newBmp = null;
			Log.e(TAG, "[getImageThumbnail]:imagePath " + imagePath + " has get thumbnail failed,new width " + newWidth + " new height " + newHeight);
			newBmp = Bitmap.createBitmap(bitmap, 0, 0, newWidth, newHeight);
			bitmap.recycle();
			bitmap = newBmp;
		}
		return bitmap;
	}

	private Bitmap getVideoThumbnail(String videoPath, int width, int height,
			int kind) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = null;
		// 获取视频的缩略图
		try{
			retriever  = new MediaMetadataRetriever();
			retriever.setDataSource(videoPath);
			String timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			int time = Integer.valueOf(timeString != null?timeString:"0");
			bitmap = retriever.getFrameAtTime(time/2);
//			if(time > CAPTURE_POSITION){//单位是微秒
//				bitmap = retriever.getFrameAtTime(CAPTURE_POSITION, MediaMetadataRetriever.OPTION_NEXT_SYNC);
//			}else{
//				bitmap = retriever.getFrameAtTime();
//			}
		}catch (Throwable t) {
			t.printStackTrace();
		}finally{
			if(retriever != null)
				retriever.release();
			retriever = null;
		}
		
		if(bitmap != null){
			float scaleWidth = (float)width / bitmap.getWidth();
			float scaleHeight = (float)height / bitmap.getHeight();
			Matrix matrix = new Matrix();
			matrix.setScale(scaleWidth, scaleHeight);
			Bitmap bmp =Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix , true);
			bitmap.recycle();
			return bmp;
		}
		return bitmap;
	}

	private boolean isAllSpaces(String name){
		if(name == null)
		    return true;
		String tmp=new String(name);
		tmp.replaceAll(" ", "");
		if(tmp.length()==0)
			return true;
		return false;
	}
	
	//如果是扫描过的盘，先从DB中取出所有file记录，再与mAllPathList对比，得到新增的文件，删除的文件，最后一次性更新DB
	private void saveSeachedMediaFiles(String filePath) {	
		Log.e(TAG, "saveSeachedFiles file start:" + mMountPath);
		long lastModTime =0;
	    int batchUpdateExistFielNum = 200;
	    LinkedList<Integer> fileIDExistAudio = new LinkedList<Integer>();
	    LinkedList<Integer> fileIDExistVideo = new LinkedList<Integer>();
	    LinkedList<Integer> fileIDExistImage = new LinkedList<Integer>();
		Cursor cursor = mMediaScannerDBHelper.getFilesSaved();
		if (cursor == null) {
			saveNewSearchedMediaFile();
			Log.e(TAG, "saveSeachedFiles file cursor == null:" + mMountPath);
		}else{
			while(cursor.moveToNext()){
				int fileID = cursor.getInt(MediaDB.ID_INDEX);
				String fileName = cursor.getString(MediaDB.NAME_INDEX);
				String path = cursor.getString(MediaDB.PATH_INDEX);
				long modTime = cursor.getLong(MediaDB.MOD_TIME_INDEX);
				int type = cursor.getInt(MediaDB.TYPE_INDEX);
				MEDIA_TYPE mediaType=MEDIA_TYPE.values()[type];
				if (hasFileInSearchedPath(path, fileName, modTime,mediaType)) {
					if(mediaType==MEDIA_TYPE.AUDIO)
						fileIDExistAudio.addLast(fileID);
					else if(mediaType==MEDIA_TYPE.VIDEO)
						fileIDExistVideo.addLast(fileID);
					else if(mediaType==MEDIA_TYPE.IMAGE)
						fileIDExistImage.addLast(fileID);
					
					if(fileIDExistAudio.size()%batchUpdateExistFielNum==0){
						mMediaScannerDBHelper.upDateExistFile(fileIDExistAudio);
						fileIDExistAudio.clear();
					}else if(fileIDExistVideo.size()%batchUpdateExistFielNum==0){
						mMediaScannerDBHelper.upDateExistFile(fileIDExistVideo);
						fileIDExistVideo.clear();
					}if(fileIDExistImage.size()%batchUpdateExistFielNum==0){
						mMediaScannerDBHelper.upDateExistFile(fileIDExistImage);
						fileIDExistImage.clear();
					}
				}
				//if(modTime>lastModTime && mediaType==MEDIA_TYPE.AUDIO)
				//	lastModTime=modTime;
				
				if (Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath)) {
					cursor.close();
					mScanThread = null;
					return;
				}
			}
			cursor.close();
			mMediaScannerDBHelper.upDateExistFile(fileIDExistAudio);
			mMediaScannerDBHelper.upDateExistFile(fileIDExistVideo);
			mMediaScannerDBHelper.upDateExistFile(fileIDExistImage);
			saveNewSearchedMediaFile();
		}
		mMediaScannerDBHelper.upDateNewFile(mLastModified-MilSecondsNewTime);
		Log.e(TAG, "saveSeachedFiles file end:" + mMountPath);
		mediaDirInit();
		freeMapMemory();
		Log.e(TAG, "mediaDirInit end: " + mMountPath);
		//System.gc();
	}
	
	private void freeMapMemory(){
		LinkedList<AllFilesInOnePath> allFiles;
		allFiles = mAllPathListAudio;
		for(AllFilesInOnePath onePath: allFiles) {
			onePath.mFilesMap = null;
		}
		allFiles = mAllPathListVideo;
		for(AllFilesInOnePath onePath: allFiles) {
			onePath.mFilesMap = null;
		}
		allFiles = mAllPathListImage;
		for(AllFilesInOnePath onePath: allFiles) {
			onePath.mFilesMap = null;
		}
		mAllPathMapAudio = null;
		mAllPathMapVideo = null;
		mAllPathMapImage = null;
		System.gc();
	}
	private void saveNewSearchedMediaFile(){
		LinkedList<FileEntry>  allNewFiles = new LinkedList<FileEntry>();
		for (AllFilesInOnePath onePath:mAllPathListAudio) {
			if (Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath))
				return;
			if (onePath.mFilesList.size() > 0) {
				for (FileEntry fileEntry : onePath.mFilesList) {
					if (fileEntry.mIsNewSearched) {
						allNewFiles.addLast(fileEntry);
					}
				}
			}
		}
		mMediaScannerDBHelper.fileInsert(allNewFiles,MEDIA_TYPE.AUDIO);
		
		allNewFiles.clear();
		for (AllFilesInOnePath onePath:mAllPathListVideo) {
			if (Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath))
				return;
			if (onePath.mFilesList.size() > 0) {
				for (FileEntry fileEntry : onePath.mFilesList) {
					if (fileEntry.mIsNewSearched) {
						allNewFiles.addLast(fileEntry);
					}
				}
			}
		}
		mMediaScannerDBHelper.fileInsert(allNewFiles,MEDIA_TYPE.VIDEO);
		
		allNewFiles.clear();
		for (AllFilesInOnePath onePath:mAllPathListImage) {
			if (Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath))
				return;
			if (onePath.mFilesList.size() > 0) {
				for (FileEntry fileEntry : onePath.mFilesList) {
					if (fileEntry.mIsNewSearched) {
						allNewFiles.addLast(fileEntry);
					}
				}
			}
		}
		mMediaScannerDBHelper.fileInsert(allNewFiles,MEDIA_TYPE.IMAGE);
	}
	
	private boolean hasFileInSearchedPath(String path,String fileName,long modTime,MEDIA_TYPE mediaType){
		HashMap<String, AllFilesInOnePath>  allFilesMap;
		if(mediaType==MEDIA_TYPE.AUDIO)
			allFilesMap = mAllPathMapAudio;
		else if(mediaType==MEDIA_TYPE.VIDEO)
			allFilesMap = mAllPathMapVideo;
		else if(mediaType==MEDIA_TYPE.IMAGE)
			allFilesMap = mAllPathMapImage;
		else {
			Log.e(TAG, "hasFileInSearchedPath mediaType error ");
			return false;
		}
		
		String pathName = mMountPath + path ;
		pathName = pathName.substring(0,pathName.length()-1);
		AllFilesInOnePath onePath = allFilesMap.get(pathName);
		if(onePath == null){
			///Log.e(TAG, "new file,onePath == null: " + path +"/" + fileName+ ",modTime:"+ modTime + ",mediaType:" + mediaType.toString());
			return false;
		}
		if (onePath.mFilesMap!=null) {
			FileEntry fileEntry = onePath.mFilesMap.get(fileName);
			if(fileEntry == null){
				//Log.e(TAG, "new file,fileEntry == null: " + path +"/" + fileName+ ",modTime:"+ modTime + ",mediaType:" + mediaType.toString());
				return false;
			}
			if (modTime == fileEntry.mModTime) {
				fileEntry.mIsNewSearched = false;
				//Log.e(TAG, "old file: " + onePath.mPath +"/" + fileName);
				return true;
			}
		}
		//Log.e(TAG, "new file: " + path +"/" + fileName+ ",modTime:"+ modTime + ",mediaType:" + mediaType.toString());
		return false;
	}
	
////////////////////////////////////////////////////////////////////////////////////////
	private class MediaDirectory{
		public String mDirName;
		public LinkedList<FileEntry> mFiles;
		public LinkedList<MediaDirectory> mSubDirectories = null;
		public String[] mFilesStringForBrowser = null;
		public String[] mSubDirectoriesStringsForBrowser = null;
		public String mDirNamePY;
		public MediaDirectory(String name){
			mDirName = name;
			mFiles = null;
			mSubDirectories = new LinkedList<MediaDirectory>();
			mDirNamePY = PingYingTool.parseString(name);
		}
	}
	
	private static Comparator<MediaDirectory> mComparatorMediaDirectory= new Comparator<MediaDirectory>() {
		@Override
		public int compare(MediaDirectory lData, MediaDirectory rData) {
			String lName = lData.mDirNamePY;
			String rName = rData.mDirNamePY;
			return CommonUtil.compareStr(lName,rName);
		}
	};

	private void mediaDirInit(){
		createAllMediaDirectories(mRootMediaDirectoryAudio,mAllPathListAudio);
		createAllMediaDirectories(mRootMediaDirectoryVideo,mAllPathListVideo);
		createAllMediaDirectories(mRootMediaDirectoryImage,mAllPathListImage);
		sortMediaDirectory(mRootMediaDirectoryAudio);
		sortMediaDirectory(mRootMediaDirectoryVideo);
		sortMediaDirectory(mRootMediaDirectoryImage);
		Log.e(TAG, "audio files..." );
		//testDump(mRootMediaDirectoryAudio);
		Log.e(TAG, "video files..." );
		//testDump(mRootMediaDirectoryVideo);
		Log.e(TAG, "image files..." );
		//testDump(mRootMediaDirectoryImage);
	}
	
	private void createAllMediaDirectories(MediaDirectory rootMediaDirectory,
			 LinkedList<AllFilesInOnePath> allPathList){
		
		for (AllFilesInOnePath entry:allPathList) {
			MediaDirectory mediaDir = getOneMediaDirectory(rootMediaDirectory,entry.mPath);
			mediaDir.mFiles = entry.mFilesList;
		}
	}
	
	private MediaDirectory getOneMediaDirectory(MediaDirectory rootMediaDirectory,String path){
		if(rootMediaDirectory.mDirName.equals(path))
			return rootMediaDirectory;
		String noRoot = path.substring(rootMediaDirectory.mDirName.length()+1);
		if(noRoot == null)
			return rootMediaDirectory;
		String dirs[] = noRoot.split("/");
		MediaDirectory mediaDir = rootMediaDirectory;
		for(String dir:dirs){
			mediaDir = createOneMediaDirectory(mediaDir,dir);
		}
		return mediaDir;
	}
	
	private MediaDirectory createOneMediaDirectory(MediaDirectory mediaDir,String dir){
		for(MediaDirectory one:mediaDir.mSubDirectories){
			if(one.mDirName.equals(dir))
				return one;
		}
		MediaDirectory one = new MediaDirectory(dir);
		mediaDir.mSubDirectories.addLast(one);
		return one;
	}
	
	private void sortMediaDirectory(MediaDirectory mediaDirectory){
        if(mediaDirectory.mSubDirectories.size()>1)
        	Collections.sort(mediaDirectory.mSubDirectories, mComparatorMediaDirectory);
		for(MediaDirectory dir:mediaDirectory.mSubDirectories){
			sortMediaDirectory(dir);
		}
	}
	
	private void testDump(MediaDirectory mediaDirectory){
		Log.i(TAG, "testDump dir:" + mediaDirectory.mDirName);
		if(mediaDirectory.mFiles!=null){
			for(FileEntry file:mediaDirectory.mFiles){
				Log.i(TAG, "testDump file:" + file.mName);
			}
		}
		
		for(MediaDirectory dir:mediaDirectory.mSubDirectories){
			testDump(dir);
		}
	}
	
////////////////////////////////////////////////////////////////////////////////////////
	
	private  void searchAndSortDir(final File directory, LinkedList<File> fastFilesSubFolderList) {
		if (!directory.isDirectory()) {
			return ;
		}
		File[] files = directory.listFiles();
		if (files == null || files.length <= 0) {
			return;
		}
		String curPath = directory.getAbsolutePath();
		//Log.e(TAG, "searchAndSortDir : " + curPath);
		HashMap<String, FileEntry> filesOnePathMapAudio = null;  
		HashMap<String, FileEntry> filesOnePathMapVideo = null;  
		HashMap<String, FileEntry> filesOnePathMapImage = null;  
		LinkedList<FileEntry> filesOnePathListAudio = null;
		LinkedList<FileEntry> filesOnePathListVideo = null;
		LinkedList<FileEntry> filesOnePathListImage = null;
		int tmpPreAllNumAudio = mMediaPathsList.get(MEDIA_TYPE.AUDIO).mFilesNum;
		int tmpPreAllNumVideo = mMediaPathsList.get(MEDIA_TYPE.VIDEO).mFilesNum;
		int tmpPreAllNumImage = mMediaPathsList.get(MEDIA_TYPE.IMAGE).mFilesNum;
        int tmpNumInPathAudio = 0;
        int tmpNumInPathVideo = 0;
        int tmpNumInPathImage = 0;
        
        int len = files.length;
		for (int i = 0 ;i < len;i++) {
			waitWhenSleep();
			if(Thread.interrupted() ||!CommonUtil.isDeviceExsit(mMountPath)){
				Log.e(TAG, "searchAndSortDir DEVID_NULL unmounted ");
				return;
			}
			
			File file = files[i];
			String fileName = file.getName();
			if (file.isDirectory()) {
				fastFilesSubFolderList.addLast(file);
			} else if (file.isFile()) {
				MEDIA_TYPE fileType = CommonUtil.getMediaType(fileName);
				FileEntry fileOneEntry =null;
				if(fileType != MEDIA_TYPE.INVALID){
					fileOneEntry = new FileEntry(curPath,fileName,file.lastModified());
				}
				if(fileType==MEDIA_TYPE.AUDIO){
					if(filesOnePathListAudio ==null){
						filesOnePathListAudio = new LinkedList<FileEntry>();
						filesOnePathMapAudio = new HashMap<String, FileEntry>();
					}
					if(file.lastModified()>mLastModified)
						mLastModified=file.lastModified();
					filesOnePathMapAudio.put(fileName, fileOneEntry);
					filesOnePathListAudio.addLast(fileOneEntry);
					mMediaPathsList.get(MEDIA_TYPE.AUDIO).mFilesNum ++;
					tmpNumInPathAudio++;
//					Log.e(TAG, "findAudio  : " + curPath + "/" +fileName);
				}else if(fileType==MEDIA_TYPE.VIDEO){
					if(filesOnePathListVideo ==null){
						filesOnePathListVideo  = new LinkedList<FileEntry>();
						filesOnePathMapVideo = new HashMap<String, FileEntry>();
					}
					filesOnePathMapVideo.put(fileName, fileOneEntry);
					filesOnePathListVideo.addLast(fileOneEntry);
					mMediaPathsList.get(MEDIA_TYPE.VIDEO).mFilesNum ++;
					tmpNumInPathVideo ++;
				}else if(fileType==MEDIA_TYPE.IMAGE){
					if(filesOnePathListImage==null){
						filesOnePathListImage  = new LinkedList<FileEntry>();
						filesOnePathMapImage = new HashMap<String, FileEntry>();
					}
					filesOnePathMapImage.put(fileName, fileOneEntry);
					filesOnePathListImage.addLast(fileOneEntry);
					mMediaPathsList.get(MEDIA_TYPE.IMAGE).mFilesNum ++;
					tmpNumInPathImage ++;
				}
				//if(mAudioFilesNum%100 == 0)
				//    Log.e(TAG, "getPathHasAudio  : " + mAudioFilesNum);
			}
		}

		if (fastFilesSubFolderList.size() > 0) {
			Collections.sort(fastFilesSubFolderList, com.adayo.mediaScanner.CommonUtil.mComparator);
		}
		if (filesOnePathListAudio!=null) {
			Collections.sort(filesOnePathListAudio, mComparatorFileEntry);
			AllFilesInOnePath one = new AllFilesInOnePath(curPath,tmpPreAllNumAudio,tmpNumInPathAudio);
	        one.mFilesList = filesOnePathListAudio;
	        one.mFilesMap = filesOnePathMapAudio;
			mAllPathListAudio.addLast(one);
			mAllPathMapAudio.put(curPath, one);
		}
		if (filesOnePathListVideo!=null) {
			Collections.sort(filesOnePathListVideo, mComparatorFileEntry);
			AllFilesInOnePath one = new AllFilesInOnePath(curPath,tmpPreAllNumVideo,tmpNumInPathVideo);
	        one.mFilesList = filesOnePathListVideo;
	        one.mFilesMap = filesOnePathMapVideo;
			mAllPathListVideo.addLast(one);
			mAllPathMapVideo.put(curPath, one);
		}
		if (filesOnePathListImage!=null) {
			Collections.sort(filesOnePathListImage, mComparatorFileEntry);
			AllFilesInOnePath one = new AllFilesInOnePath(curPath,tmpPreAllNumImage,tmpNumInPathImage);
	        one.mFilesList = filesOnePathListImage;
	        one.mFilesMap = filesOnePathMapImage;
			mAllPathListImage.addLast(one);
			mAllPathMapImage.put(curPath, one);
		}
		
		return;
	}

    private  boolean searchPaths(final String mountPath){
		File startPath = new File(mountPath);
		if (!startPath.isDirectory()) {
			Log.w(TAG, "searchPaths(String):" + mountPath + " is not a directory!");
			return false;
		}
		LinkedList<File> fastFilesSubFolderList = new LinkedList<File>();
		LinkedList<File> dirList = new LinkedList<File>();
		dirList.addFirst(startPath);
		Log.e(TAG, "searchPaths scan file start:" + mountPath);
		while (true) {
			if (dirList.size() == 0 )
				break;
			File dir = dirList.getFirst();
			dirList.removeFirst();
			
			searchAndSortDir(dir,fastFilesSubFolderList);
		
		    if(fastFilesSubFolderList.size()>0){
		    	dirList.addAll(fastFilesSubFolderList);
		    	fastFilesSubFolderList.clear();
		    }
		}
		
		Log.e(TAG, "searchPaths end:" + mountPath + ",audioFiles: " + mMediaPathsList.get(MEDIA_TYPE.AUDIO).mFilesNum
				  + ",videoFiles: " + mMediaPathsList.get(MEDIA_TYPE.VIDEO).mFilesNum
				  + ",imageFiles: " + mMediaPathsList.get(MEDIA_TYPE.IMAGE).mFilesNum);
	    //testDumpAllFiles();
	    return true;
	}
	
    private void testDumpAllFiles(){
		for (Iterator<AllFilesInOnePath> iter = mAllPathListAudio.iterator(); iter.hasNext();) {
			AllFilesInOnePath onePath = (AllFilesInOnePath) (iter.next());
			Log.e(TAG, "dump audio path:" + onePath.mPath);
		    if(onePath.mFilesList.size()>0){
		    	for (FileEntry name : onePath.mFilesList) {
		    		Log.e(TAG, "dump audio file:" + name.mName);
		    	}
		    }
		}
		
		for (Iterator<AllFilesInOnePath> iter = mAllPathListVideo.iterator(); iter.hasNext();) {
			AllFilesInOnePath onePath = (AllFilesInOnePath) (iter.next());
			Log.e(TAG, "dump video path:" + onePath.mPath);
		    if(onePath.mFilesList.size()>0){
		    	for (FileEntry name : onePath.mFilesList) {
		    		Log.e(TAG, "dump video file:" + name.mName);
		    	}
		    }
		}
		
		for (Iterator<AllFilesInOnePath> iter = mAllPathListImage.iterator(); iter.hasNext();) {
			AllFilesInOnePath onePath = (AllFilesInOnePath) (iter.next());
			Log.e(TAG, "dump image path:" + onePath.mPath);
		    if(onePath.mFilesList.size()>0){
		    	for (FileEntry name : onePath.mFilesList) {
		    		Log.e(TAG, "dump image file:" + name.mName);
		    	}
		    }
		}
    }

		public MediaObjectList getFiles() {
			return mMediaScannerDBHelper.getFiles();
		}

		public ID3Info getFileID3(MediaObjectName file) {
			return mMediaScannerDBHelper.getFileID3(file);
		
		}

		public MediaObjectList getAllPaths() {
			return mMediaScannerDBHelper.getAllPaths();
		}

		public MediaObjectList getFilesForPath(MediaObjectName path) {
			return mMediaScannerDBHelper.getFilesForPath(path);
		}

		public MediaObjectList getAllArtist() {
			return mMediaScannerDBHelper.getAllArtist();
		}

		public MediaObjectList getFilesForArtist(MediaObjectName artist) {
			return mMediaScannerDBHelper.getFilesForArtist(artist);
		}

		public MediaObjectList getAllAlbum() {
			return mMediaScannerDBHelper.getAllAlbum();
		}

		public MediaObjectList getFilesForAlbum(MediaObjectName album) {
			return mMediaScannerDBHelper.getFilesForAlbum(album);
		}

		public MediaObjectList getAllGenre() {
			return mMediaScannerDBHelper.getAllGenre();
		}

		public MediaObjectList getFilesForGenre(MediaObjectName genre) {
			return mMediaScannerDBHelper.getFilesForGenre(genre);
		}

		public MediaObjectList getAllComposer() {
			return mMediaScannerDBHelper.getAllComposer();
		}

		public MediaObjectList getFilesForComposer(MediaObjectName composer) {
			return mMediaScannerDBHelper.getFilesForComposer(composer);
		}
		
		public boolean setFavorite(MediaObjectName file,boolean flag) {
			return mMediaScannerDBHelper.setFavorite(file, flag);
		}

		public MediaObjectList getAllFavorite() {
			return mMediaScannerDBHelper.getAllFavorite();
		}
	
}
