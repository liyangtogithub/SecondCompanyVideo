/**
 *  Adayo Project - CommonUI
 *
 *  Copyright (C) Adayo 2013 - 2025
 *
 *  File:EQPopupWindow.java
 *
 *  Revision:
 *  
 *  2014年11月26日
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.SettingConstantsDef.EQ_TYPE;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.PopupWindow;

/**
 * @author ChenJia
 * @Data 2014年11月26日
 */
public class EQPopupWindow extends PopupWindow {
	private String TAG = EQPopupWindow.class.getSimpleName();

	private EQButton mEQButton = null;

	/**
	 * x offset of EQ popup window, base anchor view.
	 */
	private static final int mXOffset = -520;

	/**
	 * y offset of EQ popup window, base anchor view.
	 */
	private static final int mYOffset = 0;

//	private static final int mWithOfPopWindow = 657;
//	private static final int mHeightOfPopWindow = 252;
	
	private MarqueeText mEQTypeFlatBtn = null;
	private MarqueeText mEQTypePopBtn = null;
	private MarqueeText mEQTypeUserBtn = null;
	private MarqueeText mEQTypeTechnoBtn = null;
	private MarqueeText mEQTypeRockBtn = null;
	private MarqueeText mEQTypeClassicalBtn = null;
	private MarqueeText mEQTypeJazzBtn = null;
	private MarqueeText mEQTypeOptimalBtn = null;

	private Timer mTimer = new Timer();
	private TimerTask mDismissTask = null;
	
	private Context mContext = null;
	
	public EQPopupWindow(EQButton eqButton, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, null, 0, R.style.KeyboardTheme);
		mEQButton = eqButton;
		mContext = context;

		View view = LayoutInflater.from(context).inflate(R.layout.layout_eq_popup_window, null);
		setContentView(view);
		
		Drawable drawable = context.getResources().getDrawable(R.drawable.eq_pop_window_bg);
		int mHeightOfPopWindow = 657;
		int mWithOfPopWindow = 252;
		
		if(drawable != null){
			mHeightOfPopWindow = drawable.getIntrinsicHeight();
			mWithOfPopWindow = drawable.getIntrinsicWidth();
		}
		setHeight(mHeightOfPopWindow);
		setWidth(mWithOfPopWindow);
		setOutsideTouchable(true);
		setFocusable(true);

		mEQTypeFlatBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_flat);
		mEQTypePopBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_pop);
		mEQTypeUserBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_user);
		mEQTypeRockBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_rock);
		mEQTypeTechnoBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_techno);
		mEQTypeClassicalBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_classical);
		mEQTypeJazzBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_jazz);
		mEQTypeOptimalBtn = (MarqueeText) view.findViewById(R.id.btn_eq_type_optimal);

		mEQTypeFlatBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.flat);
				mEQTypeFlatBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeFlatBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypePopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.pop);
				mEQTypePopBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypePopBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeUserBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.user);
				mEQTypeUserBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeUserBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeRockBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.rock);
				mEQTypeRockBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeRockBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeTechnoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.techno);
				mEQTypeTechnoBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeTechnoBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeClassicalBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.classic);
				mEQTypeClassicalBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeClassicalBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeJazzBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.jazz);
				mEQTypeJazzBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeJazzBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		mEQTypeOptimalBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateEQType(EQ_TYPE.optimal);
				mEQTypeOptimalBtn.startMarquee(1000);
				refreshDismissTime();
			}
		});
		mEQTypeOptimalBtn.setOnFocusChangeListener(mOnFocusChangeListener);
		
		Log.d(TAG,"current eq type:" + SettingConstantsDef.getCurrentEqType(mContext));
		updateEQType(SettingConstantsDef.getCurrentEqType(mContext));
	}
	
	private OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			refreshDismissTime();
		}
	};
	
	private void updateEQType(EQ_TYPE eqType){
		notifyEQTypeChanged(eqType);
		SettingConstantsDef.setCurrentEqType(mContext, eqType);
		setSelected(eqType);
	}

	public void show() {
		updateEQType(SettingConstantsDef.getCurrentEqType(mContext));
		refreshDismissTime();
		showAsDropDown(mEQButton, mContext.getResources().getDimensionPixelSize(R.dimen.eq_popup_window_xoffset), mYOffset);
		mEQTypeFlatBtn.startMarquee(1000);
		mEQTypePopBtn.startMarquee(1000);
		mEQTypeUserBtn.startMarquee(1000);
		mEQTypeRockBtn.startMarquee(1000);
		mEQTypeTechnoBtn.startMarquee(1000);
		mEQTypeClassicalBtn.startMarquee(1000);
		mEQTypeJazzBtn.startMarquee(1000);
		mEQTypeOptimalBtn.startMarquee(1000);
	}

	// /////////////////////////////////////
	// EQ type change listener
	// /////////////////////////////////////
	public static interface IEQTypeChangeListener {
		void eqTypeChange(EQ_TYPE newEQType);
	}

	private IEQTypeChangeListener mEQTypeChangeListener = null;

	public void setEQTypeChangeListener(IEQTypeChangeListener listener) {
		mEQTypeChangeListener = listener;
	}

	private void notifyEQTypeChanged(EQ_TYPE newEQType) {
		if (mEQTypeChangeListener != null) {
			mEQTypeChangeListener.eqTypeChange(newEQType);
		}
	}
	
	private void refreshDismissTime(){
		try{
			if(mDismissTask != null)
				mDismissTask.cancel();
		}catch(Exception e){
			e.printStackTrace();
		}
		initTimerTask();
		mTimer.schedule(mDismissTask, 7*1000);
	}
	
	private void initTimerTask(){
		mDismissTask = new TimerTask() {
			
			@Override
			public void run() {
				mEQButton.post(new Runnable() {
					
					@Override
					public void run() {
						Log.d(TAG,"dimiss eq window");
						dismiss();
					}
				});
			}
		};
	}
	private void setSelected(EQ_TYPE eqType){
		mEQTypeClassicalBtn.setSelected(false);
		mEQTypeFlatBtn.setSelected(false);
		mEQTypeJazzBtn.setSelected(false);
		mEQTypeOptimalBtn.setSelected(false);
		mEQTypePopBtn.setSelected(false);
		mEQTypeRockBtn.setSelected(false);
		mEQTypeTechnoBtn.setSelected(false);
		mEQTypeUserBtn.setSelected(false);
		switch (eqType) {
		case flat:
			mEQTypeFlatBtn.setSelected(true);
			break;
		case pop:
			mEQTypePopBtn.setSelected(true);
			break;
		case user:
			mEQTypeUserBtn.setSelected(true);
			break;
		case techno:
			mEQTypeTechnoBtn.setSelected(true);
			break;
		case rock:
			mEQTypeRockBtn.setSelected(true);
			break;
		case classic:
			mEQTypeClassicalBtn.setSelected(true);
			break;
		case jazz:
			mEQTypeJazzBtn.setSelected(true);
			break;
		case optimal:
			mEQTypeOptimalBtn.setSelected(true);
			break;
		default:
		}
	}
	
}
