/**
 *  Foryou Project - CommonUI
 *
 *  Copyright (C) Foryou 2013 - 2015
 *
 *  File:KeyboardPopupWindow.java
 *
 *  Revision:
 *  
 *  2014-3-12
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import com.adayo.an6v.ui.Keyboard.KeyboardMode;
import com.adayo.an6v.ui.Keyboard.OnGotoKeyboardClickListener;
import com.adayo.an6v.ui.Keyboard.OnGotoKeyboardLongClickListener;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.PopupWindow;

/**
 * @author ChenJia
 * @Data 2014-3-12
 */
public class KeyboardPopupWindow extends PopupWindow {
	private Keyboard mKeyboard;
	private TopBar mAnchorView;
	private boolean cancleable = true;

	/**
	 * @param context
	 */
	public KeyboardPopupWindow(Context context, KeyboardMode mode) {
		super(context, null, 0, R.style.KeyboardTheme);

		View view = LayoutInflater.from(context).inflate(R.layout.layout_keyboard_popup_window, null);
		view.setOnTouchListener(mDismissTouchListener);
		setContentView(view);
		setHeight(context.getResources().getDimensionPixelSize(R.dimen.keyboard_height));
		setWidth(context.getResources().getDimensionPixelSize(R.dimen.keyboard_width));
		setFocusable(true);

		mKeyboard = (Keyboard) view.findViewById(R.id.keyboard_popup);
		mKeyboard.setMode(mode);
		mKeyboard.setOnGotoKeyboardClickListener(new OnGotoKeyboardClickListener() {
			@Override
			public void onNumberClick(int number) {
				resetTimeout();
				if (mOnGotoKeyboardClickListener != null) {
					mOnGotoKeyboardClickListener.onNumberClick(number);
				}
			}

			@Override
			public void onDel() {
				resetTimeout();
				if (mOnGotoKeyboardClickListener != null) {
					mOnGotoKeyboardClickListener.onDel();
				}
			}

			@Override
			public void onOk() {
				resetTimeout();
				if (mOnGotoKeyboardClickListener != null) {
					mOnGotoKeyboardClickListener.onOk();
				}
			}

			@Override
			public void onCancel() {
				resetTimeout();
				if (mOnGotoKeyboardClickListener != null) {
					mOnGotoKeyboardClickListener.onCancel();
				}
			}
		});

		mKeyboard.setOnGotoKeyboardLongClickListener(new OnGotoKeyboardLongClickListener() {

			@Override
			public boolean onNumberLongClick(int number) {
				resetTimeout();
				if (mOnGotoKeyboardLongClickListener != null) {
					return mOnGotoKeyboardLongClickListener.onNumberLongClick(number);
				}
				return false;
			}

			@Override
			public boolean onDelLongClick() {
				resetTimeout();
				if (mOnGotoKeyboardLongClickListener != null) {
					return mOnGotoKeyboardLongClickListener.onDelLongClick();
				}
				return false;
			}

			@Override
			public boolean onOkLongClick() {
				resetTimeout();
				if (mOnGotoKeyboardLongClickListener != null) {
					return mOnGotoKeyboardLongClickListener.onOkLongClick();
				}
				return false;
			}

			@Override
			public boolean onCancelLongClick() {
				resetTimeout();
				if (mOnGotoKeyboardLongClickListener != null) {
					return mOnGotoKeyboardLongClickListener.onCancelLongClick();
				}
				return false;
			}
		});
	}
	
	public void show(View anchorView) {
		setOutsideTouchable(true);
		update(mAnchorView, -1, -1);
		showAsDropDown(anchorView, 0, 0);
		mKeyboard.setView(null);
	}

	public void show(TopBar anchorView) {
		show(anchorView, null);
	}

	public void show(TopBar anchorView, View inputView) {
		show(anchorView, inputView, true);
	}

	public void show(TopBar anchorView, View inputView, boolean dismissWithPopupMenuButton) {
		mAnchorView = anchorView;
		// if (dismissWithPopupMenuButton)
		// popupMenuButton.addDismissTogetherPoppupWindow(this);
		setOutsideTouchable(true);
		update(mAnchorView, -1, -1);
		showAsDropDown(anchorView, 0, 0);
		mKeyboard.setView(inputView);
	}

	// ///////////////////////////////////////
	// Keyboard Mode
	// ///////////////////////////////////////
	public void setMode(KeyboardMode mode) {
		mKeyboard.setMode(mode);
	}

	// ///////////////////////////////////////
	// Get cancel / OK button view
	// ///////////////////////////////////////
	public Button getOKButton() {
		return mKeyboard.getOKButton();
	}

	// ///////////////////////////////////////
	// Enable
	// ///////////////////////////////////////
	public void enableCancelButton(boolean enabled) {
		mKeyboard.enableCancelButton(enabled);
	}

	public void enableOkButton(boolean enabled) {
		mKeyboard.enableOkButton(enabled);
	}

	// ///////////////////////////////////////
	// Custom Input area View
	// ///////////////////////////////////////
	public void setView(View view) {
		mKeyboard.setView(view);
	}

	// ///////////////////////////////////////
	// long Click Listener
	// ///////////////////////////////////////
	private OnGotoKeyboardLongClickListener mOnGotoKeyboardLongClickListener = null;

	public void setOnGotoKeyboardLongClickListener(OnGotoKeyboardLongClickListener onGotoKeyboardLongClickListener) {
		mOnGotoKeyboardLongClickListener = onGotoKeyboardLongClickListener;
	}

	// ///////////////////////////////////////
	// Click Listener
	// ///////////////////////////////////////
	private OnGotoKeyboardClickListener mOnGotoKeyboardClickListener = null;

	public void setOnGotoKeyboardClickListener(OnGotoKeyboardClickListener onGotoKeyboardClickListener) {
		mOnGotoKeyboardClickListener = onGotoKeyboardClickListener;
	}
	
	public void setCancleable(boolean flag){
		cancleable = flag;
	}

	private OnTouchListener mDismissTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN && cancleable){
				dismiss();
				return true;
			}
			return false;
		}
	};

	// ///////////////////////////////////////
	// click
	// ///////////////////////////////////////
	private boolean canHandleClick() {
		if (!isShowing()) {
			return false;
		}
		resetTimeout();
		return true;
	}

	public void clickKey(int key) {
		if (canHandleClick())
			mKeyboard.clickKey(key);
	}

	public void clickDel() {
		if (canHandleClick())
			mKeyboard.clickDel();
	}

	public void clickOk() {
		if (canHandleClick())
			mKeyboard.clickOk();
	}

	// /////////////////////////////////////
	// Timeout for dismiss popup window
	// /////////////////////////////////////
	public interface OnTimeoutResetListener {
		void timeoutReset();
	}

	private final int MSG_TIMEOUT_DISMISS = 0;
	private static int mTimeoutForWindowDismiss = 10 * 1000;
	private OnTimeoutResetListener mOnTimeoutResetListener = null;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT_DISMISS:
				dismiss();
				break;
			default:
				break;
			}
		}
	};

	public void resetTimeout() {
		if (mOnTimeoutResetListener != null) {
			mOnTimeoutResetListener.timeoutReset();
		}
	}

	void stopTimeout() {
		// mHandler.removeMessages(MSG_TIMEOUT_DISMISS);
	}

	public void setTimeoutMs(int Millis) {
		mTimeoutForWindowDismiss = Millis;
		resetTimeout();
	}

	public void setTimeoutResetListener(OnTimeoutResetListener timeoutResetListener) {
		mOnTimeoutResetListener = timeoutResetListener;
	}
}
