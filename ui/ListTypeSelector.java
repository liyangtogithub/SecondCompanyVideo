package com.adayo.an6v.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ListTypeSelector extends LinearLayout {
	
	private static final String TAG = "ListTypeSelector";
	private Context mContext;
	private LinearLayout mRootView;
	private ListView mListView;
	private LinearLayout mLlListType;
	public ListTypeSelector(Context context) {
		this(context,null);
	}
	
	public ListTypeSelector(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}

	public ListTypeSelector(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}
	
	public void init(){
		mRootView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_list_type_selector, this);
		mLlListType = (LinearLayout) mRootView.findViewById(R.id.ll_list_type_id);
		mLlListType.setFocusable(true);
		mRootView.setFocusable(true);
		mRootView.setOrientation(VERTICAL);
		mRootView.setBackgroundResource(R.drawable.list_type_selector_bg);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				getResources().getDrawable(R.drawable.list_type_selector_bg).getIntrinsicWidth(),
				FrameLayout.LayoutParams.WRAP_CONTENT);
		mLlListType.setLayoutParams(params);
	}
	
	public void addView(int resId,int titleId,final OnClickListener onClickListener){
		TextView textView = new TextView(mContext);
		Drawable drawable = mContext.getResources().getDrawable(resId);
		drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
		Log.i(TAG,"textview up drawable " + drawable);
		textView.setBackgroundResource(resId);
		textView.setGravity(Gravity.CENTER);
		textView.setPadding(0,getResources().getDimensionPixelSize(R.dimen.list_type_text_padding_top_in_center_gravity), 0, 0);
		textView.setText(getResources().getString(titleId));
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimensionPixelSize(R.dimen.font_size_list_type_text));
		textView.setFocusable(true);
		textView.setEllipsize(TruncateAt.END);
		textView.setSingleLine();
		if(onClickListener != null){
			textView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onClickListener.onClick(v);
					clearAllListTypeButtonSelection();
					v.setSelected(true);
				}
			});
		}
		textView.setTextColor(getResources().getColorStateList(R.color.selector_list_type_text));
		mLlListType.addView(textView);
	}
	
	public void clearAllListTypeButtonSelection(){
		for (int i = 0; i < mLlListType.getChildCount(); i++) {
			mLlListType.getChildAt(i).setSelected(false);
		}
	}
	
	public void setSelected(int position){
		if(position >= mLlListType.getChildCount())
			return;
		clearAllListTypeButtonSelection();
		mLlListType.getChildAt(position).setSelected(true);
	}
	
}
