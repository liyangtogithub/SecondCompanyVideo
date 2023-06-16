/**
 *  Adayo Project - IPod
 *
 *  Copyright (C) Adayo 2013 - 2015
 *
 *  File:Clock.java
 *
 *  Revision:
 *  
 *  2013-9-12
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * @author ChenJia
 * @Data 2013-9-12
 */
public class Clock extends RelativeLayout {

	private boolean mAttached;
	private Calendar mCalendar;
	private SimpleDateFormat mClockFormat;
	private String mClockFormatString;
	private LayoutInflater mInflater;
	private RelativeLayout mClockLayout;

	private TextView mClockTv;
	private ImageView mAmPmIv;

	/**
	 * @param context
	 */
	public Clock(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public Clock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public Clock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mInflater = LayoutInflater.from(context);
		mClockLayout = (RelativeLayout) mInflater.inflate(R.layout.header_clock, this);
		mClockTv = (TextView) mClockLayout.findViewById(R.id.tv_clock);
		mAmPmIv = (ImageView) mClockLayout.findViewById(R.id.iv_am_pm_clock);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

			getContext().registerReceiver(mIntentReceiver, filter);
		}
		mCalendar = Calendar.getInstance(TimeZone.getDefault());
		updateClock();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			mAttached = false;
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
				if (mClockFormat != null) {
					mClockFormat.setTimeZone(mCalendar.getTimeZone());
				}
			}
			updateClock();
		}
	};

	private final void updateClock() {
		mCalendar.setTimeInMillis(System.currentTimeMillis());
		refresh();
	}

	private final void refresh() {
//		Context context = getContext();
//		boolean b24 = DateFormat.is24HourFormat(context);
//		int res;
//
//		if (b24) {
//			res = R.string.twenty_four_hour_time_format;
//			mAmPmIv.setVisibility(View.GONE);
//		} else {
//			res = R.string.twelve_hour_time_format;
//			mAmPmIv.setVisibility(View.VISIBLE);
//		}
//
//		SimpleDateFormat sdf;
//		String format = context.getString(res);
//		if (!format.equals(mClockFormatString)) {
//			mClockFormat = sdf = new SimpleDateFormat(format);
//			mClockFormatString = format;
//		} else {
//			sdf = mClockFormat;
//		}
//
//		String clockStr = sdf.format(mCalendar.getTime());
//		mClockTv.setText(clockStr);
//		
//		if (!b24) {
//			int fieldAmPm = mCalendar.get(Calendar.AM_PM);
//			if (fieldAmPm == Calendar.AM) {
//				mAmPmIv.setImageResource(R.drawable.header_clock_am_icon);
//			} else if (fieldAmPm == Calendar.PM) {
//				mAmPmIv.setImageResource(R.drawable.header_clock_pm_icon);
//			}
//		}
		Context context = getContext();
		boolean b24 = DateFormat.is24HourFormat(context);
		String time=DateFormat.getTimeFormat(getContext()).format(mCalendar.getTime());
		if (b24) {
			mAmPmIv.setVisibility(View.GONE);
		} else {
			mAmPmIv.setVisibility(View.VISIBLE);
		}
		if(b24){
			mClockTv.setText(time);
		}else{
			String time1=time.replace(":", "");
			String bb=time1.replace(" ", "");
			String dd=bb.replaceAll("[0-9]*", "");
			String cc=time.replaceAll(dd, "");
			mClockTv.setText(cc);
			int fieldAmPm = mCalendar.get(Calendar.AM_PM);
			if (fieldAmPm == Calendar.AM) {
				mAmPmIv.setImageResource(R.drawable.header_clock_am_icon);
			} else if (fieldAmPm == Calendar.PM) {
				mAmPmIv.setImageResource(R.drawable.header_clock_pm_icon);
			}
		}
	}
}
