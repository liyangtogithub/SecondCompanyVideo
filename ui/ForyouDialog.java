/**
 *  Foryou Project - CommonUI
 *
 *  Copyright (C) Foryou 2013 - 2025
 *
 *  File:ForyouDialog.java
 *
 *  Revision:
 *  
 *  2014年11月3日
 *		- first revision
 *  
 */
package com.adayo.an6v.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author ChenJia
 * @Data 2014年11月3日
 */
public class ForyouDialog extends Dialog implements DialogInterface {
	private static final String TAG = "ForyouDialog";

	private CharSequence mMessage = null;

	private TextView mTitleTv = null;
	private TextView mMessageTv = null;
	private ImageView mIconIv = null;
	private Button mOkBtn = null;
	private Button mCancelBtn = null;
	private String mOkButtonName = null;
	private String mCancelButtonName = null;
	private ListView mListView = null;
	private View mView = null;
	private DialogListViewAdapter mAdapter = null;

	private RelativeLayout mTitlePanel = null;
	private LinearLayout mContentPanel = null;
	private RelativeLayout mButtonPanel = null;
	private int mCheckedItem = -1;

	public ForyouDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	public ForyouDialog(Context context, int theme) {
		super(context, R.style.ForyouDialogTheme);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initLayout();
	}

