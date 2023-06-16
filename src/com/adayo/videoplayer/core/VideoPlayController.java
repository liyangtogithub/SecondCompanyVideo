package com.adayo.videoplayer.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;
import android.app.Service;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaPlayer.TrackInfo;
import android.media.TimedText;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import com.adayo.an6v.ui.Utils;
import com.adayo.mediaScanner.AdayoMediaManager;
import com.adayo.mediaScanner.AdayoMediaScanner;
import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.MediaObjectQueryInterface;
import com.adayo.mediaScanner.MediaScannerInterface.SCANNING_STATE;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.ScannerManager;
import com.adayo.midware.constant.CanboxConstantsDef;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.channelManagerDef.MEDIA_SOURCE_ID;
import com.adayo.midware.constant.channelManagerDef.SEAT_TYPE;
import com.adayo.midwareproxy.canbox.CanboxManager;
import com.adayo.midwareproxy.setting.SettingManager;
import com.adayo.midwareproxy.sourceswitch.SourceSwitchManager;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.Constants.RepeateMode;
import com.adayo.videoplayer.Constants.ShuffleMode;
import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.VideoUtils;
import com.adayo.videoplayer.db.VideoBean;
import com.adayo.videoplayer.interfaces.IVideoPlayStateChangedListener;
import com.adayo.videoplayer.widget.VideoToast;
import com.autochips.media.AtcMediaPlayer;

public class VideoPlayController {

    public static final int IDLE = 0;
    public static final int INIT = 1;
    public static final int PREPARING = 2;
    public static final int PREPARED = 3;
    public static final int STARTED = 4;
    public static final int PAUSED = 5;
    public static final int STOPPED = 6;
    public static final int COMPLETED = 7;
    public static final int ERROR = 8;
    public static final int RELEASED = 9;

    public static final String TAG = "myvideo VideoPlayController";

    private static final int FRACTOR = 314159267;
    private static final float RANDOM = 0.5f;
    private static final int MSG_ON_ERROE_NEXT = 1;
    private static final int MSG_FF_PLAY = 2;
    private static final int MSG_NOTHING_PLAY = 3;
    private static final int MSG_RET_VIDEO_LIST = 4;
    public static final int MSG_UPDATE_FINISH = 5;
    private static final int MSG_CLOSE_SOURCE = 6;
    private static final int MSG_INIT_AGAIN = 7;
    private static final int MSG_STORAGE_UNMOUNT = 8;
    private static final int MSG_STORAGE_SCAN_END = 10;
    private static final int MSG_CHECK_STORAGE_STATE = 11;
    private static final int MSG_CAN_BOX_PLAYING = 12;

    private static final long TIME_SHOW_ERROR = 2 * 1000;
    private static final long TIME_FF_PLAY = 2 * 1000;
    private static final String SRT = ".srt";
    private static final long TIME_DELAY = 5 * 1000;

    private static final int MOUNTED = 0;
    private static final int SCANING_FILE = 1;
    private static final int SCAN_FINISH = 2;
    private static final int NOT_MOUNTED = 3;
    private static final int QUERY_COUNT = 50;
    private static final int SLEEP_TIME = 100;
    private static final int TIME_ONE_THOUSAND = 1000;
    private static final int TIME_ZERO = 0;
    private static final int ATC_ERROE = -38;
    static final String USB_PATH = "/mnt/udisk";

    private Set<IVideoPlayStateChangedListener> mCallbackSet = new HashSet<IVideoPlayStateChangedListener>();

    // private List<String> mVideoPathsList = new LinkedList<String>();

    // private List<String> mAllVideoPathsList = new ArrayList<String>();

    private Stack<String> mHistoryPlayList = new Stack<String>();
    // private List<String> mNotPlayedVideo = new ArrayList<String>();
    private List<VideoBean> mPlayList = new ArrayList<VideoBean>();
    private List<VideoBean> mVideoListForBrowser = new ArrayList<VideoBean>();
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private volatile MediaPlayer mMediaPlayer = null;
    private volatile String mCurrentPlayingPath = "";
    private MediaPlayerListener mMediaPlayerListener = new MediaPlayerListener();
    private STORAGE_PORT mSleepDevice;

    private Deque<STORAGE_PORT> mMountedList = new LinkedBlockingDeque<STORAGE_PORT>();

    private int mCurrentPlayPos;

    private int mCurrentPlayingIndex = 0;

    private volatile int mMediaPlayerState = -1;

    private ShuffleMode mShuffleMode = ShuffleMode.OFF;
    private RepeateMode mRepeatMode = RepeateMode.ALL;

    private boolean mIsPrevOp;

    // 当前的播放队列的设备，和列表的区分开来
    private STORAGE_PORT mCurrentDevice = STORAGE_PORT.STORAGE_ALL;

    VideoToast mToast = null;

    private ListManager mListManager;

    public int mCapable;
    private STORAGE_PORT mSelectedDevice;
    private Handler mAsyncHandler;
    private boolean mCanShowToast = false;
    private boolean mIsFFPlay = false;
    private int mPlayBackValue;
    AtcMediaPlayer mAtcFFMediaPlayer = null;

    private int[] mStorageScanState = new int[STORAGE_PORT.values().length];
    private int[] mFFPlayArray = { AtcMediaPlayer.MEDIA_PLAYBACK_FF_4X, AtcMediaPlayer.MEDIA_PLAYBACK_FF_8X,
            AtcMediaPlayer.MEDIA_PLAYBACK_FF_16X, AtcMediaPlayer.MEDIA_PLAYBACK_FF_32X };
    private int[] mRWPlayArray = { AtcMediaPlayer.MEDIA_PLAYBACK_RW_4X, AtcMediaPlayer.MEDIA_PLAYBACK_RW_8X,
            AtcMediaPlayer.MEDIA_PLAYBACK_RW_16X, AtcMediaPlayer.MEDIA_PLAYBACK_RW_32X };

