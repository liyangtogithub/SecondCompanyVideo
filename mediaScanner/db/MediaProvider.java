package com.adayo.mediaScanner.db;

import java.util.LinkedList;

import com.adayo.mediaScanner.MediaScannerInterface;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.fileScanner.MediaScanner;
import com.adayo.midwareproxy.utils.AdayoLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MediaProvider extends ContentProvider{

	private static final String TAG = "MediaProvider";
	
	private static final String MEDIA_AUTHORITY = "Media";
    private static final UriMatcher mUriMatcher;
    private static final int WHOLETABLE = 1;
    private static final int ID3INFO = 2;
    
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "songs", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "artists", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "albums", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "genres", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "favorites", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "composers", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "filePath", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "id3info", ID3INFO);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "videos", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY, "photos", WHOLETABLE);
        mUriMatcher.addURI(MEDIA_AUTHORITY,"thumbnails",WHOLETABLE);
    }
    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		return null;
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		// TODO Auto-generated method stub
		return super.bulkInsert(uri, values);
	}

		
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		try {
			Cursor c = null ; 
			String path = uri.getPath();
			int index = path.indexOf(":");
			STORAGE_PORT storage_id = getStroagePort(uri);
			
			if(index>0){
				path = path.substring(0, index);
			}
			
			Log.v(TAG, "query uri="+uri.toString()+" path="+uri.getPath()+" storage_id="+storage_id.name());
			SQLiteDatabase db = getDataBase(storage_id,false);
			
			if(db==null){
				Log.e(TAG,"db == null");
				return null;
			}
			
			LinkedList<Integer> part = MediaDB.getPartionsID(storage_id);
			if(part==null){
				Log.e(TAG,"no partition mounted.");
				return null;
			}
			
			if(storage_id != STORAGE_PORT.INVALID){
				String partitions = "-1";
				for (int i = 0; i < part.size(); i++) {
					partitions += ","+part.get(i);
				}
				if(TextUtils.isEmpty(selection)){
					selection = " partion_id in ("+partitions+")";
				}else
					selection += " and partion_id in ("+partitions+")";
			}
			
			if(path.startsWith("/songs")){
				c = db.query("audio", projection, selection, selectionArgs, null, null, sortOrder);			
			} else if(path.startsWith("/artists")) {
				c = db.query("artist_name", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/albums")) {
				c = db.query("album_name", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/genres")) {
				c = db.query("genre_name", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/composers")) {
				c = db.query("composer_name", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/id3_info")) {
				c = db.query("id3_info", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/id3info")) {
				c = db.query("view_id3info", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/favorites")) {
				c = db.query("file_info", projection, selection, selectionArgs, null, null, sortOrder);	
			} else if(path.startsWith("/filePath")) {
				//c = db.rawQuery(MediaDB.getForPathSql(devPath), selectionArgs);	
			} else if(path.startsWith("/path_name")){
				c = db.query("path_name", projection, selection, selectionArgs, null, null, sortOrder);
			}else if(path.startsWith("/videos")){
				c = db.query("video", projection, selection, selectionArgs, null, null, sortOrder);
			}else if(path.startsWith("/photos")){
				c = db.query("image", projection, selection, selectionArgs, null, null, sortOrder);
			}else if(path.startsWith("/thumbnails")){
				c = db.query("thumbnail",projection, selection, selectionArgs, null, null, sortOrder);
			}
			
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 

	}
	

	private SQLiteDatabase getDataBase(STORAGE_PORT s,boolean writeable) {

		MediaDB db = MediaScanner.mMediaDB;
		if(db ==null){
			AdayoLog.logError(TAG, "get db error!");
			return null;
		}
		
		if(writeable){
			return db.getWritableDatabase();
		} else {
			return db.getReadableDatabase();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String path = uri.getPath();
		int index = path.indexOf(":");
		path = path.substring(0, index);
		STORAGE_PORT s = getStroagePort(uri);
		
		Log.v(TAG, "query uri="+uri.toString()+" path="+uri.getPath()+" STORAGE_PORT="+s.toString());
		SQLiteDatabase db = getDataBase(s,true);
				
		if(path.startsWith("/songs")){
			db.update("file_info", values, selection, selectionArgs);
		}
		return 0;
	}
	
	private STORAGE_PORT getStroagePort(Uri uri) {
		String path = uri.getPath();
		int index = path.indexOf(":");
		String sName = path.substring(index+1);
		
		return STORAGE_PORT.valueOf(sName);
	}
}
