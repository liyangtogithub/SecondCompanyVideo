
package com.adayo.midware.mpeg.db;

import android.database.Cursor;

import com.adayo.midware.constant.MpegConstantsDef.DISC_ROM_MEDIA_TYPE;
import com.adayo.midwareproxy.utils.AdayoLog;

public class MediaDBUtil {
    private MediaDBCenter dbAdapter;

    public void setDbAdapter(MediaDBCenter dbAdapter) {
        this.dbAdapter = dbAdapter;
    }

    public MediaDBUtil(MediaDBCenter dbAdapter) {
        super();
        this.dbAdapter = dbAdapter;
    }

    public void deleteMedia(){
        dbAdapter.delete(null);
    }
    
    public void deleteAllTable(){
        dbAdapter.deleteAllTables();
    }
    
    public String getFileName(int id) {
        return dbAdapter.StrSelect("select filename from mediafiles where _id=" + id);
    }

    public int getRomFilesCount(int mediaType, int folderIndex) {
        String sql = "select count(1) from mediafiles where clips <> 0 and parentid=" + folderIndex;
        if (mediaType == 0) {
            sql += " and (clips in(1,2,3,4) or (types like '%,1,%' and clips = 15))";
        } else if (mediaType == 1) {
            sql += " and (clips in(6,7,8) or (types like '%,2,%' and clips = 15))";
        } else if (mediaType == 2) {
            sql += " and (clips = 5 or (types like '%,3,%' and clips = 15))";
        }

        return dbAdapter.IntSelect(sql);
    }

    public int getCurrentFolderCount(int playPId, DISC_ROM_MEDIA_TYPE type) {
        String andWhere = "";

        if (type == DISC_ROM_MEDIA_TYPE.AUDIO) {
            andWhere = " and clips in(1,2,3,4) ";
        } else if (type == DISC_ROM_MEDIA_TYPE.VIDEO) {
            andWhere = " and clips in(6,7,8)  ";
        } else if (type == DISC_ROM_MEDIA_TYPE.PHOTO) {
            andWhere = " and clips = 5 ";
        }

        return dbAdapter.IntSelect("select count(1) from mediafiles "
                + " where isfile = 1 and parentid =" + playPId + andWhere);
    }

    public int getCurrentFileTotal(int playPId, DISC_ROM_MEDIA_TYPE type) {
        String andWhere = "";

        if (type == DISC_ROM_MEDIA_TYPE.AUDIO) {
            andWhere = " and clips in(1,2,3,4) ";
        } else if (type == DISC_ROM_MEDIA_TYPE.VIDEO) {
            andWhere = " and clips in(6,7,8)  ";
        } else if (type == DISC_ROM_MEDIA_TYPE.PHOTO) {
            andWhere = " and clips = 5 ";
        }

        return dbAdapter.IntSelect("select count(1) from mediafiles "
                + " where isfile = 2 and parentid =" + playPId + andWhere);
    }

    public int getCurrentPosition(int playPId, int playedId, DISC_ROM_MEDIA_TYPE type) {
        String andWhere = "";

        if (type == DISC_ROM_MEDIA_TYPE.AUDIO) {
            andWhere = " and clips in(1,2,3,4) ";
        } else if (type == DISC_ROM_MEDIA_TYPE.VIDEO) {
            andWhere = " and clips in(6,7,8)  ";
        } else if (type == DISC_ROM_MEDIA_TYPE.PHOTO) {
            andWhere = " and clips = 5 ";
        }

        return dbAdapter.IntSelect("select count(1) from mediafiles "
                + " where isfile = 2 and parentid =" + playPId
                + " and _id <=" + playedId + andWhere);
    }
    
    public int getFileIndex(int playPId, int position, DISC_ROM_MEDIA_TYPE type) {
    	String andWhere = "";
    	
    	if (type == DISC_ROM_MEDIA_TYPE.AUDIO) {
    		andWhere = " and clips in(1,2,3,4) ";
    	} else if (type == DISC_ROM_MEDIA_TYPE.VIDEO) {
    		andWhere = " and clips in(6,7,8)  ";
    	} else if (type == DISC_ROM_MEDIA_TYPE.PHOTO) {
    		andWhere = " and clips = 5 ";
    	}

    	return dbAdapter.IntSelect("select _id from mediafiles "
    			+ " where isfile = 2 and parentid =" + playPId
    			+ andWhere
    			+ " limit "+position+",1");
    }
    
    public boolean updateTypes() {

        String sql = "update mediafiles set types = types  ||  '1,'"
                + " where _id in (select distinct parentid from mediafiles where  clips in(1,2,3,4)) and isfile = 1";
        dbAdapter.excute(sql);

        sql = "update mediafiles set types = types  ||  '2,'"
                + " where _id in (select distinct parentid from mediafiles where  clips in(6,7,8)) and isfile = 1";
        dbAdapter.excute(sql);

        sql = "update mediafiles set types = types  ||  '3,'"
                + " where _id in (select distinct parentid from mediafiles where  clips = 5) and isfile = 1";
        dbAdapter.excute(sql);

        sql = "select distinct parentid from mediafiles where _id <>2000 and isfile=1 order by _id desc";

        Cursor c = null;

        try {
            c = dbAdapter.querySql(sql);

            String types = "";
            String subTypes;
            String[] tmpTypes;
            while (c.moveToNext()) {
                subTypes = ",";
                sql = "select types from mediafiles where isfile = 1 and types <> ',' and parentid = "
                        + c.getInt(0)
                        + " order by types ";
                Cursor curType = dbAdapter.querySql(sql);
                while (curType.moveToNext()) {
                    String temp = curType.getString(0).substring(1);

                    if (!subTypes.contains(temp))
                        subTypes += temp;
                }

                curType.close();

                sql = "select types from mediafiles where isfile = 1 and types <> ',' and _id = "
                        + c.getInt(0);

                types = dbAdapter.StrSelect(sql);
                if (types != null && types.length() > 1) {
                    types = types.substring(1);

                    tmpTypes = types.split(",");
                    if (tmpTypes.length > 0)
                        for (String s : tmpTypes) {
                            subTypes = subTypes.replaceAll("," + s, "");
                        }

                }

                if (subTypes != null && subTypes.length() > 1) {
                    sql = "update mediafiles set types = types || '" + subTypes.substring(1)
                            + "'  where _id = "
                            + c.getInt(0);
                    dbAdapter.excute(sql);
                }
            }
        } catch (Exception e) {            
            e.printStackTrace();
            return false;
        } finally {
            if (null != c) {
                c.close();
            }
        }
        
        return true;
    }
    
    public int getMediaFileCount(){
        
        return dbAdapter.IntSelect("select count(1) from mediafiles");
    }
    
    public Cursor getNoNameIDs(DISC_ROM_MEDIA_TYPE type, int pId, int limit){

        StringBuilder where = new StringBuilder(
                "select _id from mediafiles where parentid<>-1 and filename is null and clips <> 0");
        
        if (type == DISC_ROM_MEDIA_TYPE.AUDIO) {
            where.append(" and clips in (1,2,3,4,15)");
        } else if (type == DISC_ROM_MEDIA_TYPE.VIDEO) {
            where.append(" and clips in (6,7,8,15)");
        } else if(type == DISC_ROM_MEDIA_TYPE.PHOTO) {
            where.append(" and clips in (5,15)");
        }
        
        if(pId>0){
        	where.append(" and parentid ="+pId);
        }
        
        where.append(" order by parentid,_id");

        if(limit>0){
        	where.append("  limit 0,"+limit);
        }
        
        return dbAdapter.querySql(where.toString());
    }
}
