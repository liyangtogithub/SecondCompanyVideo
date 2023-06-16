package com.adayo.mediaScanner;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.midwareproxy.utils.AdayoLog;


public class AdayoMediaManager {
	private static String TAG = AdayoMediaManager.class.getName();
	private static AdayoMediaManager mManager = null;
	private static Context mContext;

	private final static String MEDIA_AUTHORITY = "Media";
	
	public final static String _ID = "_id";
	public final static String AUDIO_ID = "id";
	public final static String AUDIO_NAME = "name";
	public final static String AUDIO_PATH_ID = "path_id";
	public final static String AUDIO_PATH_NAME = "path_name";
	public final static String FILE_ID = "file_id";
	public final static String TITLE = "title";
	public final static String ARTIST_ID = "artist_id";
	public final static String ARTIST_NAME = "artist_name";
	public final static String ALBUM_ID = "album_id";
	public final static String ALBUM_NAME = "album_name";		
	public final static String GENRE_ID = "genre_id";
	public final static String GENRE_NAME = "genre_name";
	public final static String COMPOSER_ID = "composer_id";
	public final static String COMPOSER_NAME = "composer_name";
	public final static String DURATION = "duration";
	public final static String PIC = "pic";
	public final static String FAVORITE = "favorite";
	public final static String TOP = "top";
	public final static String NEW = "new";
	public final static String HIDE = "hide";
	public final static String NAME_PY = "name_py";
	public final static String PLAY_TIMES = "play_times";
	public static int ON = 1;
    public static int OFF = 0;
    
	public final static String VIDEO_ID = "id";
	public final static String VIDEO_PATH_NAME = "path_name";
	public final static String VIDEO_FILE_NAME = "name";
	public final static String VIDEO_FILE_NAME_PY = "name_py";
	public final static String PHOTO_ID = VIDEO_ID;
	public final static String PHOTO_PATH_NAME = VIDEO_PATH_NAME;
	public final static String PHOTO_FILE_NAME = VIDEO_FILE_NAME;
	public final static String PHOTO_FILE_NAME_PY = VIDEO_FILE_NAME_PY;
	public static final String THUMBNAIL_FILE_ID = "file_id";
	public static final String THUMBNAIL_PATH_NAME = "thumb_path";
	
	public static final String PARTION_ID="partion_id";
	private static final String TITLE_PY = "title_py";
	
	private AdayoMediaManager() {
		
	}

	public static AdayoMediaManager getMediaManager(Context context) {
		if (mManager == null) {
			mContext = context;
			mManager = new AdayoMediaManager();
		}
		return mManager;
	}
	/**
	 * 返回MediaScanner的数据库的表的Uri。数据内容是所有已经插上的设备
	 * 
	 * @param table 可取的值有
	 * "songs"
        "artists"
        "albums"
        "genres"
        "favorites"
        "composers"
        "filePath"
        "id3info"
       "videos",
       "photos"
        thumbnails"
	 * @return MediaScanner的数据库表中的对应的URI
	 */
	public static Uri getUri(String table){
		
		return Uri.parse("content://"+MEDIA_AUTHORITY+"/"+table+":"+STORAGE_PORT.STORAGE_ALL.name());
	}
	
	
	/**
	 * 返回MediaScanner的数据库的表的Uri。
	 * @param path 指定的设备。例如 STORAGE_PORT.USB1.name()
	 * @param table 数据库表
	 * @return MediaScanner的数据库表中的对应的URI
	 */
	public static Uri getUri(String path, String table){
		if(null == path){
			return getUri(table);
		}
		return Uri.parse("content://"+MEDIA_AUTHORITY+"/"+table+":"+path);
	}
	
