/**
 *  Adayo Project - CommonUI
 *
 *  Copyright (C) Adayo 2013 - 2025
 *
 *  File:ParkingWarningView.java
 *
 *  Revision:
 *  
 *  2014年7月10日
 *		- first revision
 *  
 */

package com.adayo.an6v.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

/**
 * @author ChenJia
 * @Data 2014年7月10日
 */
public class ParkingWarningView extends RelativeLayout {

	/**
	 * @param context
	 */
	public ParkingWarningView(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public ParkingWarningView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public ParkingWarningView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater.from(context).inflate(R.layout.layout_parking_warning, this);
		setClickable(true);
	}
}
