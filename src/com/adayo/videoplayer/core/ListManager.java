package com.adayo.videoplayer.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import android.util.SparseArray;

import com.adayo.an6v.ui.Utils;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.db.VideoBean;

public class ListManager {
    private static final String TAG = "myvideo ListManager";
    // storage --> video paths
    SparseArray<List<VideoBean>> mVideoList = new SparseArray<List<VideoBean>>();
    TreeSet<VideoBean> mAllVideos;
    private AdayoVideoPlayerApplication mContext;

    public ListManager() {
        mContext = AdayoVideoPlayerApplication.instance();

        for (STORAGE_PORT storage : STORAGE_PORT.values()) {
            // if(storage.equals(STORAGE_PORT.STORAGE_ALL))
            // continue;
            mVideoList.put(storage.ordinal(), new ArrayList<VideoBean>());
        }

        STORAGE_PORT[] mountedStorages = Utils.getMountedStorage();
        if (mountedStorages != null){
            for (STORAGE_PORT storage : mountedStorages) {
                mVideoList.put(storage.ordinal(), mContext.queryVideoBeansList(storage));
            }
        }
        mVideoList.put(STORAGE_PORT.STORAGE_ALL.ordinal(), mContext.queryVideoBeansList(STORAGE_PORT.STORAGE_ALL));

    }

    public void updateAfterScanFileEnd(STORAGE_PORT storage) {
        List<VideoBean> list = mContext.queryVideoBeansList(storage);
        Trace.d(TAG, "[updateAfterScanFileEnd]: mPlayList.size()==" + list.size());
        mVideoList.put(storage.ordinal(), list);
        list = mContext.queryVideoBeansList(STORAGE_PORT.STORAGE_ALL);
        mVideoList.put(STORAGE_PORT.STORAGE_ALL.ordinal(), list);
    }

    public List<VideoBean> getVideoList(STORAGE_PORT storage) {
        Trace.d(TAG, "[getVideoList]:storage:" + storage);
        Trace.d(TAG, "[getVideoList]:mVideoList==null? " + mVideoList);
        return new ArrayList<VideoBean>(mVideoList.get(storage.ordinal()));
    }

    public void storageRemoved(STORAGE_PORT storage) {
        mVideoList.get(storage.ordinal()).clear();
        List<VideoBean> list = mVideoList.get(STORAGE_PORT.STORAGE_ALL.ordinal());
        Trace.d(TAG, "[storageRemoved]:before remove storage all list size " + list.size());
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            VideoBean videoBean = (VideoBean) iterator.next();
            STORAGE_PORT storage1 = Utils.getWhichStorageIn(videoBean.mPath);
            if (storage1.equals(storage)){
                iterator.remove();
            }
        }
        Trace.d(TAG, "[storageRemoved]:after remove storage all list size " + list.size());
    }

}
