package com.adayo.videoplayer.db;

public class VideoBean {
    public String mPath;
    public String mName; 
    public String mNamePy;
    public int mFileId;

    public VideoBean() {
        mName = mNamePy = mPath = "";
        mFileId = -1;
    }
}
