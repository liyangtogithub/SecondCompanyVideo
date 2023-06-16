package com.adayo.videoplayer.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.adayo.an6v.ui.ControlButtons;
import com.adayo.an6v.ui.EQButton;
import com.adayo.an6v.ui.ParkingWarningView;
import com.adayo.an6v.ui.TopBar;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.AuxVideoConstantsDef.ParkingCallBackListenr;
import com.adayo.midware.constant.AuxVideoConstantsDef.VIDEO_FORMAT;
import com.adayo.midware.constant.AuxVideoConstantsDef.VIDEO_STATE;
import com.adayo.midware.constant.AuxVideoConstantsDef.VIDEO_TYPE;
import com.adayo.midwareproxy.video.VideoParkingManager;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.Constants;
import com.adayo.videoplayer.Constants.RepeateMode;
import com.adayo.videoplayer.Constants.ShuffleMode;
import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;
import com.adayo.videoplayer.VideoUtils;
import com.adayo.videoplayer.VideoMainActivity;
import com.adayo.videoplayer.core.VideoPlayController;
import com.adayo.videoplayer.interfaces.IVideoPlayStateChangedListener;
import com.adayo.videoplayer.interfaces.VideoPlayStateChangeListener;

public class VideoPlayFragment extends Fragment {

    protected static final String TAG = VideoPlayFragment.class.getSimpleName();

    private static final int MSG_REFRESH_PLAY_TIME = 1;
    private static final int MSG_HIDE_TOPBAR_CONTROLERS = 2;
    private static final int MSG_TIMED_TEXT = 3;
    private static final int MSG_VIDEO_CHANGED = 4;
    private static final int MSG_PLAY_STATE_CHANGED = 5;
    private static final int MSG_PARKING_VIEW_VISIBLE = 6;
    private static final int MSG_REFRESH_LAYOUT = 7;
    private static final int REFRESH_PERIOD = 1 * 500;
    public static final String REFRESH_FRAGMENT_ACTION = "refresh.fragment.action";
    public static final String SHOW_SECOND_PAGE = "show_second_page";
    RefreshReceiver mRefreshReceiver = null;

    private static final long TIME_SHOW_TOPBAR_CONTROLER_BUTTONS = 1 * 5000;

    private static final int PARKING = 1;

    ParkingWarningView mParkingWarningView;

    private int mSeekbarHeight = 0;
    private int mSeekbarWidth = 0;

    View mRootView;
    ControlButtons mControlButtons;
    SeekBar mSbPlay;
    TextView mTvHasPlayTime;
    TextView mTvRemainTime;
    LinearLayout mLlBottomBar;
    EQButton mEQButton;
    RelativeLayout mEQBtnLayout;
    VideoPlayController mPlayControler;
    private TextView mTvTitlePlayInfo;
    private TextView mTvTimedText;
    private RelativeLayout mRlPlayPosBar;
    private TopBar mTopBar;
    boolean mIsAutoPlay = true;
    boolean mIsAutoPlayWhenShow = false;
    boolean mIetParkingVisible = false;;
    private boolean mIsPause = false;

