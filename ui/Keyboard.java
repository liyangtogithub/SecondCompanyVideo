/**
 *  Adayo Project - AdayoUI
 *
 *  Copyright (C) Adayo 2015 - 2025
 *
 *  File:Keyboard.java
 *
 *  Revision:
 *  
 *  2013-12-19
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

/**
 * @author ChenJia
 * @Data 2013-12-19
 */
public class Keyboard extends RelativeLayout {
	public static final int MODE_GOTO = 0;
	public static final int MODE_PHONE = 1;

	public enum KeyboardMode {
		GOTO(0), INVALID(100);

		int mValue = 0;

		KeyboardMode(int value) {
			mValue = value;
		}

		public static final KeyboardMode intToKeyboardMode(int value) {
			switch (value) {
			case 0:
				return GOTO;
			default:
				return INVALID;
			}
		}
	}

	private KeyboardMode mMode = KeyboardMode.GOTO;

	private Button mKey0Btn;
	private Button mKey1Btn;
	private Button mKey2Btn;
	private Button mKey3Btn;
	private Button mKey4Btn;
	private Button mKey5Btn;
	private Button mKey6Btn;
	private Button mKey7Btn;
	private Button mKey8Btn;
	private Button mKey9Btn;

	private ImageView mDelIb;
	private Button mOkBtn;

	private TableLayout mKeyboardLayout;
	private RelativeLayout mKeyboardFirstLine;
	private RelativeLayout mInputAreaRl;

