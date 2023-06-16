package com.adayo.videoplayer.fragments;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import com.adayo.an6v.ui.TopBar;
import com.adayo.an6v.ui.TopBar.OnDeviceSelected;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.midware.constant.channelManagerDef.MEDIA_SOURCE_ID;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.VideoUtils;
import com.adayo.videoplayer.VideoMainActivity;
import com.adayo.videoplayer.core.VideoPlayController;
import com.adayo.videoplayer.interfaces.IVideoPlayStateChangedListener;
import com.adayo.videoplayer.interfaces.VideoPlayStateChangeListener;
import com.adayo.videoplayer.widget.GridList;
import com.adayo.videoplayer.widget.GridList.IGetThumbnail;

public class VideoListFragment extends Fragment {

    protected static final String TAG = VideoListFragment.class.getSimpleName();
    protected static final int MSG_UPDATE_LIST = 1;
    protected static final int MSG_UPDATE_LIST_ITEM_SELECTED = 2;
    public static final int MSG_STORAGE_SCANNING = 3;

    View mRootView;
    GridList mVideoList;

    String[] mTitles;
    int[] mThumbnailIds;
    Drawable[] mThumbnailDrawables;

    VideoPlayController mVideoPlayControler;
    private TextView mTvShowInfo;

    TopBar mTopBar;

