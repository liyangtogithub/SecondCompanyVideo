/**
 *  Foryou Project - BlaupunktUI
 *
 *  Copyright (C) Foryou 2013 - 2015
 *
 *  File:KeyboardButton.java
 *
 *  Revision:
 *  
 *  2013-12-20
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author ChenJia
 * @Data 2013-12-20
 */
public class BtKeyboardButton extends RelativeLayout {
	private static final String TAG = "KeyboardButton";

	private int mKeyNumber;
	private String mKeyChar;

	private TextView mNumberTv;
	private TextView mCharTv;

	public BtKeyboardButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.bt_keyboard_button);
		mKeyNumber = a.getInt(R.styleable.bt_keyboard_button_bt_key_number, -1);
		mKeyChar = a.getString(R.styleable.bt_keyboard_button_bt_key_char);
		a.recycle();

		initLayout(context);
	}

	public BtKeyboardButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BtKeyboardButton(Context context) {
		this(context, null);
	}

	public void initLayout(Context context) {
		View view = LayoutInflater.from(context).inflate(R.layout.bt_keyboard_button_layout, this);
		mNumberTv = (TextView) view.findViewById(R.id.tv_keyboard_button_num);
		mCharTv = (TextView) view.findViewById(R.id.tv_keyboard_button_char);

		if (mKeyNumber != -1) {
			mNumberTv.setText("" + mKeyNumber);
		}
		if (!TextUtils.isEmpty(mKeyChar)) {
			mCharTv.setText(mKeyChar);
		}

		setBackgroundResource(R.drawable.selector_bt_keyboard_button_bg);
		setClickable(true);
		setLongClickable(true);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return true;
	}
	
	public void hideChar(){
		mCharTv.setVisibility(View.GONE);
	}
	public void showChar(){
		mCharTv.setVisibility(View.VISIBLE);
	}
}
