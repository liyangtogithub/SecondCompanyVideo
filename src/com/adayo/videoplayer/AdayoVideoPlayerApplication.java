package com.adayo.videoplayer;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import com.adayo.mediaScanner.AdayoMediaManager;
import com.adayo.mediaScanner.MediaScannerInterface;
import com.adayo.mediaScanner.MediaScannerInterface.FilesStateChanged;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.mediaScanner.ScannerManager;
import com.adayo.midware.constant.ServiceConstants;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.SourceSwitchConstant.SourceActionType;
import com.adayo.midware.constant.channelManagerDef.MEDIA_SOURCE_ID;
import com.adayo.midware.constant.channelManagerDef.SEAT_TYPE;
import com.adayo.midwareproxy.sourceswitch.SourceActionListener;
import com.adayo.midwareproxy.sourceswitch.SourceSwitchApplication;
import com.adayo.midwareproxy.sourceswitch.SourceSwitchManager;
import com.adayo.midwareproxy.sourceswitch.SourceUIFinishListener;
import com.adayo.videoplayer.Constants.RepeateMode;
import com.adayo.videoplayer.Constants.ShuffleMode;
import com.adayo.videoplayer.core.VideoPlayController;
import com.adayo.videoplayer.db.VideoBean;
import com.adayo.videoplayer.receivers.SWCKeyReceiver;

public class AdayoVideoPlayerApplication extends SourceSwitchApplication {

    protected static final String TAG = "myvideo AdayoVideoPlayerApplication";
    private static AdayoVideoPlayerApplication mMe;
    private static final int CONNECT_TIME = 500;
    public VideoPlayController mVideoPlayControler;
    private Set<FilesStateChanged> mFilesStateChangedSet = new HashSet<MediaScannerInterface.FilesStateChanged>();
    private int mTransactionID;

    enum HandlerMsg {
        mediaStorageUnmounted, mediaStorageMounted, fileScanStart, fileScanID3End, fileScanEnd, fileSaveEnd, fileParseThumbnail
    }

