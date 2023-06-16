package com.adayo.an6v.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class SelectorPopupWindow extends PopupWindow {

	private static final String TAG = SelectorPopupWindow.class.getSimpleName();
	View mRootView;
	LinearLayout mLlSelector;
	Context mContext;
	public SelectorPopupWindow(Context context) {
		super(context);
		mContext = context;
		mRootView = LayoutInflater.from(context).inflate(R.layout.layout_selector_popup_window, null);
		mLlSelector = (LinearLayout) mRootView.findViewById(R.id.ll_selector_id);
		Drawable drawable = mContext.getResources().getDrawable(R.drawable.top_bar_selector_bg);
		setWidth(drawable.getIntrinsicWidth());
		setHeight(drawable.getIntrinsicHeight());
		setContentView(mRootView);
		setBackgroundDrawable(drawable);
		setOutsideTouchable(true);
	}
	
	@SuppressLint("NewApi")
	public TextView addItem(String info){
		
		TextView textView = new TextView(mContext);
		textView.setText(info);
		textView.setBackgroundResource(R.drawable.selector_topbar_selector_btn);
		textView.setTextSize(mContext.getResources().getDimensionPixelSize(R.dimen.topbar_device_text_size));
		if(mLlSelector.getChildCount() == 0){
			Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_top_bar_selector_expanded);
//			Log.d(TAG,"width " + drawable.getIntrinsicWidth());
			drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicWidth());
			textView.setCompoundDrawables(null, null, drawable, null);
//			textView.setCompoundDrawablesRelative(null, null, drawable, null);
			textView.setCompoundDrawablePadding(0);
			textView.setPadding(0, 0, mContext.getResources().getDimensionPixelSize(R.dimen.topbar_selector_first_item_padd_right), 0);
		}else {
			textView.setPadding(0, 0, 0, 0);
		}
//		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
//		params.gravity = Gravity.CENTER;
//		textView.setLayoutParams(params);
		textView.setGravity(Gravity.CENTER);
//		
		mLlSelector.addView(textView);
		return textView;
	}
	
	public void removeAllItem(){
		mLlSelector.removeAllViews();
	}

	public int getItemCount(){
		return mLlSelector.getChildCount();
	}

	public TextView[] updateItems(CharSequence[] infos) {
		if(infos == null || infos.length == 0){
			mLlSelector.removeAllViews();
			return null;
		}
		
		int c = infos.length - getItemCount();
		
		if(c > 0){
			while(c > 0){
				addItem("");
				c--;
			}
		}else if(c < 0){
			c = -c;
			while(c>0){
				mLlSelector.removeViewAt(0);
				c--;
			}
		}
		TextView[] tvs = new TextView[infos.length];
		for(int i = 0 ;i < mLlSelector.getChildCount();i++){
			TextView tv = (TextView) mLlSelector.getChildAt(i);
			tv.setText(infos[i]);
			tv.setVisibility(View.VISIBLE);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,mContext.getResources().getDimensionPixelSize(R.dimen.font_size_topbar_device_text));
//			Log.d(TAG, "[updateItems]:item " + infos[i]);
			tvs[i] = tv;
		}
		
//		Log.d(TAG, "[updateItems]:count " + mLlSelector.getChildCount());
		
		setHeight(infos.length * tvs[0].getBackground().getIntrinsicHeight());
		
		update();
//		android.view.ViewGroup.LayoutParams params =	mRootView.getLayoutParams();
//		if(params == null)
//			params = new FrameLayout.LayoutParams(getWidth(), getHeight());
//		params.height = infos.length * tvs[0].getBackground().getIntrinsicHeight();
//		mRootView.setLayoutParams(params);
//		update(getWidth(),infos.length * tvs[0].getBackground().getIntrinsicHeight());
//		Log.d(TAG, "height " + getHeight());
		return tvs;
	}
	
}
