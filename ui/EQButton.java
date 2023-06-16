/**
 *  Adayo Project - CommonUI
 *
 *  Copyright (C) Adayo 2013 - 2025
 *
 *  File:EQButton.java
 *
 *  Revision:
 *  
 *  2014年11月26日
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import com.adayo.an6v.ui.EQPopupWindow.IEQTypeChangeListener;
import com.adayo.midware.constant.SettingConstantsDef;
import com.adayo.midware.constant.SettingConstantsDef.EQ_TYPE;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

/**
 * @author ChenJia
 * @Data 2014年11月26日
 */
public class EQButton extends Button {
	private static final String TAG = EQButton.class.getSimpleName();

	/**
	 * EQ Popup Window instance.
	 */
	private EQPopupWindow mEqPopupWindow = null;
	private Context mContext = null;

	public EQButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		
		mEqPopupWindow = new EQPopupWindow(this, context, null, 0, 0);
		mEqPopupWindow.setEQTypeChangeListener(new IEQTypeChangeListener() {
			@Override
			public void eqTypeChange(EQ_TYPE newEQType) {
//				updateEQType(newEQType);
			}
		});

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mEqPopupWindow.show();
			}
		});
		setBackgroundResource(R.drawable.selector_eq_button_bg);
//		setText("EQ");
		setTextColor(context.getResources().getColor(android.R.color.white));
		setGravity(Gravity.CENTER);
		setFocusable(true);
		
		addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
			@Override
			public void onViewDetachedFromWindow(View v) {
				Log.d(TAG,"onViewDetachedFromWindow");
			}
			
			@Override
			public void onViewAttachedToWindow(View v) {
				Log.d(TAG,"onViewAttachedToWindow");
				refresh();
			}
		});
	}
	
	public EQButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public EQButton(Context context) {
		this(context, null);
	}
	
	public void refresh(){
//		updateEQType(SettingConstantsDef.getCurrentEqType(mContext));
	}
	
	private void updateEQType(EQ_TYPE eqType){
		String eqTypeText = null;
		switch (eqType) {
		case flat:
			eqTypeText = mContext.getString(R.string.btn_name_eq_flat);
			break;
		case pop:
			eqTypeText = mContext.getString(R.string.btn_name_eq_pop);
			break;
		case user:
			eqTypeText = mContext.getString(R.string.btn_name_eq_user);
			break;
		case techno:
			eqTypeText = mContext.getString(R.string.btn_name_eq_techno);
			break;
		case rock:
			eqTypeText = mContext.getString(R.string.btn_name_eq_rock);
			break;
		case classic:
			eqTypeText = mContext.getString(R.string.btn_name_eq_classical);
			break;
		case jazz:
			eqTypeText = mContext.getString(R.string.btn_name_eq_jazz);
			break;
		case optimal:
			eqTypeText = mContext.getString(R.string.btn_name_eq_optimal);
			break;
		default:
			eqTypeText = "";
		}
		
		setText(eqTypeText);
	}
	
	public void dismiss(){
		mEqPopupWindow.dismiss();
	}
	
	public boolean isShowing(){
		return mEqPopupWindow.isShowing();
	}
}