    private SWCKeyReceiver mSwcKeyReceivers;
    private AccOnOffReceiver mAccOnOffReceiver;
    private PowerOffBroadcastReceiver mPowerOffBroadcastReceiver;
    private ResetBroadcastReceiver mResetBroadcastReceiver;
    volatile private boolean mIsInitDone = false;
    private LocaleChangedBroadcastReceiver mLocaleChangedBroadcastReceiver;
    private boolean mIsPausedBeforeSleep = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[onCreate]");
        CrashHandler crashHandler = CrashHandler.getInstance();  
        crashHandler.init(getApplicationContext());  
        mMe = this;
        mSaveSp = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);// 这句要放在最前面
        mSwcKeyReceivers = new SWCKeyReceiver();
        registerResetBroadcast();
        mLocaleChangedBroadcastReceiver = new LocaleChangedBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mLocaleChangedBroadcastReceiver, intentFilter);
        // Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        // mPowerOffBroadcastReceiver = new PowerOffBroadcastReceiver();
        // IntentFilter filter = new IntentFilter();
        // filter.addAction(ServiceConstants.DEVICE_POWEROFF_ACTION);
        // registerReceiver(mPowerOffBroadcastReceiver, filter);

        while (!ScannerManager.getScannerManager().isServiceConnected()) {
            Trace.d(TAG, "[onCreate]:ScannerService not connected");
            try {
                Thread.sleep(CONNECT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while (SourceSwitchManager.getSourceSwitchManager() == null) {
            Trace.d(TAG, "[onCreate]:SourceSwitchManager is null");
            try {
                Thread.sleep(CONNECT_TIME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (!SourceSwitchManager.getSourceSwitchManager().isServiceConnected()) {
            Trace.d(TAG, "[onCreate]:SourceSwitcherService not connected");
            try {
                Thread.sleep(CONNECT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SourceSwitchManager.getSourceSwitchManager().setSourceUIFinishListener(MEDIA_SOURCE_ID.VIDEO,
                mSourceUIFinishListener);
        initVideoPlayControler();
        ScannerManager.getScannerManager().registerCallBack(mFilesStateChanged);

        SourceSwitchManager.getSourceSwitchManager().setSourceActionListener(MEDIA_SOURCE_ID.VIDEO,
                new SourceActionListener() {
                    @Override
                    public void onAction(SourceActionType sourceActionType, int transactionID, Bundle bundle) {
                        switch (sourceActionType) {
                            case RUN:
                                Trace.d(TAG, "source changed to video,thread id " + Thread.currentThread().getId());
                                mTransactionID = transactionID;
                                sourceBegin();
                                SourceSwitchManager.getSourceSwitchManager().runResponse(MEDIA_SOURCE_ID.VIDEO,
                                        mTransactionID);
                                break;
                            case STOP:
                                Trace.d(TAG, "video source leave away");
                                mTransactionID = transactionID;
                                mVideoPlayControler.abandonFocus();
                                onSourceEnd();
                                unregisterSWCKeyReceiver();
                                SourceSwitchManager.getSourceSwitchManager().stopResponse(MEDIA_SOURCE_ID.VIDEO,
                                        mTransactionID);
                                break;
                            case UPDATE:
                                Trace.d(TAG, "[UPDATE]:video source update");
                                int state = mVideoPlayControler.requestFocus();

                                if (state == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                                    Trace.d(TAG, "focus has got");
                                    mVideoPlayControler.sourceUpdate();
                                } else {
                                    Trace.d(TAG, "[UPDATE]:no focus");
                                }
                                SourceSwitchManager.getSourceSwitchManager().updateResponse(MEDIA_SOURCE_ID.VIDEO,
                                        transactionID);
                                break;
                            default:
                                break;
                        }
                    }
                });
        registerAccOnOffReceiver();

    }

    private void sourceBegin() {
        Trace.d(TAG, "[SOURCE_BEGIN]: thread " + Thread.currentThread().getId());
        initVideoPlayControler();
        registerSWCKeyReceiver();
    }

    private void registerAccOnOffReceiver() {
        mAccOnOffReceiver = new AccOnOffReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceConstants.DEVICE_ACCOFF_ACTION);
        filter.addAction(ServiceConstants.DEVICE_ACCON_ACTION);
        registerReceiver(mAccOnOffReceiver, filter);
    }

    UncaughtExceptionHandler mUncaughtExceptionHandler = new UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Trace.e(TAG, "uncaughtException " + ex);
            Process.killProcess(Process.myPid());
        }
    };
    private SharedPreferences mSaveSp;

    @Override
    public void onTerminate() {
        super.onTerminate();
        ScannerManager.getScannerManager().unRegisterCallBack(mFilesStateChanged);
        unregisterReceiver(mPowerOffBroadcastReceiver);
        unregisterReceiver(mAccOnOffReceiver);
        unregisterReceiver(mResetBroadcastReceiver);
        unregisterReceiver(mLocaleChangedBroadcastReceiver);
    }

    public void onSourceEnd() {
        if (mIsInitDone) {
            mVideoPlayControler.onSourceEnd();
        }
    }

    public void initVideoPlayControler() {

        if (mVideoPlayControler == null) {
            mVideoPlayControler = new VideoPlayController();
        } else {
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "stop");
            getBaseContext().sendBroadcast(i);
            if (mIsInitDone) {
                mVideoPlayControler.onSourceBegin();
            }
        }
    }

    public static AdayoVideoPlayerApplication instance() {
        return mMe;
    }

    public String getLastSavePlayPath() {
        return mSaveSp.getString(Constants.SP_KEY_PLAY_PATH, "");
    }

    // public long getLastSavePlayPosition(){
    //
    // return mSaveSp.getLong(Constants.SP_KEY_PLAY_POSITION, 0);
    // }

    public RepeateMode getSavedRepeatMode() {
        return RepeateMode.valueOf(mSaveSp.getString(Constants.SP_KEY_REPEAT_MODE, RepeateMode.ALL.name()));
    }

    public ShuffleMode getSavedShuffleMode() {
        return ShuffleMode.valueOf(mSaveSp.getString(Constants.SP_KEY_SHUFFLE_MODE, ShuffleMode.OFF.name()));
    }

    public STORAGE_PORT getLastSavedDevice() {
        return STORAGE_PORT.valueOf(mSaveSp.getString(Constants.SP_KEY_DEVICE,
                MediaScannerInterface.STORAGE_PORT.STORAGE_ALL.name()));
    }

    public void saveLastPlayPath(String path) {
        mSaveSp.edit().putString(Constants.SP_KEY_PLAY_PATH, path).commit();
    }

    public void saveShuffleMode(ShuffleMode mode) {
        mSaveSp.edit().putString(Constants.SP_KEY_SHUFFLE_MODE, mode.name()).commit();
    }

    public void saveRepeatMode(RepeateMode mode) {
        mSaveSp.edit().putString(Constants.SP_KEY_REPEAT_MODE, mode.name()).commit();
    }

    public void saveLastDevice(STORAGE_PORT port) {
        mSaveSp.edit().putString(Constants.SP_KEY_DEVICE, port.name()).commit();
    }

    public void saveLastPlayPosition(int pos) {
        try {
            Log.d(TAG, "[saveLastPlayPosition]:pos " + pos);
            mSaveSp.edit().putInt(Constants.SP_KEY_PLAY_POSITION, pos).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getLastPlayPosition() {
        return mSaveSp.getInt(Constants.SP_KEY_PLAY_POSITION, 0);
    }

    public String[] queryVideoPaths(STORAGE_PORT storage) {
        Trace.d(TAG, "[queryVideoPaths]:" + storage.name());
        String[] paths = null;
        // paths = new String[]{
        // "/mnt/sdcard/1.mp4","/mnt/sdcard/2.mp4","/mnt/sdcard/3.mp4"
        // // "/mnt/sdcard/1.mp4"
        // };
        Cursor cursor = null;
        try {
            cursor = AdayoMediaManager.getMediaManager(getApplicationContext()).getAllVideoPaths(storage.name());
            if (cursor == null) {
                return null;
            }
            paths = new String[cursor.getCount()];
            int idx = 0;
            while (cursor.moveToNext()) {
                paths[idx++] = cursor.getString(cursor.getColumnIndex(AdayoMediaManager.VIDEO_PATH_NAME));
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    public List<VideoBean> queryVideoBeansList(STORAGE_PORT storage) {
        Trace.d(TAG, "[queyVideoBeans]:" + storage.name());
        List<VideoBean> allVideoBeans = new ArrayList<VideoBean>();
        Cursor cursor = null;
        try {
            cursor = AdayoMediaManager.getMediaManager(getApplicationContext()).getAllVideoPaths(storage.name());
            if (cursor == null) {
                return allVideoBeans;
            }
            int pathCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_PATH_NAME);
            int nameCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_FILE_NAME);
            int namePyCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_FILE_NAME_PY);
            int fileIdCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_ID);
            while (cursor.moveToNext()) {
                VideoBean bean = new VideoBean();
                bean.mPath = cursor.getString(pathCol);
                bean.mName = cursor.getString(nameCol);
                bean.mNamePy = cursor.getString(namePyCol);
                bean.mFileId = cursor.getInt(fileIdCol);
                allVideoBeans.add(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return allVideoBeans;
    }

    public VideoBean[] queryVideoBeans(STORAGE_PORT storage) {
        Trace.d(TAG, "[queyVideoBeans]:" + storage.name());
        // String[] paths = null;
        // paths = new String[]{
        // "/mnt/sdcard/1.mp4","/mnt/sdcard/2.mp4","/mnt/sdcard/3.mp4"
        // // "/mnt/sdcard/1.mp4"
        // };
        VideoBean[] allVideoBeans = null;
        Cursor cursor = null;
        try {
            cursor = AdayoMediaManager.getMediaManager(getApplicationContext()).getAllVideoPaths(storage.name());
            if (cursor == null) {
                return null;
            }
            allVideoBeans = new VideoBean[cursor.getCount()];
            int idx = 0;
            int pathCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_PATH_NAME);
            int nameCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_FILE_NAME);
            int namePyCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_FILE_NAME_PY);
            int fileIdCol = cursor.getColumnIndex(AdayoMediaManager.VIDEO_ID);
            while (cursor.moveToNext()) {
                allVideoBeans[idx] = new VideoBean();
                allVideoBeans[idx].mPath = cursor.getString(pathCol);
                allVideoBeans[idx].mName = cursor.getString(nameCol);
                allVideoBeans[idx].mNamePy = cursor.getString(namePyCol);
                allVideoBeans[idx].mFileId = cursor.getInt(fileIdCol);
                idx++;
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return allVideoBeans;
    }

    public VideoPlayController getVideoPlayControler() {
        if (mVideoPlayControler == null) {
            initVideoPlayControler();
        }
        return mVideoPlayControler;
    }

    public void registerFileStateChanged(FilesStateChanged changed) {
        mFilesStateChangedSet.add(changed);
    }

    public void unregsterFileStateChanged(FilesStateChanged changed) {
        mFilesStateChangedSet.remove(changed);
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            HandlerMsg msgId = HandlerMsg.values()[msg.what];

            switch (msgId) {
                case mediaStorageUnmounted:
                    Trace.d(TAG, "[mediaStorageUnmounted]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.mediaStorageUnmounted((STORAGE_PORT) msg.obj);
                    }

                    break;
                case mediaStorageMounted:
                    Trace.d(TAG, "[mediaStorageMounted]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.mediaStorageMounted((STORAGE_PORT) msg.obj);
                    }

                    break;
                case fileScanStart:
                    Trace.d(TAG, "[fileScanStart]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.fileScanStart((STORAGE_PORT) msg.obj);
                    }

                    break;
                case fileScanID3End:
                    Trace.d(TAG, "[fileScanID3End]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.fileScanID3End((STORAGE_PORT) msg.obj);
                    }

                    break;
                case fileScanEnd:
                    Trace.d(TAG, "[fileScanEnd]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.fileScanEnd((STORAGE_PORT) msg.obj);
                    }

                    break;
                case fileSaveEnd:
                    Trace.d(TAG, "[fileSaveEnd]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        changed.fileSaveEnd((STORAGE_PORT) msg.obj);
                    }

                    break;
                case fileParseThumbnail:

                    Trace.d(TAG, "[fileParseThumbnailEnd handle]");
                    for (FilesStateChanged changed : mFilesStateChangedSet) {
                        Trace.d(TAG,
                                "[fileParseThumbnailEnd] mFilesStateChangedSet.size()" + mFilesStateChangedSet.size());
                        changed.fileParseThumbnailEnd((STORAGE_PORT) msg.obj);
                    }

                    break;
                default:
                    break;
            }
        }
    };

    FilesStateChanged mFilesStateChanged = new FilesStateChanged() {

        @Override
        public void mediaStorageUnmounted(STORAGE_PORT storage) {
            Trace.d(TAG, "[mediaStorageUnmounted]:" + storage.name());
            mHandler.obtainMessage(HandlerMsg.mediaStorageUnmounted.ordinal(), storage).sendToTarget();

        }

        @Override
        public void mediaStorageMounted(STORAGE_PORT storage) {
            mHandler.obtainMessage(HandlerMsg.mediaStorageMounted.ordinal(), storage).sendToTarget();
        }

        @Override
        public void fileScanStart(STORAGE_PORT storage) {
            mHandler.obtainMessage(HandlerMsg.fileScanStart.ordinal(), storage).sendToTarget();
        }

        @Override
        public void fileScanID3End(STORAGE_PORT storage) {
            mHandler.obtainMessage(HandlerMsg.fileScanID3End.ordinal(), storage).sendToTarget();
        }

        @Override
        public void fileScanEnd(STORAGE_PORT storage) {
            mHandler.obtainMessage(HandlerMsg.fileScanEnd.ordinal(), storage).sendToTarget();
        }

        @Override
        public void fileSaveEnd(STORAGE_PORT storage) {
            mHandler.obtainMessage(HandlerMsg.fileSaveEnd.ordinal(), storage).sendToTarget();
        }

        @Override
        public void fileParseThumbnailEnd(STORAGE_PORT storage) {
            Trace.d(TAG, "[fileParseThumbnailEnd]: ");
            mHandler.obtainMessage(HandlerMsg.fileParseThumbnail.ordinal(), storage).sendToTarget();
        }
    };
    private Set<VideoMainActivity> mActivitys = new HashSet<VideoMainActivity>();

    public void registerSWCKeyReceiver() {
        IntentFilter filter = new IntentFilter(SettingConstantsDef.MCU_KEY_IND_ACTION);
        registerReceiver(mSwcKeyReceivers, filter);
    }

    public void unregisterSWCKeyReceiver() {
        unregisterReceiver(mSwcKeyReceivers);
    }

    synchronized public HashMap<Integer, String> queryThumbnails() {
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        Cursor cursor = null;
        try {
            cursor = AdayoMediaManager.getMediaManager(getApplicationContext()).getAllThumbnail();
            if (cursor != null && cursor.getCount() > 0) {

                Trace.w(TAG, "[queryThumbnails]:get " + cursor.getCount() + " thumbnails");

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(AdayoMediaManager.THUMBNAIL_FILE_ID));
                    String path = cursor.getString(cursor.getColumnIndex(AdayoMediaManager.THUMBNAIL_PATH_NAME));
                    map.put(id, path);
                }
            } else {
                Trace.w(TAG, "[queryThumbnails]:can't not get thumbnails " + cursor == null ? "null" : "count 0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return map;
    }


    public int queryVideoFilesCount(STORAGE_PORT storage) {
        Cursor cursor = null;
        try {
            cursor = AdayoMediaManager.getMediaManager(getApplicationContext()).getAllVideoPaths(storage.name());
            if (cursor == null){
                return 0;
            }
            return cursor.getCount();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cursor != null){
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    class PowerOffBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mVideoPlayControler.stopInPowerOff();
        }

    }

    public boolean isInitDone() {
        return mIsInitDone;
    }

    public void addActivity(VideoMainActivity videoMainActivity) {
        mActivitys.add(videoMainActivity);
    }

    public void removeActivity() {
        mActivitys.clear();
    }

    public void onInitDone() {
        mIsInitDone = true;
        if (mActivitys != null) {
            for (VideoMainActivity mActivity : mActivitys) {
                if (mActivity != null){
                    mActivity.onInitDone();
                }
            }
        }

        if (MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(
                SEAT_TYPE.front_seat))) {
            Trace.d(TAG, "[onCreate]:is in video");
            sourceBegin();
        }
    }

    /*
     * oncreate -- a onresumed --- a onstop -- b ondestroyed -- b onpaused -- a
     * onstop -- a
     */
    SourceUIFinishListener mSourceUIFinishListener = new SourceUIFinishListener() {

        @Override
        public void notifyUIFinish(Bundle bundle, int transactionID) {
            Trace.d(TAG, "[notifyUIFinish]:mActivitys .size()== " + (mActivitys.size()));
            try {
                for (VideoMainActivity mActivity : mActivitys) {
                    Trace.d(TAG, "[notifyUIFinish]:activity is null " + (mActivity == null));
                    if (mActivity != null){
                        mActivity.finish();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            SourceSwitchManager.getSourceSwitchManager().UIFinishResponse(MEDIA_SOURCE_ID.MUSIC, transactionID);
        }
    };

    private boolean isInVideoSource() {
        return MEDIA_SOURCE_ID.VIDEO.equals(SourceSwitchManager.getSourceSwitchManager().getCurrentRunningSource(
                SEAT_TYPE.front_seat));
    }

    private void registerResetBroadcast() {
        IntentFilter filter = new IntentFilter(SettingConstantsDef.ACTION_FACTORY_RESET);
        mResetBroadcastReceiver = new ResetBroadcastReceiver();
        registerReceiver(mResetBroadcastReceiver, filter);
    }

    class AccOnOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Trace.d(TAG, "[AccOnOffReceiver]:action:" + action);

            mVideoPlayControler.saveStorageWhenAccOff();
            if (!isInVideoSource()) {
                Trace.d(TAG, "[AccOnOffReceiver]:not in video source");
                return;
            }
            if (ServiceConstants.DEVICE_ACCON_ACTION.equals(action)) {
                    if ( !mIsPausedBeforeSleep) {
                        mVideoPlayControler.startPlay();
                }
            } else if (ServiceConstants.DEVICE_ACCOFF_ACTION.equals(action)) {
                mIsPausedBeforeSleep = mVideoPlayControler.isPaused();
                mVideoPlayControler.pause();
            }
        }
    }

    class ResetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Trace.d(TAG, "[onReceive]");
            clearSharedPrefes();
        }

        public void clearSharedPrefes() {
            File file = getCacheDir();
            File parentFile = file.getParentFile();
            String prefsPath = parentFile.getAbsolutePath() + File.separator + "shared_prefs";
            File prefsDir = new File(prefsPath);
            Log.d(TAG, "[clearSharedPrefes]" + prefsDir.getAbsolutePath());
            File[] files = prefsDir.listFiles();

            if (files == null){
                return;
            }
            for (File prefsFile : files) {
                String name = prefsFile.getName();
                int idx = name.lastIndexOf(".xml");
                if (idx < 0){
                    continue;
                }
                name = name.substring(0, idx);
                Log.d(TAG, "name " + name);
                SharedPreferences sp = getSharedPreferences(name, 0);
                sp.edit().clear().commit();
            }
        }
    }

    class LocaleChangedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isUpdateLauncher = intent.getBooleanExtra("isUpdateLauncher", false);
            Log.e(TAG, "locale changed,will finish video activity,isUpdateLauncher " + isUpdateLauncher);
            if (isUpdateLauncher){
                return;
            }
            try {
                for (VideoMainActivity mActivity : mActivitys) {
                    if (mActivity != null){
                        mActivity.finish();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
