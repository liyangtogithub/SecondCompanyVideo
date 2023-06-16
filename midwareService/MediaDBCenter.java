package com.adayo.midware.mpeg.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.adayo.midwareproxy.mpeg.util.MediaFileBean;
import com.adayo.midwareproxy.utils.AdayoLog;

/**
 * ��ݿ������
 * 
 * @author gfxie
 * 
 */
public class MediaDBCenter {

	public static final String DataBase_Name = "media.db";

	public static final String Table_Name = "mediafiles";
	public static final int DataBase_Version = 1;

	private Context context;

	public MediaDBCenter(Context context) {
		this.context = context;
	}

	private class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DataBase_Name, null, DataBase_Version);
			// TODO Auto-generated constructor stub
		}

		public DatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, DataBase_Name, factory, DataBase_Version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			String sql = "create table " + Table_Name + " ("
					+ MediaFileColumn.Index + " integer primary key,"
					+ MediaFileColumn.ParentId + " integer, "
					+ MediaFileColumn.Clips + " integer,"
					+ MediaFileColumn.isFile + " integer,"
					+ MediaFileColumn.FileName + " text,"
					+ MediaFileColumn.Types + " text default ',');";

			db.execSQL(sql);

			sql = "create view audio as select * from mediafiles "
					+ "where clips <> 0 and filename is not null and (clips in(1,2,3,4) or (types like '%,1,%' and clips = 15))";
			db.execSQL(sql);
			
			sql = "create view video as select * from mediafiles "
				+ "where  clips <> 0 and filename is not null and (clips in(6,7,8) or (types like '%,2,%' and clips = 15))";
			db.execSQL(sql);
			
			sql = "create view photo as select * from mediafiles "
				+ "where  clips <> 0 and filename is not null and (clips = 5 or (types like '%,3,%' and clips = 15))";
			db.execSQL(sql);
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("drop table if exists mediafiles");
			db.execSQL("drop view if exists audio");
			db.execSQL("drop view if exists video");
			db.execSQL("drop view if exists photo");
			// db.execSQL("drop table if exists tb_musiclist");
			// db.execSQL("drop table if exists tb_videolist");
			// db.execSQL("drop table if exists tb_photolist");
			onCreate(db);
		}

	}

	private DatabaseHelper dbhelper;
	private SQLiteDatabase db;

	public SQLiteDatabase getWDB() {

		if (null == dbhelper)
			dbhelper = new DatabaseHelper(this.context, this.DataBase_Name,
					null, this.DataBase_Version);
		db = dbhelper.getWritableDatabase();

		return db;
	}

	public SQLiteDatabase getRDB() {
		// if(null != db && db.isOpen())
		// db.close();
		if (null == dbhelper)
			dbhelper = new DatabaseHelper(this.context, this.DataBase_Name,
					null, this.DataBase_Version);
		db = dbhelper.getReadableDatabase();
		return db;
	}

	// ִ��SQL���
	public void excute(String sql) {

		db = getWDB();
		db.execSQL(sql);

		// Log.d("excuteSql=", sql);
	}

	// �������
	public void insert(ContentValues values) {
		db = getWDB();
		db.insert("mediafiles", null, values);

	}

	// �������
	public void batchInsert(List<MediaFileBean> list) {

		// MediaFiles file = null;

		db = getWDB();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			if (list.size() > 0) {
				for (MediaFileBean file : list) {

					values.put(MediaFileColumn.Index, file.getId());
					values.put(MediaFileColumn.ParentId, file.getParentId());
					values.put(MediaFileColumn.Clips, file.getClips());
					values.put(MediaFileColumn.isFile, file.isFile);
					db.insert("mediafiles", null, values);
				}
			}

			db.setTransactionSuccessful();

		} finally {
			db.endTransaction();
		}

	}

	public void setTransaction() {
		db.beginTransaction();
	}

	public void commitAll() {
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	public void rollBackAll() {
		db.endTransaction();
	}

	// �޸�
	public void update(String filename, int id) {
		db = getWDB();
		ContentValues values = new ContentValues();
		values.put(MediaFileColumn.FileName, filename);

		String whereClause = MediaFileColumn.Index + "=?";

		String whereArgs[] = { String.valueOf(id) };

		db.update(this.Table_Name, values, whereClause, whereArgs);

		//Cursor c = db.query("audio", null, null, null, null, null, null);  
		//AdayoLog.e("update", id+"-----update    ="+c.getCount());
	}

	// �޸�
	public void batchUpdate(ArrayList<String> list) {
		db = getWDB();
		db.beginTransaction();
		try {

			if (list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					String sql = list.get(i);
					// Log.d("print", sql);
					db.execSQL(sql);
				}
			}

			db.setTransactionSuccessful();

		} finally {
			db.endTransaction();
		}

	}

	// ��ѯ
	public Cursor queryWhere(String where) {
		db = getRDB();

		StringBuffer sql = new StringBuffer("select * from mediafiles ");
		if (null != where && where.length() > 0) {
			sql.append(where);
		}
		// Cursor cursor = db.query(CommField.Tabale_Name, cols, null, null,
		// null, null, "create_time desc");
		Cursor cursor = db.rawQuery(sql.toString(), null);
		// cursor.close();

		return cursor;
	}

	// ��ѯ
	public Cursor querySql(String sql) {
		db = getRDB();
		Cursor cursor = db.rawQuery(sql, null);

		// Log.d("querySql=", sql);

		// cursor.close();
		return cursor;
	}

	// ���ص����������
	public int IntSelect(String sql) {
		int ret = 0;
		db = getRDB();

		Cursor cursor = db.rawQuery(sql.toString(), null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			ret = cursor.getInt(0);
		}
		// Log.d("IntSelect=", ret + "  |: " + sql);
		cursor.close();

		return ret;
	}

	// ���ص����ַ����
	public String StrSelect(String sql) {
		String ret = "";
		db = getRDB();
		Cursor cursor = db.rawQuery(sql.toString(), null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			ret = cursor.getString(0);
		}

		cursor.close();

		// Log.d("StrSelect=", ret + "  |: " + sql);
		return ret;

	}

	// ɾ��
	public void delete(String where) {
		db = getWDB();
		// Log.e("deleteTable", " begin: " +Calendar.getInstance().getTime());
		StringBuffer sql = new StringBuffer("delete from " + this.Table_Name);
		if (null != where && where.length() > 0) {
			sql.append(where);
		}
		// Log.d("deleteSql=", sql.toString());
		db.execSQL(sql.toString());
		// Log.e("deleteTable", " begin: " + Calendar.getInstance().getTime());

	}

	public void deleteAllTables() {
		db = getWDB();
		db.execSQL("delete from mediafiles");
		
		db.close();
	}

	public void close() {
		dbhelper.close();
	}
}
