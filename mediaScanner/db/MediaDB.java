/**
 *  Foryou Project - IPod
 *
 *  Copyright (C) Foryou 2013 - 2015
 *
 *  File:IPodDbHelper.java
 *
 *  Revision:
 *  
 *  2013-7-20
 *		- first revision
 *  
 */
package com.adayo.mediaScanner.db;

import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import android.R.integer;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.RemoteException;
import android.util.Log;

import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.ID3Info;
import com.adayo.mediaScanner.MediaObjectName;
import com.adayo.mediaScanner.db.MediaObjectList.ObjectType;
import com.adayo.mediaScanner.fileScanner.FileScanner.FileEntry;
import com.adayo.mediaScanner.fileScanner.MediaScanner;
import com.adayo.mediaScanner.fileScanner.PingYingTool;
import com.adayo.midwareproxy.utils.AdayoLog;


/**

public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;  
public static final int METADATA_KEY_ALBUM           = 1;  专辑
public static final int METADATA_KEY_ARTIST          = 2;  艺术家 
public static final int METADATA_KEY_AUTHOR          = 3;  
public static final int METADATA_KEY_COMPOSER        = 4;  作曲家
public static final int METADATA_KEY_DATE            = 5;  
public static final int METADATA_KEY_GENRE           = 6;  流派
public static final int METADATA_KEY_TITLE           = 7;  歌曲
public static final int METADATA_KEY_YEAR            = 8;  
public static final int METADATA_KEY_DURATION        = 9;  长度
public static final int METADATA_KEY_NUM_TRACKS      = 10;  
public static final int METADATA_KEY_IS_DRM_CRIPPLED = 11;  
public static final int METADATA_KEY_CODEC           = 12;  
public static final int METADATA_KEY_RATING          = 13;  
public static final int METADATA_KEY_COMMENT         = 14;  
public static final int METADATA_KEY_COPYRIGHT       = 15;  
public static final int METADATA_KEY_BIT_RATE        = 16;  
public static final int METADATA_KEY_FRAME_RATE      = 17;  
public static final int METADATA_KEY_VIDEO_FORMAT    = 18;  
public static final int METADATA_KEY_VIDEO_HEIGHT    = 19;  
public static final int METADATA_KEY_VIDEO_WIDTH     = 20;  
public static final int METADATA_KEY_WRITER          = 21;

 */
public class MediaDB extends SQLiteOpenHelper {
	public static final String NOT_EXIST_NAME_STRING = "Unknown";
	private static final String TAG = "MediaDB";
	private static final String DB_NAME = "mediaDB";
	
	private static final int VERSION = 1;
	private static SQLiteDatabase mDBRead = null;
	private static SQLiteDatabase mDBWrite = null;
	private String mMountPath;
	private int mPartionID = 0;
	private MediaScannerDBHelper mDBHelper = null;
	
	private static HashMap<STORAGE_PORT, LinkedList<Integer>> mPartionsMap = new HashMap<STORAGE_PORT, LinkedList<Integer>>();
	
	private static final String C_ID = "id";
	private static final String C_NAME = "name";
	private static final String C_NAME_PY = "name_py";
	public static int ID_INDEX = 0;
	public static int NAME_INDEX = 2;
	public static int MOD_TIME_INDEX = 4;
	public static int TYPE_INDEX = 13;
	public static int PATH_INDEX = 14;
	private static final String TABLE_FILE_INFO = "file_info";
	private static final String C_FILE_INFO_ID = "id";
	private static final String C_FILE_INFO_PATH_ID = "path_id";
	private static final String C_FILE_INFO_NAME = "name";
	private static final String C_FILE_INFO_NAME_PY = "name_py";
	private static final String C_FILE_INFO_MOD_TIME = "mod_time";
	private static final String C_FILE_INFO_PLAY_TIMES = "play_times";
	private static final String C_FILE_INFO_DELETE = "hide";
	private static final String C_FILE_INFO_TOP= "top";
	private static final String C_FILE_INFO_FAVORITE = "favorite";
	private static final String C_FILE_INFO_NEW = "new";
	private static final String C_FILE_INFO_ID3_EXIST = "id3_exist";
	private static final String C_FILE_INFO_FILE_EXIST = "file_exist";
	private static final String C_FILE_INFO_PARTION_ID = "partion_id";
	private static final String C_FILE_INFO_MEDIA_TYPE = "media_type"; //0:audio,1:video,2:image
	
	private static final String SQL_CREATE_TABLE_FILE_INFO = "CREATE TABLE " + TABLE_FILE_INFO + 
			" (" + C_FILE_INFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			       C_FILE_INFO_PATH_ID + " INTEGER," + 
			       C_FILE_INFO_NAME + " TEXT," + 
			       C_FILE_INFO_NAME_PY + " TEXT," + 
			       C_FILE_INFO_MOD_TIME + " INTEGER," + 
			       C_FILE_INFO_PLAY_TIMES + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_DELETE + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_TOP + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_FAVORITE + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_NEW      + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_ID3_EXIST + " INTEGER DEFAULT 0," + 
			       C_FILE_INFO_FILE_EXIST + " INTEGER DEFAULT 1,"  +
			       C_FILE_INFO_PARTION_ID + " INTEGER,"  +
			       C_FILE_INFO_MEDIA_TYPE + " INTEGER DEFAULT 0" +  //0:audio,1:video,2:image
			       " )";
	
