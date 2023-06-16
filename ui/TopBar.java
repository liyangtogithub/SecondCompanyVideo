/**
 *  Adayo Project - CommonUI
 *
 *  Copyright (C) Adayo 2014 - 2020
 *
 *  File:TopBar.java
 *
 *  Revision:
 *  
 *  2014-11-03
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adayo.mediaScanner.CommonUtil;
import com.adayo.mediaScanner.MediaScannerInterface.STORAGE_PORT;

/**
 * 
 * <ul>
 * <li>display title in all source
 * <li>display time in all source
 * <li>display chapter info in Disc
 * <li>display source change button in media USB/SD
 * <li>display index info in media source
 * </ul>
 * 
 * @author Chenjia
 * @data 2014/11/3
 * 
 */
public class TopBar extends RelativeLayout {
	/**
	 * control the text info display in the horizontal center of the top bar.
	 */
	public static final int CENTER_HORIZONTAL_IN_TOPBAR = -1;

	public static final int MODE_NORMAL = 0;
	public static final int MODE_LIST = 1;
	public static final int MODE_SETTING = 2;

	protected static final String TAG = TopBar.class.getSimpleName();

	private AudioManager mAudioManager;
	private Context mContext = null;
	private ImageView mBackIv = null;
	private ImageView iv_mute_state = null;
	private TextView mTitleTv = null;
	private RelativeLayout mViewHolderRl = null;
	private RelativeLayout mTitleAreaRl = null;
	private String mTitle_text;
	private Button mMenu;
	private Clock mClock;
	
	private TextView setOkTv = null; 
	private TextView setCancleTv = null; 
	