    private Handler mHandler = new Handler(AdayoVideoPlayerApplication.instance().getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_ERROE_NEXT:
                    if (mToast != null) {
                        mToast.cancel();
                    }
                    onErrorNext();
                    break;
                case MSG_FF_PLAY:
                    try {
                        mPlayBackValue++;
                        Trace.d(TAG, "playBackValue == " + mPlayBackValue);
                        if (mPlayBackValue > (mFFPlayArray.length - 1)) {
                            return;
                        }
                        if (!isInPlayingMode()) {
                            return;
                        }
                        if (mIsFFPlay) {
                            mAtcFFMediaPlayer.setPlaybackRate(mFFPlayArray[mPlayBackValue]);
                        } else {
                            mAtcFFMediaPlayer.setPlaybackRate(mRWPlayArray[mPlayBackValue]);
                        }
                        sendEmptyMessageDelayed(MSG_FF_PLAY, TIME_FF_PLAY);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_NOTHING_PLAY:
                    nothingToPlay();
                    break;
                case MSG_RET_VIDEO_LIST:
                    retVideoList((STORAGE_PORT) msg.obj);
                    break;
                case MSG_UPDATE_FINISH:
                    break;
                case MSG_CLOSE_SOURCE:
                    Trace.d(TAG, "[MSG_CLOSE_SOURCE]");
                    SourceSwitchManager.getSourceSwitchManager().audioChannelRelease(MEDIA_SOURCE_ID.VIDEO,
                            SEAT_TYPE.front_seat, null);
                    break;
                case MSG_STORAGE_UNMOUNT:
                    updatePlayListWhenRemoveStorage((STORAGE_PORT) msg.obj);
                    break;
                case MSG_STORAGE_SCAN_END:
                    updatePlayListWhenScanEnd((STORAGE_PORT) msg.obj);
                    break;
                case MSG_CHECK_STORAGE_STATE:
                    VideoPlayController.this.checkStorateState((STORAGE_PORT) msg.obj);
                    break;
                case MSG_CAN_BOX_PLAYING:
                    queryNextCanBoxTime();
                    if (mMediaPlayerState == STARTED) {
                        reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_PLAYING);
                    }
                    break;
            }
        }

    };

    private HandlerThread mHandlerThread = new HandlerThread("async_thread");

    private void handlerAsyncMessage(Message msg) {
        switch (msg.what) {
            case MSG_STORAGE_UNMOUNT:
                removeDevice((STORAGE_PORT) msg.obj);
                break;
            case MSG_STORAGE_SCAN_END:
                updateListMangerWhenScanEnd((STORAGE_PORT) msg.obj);
                break;
            case MSG_INIT_AGAIN:
                init();
                break;
            default:
                break;
        }
    }

    protected void checkStorateState(STORAGE_PORT storage) {
        Trace.d(TAG, "[checkStorateState]:storage " + storage);
        Trace.d(TAG, "[checkStorateState]:mCurrentDevice==" + mCurrentDevice);
        boolean savePathExist = false;
        String savedPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
        try {
            File file = new File(savedPath);
            savePathExist = file.exists();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!STORAGE_PORT.STORAGE_ALL.equals(storage)) {
            if (checkIsMounted(storage)) {
                Trace.d(TAG, "[checkStorateState]:mounted " + storage);
                mCurrentDevice = storage;

                mVideoListForBrowser = mPlayList = mListManager.getVideoList(mCurrentDevice);
                Trace.d(TAG, "[checkStorateState]: mPlayList.size()1==" + mPlayList.size());
                mSelectedDevice = mCurrentDevice;
                mCurrentPlayingPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
                mCurrentPlayPos = AdayoVideoPlayerApplication.instance().getLastPlayPosition();
                if (savePathExist) {
                    Trace.e(TAG, "save path exitst !!! " + mCurrentPlayingPath);
                    if (isPlayListEmpty()) {
                        addToPlayList(mCurrentPlayingPath);
                        clearCurrentPlayingInfo(mCurrentPlayingPath, mCurrentPlayPos, 0);
                    } else {
                        mCurrentPlayingIndex = -1;
                        updateCurrentPlayingInfo(mCurrentPlayingPath);
                    }
                } else {
                    Trace.e(TAG, "save path not exitst !!! " + mCurrentPlayingPath);
                    if (isInScanState(mCurrentDevice)) {
                        Trace.d(TAG, "[checkStorateState]:storage " + storage + " in scan");
                        return;
                    }

                    if (isPlayListEmpty()) {
                        Trace.d(TAG, "[checkStorateState]:storage " + storage
                                + " video play list is empty,but not in scanning");
                        storage = STORAGE_PORT.STORAGE_ALL;
                    } else {
                        clearCurrentPlayingInfo(mPlayList.get(0).mPath, 0, 0);
                    }
                }

                // if(!mCurrentPlayingPath.equals(savedPath)){
                // Trace.d(TAG,"[checkStorateState]:save path not in play list "
                // + savedPath);
                // VideoBean bean = new VideoBean();
                // bean.path = savedPath;
                // bean.name = VideoUtils.getTitle(savedPath);
                // mPlayList.add(0, bean);
                // mCurrentPlayingPath = savedPath;
                // mCurrentPlayingIndex = 0;
                // }
            } else {
                storage = STORAGE_PORT.STORAGE_ALL;
                Trace.d(TAG, "[checkStorateState]:not mounted " + storage);
            }
        }

        if (STORAGE_PORT.STORAGE_ALL.equals(storage)) {

            if (mMountedList.isEmpty()) {
                notifyNoDevices();
                willCloseSource();
                return;
            } else if (hasDeviceInScan()) {
                Trace.d(TAG, "[firstStartWhenInSource]:has storage in scan");
            } else if (!hasVideoFiles()) {
                notifyNoVideoFile();
                willCloseSource();
                return;
            }

            mCurrentDevice = storage;

            makePlayList(mCurrentDevice);

            if (savePathExist) {
                Trace.d(TAG, "[checkStorateState]:saved path exist");
                Trace.d(TAG, "[checkStorateState]:mCurrentPlayingPath==" + mCurrentPlayingPath);
                Trace.d(TAG, "[checkStorateState]:savedPath==" + savedPath);

                if (!mCurrentPlayingPath.equals(savedPath) || isPlayListEmpty()) {
                    Trace.d(TAG, "[checkStorateState]:save path not in play list " + savedPath);
                    VideoBean bean = new VideoBean();
                    bean.mPath = savedPath;
                    bean.mName = VideoUtils.getTitle(savedPath);
                    mPlayList.add(0, bean);
                    Trace.d(TAG, "[checkStorateState]: mPlayList.size()2==" + mPlayList.size());
                    mCurrentPlayingPath = savedPath;
                    mCurrentPlayingIndex = 0;
                }
            }

        }
        if (isInVideoSouce() && !isInPlayingMode()) {
            beginNewPlay();
        } else {
            Trace.d(TAG, "[checkStorateState]:not in video source");
        }

    }

    private void updateCurrentPlayingInfo(String mCurrentPlayingPath2) {
        updatePlayIndex(mCurrentPlayingPath);
        if (mCurrentPlayingIndex == -1) {
            clearCurrentPlayingInfo(mPlayList.get(0).mPath, 0, 0);
        }
    }

    private boolean isInVideoSouce() {
        return MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(
                SEAT_TYPE.front_seat));
    }

    public VideoPlayController() {
        mToast = new VideoToast(AdayoVideoPlayerApplication.instance());

        mHandlerThread.start();
        mAsyncHandler = new Handler(mHandlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                handlerAsyncMessage(msg);
            }

        };
        mAsyncHandler.sendEmptyMessageDelayed(MSG_INIT_AGAIN, TIME_DELAY);
    }

    // public VideoPlayController(SurfaceView surfaceView){
    // init();
    // setup(surfaceView);
    // }
    // public static VideoPlayControler instance(){
    // if(mControler == null){
    // mControler = new VideoPlayControler();
    // }
    // return mControler;
    // }

    public void setup(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        // mMediaPlayer.setSurface(null);
        // mMediaPlayer.setDisplay(null);
        // mSurfaceView.
        if (mSurfaceView != null) {
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(mCallback);
        }
        setSurface();
    }

    private MediaObjectQueryInterface getQuery(STORAGE_PORT storage) {
        MediaObjectQueryInterface queryInterface = AdayoMediaScanner.getAdayoMediaScanner().getQueryer(storage);
        int count = 0;
        while (queryInterface == null && count < QUERY_COUNT) {
            Trace.e(TAG, "[init]:query interface is null!!!");
            queryInterface = AdayoMediaScanner.getAdayoMediaScanner().getQueryer(storage);
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
        return queryInterface;
    }

    private void init() {
        mListManager = new ListManager();
        AdayoVideoPlayerApplication.instance().registerFileStateChanged(mFilesStateChanged);

        STORAGE_PORT[] mountedStorages = CommonUtil.getMountedStorage();
        if (mountedStorages != null) {
            for (STORAGE_PORT storage : mountedStorages) {
                if (STORAGE_PORT.STORAGE_ALL.equals(storage)) {
                    continue;
                }
                MediaObjectQueryInterface queryInterface = getQuery(storage);
                if (queryInterface == null) {
                    continue;
                }
                addToMountedList(storage);
                SCANNING_STATE state = queryInterface.getScanningState();
                if (SCANNING_STATE.SCANNING_FILE.equals(state) || SCANNING_STATE.NOT_START.equals(state)) {
                    setStorageState(storage, SCANING_FILE);
                } else {
                    setStorageState(storage, SCAN_FINISH);
                }

                Trace.d(TAG, "[init]:storage " + storage + " scan state " + state);
            }
        }

        initMediaPlayer();

        initPlayPath();

        requestFocus();

        AdayoVideoPlayerApplication.instance().onInitDone();
    }

    public int requestFocus() {
        AudioManager audioManager = (AudioManager) AdayoVideoPlayerApplication.instance().getSystemService(
                Service.AUDIO_SERVICE);
        if (MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(
                SEAT_TYPE.front_seat))) {
            Trace.d(TAG, "requestFocus ");
            return audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        return -1;
    }

    public void abandonFocus() {
        AudioManager audioManager = (AudioManager) AdayoVideoPlayerApplication.instance().getSystemService(
                Service.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    private void addToMountedList(STORAGE_PORT storage) {
        if (!mMountedList.contains(storage)) {
            mMountedList.add(storage);
        }
    }

    private void initMediaPlayer() {
        Trace.d(TAG, "[initMediaPlayer]");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(mMediaPlayerListener);
        mMediaPlayer.setOnErrorListener(mMediaPlayerListener);
        mMediaPlayer.setOnPreparedListener(mMediaPlayerListener);
        mMediaPlayer.setOnInfoListener(mMediaPlayerListener);
        mMediaPlayer.setOnSeekCompleteListener(mMediaPlayerListener);
        mMediaPlayer.setOnTimedTextListener(mMediaPlayerListener);
        mMediaPlayerState = IDLE;
    }

    public void registerVideoStateChangedListener(IVideoPlayStateChangedListener listener, boolean isActivity) {
        long time = System.currentTimeMillis();
        Trace.d(TAG, "[registerVideoStateChangedListener]");

        Trace.d(TAG,
                "[registerVideoStateChangedListener] mCallbackSet.contains(listener)=="
                        + mCallbackSet.contains(listener));
        if (mCallbackSet.contains(listener)) {
            return;
        }
        mCallbackSet.add(listener);
        if (listener != null) {
            listener.onPlayStateChanged(mMediaPlayerState);
            listener.onRepeateModeChanged(mRepeatMode);
            listener.onShuffleModeChanged(mShuffleMode);
            if (canGetDuration()) {
                listener.playVideoChanged(mCurrentPlayingPath, getDuration());
            }
        }
        if (!isActivity) {
            return;
        }

        if (mHandler.hasMessages(MSG_CHECK_STORAGE_STATE)) {
            Trace.d(TAG, "[registerVideoStateChangedListener]:has MSG_CHECK_STORAGE_STATE,not check video state");
            return;
        }

        checkVideoState(listener);
        Trace.d(TAG, "[registerVideoStateChangedListener]:spend time " + (System.currentTimeMillis() - time));
    }

    // 此函数只做判断，并不会生成播放队列。
    private void checkVideoState(IVideoPlayStateChangedListener listener) {

        Trace.d(TAG, "[checkVideoState]");
        mHandler.removeMessages(MSG_CLOSE_SOURCE);
        if (!hasDeviceMounted()) {
            if (listener != null) {
                listener.noDevices();
            }
            willCloseSource();
            return;
        }
        if (!isPlayListEmpty()) {
            Trace.d(TAG, "[checkVideoState] has PlayList");
            notifyBeginPlaying();
        } else if (!hasDeviceInScan()) {
            if (!hasVideoFiles()) {
                Trace.d(TAG, "[checkVideoState]:no video file");
                listener.noVideoFileToPlay();
                willCloseSource();
            }
        } else {// 有设备正在扫描
            Trace.d(TAG, "[checkVideoState] video is scanning ");
            if (STORAGE_PORT.STORAGE_ALL.equals(mCurrentDevice) || isInScanState(mCurrentDevice)) {
                listener.onLoading(mCurrentDevice);
            }
        }
    }

    private boolean checkIsMounted(STORAGE_PORT storage) {
        return ScannerManager.getScannerManager().isMounted(Utils.getStoragePath(storage));
    }

    private void willCloseSource() {
        Trace.d(TAG, "[willCloseSource]");
        mHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOURCE, TIME_DELAY);
    }

    public void unregisterVideoStateChangedListener(IVideoPlayStateChangedListener listener) {
        mCallbackSet.remove(listener);
    }

    public void pause() {

        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        if (mMediaPlayerState != STARTED) {
            return;
        }

        try {
            mMediaPlayer.pause();
            mMediaPlayerState = PAUSED;
            notifyPlayStateChanged();
            reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_PAUSE);
        } catch (Exception e) {
            e.printStackTrace();
            mMediaPlayerState = ERROR;
        }
        // savePlayPos();
    }

    public void stop() {
        Trace.d(TAG, "[stop()]:");
        savePlayPos();
        mediaPlayerStop();
    }

    private void mediaPlayerStop() {
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        Trace.d(TAG, "[mediaPlayerStop]:what to STOP in " + mMediaPlayerState + " state");
        if (mMediaPlayerState == IDLE || mMediaPlayerState == INIT || mMediaPlayerState == ERROR
                || mMediaPlayerState == PREPARING || mMediaPlayerState == COMPLETED) {
            return;
        }
        Trace.d(TAG, "[mediaPlayerStop]:STOPPED in " + mMediaPlayerState + " state");
        try {
            long time = System.currentTimeMillis();
            mMediaPlayer.stop();
            Trace.w(TAG, "---stop---");
            Trace.d(TAG, "mmTest stop time " + (System.currentTimeMillis() - time));
            mMediaPlayerState = STOPPED;
            notifyPlayStateChanged();
            // reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_STOP);
        } catch (Exception e) {
            mMediaPlayerState = ERROR;
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param position
     *            选中列表界面中的列表的第position个
     * @return 返回操作是否成功
     */
    public boolean selectedVideo(int position) {

        if (mVideoListForBrowser.size() <= 0) {
            Trace.e(TAG, "[selectedVideo]:position " + position);
            return false;
        }

        AdayoVideoPlayerApplication.instance().saveLastDevice(mSelectedDevice);
        mCurrentDevice = mSelectedDevice;
        play(mVideoListForBrowser.get(position).mPath);
        mPlayList = mVideoListForBrowser;
        Trace.d(TAG, "[selectedVideo]:mPlayList.size()== " + mPlayList.size());
        mCurrentPlayingIndex = position;

        return true;
    }

    public void play(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        if (!new File(path).exists()) {
            return;
        }

        Trace.d(TAG, "[play]:path " + path);

        mediaPlayerStop();

        clearCurrentPlayingInfo(path, 0, mCurrentPlayingIndex);

        beginNewPlay();
        Trace.d(TAG, "[play]:" + mCurrentPlayPos);
    }

    public void play() {
        Trace.d(TAG, "[play]:media state " + mMediaPlayerState);

        if (mMediaPlayer == null) {// 接收到系统的MediaPlayer资源之后释放掉了，所以为NULL了
            notifyNotReady();
        }
        if (mMediaPlayerState == STARTED) {
            ;// pause();
        } else if (mMediaPlayerState == PAUSED) {
            mMediaPlayer.start();
            Trace.w(TAG, "---play---");
            mMediaPlayerState = STARTED;
            startPlayCanBox();
        } else {
            beginNewPlay();
        }
        notifyPlayStateChanged();

    }

    public void reportCanbox(int canboxMediaState) {
        CanboxManager.getCanboxManager().reportMediaPlayingInfo(CanboxConstantsDef.CANBOX_FILE_TYPE_VIDEO,
                canboxMediaState, (getPlayingIndex() + 1), getPlayListCount(),
                canGetPosition() ? mMediaPlayer.getCurrentPosition() / TIME_ONE_THOUSAND : TIME_ZERO,
                canGetDuration() ? mMediaPlayer.getDuration() / TIME_ONE_THOUSAND : TIME_ZERO);
    }

    public boolean isPlaying() {
        return mMediaPlayerState == STARTED;
    }

    public boolean isPaused() {
        Trace.e(TAG, "  mMediaPlayerState == PAUSED ?==" + (mMediaPlayerState == PAUSED));
        Trace.e(TAG, "  mMediaPlayerState ==" + (mMediaPlayerState));
        return mMediaPlayerState == PAUSED;
    }

    public boolean isInPlayingMode() {
        return mMediaPlayerState == STARTED || mMediaPlayerState == PAUSED;
    }

    private boolean canGetDuration() {
        return mMediaPlayerState == PREPARED || mMediaPlayerState == STARTED || mMediaPlayerState == PAUSED
                || mMediaPlayerState == STOPPED || mMediaPlayerState == COMPLETED;
    }

    private boolean canGetPosition() {
        return mMediaPlayerState == PREPARED || mMediaPlayerState == STARTED || mMediaPlayerState == PAUSED;
    }

    private void notifyNotReady() {
        Trace.e(TAG, "MediaPlayer not ready!!!!");
        // init();
    }

    public boolean isPreparing() {
        return mMediaPlayerState == PREPARING;
    }

    private void beginNewPlay() {

        Trace.d(TAG, "[beginNewPlay]" + mCurrentPlayingPath + " state " + mMediaPlayerState);
        if (mMediaPlayerState == PREPARING) {
            Trace.d(TAG, "[beginNewPlay]" + " state PREPARING");
            return;
        }

        mHandler.removeMessages(MSG_ON_ERROE_NEXT);
        if (mToast != null) {
            mToast.cancel();
        }
        clearPlayInfo();

        if (mPlayList.isEmpty()) {
            nothingToPlay();
            return;
        }
        if (TextUtils.isEmpty(mCurrentPlayingPath)) {
            if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex >= mPlayList.size()) {
                mCurrentPlayingIndex = 0;
            }
            mCurrentPlayingPath = mPlayList.get(mCurrentPlayingIndex).mPath;
        } else {
            if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex >= mPlayList.size()) {
                updatePlayIndex(mCurrentPlayingPath);
            } else if (mPlayList != null && mPlayList.size() > 0
                    && !mPlayList.get(mCurrentPlayingIndex).mPath.equals(mCurrentPlayingPath)) {
                updatePlayIndex(mCurrentPlayingPath);
            }
        }

        if (mMediaPlayer == null) {

            return;

            // try{
            // init();
            // setSurface();
            // mMediaPlayer.setDisplay(mSurfaceHolder);
            // }catch(Exception e){
            // e.printStackTrace();
            // }
        }
        notifyBeginPlaying();
        try {
            long time = System.currentTimeMillis();
            mMediaPlayer.reset();
            Trace.d(TAG, "mmTest reset time " + (System.currentTimeMillis() - time));
            mMediaPlayerState = IDLE;

            mMediaPlayer.setLooping(RepeateMode.ONE.equals(mRepeatMode));

            File file = new File(mCurrentPlayingPath);
            FileInputStream fis = new FileInputStream(file);
            time = System.currentTimeMillis();
            mMediaPlayer.setDataSource(fis.getFD());
            mMediaPlayerState = INIT;
            Trace.d(TAG, "mmTest setDataSource time " + (System.currentTimeMillis() - time));
            fis.close();
            time = System.currentTimeMillis();
            if (mCurrentPlayingPath !=null && !mCurrentPlayingPath.equals(AdayoVideoPlayerApplication.instance().getLastSavePlayPath())) {
                Trace.d(TAG, "mCurrentPlayingPath " + mCurrentPlayingPath);
                Trace.d(TAG, "lastPlayingPath " + AdayoVideoPlayerApplication.instance().getLastSavePlayPath());
                mCurrentPlayPos = 0;
                AdayoVideoPlayerApplication.instance().saveLastPlayPosition(mCurrentPlayPos);
            } else {
                mCurrentPlayPos = AdayoVideoPlayerApplication.instance().getLastPlayPosition();
                Trace.d(TAG, "[beginNewPlay]:mCurrentPlayPos == " + mCurrentPlayPos);

            }
            mMediaPlayer.prepareAsync();
            mMediaPlayerState = PREPARING;
            // mMediaPlayer.prepare();
            // mMediaPlayerListener.onPrepared(mMediaPlayer);
            Trace.d(TAG, "mmTest prepareAsync time " + (System.currentTimeMillis() - time));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            mMediaPlayerState = ERROR;
            // removeCurrentPlayingPath();
            clearCurrentPlayingInfo("", 0, mCurrentPlayingIndex);
            showToast(R.string.play_error);
            onErrorNext();
        } catch (Exception e) {
            e.printStackTrace();
            mMediaPlayerState = ERROR;
            showToast(R.string.play_error);
            onErrorNext();
            clearCurrentPlayingInfo("", 0, mCurrentPlayingIndex);
        }
    }

    private void loadSubtitle(String videoPath) {
        if (videoPath == null) {
            return;
        }
        if (!videoPath.equals(mCurrentPlayingPath)) {
            Trace.w(TAG, "[loadSubtitle]:videoPath is not current video,videoPath " + videoPath
                    + " current video path " + mCurrentPlayingPath);
            return;
        }

        File subFile = findSubtitleFile(videoPath);

        if (subFile == null || !subFile.exists()) {
            return;
        }
        try {
            Trace.d(TAG, "[loadSubtitle]:" + subFile.getAbsolutePath());
            mMediaPlayer.addTimedTextSource(subFile.getAbsolutePath(), MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkLoadSubtitle() {
        Trace.d(TAG, "[checkLoadSubtitle]");
        try {
            final String videoPath = mCurrentPlayingPath;
            loadSubtitle(videoPath);

            TrackInfo[] trackInfos = mMediaPlayer.getTrackInfo();

            if (trackInfos == null || trackInfos.length <= 0) {
                return;
            }
            for (int i = 0; i < trackInfos.length; i++) {
                TrackInfo trackInfo = trackInfos[i];
                Trace.d(TAG, "[checkLoadSubtitle" + trackInfo.getTrackType() + " " + trackInfo.getLanguage());

                if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                    mMediaPlayer.selectTrack(i);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private File findSubtitleFile(String videoPath) {
        File videoFile = new File(videoPath);
        final String prefix = videoPath.subSequence(0, videoPath.lastIndexOf(".")).toString();
        Trace.d(TAG, "[findSubtitleFile]:" + prefix);
        if (!videoFile.exists() || !videoFile.isFile() || TextUtils.isEmpty(prefix)) {
            return null;
        }
        File subFile = null;
        try {
            subFile = new File(prefix + SRT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subFile;
    }

    private void onErrorNext() {
        mHandler.removeMessages(MSG_ON_ERROE_NEXT);
        if (mIsPrevOp) {
            playPrev();
        } else {
            playNext();
        }
    }

    public void next() {
        playNext();
    }

    public void prev() {
        playPrev();
    }

    private void playPrev() {
        mIsPrevOp = true;
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        if (ShuffleMode.OFF.equals(mShuffleMode) && mPlayList.isEmpty()) {
            nothingToPlay();
            return;
        }

        stop();

        if (ShuffleMode.OFF.equals(mShuffleMode)) {
            mCurrentPlayingIndex = mCurrentPlayingIndex - 1 >= 0 ? mCurrentPlayingIndex - 1 : mPlayList.size() - 1;
            mCurrentPlayingPath = mPlayList.get(mCurrentPlayingIndex).mPath;
        } else {
            if (mHistoryPlayList.size() <= 0) {
                randomNextVideo();
            } else {
                mCurrentPlayingPath = mHistoryPlayList.pop();
            }
        }
        mCurrentPlayPos = 0;
        beginNewPlay();
    }

    private void playNext() {
        mIsPrevOp = false;
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        if (ShuffleMode.OFF.equals(mShuffleMode) && mPlayList.isEmpty()) {
            nothingToPlay();
            return;
        }

        // if(mMediaPlayerState == ERROR)
        // return;

        stop();
        long time = System.currentTimeMillis();

        if (ShuffleMode.OFF.equals(mShuffleMode)) {
            mCurrentPlayingIndex = (mCurrentPlayingIndex + 1) % mPlayList.size();
            mCurrentPlayingPath = mPlayList.get(mCurrentPlayingIndex).mPath;
        } else {
            if (isLegalVideoPath(mCurrentPlayingPath)) {
                mHistoryPlayList.push(mCurrentPlayingPath);// 这里是上一首的
            }
            randomNextVideo();
        }

        Trace.d(TAG, "mmTest get path time " + (System.currentTimeMillis() - time));
        mCurrentPlayPos = 0;
        if (mPlayList!=null&&mPlayList.size()==1) {
            AdayoVideoPlayerApplication.instance().saveLastPlayPosition(mCurrentPlayPos);
        }
        time = System.currentTimeMillis();
        beginNewPlay();
        Trace.d(TAG, "mmTest beginNewPlay time " + (System.currentTimeMillis() - time));

        Trace.d(TAG, "[next]" + mCurrentPlayingPath);
    }

    public void seek(int pos) {

        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        if ((mCapable & AtcMediaPlayer.FILE_SEEK_UNSUPPORT) != 0) {
            Trace.d(TAG, "[seek]:not support to seek");
            return;
        }

        try {
            mMediaPlayer.seekTo(pos);
            // savePlayPos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void randomNextVideo() {
        // if(mNotPlayedVideo.size() <= 0){
        // mNotPlayedVideo.addAll(mVideoPathsList);
        // }
        mCurrentPlayingPath = choseOnePathFromNotPlayedList();
        // mNotPlayedVideo.remove(mCurrentPlayingPath);
    }

    public String choseOnePathFromNotPlayedList() {
        // if(mNotPlayedVideo.size() <= 0)
        // return "";
        // int idx = (int)(Math.random() * FRACTOR )% mNotPlayedVideo.size();
        // return mNotPlayedVideo.get(idx);
        mCurrentPlayingIndex = Math
                .abs((int) ((Math.random() * FRACTOR
                        * Math.sin(System.currentTimeMillis() / mPlayList.size() * Math.cos(mPlayList.size())) + RANDOM) % mPlayList
                        .size()));
        return mPlayList.get(mCurrentPlayingIndex).mPath;
    }

    public String getCurrentPlayingPath() {
        return mCurrentPlayingPath;
    }

    public void destroy() {
        onSourceEnd();
        AdayoVideoPlayerApplication.instance().unregsterFileStateChanged(mFilesStateChanged);

    }

    private Callback mCallback = new Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Trace.d(TAG, "[surfaceDestroyed]");
            try {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // holder.removeCallback(callback);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Trace.d(TAG, "[surfaceCreated]");
            mSurfaceHolder = holder;
            try {
                if (null != mMediaPlayer) {
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Trace.d(TAG, "[surfaceChanged]: format " + format + " width " + width);
            // if(mMediaPlayer == null){
            // init();
            // }
            // mMediaPlayer.start();
        }
    };

    class MediaPlayerListener implements OnCompletionListener, OnErrorListener, OnInfoListener, OnPreparedListener,
            OnSeekCompleteListener, OnVideoSizeChangedListener, OnTimedTextListener {

        @Override
        public void onTimedText(MediaPlayer mp, TimedText text) {
            String textString = text == null ? "" : text.getText() == null ? "" : text.getText();
            Trace.d(TAG, "[onTimedText]:" + textString);
            // AtcTimedText atcTimedText = new AtcTimedText(text);
            // if(atcTimedText != null){
            // Bitmap bmp = atcTimedText.getPicture();
            // notifyTimedText(textString,bmp);
            // }else{
            // notifyTimedText(textString, null);
            // }
            notifyTimedText(textString, null);
        }

        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            Trace.d(TAG, "[onVideoSizeChanged]: width " + width + " height " + height);
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            Trace.d(TAG, "[onSeekComplete]");
            notifyOnSeekComplete();
            Trace.d(TAG, "[onSeekComplete]:play state " + mMediaPlayer.isPlaying());
            // if(mMediaPlayerState == COMPLETED){
            // if(!mMediaPlayer.isPlaying()){
            // mMediaPlayer.start();
            // mMediaPlayerState = STARTED;
            // }
            // }else if(!mMediaPlayer.isPlaying()){
            // mMediaPlayer.start();
            // mMediaPlayerState = STARTED;
            // }
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            Trace.d(TAG, "[onPrepared]");
            mMediaPlayerState = PREPARED;

            checkLoadSubtitle();
            long time = System.currentTimeMillis();
            mp.start();
            Settings.System.putString(AdayoVideoPlayerApplication.instance().getContentResolver(),
                    SettingConstantsDef.AUTOTEST_VIDEO_FILE_FROM, sdOrUsb());
            Trace.w(TAG, "---play---");
            Trace.d(TAG, "mmTest start time " + (System.currentTimeMillis() - time));
            mMediaPlayerState = STARTED;
            savePlayPath();
            notifyPlayVideoChanged();
            notifyPlayStateChanged();
            startPlayCanBox();
            AtcMediaPlayer atcMediaPlayer = new AtcMediaPlayer(mMediaPlayer);
            mCapable = atcMediaPlayer.getMediaCapability();
            checkVideoSupportState();

            if ((mCapable & AtcMediaPlayer.FILE_SEEK_UNSUPPORT) != 0) {
                Trace.w(TAG, "onPreparedtoPlay this file do not support seek");
                notifySeekable(false);
            } else {
                Trace.w(TAG, "onPreparedtoPlay this file  support seek");
                notifySeekable(true);
                int duration = getDuration();
                Trace.d(TAG, "duration==" + duration + "   mCurrentPlayPos==" + mCurrentPlayPos);
                if (mCurrentPlayPos != 0 && mCurrentPlayPos < duration) {
                    Trace.d(TAG, "[onPrepared]:seek to " + mCurrentPlayPos);
                    mMediaPlayer.seekTo(mCurrentPlayPos);
                } else if (duration <= 0) {
                    Trace.e(TAG, "[onPrepared]:error to seek pos,mCurrentPlayPos " + mCurrentPlayPos + " duration "
                            + duration);
                }
            }
        }

        private String sdOrUsb() {
            byte[] mData = new byte[2];
            mData[0] = SettingConstantsDef.ADAYO_SD_SOURCE;
            mData[1] = SettingConstantsDef.ADAYO_OFF_SOURCE;
            if (mCurrentPlayingPath != null) {
                if (mCurrentPlayingPath.indexOf(USB_PATH) != -1) {
                    mData[0] = SettingConstantsDef.ADAYO_USB_SOURCE;
                    Trace.d(TAG, "sdOrUsb(): mCurrentPlayingPath  USB" );
                    SettingManager.getInstance().writeDataToMCU(SettingConstantsDef.ADAYO_MCU_CMMD_SOURCE_CHANGE, mData);
                    return "USB";
                }
            }
            SettingManager.getInstance().writeDataToMCU(SettingConstantsDef.ADAYO_MCU_CMMD_SOURCE_CHANGE, mData);
            Trace.d(TAG, "sdOrUsb(): mCurrentPlayingPath   SD" );
            return "SD";
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            Trace.d(TAG, "[onInfo]: what " + what + " extra " + extra);

            switch (what) {
                case AtcMediaPlayer.MEDIA_INFO_CBM_STOP:
                    Trace.d(TAG, "MEDIA_INFO_CBM_STOP");
                    stop();
                    // destroy();
                    Trace.d(TAG, "done MEDIA_INFO_CBM_STOP");
                    mMediaPlayerState = STOPPED;
                    return false;
                case AtcMediaPlayer.MEDIA_INFO_UNSUPPORTED_AUDIO:
                    Trace.d(TAG, "MEDIA_INFO_UNSUPPORTED_AUDIO");
                    showToast(R.string.unsupport_audio_type);
                    if (!mHandler.hasMessages(MSG_ON_ERROE_NEXT)) {
                        mHandler.sendEmptyMessageDelayed(MSG_ON_ERROE_NEXT, TIME_SHOW_ERROR);
                    }
                    break;
                case AtcMediaPlayer.MEDIA_INFO_UNSUPPORTED_VIDEO:
                    Trace.d(TAG, "MEDIA_INFO_UNSUPPORTED_VIDEO");
                    showToast(R.string.unsupport_video_type);
                    mHandler.sendEmptyMessageDelayed(MSG_ON_ERROE_NEXT, TIME_SHOW_ERROR);
                    break;
                case AtcMediaPlayer.MEDIA_INFO_CBM_FORBID:
                    Trace.d(TAG, "MEDIA_INFO_CBM_FORBID");
                    mMediaPlayerState = PAUSED;
                    break;
                case AtcMediaPlayer.MEDIA_INFO_CBM_RESUME:
                    Trace.d(TAG, "MEDIA_INFO_CBM_RESUME");
                    break;
                case AtcMediaPlayer.MEDIA_INFO_CBM_START:
                    Trace.d(TAG, "MEDIA_INFO_CBM_START");
                    break;
                case AtcMediaPlayer.MEDIA_INFO_DIVX:
                    Trace.d(TAG, "MEDIA_INFO_DIVX");
                    break;
                case AtcMediaPlayer.MEDIA_INFO_DIVXDRM:
                    Trace.d(TAG, "MEDIA_INFO_DIVXDRM");
                    break;
                case AtcMediaPlayer.MEDIA_INFO_DIVXDRM_ERROR:
                    Trace.d(TAG, "MEDIA_INFO_DIVXDRM_ERROR");
                    break;
                case AtcMediaPlayer.MEDIA_INFO_UNSUPPORTED_MENU:
                    Trace.d(TAG, "MEDIA_INFO_UNSUPPORTED_MENU");
                    break;
                case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                    Trace.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                    notifySeekable(false);
                    break;
                case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                    Trace.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Trace.d(TAG, "MEDIA_INFO_BUFFERING_END");
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Trace.d(TAG, "MEDIA_INFO_BUFFERING_START");
                    break;
                case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    Trace.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                    break;
                case MediaPlayer.MEDIA_INFO_UNKNOWN:
                    Trace.d(TAG, "MEDIA_INFO_UNKNOWN");
                    break;
                case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    Trace.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                    break;
                case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                    Trace.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {

            Trace.d(TAG, "[onError]:  currentPosition " + mp.getCurrentPosition());
            Trace.d(TAG, "[onError]: what " + what + " extra " + extra);
            AdayoVideoPlayerApplication.instance().saveLastPlayPosition(mp.getCurrentPosition());
            mMediaPlayerState = ERROR;
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    // mToast.makeText(AdayoVideoPlayerApplication.instance(),
                    // AdayoVideoPlayerApplication.instance().getString(R.string.unsupport_video_type),
                    // Toast.LENGTH_SHORT).show();
                    if (extra == AtcMediaPlayer.MEDIA_ERROR_UNSUPPORTED_FILE) {
                        showToast(R.string.unsupport_video_type_can_not_play);
                    } else {
                        showToast(R.string.play_error);
                    }
                    mHandler.sendEmptyMessageDelayed(MSG_ON_ERROE_NEXT, TIME_SHOW_ERROR);
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    Trace.d(TAG, "[onError]:MEDIA_ERROR_SERVER_DIED, extra " + extra);
                    // AdayoVideoPlayerApplication.instance().onMediaServerDied();
                    showToast(R.string.player_error);

                    stop();
                    if (mMediaPlayer != null) {
                        mMediaPlayer.release();
                    }
                    mMediaPlayer = null;
                    mMediaPlayerState = RELEASED;

                    initMediaPlayer();
                    setSurface();
                    // if(isInVideoSouce())
                    // beginNewPlay();

                    break;
                case ATC_ERROE:
                case MediaPlayer.MEDIA_ERROR_IO:
                    if (mMediaPlayer != null) {
                        mMediaPlayerState = ERROR;
                    }
                    if (!mHandler.hasMessages(MSG_ON_ERROE_NEXT)) {
                        mHandler.sendEmptyMessageDelayed(MSG_ON_ERROE_NEXT, TIME_SHOW_ERROR);
                    }
                    break;
                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    Trace.e(TAG, "MEDIA_ERROR_MALFORMED");
                    break;
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    Trace.e(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                    break;
                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    Trace.e(TAG, "MEDIA_ERROR_TIMED_OUT");
                    break;
                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    Trace.e(TAG, "MEDIA_ERROR_UNSUPPORTED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_CARD_EJECT:
                    Trace.e(TAG, "MEDIA_ERROR_CARD_EJECT");
                    stop();
                    initMediaPlayer();
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_DATA:
                    Trace.e(TAG, "MEDIA_ERROR_DATA");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_DIVXDRM_NEVER_REGISTERED:
                    Trace.e(TAG, "MEDIA_ERROR_DIVXDRM_NEVER_REGISTERED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_DIVXDRM_NOT_AUTHORIZED:
                    Trace.e(TAG, "MEDIA_ERROR_DIVXDRM_NOT_AUTHORIZED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_DIVXDRM_NOT_REGISTERED:
                    Trace.e(TAG, "MEDIA_ERROR_DIVXDRM_NOT_REGISTERED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_DIVXDRM_RENTAL_EXPIRED:
                    Trace.e(TAG, "MEDIA_ERROR_DIVXDRM_RENTAL_EXPIRED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_INVALID_PARAMETER:
                    Trace.e(TAG, "MEDIA_ERROR_INVALID_PARAMETER");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_OUTOFMEMOTY:
                    Trace.e(TAG, "MEDIA_ERROR_OUTOFMEMOTY");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_READ_FILE_FAILED:
                    Trace.e(TAG, "MEDIA_ERROR_READ_FILE_FAILED");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_UNSUPPORTED_DRM:
                    Trace.e(TAG, "MEDIA_ERROR_UNSUPPORTED_DRM");
                    break;
                case AtcMediaPlayer.MEDIA_ERROR_UNSUPPORTED_FILE:
                    Trace.e(TAG, "MEDIA_ERROR_UNSUPPORTED_FILE");
                    break;
                default:
                    // destroy();
                    // initMediaPlayer();
                    // AdayoVideoPlayerApplication.instance().registerFileStateChanged(mFilesStateChanged);
                    // setup(mSurfaceView);

                    break;
            }

            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            Trace.d(TAG, "[onCompletion]: isLooping " + mp.isLooping());
            Trace.d(TAG, "[onCompletion]: mRepeatMode " + mRepeatMode);
            if (mMediaPlayerState == ERROR) {
                return;
            } else {
                
                mMediaPlayerState = COMPLETED;
            }
            if (RepeateMode.ONE.equals(mRepeatMode)) {
                if (mMediaPlayerState == IDLE) {
                    beginNewPlay();
                }
                // else
                // seek(0);
            } else {
                playNext();
            }
        }
    }

    private void startPlayCanBox() {
        if (mHandler.hasMessages(MSG_CAN_BOX_PLAYING)) {
            return;
        }
        mHandler.sendEmptyMessageDelayed(MSG_CAN_BOX_PLAYING, 0);
    }

    private void queryNextCanBoxTime() {
        if (mHandler.hasMessages(MSG_CAN_BOX_PLAYING)) {
            return;
        }
        if (mMediaPlayerState == STARTED) {
            mHandler.sendEmptyMessageDelayed(MSG_CAN_BOX_PLAYING, TIME_ONE_THOUSAND);
        }
    }

    public void removeCurrentPlayingPath() {
        try {
            if (!new File(mCurrentPlayingPath).exists()) {
                Trace.d(TAG, "[removeCurrentPlayingPath]");
                // mVideoPathsList.remove(mCurrentPlayingPath);
                // mPlayList.remove(mCurrentPlayingPath);

                for (VideoBean bean : mPlayList) {
                    if (bean.mPath.equals(mCurrentPlayingPath)) {
                        mPlayList.remove(bean);
                        break;
                    }
                }

                mCurrentPlayingIndex = mCurrentPlayingIndex - 1 >= 0 ? mCurrentPlayingIndex - 1 : mPlayList.size() - 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToast(final int reason) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Trace.d(TAG, "[showToast]:");
                if (mCanShowToast && mToast != null) {
                    mToast.showText(reason);
                } else {
                    Trace.d(TAG,
                            "[showToast]:can;t show toast , "
                                    + AdayoVideoPlayerApplication.instance().getString(reason));
                }
            }
        });

    }

    private void notifySeekable(boolean seekable) {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.isSeekable(seekable);
        }
    }

    public void notifyTimedText(String text, Bitmap bmp) {
        for (IVideoPlayStateChangedListener mPlayStateChangeListener : mCallbackSet) {
            mPlayStateChangeListener.onTimedText(text, bmp);
        }
    }

    public void setSurface() {
        if (mSurfaceView != null && mMediaPlayer != null) {
            try {
                mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mSurfaceView.getHolder().addCallback(mCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initPlayPath() {
        mCurrentPlayingPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
        Trace.e(TAG, "save path " + mCurrentPlayingPath);

        mCurrentDevice = AdayoVideoPlayerApplication.instance().getLastSavedDevice();

        STORAGE_PORT storage = com.adayo.an6v.ui.Utils.getWhichStorageIn(mCurrentPlayingPath);

        if (storage == null || STORAGE_PORT.INVALID.equals(storage)) {
            mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
        } else if (!storage.equals(mCurrentDevice) && !STORAGE_PORT.STORAGE_ALL.equals(mCurrentDevice)) {
            mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
        } else if (!mMountedList.contains(mCurrentDevice)) {
            mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
        }
        AdayoVideoPlayerApplication.instance().saveLastDevice(mCurrentDevice);
        Trace.d(TAG, "[initPlayPath]:storage " + mCurrentDevice);
        mVideoListForBrowser = mPlayList = mListManager.getVideoList(mCurrentDevice);
        Trace.d(TAG, "[initPlayPath]: mPlayList.size()==" + mPlayList.size());
        mSelectedDevice = mCurrentDevice;

        if (!new File(mCurrentPlayingPath).exists()) {
            Trace.e(TAG, "save path not exitst !!! " + mCurrentPlayingPath);
            if (!isPlayListEmpty()) {
                clearCurrentPlayingInfo(mPlayList.get(0).mPath, 0, 0);
            } else {
                clearCurrentPlayingInfo("", 0, 0);
            }
            savePlayPath();
        } else {
            mCurrentPlayingIndex = -1;
            Trace.d(TAG, "[initPlayPath] mCurrentPlayPos== " + mCurrentPlayPos);
            mCurrentPlayPos = AdayoVideoPlayerApplication.instance().getLastPlayPosition();
            updatePlayIndex(mCurrentPlayingPath);
            if (mCurrentPlayingIndex == -1) {
                Trace.d(TAG, "[initPlayPath]:not in play list");
                addToPlayList(mCurrentPlayingPath);
            }
        }

    }

    private void updatePlayIndex(String path) {
        for (int i = 0; i < mPlayList.size(); i++) {
            if (mPlayList.get(i).mPath.equals(path)) {
                mCurrentPlayingIndex = i;
                break;
            }
        }
    }

    // 解决在源内mCurrentPlayingPath被UNMOUNTED置为空的情况
    private void checkSavePathInPlayList() {
        String path = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
        for (int i = 0; i < mPlayList.size(); i++) {
            if (mPlayList.get(i).mPath.equals(path)) {
                mCurrentPlayingPath = path;
            }
        }
    }

    public int getCurrentPos() {
        if (mMediaPlayer == null) {
            // notifyNotReady();
            return 0;
        }
        if (!canGetPosition()) {
            Trace.d(TAG, "[getCurrentPos]:can't get play pos");
            return -1;
        }
        mCurrentPlayPos = mMediaPlayer.getCurrentPosition();
        return mCurrentPlayPos;
    }

    public int getPlayState() {
        return mMediaPlayerState;
    }

    public ShuffleMode getShuffleMode() {
        return mShuffleMode;
    }

    public void switchShuffleMode() {
        if (ShuffleMode.OFF.equals(mShuffleMode)) {
            mShuffleMode = ShuffleMode.ON;
            if (RepeateMode.ONE.equals(getRepeatMode())) {
                mRepeatMode = RepeateMode.ALL;
                mMediaPlayer.setLooping(false);
                AdayoVideoPlayerApplication.instance().saveRepeatMode(mRepeatMode);
                notifyRepeatModeChanged();
            }
        } else {
            mShuffleMode = ShuffleMode.OFF;
        }
        AdayoVideoPlayerApplication.instance().saveShuffleMode(mShuffleMode);
        notifyShuffleModeChanged();
    }

    public RepeateMode getRepeatMode() {
        return mRepeatMode;
    }

    public void switchRepeatMode() {
        if (RepeateMode.ALL.equals(mRepeatMode)) {
            mRepeatMode = RepeateMode.ONE;

            if (ShuffleMode.ON.equals(mShuffleMode)) {
                mShuffleMode = ShuffleMode.OFF;
                AdayoVideoPlayerApplication.instance().saveShuffleMode(mShuffleMode);
                notifyShuffleModeChanged();
            }

        } else {
            mRepeatMode = RepeateMode.ALL;
        }
        AdayoVideoPlayerApplication.instance().saveRepeatMode(mRepeatMode);
        notifyRepeatModeChanged();

        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(RepeateMode.ONE.equals(mRepeatMode));
        }
    }

    private void notifyShuffleModeChanged() {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onShuffleModeChanged(mShuffleMode);
        }
    }

    private void notifyRepeatModeChanged() {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onRepeateModeChanged(mRepeatMode);
        }
    }

    private void notifyOnSeekComplete() {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onSeekCompletion();
        }
    }

    private void notifyPlayVideoChanged() {
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.playVideoChanged(mCurrentPlayingPath, canGetDuration() ? mMediaPlayer.getDuration() : 0);
        }

        if (mVideoListForBrowser.size() >= 0) {
            for (int i = 0; i < mVideoListForBrowser.size(); i++) {
                if (mVideoListForBrowser.get(i).mPath.equals(mCurrentPlayingPath)) {
                    notifyListSelectedChanged(i);
                    break;
                }
            }
        }
    }

    private void clearPlayInfo() {
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.playVideoChanged("", 0);
        }
        if (mVideoListForBrowser.size() > 0) {
            for (int i = 0; i < mVideoListForBrowser.size(); i++) {
                if (mVideoListForBrowser.get(i).mPath.equals(mCurrentPlayingPath)) {
                    notifyListSelectedChanged(i);
                    break;
                }
            }
        }
    }

    private void notifyPlayStateChanged() {

        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }

        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onPlayStateChanged(mMediaPlayerState);
        }
    }

    public int getDuration() {

        if (mMediaPlayer == null) {
            notifyNotReady();
            return 0;
        }

        if (isInPlayingMode()) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    /*
     * public ArrayList<VideoListItem> queryVidoeList(){ mPlayList.add(new
     * VideoListItem()); }
     */

    private void nothingToPlay() {
        Trace.e(TAG, "nothingToPlay");
        notifyPlayStateChanged();
        if (mMediaPlayer != null && isInPlayingMode()) {
            mediaPlayerStop();
        }
    }

    public void savePlayPath() {
        AdayoVideoPlayerApplication.instance().saveLastPlayPath(mCurrentPlayingPath);
    }

    public void savePlayPos() {
        if (mMediaPlayer == null) {
            notifyNotReady();
            return;
        }
        try {

            Trace.d(TAG, "[savePlayPos()]:  try to save Position " + mMediaPlayer.getCurrentPosition());
            if (mMediaPlayer.isPlaying() || mMediaPlayerState == PAUSED) {
                int position = getCurrentPos();
                if (position != -1) {
                    Trace.d(TAG, "[savePlayPos()]:  canSavePosition position==" + position);
                    AdayoVideoPlayerApplication.instance().saveLastPlayPosition(position);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private void setDevice2(STORAGE_PORT storage){
    // mCurrentDevice = storage;
    // AdayoVideoPlayerApplication.instance().saveLastDevice(storage);
    // onStorageSelected(storage);
    // }
    public void onStorageSelected(STORAGE_PORT storage) {
        Trace.d(TAG, "[onStorageSelected]:selected storage " + storage + " mCurrentDevice " + mCurrentDevice);
        Trace.d(TAG, "[onStorageSelected]:mMountedList==null? " + mMountedList);

        if (!STORAGE_PORT.STORAGE_ALL.equals(storage) && !mMountedList.contains(storage)) {
            Trace.d(TAG, "[onStorageSelected]:mounted list not has this storage " + storage);
            return;
        }

        // updateVideoPlayList(AdayoVideoPlayerApplication.instance().queryVideoPaths(storage));
        // mCurrentDevice = storage;
        mSelectedDevice = storage;
        if (!STORAGE_PORT.STORAGE_ALL.equals(storage)) {
            MediaObjectQueryInterface queryInterface = AdayoMediaScanner.getAdayoMediaScanner().getQueryer(storage);
            if (queryInterface == null) {
                Trace.d(TAG, "[onStorageSelected]:query interface is null!!!");
                return;
            }
            SCANNING_STATE state = queryInterface.getScanningState();
            if (SCANNING_STATE.NOT_START.equals(state) || SCANNING_STATE.SCANNING_FILE.equals(state)) {
                retVideoList(null, null, null, storage);
                return;
            }
        } else if (hasDeviceInScan()) {
            Trace.d(TAG, "[onStorageSelected]: hasDeviceInScan ");
            retVideoList(null, null, null, storage);
            return;
        }
        if (mListManager == null) {
            Trace.d(TAG, "[onStorageSelected]: mListManager ==null ?" + mListManager);
            return;
        }
        mVideoListForBrowser = mListManager.getVideoList(storage);
        Trace.d(TAG, "[onStorageSelected]: mVideoListForBrowser ==null? " + mVideoListForBrowser);
        if (mVideoListForBrowser.size() <= 0) {
            retVideoList(null, null, null, storage);
            return;
        }

        retVideoList(storage);
    }

    // private void notifyStorageScanning(STORAGE_PORT storage) {
    // Trace.d(TAG, "[notifyStorageScanning]:storage " + storage);
    // for(IVideoPlayStateChangedListener listener:mCallbackSet){
    // listener.storageScanning(storage);
    // }
    // }
    private void notifyListSelectedChanged(int playingIndex) {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onListSelectedChanged(playingIndex);
        }
    }

    public void startBrowser() {
        Trace.d(TAG, "[startBrowser]storage " + mCurrentDevice);
        onStorageSelected(mCurrentDevice);
    }

    public void onStorageScanEnd(final STORAGE_PORT storage) {

        mAsyncHandler.obtainMessage(MSG_STORAGE_SCAN_END, storage).sendToTarget();

    }

    private void updateListMangerWhenScanEnd(STORAGE_PORT storage) {
        Trace.d(TAG, "[updateListMangerWhenScanEnd]:storage " + storage.name());
        mListManager.updateAfterScanFileEnd(storage);
        mHandler.obtainMessage(MSG_STORAGE_SCAN_END, storage).sendToTarget();
    }

    private void updatePlayListWhenScanEnd(STORAGE_PORT storage) {
        Trace.d(TAG, "[updatePlayListWhenScanEnd]:storage " + storage.name() + " playing path " + mCurrentPlayingPath
                + " mCurrentDevice " + mCurrentDevice + " selected storage " + mSelectedDevice);
        setStorageState(storage, SCAN_FINISH);
        if (mSelectedDevice != null && mSelectedDevice.equals(storage)) {
            mVideoListForBrowser = mListManager.getVideoList(storage);
            retVideoList(storage);
        } else if (STORAGE_PORT.STORAGE_ALL.equals(mSelectedDevice)) {
            if (!hasDeviceInScan()) {
                mVideoListForBrowser = mListManager.getVideoList(mSelectedDevice);
                retVideoList(mSelectedDevice);
            }
        }
        if (mCurrentDevice != null && storage.equals(mCurrentDevice) || STORAGE_PORT.STORAGE_ALL.equals(mCurrentDevice)) {
            makePlayList(storage);
        }
        checkSavePathInPlayList();
        updatePlayIndex(mCurrentPlayingPath);

        boolean isInSource = MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager()
                .getCurrentRunningSource(SEAT_TYPE.front_seat));
        Trace.d(TAG, "[updatePlayListWhenScanEnd]:whichSource=="
                + SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(SEAT_TYPE.front_seat));
        Trace.d(TAG, "[updatePlayListWhenScanEnd]:isInSource==" + isInSource);
        Trace.d(TAG, "[updatePlayListWhenScanEnd]:isInPlayingMode()==" + isInPlayingMode());
        Trace.d(TAG, "[updatePlayListWhenScanEnd]:isPlayListEmpty()==" + isPlayListEmpty());

        if (isInSource && !isInPlayingMode() && !isPlayListEmpty()) {
            Trace.d(TAG, "[updatePlayListWhenScanEnd]:will play video");
            beginNewPlay();
        }
        notifyPlayVideoChanged();

        if (isMissMounted()) {
            Trace.d(TAG, "[updatePlayListWhenScanEnd]:is missing mounted");
            return;
        }

        if (!hasDeviceMounted()) {
            Trace.d(TAG, "[onStorageScanEnd]:no devices");
            notifyNoDevices();
            willCloseSource();
        } else if (hasDeviceInScan()) {
            Trace.d(TAG, "[onStorageScanEnd]:wait for scan callback");
        } else if (!hasVideoFiles()) {
            Trace.d(TAG, "[onStorageScanEnd]:no audio");
            notifyNoVideoFile();
            willCloseSource();
        }
    }

    private boolean isMissMounted() {
        return SourceSwitchManager.getSourceSwitchManager().isMissMeidaInsertFlag();
    }

    private void makePlayList(STORAGE_PORT storage) {
        Trace.d(TAG, "[makePlayList]:mCurrentDevice == " + mCurrentDevice);
        mPlayList = mListManager.getVideoList(mCurrentDevice);
        Trace.d(TAG, "[makePlayList]:isPlayListEmpty == " + isPlayListEmpty());
        if (isPlayListEmpty()) {
            // 没有视频文件
            if (!STORAGE_PORT.STORAGE_ALL.equals(mCurrentDevice)) {
                mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
                AdayoVideoPlayerApplication.instance().saveLastDevice(mCurrentDevice);
                mPlayList = mListManager.getVideoList(mCurrentDevice);
                Trace.d(TAG, "[makePlayList] mPlayList.size ==" + mPlayList.size());
            }
            mCurrentPlayingPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
            boolean savedPathExists = new File(mCurrentPlayingPath).exists();
            if (savedPathExists) {
                updatePlayIndex(mCurrentPlayingPath);
            } else if (!mPlayList.isEmpty()) {
                clearCurrentPlayingInfo(mPlayList.get(0).mPath, 0, 0);
            }
        }
    }

    private void addToPlayList(String path) {
        VideoBean bean = new VideoBean();
        bean.mPath = mCurrentPlayingPath;
        bean.mName = VideoUtils.getTitle(mCurrentPlayingPath);
        mPlayList.add(bean);
        Trace.d(TAG, "[addToPlayList] mPlayList.size ==" + mPlayList.size());
        mCurrentPlayingIndex = mPlayList.size() - 1;
    }

    private boolean hasDeviceInScan() {

        for (STORAGE_PORT storage : mMountedList) {
            if (getStorageState(storage) == MOUNTED || getStorageState(storage) == SCANING_FILE) {
                return true;
            }
        }
        return false;
    }

    private int getStorageState(STORAGE_PORT storage) {
        synchronized (mStorageScanState) {
            return mStorageScanState[storage.ordinal()];
        }
    }

    private void setStorageState(STORAGE_PORT storage, int state) {
        synchronized (mStorageScanState) {
            mStorageScanState[storage.ordinal()] = state;
        }
    }

    private void notifyBeginPlaying() {
        Trace.d(TAG, "[notifyBeginPlaying]");
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.beginNewPlaying();
        }
    }

    public void findPrevStorage() {

    }

    public STORAGE_PORT getCurrentDevice() {
        return mCurrentDevice;
    }

    public void removeDevice(STORAGE_PORT storage) {
        mListManager.storageRemoved(storage);
        mHandler.obtainMessage(MSG_STORAGE_UNMOUNT, storage).sendToTarget();
    }

    private void updatePlayListWhenRemoveStorage(STORAGE_PORT storage) {
        Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:mCurrenDevice " + mCurrentDevice);
        Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:mSelectedDevice " + mSelectedDevice);
        // AdayoVideoPlayerApplication.instance().saveLastSelectedDevice(mCurrentDevice);
        // notifyStorageUnmounted();
        boolean isInSource = MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager()
                .getCurrentRunningSource(SEAT_TYPE.front_seat));
        Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:is in video source " + isInSource);

        /**
         * 如果拔下来的设备是拔掉设备前播放的设备，那么在mediaStroageUnmount的时候，会把mCurrentDevice设置为ALL
         * 
         * 如果不是的话，mCurrentDevice有可能是ALL类型，也有可能不是
         * 
         */

        if (STORAGE_PORT.STORAGE_ALL.equals(mCurrentDevice)) {
            mPlayList = mListManager.getVideoList(mCurrentDevice);
            Trace.d(TAG, "[updatePlayListWhenRemoveStorage]: mPlayList.size()==" + mPlayList.size());
            if (isPlayListEmpty()) {
                if (hasDeviceInScan()) {
                    notifyOnLoading();
                }
            }
            updatePlayIndex(mCurrentPlayingPath);
            if (isInSource && !isInPlayingMode() && !isPlayListEmpty()) {
                beginNewPlay();
            }
        }
        notifyPlayVideoChanged();

        mVideoListForBrowser = mListManager.getVideoList(mSelectedDevice);
        retVideoList(mSelectedDevice);

        if (isInSleepSource()) {
            Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:in sleep source,not check");
            return;
        }

        if (!hasDeviceMounted()) {
            Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:no devices");
            notifyNoDevices();
            willCloseSource();
        } else if (hasDeviceInScan()) {
            Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:wait for scan callback");
        } else if (!hasVideoFiles()) {
            Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:no audio");
            notifyNoVideoFile();
            willCloseSource();
        }
    }

    private boolean isInSleepSource() {
        return MEDIA_SOURCE_ID.SLEEP.equals(SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(
                SEAT_TYPE.front_seat));
    }

    private void notifyOnLoading() {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.onLoading(mCurrentDevice);
        }
    }

    private void printMountedList() {
        for (STORAGE_PORT storage : mMountedList) {
            Trace.d(TAG, "[printMountedList]:" + storage);
        }
    }

    private void notifyNoDevices() {
        Trace.d(TAG, "[notifyNoDevices]");
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.noDevices();
        }
        willCloseSource();
    }

    private void notifyNoVideoFile() {
        Trace.d(TAG, "[notifyNoVideoFile]");
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.noVideoFileToPlay();
        }
        willCloseSource();
    }

    public void clearCurrentPlayingInfo(String playingPath, int pos, int playingIndex) {
        Trace.d(TAG, "[clearCurrentPlayingInfo]:pos " + pos);
        mCurrentPlayingPath = playingPath;
        mCurrentPlayPos = pos;
        mCurrentPlayingIndex = playingIndex;
    }

    public boolean isLegalVideoPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    private void clearPlayList() {
        mPlayList.clear();
    }

    /**
     * first start
     */
    public void firstStartWhenInSource() {

        Trace.d(TAG, "[firstStartWhenInSource]:");
        mShuffleMode = AdayoVideoPlayerApplication.instance().getSavedShuffleMode();
        mRepeatMode = AdayoVideoPlayerApplication.instance().getSavedRepeatMode();
        mCurrentDevice = AdayoVideoPlayerApplication.instance().getLastSavedDevice();
        mCurrentPlayPos = AdayoVideoPlayerApplication.instance().getLastPlayPosition();
        mCurrentPlayingPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();

        Trace.d(TAG, "[firstStartWhenInSource]:play pos " + mCurrentPlayPos);
        notifyRepeatModeChanged();
        notifyShuffleModeChanged();

        if (isMissMounted()) {
            Trace.d(TAG, "[firstStartWhenInSource]:is in miss storage,sleep device " + mSleepDevice);
            mSelectedDevice = mCurrentDevice = mSleepDevice;
            if (mSleepDevice != null) {
                AdayoVideoPlayerApplication.instance().saveLastDevice(mCurrentDevice);
                boolean deviceExist = checkIsMounted(mSleepDevice);
                Trace.d(TAG, "[firstStartWhenInSource]:deviceExist== " + deviceExist);
                if (!STORAGE_PORT.STORAGE_ALL.equals(mSleepDevice) && deviceExist) {
                    if (!mMountedList.contains(mSleepDevice)) {
                        Trace.d(TAG, "[firstStartWhenInSource]:sleep storage " + mSleepDevice
                                + " has mounted,but not in mounted list");
                        addToMountedList(mSleepDevice);
                    }
                    Trace.d(TAG, "[firstStartWhenInSource]:current device " + mCurrentDevice
                            + " will set to sleep deivce " + mSleepDevice);
                    boolean savedPathExist = false;
                    String savedPath = AdayoVideoPlayerApplication.instance().getLastSavePlayPath();
                    try {
                        savedPathExist = new File(savedPath).exists();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    if (getStorageState(mSleepDevice) == SCAN_FINISH) {
                        makePlayList(mSleepDevice);
                    } else if (savedPathExist) {
                        addToPlayList(savedPath);
                        Trace.d(TAG, "[firstStartWhenInSource]:saved path exist");
                    } else {
                        Trace.d(TAG, "[firstStartWhenInSource]:saved path not exist,wait for scan end");
                        return;
                    }

                } else {
                    // 休眠后设备不存在了
                    Trace.d(TAG, "[firstStartWhenInSource]:sleep storage " + mSleepDevice + " will wait for check");
                    Message msg = mHandler.obtainMessage(MSG_CHECK_STORAGE_STATE, mSleepDevice);
                    mHandler.sendMessageDelayed(msg, TIME_DELAY);
                    return;
                }
            }
        }

        if (mMountedList.isEmpty()) {
            notifyNoDevices();
            return;
        } else if (hasDeviceInScan()) {
            Trace.d(TAG, "[firstStartWhenInSource]:has storage in scan");
        } else if (!hasVideoFiles()) {
            notifyNoVideoFile();
            return;
        }
        Trace.d(TAG, "[firstStartWhenInSource]:saved path " + mCurrentPlayingPath);
        Trace.d(TAG, "[firstStartWhenInSource]:mShuffleMode " + mShuffleMode);
        Trace.d(TAG, "[firstStartWhenInSource]:mRepeatMode " + mRepeatMode);
        Trace.d(TAG, "[firstStartWhenInSource]:play pos " + mCurrentPlayPos);
        boolean savedPathExitst = new File(mCurrentPlayingPath).exists();
        if (savedPathExitst) {
            savedPathExitst =false;
            for (int i = 0; i < mPlayList.size(); i++) {
                if (mPlayList.get(i).mPath.equals(mCurrentPlayingPath)){
                    savedPathExitst=true;
                    Trace.d(TAG, "[firstStartWhenInSource]:saved path exists " + mCurrentPlayingPath);
                }
            }
        }
        
        if (mMediaPlayerState == STARTED) {
            return;
        }
        if (mMediaPlayerState == PAUSED) {
            play();
            notifyPlayVideoChanged();
        } else if (savedPathExitst) {
            beginNewPlay();
        } else {
            Trace.d(TAG, "[firstStartWhenInSource]:saved path not exists " + mCurrentPlayingPath);
            if (isPlayListEmpty()) {
                makePlayList(mCurrentDevice);
            }
            if (!isPlayListEmpty()) {
                clearCurrentPlayingInfo("", 0, 0);
                beginNewPlay();
            }
        }
    }

    private FilesStateChanged mFilesStateChanged = new FilesStateChanged() {

        @Override
        public void mediaStorageUnmounted(STORAGE_PORT storage) {
            Trace.d(TAG, "[mediaStorageUnmounted]:storage " + storage.name());
            Trace.d(TAG, "[mediaStorageUnmounted]:getStorageState(storage) " + getStorageState(storage));

            if (getStorageState(storage) == NOT_MOUNTED || isMissMounted()) {
                Trace.d(TAG, "[mediaStorageUnmounted]:isMissMounted " + isMissMounted());
                return;
            }
            setStorageState(storage, NOT_MOUNTED);

            if (mCurrentDevice != null && mCurrentDevice.equals(storage)) {
                mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
                AdayoVideoPlayerApplication.instance().saveLastDevice(mCurrentDevice);
            }
            mMountedList.remove(storage);
            printMountedList();
            STORAGE_PORT storage1 = com.adayo.an6v.ui.Utils.getWhichStorageIn(mCurrentPlayingPath);

            boolean isInSource = MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager()
                    .getCurrentRunningSource(SEAT_TYPE.front_seat));
            if (storage1 != null && storage1.equals(storage)) {
                stop();
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                    mMediaPlayerState = RELEASED;
                    mMediaPlayer = null;
                }
                if (isInSource) {
                    initMediaPlayer();
                    setSurface();
                }
                // 正在播放的设备被移除掉了，则当前播放的设备设为STORAGE_ALL，并且查询其它设备的文件
                mCurrentDevice = STORAGE_PORT.STORAGE_ALL;
                AdayoVideoPlayerApplication.instance().saveLastDevice(mCurrentDevice);
                clearCurrentPlayingInfo("", 0, -1);
                clearPlayList();
            }

            if (mSelectedDevice != null && mSelectedDevice.equals(storage)) {
                Trace.d(TAG, "[updatePlayListWhenRemoveStorage]:selected storage equals remove storage " + storage);
                mSelectedDevice = mCurrentDevice;
            }

            mAsyncHandler.obtainMessage(MSG_STORAGE_UNMOUNT, storage).sendToTarget();
        }

        @Override
        public void mediaStorageMounted(STORAGE_PORT storage) {

            addToMountedList(storage);
            setStorageState(storage, MOUNTED);
            Trace.d(TAG, "[mediaStorageMounted]:storage ==" + storage);
            if (countDownToCloseSource()) {
                Trace.d(TAG, "[mediaStorageMounted]:will cancel close source");
                removeToCloseSource();
            }

            // UpdatePlayListThread thread = mUpdateThreadMap.get(storage);
            // if(thread != null){
            // thread.exit();
            // }
            // thread = new UpdatePlayListThread(storage);
            // mUpdateThreadMap.put(storage, thread);
        }

        @Override
        public void fileScanStart(STORAGE_PORT storage) {
            setStorageState(storage, SCANING_FILE);

        }

        @Override
        public void fileScanID3End(STORAGE_PORT storage) {
        }

        @Override
        public void fileScanEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileScanEnd]:storage " + storage);

        }

        @Override
        public void fileSaveEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileSaveEnd]:storage " + storage.name());
            onStorageScanEnd(storage);

        }

        @Override
        public void fileParseThumbnailEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileParseThumbnailEnd]:storage " + storage.name());
        }
    };

    // public Drawable getThumbnail(int fileId){
    // String thumbPath = mVideoThumbnailMap.get(fileId);
    // return getThumbnailByThumbnailPath(thumbPath);
    // }

    public Drawable getThumbnailByFullPath(String fullPath) {

        String thumbnailPath = CommonUtil.getThumbnailFilePath(fullPath);

        return getThumbnailByThumbnailPath(thumbnailPath);
    }

    protected void removeToCloseSource() {
        mHandler.removeMessages(MSG_CLOSE_SOURCE);
    }

    protected boolean countDownToCloseSource() {
        return mHandler.hasMessages(MSG_CLOSE_SOURCE);
    }

    private Drawable getThumbnailByThumbnailPath(String thumbnailPath) {
        Drawable drawable = null;
        Bitmap bmp = BitmapFactory.decodeFile(thumbnailPath);

        if (bmp == null) {
            return null;
        }
        drawable = new BitmapDrawable(AdayoVideoPlayerApplication.instance().getResources(), bmp);

        return drawable;

    }

    private void retVideoList(STORAGE_PORT storage) {
        int playingIndex = -1;
        if (mVideoListForBrowser != null && mVideoListForBrowser.size() >= 0) {
            String[] titles = new String[mVideoListForBrowser.size()];
            int[] fileIds = new int[mVideoListForBrowser.size()];
            String[] fullPaths = new String[mVideoListForBrowser.size()];
            for (int i = 0; i < mVideoListForBrowser.size(); i++) {
                VideoBean bean = mVideoListForBrowser.get(i);
                titles[i] = bean.mName;
                fileIds[i] = bean.mFileId;
                fullPaths[i] = bean.mPath;
                if (bean.mPath.equals(mCurrentPlayingPath)) {
                    playingIndex = i;
                }
            }
            retVideoList(titles, fileIds, fullPaths, storage);
        } else {
            retVideoList(null, null, null, storage);
        }
        if (playingIndex != -1) {
            notifyListSelectedChanged(playingIndex);
        }
    }

    private void retVideoList(String[] titles, int[] fileIds, String[] fullPaths, STORAGE_PORT storage) {
        for (IVideoPlayStateChangedListener listener : mCallbackSet) {
            listener.retVideoList(titles, fileIds, fullPaths, storage);
        }
    }

    public void surfaceHadBeenDestroyed() {
        mSurfaceHolder = null;
        mSurfaceView = null;
    }

    public void onSourceBegin() {
        Trace.d(TAG, "[onSourceBegin]");
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        if (mSurfaceView != null) {
            setup(mSurfaceView);
        }
        requestFocus();
        firstStartWhenInSource();
    }

    public void onSourceEnd() {
        Trace.d(TAG, "[onSourceEnd]:play pos " + mCurrentPlayPos);
        stop();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
        mMediaPlayerState = RELEASED;
    }

    OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {

            Trace.d(TAG, "onAudioFocusChange");

            if (mMediaPlayer == null) {
                return;
            }

            try {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Trace.d(TAG, "AUDIOFOCUS_GAIN");
                        Trace.d(TAG, "mMediaPlayerState==" + mMediaPlayerState);

                        mMediaPlayer.setVolume(1.0f, 1.0f);

                        if (mMediaPlayerState == PAUSED) {
                            mMediaPlayer.start();
                            mMediaPlayerState = STARTED;
                            Trace.w(TAG, "---play---");
                            startPlayCanBox();
                        } else {
                            play();
                        }

                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        Trace.d(TAG, "AUDIOFOCUS_LOSS");
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                            mMediaPlayerState = PAUSED;
                            reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_PAUSE);
                        }

                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Trace.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");

                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                            mMediaPlayerState = PAUSED;
                            reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_PAUSE);
                        }

                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Trace.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                            mMediaPlayerState = PAUSED;
                            reportCanbox(CanboxConstantsDef.CANBOX_MEDIA_PAUSE);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public boolean hasDeviceMounted() {
        return !mMountedList.isEmpty();
    }

    public boolean hasVideoFiles() {
        long time = System.currentTimeMillis();
        boolean flag = AdayoMediaManager.getMediaManager(AdayoVideoPlayerApplication.instance()).hasVideoFiles();
        Trace.d(TAG, "[hasVideoFiles]:spend time " + (System.currentTimeMillis() - time) + " has video " + flag);
        return flag;
    }

    public int getPlayingIndex() {
        return mCurrentPlayingIndex;
    }

    public int getPlayListCount() {
        return mPlayList.size();
    }

    public STORAGE_PORT getStorage() {
        Trace.d(TAG, "[getStorage]:" + mCurrentDevice);
        return mCurrentDevice;
    }

    public void stopInPowerOff() {
        Trace.d(TAG, "[stopInPowerOff]");
        stop();
    }

    public boolean isInScanState(STORAGE_PORT storage) {

        if (STORAGE_PORT.STORAGE_ALL.equals(storage)) {
            return hasDeviceInScan();
        }

        int state = getStorageState(storage);

        return state == MOUNTED || state == SCANING_FILE;

        // MediaObjectQueryInterface queryInterface = getQuery(storage);
        // if(queryInterface == null){
        // return false;
        // }
        // SCANNING_STATE state = queryInterface.getScanningState();
        // return state.equals(SCANNING_STATE.NOT_START) ||
        // state.equals(SCANNING_STATE.SCANNING_FILE);
    }

    public void startPlay() {
        Trace.d(TAG, "[startPlay]:media state " + mMediaPlayerState);

        if (mMediaPlayer == null) {// 接收到系统的MediaPlayer资源之后释放掉了，所以为NULL了
            notifyNotReady();
        }
        if (canStarted()) {
            mediaPlayerStart();
        }
        notifyPlayStateChanged();
    }

    private void mediaPlayerStart() {
        mMediaPlayer.start();
        Trace.w(TAG, "---play---");
        mMediaPlayerState = STARTED;
        startPlayCanBox();
    }

    private boolean canStarted() {
        return mMediaPlayerState == PREPARED || mMediaPlayerState == STARTED || mMediaPlayerState == PAUSED
                || mMediaPlayerState == COMPLETED;
    }

    public STORAGE_PORT getSelectedDevice() {
        return mSelectedDevice;
    }

    public void activityOnPause() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    public boolean canShowPlayIndex() {
        return !isPlayListEmpty();
    }

    private boolean isPlayListEmpty() {
        return mPlayList == null || mPlayList.size() <= 0;
    }

    public void checkVideoSupportState() {
        if (mMediaPlayer != null && isInPlayingMode()) {
            if ((mCapable & AtcMediaPlayer.AUDIO_CODEC_UNSUPPORT) != 0) {
                Trace.d(TAG, "checkVideoSupportState  AUDIO_CODEC_UNSUPPORT" );
                if (!mHandler.hasMessages(MSG_ON_ERROE_NEXT)) {
                    mHandler.sendEmptyMessageDelayed(MSG_ON_ERROE_NEXT, TIME_SHOW_ERROR);
                }
                showToast(R.string.unsupport_audio_type);
            } else if ((mCapable & AtcMediaPlayer.VIDEO_CODEC_UNSUPPORT) != 0) {
                Trace.d(TAG, "checkVideoSupportState  VIDEO_CODEC_UNSUPPORT" );
                showToast(R.string.unsupport_video_type);
            }
        }
    }

    public void canShowToast(final boolean canShow) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCanShowToast = canShow;
                if (!mCanShowToast && mToast != null) {
                    mToast.cancel();
                }
            }
        });
    }

    public void saveStorageWhenAccOff() {
        mSleepDevice = mCurrentDevice;
        Trace.d(TAG, "[saveStorageWhenAccOff]:save sleep storage " + mSleepDevice);
    }

    public void sourceUpdate() {
        play();
    }

    public void playFF() {
        Trace.d(TAG, "[playFF]: atcFFMediaPlayer == " + mAtcFFMediaPlayer);
        if (!isInPlayingMode()) {
            return;
        }
        mHandler.removeMessages(MSG_FF_PLAY);
        mIsFFPlay = true;
        mPlayBackValue = 0;
        try {
            mAtcFFMediaPlayer = new AtcMediaPlayer(mMediaPlayer);
            mAtcFFMediaPlayer.setPlaybackRate(AtcMediaPlayer.MEDIA_PLAYBACK_FF_2X);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        mHandler.sendEmptyMessageDelayed(MSG_FF_PLAY, TIME_FF_PLAY);
    }

    public void playRW() {
        Trace.d(TAG, "[playRW]: atcFFMediaPlayer == " + mAtcFFMediaPlayer);
        if (!isInPlayingMode()) {
            return;
        }
        mHandler.removeMessages(MSG_FF_PLAY);
        mIsFFPlay = false;
        mPlayBackValue = 0;
        try {
            mAtcFFMediaPlayer = new AtcMediaPlayer(mMediaPlayer);
            mAtcFFMediaPlayer.setPlaybackRate(AtcMediaPlayer.MEDIA_PLAYBACK_RW_2X);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        mHandler.sendEmptyMessageDelayed(MSG_FF_PLAY, TIME_FF_PLAY);
    }

    public void setRealPlay() {
        Trace.d(TAG, "[setRealPlay]: atcFFMediaPlayer == " + mAtcFFMediaPlayer);
        if (!isInPlayingMode()) {
            return;
        }
        mHandler.removeMessages(MSG_FF_PLAY);
        try {
            mAtcFFMediaPlayer = new AtcMediaPlayer(mMediaPlayer);
            mAtcFFMediaPlayer.setPlaybackRate(AtcMediaPlayer.MEDIA_PLAYBACK_NORMAL);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
