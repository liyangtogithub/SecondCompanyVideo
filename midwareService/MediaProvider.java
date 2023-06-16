package com.adayo.midware.mpeg.db;

import java.util.HashMap;

import com.adayo.midware.constant.MpegConstantsDef;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class MediaProvider extends ContentProvider{
	private MediaDBCenter dbHelper;
	
    private static final UriMatcher mUriMatcher;
    private static final int WHOLETABLE = 1;
    private static final int TABLE_ID = 2;
    private static HashMap<String, String> projectionMap;
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "media", WHOLETABLE);
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "media/#", TABLE_ID);
        
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "audio", WHOLETABLE);
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "audio/#", TABLE_ID);
        
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "video", WHOLETABLE);
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "video/#", TABLE_ID);
        
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "photo", WHOLETABLE);
        mUriMatcher.addURI(MpegConstantsDef.DVD_AUTHORITY, "photo/#", TABLE_ID);
        
        projectionMap = new HashMap<String, String>();
        projectionMap.put(MediaFileColumn.Index, MediaFileColumn.Index);
        projectionMap.put(MediaFileColumn.FileName, MediaFileColumn.FileName);
        projectionMap.put(MediaFileColumn.ParentId, MediaFileColumn.ParentId);
        projectionMap.put(MediaFileColumn.Clips, MediaFileColumn.Clips);
        projectionMap.put(MediaFileColumn.isFile, MediaFileColumn.isFile);
        projectionMap.put(MediaFileColumn.Types, MediaFileColumn.Types);
    }
    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = dbHelper.getWDB();
        int count;
        
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = dbHelper.getWDB();
		
		long rowId = db.insert("mediafiles", null, values);
        if (rowId > 0) {
            Uri mUri = ContentUris.withAppendedId(MediaFileColumn.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(mUri, null);
            return mUri;
        }
		
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
		dbHelper = new MediaDBCenter(this.getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		SQLiteDatabase db = dbHelper.getRDB();
		Cursor c = null ; 
		String path = uri.getPath();
		if(path.startsWith("/media")){
			c = db.query("mediafiles", projection, selection, selectionArgs, null, null, sortOrder);			
		} else if(path.startsWith("/audio")) {
			c = db.query("audio", projection, selection, selectionArgs, null, null, sortOrder);	
		} else if(path.startsWith("/video")) {
			c = db.query("video", projection, selection, selectionArgs, null, null, sortOrder);	
		} else if(path.startsWith("/photo")) {
			c = db.query("photo", projection, selection, selectionArgs, null, null, sortOrder);	
		}
		//Log.e("test", "query uri="+uri.toString()+" path="+uri.getPath()+" ===="+c.getCount());
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