	private void initLayout() {
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setContentView(R.layout.layout_foryou_dialog);

		mTitleTv = (TextView) getWindow().findViewById(R.id.tv_dialog_title_name);
		mMessageTv = (TextView) getWindow().findViewById(R.id.tv_dialog_message);
		mTitlePanel = (RelativeLayout) getWindow().findViewById(R.id.rl_dialog_title_panel);
		mContentPanel = (LinearLayout) getWindow().findViewById(R.id.ll_content_panel);
		mButtonPanel = (RelativeLayout) getWindow().findViewById(R.id.rl_button_panel);
		mOkBtn = (Button) getWindow().findViewById(R.id.btn_dialog_ok);
		mOkBtn.setText(mOkButtonName);
		mOkBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mOKClickListener != null) {
					mOKClickListener.onClick(ForyouDialog.this, -1);
				}
			}
		});
		mCancelBtn = (Button) getWindow().findViewById(R.id.btn_dialog_cancel);
		mCancelBtn.setText(mCancelButtonName);
		mCancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCancelClickListener != null) {
					mCancelClickListener.onClick(ForyouDialog.this, -2);
				}
			}
		});

		boolean hasTitle = !TextUtils.isEmpty(mTitle);
		boolean hasIcon = mIconId > 0;
		if (hasTitle) {
			mTitleTv.setText(mTitle);
		} else {
			mTitleTv.setVisibility(View.GONE);
		}

		if (hasIcon) {
			Drawable left = getWindow().getContext().getResources().getDrawable(mIconId);
			left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
			mTitleTv.setCompoundDrawables(left, null, null, null);
			mTitleTv.setCompoundDrawablePadding(10);
		} else {
			mTitleTv.setCompoundDrawables(null, null, null, null);
		}

		if (!hasIcon && !hasTitle) {
			mTitlePanel.setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(mMessage)) {
			mMessageTv.setText(mMessage);
		} else {
			mContentPanel.setVisibility(View.GONE);
		}

		if (mListView != null) {
			mContentPanel.setVisibility(View.VISIBLE);
			mContentPanel.removeAllViews();
			LinearLayout.LayoutParams lpLv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			mContentPanel.addView(mListView, lpLv);
			mContentPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
			mContentPanel.setPadding(0, 15, 0, 15);
			mListView.setAdapter(mAdapter);
			if (mAdapter.getCount() <= 2) {
				mContentPanel.setPadding(0, 30, 0, 15);
			}

			if (mCheckedItem == -2) {
				mAdapter.setIconEnable(false);
			} else {
				mAdapter.setIconEnable(true);
				if (mCheckedItem >= 0) {
					mListView.setItemChecked(mCheckedItem, true);
					mListView.setSelection(mCheckedItem);
					Log.d(TAG, "checked:" + mListView.isItemChecked(mCheckedItem));
				}
			}
		}

		if (mView != null) {
			mContentPanel.setVisibility(View.VISIBLE);
			mContentPanel.removeAllViews();
			LinearLayout.LayoutParams lpLv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			mContentPanel.addView(mView, lpLv);
			mContentPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
			mContentPanel.setPadding(0, 0, 0, 0);
		}

		if (mOKClickListener == null) {
			mOkBtn.setVisibility(View.GONE);
		}

		if (mCancelClickListener == null) {
			mCancelBtn.setVisibility(View.GONE);
		}

		if (mOKClickListener == null && mCancelClickListener == null) {
			mButtonPanel.setVisibility(View.GONE);
		} else if (mListView != null) {
			mButtonPanel.setPadding(0, 0, 0, 15);
		}

		if (mView != null) {
			mButtonPanel.setPadding(0, 0, 0, 15);
		}
	}

	private CharSequence mTitle = null;

	public void setTitle(CharSequence title) {
		mTitle = title;
		if (mTitleTv != null) {
			mTitleTv.setText(title);
		}
	}

	private int mIconId = 0;

	public void setIcon(int iconId) {
		mIconId = iconId;
		if (mIconIv != null) {
			mIconIv.setImageResource(iconId);
		}
	}

	public void setMessage(CharSequence message) {
		mMessage = message;
		if (mMessageTv != null) {
			mMessageTv.setText(message);
		}
	}

	private OnClickListener mOKClickListener = null;
	private OnClickListener mCancelClickListener = null;

	protected void setOkButton(String name, OnClickListener onClickListener) {
		mOkButtonName = name;
		mOKClickListener = onClickListener;
	}

	protected void setCancelButton(String name, OnClickListener onClickListener) {
		mCancelButtonName = name;
		mCancelClickListener = onClickListener;
	}

	protected void setAdapter(DialogListViewAdapter adapter) {
		mAdapter = adapter;
	}

	protected void setListView(ListView listView) {
		mListView = listView;
	}

	public ListView getListView() {
		return mListView;
	}

	protected void setCheckedItem(int position) {
		mCheckedItem = position;
	}

	protected void setView(View view) {
		mView = view;
	}

	// /////////////////////////////////////
	// Dialog control
	// /////////////////////////////////////
	public static class Builder {
		private Context mContext = null;
		private LayoutInflater mLayoutInflater = null;
		private int mTheme = 0;

		private CharSequence mTitle = null;
		private CharSequence mMessage = null;
		private int mIconId = 0;
		private View mView = null;

		private String mOkButtonName = null;
		private OnClickListener mOKClickListener = null;
		private String mCancelButtonName = null;
		private OnClickListener mCancelClickListener = null;

		private CharSequence[] mItems = null;
		private int mCheckedItem = -1;
		private OnClickListener mItemsOnClickListener = null;

		private ListView mListView = null;

		public Builder(Context context) {
			mContext = context;
			mLayoutInflater = LayoutInflater.from(context);
		}

		public Builder setTitle(CharSequence title) {
			mTitle = title.toString();
			return this;
		}

		public Builder setMessage(CharSequence message) {
			mMessage = message;
			return this;
		}

		public Builder setIcon(int iconId) {
			mIconId = iconId;
			return this;
		}

		public Builder setView(View view) {
			mView = view;
			return this;
		}

		public Builder setOkButton(int resId, OnClickListener clickListener) {
			mOkButtonName = mContext.getString(resId);
			mOKClickListener = clickListener;
			return this;
		}

		public Builder setCancelButton(int resId, OnClickListener clickListener) {
			mCancelButtonName = mContext.getString(resId);
			mCancelClickListener = clickListener;
			return this;
		}

		public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, OnClickListener listener) {
			mItems = items;
			mCheckedItem = checkedItem;
			mItemsOnClickListener = listener;
			return this;
		}

		public ForyouDialog create() {
			final ForyouDialog dialog = new ForyouDialog(mContext, mTheme);

			if (!TextUtils.isEmpty(mTitle)) {
				dialog.setTitle(mTitle);
			}

			if (mIconId != 0) {
				dialog.setIcon(mIconId);
			}

			if (!TextUtils.isEmpty(mMessage)) {
				dialog.setMessage(mMessage);
			}

			if (mOKClickListener != null) {
				dialog.setOkButton(mOkButtonName, mOKClickListener);
			}

			if (mCancelClickListener != null) {
				dialog.setCancelButton(mCancelButtonName, mCancelClickListener);
			}

			if (mView != null) {
				dialog.setView(mView);
			}

			if (mItems != null) {
				createListView(dialog);
			}

			return dialog;
		}

		private void createListView(final ForyouDialog dialog) {
			mListView = (ListView) mLayoutInflater.inflate(R.layout.layout_dialog_listview, null);
			DialogListViewAdapter listAdapter = new DialogListViewAdapter(mContext, mItems);
			mListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					mItemsOnClickListener.onClick(dialog, position);
				}
			});
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			dialog.setCheckedItem(mCheckedItem);
			dialog.setListView(mListView);
			dialog.setAdapter(listAdapter);
		}

		public ListView getListView() {
			return mListView;
		}

		public ForyouDialog show() {
			ForyouDialog dialog = create();
			dialog.show();
			return dialog;
		}
	}

	private static class DialogListViewAdapter extends BaseAdapter {
		private LayoutInflater mInflater = null;
		private CharSequence[] mItems = null;
		private boolean mShowIcon = false;;

		public DialogListViewAdapter(Context context, CharSequence[] items) {
			super();
			mInflater = LayoutInflater.from(context);
			mItems = items;
		}

		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public Object getItem(int position) {
			return mItems[position];
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		public void setIconEnable(boolean enable) {
			mShowIcon = enable;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.layout_dialog_listview_items, null);
				viewHolder = new ViewHolder();
				viewHolder.mTitleTv = (TextView) convertView.findViewById(R.id.tv_dialog_listview_title);
				viewHolder.mIconIv = (ImageView) convertView.findViewById(R.id.iv_dialog_listview_icon);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			viewHolder.mTitleTv.setText(mItems[position]);
			if (mShowIcon) {
				viewHolder.mIconIv.setVisibility(View.VISIBLE);
			} else {
				viewHolder.mIconIv.setVisibility(View.GONE);
			}
			return convertView;
		}
	}

	private static class ViewHolder {
		public TextView mTitleTv = null;
		public ImageView mIconIv = null;
	}
}