	private static final String TABLE_PARTIONS= "partions"; 
	private static final String C_PARTIONS_MOUNT_PATH = "mount_path";
	private static final String SQL_CREATE_TABLE_PARTION = "CREATE TABLE " + TABLE_PARTIONS + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			     C_NAME + " TEXT," + 
			     C_PARTIONS_MOUNT_PATH + " TEXT" + 
			   " )";
	
	private static final String TABLE_PATH = "path_name";
	private static final String SQL_CREATE_TABLE_PATH = "CREATE TABLE " + TABLE_PATH + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			       C_NAME + " TEXT" +  " )";
	
	private static final String TABLE_ID3_INFO = "id3_info";
	private static final String C_ID3_INFO_ID = "id";
	private static final String C_ID3_INFO_FILE_ID = "file_id";
	private static final String C_ID3_INFO_TITLE = "title";
	private static final String C_ID3_INFO_TITLE_PY = "title_py";
	private static final String C_ID3_INFO_ARTIST_ID = "artist_id";
	private static final String C_ID3_INFO_ALBUM_ID = "album_id";
	private static final String C_ID3_INFO_COMPOSER_ID = "composer_id";	
	private static final String C_ID3_INFO_GENRE_ID = "genre_id";		
	private static final String C_ID3_INFO_DURATION = "duration";	
	private static final String C_ID3_INFO_CD_TRACK_NUMBER = "track_number";
	private static final String C_ID3_INFO_PIC = "pic_name";
	private static final String SQL_CREATE_TABLE_ID3_INFO = "CREATE TABLE " + TABLE_ID3_INFO + 
			" (" + C_ID3_INFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			       C_ID3_INFO_FILE_ID  + " INTEGER," + 
			       C_ID3_INFO_TITLE  + " TEXT," + 
			       C_ID3_INFO_TITLE_PY  + " TEXT," + 
			       C_ID3_INFO_ARTIST_ID + " INTEGER," + 
			       C_ID3_INFO_ALBUM_ID    + " INTEGER," + 
			       C_ID3_INFO_COMPOSER_ID + " INTEGER," + 
			       C_ID3_INFO_GENRE_ID + " INTEGER," + 
			       C_ID3_INFO_DURATION + " TEXT," + 
			       C_ID3_INFO_CD_TRACK_NUMBER + " INTEGER," + 
			       C_ID3_INFO_PIC      + " TEXT" + " )";
	
	private static final String TABLE_ARTIST = "artist_name";
	private static final String SQL_CREATE_TABLE_ARTIST = "CREATE TABLE " + TABLE_ARTIST + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			       C_NAME + " TEXT," +
			       C_NAME_PY + " TEXT"  +  " )";
	
	private static final String TABLE_ALBUM = "album_name";
	private static final String SQL_CREATE_TABLE_ALBUM  = "CREATE TABLE " + TABLE_ALBUM + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
		       C_NAME + " TEXT," +
		       C_NAME_PY + " TEXT"  +  " )";
	
	private static final String TABLE_COMPOSER = "composer_name";
	private static final String SQL_CREATE_TABLE_COMPOSER = "CREATE TABLE " + TABLE_COMPOSER + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
		       C_NAME + " TEXT," +
		       C_NAME_PY + " TEXT"  +  " )";
	
	private static final String TABLE_GENRE = "genre_name";
	private static final String SQL_CREATE_TABLE_GENRE = "CREATE TABLE " + TABLE_GENRE + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
		       C_NAME + " TEXT," +
		       C_NAME_PY + " TEXT"  +  " )";
	
	private static final String TABLE_THUMBNAIL = "thumbnail";
	private static final String C_THUMBNAIL_FILE_ID = "file_id";
	private static final String C_THUMBNAIL_IMG_PATH = "thumb_path";
	private static final String C_THUMBNAIL_IMG_PATH_PY = "thumb_path_py";
	private static final String SQL_CREATE_TABLE_THUMBNAIL = "CREATE TABLE " + TABLE_THUMBNAIL + 
			" (" + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			           C_THUMBNAIL_FILE_ID + " INTEGER," +
			           C_THUMBNAIL_IMG_PATH + " TEXT" +
			           //C_THUMBNAIL_IMG_PATH_PY + " TEXT"  +  
			           " )";
	
	private  final String SQL_CREATE_VIEW_ID3INFO = "CREATE VIEW view_id3info as select id3_info.id as _id,id3_info.file_id, id3_info.artist_id, id3_info.album_id, "
			+ "id3_info.genre_id, id3_info.composer_id, id3_info.title, id3_info.title_py,id3_info.duration, id3_info.track_number, "
			+ "artist_name.name as artist_name,album_name.name as album_name, "
			+ "genre_name.name as genre_name,composer_name.name as composer_name, " 
			+ "artist_name.name_py as artist_name_py,album_name.name_py as album_name_py, "
			+ "genre_name.name_py as genre_name_py,composer_name.name_py as composer_name_py, " 
			+"id3_info.pic_name, file_info.*"
			+ " from id3_info left join artist_name on  id3_info.artist_id = artist_name.id "                                        
			+ " left join album_name on id3_info.album_id = album_name.id "
			+ " left join genre_name on id3_info.genre_id = genre_name.id "
			+ " left join composer_name on id3_info.composer_id = composer_name.id" +
			", file_info where file_info.id=id3_info.file_id and file_info.file_exist=1 and file_info.media_type=0" ; 
	
//	private static final String SQL_VIEW_AUDIO = "create view audio as select file_info .*,path_name.name|| file_info.name  as path_name " //
//			+ " from file_info left join path_name on file_info.path_id = path_name.id "
//			+ " and file_info.file_exist = 1";
	//用left join 时在扫描过程中查询，path_name返回加null,改成如下where条件查询
	private  final String SQL_VIEW_AUDIO = "create view audio as select file_info.id as _id,file_info.*,path_name.name as path, partions.mount_path||path_name.name||file_info.name  as path_name " //
	+ " from file_info,path_name,partions where file_info.partion_id=partions.id and file_info.path_id=path_name.id and file_info.file_exist=1 and file_info.media_type=0 order by lower(trim(name_py))";