    Handler mHandler = new Handler(AdayoVideoPlayerApplication.instance().getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_PLAY_TIME:
                    updatePlayTime();

                    break;
                case MSG_HIDE_TOPBAR_CONTROLERS:
                    Trace.e(TAG, "MSG_HIDE_TOPBAR_CONTROLERS");
                    if (!mEQButton.isShowing() && mPlayControler.isInPlayingMode()) {
                        hideTopBarAndBottomBar();
                    } else {
                        refreshHideTopBarControlerButtonsTimeout();
                    }
                    break;
                // case MSG_SHOW_TOPBAR_CONTROLERS:
                // showTopBarAndBottomBar();
                // break;
                case MSG_TIMED_TEXT:
                    String text = (String) msg.obj;
                    // Trace.e(TAG, "[MSG_TIMED_TEXT]:" + text);

                    mTvTimedText.setText(text);
                    // mTvTimedText.setBackground(new
                    // BitmapDrawable(getResources(),(Bitmap) msg.obj));

                    break;
                case MSG_VIDEO_CHANGED:
                    clearTimedText();
                    if (mTvTitlePlayInfo != null && mPlayControler.canShowPlayIndex()) {
                        mTvTitlePlayInfo.setText((mPlayControler.getPlayingIndex() + 1) + "/"
                                + mPlayControler.getPlayListCount());
                    }
                    // Trace.e(TAG, "[MSG_VIDEO_CHANGED]:mPlaylist.size()==" +
                    // mPlayControler.getPlayListCount());
                    mTopBar.setTitle(getString(R.string.app_name) + "("
                            + VideoUtils.toDeviceText(mPlayControler.getStorage()) + ")");
                    int duration = msg.arg1;
                    mSbPlay.setMax(duration);
                    break;
                case MSG_PLAY_STATE_CHANGED:
                    playStateChanged(msg.arg1);
                    break;
                case MSG_PARKING_VIEW_VISIBLE:
                    if (mIetParkingVisible == true) {
                        mParkingWarningView.setVisibility(View.VISIBLE);
                    }
                    break;
                case MSG_REFRESH_LAYOUT:
                    showTopBar();
                    mLlBottomBar.setVisibility(View.VISIBLE);
                    mEQBtnLayout.setVisibility(View.VISIBLE);
                    refreshHideTopBarControlerButtonsTimeout();
                    break;
                default:
                    break;
            }
        }
    };

    private int mParkingState;

    private ParkingCallBackListenr mParkingCallBackListenr = new ParkingCallBackListenr() {

        @Override
        public void reportVideoStatus(VIDEO_TYPE videoType, VIDEO_STATE videoState, VIDEO_FORMAT videoFormat) {

        }

        @Override
        public void onParingStateChanged(int state) {
            mParkingState = state;
            Trace.d(TAG, "[onParingStateChanged]:parking state " + state);
            if (mParkingState == 0) {
                if (mPlayControler.isPaused() || mPlayControler.isPlaying()) {
                    showParkingView();
                }
            } else {
                dismissParkingView();
            }
        }
    };

    private IVideoPlayStateChangedListener mChangeListener = new PlayStateChagnedListener();

    public void showTopBarAndBottomBar() {
        showTopBar();
        if (!mIetParkingVisible) {
            mPlayControler.canShowToast(true);
            mPlayControler.checkVideoSupportState();
        }
        mLlBottomBar.setVisibility(View.VISIBLE);
        mEQBtnLayout.setVisibility(View.VISIBLE);
    }

    public void hideTopBarAndBottomBar() {
        mHandler.removeMessages(MSG_HIDE_TOPBAR_CONTROLERS);
        if (getMainActivity() == null) {
            Trace.e(TAG, "[hideTopBarAndBottomBar] getMaintivity is null !!!!!");
            return;
        }
        hideTopBar();
        mLlBottomBar.setVisibility(View.GONE);
        mEQBtnLayout.setVisibility(View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mSeekbarHeight == 0 || mSeekbarWidth == 0) {
            Drawable drawable = getResources().getDrawable(R.drawable.seek_bar_bg);
            mSeekbarHeight = drawable.getIntrinsicHeight();
            mSeekbarWidth = drawable.getIntrinsicWidth();
        }
        mPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_play, container, false);

        mControlButtons = (ControlButtons) mRootView.findViewById(R.id.control_buttons_id);

        mSbPlay = (SeekBar) mRootView.findViewById(R.id.pb_play_progress_id);

        mTvHasPlayTime = (TextView) mRootView.findViewById(R.id.tv_time_left);
        mTvRemainTime = (TextView) mRootView.findViewById(R.id.tv_time_right);

        mLlBottomBar = (LinearLayout) mRootView.findViewById(R.id.ll_playmask_bottom_bar_id);

        mRlPlayPosBar = (RelativeLayout) mRootView.findViewById(R.id.rl_play_pos_id);

        mTvTimedText = (TextView) mRootView.findViewById(R.id.tv_timed_text_id);

        mEQButton = (EQButton) mRootView.findViewById(R.id.btn_eq);
        mEQBtnLayout = (RelativeLayout) mRootView.findViewById(R.id.eq_btn_layout);
        mParkingWarningView = (ParkingWarningView) mRootView.findViewById(R.id.parking_view_id);
        mParkingWarningView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // beep
            }
        });
        mParkingWarningView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_DOWN)) {
                    if (mTopBar.getVisibility() == View.GONE) {
                        showTopBar();
                        refreshHideTopBarControlerButtonsTimeout();
                    } else {
                        hideTopBarAndBottomBar();
                    }
                }
                return false;
            }
        });

        mSbPlay.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        setupSeekBar();
        mControlButtons.makeFirstPageButtons(Constants.FIRST_PAGE_BUTTONS_IDS, Constants.FIRST_PAGE_BUTTONS_RESIDS,
                Constants.FIRST_PAGE_BUTTONS_TITLES, mOnClickListener, mOnLongClickListener, mOnTouchListener);

        mControlButtons.makeSecondPageButtons(Constants.SECOND_PAGE_BUTTONS_IDS, Constants.SECOND_PAGE_BUTTONS_RESIDS,
                Constants.SECOND_PAGE_BUTTONS_TITLES, mOnClickListener, mOnLongClickListener, null);

        mControlButtons.showWhatPageButtons(ControlButtons.SHOW_FIRST_PAGE_BUTTONS);
        mTopBar = (TopBar) mRootView.findViewById(R.id.topbar_id);
        mParkingState = VideoParkingManager.getVideoParkingManager().getParkingState();
        Trace.d(TAG, "[onCreate]:parking state " + mParkingState);
        VideoParkingManager.getVideoParkingManager().registerParkingCallBackListenrs(mParkingCallBackListenr);

        return mRootView;
    }

    private void setupSeekBar() {
        RelativeLayout.LayoutParams params = (LayoutParams) mSbPlay.getLayoutParams();
        if (params == null) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
        }
        params.height = mSeekbarHeight;
        params.width = mSeekbarWidth;
        mSbPlay.setLayoutParams(params);
    }

    @Override
    public void onResume() {
        super.onResume();
        Trace.d(TAG, "[onResume]");
        mPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
        initTopBar();
        refreshHideTopBarControlerButtonsTimeout();
        startRefreshTime();
        if (!mIetParkingVisible) {
            mPlayControler.canShowToast(true);
            mPlayControler.checkVideoSupportState();
        }
        mEQButton.refresh();
        // Trace.d(TAG, "[onResume]:duration " + mPlayControler.getDuration());
        // Trace.d(TAG, "[onResume]:PlayState " +
        // mPlayControler.getPlayState());
        mControlButtons.setOnTouchListener(mOnTouchListener);
        mTopBar.setOnTouchListener(mOnTouchListener);
        mPlayControler.registerVideoStateChangedListener(mChangeListener, false);
        if (mPlayControler.isPaused() && !isHidden() && mIsAutoPlay) {
            mPlayControler.play();
        }
        mIsPause = false;
        registerRefreshBroadcast();
    }

    private void registerRefreshBroadcast() {
        IntentFilter filter = new IntentFilter(REFRESH_FRAGMENT_ACTION);
        mRefreshReceiver = new RefreshReceiver();
        getActivity().registerReceiver(mRefreshReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        removeHandlerMsg();
        mControlButtons.setOnTouchListener(null);
        mTopBar.setOnTouchListener(null);
        updateAutoPlay();
        mIsPause = true;
        getActivity().unregisterReceiver(mRefreshReceiver);

    }

    // @Override
    // public void setUserVisibleHint(boolean isVisibleToUser) {
    // super.setUserVisibleHint(isVisibleToUser);
    // Trace.d(TAG, "[setUserVisibleHint]:isVisibleToUser" + isVisibleToUser);
    // if(isVisibleToUser){
    // mPlayControler.play();
    // }
    // }

    private void updateAutoPlay() {
        if (mPlayControler.isPaused()) {
            mIsAutoPlay = false;
        } else {
            mIsAutoPlay = true;
        }
    }

    private void updateAutoPlayWhenShow() {
        if (mPlayControler.isPaused()) {
            mIsAutoPlayWhenShow = false;
        } else {
            mIsAutoPlayWhenShow = true;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Trace.d(TAG, "[onHiddenChanged]:hidden " + hidden);
        if (!hidden) {
            mTopBar.setTitle(getString(R.string.app_name) + "(" + VideoUtils.toDeviceText(mPlayControler.getStorage())
                    + ")");
            if ((mPlayControler.isInPlayingMode() && mPlayControler.isPaused() && mIsAutoPlay) || mIsAutoPlayWhenShow) {
                mPlayControler.play();
            }
        } else {
            if (mEQButton != null) {
                mEQButton.dismiss();
            }
        }

    }

    private void removeHandlerMsg() {
        mHandler.removeMessages(MSG_REFRESH_PLAY_TIME);
        mHandler.removeMessages(MSG_HIDE_TOPBAR_CONTROLERS);
        // mHandler.removeMessages(MSG_SHOW_TOPBAR_CONTROLERS);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Trace.d(TAG, "onDestroy");
        VideoParkingManager.getVideoParkingManager().unRegisterParkingCallBackListenrs(mParkingCallBackListenr);
        if (mPlayControler != null) {
            mPlayControler.unregisterVideoStateChangedListener(mChangeListener);
        }
        mHandler.removeCallbacksAndMessages(null);
        mPlayControler = null;
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

    private OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mPlayControler.seek(progress);
                refreshHideTopBarControlerButtonsTimeout();
            }
        }
    };

    OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            refreshHideTopBarControlerButtonsTimeout();

            switch (v.getId()) {
                case R.id.btn_goto_list_id:
                    updateAutoPlay();
                    updateAutoPlayWhenShow();
                    mPlayControler.pause();
                    getMainActivity().showListFragment();
                    break;
                case R.id.btn_show_second_page_buttons_id:
                    mControlButtons.showWhatPageButtons(ControlButtons.SHOW_SECOND_PAGE_BUTTONS);
                    break;
                case R.id.hide_second_page_buttons_id:
                    mControlButtons.showWhatPageButtons(ControlButtons.SHOW_FIRST_PAGE_BUTTONS);
                    break;
                case R.id.btn_play_pause_id:
                    if (mPlayControler.isPaused()) {
                        mPlayControler.play();
                    } else if (mPlayControler.isPlaying()) {
                        mPlayControler.pause();
                    }
                    break;
                case R.id.btn_play_next:
                    if (mPlayControler.isPreparing()) {
                        return;
                    }
                    mPlayControler.next();
                    break;
                case R.id.btn_play_prev:
                    if (mPlayControler.isPreparing()) {
                        return;
                    }
                    mPlayControler.prev();
                    break;
                case R.id.btn_shuffle_id:
                    mPlayControler.switchShuffleMode();
                    break;
                case R.id.btn_repeat_id:
                    mPlayControler.switchRepeatMode();
                    break;
                case R.id.btn_eq_id:
                    Intent mIntent = new Intent();
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mIntent.setClassName("com.android.settings", "com.android.settings.eq.MainActivity");
                    getActivity().startActivity(mIntent);
            }
        }
    };

    OnLongClickListener mOnLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            mHandler.removeMessages(MSG_HIDE_TOPBAR_CONTROLERS);
            showTopBarAndBottomBar();
            if (v.getId() == R.id.btn_play_next) {
                mPlayControler.playFF();

            } else if (v.getId() == R.id.btn_play_prev) {
                mPlayControler.playRW();
            }
            return true;
        }
    };

    OnTouchListener mOnTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d(TAG, "[mOnTouchListener,onTouch] event.getAction()==" + event.getAction());

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (v.getId() == R.id.btn_play_next) {
                    mPlayControler.setRealPlay();
                } else if (v.getId() == R.id.btn_play_prev) {
                    mPlayControler.setRealPlay();
                }
                refreshHideTopBarControlerButtonsTimeout();
            }
            return false;
        }
    };

    public VideoMainActivity getMainActivity() {
        return (VideoMainActivity) getActivity();
    }

    public void initTopBar() {
        mTopBar.removeAllAddedViews();
        mTopBar.setTitleMode(TopBar.MODE_NORMAL);
        mTopBar.setTitle(getString(R.string.app_name) + "(" + VideoUtils.toDeviceText(mPlayControler.getStorage())
                + ")");
        mTvTitlePlayInfo = mTopBar.addTextInfo(TopBar.CENTER_HORIZONTAL_IN_TOPBAR, "");
        mTvTitlePlayInfo.setGravity(Gravity.CENTER);
        mTvTitlePlayInfo.setText((mPlayControler.getPlayingIndex() + 1) + "/" + mPlayControler.getPlayListCount());
        // mTvTitlePlayInfo.setTextSize(getResources().getDimensionPixelSize(R.dimen.font_size_title));
        // mTvTitlePlayInfo.setWidth(getResources().getDimensionPixelSize(R.dimen.tv_width_video_title));
        // mTvTitlePlayInfo.setEllipsize(TruncateAt.MARQUEE);
    }

    public void playStateChanged(int playState) {
        switch (playState) {
            case Constants.PLAYING:
                mRlPlayPosBar.setVisibility(View.VISIBLE);
                mControlButtons.updateButtonBackground(R.id.btn_play_pause_id, R.drawable.selector_pause);
                startRefreshTime();
                refreshHideTopBarControlerButtonsTimeout();
                if (mParkingState == PARKING) {
                    dismissParkingView();
                } else {
                    showParkingView();
                }

                break;
            case Constants.PAUSE:
                mRlPlayPosBar.setVisibility(View.VISIBLE);
                mControlButtons.updateButtonBackground(R.id.btn_play_pause_id, R.drawable.selector_play_btn);
                if (mParkingState == PARKING) {
                    dismissParkingView();
                } else {
                    showParkingView();
                }
                mRlPlayPosBar.setVisibility(View.GONE);
                break;
            case Constants.STOP:
                mHandler.removeMessages(MSG_REFRESH_PLAY_TIME);
                mControlButtons.updateButtonBackground(R.id.btn_play_pause_id, R.drawable.selector_play_btn);

                mRlPlayPosBar.setVisibility(View.GONE);
                dismissParkingView();
                if (!mIsPause) {
                    showTopBarAndBottomBar();
                }
                break;
            default:
                mRlPlayPosBar.setVisibility(View.GONE);
                break;
        }
    }

    private void startRefreshTime() {
        mHandler.sendEmptyMessageDelayed(MSG_REFRESH_PLAY_TIME, 0);
    }

    public void queryNextFreshTime(int position) {
        mHandler.sendEmptyMessageDelayed(MSG_REFRESH_PLAY_TIME, REFRESH_PERIOD - position % REFRESH_PERIOD);
    }

    private void updatePlayTime() {
        // Trace.d(TAG, "[MSG_REFRESH_PLAY_TIME]:" +
        // mPlayControler.getCurrentPos());

        if (!isResumed()) {
            Trace.d(TAG, "[updatePlayTime]:not in resumed");
            return;
        }

        int position = mPlayControler.getCurrentPos();
        mSbPlay.setProgress(position);
        queryNextFreshTime(position);
        mTvHasPlayTime.setText(VideoUtils.toTimeFormat(position));
        mTvRemainTime.setText("-" + VideoUtils.toTimeFormat(mSbPlay.getMax() - position));
    }

    public void playProgressChanged(int curPos, long videoDuration) {
        if (videoDuration > 0 && mSbPlay.getProgress() != videoDuration) {
            mSbPlay.setMax((int) videoDuration);
        }
        mSbPlay.setProgress(curPos);
    }

    public void refreshHideTopBarControlerButtonsTimeout() {
        mHandler.removeMessages(MSG_HIDE_TOPBAR_CONTROLERS);
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_TOPBAR_CONTROLERS, TIME_SHOW_TOPBAR_CONTROLER_BUTTONS);
    }

    public void onTouch(MotionEvent event) {

        if (mPlayControler == null) {
            mPlayControler = AdayoVideoPlayerApplication.instance().getVideoPlayControler();
        }
        if (mPlayControler == null) {
            return;
        }
        if (mPlayControler.getPlayState() == Constants.PLAYING || mPlayControler.getPlayState() == Constants.PAUSE) {

            if (!(event.getAction() == MotionEvent.ACTION_DOWN)) {
                return;
            }
            if (mLlBottomBar.getVisibility() == View.GONE) {
                showTopBarAndBottomBar();
                refreshHideTopBarControlerButtonsTimeout();
            } else {
                hideTopBarAndBottomBar();
            }
        } else {
            showTopBarAndBottomBar();
        }
    }

    class PlayStateChagnedListener extends VideoPlayStateChangeListener {

        @Override
        public void onSeekCompletion() {
            // mSbPlay.setProgress(curPos);
        }

        @Override
        public void playVideoChanged(String playingPath, int duration) {
            Trace.d(TAG, "[playVideoChanged]:playingPath " + playingPath + " duration " + duration);
            mHandler.obtainMessage(MSG_VIDEO_CHANGED, duration, 0, playingPath).sendToTarget();
        }

        @Override
        public void onPlayStateChanged(int playState) {
            mHandler.obtainMessage(MSG_PLAY_STATE_CHANGED, playState, 0).sendToTarget();
        }

        @Override
        public void onShuffleModeChanged(ShuffleMode mode) {
            if (ShuffleMode.ON.equals(mode)) {
                mControlButtons.updateButtonText(R.id.btn_shuffle_id, R.string.shuffle_text);
                mControlButtons.updateButtonBackground(R.id.btn_shuffle_id, R.drawable.selector_shuffle_btn);
            } else {
                mControlButtons.updateButtonText(R.id.btn_shuffle_id, R.string.shuffle_none_text);
                mControlButtons.updateButtonBackground(R.id.btn_shuffle_id, R.drawable.selector_shuffle_none_btn);
            }
        }

        @Override
        public void onRepeateModeChanged(RepeateMode mode) {
            if (RepeateMode.ALL.equals(mode)) {
                mControlButtons.updateButtonBackground(R.id.btn_repeat_id, R.drawable.selector_repeat_all_btn);
            } else {
                mControlButtons.updateButtonBackground(R.id.btn_repeat_id, R.drawable.selector_repeat_one_btn);
            }
        }

        @Override
        public void onTimedText(String text, Bitmap bmp) {
            mHandler.obtainMessage(MSG_TIMED_TEXT, text).sendToTarget();
        }
    };

    private void dismissParkingView() {
        mIetParkingVisible = false;
        mParkingWarningView.setVisibility(View.GONE);
    }

    protected void clearTimedText() {
        mHandler.obtainMessage(MSG_TIMED_TEXT, "").sendToTarget();
    }

    private void showParkingView() {
        mPlayControler.canShowToast(false);
        mIetParkingVisible = true;
        mHandler.sendEmptyMessageDelayed(MSG_PARKING_VIEW_VISIBLE, REFRESH_PERIOD);
    }

    public class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "[RefreshReceiver]:" + action);
            if (intent.getBooleanExtra(SHOW_SECOND_PAGE, false)) {
                mControlButtons.showWhatPageButtons(ControlButtons.SHOW_SECOND_PAGE_BUTTONS);
            }
            receiverOntouch();
        }

    }

    public void receiverOntouch() {
        mHandler.obtainMessage(MSG_REFRESH_LAYOUT, "").sendToTarget();
    }
}
