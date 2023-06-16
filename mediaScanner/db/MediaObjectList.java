package com.adayo.mediaScanner.db;

import com.adayo.mediaScanner.MediaObjectName;

import android.R.integer;
import android.database.Cursor;

public class MediaObjectList{
	public enum ObjectType{
		WITH_PATH,
		NO_PATH,
	}
	private Cursor mCursor;
	private ObjectType mObjectType;
	private int mIndexID;
	private MediaScannerDBHelper mDBHelper;
	private String mMountPath;
	public MediaObjectList(Cursor c, MediaScannerDBHelper dbHelper, ObjectType type, String path){
		mCursor = c;
		mObjectType = type;
		mDBHelper = dbHelper;
		mMountPath = path;
		dbHelper.addMediaObjectList(this);
		mIndexID = dbHelper.getMediaObjectListIndexID(this);
	}
	
	public int getIndexID(){
		return mIndexID;
	}
	
	private String getObjectName(){
		String name=null;
		switch (mObjectType) {
		case WITH_PATH:
			name = mMountPath + mCursor.getString(2) + mCursor.getString(1);
			//name =  mCursor.getString(2) + mCursor.getString(1);
			break;
		case NO_PATH:
			name = mCursor.getString(1);
			break;
		default:
			break;
		}
		return name;
	}
	
	public int getSize(){
		return mCursor.getCount(); 
	}
	
	public MediaObjectName getNextMediaObject(){
		if(mCursor.isAfterLast()){
			return null;
		}
		MediaObjectName mediaObjectName = new MediaObjectName(mCursor.getInt(0),getObjectName());
		mCursor.moveToNext();
//		if(mCursor.isAfterLast()){
//			mCursor.close();
//		}
		return mediaObjectName;
	}
	
	public MediaObjectName getMediaObject(int index){
		if(index>mCursor.getCount() || index<0){
			return null;
		}
		
		mCursor.moveToPosition(index);
		MediaObjectName mediaObjectName = new MediaObjectName(mCursor.getInt(0),getObjectName());
		//mCursor.moveToNext();

		return mediaObjectName;
	}
	
	public void releaseCursor(){
		if(!mCursor.isClosed())
		    mCursor.close();
	}
	
	public void finishQuery(){
		if(!mCursor.isClosed())
		    mCursor.close();
	    mDBHelper.removeMediaObjectListIndexID(this);
	}
}
