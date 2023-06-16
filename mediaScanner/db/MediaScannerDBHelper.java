package com.adayo.mediaScanner.db;

import java.io.File;
import java.util.LinkedList;
import com.adayo.mediaScanner.MediaObjectName;
import com.adayo.mediaScanner.ID3Info;
import com.adayo.mediaScanner.MediaScannerInterface.MEDIA_TYPE;
import com.adayo.mediaScanner.db.MediaObjectList;
import com.adayo.mediaScanner.fileScanner.FileScanner.FileEntry;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

public class MediaScannerDBHelper {
	private static final String TAG = "MediaScannerDBHelper";
	private boolean mDBExist = false;
	private MediaDB mMediaDB = null;
	private Context mContext = null;
	private LinkedList<MediaObjectList> mMediaLists = null;

	public MediaScannerDBHelper(Context context, String path,String devID) {
		mContext = context;
		devID  = "id." + devID;
		mMediaDB = new MediaDB(context, path, this);
		mMediaLists = new LinkedList<MediaObjectList>();
		mMediaDB.init();
	}

	public void addMediaObjectList(MediaObjectList list) {
		if (list != null && !mMediaLists.contains(list)) {
			mMediaLists.add(list);
		}
	}

	public int getMediaObjectListIndexID(MediaObjectList list) {
		if (list != null && !mMediaLists.contains(list)) {
			return -1;
		}
		return mMediaLists.indexOf(list);
	}

	public MediaObjectList getMediaObjectList(int index) {
		if (index < 0 || index >= mMediaLists.size())
			return null;

		return mMediaLists.get(index);
	}

	public void removeMediaObjectListIndexID(MediaObjectList list) {
		if (!mMediaLists.contains(list)) {
			return;
		}
		mMediaLists.remove(list);
	}
	
	public MediaDB getMediaDB(){
		return mMediaDB;
	}

	public void releaseDB(){
		for(MediaObjectList list:mMediaLists){
			list.releaseCursor();
		}
		mMediaDB.closetDB();
	}
	
	public void setFavorite(String fullFileName,int flag){
		mMediaDB.setFavorite(fullFileName,flag);
	}
	public void setTop(String fullFileName,int flag){
		mMediaDB.setTop(fullFileName,flag);
	}
	public void setDelete(String fullFileName,int flag){
		mMediaDB.setDelete(fullFileName,flag);
	}
	public void addPlayTimes(String fullFileName){
		mMediaDB.addPlayTimes(fullFileName);
	}

	public void fileInsert(LinkedList<FileEntry>  allNewFiles,MEDIA_TYPE type) {
		if(allNewFiles.size()>0)
		     mMediaDB.insertFile(allNewFiles, type);
	}
	
	public void upDateExistFile(LinkedList<Integer> fileIDExist) {
		if(fileIDExist.size()>0)
		    mMediaDB.upDateExistFile(fileIDExist);
	}
	
	public void upDateNewFile(long lastModTime){
		mMediaDB.upDateNewFile(lastModTime);
	}

//	public void delAllFilesNotExist() {
//		mMediaDB.delAllFilesNotExist();
//	}

	// 开机时，为防止上次断电有在扫描，先清除数据
	public void updateAllFilesToNotExist() {
		mMediaDB.updateAllFilesToNotExist();
	}

	public void saveID3(LinkedList<ID3Info> id3List) {
		try {
			mMediaDB.saveID3(id3List);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public MediaObjectList getFilesID3NotExist() {
		return mMediaDB.getFilesID3NotExist();
	}
	
	public Cursor getFilesImageVideoThumbNotParsed(MEDIA_TYPE type){
		return mMediaDB.getFilesImageVideoThumbNotParsed(type);
	}
	
	public void saveThumbNailPic(int fileID, String picName){
		mMediaDB.saveThumbNailPic(fileID,picName);
	}
	
	public Cursor getFilesSaved() {
		return mMediaDB.getFilesSaved();
	}

	// 获取所有媒体文件
	public MediaObjectList getFiles() {
		return mMediaDB.getFilesExist();
	}

	public ID3Info getFileID3(MediaObjectName file) {
		return mMediaDB.getID3(file);
	}

	// 获取一个目录下的所有歌曲
	public MediaObjectList getAllPaths() {
		return mMediaDB.getAllPaths();
	}

	public MediaObjectList getFilesForPath(MediaObjectName path) {
		return mMediaDB.getFilesForPath(path);
	}

	// 获取一个歌手下的所有歌曲
	public MediaObjectList getAllArtist() {
		return mMediaDB.getAllArtist();
	}

	public MediaObjectList getFilesForArtist(MediaObjectName artist) {
		return mMediaDB.getFilesForArtist(artist);
	}

	// 获取一个专辑下的所有歌曲
	public MediaObjectList getAllAlbum() {
		return mMediaDB.getAllAlbum();
	}

	public MediaObjectList getFilesForAlbum(MediaObjectName album) {
		return mMediaDB.getFilesForAlbum(album);
	}

	// 获取一个genre下的所有歌曲
	public MediaObjectList getAllGenre() {
		return mMediaDB.getAllGenre();
	}

	public MediaObjectList getFilesForGenre(MediaObjectName genre) {
		return mMediaDB.getFilesForGenre(genre);
	}

	// 获取一个composer下的所有歌曲
	public MediaObjectList getAllComposer() {
		return mMediaDB.getAllComposer();
	}

	public MediaObjectList getFilesForComposer(MediaObjectName composer) {
		return mMediaDB.getFilesForComposer(composer);
	}

	// favorite
	public boolean setFavorite(MediaObjectName file, boolean flag) {
		return mMediaDB.setFavorite(file, flag);
	}

	public MediaObjectList getAllFavorite() {
		return mMediaDB.getAllFavorite();
	}
}