	/**
	 * @param context
	 */
	public Keyboard(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public Keyboard(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public Keyboard(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.keyboard);
		int mode = a.getInt(R.styleable.keyboard_keyboard_mode, MODE_GOTO);
		mMode = KeyboardMode.intToKeyboardMode(mode);
		a.recycle();

		initLayout(context);
	}

	private RelativeLayout mRootView;
	private LayoutInflater mLayoutInflater;

	private void initLayout(Context context) {
		mLayoutInflater = LayoutInflater.from(context);
		mRootView = (RelativeLayout) mLayoutInflater.inflate(R.layout.layout_keyboard, this);

		mInputAreaRl = (RelativeLayout) mRootView.findViewById(R.id.rl_keyboard_input_area_common);
		mKey0Btn = (Button) mRootView.findViewById(R.id.btn_key_0_common);
		mKey1Btn = (Button) mRootView.findViewById(R.id.btn_key_1_common);
		mKey2Btn = (Button) mRootView.findViewById(R.id.btn_key_2_common);
		mKey3Btn = (Button) mRootView.findViewById(R.id.btn_key_3_common);
		mKey4Btn = (Button) mRootView.findViewById(R.id.btn_key_4_common);
		mKey5Btn = (Button) mRootView.findViewById(R.id.btn_key_5_common);
		mKey6Btn = (Button) mRootView.findViewById(R.id.btn_key_6_common);
		mKey7Btn = (Button) mRootView.findViewById(R.id.btn_key_7_common);
		mKey8Btn = (Button) mRootView.findViewById(R.id.btn_key_8_common);
		mKey9Btn = (Button) mRootView.findViewById(R.id.btn_key_9_common);
		mDelIb = (ImageView) mRootView.findViewById(R.id.btn_key_del_common);
		mOkBtn = (Button) mRootView.findViewById(R.id.btn_key_ok_common);
		mKeyboardLayout = (TableLayout) mRootView.findViewById(R.id.tl_keyboard_layout);
		mKeyboardFirstLine = (RelativeLayout) mRootView.findViewById(R.id.rl_keyboard_first_line_layout);

		mKey0Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(0);
			}
		});
		mKey0Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(0);
			}
		});
		mKey1Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(1);
			}
		});
		mKey1Btn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(1);
			}
		});
		mKey2Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(2);
			}
		});
		mKey2Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(2);
			}
		});
		mKey3Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(3);
			}
		});
		mKey3Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(3);
			}
		});
		mKey4Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(4);
			}
		});
		mKey4Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(4);
			}
		});
		mKey5Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(5);
			}
		});
		mKey5Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(5);
			}
		});
		mKey6Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(6);
			}
		});
		mKey6Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(6);
			}
		});
		mKey7Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(7);
			}
		});
		mKey7Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(7);
			}
		});
		mKey8Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(8);
			}
		});
		mKey8Btn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(8);
			}
		});
		mKey9Btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNumClick(9);
			}
		});
		mKey9Btn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onNumLongClick(9);
			}
		});

		mDelIb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onDelClick();
			}
		});
		mDelIb.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onDelLongClick();
			}
		});
		mOkBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onOKClick();
			}
		});
		mOkBtn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onOKLongClick();
			}
		});

		// block divide click
		mKeyboardLayout.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		mKeyboardFirstLine.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		setMode(mMode);
	}

	private void onNumClick(int num) {
		mOnGotoKeyboardClickListener.onNumberClick(num);
	}

	private boolean onNumLongClick(int num) {
		if (mMode == KeyboardMode.GOTO) {
			return mOnGotoKeyboardLongClickListener.onNumberLongClick(num);
		} else {
			return false;
		}
	}

	private void onDelClick() {
		mOnGotoKeyboardClickListener.onDel();
	}

	private boolean onDelLongClick() {
		if (mMode == KeyboardMode.GOTO) {
			return mOnGotoKeyboardLongClickListener.onDelLongClick();
		} else {
			return false;
		}
	}

	private void onCancelClick() {
		mOnGotoKeyboardClickListener.onCancel();
	}

	private boolean onCancelLongClick() {
		return mOnGotoKeyboardLongClickListener.onCancelLongClick();
	}

	private void onOKClick() {
		mOnGotoKeyboardClickListener.onOk();
	}

	private boolean onOKLongClick() {
		return mOnGotoKeyboardLongClickListener.onOkLongClick();
	}

	// ///////////////////////////////////////
	// Get CANCEL/OK Button View
	// ///////////////////////////////////////
	public Button getOKButton() {
		return mOkBtn;
	}

	// ///////////////////////////////////////
	// Enable
	// ///////////////////////////////////////
	public void enableCancelButton(boolean enabled) {
		mDelIb.setEnabled(enabled);
	}

	public void enableOkButton(boolean enabled) {
		mOkBtn.setEnabled(enabled);
	}

	public void enableNumberButton(int num, boolean enabled) {
		switch (num) {
		case 0:
			mKey0Btn.setEnabled(enabled);
			break;
		case 1:
			mKey1Btn.setEnabled(enabled);
			break;
		case 2:
			mKey2Btn.setEnabled(enabled);
			break;
		case 3:
			mKey3Btn.setEnabled(enabled);
			break;
		case 4:
			mKey4Btn.setEnabled(enabled);
			break;
		case 5:
			mKey5Btn.setEnabled(enabled);
			break;
		case 6:
			mKey6Btn.setEnabled(enabled);
			break;
		case 7:
			mKey7Btn.setEnabled(enabled);
			break;
		case 8:
			mKey8Btn.setEnabled(enabled);
			break;
		case 9:
			mKey9Btn.setEnabled(enabled);
			break;
		}
	}

	// ///////////////////////////////////////
	// Goto, Phone Mode
	// ///////////////////////////////////////
	public void setMode(KeyboardMode keyboardMode) {
		applyMode(keyboardMode);
	}

	private void applyMode(KeyboardMode mode) {
		mMode = mode;
		mDelIb.setVisibility(View.VISIBLE);
		mOkBtn.setVisibility(View.VISIBLE);
	}

	// ///////////////////////////////////////
	// Custom Input area View
	// ///////////////////////////////////////
	public void setView(View view) {
		if (view != null) {
			mInputAreaRl.removeAllViews();
			mInputAreaRl.addView(view, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}

	// ///////////////////////////////////////
	// Long Click Listener
	// ///////////////////////////////////////
	public interface OnKeyboardLongClickListener {
		boolean onNumberLongClick(int number);

		boolean onDelLongClick();
	}

	public interface OnGotoKeyboardLongClickListener extends OnKeyboardLongClickListener {
		boolean onOkLongClick();

		boolean onCancelLongClick();
	}

	private OnGotoKeyboardLongClickListener mOnGotoKeyboardLongClickListener = new OnGotoKeyboardLongClickListener() {

		@Override
		public boolean onNumberLongClick(int number) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDelLongClick() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onOkLongClick() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onCancelLongClick() {
			// TODO Auto-generated method stub
			return false;
		}

	};

	public void setOnGotoKeyboardLongClickListener(OnGotoKeyboardLongClickListener onGotoKeyboardLongClickListener) {
		mOnGotoKeyboardLongClickListener = onGotoKeyboardLongClickListener;
	}

	// ///////////////////////////////////////
	// Click Listener
	// ///////////////////////////////////////
	public interface OnKeyboardClickListener {
		void onNumberClick(int number);

		void onDel();
	}

	public interface OnGotoKeyboardClickListener extends OnKeyboardClickListener {
		void onOk();

		void onCancel();
	}

	private OnGotoKeyboardClickListener mOnGotoKeyboardClickListener = new OnGotoKeyboardClickListener() {
		@Override
		public void onNumberClick(int number) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onDel() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onOk() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onCancel() {
			// TODO Auto-generated method stub
		}
	};

	public void setOnGotoKeyboardClickListener(OnGotoKeyboardClickListener onGotoKeyboardClickListener) {
		mOnGotoKeyboardClickListener = onGotoKeyboardClickListener;
	}

	// ///////////////////////////////////////
	// click event
	// ///////////////////////////////////////
	public void clickKey(int key) {
		switch (key) {
		case 0:
			if (mKey0Btn.isEnabled()) {
				onNumClick(0);
			}
			break;
		case 1:
			if (mKey1Btn.isEnabled()) {
				onNumClick(1);
			}
			break;
		case 2:
			if (mKey2Btn.isEnabled()) {
				onNumClick(2);
			}
			break;
		case 3:
			if (mKey3Btn.isEnabled()) {
				onNumClick(3);
			}
			break;
		case 4:
			if (mKey4Btn.isEnabled()) {
				onNumClick(4);
			}
			break;
		case 5:
			if (mKey5Btn.isEnabled()) {
				onNumClick(5);
			}
			break;
		case 6:
			if (mKey6Btn.isEnabled()) {
				onNumClick(6);
			}
			break;
		case 7:
			if (mKey7Btn.isEnabled()) {
				onNumClick(7);
			}
			break;
		case 8:
			if (mKey8Btn.isEnabled()) {
				onNumClick(8);
			}
			break;
		case 9:
			if (mKey9Btn.isEnabled()) {
				onNumClick(9);
			}
			break;
		}
	}

	public void clickDel() {
		onDelClick();
	}

	public void clickOk() {
		if (mOkBtn.isEnabled()) {
			onOKClick();
		}
	}

	public void clickCancel() {
		onCancelClick();
	}
}