	private  final String SQL_VIEW_VIDEO = "create view video as select file_info.id ,file_info.name,file_info.name_py,file_info.partion_id,partions.mount_path||path_name.name||file_info.name as path_name  " //
			+ " from file_info,path_name,partions where file_info.partion_id=partions.id and file_info.path_id=path_name.id and file_info.file_exist=1 and file_info.media_type=1";

	private  final String SQL_VIEW_IMAGE = "create view image as select file_info.id,file_info.name,file_info.name_py,file_info.partion_id, partions.mount_path||path_name.name||file_info.name as path_name " //
			+ " from file_info,path_name,partions where file_info.partion_id=partions.id and file_info.path_id=path_name.id and file_info.file_exist=1 and file_info.media_type=2";

	public static String getForPathSql(String devPath){
		return "select path_id,'"+devPath+"'||path_name path_name "
						+ "from audio where audio.id =?";
	}

	@SuppressWarnings("unused")
	private Context mContext;

  
   // public MediaDB(Context context,String name,int version){  
   // 	super(context,name,null,version);  
   // }  
    
	public MediaDB(Context context, String path, MediaScannerDBHelper dbHelper ){
		super(context,DB_NAME,null,VERSION); 
		mMountPath = path;
		mDBHelper = dbHelper;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_TABLE_FILE_INFO);
		db.execSQL(SQL_CREATE_TABLE_PATH);
		db.execSQL(SQL_CREATE_TABLE_ID3_INFO);
		db.execSQL(SQL_CREATE_TABLE_ARTIST);
		db.execSQL(SQL_CREATE_TABLE_ALBUM);
		db.execSQL(SQL_CREATE_TABLE_COMPOSER);
		db.execSQL(SQL_CREATE_TABLE_GENRE);
		db.execSQL(SQL_CREATE_TABLE_PARTION);
		db.execSQL(SQL_CREATE_VIEW_ID3INFO);
		db.execSQL(SQL_CREATE_TABLE_THUMBNAIL);
		db.execSQL(SQL_VIEW_AUDIO);
		db.execSQL(SQL_VIEW_VIDEO);
		db.execSQL(SQL_VIEW_IMAGE);
		
