/**
 *  Foryou Project - DemoID3Marquen
 *
 *  Copyright (C) Foryou 2013 - 2025
 *
 *  File:MarqueeTextView.java
 *
 *  Revision:
 *  
 *  2014骞�8鏈�5鏃�
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author ChenJia
 * @Data 2014骞�8鏈�5鏃�
 */
public class MarqueeText extends RelativeLayout {
	private static final String TAG = "MarqueeTextView";

	private int mMarqueeTextSize = 0;
	private String mMarqueeText = null;
	private int mMarqueeWidth = 200;
	private int mMarqueeTextLength = 0;
	private Drawable mMarqueeBackground = null;
	private ColorStateList mMarqueeTextColor = null;
	
	private TextView mHeadTv = null;
	private TextView mTailTv = null;

	private final int DELAY_FOR_MARQUEE = 3000;
	private final int MARQUEE_SPEED_PX_PER_SEC = 30;
	private final int DISTANCE_HEAD_AND_TAIL_PERCENT = 50;

	/**
	 * @param context
	 */
	public MarqueeText(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public MarqueeText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	@SuppressLint("NewApi")
	public MarqueeText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.marquee_text_view);
		mMarqueeText = a.getString(R.styleable.marquee_text_view_text);
		mMarqueeTextSize = a.getDimensionPixelSize(R.styleable.marquee_text_view_textSize, 16);
		mMarqueeTextColor = a.getColorStateList(R.styleable.marquee_text_view_textColor);
		mMarqueeWidth = a.getDimensionPixelSize(R.styleable.marquee_text_view_marquee_width, mMarqueeWidth);
		mMarqueeBackground = a.getDrawable(R.styleable.marquee_text_view_background);
		
		a.recycle();

		setBackground(mMarqueeBackground);
		
		mHeadTv = new TextView(context);
		mHeadTv.setId(R.id.tv_marquee_head);
		mHeadTv.setSingleLine();
		mHeadTv.setTextColor(mMarqueeTextColor != null ? mMarqueeTextColor : ColorStateList.valueOf(0xFFFFFFFF));
		mHeadTv.setGravity(Gravity.CENTER_VERTICAL);
		
		mTailTv = new TextView(context, attrs);
		mTailTv.setId(R.id.tv_marquee_tail);
		mTailTv.setSingleLine();
		mTailTv.setEllipsize(TruncateAt.END);
		mTailTv.setTextColor(mMarqueeTextColor != null ? mMarqueeTextColor : ColorStateList.valueOf(0xFFFFFFFF));
		mTailTv.setGravity(Gravity.CENTER_VERTICAL);

		
		LayoutParams lpHead = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		lpHead.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		addView(mHeadTv, lpHead);

		LayoutParams lpTail = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		lpTail.addRule(RelativeLayout.RIGHT_OF, R.id.tv_marquee_head);
		lpTail.setMargins(DISTANCE_HEAD_AND_TAIL_PERCENT, 0, 0, 0);
		addView(mTailTv, lpTail);

		setText(mMarqueeText);
		setTextSize(mMarqueeTextSize);