    Handler mHandler = new Handler(AdayoVideoPlayerApplication.instance().getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_LIST:

                    mTopBar.updateDevices();
                    onDeviceSelected(mVideoPlayControler.getSelectedDevice());
                    break;
                case MSG_UPDATE_LIST_ITEM_SELECTED:

                    mVideoList.setCurrentPlayingIndex(msg.arg1);

                    break;

                case MSG_STORAGE_SCANNING:
                    storageScanning((STORAGE_PORT) msg.obj);
                    break;
                default:
                    break;
            }
        }

    };
    private IVideoPlayStateChangedListener mChangeListener = new PlayStateChagnedListener();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
    }

    // mVideoList = (ListTypeSelector)
    // mRootView.findViewById(R.id.video_list_id);
    //
    // mVideoList.addView(R.drawable.selector_list_type_favorite,R.string.favorite_text,
    // new OnClickListener() {
    //
    // @Override
    // public void onClick(View v) {
    //
    // }
    // });
    //

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Trace.d(TAG, "[onCreateView]");
        mRootView = inflater.inflate(R.layout.fragment_list, container, false);
        mVideoList = (GridList) mRootView.findViewById(R.id.video_list_id);
        mVideoList.setup(MEDIA_SOURCE_ID.VIDEO, new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mVideoPlayControler.isPreparing()) {
                    mVideoPlayControler.selectedVideo(position);
                    getMainActivity().showPlayFragment();
                }
            }
        }, new OnClickListener() {

            @Override
            public void onClick(View v) {
                getMainActivity().showPlayFragment();
                // if(mVideoPlayControler.isPaused())
                // mVideoPlayControler.play();
            }
        }, new IGetThumbnail() {

            @Override
            public Drawable getThumbnail(int fileId) {
                // return mVideoPlayControler.getThumbnail(fileId);
                return null;
            }

            @Override
            public Drawable getThumbnail(String fullPath) {
                if (mVideoPlayControler != null) {
                    return mVideoPlayControler.getThumbnailByFullPath(fullPath);
                } else {
                    return null;
                }
            }
        });
        mTopBar = (TopBar) mRootView.findViewById(R.id.topbar_id);
        mTvShowInfo = (TextView) mRootView.findViewById(R.id.tv_show_info_id);
        return mRootView;
    }

    protected VideoMainActivity getMainActivity() {
        return (VideoMainActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        Trace.d(TAG, "[onResume]:playing path " + mVideoPlayControler.getCurrentPlayingPath());
        initTopBar();
        mVideoPlayControler.registerVideoStateChangedListener(mChangeListener, false);
        mVideoPlayControler.startBrowser();
        mTopBar.obtainFoucs();
    }

    @Override
    public void onPause() {
        super.onPause();
        Trace.d(TAG, "[onPause]");
    }

    @Override
    public void onStop() {
        super.onStop();
        mVideoPlayControler.unregisterVideoStateChangedListener(mChangeListener);
        Trace.d(TAG, "[onStop]");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Trace.d(TAG, "[onDestroy]");
        mVideoPlayControler = null;
    }

    public void initTopBar() {

        mTopBar.setTextInfo("");

        // mTvTitleDeviceInfo =
        // mTopBar.addTextInfo(TopBar.CENTER_HORIZONTAL_IN_TOPBAR, "");

        mTopBar.setTitle(getString(R.string.video_list_title));
        mTopBar.setTitleMode(TopBar.MODE_NORMAL);

        mTopBar.addDevicesSelector(new OnDeviceSelected() {
            @Override
            public void onDeviceSelected(STORAGE_PORT storage) {
                VideoListFragment.this.onDeviceSelected(storage);
            }
        });
        STORAGE_PORT storage = mVideoPlayControler.getSelectedDevice();

        onDeviceSelected(storage);
        mTopBar.showDeviceTextInTitle(storage);

    }

    public void onDeviceSelected(STORAGE_PORT storage) {
        mVideoPlayControler.onStorageSelected(storage);
        mTopBar.setTitle(getString(R.string.video_list_title) + "(" + VideoUtils.toDeviceText(storage) + ")");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mTopBar.obtainFoucs();
            mTopBar.setTitle(getString(R.string.video_list_title) + "("
                    + VideoUtils.toDeviceText(mVideoPlayControler.getStorage()) + ")");
        }
    }

    public void showTopBar() {
        mTopBar.setVisibility(View.VISIBLE);
    }

    public void hideTopBar() {
        mTopBar.setVisibility(View.GONE);
    }

    public void setTopBarTouchListener(OnTouchListener onTouchListener) {
        mTopBar.setOnTouchListener(onTouchListener);
    }

    public void playVideoChanged(String playingPath) {
    }

    class PlayStateChagnedListener extends VideoPlayStateChangeListener {

        @Override
        public void playVideoChanged(String playingPath, int duration) {
            VideoListFragment.this.playVideoChanged(playingPath);
        }

        @Override
        public void retVideoList(String[] titles, int[] fileIds, String[] fullPaths, STORAGE_PORT storage) {
            Trace.i(TAG, "[retVideoList]:titles length " + (titles == null ? "null" : titles.length) + " storage "
                    + storage.name());
            // mVideoList.updateList(titles,fileIds,fullPaths, false,null,null,
            // GridList.HSV_MODE);
            // mTopBar.showDeviceTextInTitle(storage);
            DoChangedRunnable runnable = new DoChangedRunnable();
            runnable.mTitles = titles;
            runnable.mFileIds = fileIds;
            runnable.mFullPaths = fullPaths;
            runnable.mStorage = storage;
            mHandler.post(runnable);
        }

        @Override
        public void onListSelectedChanged(int position) {
            mHandler.obtainMessage(MSG_UPDATE_LIST_ITEM_SELECTED, position, 0).sendToTarget();
        }

        @Override
        public void storageScanning(STORAGE_PORT storage) {
            Trace.d(TAG, "[storageScanning]:storage  " + storage);
            // mHandler.obtainMessage(MSG_STORAGE_SCANNING,storage).sendToTarget();
        }

        @Override
        public void storageUnmounted() {
            super.storageUnmounted();
            Trace.d(TAG, "[storageUnmounted]");
            mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
        }

    };

    public void storageScanning(STORAGE_PORT storage) {
        mTvShowInfo.setText(R.string.text_loading);
    }

    public void devicesChanged(STORAGE_PORT storage) {
        mTopBar.updateDevices();
    }

    class DoChangedRunnable implements Runnable {

        public String[] mTitles;
        public String[] mFullPaths;
        public int[] mFileIds;
        public STORAGE_PORT mStorage;

        @Override
        public void run() {
            mVideoList.updateList(mTitles, mFileIds, mFullPaths, false, null, null, GridList.HSV_MODE);
            mTopBar.showDeviceTextInTitle(mStorage);
            mTopBar.updateDevices();
            if (mTitles == null || mTitles.length == 0) {
                mTvShowInfo.setVisibility(View.VISIBLE);
                if (mVideoPlayControler.isInScanState(mStorage)) {
                    Trace.d(TAG, "storage " + mStorage + " is in scan state");
                    mTvShowInfo.setText(R.string.text_loading);
                } else {
                    Trace.d(TAG, "storage " + mStorage + " is in scan state");
                    mTvShowInfo.setText(R.string.text_list_empty);
                }
            } else {
                mTvShowInfo.setVisibility(View.GONE);
            }
        }
    }
}