		db.execSQL("insert into " + TABLE_ARTIST   +" values(-1," + "'"+ NOT_EXIST_NAME_STRING + "'" + ",'"+ NOT_EXIST_NAME_STRING + "'" + ")");
		db.execSQL("insert into " + TABLE_ALBUM    +" values(-1," + "'"+ NOT_EXIST_NAME_STRING + "'" + ",'"+ NOT_EXIST_NAME_STRING + "'" + ")");
		db.execSQL("insert into " + TABLE_COMPOSER +" values(-1," + "'"+ NOT_EXIST_NAME_STRING + "'" + ",'"+ NOT_EXIST_NAME_STRING + "'" + ")");
		db.execSQL("insert into " + TABLE_GENRE    +" values(-1," + "'"+ NOT_EXIST_NAME_STRING + "'" + ",'"+ NOT_EXIST_NAME_STRING + "'" + ")");
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
	}
	
	public void init(){
		synchronized (TAG) {
			String devID = CommonUtil.getDevID(mMountPath);
			if(mDBWrite==null)
			    mDBWrite = getWritableDatabase();
			if(mDBRead==null)
			    mDBRead = getReadableDatabase();
			mPartionID = getIDForPartion(devID,mMountPath);
			Log.i(TAG, "db init mPartionID: " + mPartionID + ",path:" + mMountPath);
			STORAGE_PORT storage = CommonUtil.getStoragePort(mMountPath);
			LinkedList<Integer> ids = mPartionsMap.get(storage);
			if(ids == null){
				ids = new LinkedList<Integer>();
			}
			ids.addLast(mPartionID);
			mPartionsMap.put(storage, ids);
		}
	}
	
	public void closetDB(){
		//mDBRead.close();
		//mDBWrite.close();
		
		synchronized (TAG) {
			STORAGE_PORT storage = CommonUtil.getStoragePort(mMountPath);
			LinkedList<Integer> ids = mPartionsMap.get(storage);
			if(ids != null){
				ids.clear();
			}
			mPartionsMap.remove(storage);
		}
	}
	
	public static void closeWriteDB(){
		if(mDBWrite!=null)
		   mDBWrite.close();
		mDBWrite=null;
	}
	
	public static LinkedList<Integer> getPartionsID(STORAGE_PORT storage){
		if(storage == STORAGE_PORT.STORAGE_ALL){
			LinkedList<Integer> list = new LinkedList<Integer>();
			
			Iterator<LinkedList<Integer>> iter = mPartionsMap.values().iterator();
			while (iter.hasNext()) {
				LinkedList<Integer> val = (LinkedList<Integer>) iter.next();
				for(Integer I:val)
				   list.add(I);
			} 
			return list;
		} else {
			return mPartionsMap.get(storage);
		}
	}
	
	private synchronized Cursor execQuery(String sql){
		try {
			return mDBRead.rawQuery(sql, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void execSQL(String sql) {
		// Log.i(TAG, "execSQL: " + sql);
        synchronized(mDBWrite){
    		try {
    			mDBWrite.execSQL(sql);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
		}
	}
	
	private int getIDForPartion(String devID,String mountPath){
		int id;
		if(null == devID)
			return -1;
		
		devID = sqliteEscape(devID);
		String sqlQuery = "select " + TABLE_PARTIONS+ "." + C_ID + " from " + TABLE_PARTIONS + " where " 
		+ TABLE_PARTIONS + "." + C_NAME + "="  + "'" +devID + "'";
	    Cursor cursor = execQuery(sqlQuery);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			id = cursor.getInt(0);
			
			 String sqlUpdateFile = "update " + TABLE_PARTIONS + " set "+  C_PARTIONS_MOUNT_PATH + "=" + "'" + mountPath + "'"
				       + " where "  + C_ID + "=" + id;;
			Log.i(TAG, "getIDForPartion: " + sqlUpdateFile);
			execSQL(sqlUpdateFile);
		}else {
			cursor.close();
			String sqlInsertPath = "insert into " + TABLE_PARTIONS + "(" + C_NAME +","+C_PARTIONS_MOUNT_PATH+ ")" 
			+ " values(" + "'" + devID + "'" + ",'" + mountPath + "'" +")" ;
			execSQL(sqlInsertPath);
			cursor = execQuery(sqlQuery);
			cursor.moveToFirst();
			id = cursor.getInt(0);
		}
		cursor.close();
		return id;
	}
	
	private int getIDForName(String table,String name,String name_py){
		int id;
		if(null == name)
			return -1;
		
		name = sqliteEscape(name);
		name_py = sqliteEscape(name_py);
		String sqlQuery = "select " + table+ "." + C_ID + " from " + table + " where " 
		+ table + "." + C_NAME + "="  + "'" +name + "'";
	    Cursor cursor = execQuery(sqlQuery);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			id = cursor.getInt(0);
		}else {
			cursor.close();
			String sqlInsertPath = "insert into " + table + "(" + C_NAME + ","+C_NAME_PY+ ")" +
			                    " values(" + "'" + name + "'" + ",'" + name_py + "'"+")" ;
			execSQL(sqlInsertPath);
			cursor = execQuery(sqlQuery);
			cursor.moveToFirst();
			id = cursor.getInt(0);
		}
		cursor.close();
		return id;
	}
	
	private int getIDForName(String table,String name){
		int id;
		if(null == name)
			return -1;
		
		name = sqliteEscape(name);
		String sqlQuery = "select " + table+ "." + C_ID + " from " + table + " where " 
		+ table + "." + C_NAME + "="  + "'" +name + "'";
	    Cursor cursor = execQuery(sqlQuery);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			id = cursor.getInt(0);
		}else {
			cursor.close();
			String sqlInsertPath = "insert into " + table + "(" + C_NAME + ")" + " values(" + "'" + name + "'" +")" ;
			execSQL(sqlInsertPath);
			cursor = execQuery(sqlQuery);
			cursor.moveToFirst();
			id = cursor.getInt(0);
		}
		cursor.close();
		return id;
	}
	
	private String getNameForID(String table,int id){
		if(-1==id)
			return null;
		String name;
		String sqlQuery = "select " + table+ "." + C_NAME + " from " + table + " where " 
		+ table + "." + C_ID + "="  +id ;
	    Cursor cursor = execQuery(sqlQuery);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			name = cursor.getString(0);
		}else {
			name = " ";
		}
		cursor.close();
		return name;
	}
	
	private String getFileName(String fullFileName){
		return fullFileName.substring(fullFileName.lastIndexOf("/")+1);
	}
	private String getPathName(String fullFileName){
		return fullFileName.substring(0, fullFileName.lastIndexOf("/")+1);
	}
	private int mPathIDCachedSet = 0xffffffff;
	private String mPathNameCachedSet = " ";
	private void setFileInfo(String fullFileName,String colName,int flag){
	    String fileName = getFileName(fullFileName);
	    String path = getPathName(fullFileName);

		fileName = sqliteEscape(fileName);
		int pathID = 0;
		if(!mPathNameCachedSet.equals(path)){
			mPathIDCachedSet = getIDForName(TABLE_PATH, path);
			mPathNameCachedSet = path;
		}
		pathID = mPathIDCachedSet;
		String sqlUpdateFile;
		if(C_FILE_INFO_PLAY_TIMES.equals(colName)){
			 sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  colName + "=" + colName + "+1"
				       + " where "  + C_FILE_INFO_PATH_ID + "=" + pathID
				       +" and " +  C_FILE_INFO_NAME + "=" + "'"+fileName+ "'"
			           + " and " + C_FILE_INFO_PARTION_ID + "=" + mPartionID;
		}else{
			 sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  colName + "=" + flag
			       + " where "  + C_FILE_INFO_PATH_ID + "=" + pathID
			       +" and " +  C_FILE_INFO_NAME + "="  + "'" +fileName+ "'"
		           + " and " + C_FILE_INFO_PARTION_ID + "=" + mPartionID;
		}
		Log.i(TAG, "setFileInfo: " + sqlUpdateFile);
		execSQL(sqlUpdateFile);
	}
	
	private int getTopMax(){
		String sqlTopMax;
		int max;
		sqlTopMax = "select max(" + C_FILE_INFO_TOP + ") from "+ TABLE_FILE_INFO;
		Cursor cursor = execQuery(sqlTopMax);
		if (cursor.getCount() > 0) {
		    cursor.moveToFirst();
		    max = cursor.getInt(0);
		} else {
			Log.e(TAG, "getTopMax count ===0.");
			max = 0;
		}
		cursor.close();
		return max;
	}
	private int mTopMaxCache = 0;
	public void setTop(String fullFileName,int flag){
		if(flag==1){
			if(mTopMaxCache==0)
				mTopMaxCache = getTopMax();
			mTopMaxCache++;
			setFileInfo(fullFileName,C_FILE_INFO_TOP,mTopMaxCache);
		}else
			setFileInfo(fullFileName,C_FILE_INFO_TOP,flag);
	}
	
	public void setFavorite(String fullFileName,int flag){
		setFileInfo(fullFileName,C_FILE_INFO_FAVORITE,flag);
	}
	public void setDelete(String fullFileName,int flag){
		setFileInfo(fullFileName,C_FILE_INFO_DELETE,flag);
	}
	public void addPlayTimes(String fullFileName){
		setFileInfo(fullFileName,C_FILE_INFO_PLAY_TIMES,1);
	}

	public synchronized void insertFile(LinkedList<FileEntry> allNewFiles,MEDIA_TYPE type) {
		synchronized(mDBWrite){
			Log.e(TAG, "batch insertFiles start,files size: " + allNewFiles.size());
			mDBWrite.beginTransaction();        //手动设置开始事务

			for(FileEntry file:allNewFiles){
				if(Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath)){
					Log.e(TAG, "batch break insertFile");
					mDBWrite.endTransaction();        //处理完成 
					return;
				}
				
				insertFile(file.mName,file.mFileNamePYForDB,file.mPathNameForDB,file.mModTime,type);
			}
			mDBWrite.setTransactionSuccessful(); //设置事务处理成功，不设置会自动回滚不提交
			mDBWrite.endTransaction();        //处理完成 
			Log.e(TAG, "batch insertFiles end,files size: " + allNewFiles.size());
		}
	}
	
	private int mPathIDCachedInsert = 0xffffffff;
	private String mPathNameCachedInsert = " ";
	private void insertFile(String fileName, String fileNamePY, String path, long modTime,MEDIA_TYPE type) {
		fileName = sqliteEscape(fileName);
		fileNamePY = sqliteEscape(fileNamePY);
		int pathID = 0;
		if(!mPathNameCachedInsert.equals(path)){
			mPathIDCachedInsert = getIDForName(TABLE_PATH, path);
			mPathNameCachedInsert = path;
		}
		pathID = mPathIDCachedInsert;
		String sqlInsertFile = "insert into " + TABLE_FILE_INFO + "(" 
		         + C_FILE_INFO_PATH_ID + "," 
			     + C_FILE_INFO_NAME +"," 
			     + C_FILE_INFO_NAME_PY +"," 
				 + C_FILE_INFO_MOD_TIME + "," 
			     + C_FILE_INFO_PLAY_TIMES + "," 
			     + C_FILE_INFO_DELETE + "," 
			     + C_FILE_INFO_TOP + ","
			     + C_FILE_INFO_FAVORITE + ","
			     + C_FILE_INFO_NEW + ","
			     + C_FILE_INFO_ID3_EXIST + ","
			     + C_FILE_INFO_FILE_EXIST + ","
			     + C_FILE_INFO_PARTION_ID + ","
			     + C_FILE_INFO_MEDIA_TYPE
			     + ") values(" + pathID + ","+ "'" + fileName +"'" + ","+ "'" + fileNamePY +"'"+ "," + modTime + "," + 
			       "0,0,0,0,0,0,1," + mPartionID+ "," + type.ordinal()+")" ;
		
		//Log.i(TAG, "insertFile: " + sqlInsertFile);
		execSQL(sqlInsertFile);
		return ;
	}
	
	public MediaObjectList getFilesID3NotExist(){
		String sqlQuery = "select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + ","+ TABLE_FILE_INFO + "." + C_FILE_INFO_NAME + "," + TABLE_PATH + "." + C_NAME
		+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH + " where " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID3_EXIST + "=0" + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		
		//Log.i(TAG, "getFilesID3NotExist: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			cursor.moveToFirst();
		    return new MediaObjectList(cursor,mDBHelper,ObjectType.WITH_PATH,mMountPath);
		}else{
			cursor.close();
			return null;
		}
	}
	
	public Cursor getFilesImageVideoThumbNotParsed(MEDIA_TYPE type){
		int t;
		if(type == MEDIA_TYPE.VIDEO){ //0:audio,1:video,2:image
			t = 1;
		}else if(type == MEDIA_TYPE.IMAGE){
			t = 2;
		}else{
			return null;
		}
		String sqlQuery = "select file_info.id,partions.mount_path, path_name.name as path_name, file_info.name  as file_name " //
	              + " from file_info,path_name,partions where file_info.partion_id=partions.id and file_info.path_id=path_name.id and file_info.file_exist=1 and " + 
				"file_info.media_type=" + t + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID + " and " 
				+"file_info.id not in (select " + C_THUMBNAIL_FILE_ID + " from " + TABLE_THUMBNAIL + ")";

				
		//Log.i(TAG, "getFilesImageVideoThumbNotParsed: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
		    return cursor;
		}else{
			cursor.close();
			return null;
		}
	}
	
	public void saveThumbNailPic(int fileID, String picName){
		if(picName == null)
			picName = "NULL";
		//String picName_PY = PingYingTool.parseString(picName);
		picName = sqliteEscape(picName);
		//picName_PY = sqliteEscape(picName_PY);
		String sqlInsertPath = "insert into " + TABLE_THUMBNAIL + "(" + C_THUMBNAIL_FILE_ID +","+C_THUMBNAIL_IMG_PATH+")" //+","+C_THUMBNAIL_IMG_PATH_PY 
		+ " values(" +fileID+ ",'" + picName + "'" +
		//",'"  + picName_PY + "'" + 
		")" ;
		execSQL(sqlInsertPath);
	}
	
	public Cursor getFilesSaved(){
		String sqlQuery = "select file_info.*,path_name.name as path " //
				+ " from file_info,path_name where file_info.path_id=path_name.id" + " and " 
						+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID;;
		
		Log.i(TAG, "getFilesSaved: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
		    return cursor;
		}else{
			cursor.close();
			return null;
		}
	}
	
	public void upDateExistFile(LinkedList<Integer> fileIDExist){
		if(fileIDExist==null)
			return;
		if( fileIDExist.size()==0)
			return;	
		String sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  C_FILE_INFO_FILE_EXIST + "=1" 
			       + " where " + TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " + C_FILE_INFO_ID +" in(";
        int MAX_UPDATE_NUM = 500;//limit the sql text length.
		
		String idStr = new String();
		int i=0;
		for(i=0;i<fileIDExist.size();i++){
			if(Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath)){
				Log.e(TAG, "upDateExistFile batch break");
				return;
			}
			
			idStr = idStr + fileIDExist.get(i) + ",";
			if((i+1)%MAX_UPDATE_NUM==0){
				String updateSql = new String();
				updateSql = sqlUpdateFile + idStr;
				updateSql = updateSql.substring(0, updateSql.length()-1) + ")";
				execSQL(updateSql);
				idStr = new String();
			}
		}
		
		if((i+1)%MAX_UPDATE_NUM!=0){
			String updateSql = new String();
			updateSql = sqlUpdateFile + idStr;
			updateSql = updateSql.substring(0, updateSql.length()-1) + ")";
			execSQL(updateSql);
			idStr = new String();
		}

		return ;
	}
	
	public void upDateNewFile(long lastModTime){
		String sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  C_FILE_INFO_NEW + "=1" 
			       + " where "  + TABLE_FILE_INFO +"." +  C_FILE_INFO_MOD_TIME + ">" + lastModTime
		           + " and " + TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID 
		           + " and " + TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		execSQL(sqlUpdateFile);
	}

