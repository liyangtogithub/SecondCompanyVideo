package com.adayo.videoplayer.interfaces;

import android.graphics.Bitmap;

import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.videoplayer.Constants;
import com.adayo.videoplayer.Constants.RepeateMode;
import com.adayo.videoplayer.Constants.ShuffleMode;

public interface IVideoPlayStateChangedListener {
    public void onPlayStateChanged(int playState);

    public void playVideoChanged(String playingPath, int duration);

    public void onSeekCompletion();

    public void onShuffleModeChanged(ShuffleMode mode);

    public void onRepeateModeChanged(RepeateMode mode);

    public void retVideoList(String[] titles, int[] fileIds, String[] fullPaths, STORAGE_PORT storage);

    public void onListSelectedChanged(int position);

    public void onTimedText(String text, Bitmap bmp);

    public void isSeekable(boolean isSeekable);

    public void noVideoFileToPlay();

    public void noDevices();

    public void storageScanning(STORAGE_PORT storage);

    public void beginNewPlaying();

    public void storageUnmounted();

    public void onVideoListEmpty(STORAGE_PORT storage);

    public void onLoading(STORAGE_PORT mCurrentDevice);
}
