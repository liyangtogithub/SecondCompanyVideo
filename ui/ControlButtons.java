package com.adayo.an6v.ui;


import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by xryu on 2014/10/18.
 * 
 * 
 * 底栏按钮分两部分，第一部分是第一页按钮，第二部分是“更多”按钮（第二页按钮）
 * 
 * 默认显示第一页的按钮
 * 
 * 
 * 第一页按钮用的RelativeLayout布局，按钮的布局是第一个按钮在最左边，中间三个按钮连在一块放在中间，第五个按钮最左边。其中中间三个按钮在LinearLayout布局内
 * 
 * 第二页按钮用的是GridLayout 加上 一个TextView。其中，TextView按钮单独用来表示隐藏第二页按钮。其它的按钮全部在GridLayout内。
 * 第二页按钮布局的原则是，所有的功能按钮尽可能地排列对齐。一行最多放五个按钮。当有8个按钮的时候，那么布局会是
 * A B C D   T
 * E F G H
 * （T为隐藏第二页的按钮，A、B、C、D、E、F、G、H分别为不同的按钮,具体效果可以参考DVD的效果图）
 * 
 * 当有7个按钮的时候，排列如上图，只是没有H按钮
 * 
 * 当有6个按钮的时候，排列就变成如下了
 * A B C     T
 * E F G
 * 
 * 当有5个按钮的时候，排列就如下了
 * A B C D E T
 * 
 * 
 * 使用public void makeFirstPageButtons(int[] ids,int[] bgIds,int[] titleIds,OnClickListener onClickListener)
 * 函数，会生成第一页的所有按钮。
 * 
 * 也可以使用ad
 * 
 * 
 * 
 * 使用public void makeSecondPageButtons(int[] ids,int[] bgIds,int[] titleIds,OnClickListener onClickListener)
 * 函数，会生成第二页的所有按钮。
 * 
 * 使用public void showWhatPageButtons(int page)控制显示第几页按钮
 * 
 *  
 * 
 * 
 * 
 * 
 * 
 */
public class ControlButtons extends RelativeLayout{
    private static final String TAG = ControlButtons.class.toString();
	private static final int START_ALIGN_PARENT_RIGHT_COUNT = 4;
	private static final int SECOND_PAGE_MAX_COL_NUM = 5;
	private static final int MAX_BUTTON_NUM = 6;
	
	//for show/hide ControlButtons
	public static final int SHOW_FIRST_PAGE_BUTTONS = 0;
	public static final int SHOW_SECOND_PAGE_BUTTONS = 1;
	public static final int HIDE_CONTROL_BUTTONS = -1;
	
	
	protected static final int MSG_BACK_TO_FIRST_PAGE = 1;
	private static final long TIME_SHOW_SECOND_PAGE = 8*1000;
	
	
	
    protected Context mContext = null;
    protected RelativeLayout mFirstPageButtonsRl = null;
    protected LinearLayout mFirstPageMiddleButtonsLl = null;
    protected GridLayout mSecondPageButtonsGl = null;
    protected RelativeLayout mSecondPageButtonsRl = null;
//    protected ArrayList<Integer> mFirstPageButtonsId = new ArrayList<Integer>();
//    protected ArrayList<Integer> mFirstPageButtonsIcon = new ArrayList<Integer>();
//    protected ArrayList<Integer> mSecondPageButtonsIcon = new ArrayList<Integer>();
//    protected ArrayList<Integer> mSecondPageButtonsId = new ArrayList<Integer>();
//    protected ArrayList<Integer> mSecondPageButtonsTextId = new ArrayList<Integer>();
    
    
    
    protected View.OnClickListener mOnClickListener = null;
	private View mHideSecondPageButtonsBtn;
//	private View[] mFirstPageButtonViews;
//	private View[] mSecondPageButtonViews;
	
	protected ArrayList<View> mFirstPageButtonsList = new ArrayList<View>();
	protected ArrayList<View> mSecondPageButtonsList = new ArrayList<View>();

