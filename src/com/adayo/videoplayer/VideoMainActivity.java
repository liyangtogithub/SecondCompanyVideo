package com.adayo.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.midware.constant.CanboxConstantsDef;
import com.adayo.videoplayer.core.VideoPlayController;
import com.adayo.videoplayer.fragments.VideoListFragment;
import com.adayo.videoplayer.fragments.VideoPlayFragment;
import com.adayo.videoplayer.fragments.VideoShowInfoFragment;
import com.adayo.videoplayer.interfaces.IVideoPlayStateChangedListener;
import com.adayo.videoplayer.interfaces.VideoPlayStateChangeListener;

public class VideoMainActivity extends Activity {

    private static final String TAG = "myvideo VideoMainActivity";
    private static final int MSG_FILE_SCAN_END = 0;
    public static final int MSG_PLAY_STATE_CHANGED = 2;
    public static final int MSG_NO_VIDEO_FILE = 3;
    public static final int MSG_NO_DEVICE = 4;
    public static final int MSG_BEGIN_NEW_PLAYING = 5;
    public static final int MSG_SURFACE_VISIBLE = 6;
    private static final int MSG_INIT_DONE = 7;
    private VideoPlayFragment mPlayFragment;
    private VideoListFragment mListFragment;
    private SurfaceView mSurfaceView;
    // private View mContainerView;
    private VideoPlayController mVideoPlayControler;
    private Fragment mCurrentFragment;
    // private ImageView mIvActivityBg;
    private boolean mIsPause = false;

    private IVideoPlayStateChangedListener mVideoPlayStateChangeListener = new PlayStateChagnedListener();

    Handler mHandler = new Handler(AdayoVideoPlayerApplication.instance().getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_SCAN_END:

                    break;
                case MSG_PLAY_STATE_CHANGED:
                    switch (msg.arg1) {
                        case Constants.PLAYING:
                            // mSurfaceView.setBackground(null);
                            // if(mSurfaceView.getVisibility() == View.VISIBLE)
                            // mIvActivityBg.setVisibility(View.GONE);
                            // mContainerView.setBackgroundResource(android.R.color.transparent);

                            break;
                        case Constants.PAUSE:
                            // mContainerView.setBackgroundResource(android.R.color.transparent);
                            // mSurfaceView.setBackground(null);
                            break;
                        case Constants.STOP:
                            // mSurfaceView.setBackgroundResource(R.drawable.activity_bg);
                        default:
                            break;
                    }
                    break;
                case MSG_NO_VIDEO_FILE:
                    showNoVideoFileFragment();
                    break;
                case MSG_NO_DEVICE:
                    showNoDeviceFragment();
                    break;
                case MSG_BEGIN_NEW_PLAYING:
                    if (!mIsPause) {
                        VideoMainActivity.this.showPlayFragment();
                        mVideoPlayControler.canShowToast(true);
                    }
                    break;
                case MSG_SURFACE_VISIBLE:
                    mSurfaceView.setVisibility(View.VISIBLE);
                    break;
                case MSG_INIT_DONE:
                    if (mPlayFragment==null) {
                        initFragment();
                    }
                    if (mVideoPlayControler == null) {
                        mVideoPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
                    }
                    mVideoPlayControler.registerVideoStateChangedListener(mVideoPlayStateChangeListener, true);
                    mVideoPlayControler.setup(mSurfaceView);
                    mSurfaceView.setVisibility(View.INVISIBLE);
                    mHandler.sendEmptyMessageDelayed(MSG_SURFACE_VISIBLE, 500);
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Trace.d(TAG, "[onCreate]:" + this);
        setContentView(R.layout.activity_video_main);

        initSufaceView();
        AdayoVideoPlayerApplication.instance().registerFileStateChanged(mFilesStateChanged);
        mVideoPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();

        if (!AdayoVideoPlayerApplication.instance().isInitDone()) {
            Trace.d(TAG, "[onCreate]:init isn't done");
            showLoadingFragment();
        } else {
            initFragment();
            showPlayFragment();

        }
    }