	private View topBar;
	private int mMode = -1;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if(AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())){
				int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
				updateMute(ringerMode);
			}
		}
	};
	private SelectorPopupWindow mSelectorWindow;
	private OnDeviceSelected mOnDeviceSelected;
	
	private OnButtonClicked mButtonClicked;
	
	public TopBar(Context context) {
		this(context, null);
	}

	public TopBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TopBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TopBar);
		mTitle_text = a.getString(R.styleable.TopBar_title_text);
		mMode = a.getInt(R.styleable.TopBar_title_mode, -1);
		a.recycle();
		initLayout(context);
		mSelectorWindow = new SelectorPopupWindow(mContext);
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	private void initLayout(Context context) {
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View view = layoutInflater.inflate(R.layout.layout_top_bar, this);

		mContext = context;
		topBar = view.findViewById(R.id.top_bar_id);
		mBackIv = (ImageView) view.findViewById(R.id.iv_topbar_back);
		iv_mute_state = (ImageView) view.findViewById(R.id.iv_mute_state);
		mTitleTv = (TextView) view.findViewById(R.id.tv_topbar_title);
		mClock = (Clock) view.findViewById(R.id.clock_top_bar_time);
		mMenu = (Button) view.findViewById(R.id.btn_menu);
		mMenu.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Thread mThread = new Thread() {
					@Override
					public void run() {
						Instrumentation mInstrumentation = new Instrumentation();
						mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
						super.run();
					}
				};
				mThread.start();
			}
		});
		if (mTitle_text != null) {
			mTitleTv.setText(mTitle_text);
		}
		mViewHolderRl = (RelativeLayout) view.findViewById(R.id.rl_view_holder);
		mTitleAreaRl = (RelativeLayout) view.findViewById(R.id.rl_topbar_title_area);
		mTitleAreaRl.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					mBackIv.setImageResource(R.drawable.ic_top_bar_back_p);
					break;
				case MotionEvent.ACTION_UP:
					mBackIv.setImageResource(R.drawable.ic_top_bar_back_n);
					break;
				default:
					break;
				}
				return false;
			}
		});
		mTitleAreaRl.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus){
					mBackIv.setImageResource(R.drawable.ic_top_bar_back_p);
				}else {
					mBackIv.setImageResource(R.drawable.ic_top_bar_back_n);
				}
				
			}
		});
		if (context instanceof Activity) {
			final Activity activity = (Activity) context;
			mTitleAreaRl.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					activity.onBackPressed();
				}
			});
		}
		if(mMode != -1){
			setTitleMode(mMode);
		}
		
		setOkTv = (TextView) view.findViewById(R.id.set_ok);
		setCancleTv = (TextView) view.findViewById(R.id.set_cancle);
		
		setOkTv.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mButtonClicked.OnButtonClicked(true, false);
			}
		});
		
		setCancleTv.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mButtonClicked.OnButtonClicked(false, true);
			}
		});
	}
	
	public void setClickListener(OnButtonClicked buttonClicked){
		this.mButtonClicked = buttonClicked;
	}
	
	public void showMenuBtn() {
		mClock.setVisibility(View.GONE);
		mMenu.setVisibility(View.VISIBLE);
	}
	public void setSelectVisible(boolean isVisiable){
		if (isVisiable) {
			setOkTv.setVisibility(View.VISIBLE);
			setCancleTv.setVisibility(View.VISIBLE);
		}else {
			setOkTv.setVisibility(View.GONE);
			setCancleTv.setVisibility(View.GONE);
		}
	}
	
	public void show(){
		if(topBar != null){//&& !topBar.isShown()
			topBar.setVisibility(View.VISIBLE);
		}
	}
	
	public void hide(){
		if(topBar != null){//&& topBar.isShown()
			topBar.setVisibility(View.GONE);
		}
	}
	
	public boolean isTopbarShown(){
		if(topBar != null){
			return topBar.isShown();
		}
		return true;
	}
	
	public void setTitle(int resid) {
		mTitleTv.setText(resid);
	}

	/**
	 * Set title in the top bar.
	 * 
	 * @param title
	 */
	public void setTitle(CharSequence title) {
		mTitleTv.setText(title);
	}

	/**
	 * Set title click listener
	 * 
	 * @param listener
	 */
	public void setTitleClickListener(OnClickListener listener) {
		mTitleAreaRl.setOnClickListener(listener);
	}

	/**
	 * add text info in the middle area of the top bar.
	 * 
	 * @param marginLeft
	 *            if marginLeft > 0, this parameter is distance between the text info and it's left
	 *            widget; if marginLeft = {@link com.adayo.an6v.ui.CENTER_HORIZONTAL_IN_TOPBAR},
	 *            this text info will display center horizontal in the top bar.
	 * @param info
	 *            the text will display in the top bar.
	 * @return a {@link TextView} instance that holder text info and display in the top bar.
	 */
	public TextView addTextInfo(int marginLeft, CharSequence info) {
		TextView tv = new TextView(mContext);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.rl_view_holder_add_text_size));
		tv.setText(info);
		tv.setGravity(Gravity.CENTER_VERTICAL);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		if (marginLeft == CENTER_HORIZONTAL_IN_TOPBAR) {
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		} else if (marginLeft >= 0) {
			lp.leftMargin = marginLeft;
		}
		mViewHolderRl.addView(tv, mViewHolderRl.getChildCount(), lp);
		return tv;
	}
	
	private void configureDeviceTextViewInTitle(TextView textView){
		Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_top_bar_selector_normal);
		textView.setBackgroundResource(R.drawable.selector_topbar_selector_btn);
		drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
		textView.setCompoundDrawables(null, null, drawable, null);
		textView.setCompoundDrawablePadding(0);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,mContext.getResources().getDimensionPixelSize(R.dimen.topbar_device_text_size));
		textView.setGravity(Gravity.CENTER);
		textView.setPadding(0, 0, mContext.getResources().getDimensionPixelSize(R.dimen.topbar_selector_first_item_padd_right), 0);
		textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!mSelectorWindow.isShowing()){
					mSelectorWindow.showAsDropDown(v, 0, 0 - v.getHeight());
				}
				else{
					mSelectorWindow.dismiss();
				}
			}
		});
	}
	
	/**
	 * 
	 * @param infos 闂囷拷鐟曚焦妯夌粈铏规畱鐠佹儳顦�
	 * @return 鏉╂柨娲栧В蹇庣娑撶寘extView
	 * 
	 *
	 * 
	 */
	public TextView[] addDevicesSelector(OnDeviceSelected onDeviceSelected){
		mOnDeviceSelected = onDeviceSelected;
		STORAGE_PORT[] mountedStorages = CommonUtil.getMountedStorage();
		
		if(mountedStorages == null || mountedStorages.length == 0)
			return null;
		
		STORAGE_PORT[] tmpPorts = new STORAGE_PORT[mountedStorages.length + 1];
		
		System.arraycopy(mountedStorages, 0, tmpPorts, 0,mountedStorages.length);
		tmpPorts[mountedStorages.length] = STORAGE_PORT.STORAGE_ALL;
		
		mountedStorages = tmpPorts;
		
		String[] mountedStoragesText = new String[mountedStorages.length];
		for (int i = 0; i < mountedStorages.length; i++) {
			mountedStoragesText[i] = STORAGE_PORT_To_String(mountedStorages[i], mContext);
		}
		TextView[] tvs = mSelectorWindow.updateItems(mountedStoragesText);
		if(mSelectorWindow.getItemCount() <= 0){
			removeAllAddedViews();
			mSelectorWindow.dismiss();
		}else{
			
			for (int i = 0 ; i < tvs.length;i++) {
				TextView textView = tvs[i];
				textView.setTag(mountedStorages[i]);
				textView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						STORAGE_PORT storage = (STORAGE_PORT) v.getTag();
						Log.d(TAG, "storage " + storage.name());
						if(mOnDeviceSelected != null)
							mOnDeviceSelected.onDeviceSelected(storage);
						setTextInfo(((TextView)v).getText());
						hideDeviceSelector();
					}
				});
			}
			
			TextView tv;
			if(mViewHolderRl.getChildCount() <= 0 || !(mViewHolderRl.getChildAt(0) instanceof TextView)){
				removeAllAddedViews();
				tv = addTextInfo(CENTER_HORIZONTAL_IN_TOPBAR, mountedStoragesText[0]);
				tv.setTag(mountedStorages[0]);
				configureDeviceTextViewInTitle(tv);
			}else{
				tv = (TextView) mViewHolderRl.getChildAt(0);
				tv.setTag(mountedStorages[0]);
				configureDeviceTextViewInTitle(tv);
				tv.setText(mountedStoragesText[0]);
			}
		}
		
		return tvs;
	}
	
	public void showDeviceTextInTitle(STORAGE_PORT storage){
		setTextInfo(STORAGE_PORT_To_String(storage, mContext));
		if(mViewHolderRl.getChildAt(0) != null)
			mViewHolderRl.getChildAt(0).setTag(storage);
	}
	
	public void setTextInfo(CharSequence info){
		TextView tv = (TextView) mViewHolderRl.getChildAt(0);
		if(tv != null)
			tv.setText(info);
	}
	
	public TextView[] updateDevices(){
		STORAGE_PORT storage = null;
		try{
			if(mViewHolderRl.getChildAt(0) != null){
				storage = (STORAGE_PORT) mViewHolderRl.getChildAt(0).getTag();
			}
		
		}catch(NullPointerException e){
			e.printStackTrace();
		}
		hideDeviceSelector();
		TextView[] tvs = addDevicesSelector(mOnDeviceSelected);
		if(tvs == null){
			removeAllAddedViews();
			return null;
		}
		
		boolean flag = false;
		for (TextView textView : tvs) {
			if(textView.getTag().equals(storage))
				flag = true;
		}
		if(flag && storage != null){
			showDeviceTextInTitle(storage);
		}else
			showDeviceTextInTitle(STORAGE_PORT.STORAGE_ALL);
		return tvs;
	}
	
	/**
	 * remove all views that display in the middle area and added before.
	 */
	public void removeAllAddedViews() {
		mViewHolderRl.removeAllViews();
		mSelectorWindow.dismiss();
	}
	
	/**
	 * change the position of title relative the arrow.
	 * 
	 * @param mode
	 */
	public void setTitleMode(int mode) {
		RelativeLayout.LayoutParams lpTitle = (LayoutParams) mTitleTv.getLayoutParams();
		if (mode == MODE_NORMAL) {
			lpTitle.leftMargin = mContext.getResources()
					.getDimensionPixelSize(R.dimen.margin_left_top_bar_title_normal);
			mTitleAreaRl.setPadding(
					mContext.getResources().getDimensionPixelSize(R.dimen.padding_left_top_bar_title_area), 0, 0, 0);
		} else if (mode == MODE_LIST) {
			lpTitle.leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.margin_left_top_bar_title_list);
			mTitleAreaRl.setPadding(
					mContext.getResources().getDimensionPixelSize(R.dimen.padding_left_top_bar_title_area), 0, 0, 0);
		} else if (mode == MODE_SETTING) {
			lpTitle.leftMargin = mContext.getResources().getDimensionPixelSize(
					R.dimen.margin_left_top_bar_title_setting);
			mTitleAreaRl.setPadding(
					mContext.getResources().getDimensionPixelSize(R.dimen.padding_left_top_bar_title_area_setting), 0,
					0, 0);
		}
		mTitleTv.setLayoutParams(lpTitle);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		updateMute(mAudioManager.getRingerMode());
		IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
		getContext().registerReceiver(mReceiver, filter);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		getContext().unregisterReceiver(mReceiver);
		super.onDetachedFromWindow();
	}
	
	public void hideDeviceSelector(){
		mSelectorWindow.dismiss();
	}
	
	private void updateMute(int ringerMode) {
		boolean isSysMute = ringerMode == AudioManager.RINGER_MODE_SILENT;
		iv_mute_state.setVisibility(isSysMute ? VISIBLE :GONE);
	}

	public interface OnDeviceSelected {
		public void onDeviceSelected(STORAGE_PORT storage);
	}
	
	public interface OnButtonClicked{
		public void OnButtonClicked(boolean OkState, boolean CancleState);
	}
	public String STORAGE_PORT_To_String(STORAGE_PORT storage,Context context){
		return Utils.getStorageName(storage,context);
	}
	public void obtainFoucs(){
		mTitleAreaRl.requestFocus();
	}
	public void checkSetTitle(String text){
		if(text != null && text.equals(mTitleTv.getText().toString()))
			return;
		setTitle(text);
	}
}