	public View mRootView = null;
	
	
	Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_BACK_TO_FIRST_PAGE:
				
				showWhatPageButtons(SHOW_FIRST_PAGE_BUTTONS);
				
				break;

			default:
				break;
			}
		}
		
	};
	
	
	public ControlButtons(Context context){
		this(context,null);
	}
	
	public ControlButtons(Context context,AttributeSet attrs){
		this(context,attrs,0);
	}
	
	public ControlButtons(Context context,AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		mContext = context;
		if(isInEditMode())
			return;
		
		mRootView = LayoutInflater.from(context).inflate(R.layout.control_buttons, this);
		
		if(mFirstPageButtonsRl == null)
            mFirstPageButtonsRl = (RelativeLayout) mRootView.findViewById(R.id.ll_first_buttons_id);
        if(mSecondPageButtonsGl == null)
            mSecondPageButtonsGl = (GridLayout) mRootView.findViewById(R.id.gl_second_buttons_id);
        if(mSecondPageButtonsRl == null)
        	mSecondPageButtonsRl = (RelativeLayout) mRootView.findViewById(R.id.ll_second_buttons_id);
        if(mHideSecondPageButtonsBtn == null)
        	mHideSecondPageButtonsBtn = mRootView.findViewById(R.id.hide_second_page_buttons_id);
        if(mFirstPageMiddleButtonsLl == null){
        	mFirstPageMiddleButtonsLl = new LinearLayout(mContext,null,0);
        	mFirstPageMiddleButtonsLl.setId(R.id.ll_play_control_id);
        	addFirstPageMiddleButtons(mFirstPageMiddleButtonsLl);
        }
        
        mFirstPageButtonsRl.setFocusable(true);
        mFirstPageMiddleButtonsLl.setFocusable(true);
        mSecondPageButtonsGl.setFocusable(true);
        
        setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Log.d(TAG, "[onFocusChange]:has focus " + hasFocus);
			}
		});
	}
	
    public ControlButtons(View layoutView){
    	super(layoutView.getContext());
//        mMainActivity = activity;
        mContext = layoutView.getContext();
        if(mFirstPageButtonsRl == null)
            mFirstPageButtonsRl = (RelativeLayout) layoutView.findViewById(R.id.ll_first_buttons_id);
        if(mSecondPageButtonsGl == null)
            mSecondPageButtonsGl = (GridLayout) layoutView.findViewById(R.id.gl_second_buttons_id);
        if(mSecondPageButtonsRl == null)
        	mSecondPageButtonsRl = (RelativeLayout)layoutView.findViewById(R.id.ll_second_buttons_id);
//        mFirstPageButtonsId.clear();
//        mFirstPageButtonsIcon.clear();
//        mFirstPageButtonsId.add(R.id.ib_goto_list_id);
//        mFirstPageButtonsId.add(R.id.ib_play_prev_id);
//        mFirstPageButtonsId.add(R.id.ib_play_pause_id);
//        mFirstPageButtonsId.add(R.id.ib_play_next_id);
//        mFirstPageButtonsIcon.add(R.drawable.ic_goto_list_btn_n);
//        mFirstPageButtonsIcon.add(R.drawable.ic_play_prev_btn_n);
//        mFirstPageButtonsIcon.add(R.drawable.ic_play_btn);
//        mFirstPageButtonsIcon.add(R.drawable.ic_play_next_btn_n);
        if(mHideSecondPageButtonsBtn == null)
        	mHideSecondPageButtonsBtn = layoutView.findViewById(R.id.hide_second_page_buttons_id);
        if(mFirstPageMiddleButtonsLl == null){
        	mFirstPageMiddleButtonsLl = new LinearLayout(mContext,null,0);
        	mFirstPageMiddleButtonsLl.setId(R.id.ll_play_control_id);
        	addFirstPageMiddleButtons(mFirstPageMiddleButtonsLl);
        }
        mRootView = (View) mFirstPageButtonsRl.getParent();
    }
    
    public ControlButtons(Activity activity){
    	super(activity.getApplicationContext());
    	 mContext = activity.getApplicationContext();
         if(mFirstPageButtonsRl == null)
             mFirstPageButtonsRl = (RelativeLayout) activity.findViewById(R.id.ll_first_buttons_id);
         if(mSecondPageButtonsGl == null)
             mSecondPageButtonsGl = (GridLayout) activity.findViewById(R.id.gl_second_buttons_id);
         if(mSecondPageButtonsRl == null)
         	mSecondPageButtonsRl = (RelativeLayout)activity.findViewById(R.id.ll_second_buttons_id);
//         mFirstPageButtonsId.clear();
//         mFirstPageButtonsIcon.clear();
//         mFirstPageButtonsId.add(R.id.ib_goto_list_id);
//         mFirstPageButtonsId.add(R.id.ib_play_prev_id);
//         mFirstPageButtonsId.add(R.id.ib_play_pause_id);
//         mFirstPageButtonsId.add(R.id.ib_play_next_id);
//         mFirstPageButtonsIcon.add(R.drawable.ic_goto_list_btn_n);
//         mFirstPageButtonsIcon.add(R.drawable.ic_play_prev_btn_n);
//         mFirstPageButtonsIcon.add(R.drawable.ic_play_btn);
//         mFirstPageButtonsIcon.add(R.drawable.ic_play_next_btn_n);
         if(mHideSecondPageButtonsBtn == null)
         	mHideSecondPageButtonsBtn = activity.findViewById(R.id.hide_second_page_buttons_id);
         if(mFirstPageMiddleButtonsLl == null){
         	mFirstPageMiddleButtonsLl = new LinearLayout(mContext,null,0);
         	mFirstPageMiddleButtonsLl.setId(R.id.ll_play_control_id);
         	addFirstPageMiddleButtons(mFirstPageMiddleButtonsLl);
         }
         
         mRootView = (View) mFirstPageButtonsRl.getParent();
    }
    private void addFirstPageMiddleButtons(View view) {
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
    			(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.CENTER_IN_PARENT);
    	mFirstPageMiddleButtonsLl.setLayoutParams(params);
	}


	/**
     * 
     * @param id TextView的ID
     * @param bgId TextView的背景
     * @param titleId 如果没有标题，则设成View.NO_ID
     * @param onClickListener 
     * @param onTouchListener 
     * @param onLongClickListener 
     * @return
     */
    public TextView makeTextView(int id,int bgId,int titleId, final OnClickListener onClickListener, final OnLongClickListener onLongClickListener, final OnTouchListener onTouchListener){
    	TextView tv = new TextView(mContext,null,0);
    	tv.setId(id);
    	tv.setBackgroundResource(bgId);
    	if(titleId != View.NO_ID)
    		tv.setText(titleId);
    	tv.setGravity(Gravity.CENTER);
    	tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimensionPixelSize(R.dimen.font_size_control_buttons));
    	tv.setTextColor(mContext.getResources().getColor(R.color.control_buttons_text_color));
    	tv.setPadding(0, mContext.getResources().getDimensionPixelSize(R.dimen.control_buttons_text_padding_top_in_center_gravity), 0, 0);
    	tv.setSingleLine();
    	try {
    		if(Locale.getDefault().getLanguage().startsWith("ru")){
    			tv.setWidth(mContext.getResources().getDrawable(R.drawable.control_buttons_p).getIntrinsicWidth());
    			tv.setEllipsize(TruncateAt.END);
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	tv.setFocusable(true);
		if (onClickListener != null)
			tv.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					refreshReturnFirstPage();
					onClickListener.onClick(v);
				}
			});
		if (onLongClickListener != null)
			tv.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					refreshReturnFirstPage();
					return onLongClickListener.onLongClick(v);
				}
			});
		if (onTouchListener != null)
			tv.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					refreshReturnFirstPage();
					return onTouchListener.onTouch(v, event);
				}
			});
    	return tv;
    }

	/**
     * 
     * @param id 按钮id
     * @param bgId 背景id
     * @param titleId 按钮名字符串id
     * @param onClickListener 单击事件回调
     */
    public void addTextViewToFirstPageButtonsLastIndex(int id,int bgId,int titleId,OnClickListener onClickListener,OnLongClickListener onLongClickListener,OnTouchListener onTouchListener){
    	TextView tv = makeTextView(id, bgId, titleId,onClickListener,onLongClickListener,onTouchListener);
    	addToFirstPageButtonsLastIndex(tv);
    }
    
	public void addToFirstPageButtonsLastIndex(View view) {
		if (mFirstPageButtonsRl == null)
			return;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		// -1：表示减去中间的LinearLayout
		int firstPageButtonsCount = mFirstPageButtonsRl.getChildCount() - 1 + mFirstPageMiddleButtonsLl.getChildCount();
		int leftViewId = View.NO_ID;
		View leftView = null;
		try {
			leftView = mFirstPageButtonsRl.getChildAt(mFirstPageButtonsRl.getChildCount() - 1);
			leftViewId =leftView.getId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boolean alignParentRight = firstPageButtonsCount >= START_ALIGN_PARENT_RIGHT_COUNT;
		if(alignParentRight){
			if(leftView != null && firstPageButtonsCount > START_ALIGN_PARENT_RIGHT_COUNT){
				RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams
						(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
				params2.addRule(RelativeLayout.RIGHT_OF, mFirstPageMiddleButtonsLl.getId());
				mFirstPageButtonsRl.updateViewLayout(leftView, params2);
			}
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		}
		else{
			
			if(firstPageButtonsCount == 0){
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
		}
		if (view instanceof TextView || view instanceof Button) {
			((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX,mContext.getResources().getDimensionPixelSize(R.dimen.font_size_control_buttons));
			((TextView) view).setTextColor(mContext.getResources().getColor(R.color.control_buttons_text_color));
		}
		if(firstPageButtonsCount >= MAX_BUTTON_NUM - 1){
			RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams
					(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			params2.addRule(RelativeLayout.RIGHT_OF,mFirstPageButtonsRl.getChildAt(1).getId());
			mFirstPageButtonsRl.updateViewLayout(mFirstPageMiddleButtonsLl,params2);
		}
		if(firstPageButtonsCount == 0){
			view.setLayoutParams(params);
			mFirstPageButtonsRl.addView(view);
		}else if(firstPageButtonsCount < START_ALIGN_PARENT_RIGHT_COUNT){
			mFirstPageMiddleButtonsLl.addView(view);
		}else{
			view.setLayoutParams(params);
			mFirstPageButtonsRl.addView(view);
		}
		mFirstPageButtonsList.add(view);
	}
    public void addViewToFirstPageButtonsLastIndex(View view){
    	if(mFirstPageButtonsRl == null)
    		return;
    	addToFirstPageButtonsLastIndex(view);
    }
    
    
    /**
     * 
     * @param ids 每一个按钮的Id
     * @param bgIds 每一个按钮的背景Id
     * @param titleIds 每一个按钮的Title（按钮名）的Id
     * @param onClickListener 所有的按钮共用一个监听回调接口
     */
    public void makeFirstPageButtons(int[] ids,int[] bgIds,int[] titleIds,OnClickListener onClickListener,OnLongClickListener onLongClickListener,OnTouchListener onTouchListener){
    	
    	Log.e(TAG, "makeFirstPageButtons");
    	
    	if(ids == null || bgIds == null || titleIds == null)
    		return;
    	
    	if(ids.length != bgIds.length || bgIds.length != titleIds.length)
    		return;
    	
    	if(ids.length == 0)
    		return;
    	initFirstPageButtonsLayout();
    	for(int i = 0;i < ids.length;i++){
    		mFirstPageButtonsList.add(makeTextView(ids[i], bgIds[i], titleIds[i],onClickListener,onLongClickListener,onTouchListener));
    	}
    	makeFirstPageButtons(mFirstPageButtonsList);
    	
//    	addFocusables(mFirstPageButtonsList, View.FOCUS_BACKWARD | View.FOCUS_DOWN | View.FOCUS_FORWARD | View.FOCUS_LEFT | View.FOCUS_RIGHT | View.FOCUS_UP,View.FOCUSABLES_ALL);
    }
    
    @SuppressLint("NewApi")
	private void makeFirstPageButtons(ArrayList<View> views){
    	if(views == null)
    		return;
    	if(views.size() == 0)
    		return;
    	
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    	views.get(0).setLayoutParams(params);
    	mFirstPageButtonsRl.addView(views.get(0));
    	if(views.size() <= 1)
    		return;
		for(int i = 1;i < views.size() && i < START_ALIGN_PARENT_RIGHT_COUNT;i++){
			mFirstPageMiddleButtonsLl.addView(views.get(i));
		}
		if(views.size() <= START_ALIGN_PARENT_RIGHT_COUNT)
			return;
		int leftViewId = mFirstPageMiddleButtonsLl.getId();
		View view = null;
		for(int i = START_ALIGN_PARENT_RIGHT_COUNT;i < views.size() - 1;i++){
			view = views.get(i);
			params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.RIGHT_OF, leftViewId);
			view.setLayoutParams(params);
			mFirstPageButtonsRl.addView(view);
			leftViewId = view.getId();
		}
		view = views.get(views.size() - 1);
		params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		view.setLayoutParams(params);
		mFirstPageButtonsRl.addView(view);
		
		params = (LayoutParams) mFirstPageMiddleButtonsLl.getLayoutParams();
		if(views.size() >= MAX_BUTTON_NUM){
			params.removeRule(RelativeLayout.CENTER_IN_PARENT);
			params.addRule(RIGHT_OF,views.get(0).getId());
		}else{
			params.removeRule(RelativeLayout.RIGHT_OF);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
		}
		
	}
    
    /**
     * 
     * @param ids 每一个按钮的Id
     * @param bgIds 每一个按钮的背景Id
     * @param titleIds 每一个按钮的Title（按钮名）的Id
     * @param onClickListener 所有的按钮共用一个监听回调接口
     */
    public void makeSecondPageButtons(int[] ids,int[] bgIds,int[] titleIds,OnClickListener onClickListener,OnLongClickListener onLongClickListener,OnTouchListener onTouchListener){
    	if(ids == null || bgIds == null || titleIds == null)
    		return;
    	
    	if(ids.length != bgIds.length || bgIds.length != titleIds.length)
    		return;
    	
    	if(ids.length == 0)
    		return;
    	
    	initSecondPageButtonsLayout();
    	
        for(int i = 0 ;i < ids.length;i++){
        	mSecondPageButtonsList.add(makeTextView(ids[i], bgIds[i],titleIds[i],onClickListener,onLongClickListener,onTouchListener));
        }
        makeSecondPageButtons(mSecondPageButtonsList);
        if(mHideSecondPageButtonsBtn != null)
        	mHideSecondPageButtonsBtn.setOnClickListener(onClickListener);
        
        for (View view : mSecondPageButtonsList) {
			view.setOnFocusChangeListener(new OnFocusChangeListener() {
				
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					refreshReturnFirstPage();
				}
			});
		}
        
//        addFocusables(mSecondPageButtonsList, View.FOCUS_BACKWARD | View.FOCUS_DOWN | View.FOCUS_FORWARD | View.FOCUS_LEFT | View.FOCUS_RIGHT | View.FOCUS_UP,View.FOCUSABLES_ALL);
    }
    
    public void makeSecondPageButtons(ArrayList<View> views){
    	if(views == null)
            return;
        
    	if(views.size() == 0)
    		return;
    	
    	
    	//计算rowCount的时候，首先要算出最少需要多少行，rowCount行最多可以排下rowCount*SECOND_PAGE_MAX_COL_NUM个按钮
    	//比rowCount*SECOND_PAGE_MAX_COL_NUM还要少的按钮，肯定可以在rowCount行内排好
    	int rowCount = (int) Math.ceil(views.size() / (double)SECOND_PAGE_MAX_COL_NUM);
    	
    	//定下rowCount后，自然就很容易地确定出每列排多少个。
    	int colCount = (int) Math.ceil((double)views.size() / rowCount);
    	
    	
    	
    	Log.e(TAG, "[makeSecondPageButtons]:rowCount " + rowCount + " colCount " + colCount);
    	mSecondPageButtonsGl.setRowCount(rowCount);
    	mSecondPageButtonsGl.setColumnCount(colCount);
    	
        for(int i = 0 ;i < views.size();i++){
        	View view = views.get(i);
            int row = i / colCount;
            int col = i%colCount;
            mSecondPageButtonsGl.addView(view,makeGridLayoutParams(row, col));
        }
    }
    
    public GridLayout.LayoutParams makeGridLayoutParams(int row,int col){
    	GridLayout.Spec rowSpec = GridLayout.spec(row);
        GridLayout.Spec colSpec = GridLayout.spec(col);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec,colSpec);
        return params;
    }
    
    public void addTextViewToSecondPageButtonsLastIndex(int id,int bgId,int titleId,OnClickListener onClickListener,OnLongClickListener onLongClickListener,OnTouchListener onTouchListener){
    	TextView tv = makeTextView(id, bgId, titleId,onClickListener,onLongClickListener,onTouchListener);
    	addViewToSecondPageButtonsLastIndex(tv);
    }
    
    public void addViewToSecondPageButtonsLastIndex(View view){
    	int viewCount = mSecondPageButtonsList.size() + 1;
    	
    	int beforeRowCount = (int) Math.ceil(mSecondPageButtonsList.size() / (double)SECOND_PAGE_MAX_COL_NUM);
    	
    	int beforeColCount = (int) Math.ceil((double)mSecondPageButtonsList.size() / beforeRowCount);
    	
    	int afterRowCount = (int) Math.ceil(viewCount / (double)SECOND_PAGE_MAX_COL_NUM);
    	
    	int afterColCount = (int) Math.ceil((double)viewCount / beforeRowCount);
    	
    	mSecondPageButtonsList.add(view);
    	
    	//如果需要排列的行数和列数都不变的话，直接添加即可，则否所有的按钮都要重新排列.
    	if(beforeRowCount == afterRowCount && beforeColCount == afterColCount){
    		int col = (viewCount - 1)% afterColCount;
    		int row = afterRowCount - 1;
            mSecondPageButtonsGl.addView(view,makeGridLayoutParams(row, col));
    	}else{
    		mSecondPageButtonsGl.removeAllViews();
    		makeSecondPageButtons(mSecondPageButtonsList);
    	}
    }
    
    
    /**
     * 设置隐藏第二页按钮的OnClickListener.一般用于使用addViewToSecondPageButtonsLastIndex函数设置第二页按钮之后。也可以随意调用该函数。
     * 默认在makeSecondPageButtons中会设置传入的OnClickListener。
     * @param onClickListener
     */
    public void setHideSecondPageButtonsButtonClickListener(OnClickListener onClickListener){
    	if(mHideSecondPageButtonsBtn != null)
    		mHideSecondPageButtonsBtn.setOnClickListener(onClickListener);
    }
    
    
    /**
     * 更新对应Id的ImageButton的Image Source
     * 在第一页按钮中查找
     * @param Id
     * @param resId
     */
    public void updateFirstPageButtonIcon(int id,int resId) {
		updateImageButtonIcon(mFirstPageButtonsList, id,resId);
	}
    
    /**
     * 更新对应Id的ImageButton的Image Source
     * 在第二页按钮中查找
     * @param Id
     * @param resId
     */
    public void updateSecondPageButtonIcon(int id,int resId){
    	updateImageButtonIcon(mSecondPageButtonsList, id, resId);
    }
    
    /**
     * 更新对应Id的ImageButton的Image Source
     * 在第一页按钮中和第二页按钮中查找
     * @param Id
     * @param resId
     */
    public void updateImageButtonIcon(int Id,int resId){
       updateImageButtonIcon(mFirstPageButtonsList,Id,resId);
       updateImageButtonIcon(mSecondPageButtonsList,Id,resId);
       
    }
    
    /**
     * 更新第一页按钮的背景
     * 
     * @param id 对应button的ID
     * @param bgId 背景资源ID
     */
    public void updateFirstPageButtonBackground(int id,int bgId){
    	View view = findViewById(mFirstPageButtonsList, id);
    	if(view != null)
    		view.setBackgroundResource(bgId);
    }
    /**
     * 
     * 更新第二页按钮的背景
     * 
     * @param id 对应button的ID
     * @param bgId 背景资源ID
     */
    public void updateSecondPageButtonBackground(int id,int bgId){
    	View view = findViewById(mSecondPageButtonsList, id);
    	if(view != null)
    		view.setBackgroundResource(bgId);
    }
    
    /**
     * 更新按钮背景图片
     * 在第一页和第二页的按钮查找需要更新的按钮，更新第一页找到的按钮，同时也会更新在第二页找到的按钮
     *
     * 
     * @param id 对应button的ID
     * @param bgId 背景资源ID
     */
    public void updateButtonBackground(int id,int bgId) {
    	updateFirstPageButtonBackground(id, bgId);
		updateSecondPageButtonBackground(id, bgId);
	}
    
    /**
     * 更新按钮的字符串
     * 先在第一页按钮搜索，然后在第二页按钮搜索
     * 
     * @param id 欲更新的button 的 id
     * @param titleId 字符串id
     */
    public void updateButtonText(int id,int titleId){
    	updateButtonText(mFirstPageButtonsList, id, titleId);
    	updateButtonText(mSecondPageButtonsList, id, titleId);
    }
    
    /**
     * 更新第一页按钮的字符串
     * @param id 欲更新的button 的 id
     * @param titleId 字符串id
     */
    public void updateFirstPageButtonText(int id,int titleId){
    	updateButtonText(mFirstPageButtonsList, id, titleId);
    }
    
    
    /**
     * 更新第二页按钮的字符串
     * @param id 欲更新的button 的 id
     * @param titleId 字符串id
     */
    public void updateSecondPageButtonText(int id,int titleId){
    	updateButtonText(mSecondPageButtonsList, id, titleId);
    }
    
    
    private View findViewById(ArrayList<View> views,int id){
    	for (View view : views) {
			if(view.getId() == id)
				return view;
		}
    	return null;
    }

    private  boolean updateImageButtonIcon(ArrayList<View> views,int Id,int resId){
        if(views.size() <= 0)
            return false;
        int count = views.size();
        for(int i = 0;i < count;i++){
            View view = views.get(i);
            if(view != null && view.getId() == Id && view instanceof ImageButton){
                ((ImageButton)view).setImageResource(resId);
                return true;
            }
        }
        return false;
    }
    
    
    private boolean updateButtonText(ArrayList<View>views,int id,int titleId){
    	if(views.size() <= 0)
            return false;
        int count = views.size();
        for(int i = 0;i < count;i++){
            View view = views.get(i);
            if(view != null && view.getId() == id && (view instanceof TextView || view instanceof Button)){
                ((TextView)view).setText(titleId);
                return true;
            }
        }
        return false;
    }
    
    protected void setFirstPageButtonsClickListener(View.OnClickListener onClickListener){
    	if(mFirstPageButtonsList == null)
    		return;
        int count = mFirstPageButtonsList.size();
        for(int i = 0 ; i < count;i++){
            mFirstPageButtonsList.get(i).setOnClickListener(onClickListener);
        }
    }
    /**
     * 
     * @param page 0表示显示第一页按钮，1表示显示第二页按钮,-1表示都不显示
     */
//    public void showWhatPageButtons(int page){
//    	showWhatPageButtons(page, true);
//    }
    
    public void showWhatPageButtons(int page){
    	mHandler.removeMessages(MSG_BACK_TO_FIRST_PAGE);
        if(page == SHOW_FIRST_PAGE_BUTTONS) {
            
            boolean hasFocus = false;
			for (View view : mSecondPageButtonsList) {
				hasFocus |= view.hasFocus();
			}
			
			hasFocus |= mHideSecondPageButtonsBtn.hasFocus();
						            
			mRootView.setVisibility(View.VISIBLE);
			mSecondPageButtonsRl.setVisibility(View.GONE);
			mFirstPageButtonsRl.setVisibility(View.VISIBLE);
			
            if(hasFocus){
            	if(mFirstPageButtonsList.size() > 0){
            		mFirstPageButtonsList.get(0).requestFocus();
            	}
            }
        }
        else if(page == SHOW_SECOND_PAGE_BUTTONS){
            
            boolean hasFocus = false;
			for (View view : mFirstPageButtonsList) {
				hasFocus |= view.hasFocus();
			}
			mRootView.setVisibility(View.VISIBLE);
			mFirstPageButtonsRl.setVisibility(View.GONE);
			mSecondPageButtonsRl.setVisibility(View.VISIBLE);
			if(mSecondPageButtonsList != null && mSecondPageButtonsList.size() > 0){
				mHandler.sendEmptyMessageDelayed(MSG_BACK_TO_FIRST_PAGE, TIME_SHOW_SECOND_PAGE);
			}
						
            if(hasFocus){
            	if(mSecondPageButtonsList.size() > 0){
            		mSecondPageButtonsList.get(0).requestFocus();
            	}
            }
        }
        else if(page == HIDE_CONTROL_BUTTONS){
//        	mFirstPageButtonsRl.setVisibility(View.GONE);
//        	mSecondPageButtonsGl.setVisibility(View.GONE);
        	mRootView.setVisibility(View.GONE);
        }
    }
    /**
     * 初始化第一页按钮
     * 
     */
    
    public void initFirstPageButtonsLayout(){
    	mFirstPageButtonsRl.removeAllViews();
    	mFirstPageMiddleButtonsLl.removeAllViews();
    	mFirstPageButtonsRl.addView(mFirstPageMiddleButtonsLl);
    	mFirstPageButtonsList.clear();
    }
    
    
    /**
     * 初始化第二页按钮
     * 
     */
    public void initSecondPageButtonsLayout(){
    	mSecondPageButtonsGl.removeAllViews();
    	mSecondPageButtonsList.clear();
    }
    
    public void refreshReturnFirstPage() {
    	if(mHandler.hasMessages(MSG_BACK_TO_FIRST_PAGE)){
    		mHandler.removeMessages(MSG_BACK_TO_FIRST_PAGE);
    		mHandler.sendEmptyMessageDelayed(MSG_BACK_TO_FIRST_PAGE, TIME_SHOW_SECOND_PAGE);
    	}
	}
    
    public void disableButton(int id){
    	for (View view : mFirstPageButtonsList) {
			if(view.getId() == id){
				view.setEnabled(false);
				return;
			}
		}
    	for(View view:mSecondPageButtonsList){
    		if(view.getId() == id){
    			view.setEnabled(false);
    			return;
    		}
    	}
    }
    
    public void enableButton(int id){
    	for (View view : mFirstPageButtonsList) {
			if(view.getId() == id){
				view.setEnabled(true);
				return;
			}
		}
    	for(View view:mSecondPageButtonsList){
    		if(view.getId() == id){
    			view.setEnabled(true);
    			return;
    		}
    	}
    }
    
}