    private void initSufaceView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.sf_video_play_id);
        mSurfaceView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mCurrentFragment == null){
                    return false;
                }
                Trace.d(TAG, "[onTouch]" + mCurrentFragment.getClass().getSimpleName());
                if (mCurrentFragment instanceof VideoPlayFragment){
                    mPlayFragment.onTouch(event);
                } 
                return false;
            }
        });
    }

    private void initFragment() {
        mPlayFragment = new VideoPlayFragment();
        mListFragment = new VideoListFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container_id, mPlayFragment);
        ft.add(R.id.fragment_container_id, mListFragment);
        ft.hide(mListFragment);
        ft.hide(mPlayFragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onResume() {
        Trace.d(TAG, "[onResume]:" + this);
        super.onResume();
        AdayoVideoPlayerApplication.instance().addActivity(this);
        getWindow().getDecorView().setBackgroundResource(R.drawable.activity_bg);
        if (AdayoVideoPlayerApplication.instance().isInitDone()) {
            if (mPlayFragment==null) {
                initFragment();
            }
            showPlayFragment();
            mVideoPlayControler.registerVideoStateChangedListener(mVideoPlayStateChangeListener, true);
            if (mSurfaceView == null) {
                initSufaceView();
            }
            mVideoPlayControler.setup(mSurfaceView);
        }
        mIsPause = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Trace.d(TAG, "[onPause]:" + this);
        mIsPause = true;
        mVideoPlayControler.canShowToast(false);
        AdayoVideoPlayerApplication.instance().addActivity(this);
    }

    @Override
    protected void onStop() {
        Trace.d(TAG, "[onStop]:" + this);
        super.onStop();
        AdayoVideoPlayerApplication.instance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Trace.d(TAG, "[onDestroy]:" + this);
        mVideoPlayControler.unregisterVideoStateChangedListener(mVideoPlayStateChangeListener);
        AdayoVideoPlayerApplication.instance().unregsterFileStateChanged(mFilesStateChanged);
        mVideoPlayControler.surfaceHadBeenDestroyed();
        AdayoVideoPlayerApplication.instance().removeActivity();
        mCurrentFragment = null;
        mPlayFragment = null;
        mListFragment = null;
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment instanceof VideoListFragment) {
            showPlayFragment();
        } else {
            super.onBackPressed();
        }
    }

    public void goHome() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    class PlayStateChagnedListener extends VideoPlayStateChangeListener {

        @Override
        public void onPlayStateChanged(int playState) {
            Trace.d(TAG, "[playStateChanged]:" + playState);

            // if(mShowInfoFragment != null){
            // showPlayFragment();
            // }

            mHandler.obtainMessage(MSG_PLAY_STATE_CHANGED, playState, 0).sendToTarget();
        }

        @Override
        public void noVideoFileToPlay() {
            Trace.d(TAG, "[noVideoFileToPlay]");
            mHandler.sendEmptyMessage(MSG_NO_VIDEO_FILE);
        }

        @Override
        public void noDevices() {
            Trace.d(TAG, "[noDevices]");
            super.noDevices();
            mHandler.sendEmptyMessage(MSG_NO_DEVICE);
        }

        @Override
        public void beginNewPlaying() {
            Trace.d(TAG, "[beginNewPlaying]");
            super.beginNewPlaying();
            mHandler.sendEmptyMessage(MSG_BEGIN_NEW_PLAYING);

        }

        @Override
        public void onLoading(final STORAGE_PORT storage) {
            super.onLoading(storage);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VideoMainActivity.this.onLoading(storage);
                }
            });
        }

    };

    private VideoShowInfoFragment mShowInfoFragment;

    public void nothingToPlay() {
        // mIvActivityBg.setVisibility(View.VISIBLE);
    }

    protected void onLoading(STORAGE_PORT storage) {
        showLoadingFragment();
    }

    @SuppressLint("NewApi")
    public void showPlayFragment() {
        Trace.d(TAG, "[showPlayFragment]," + " mCurrentFragment " + "is play fragment "
                + (mCurrentFragment instanceof VideoPlayFragment) + " isDestroyed " + isDestroyed() + " isFinishing "
                + isFinishing());

        if (isDestroyed() || isFinishing()) {
            return;
        }
        if (!mIsPause) {
            mVideoPlayControler.canShowToast(true);
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mShowInfoFragment != null) {
            ft.remove(mShowInfoFragment);
            mShowInfoFragment = null;
        }

        if (!(mCurrentFragment instanceof VideoPlayFragment)) {
            if (mListFragment!= null) {
                ft.hide(mListFragment);
            }
            if (mPlayFragment!= null) {
                ft.show(mPlayFragment);
            }
            mCurrentFragment = mPlayFragment;
        }
        ft.commitAllowingStateLoss();
    }

    public void showListFragment() {
        mVideoPlayControler.canShowToast(false);
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mShowInfoFragment != null) {
            ft.remove(mShowInfoFragment);
            mShowInfoFragment = null;
        }

        if (!(mCurrentFragment instanceof VideoListFragment)) {
            if (mPlayFragment!= null) {
                ft.hide(mPlayFragment);
            }
            if (mListFragment!= null) {
                ft.show(mListFragment);
            }
            mCurrentFragment = mListFragment;
        }

        ft.commitAllowingStateLoss();
    }

    protected void showNoVideoFileFragment() {
        mCurrentFragment = null;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mShowInfoFragment != null) {
            ft.remove(mShowInfoFragment);
        }
        mShowInfoFragment = VideoShowInfoFragment.newInstance(getString(R.string.text_no_video_file), false);
        ft.add(R.id.fragment_container_id, mShowInfoFragment);
        if (mCurrentFragment != null) {
            ft.hide(mCurrentFragment);
        }
        ft.show(mShowInfoFragment);
        ft.commitAllowingStateLoss();
    }

    public void showNoDeviceFragment() {
        mCurrentFragment = null;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mShowInfoFragment != null) {
            ft.remove(mShowInfoFragment);
        }
        mShowInfoFragment = VideoShowInfoFragment.newInstance(getString(R.string.text_no_device), false);
        ft.add(R.id.fragment_container_id, mShowInfoFragment);
        if (mCurrentFragment != null) {
            ft.hide(mCurrentFragment);
        }
        ft.show(mShowInfoFragment);
        ft.commitAllowingStateLoss();
    }

    public void showLoadingFragment() {
        mVideoPlayControler.reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_READING);
        mCurrentFragment = null;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mShowInfoFragment != null) {
            ft.remove(mShowInfoFragment);
        }
        mShowInfoFragment = VideoShowInfoFragment.newInstance(getString(R.string.text_loading), true);
        ft.add(R.id.fragment_container_id, mShowInfoFragment);
        if (mCurrentFragment != null) {
            ft.hide(mCurrentFragment);
        }
        ft.show(mShowInfoFragment);
        ft.commitAllowingStateLoss();
    }

    FilesStateChanged mFilesStateChanged = new FilesStateChanged() {

        @Override
        public void mediaStorageUnmounted(STORAGE_PORT storage) {
            Trace.d(TAG, "[mediaStorageUnmounted]:" + storage.name());
            if (mCurrentFragment instanceof ListFragment) {
                mListFragment.devicesChanged(storage);
            }
        }

        @Override
        public void mediaStorageMounted(STORAGE_PORT storage) {
            if (mCurrentFragment instanceof ListFragment) {
                mListFragment.devicesChanged(storage);
            }
        }

        @Override
        public void fileScanStart(STORAGE_PORT storage) {

        }

        @Override
        public void fileScanID3End(STORAGE_PORT storage) {

        }

        @Override
        public void fileScanEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileScanEnd]" + storage.name());
        }

        @Override
        public void fileSaveEnd(STORAGE_PORT storage) {

        }

        @Override
        public void fileParseThumbnailEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileParseThumbnailEnd]:storage " + storage);
            if (mListFragment != null && mCurrentFragment != null && mCurrentFragment instanceof VideoListFragment) {
                mListFragment.onDeviceSelected(storage);
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public VideoPlayController getVideoPlayControler() {
        return mVideoPlayControler;
    }

    public void onInitDone() {
        Trace.d(TAG, "[onInitDone]");
        mHandler.sendEmptyMessage(MSG_INIT_DONE);
    }

    public boolean isPlayFragment() {
        if (mCurrentFragment == null) {
            return false;
        }
        return mCurrentFragment instanceof VideoPlayFragment;
    }

}