//	public void delAllFilesNotExist() {
//		// del id3 first
//		String sqlDelId3 = "delete from " + TABLE_ID3_INFO + " where " + TABLE_ID3_INFO + "." + C_ID3_INFO_FILE_ID
//				+ " in (select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + " from " + TABLE_FILE_INFO + " where "
//				+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=0)";
//		execSQL(sqlDelId3);
//
//		// String sqlDelPath = "delete from " + TABLE_PATH + " where "+ TABLE_PATH+ "." +C_ID
//		// + " in (select " + TABLE_FILE_INFO +"." + C_FILE_INFO_PATH_ID +" from " + TABLE_FILE_INFO
//		// + " where "+ TABLE_FILE_INFO+"."+C_FILE_INFO_FILE_EXIST + "=0)";
//		// execSQL(sqlDelPath);
//
//		String sqlDelFile = "delete from " + TABLE_FILE_INFO + " where " + C_FILE_INFO_FILE_EXIST + "=0";
//		execSQL(sqlDelFile);
//		return;
//	}
	
	public void updateAllFilesToNotExist(){
		String sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  C_FILE_INFO_FILE_EXIST + "=0, " 
				+ C_FILE_INFO_NEW + "=0" 
	          + " where " + TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID;;
	    Log.i(TAG, "updateAllFilesToNotExist: " + sqlUpdateFile);
	    execSQL(sqlUpdateFile);
	}

	public  void saveID3(LinkedList<ID3Info> id3List) {
		synchronized(mDBWrite){
			try {
				Log.e(TAG, "batch saveID3 start,files size: " + id3List.size());
				mDBWrite.beginTransaction();        //手动设置开始事务

				for(ID3Info id3:id3List){
					if(Thread.interrupted() || !CommonUtil.isDeviceExsit(mMountPath)){
						Log.e(TAG, "batch break");
						break;
					}
					saveID3(id3);
				}
				mDBWrite.setTransactionSuccessful(); //设置事务处理成功，不设置会自动回滚不提交
				mDBWrite.endTransaction();        //处理完成 
				Log.e(TAG, "batch saveID3 end,files size: " + id3List.size());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void saveID3(ID3Info id3) {
		int albumID;
		int artistID;
		int genreID;
		int composerID;
		
		id3.title = sqliteEscape(id3.title);
		id3.title_py = sqliteEscape(id3.title_py);
		id3.picPath = sqliteEscape(id3.picPath);
		albumID = getIDForName(TABLE_ALBUM, id3.album.getName(),id3.album_py);
		artistID = getIDForName(TABLE_ARTIST, id3.artist.getName(),id3.artist_py);
		genreID = getIDForName(TABLE_GENRE, id3.genre.getName(),id3.genre_py);
		composerID = getIDForName(TABLE_COMPOSER, id3.composer.getName(),id3.composer_py);
		
		int track_number = 0;
		try{
			if(id3.track_number.contains("/")){
				//2/19 should store track_number 2 and total track 19 seperatly
				id3.track_number = id3.track_number.substring(0, id3.track_number.indexOf("/"));
			}
			track_number = Integer.parseInt(id3.track_number);
		}catch(Exception e){
			AdayoLog.logError(TAG, "Integer.parseInt(id3.track_number),error,track_number="+id3.track_number);
			track_number = 0;
			e.printStackTrace();
		}
		
		String sqlInsertFile = "insert into " + TABLE_ID3_INFO + "(" 
		        + C_ID3_INFO_FILE_ID + "," 
				+ C_ID3_INFO_TITLE +"," 
				+ C_ID3_INFO_TITLE_PY +"," 
		        + C_ID3_INFO_ARTIST_ID + "," 
				+ C_ID3_INFO_ALBUM_ID + ","
				+ C_ID3_INFO_COMPOSER_ID + "," 
				+ C_ID3_INFO_GENRE_ID+"," 
				+ C_ID3_INFO_DURATION + "," 
				+ C_ID3_INFO_CD_TRACK_NUMBER + "," 
				+ C_ID3_INFO_PIC+ ") values(" + id3.file.getID() + ","+ "'" + id3.title +"','"  + id3.title_py +"'"
				+ "," + artistID+ "," + albumID + "," + composerID + "," + genreID  
				+ ","  +"'" + id3.duration +"'" 
				+ ","  + track_number + "," +"'"+ id3.picPath  +"'" + ")" ;
		execSQL(sqlInsertFile);
		
		String sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "+  C_FILE_INFO_ID3_EXIST + "=1 where " + C_FILE_INFO_ID + "=" + id3.file.getID();
		execSQL(sqlUpdateFile);
		return;
	}
	
    /////for query interface///////////////////////////////////////////////////////
	public MediaObjectList getFilesExist(){
		String sqlQuery = "select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + ","+ TABLE_FILE_INFO + "." + C_FILE_INFO_NAME + "," + TABLE_PATH + "." + C_NAME
		+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH + " where " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		
		Cursor cursor = execQuery(sqlQuery);
		cursor.moveToFirst();
		return new MediaObjectList(cursor,mDBHelper,ObjectType.WITH_PATH,mMountPath);
	} 
	
	public ID3Info getID3(MediaObjectName file){
		ID3Info id3 = null;
		String sqlQuery = "select " + TABLE_ID3_INFO + "." + C_ID3_INFO_ID + ","
		        + TABLE_ID3_INFO + "." + C_ID3_INFO_TITLE + "," 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_ARTIST_ID + "," 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_ALBUM_ID + "," 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_COMPOSER_ID + "," 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_GENRE_ID + "," 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_DURATION 	
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_CD_TRACK_NUMBER 
				+ " from " + TABLE_FILE_INFO + "," + TABLE_ID3_INFO + " where " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID + "=" + file.getID() + " and "
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID + "=" + TABLE_ID3_INFO+ "." + C_ID3_INFO_ID + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID3_EXIST + "=1" + " and " 
			    + TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
			    + TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		//Log.i(TAG, "getID3: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()==0){
			id3=null;
		}else {
			id3 = new ID3Info();
			id3.file=file;
		    cursor.moveToFirst();
		    id3.title = cursor.getString(1);
		    id3.title = id3.title == "null" ? null :id3.title;
		    
		    String tmp = getNameForID(TABLE_ARTIST, cursor.getInt(2));
		    id3.artist = (new MediaObjectName(cursor.getInt(2),tmp));
		    
		    tmp = getNameForID(TABLE_ALBUM, cursor.getInt(3));
		    id3.album = (new MediaObjectName(cursor.getInt(3),tmp));
		    
		    tmp = getNameForID(TABLE_COMPOSER, cursor.getInt(4));
		    id3.composer = ( new MediaObjectName(cursor.getInt(4),tmp));
		    
		    tmp = getNameForID(TABLE_GENRE, cursor.getInt(5));
		    id3.genre = (new MediaObjectName(cursor.getInt(5),tmp));
		    
		    id3.duration = cursor.getString(6);
		    id3.duration = id3.duration == "null"? null: id3.duration;
		    
		    id3.track_number = cursor.getString(7);
		    id3.track_number = id3.track_number == "null"? null: id3.track_number;
		}

		cursor.close();
		return id3;
	}
	
	// 获取一个目录下的所有歌曲
	public MediaObjectList getAllPaths(){
		String sqlQuery = "select distinct " + TABLE_PATH + "." + C_ID + "," + TABLE_PATH + "." + C_NAME 				
				+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH + " where " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		//Log.i(TAG, "getAllPaths: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			 cursor.moveToFirst();
		     return new MediaObjectList(cursor,mDBHelper,ObjectType.NO_PATH,mMountPath);
		}else {
			cursor.close();
			return null;
		}
	}

	public MediaObjectList getFilesForPath(MediaObjectName path){
		if(path == null)
			return null;
		String sqlQuery = "select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + ","+ TABLE_FILE_INFO + "." + C_FILE_INFO_NAME + "," + TABLE_PATH + "." + C_NAME
		+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH + " where " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + path.getID() + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		//Log.i(TAG, "getFilesForPath: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			 cursor.moveToFirst();
		     return new MediaObjectList(cursor,mDBHelper,ObjectType.WITH_PATH,mMountPath);
		}else{
			cursor.close();
			return null;
		}
	}
	
	private MediaObjectList getSQLQueryIDForID3(String tableName){
		String sqlQuery = "select distinct " + tableName + "." + C_ID + "," + tableName + "." + C_NAME 				
				+ " from " + TABLE_FILE_INFO + "," + TABLE_ID3_INFO + "," + tableName + " where " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID + "=" + TABLE_ID3_INFO+ "." + C_ID3_INFO_FILE_ID + " and " 
				+ TABLE_ID3_INFO + "." + C_ID3_INFO_ARTIST_ID + "=" + tableName+ "." + C_ID + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID3_EXIST + "=1" + " and "
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID;;
		
		//Log.i(TAG, "getSQLQueryIDForID3: " + tableName + ", sql:"+sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			 cursor.moveToFirst();
		     return new MediaObjectList(cursor,mDBHelper,ObjectType.NO_PATH,mMountPath);
		}else{
			cursor.close();
			return null;
		}		
	}
	
	private MediaObjectList getSQLQueryFilesForID3(MediaObjectName objectID3, String id){
		if(objectID3 == null )
			return null;
		if(objectID3.getID() < 0 )
			return null;		
		if(objectID3.getName() == null )
			return null;

		String sqlQuery = "select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + ","+ TABLE_FILE_INFO + "." + C_FILE_INFO_NAME + "," + TABLE_PATH + "." + C_NAME
		+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH +"," + TABLE_ID3_INFO+ " where " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID + "=" + TABLE_ID3_INFO+ "." + C_ID3_INFO_FILE_ID + " and " 
		+ TABLE_ID3_INFO + "." + id + "=" + objectID3.getID() + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_ID3_EXIST + "=1" + " and "
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
		+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID;;
		//Log.i(TAG, "getSQLQueryFilesForID3: " + objectID3.getID()+ "," + objectID3.getName() + ", sql:"+sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			 cursor.moveToFirst();
		     return new MediaObjectList(cursor,mDBHelper,ObjectType.WITH_PATH,mMountPath);
		}else{
			cursor.close();
			return null;
		}
	}	
	
	// 获取一个歌手下的所有歌曲
	public MediaObjectList getAllArtist(){
		return getSQLQueryIDForID3(TABLE_ARTIST);
	}

	public MediaObjectList getFilesForArtist(MediaObjectName artist){
		return getSQLQueryFilesForID3(artist, C_ID3_INFO_ARTIST_ID);
	}

	// 获取一个专辑下的所有歌曲
	public MediaObjectList getAllAlbum(){
		return getSQLQueryIDForID3(TABLE_ALBUM);
	}

	public MediaObjectList getFilesForAlbum(MediaObjectName album){
		return getSQLQueryFilesForID3(album, C_ID3_INFO_ALBUM_ID);
	}
	
	// 获取一个genre下的所有歌曲
	public MediaObjectList getAllGenre(){
		return getSQLQueryIDForID3(TABLE_GENRE);
	}
	
	public MediaObjectList getFilesForGenre(MediaObjectName genre){
	    return getSQLQueryFilesForID3(genre, C_ID3_INFO_GENRE_ID);
	}
	
	// 获取一个composer下的所有歌曲
	public MediaObjectList getAllComposer(){
		return getSQLQueryIDForID3(TABLE_COMPOSER);
	}
	public MediaObjectList getFilesForComposer(MediaObjectName composer){
		return getSQLQueryFilesForID3(composer,C_ID3_INFO_COMPOSER_ID);
	}
	
	// favorite
	public boolean setFavorite(MediaObjectName file,boolean flag){
		String flaString = flag==true?"1":"0";
		String sqlUpdateFile = "update " + TABLE_FILE_INFO + " set "  +  C_FILE_INFO_FAVORITE + "=" + flaString+" where "
				+  TABLE_FILE_INFO + "." + C_FILE_INFO_ID +"=" + file.getID() + " and " 
				 + TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID;;
		//Log.i(TAG, "sqlUpdateFile: " + sqlUpdateFile);
		execSQL(sqlUpdateFile);
		return true;
	}
	
	public MediaObjectList getAllFavorite(){
		String sqlQuery = "select " + TABLE_FILE_INFO + "." + C_FILE_INFO_ID + ","+ TABLE_FILE_INFO + "." + C_FILE_INFO_NAME + "," + TABLE_PATH + "." + C_NAME				
				+ " from " + TABLE_FILE_INFO + "," + TABLE_PATH + " where " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PATH_ID + "=" + TABLE_PATH+ "." + C_ID + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_FAVORITE + "=1 and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_FILE_EXIST + "=1" + " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_PARTION_ID + "=" + mPartionID+ " and " 
				+ TABLE_FILE_INFO + "." + C_FILE_INFO_MEDIA_TYPE + "=0" ;
		//Log.i(TAG, "getAllFavorite: " + sqlQuery);
		Cursor cursor = execQuery(sqlQuery);
		if(cursor.getCount()>0){
			 cursor.moveToFirst();
		     return new MediaObjectList(cursor,mDBHelper,ObjectType.WITH_PATH,mMountPath);
		}else{
			cursor.close();
			return null;
		}
	}
	
	private static String sqliteEscape(String keyWord){  
		if(keyWord == null)
			return null;
        //keyWord = keyWord.replace("/", "//");  
        keyWord = keyWord.replace("'", "''");  
        //keyWord = keyWord.replace("[", "/[");  
        //keyWord = keyWord.replace("]", "/]");  
        //keyWord = keyWord.replace("%", "/%");  
        //keyWord = keyWord.replace("&","/&");  
        //keyWord = keyWord.replace("_", "/_");  
        //keyWord = keyWord.replace("(", "/(");  
        //keyWord = keyWord.replace(")", "/)");  
        return keyWord;  
    }  
}