	/**
	 * 查询songs表中的音频文件，返回的字段有AUDIO_PATH_ID
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name()
	 * @param pathId 指定的path id
	 * @return 指定的path id的歌曲
	 */
	public Cursor getSongs(String devPath,String pathId) {
		
		String selection = "";
		String[] selectionArgs = null;
		
		if(null != pathId){
			selection = "path_id=?";
			selectionArgs = new String[]{ pathId };
		}
		
		return getSongs(devPath, selection, selectionArgs);
	}
	/**
	 * 查询songs表中的音频文件，返回的字段有
	 * _ID,AUDIO_ID,AUDIO_NAME,PLAY_TIMES,AUDIO_PATH_NAME,FAVORITE,NEW,TOP
	 *  其排序的方式为top desc，且不返回已经标记为删除的条目
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name()
	 * @param selection 数据库查询语句的selection
	 * @param selectionArgs 数据库查询语句的selectionArgs
	 * @return 符合条件的songs表中的条目
	 */
	public Cursor getSongs(String devPath,String selection,String selectionArgs[]) {
		Uri uri = getUri(devPath,"songs");
		String order = "top desc";
		
		if(selection != null && selection.length()>0){
			selection += " and "+HIDE+"="+OFF;
		} else {
			selection = HIDE+"="+OFF;
		}
		
		return mContext.getContentResolver().query(uri, new String[]{_ID,AUDIO_ID,AUDIO_NAME,PLAY_TIMES,AUDIO_PATH_NAME,FAVORITE,NEW,TOP}, selection, selectionArgs, order);
	}
	
	/**
	 * 查询songs表中的音频文件中的已收藏的文件
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的songs表中的条目
	 */
	public Cursor getFavoriteCursor(String devPath) {
		String selection = FAVORITE+"=?";
		String[] selectionArgs = new String[]{ String.valueOf(ON) };
		
		return getSongs(devPath, selection, selectionArgs);
	}
	/**
	 * 查询songs表中的音频文件中的新增文件
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的songs表中的条目
	 */
	public Cursor getNewCursor(String devPath) {
		String selection = NEW+"=?";
		String[] selectionArgs = new String[]{ String.valueOf(ON) };
		return getSongs(devPath, selection, selectionArgs);
	}
	/**
	 * 查询id3info表中的艺人信息，其中的字段有ARTIST_ID，ARTIST_NAME，不包括已经标记删除的。按照艺人名称的拼音排序
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的id3info表中的条目
	 */
	public Cursor getArtists(String devPath) {
		Uri uri = getUri(devPath,"id3info");

		String id = "distinct "+ARTIST_ID+" as _id";
		String[] projection = {id,ARTIST_NAME};
		String selection = HIDE+"="+OFF;
		String order = "lower(trim(artist_name_py))";
		
		return getList(uri,projection,selection, null,order);
	}
	/**
	 * 查询id3info表中的专辑信息，其中的字段有ALBUM_ID，ALBUM_NAME，不包括已经标记删除的。按照专辑名称的拼音排序
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的id3info表中的条目
	 */	
	public Cursor getAlbums(String devPath) {
		Uri uri = getUri(devPath,"id3info");
		
		String id = "distinct "+ALBUM_ID+" as _id";
		String[] projection = {id,ALBUM_NAME};
		String selection = HIDE+"="+OFF;
		String order = "lower(trim(album_name_py))";
		
		return getList(uri,projection,selection, null,order);
	}
	/**
	 * 查询id3info表中的Genres信息，其中的字段有GENRES_ID，GENRES_NAME，不包括已经标记删除的。
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的id3info表中的条目
	 */	
	public Cursor getGenres(String devPath) {
		Uri uri = getUri(devPath,"id3info");

		String id = "distinct "+GENRE_ID+" as _id";
		String[] projection = {id,GENRE_NAME};
		String selection = HIDE+"="+OFF;
		
		return getList(uri,projection,selection, null, null);
	}
	/**
	 * 查询id3info表中的Composers信息，其中的字段有COMPOSERS_ID，COMPOSERS_NAME，不包括已经标记删除的。
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name() 
	 * @return 符合条件的id3info表中的条目
	 */	
	public Cursor getComposers(String devPath) {
		Uri uri = getUri(devPath,"id3info");

		String id = "distinct "+COMPOSER_ID+" as _id";
		String[] projection = {id,COMPOSER_NAME};
		String selection = HIDE+"="+OFF;
		
		return getList(uri,projection,selection, null,null);
	}
	/**
	 * 根据favorites table获取已收藏的歌曲（不建议用，建议使用 getFavoriteCursor）
	 */
	private Cursor getFavorites(String devPath) {
		Uri uri = getUri(devPath,"favorites");
		String where = "favorite = 1";
		
		return getList(uri,null,where, null,null);
	}
	//filePath 这个在MediaProvider中没有实现
	private HashMap<String, String> getPathById(String devPath,String Id) {
		Uri uri = getUri(devPath,"filePath");
		
		return getPathByFileId(uri,Id);
	}
	
