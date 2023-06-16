package com.adayo.videoplayer.interfaces;

import android.graphics.Bitmap;

import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;
import com.adayo.videoplayer.Constants.RepeateMode;
import com.adayo.videoplayer.Constants.ShuffleMode;

public class VideoPlayStateChangeListener implements IVideoPlayStateChangedListener {

    @Override
    public void onPlayStateChanged(int playState) {

    }

    @Override
    public void playVideoChanged(String playingPath, int duration) {

    }

    @Override
    public void onSeekCompletion() {

    }

    @Override
    public void onShuffleModeChanged(ShuffleMode mode) {

    }

    @Override
    public void onRepeateModeChanged(RepeateMode mode) {

    }

    @Override
    public void retVideoList(String[] titles, int[] fileIds, String[] fullPaths, STORAGE_PORT storage) {

    }

    @Override
    public void onListSelectedChanged(int position) {

    }

    @Override
    public void onTimedText(String text, Bitmap bmp) {

    }

    @Override
    public void isSeekable(boolean isSeekable) {

    }

    @Override
    public void noVideoFileToPlay() {

    }

    @Override
    public void noDevices() {

    }

    @Override
    public void storageScanning(STORAGE_PORT storage) {

    }

    @Override
    public void beginNewPlaying() {

    }

    @Override
    public void storageUnmounted() {

    }

    @Override
    public void onVideoListEmpty(STORAGE_PORT storage) {

    }

    @Override
    public void onLoading(STORAGE_PORT mCurrentDevice) {

    }

}