		setClickable(true);
		calcMarqueeTextStringLength();
	}

	private static final int MSG_START_MARQUEE = 0;

	private Handler mHander = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_START_MARQUEE:
				if (canMarquee()) {
					mTailTv.setVisibility(View.VISIBLE);
					createAnimation();
					mValueAnimator.start();
					mHeadTv.setFocusable(false);
					mTailTv.setFocusable(false);
					setFocusable(true);
				}
				break;
			}
		}
	};

	public void startMarquee() {
		startMarquee(DELAY_FOR_MARQUEE);
	}
	
	public void startMarquee(int delayToStartMs) {
		if (mValueAnimator != null) {
			mValueAnimator.cancel();
			mValueAnimator.setCurrentPlayTime(0);
		}
		mHeadTv.setVisibility(View.VISIBLE);
		mHander.removeMessages(MSG_START_MARQUEE);
		mHander.sendEmptyMessageDelayed(MSG_START_MARQUEE, delayToStartMs);
		mHeadTv.setFocusable(false);
		mTailTv.setFocusable(false);
		setFocusable(true);
	}
	
	public void stopMarquee(){
		mHander.removeMessages(MSG_START_MARQUEE);
		if(mValueAnimator != null){
			mValueAnimator.cancel();
			mValueAnimator.setCurrentPlayTime(0);
		}
		if(canMarquee()){
			mHeadTv.setVisibility(View.GONE);
			mTailTv.setVisibility(View.VISIBLE);
		}
		mHeadTv.setFocusable(false);
		mTailTv.setFocusable(false);
		setFocusable(true);
	}

	public void setText(CharSequence text) {
		mHeadTv.setText(text);
		mTailTv.setText(text);
		calcMarqueeTextStringLength();
		requestLayout();
	}

	public void setTextSize(float size) {
		mHeadTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
		mTailTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
		calcMarqueeTextStringLength();
	}

	public void setTextColor(int color) {
		mHeadTv.setTextColor(color);
		mTailTv.setTextColor(color);
	}

	public void setTypeface(Typeface tf) {
		mHeadTv.setTypeface(tf);
		mTailTv.setTypeface(tf);
		calcMarqueeTextStringLength();
	}

	private boolean canMarquee() {
		Paint tvPaint = mHeadTv.getPaint();
		int size = (int) tvPaint.measureText(mHeadTv.getText().toString());
		int width = getWidth();
//		Log.d(TAG, "acutal size:" + size + ", layout width:" + width + ", other width:" + (getRight() - getLeft()));
		if (width == 0) {
			return false;
		}
		return size > width ? true : false;
	}

	private float getDurationSecond(TextView tv) {
		Paint tvPaint = tv.getPaint();
		float size = tvPaint.measureText(tv.getText().toString());
		return size / MARQUEE_SPEED_PX_PER_SEC;
	}

	private int getScrollDistance(TextView tv) {
		Paint tvPaint = tv.getPaint();
		float size = tvPaint.measureText(tv.getText().toString());
		int width = getRight() - getLeft();
		return (int) (size + width * DISTANCE_HEAD_AND_TAIL_PERCENT / 100);
	}

	private ValueAnimator mValueAnimator = null;

	private void createAnimation() {
		float duration = getDurationSecond(mHeadTv);
		mValueAnimator = ObjectAnimator.ofInt(this, "scrollX", this.getScrollX(),
				this.getScrollX() + getScrollDistance(mHeadTv)).setDuration((long) (duration * 1000));
		mValueAnimator.setInterpolator(new LinearInterpolator());
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int childCount = getChildCount();
		int childLeft = 0;

		int layoutWidth = r - l;
//		for (int i = 0; i < childCount; i++) {
//			final View child = getChildAt(i);
//			if (child.getVisibility() != View.GONE) {
//
//				final int childHeight = child.getMeasuredHeight();
//				if ((mMarqueeTextLength < layoutWidth) && i == 1) {
//					child.setVisibility(View.GONE);
//				} else {
//					child.setVisibility(View.VISIBLE);
//				}
//				int childTop = getPaddingTop();
//
//				child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(), childTop + childHeight);
//				childLeft += mMarqueeTextLength + ((r - l) * DISTANCE_HEAD_AND_TAIL_PERCENT / 100);
//			}
//		}
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {

				final int childHeight = child.getMeasuredHeight();
				if ((mMarqueeTextLength < layoutWidth) && i == 1) {
					child.setVisibility(View.GONE);
				} else {
					if (mMarqueeTextLength < layoutWidth && i == 0) {
						childLeft = (r - l - mMarqueeTextLength) / 2;
					}
					child.setVisibility(View.VISIBLE);
				}
				int childTop = getPaddingTop();

				child.layout(childLeft, childTop,
						childLeft + child.getMeasuredWidth(), childTop
								+ childHeight);
				childLeft += mMarqueeTextLength
						+ ((r - l) * DISTANCE_HEAD_AND_TAIL_PERCENT / 100);
			}
		}
	}

	private void calcMarqueeTextStringLength() {
		Paint paint = mHeadTv.getPaint();
		mMarqueeTextLength = (int) paint.measureText(mHeadTv.getText().toString());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int maxChildHeight = 0;

		final int verticalPadding = getPaddingTop() + getPaddingBottom();
		final int horizontalPadding = getPaddingLeft() + getPaddingRight();

		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();

			int childWidthMode;
			if (lp.width == LayoutParams.WRAP_CONTENT) {
				childWidthMode = MeasureSpec.AT_MOST;
			} else {
				childWidthMode = MeasureSpec.EXACTLY;
			}

			int childHeightMode;
			if (lp.height == LayoutParams.WRAP_CONTENT) {
				childHeightMode = MeasureSpec.AT_MOST;
			} else {
				childHeightMode = MeasureSpec.EXACTLY;
			}

			if (i == 0)
				widthSize = mMarqueeTextLength;
			else
				widthSize = MeasureSpec.getSize(widthMeasureSpec);
			final int childWidthMeasureSpec = MeasureSpec
					.makeMeasureSpec(widthSize - horizontalPadding, childWidthMode);
			final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize - verticalPadding,
					childHeightMode);

			child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
		}
		if (heightMode == MeasureSpec.AT_MOST) {
			heightSize = maxChildHeight + verticalPadding;
		}
		setMeasuredDimension(widthSize, heightSize);
	}
}