	private Cursor getList(Uri uri,String[] projection,String selection,String[] selectionArgs, String sortOrder) {
		
		return mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
	}
	
	private HashMap<String, String> getPathByFileId(Uri uri,String Id) {
		
		String path_id = "path_id";
		String path_name = "path_name";
		HashMap<String, String> map = new HashMap<String, String>();
		Cursor c = mContext.getContentResolver().query(uri, null, "path_id=?", new String[]{Id}, null);
		if(null != c){
			try {
				while(c.moveToNext()){
					map.put(path_id, c.getString(0));
					map.put(path_name, c.getString(1));
				}
				c.close();
			} catch (Exception e) {
				e.printStackTrace();
				c.close();
			}
		}
		return map;
	}
	/**
	 * 获取ID3信息
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name()
	 * @param fileId 音频的文件ID
	 * @return 字段有FILE_ID、TITLE、ARTIST_ID、ALBUM_ID、ALBUM_NAME、
	 * GENRE_ID、COMPOSER_ID、COMPOSER_NAME、DURATION
	 */
	public HashMap<String, String> getId3Info(String devPath,String fileId) {
		HashMap<String, String> map = new HashMap<String, String>();
		Uri uri = getUri(devPath,"id3info");
				
		String selection = "file_id=?";
		String[] selectionArgs = { fileId };
		String[] projection = { FILE_ID, TITLE, ARTIST_ID, ARTIST_NAME,
				ALBUM_ID, ALBUM_NAME, GENRE_ID, GENRE_NAME, COMPOSER_ID,
				COMPOSER_NAME ,DURATION};

		Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, null);
		if(null != c){
			try {
				while(c.moveToNext()){
					
					map.put(FILE_ID, c.getString(0));
					map.put(TITLE, c.getString(1));
					
					map.put(ARTIST_ID, c.getString(2));
					map.put(ARTIST_NAME, c.getString(3));
					
					map.put(ALBUM_ID, c.getString(4));
					map.put(ALBUM_NAME, c.getString(5));
					
					map.put(GENRE_ID, c.getString(6));
					map.put(GENRE_NAME, c.getString(7));
					
					map.put(COMPOSER_ID, c.getString(8));
					map.put(COMPOSER_NAME, c.getString(9));
					map.put(DURATION, c.getString(10));
					
				}
				c.close();
			} catch (Exception e) {
				e.printStackTrace();
				c.close();
			}
		}
		return map;
	}
	
	/**
	 * 根据指定的id3信息的id获取相应的歌曲
	 * 
	 * devPath 设备路径
	 * 
	 * field id3信息的id 包括（artist_id，album_id，genre_id，composer_id）
	 * 
	 * Id 和field对应的id值
	 * 
	 * */
	public Cursor getSongById(String devPath,String field,String Id) {
		
		ArrayList<HashMap<String, String>> mList = new ArrayList<HashMap<String, String>>();
		Uri uri = getUri(devPath,"id3info");
		String selection = field+"=?";
		String[] selectionArgs = { Id };
		String order = "upper(trim(title_py))";
		return mContext.getContentResolver().query(uri, new String[]{_ID,AUDIO_ID,AUDIO_NAME,FAVORITE}, selection, selectionArgs, order);
	}
	/**
	 * 为指定的音频设置收藏
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name()
	 * @param id 音频文件的ID
	 * @param isFavorite 是否收藏
	 */
	public void setFavorite(String devPath, String id, boolean isFavorite){
		
		String whereClause = "id=?";
		String whereArgs[] = {id};
		Uri uri = getUri(devPath,"songs");
		
		ContentValues mValues = new ContentValues();
		if(isFavorite){
			mValues.put("favorite", "1");
		} else {
			mValues.put("favorite", "0");
		}
		
		mContext.getContentResolver().update(uri, mValues, whereClause, whereArgs);
	}
	
	//---------------------xuweifu------------------------
	/**
	 * 
	 * @param devPath 指定的设备，例如STORAGE_PORT.USB1.name()
	 * @param propertys 
	 * @param values
	 * @return
	 */
	public Cursor getSongList(String devPath,String[] propertys,Object[] values) {
		Uri uri = getUri(devPath,"songs");
		
		String id = "id";
		String name = "name";
		String path_id = "path_id";
		String path_name = "path_name";
		
		String order = "lower(trim("+name+"))";
		
		StringBuffer sb_selection = new StringBuffer();
		String[] arr = new String[values.length];

		if(propertys.length!=values.length){
			AdayoLog.logError(TAG, "getSongList error,propertys.length!=values.length!");
		}
		
		for(int i=0;i<propertys.length;i++){
			if(i==0){
				sb_selection.append(propertys[i]).append("=?");
			}else{
				sb_selection.append(" and ").append(propertys[i]).append("=?");
			}
			
			arr[i] =String.valueOf(values[i]);
		}
		
		Cursor c = mContext.getContentResolver().query(uri, new String[]{id,name,path_id,path_name}, sb_selection.toString(), arr, order);
		
		return c;
	}
	
	public Cursor getId3InfoList(String devPath,String[] propertys,Object[] values,String groupby,String orderBy) {
		Uri uri = getUri(devPath,"id3info");
		String _id = "_id";
		String file_id = "file_id";
		String title = "title";
		String artist_id = "artist_id";
		String artist_name = "artist_name";
		String album_id = "album_id";
		String album_name = "album_name";		
		String genre_id = "genre_id";
		String genre_name = "genre_name";
		String composer_id = "composer_id";
		String composer_name = "composer_name";
		String duration = "duration";
		String pic_name = "pic_name";
		
//		String selection = "file_id=?";
//		String[] selectionArgs = { fileId };
		
		String[] projection = { _id,file_id, title, artist_id, artist_name,
				album_id, album_name, genre_id, genre_name, composer_id,
				composer_name ,duration,pic_name};
		
		StringBuffer sb_selection = new StringBuffer();
		String[] arr = new String[values.length];

		if(propertys.length!=values.length){
			AdayoLog.logError(TAG, "getId3InfoList error,propertys.length!=values.length!");
		}
		
		for(int i=0;i<propertys.length;i++){
			if(i==0){
				sb_selection.append(propertys[i]).append("=?");
			}else{
				sb_selection.append(" and ").append(propertys[i]).append("=?");
			}
			
			arr[i] =String.valueOf(values[i]);
		}
		if(groupby!=null){
			if(sb_selection.length()<=0){
				sb_selection.append(" 1=1 "+groupby);
			}else{
				sb_selection.append(" "+groupby);
			}
			
		}
		if(orderBy==null){
			orderBy = "lower(title) asc";
		}
		Cursor c = mContext.getContentResolver().query(uri, projection, sb_selection.toString(), arr, orderBy);
		return c;
	}
	
	/**
	 * for video by xryu
	 */

	public Cursor getAllVideoPaths(String devPath){
		Cursor cursor = null;
		
		Uri uri = getUri(devPath,"videos");
		
		cursor = mContext.getContentResolver().query(uri, new String[]{VIDEO_ID,VIDEO_PATH_NAME,VIDEO_FILE_NAME,VIDEO_FILE_NAME_PY}, null, null, "lower(trim(name_py))");
		
		return cursor;
	}
	
	/**
	 * for photo by xryu
	 */

	public Cursor getAllPhotoPaths(String devPath){
		Cursor cursor = null;
		
		Uri uri = getUri(devPath,"photos");
		
		cursor = mContext.getContentResolver().query(uri, new String[]{PHOTO_ID,PHOTO_PATH_NAME,PHOTO_FILE_NAME,PHOTO_FILE_NAME_PY}, null, null, "lower(trim(name_py))");
		
		return cursor;
	}
	
	public Cursor getAllThumbnail(){
		Cursor cursor = null;
		
		Uri uri = getUri("thumbnails");
		
		cursor = mContext.getContentResolver().query(uri, new String[]{THUMBNAIL_FILE_ID, THUMBNAIL_PATH_NAME}, null, null,null);
		
		return cursor;
	}

	public Cursor getFreeStyleCursor(String devPath) {
		Log.d(TAG, TAG+"=========getFreeStyleCursor=========");
		String selection ="("+NEW+"= ?"+" or "+FAVORITE+"= ?)";
		String[] selectionArgs = new String[]{ String.valueOf(ON),String.valueOf(ON)};
		Uri uri = getUri(devPath,"songs");
		String order = "play_times desc";
		
		if(selection != null && selection.length()>0){
			selection += " and "+HIDE+"="+OFF;
		} else {
			selection = HIDE+"="+OFF;
		}
		
		return mContext.getContentResolver().query(uri, new String[]{_ID,AUDIO_ID,AUDIO_NAME,AUDIO_PATH_ID,AUDIO_PATH_NAME,NEW,FAVORITE}, selection, selectionArgs, order);
	}
	
	public Cursor getSongsByAritsId(String devPath,String fileId,String Id){
		Cursor cursor = null;
		
		Log.d(TAG, "[getSongsByAritsId]:" + devPath + " fileId " + fileId + " id " + Id);
		
		ArrayList<HashMap<String, String>> mList = new ArrayList<HashMap<String, String>>();
		Uri uri = getUri(devPath,"id3info");
		String selection = fileId+"=?";
		String[] selectionArgs = { Id };
		String order = "upper(trim(title_py))";
		
//		mContext.getContentResolver().
		
		return cursor;
	}
	
	public Cursor getArtistsSongs(String devPath) {
		Uri uri = getUri(devPath,"id3info");

		String[] projection = {FILE_ID,ARTIST_ID,ARTIST_NAME,TITLE};
		String selection = HIDE + "=" + OFF ;
		String order = "lower(trim(artist_name_py)),lower(trim(title_py))";
		
//		String id = "distinct "+ARTIST_ID+" as _id";
//		String[] projection = {id,ARTIST_NAME};
//		String selection = HIDE+"="+OFF;
//		String order = "lower(trim(artist_name_py))";
		
		return getList(uri,projection,selection, null,order);
	}
	
	public Cursor getAlbumsSongs(String devPath) {
		Uri uri = getUri(devPath,"id3info");
		
		
		String[] projection = {FILE_ID,ALBUM_ID,ALBUM_NAME,TITLE};
		String selection = HIDE + "=" + OFF ;
		String order = "lower(trim(album_name_py)),lower(trim(title_py))";
//		String id = "distinct "+ALBUM_ID+" as _id";
//		String[] projection = {id,ALBUM_NAME};
//		String selection = HIDE+"="+OFF;
//		String order = "lower(trim(album_name_py))";
		
		return getList(uri,projection,selection, null,order);
	}
	
	public boolean hasVideoFiles(){
		Uri uri = getUri("videos");
		Cursor cursor = null;
		try{
			cursor = mContext.getContentResolver().query(uri, new String[]{VIDEO_ID}, null, null, "lower(trim(name_py)) limit 1");
			if(cursor == null)
				return false;
			
			return cursor.getCount() != 0;
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		return false;
	}
	
	public Cursor getID3Songs(STORAGE_PORT storage){
		Uri uri = getUri(storage.name(),"id3info");
		
		
		String[] projection = { _ID, AUDIO_ID, TITLE,PLAY_TIMES,
				 FAVORITE, NEW, TOP, ALBUM_ID, ARTIST_ID,
				 };
		String selection = HIDE + "=" + OFF ;
		String order = "lower(trim(title_py))";
		
		return getList(uri,projection,selection, null,order);
	}
	
	public Cursor searchSongs(String searchText,String pinYingText){
		String selection;
		if(pinYingText.equals(searchText)){
			selection = " upper(" + AdayoMediaManager.TITLE_PY	+ ") like '%" + searchText + "%'" + " and "+HIDE+"="+OFF;
		}else {
			selection = " upper(" + AdayoMediaManager.TITLE	+ ") like '%" + searchText + "%'" + " and "+HIDE+"="+OFF;
		}
		Uri uri = getUri(STORAGE_PORT.STORAGE_ALL.name(),"id3info");
		String order = "top desc";		
		return mContext.getContentResolver().query(uri, new String[]{FILE_ID + " as _id",TITLE,FAVORITE}, selection, null, order);
	}
}
