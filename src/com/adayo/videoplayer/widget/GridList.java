package com.adayo.videoplayer.widget;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adayo.an6v.ui.Utils;
import com.adayo.mediaScanner.CommonUtil;
import com.adayo.midware.constant.channelManagerDef.MEDIA_SOURCE_ID;
import com.adayo.videoplayer.AdayoVideoPlayerApplication;
import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;

@SuppressLint("NewApi")
public class GridList extends LinearLayout {

    public int mRootViewWidth;

    public static final String TAG = GridList.class.getSimpleName();
    public static final int DEFAULT_FOLDER_DRAWABLE_ID = R.drawable.ic_list_item_type_folder;
    public static final int DEFAULT_IMAGE_DRAWABLE_ID = R.drawable.ic_list_item_type_photo;
    public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory() / 2;
    public static final int HSV_MODE = 1;
    public static final int VSV_MODE = 2;
    public static final int FROM_SD = 1;
    public static final int FROM_USB = 2;
    public static final int FROM_NOTHIN = 0;

    protected static final int MSG_GET_THUMB = 1;
    protected static final int MSG_SET_THUMB = 2;

    private static final int MSG_GET_ONE_THUMBNAIL = 3;
    private static final int BOUND = 4;

    public static Drawable DEFAULT_IMAGE_THUMB_DRAWABLE = null;
    public static Drawable DEFAULT_FOLDER_THUMB_DRAWABLE = null;

    View mRootView;
    RecyclerView mGvList;
    ListView mLvList;
    TextView mTvBackPlaymask;
    TextView mTvChangeListMode;
    ListAdapter mListAdapter = new ListAdapter();
    RelativeLayout mRlRightArea;
    RelativeLayout mRlChangeListMode;

    public int mMaxTitleWidth;

    enum ListType {
        LIST, // 列表模式
        THUMBNAILS, // 缩略图模式
    }

    ListType mListType = ListType.LIST;

    private int mCurrentPlayingIndex;
    private OnItemClickListener mOnListItemClickListener;
    private MEDIA_SOURCE_ID mSource = MEDIA_SOURCE_ID.VIDEO;
    // private OnFileItemClick mOnFlieItemClick;
    private ArrayList<ListItem> mAllFilesPaths = new ArrayList<GridList.ListItem>();
    private IGetThumbnail mGetThumbnailInterface = null;
    private LruCache<Integer, Drawable> mCaches = new LruCache<Integer, Drawable>((int) (MAX_MEMORY / 4)) {
        @Override
        protected int sizeOf(Integer key, Drawable value) {
            // TODO Auto-generated method stub
            return measureOneItemSize(value);
        }

    };
    private ThumbGalleryAdapter mThumbAdapter;
    private HandlerThread mAsyncHandlerThread = new HandlerThread("HandlerThread");
    Handler mAsyncHandler = null;

    private GetThumbnailsThread mGetThumbnailsThread = new GetThumbnailsThread();

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            ListItem item = (ListItem) msg.obj;
            Drawable drawable = item.mDrawable;
            item.mDrawable = null;
            int pos = item.mPosition;
            Log.d(TAG, "[MSG_SET_THUMB]: " + pos);
            ThumbGalleryAdapter.ViewHolder holder = (ThumbGalleryAdapter.ViewHolder) mGvList
                    .findViewHolderForPosition(pos);
            try {
                switch (msg.what) {
                    case MSG_SET_THUMB:

                        // ThumbGalleryAdapter.ViewHolder holder =
                        // (GridList.ThumbGalleryAdapter.ViewHolder) item.obj;
                        if (holder == null) {
                            Log.e(TAG, "[handleMessage(mHandler)]:position " + pos + " can't find view holder");
                            break;
                        }
                        Trace.d(TAG, "[MSG_SET_THUMB]:" + holder + " drawable " + drawable);
                        // if(item.isFile){
                        // // drawable.setBounds(0, 0,
                        // drawable.getIntrinsicWidth(),
                        // drawable.getIntrinsicHeight());
                        // // holder.tvThumb.setCompoundDrawables(null,
                        // drawable, null, null);
                        // holder.setThumb(drawable);
                        // }else{
                        // holder.tvThumb.setBackground(drawable);
                        holder.setThumb(drawable);
                        // }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                drawable = decodeThumbnailsDrawable(item.mFullPath);
                holder.setThumb(drawable);

            }
        }
    };

    private int measureOneItemSize(Drawable value) {

        if (value instanceof BitmapDrawable) {

            Bitmap bitmap = ((BitmapDrawable) value).getBitmap();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                return bitmap.getByteCount();// 当前系统版本大于4.0以上
            }
            return bitmap.getRowBytes() * bitmap.getHeight();
        } else {
            return value.getBounds().width() * value.getBounds().height() * BOUND;// default
            // type
            // is
            // ARGB_8888
        }
    }

    public GridList(Context context) {
        this(context, null);
    }

    public GridList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            return;
        }
        init();
    }

    private void init() {
        Log.d(TAG, "[init]");

        mAsyncHandlerThread.start();

        mGetThumbnailsThread.start();

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Service.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);

        mRootViewWidth = outMetrics.widthPixels;

        mAsyncHandler = new Handler(mAsyncHandlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                GridList.this.handleAsyncMessage(msg);
            }

        };

        DEFAULT_IMAGE_THUMB_DRAWABLE = getResources().getDrawable(DEFAULT_IMAGE_DRAWABLE_ID);
        DEFAULT_FOLDER_THUMB_DRAWABLE = getResources().getDrawable(DEFAULT_FOLDER_DRAWABLE_ID);

        mRootView = LayoutInflater.from(getContext()).inflate(R.layout.grid_list, this);
        mGvList = (RecyclerView) mRootView.findViewById(R.id.rv_grid_list_id);
        mTvBackPlaymask = (TextView) mRootView.findViewById(R.id.tv_back_to_palymask_id);
        mTvChangeListMode = (TextView) mRootView.findViewById(R.id.tv_change_list_mode_id);
        mRlChangeListMode = (RelativeLayout) mRootView.findViewById(R.id.rl_change_list_mode_id);
        mRlRightArea = (RelativeLayout) mRootView.findViewById(R.id.rl_back_area);
        mLvList = (ListView) mRootView.findViewById(R.id.lv_grid_list_id);
        mLvList.setAdapter(mListAdapter);
        mThumbAdapter = new ThumbGalleryAdapter(getContext());
        setThumbList(2, HSV_MODE);
        mGvList.setAdapter(mThumbAdapter);
        mGvList.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

        });

        mMaxTitleWidth = mRlRightArea.getBackground().getIntrinsicWidth();
        mTvBackPlaymask.setWidth(mMaxTitleWidth);

        mRlChangeListMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AdayoVideoPlayerApplication.instance().getVideoPlayControler().isPreparing()) {
                    return;
                }
                if (ListType.LIST.equals(mListType)) {
                    setVideoGridMode();
                    mListType = ListType.THUMBNAILS;
                } else {
                    setVideoListMode();
                    mListType = ListType.LIST;
                }
            }
        });

    }

    public void setVideoGridMode() {

        mGvList.setVisibility(VISIBLE);
        mLvList.setVisibility(GONE);

        mRlRightArea.setVisibility(VISIBLE);

        mTvChangeListMode.setBackgroundResource(R.drawable.selector_list_mode_list_btn);
        mTvChangeListMode.setText(getResources().getString(R.string.goto_list_text));

    }

    public void setVideoListMode() {

        mLvList.setVisibility(VISIBLE);
        mGvList.setVisibility(GONE);

        mRlRightArea.setVisibility(VISIBLE);

        mTvChangeListMode.setBackgroundResource(R.drawable.selector_list_mode_grid_btn);
        mTvChangeListMode.setText(getResources().getString(R.string.thumbnail_text));

    }

    public void setup(MEDIA_SOURCE_ID source) {
        mSource = source;
        switch (source) {
            case VIDEO:
                setupForVideoSource();
                break;
            case PHOTO:
                setupForPhotoSource();
                break;
            default:
                break;
        }
    }

    /**
     * 该接口不适用于做文件浏览
     * 
     * @param source
     *            定义当源
     * @param onItemClickListener
     *            列表的监听接口
     * @param onBackToPlayMaskClickListener
     *            返回播放界面的监听接口
     */
    public void setup(MEDIA_SOURCE_ID source, OnItemClickListener onItemClickListener,
            OnClickListener onBackToPlayMaskClickListener, IGetThumbnail getThumbnail) {
        setup(source);
        mThumbAdapter.setOnItemClickListener(onItemClickListener);
        mLvList.setOnItemClickListener(onItemClickListener);

        if (onBackToPlayMaskClickListener != null) {
            setOnBackToPlaymaskButtonClickListener(onBackToPlayMaskClickListener);
        }
        mGetThumbnailInterface = getThumbnail;
    }

    private void setupForVideoSource() {
        mListType = ListType.LIST;
        setVideoListMode();
        setGalleryWidthInVideo();
    }

    private void setupForPhotoSource() {
        mGvList.setVisibility(VISIBLE);
        mLvList.setVisibility(GONE);
        setGalleryWidthInPhoto();
        mListType = ListType.THUMBNAILS;
        mRlRightArea.setVisibility(GONE);
        mAllFilesPaths.clear();
    }

    private void setGalleryWidthInPhoto() {
        RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) mGvList.getLayoutParams();
        if (params == null) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            mGvList.setLayoutParams(params);
        } else if (params.width != RelativeLayout.LayoutParams.MATCH_PARENT) {
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            mGvList.setLayoutParams(params);
        }
    }

    public void setGalleryWidthInVideo() {
        RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) mGvList.getLayoutParams();
        mRlRightArea.measure(0, 0);
        int measuredWidth = mRlRightArea.getMeasuredWidth();
        if (params == null) {
            params = new RelativeLayout.LayoutParams(mRootViewWidth - measuredWidth,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
        } else {
            params.width = mRootViewWidth - measuredWidth;
        }
        mGvList.setLayoutParams(params);
    }

    /**
     * 
     * @param titles
     * @param isFolder
     * @param fileCounts
     * @param mode
     *            HSV_MODE为横屏模式，VSV_MODE为竖屏模式
     */

    public void updateList(String[] titles, int[] fileIds, String[] fullPath, boolean isFolder, int[] storageIn,
            int[] fileCounts, int mode) {

        Log.d(TAG, "[updateList]");

        mCurrentPlayingIndex = -1;
        if (MEDIA_SOURCE_ID.VIDEO.equals(mSource)) {
            mListAdapter.setList(titles, fileIds, fullPath);
        }

        mGetThumbnailsThread.clear();

        mThumbAdapter.updateList(titles, fileIds, fullPath, isFolder, storageIn, fileCounts);
    }

    public void setThumbList(int rowNum, int mode) {
        GridLayoutManager layoutManager = (GridLayoutManager) mGvList.getLayoutManager();

        if (layoutManager == null) {
            layoutManager = new GridLayoutManager(getContext(), rowNum, GridLayoutManager.HORIZONTAL, false);
        }
        if (mode == HSV_MODE) {
            layoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
            // layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        } else {
            layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        }
        mGvList.setLayoutManager(layoutManager);

        // LinearLayoutManager manager = new LinearLayoutManager(getContext());
        // manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        // mGvList.setLayoutManager(manager);

    }

    public LinearLayout.LayoutParams getGvListLayout() {
        LinearLayout.LayoutParams params = (LayoutParams) mGvList.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        return params;
    }

    /**
     * 设置列表的点击事件
     * 
     * @param onItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnListItemClickListener = onItemClickListener;
        // mGvList.setOnItemClickListener(onItemClickListener);
        mThumbAdapter.setOnItemClickListener(onItemClickListener);
    }

    /**
     * 设置正在播放的条目
     * 
     * @param index
     */
    public void setCurrentPlayingIndex(int index) {
        mCurrentPlayingIndex = index;
        if (ListType.LIST.equals(mListType)) {
            mListAdapter.notifyDataSetChanged();
            mLvList.setSelectionFromTop(index, 0);
            if (mCurrentPlayingIndex < mListAdapter.getCount()) {
                mTvBackPlaymask.setText(((ListItem) mListAdapter.getItem(mCurrentPlayingIndex)).mTitle);
            }
        } else if (ListType.THUMBNAILS.equals(mListType)) {
            if (mCurrentPlayingIndex < mThumbAdapter.getItemCount()) {
                mTvBackPlaymask.setText(((ListItem) mListAdapter.getItem(mCurrentPlayingIndex)).mTitle);
            }
        }
    }

    /**
     * 设置返回播放界面按钮的Click事件
     * 
     * @param onClickListener
     */
    public void setOnBackToPlaymaskButtonClickListener(final OnClickListener onClickListener) {

        mTvBackPlaymask.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "back to playmask");
                try {
                    mGvList.stopScroll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onClickListener.onClick(v);
            }
        });
    }

    public void setCurrentPlayingTitle(String title) {
        mTvBackPlaymask.setText(title);
        mTvBackPlaymask.setWidth(mMaxTitleWidth);
    }

    OnItemClickListener mFileBrowserItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mOnListItemClickListener != null) {
                mOnListItemClickListener.onItemClick(parent, view, position, id);
            }
        }
    };

    public interface IGetThumbnail {
        public Drawable getThumbnail(int position);

        public Drawable getThumbnail(String fullPath);
    }

    // public Drawable getThumbnailDrawable(int fileId,String fullPath,boolean
    // isFile){
    // Drawable drawable;
    //
    // drawable = mCaches.get(fileId);
    //
    // if (drawable == null) {
    // drawable = decodeThumbnailsDrawable(fullPath);
    //
    // if (drawable == null) {
    // if(isFile)
    // drawable = DEFAULT_IMAGE_THUMB_DRAWABLE;
    // else
    // drawable =DEFAULT_FOLDER_THUMB_DRAWABLE;
    // }
    // else
    // setToCahche(fileId, drawable);
    // }
    //
    // return drawable;
    // }
    public void setToCahche(int fileId, Drawable drawable) {
        if (drawable == null || drawable == DEFAULT_FOLDER_THUMB_DRAWABLE || drawable == DEFAULT_IMAGE_THUMB_DRAWABLE) {
            return;
        }
        if (fileId == -1) {
            return;
        }
        mCaches.put(fileId, drawable);
    }

    // synchronized private Drawable decodeThumbnailsDrawable(int fileId){
    // return mGetThumbnailInterface.getThumbnail(fileId);
    // }
    private Drawable decodeThumbnailsDrawable(String fullPath) {
        Drawable drawable = mGetThumbnailInterface.getThumbnail(fullPath);

        if (drawable == null) {
            Bitmap bmp = getImageThumbnail(fullPath, CommonUtil.THMUBNAIL_PIC_WIDTH, CommonUtil.THMUBNAIL_PIC_HEIGHT);

            if (bmp != null) {
                drawable = new BitmapDrawable(getResources(), bmp);
            }
        }

        return drawable;
    }

    public Drawable getThumbnailFromCache(int fileId) {
        Log.d(TAG, "[getDrawable]:position " + fileId);
        return mCaches.get(fileId);
    }

    public void clearAllCahches() {
        mCaches.evictAll();
    }

    class ListItem implements Comparable<ListItem> {

        // for video listview
        public ListItem(String title, int id, String fullPath, int pos) {
            this.mTitle = title;
            mIsFile = true;
            mHasFileCount = 0;
            mThumbId = DEFAULT_IMAGE_DRAWABLE_ID;
            this.mFileId = id;
            mPosition = pos;
            mInStroage = FROM_NOTHIN;
        }

        public ListItem(String title, int id, String fullPath, boolean isFolder, int count, int pos, int from) {
            this.mTitle = title;
            mIsFile = !isFolder;
            mHasFileCount = count;
            mThumbId = DEFAULT_IMAGE_DRAWABLE_ID;
            this.mFullPath = fullPath;
            mPosition = pos;
            mInStroage = from;
            mFileId = id;
        }

        public int mPriority = 0;
        public String mTitle;
        public Drawable mDrawable;
        public int mThumbId;
        public boolean mIsFile = true;
        public String mTitlePy;
        public int mHasFileCount;
        public int mFileId;
        public int mPosition;
        public Object mObj;
        public int mInStroage;
        public String mFullPath;

        @Override
        public int compareTo(ListItem another) {
            return 0;
        }
    }

    class ListAdapter extends BaseAdapter {

        ArrayList<ListItem> mListItems = new ArrayList<GridList.ListItem>();
        private int mNumWidth = 0;

        public ListAdapter() {
        }

        public void setList(String[] titles, int[] fileIds, String[] fullPath) {
            mListItems.clear();
            mNumWidth = 0;
            if (!Utils.isArrayEmpty(titles)) {
                for (int i = 0; i < titles.length; i++) {

                    int id = -1;

                    if (fileIds != null && fileIds.length > i) {
                        id = fileIds[i];
                    }
                    mListItems.add(new ListItem(titles[i], id, fullPath[i], i));
                }
            }
            try {
                notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int fixNumTextViewWidth(TextView view, String string) {
            TextPaint paint = view.getPaint();
            return (int) Layout.getDesiredWidth(string, paint);
        }

        @Override
        public int getCount() {
            return mListItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mListItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView != null) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = LayoutInflater.from(getContext()).inflate(R.layout.grid_list_item, null);
                holder = new ViewHolder();
                holder.mTvNum = (TextView) view.findViewById(R.id.tv_video_list_num_id);
                holder.mTvTitle = (TextView) view.findViewById(R.id.tv_video_list_title_id);
                view.setTag(holder);
            }

            configureVideoListView(position, view, holder);

            return view;
        }

        class ViewHolder {
            public TextView mTvNum;
            public TextView mTvTitle/* ,tvFileCount */;
        }

        public void configureVideoListView(int position, View view, ViewHolder holder) {

            if (mNumWidth == 0) {
                mNumWidth = fixNumTextViewWidth(holder.mTvTitle,
                        getCount() + getContext().getString(R.string.number_title_divider_text));
                Drawable drawable = getResources().getDrawable(R.drawable.ic_list_item_playing);
                if (mNumWidth < drawable.getIntrinsicWidth()) {
                    mNumWidth = drawable.getIntrinsicWidth();
                }
            }

            if (position == mCurrentPlayingIndex) {
                Drawable drawable = getResources().getDrawable(R.drawable.ic_list_item_playing);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                holder.mTvNum.setCompoundDrawables(drawable, null, null, null);
                holder.mTvNum.setText("");
            } else {
                holder.mTvNum.setCompoundDrawables(null, null, null, null);
                holder.mTvNum.setText((position + 1) + getContext().getString(R.string.number_title_divider_text));
            }
            holder.mTvNum.setWidth(mNumWidth);
            holder.mTvTitle.setText(mListItems.get(position).mTitle);
        }

    }

    public class ThumbGalleryAdapter extends RecyclerView.Adapter<ThumbGalleryAdapter.ViewHolder> {

        private OnItemClickListener mOnItemClickListener;

        private List<ListItem> mItems = new LinkedList<GridList.ListItem>();
        private Context mContext;

        public ThumbGalleryAdapter(Context context) {
            mContext = context;
        }

        public void updateList(String[] titles, int[] fileIds, String[] fullPaths, boolean isFolder, int[] storageIn,
                int[] fileCounts) {
            mItems.clear();

            if (!Utils.isArrayEmpty(titles) && !Utils.isArrayEmpty(fullPaths) && titles.length == fullPaths.length) {

                for (int i = 0; i < titles.length; i++) {

                    int from = FROM_NOTHIN;
                    if (storageIn != null && isFolder && storageIn.length > i) {
                        from = storageIn[i];
                    }

                    int count = 0;
                    if (fileCounts != null && isFolder && fileCounts.length > i) {
                        count = fileCounts[i];
                    }

                    int id = 0;
                    if (fileIds != null && fileIds.length > i) {
                        id = fileIds[i];
                    }

                    mItems.add(new ListItem(titles[i], id, fullPaths[i], isFolder, count, i, from));

                }

            }
            try {
                notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @SuppressLint("NewApi")
        @Override
        public void onBindViewHolder(final ViewHolder view, final int position) {
            Drawable drawable;

            if (position >= mItems.size()) {
                Trace.e(TAG, "[onBindViewHolder]:position " + position + " > mItems.size() " + mItems.size());
                return;
            }

            ListItem item = mItems.get(position);
            String title;
            if (!item.mIsFile) {
                drawable = getThumbnailFromCache(item.mFileId);
                if (drawable == null || drawable == DEFAULT_IMAGE_THUMB_DRAWABLE) {
                    Log.w(TAG, "[onBindViewHolder]:position " + position + " can't get drawable from cache");
                    mGetThumbnailsThread.put(item);
                }

                if (drawable == null) {
                    drawable = DEFAULT_FOLDER_THUMB_DRAWABLE;
                }
                String countString = "";
                countString = "(" + item.mHasFileCount + ")";

                Drawable fromDrawable = null;
                if (item.mInStroage == FROM_USB) {
                    fromDrawable = getResources().getDrawable(R.drawable.ic_list_item_from_usb);
                } else if (item.mInStroage == FROM_SD) {
                    fromDrawable = getResources().getDrawable(R.drawable.ic_list_item_from_sd);
                }
                // view.ivThumb.setImageDrawable(drawable);
                view.setThumb(drawable);

                if (fromDrawable != null) {
                    fromDrawable.setBounds(0, 0, fromDrawable.getIntrinsicWidth(), fromDrawable.getIntrinsicHeight());
                }
                view.mTvTitle.setPadding(0, 0, 0, 0);
                view.mTvTitle.setCompoundDrawablePadding(0);
                view.mTvTitle.setCompoundDrawables(fromDrawable, null, null, null);
                int maxWidth;
                if (fromDrawable != null) {
                    maxWidth = drawable.getIntrinsicWidth() - (fixNumTextViewWidth(view.mTvTitle, countString))
                            - fromDrawable.getIntrinsicWidth();
                } else {
                    maxWidth = drawable.getIntrinsicWidth() - (fixNumTextViewWidth(view.mTvTitle, countString));
                }
                title = convertString(mItems.get(position).mTitle, view.mTvTitle, maxWidth) + countString;
                view.mTvTitle.setText(title);

                view.mTvTitle.setVisibility(VISIBLE);
            } else {
                drawable = getThumbnailFromCache(item.mFileId);
                if (drawable == null || drawable == DEFAULT_FOLDER_THUMB_DRAWABLE) {
                    Log.w(TAG, "[onBindViewHolder]:position " + position + " can't get drawable from cache");
                    mGetThumbnailsThread.put(item);
                }

                if (drawable == null) {
                    drawable = DEFAULT_IMAGE_THUMB_DRAWABLE;
                }
                title = item.mTitle;
                view.mTvTitle.setVisibility(GONE);
                view.setThumb(drawable);
            }

            if (mOnItemClickListener != null) {
                view.mThisView.setTag(Integer.valueOf(position));
                view.mThisView.setOnClickListener(mOnClickListener);
            }
        }


        private int fixNumTextViewWidth(TextView view, String string) {
            TextPaint paint = view.getPaint();
            return (int) Layout.getDesiredWidth(string, paint);
        }

        private String convertString(final String text, TextView view, int maxWidth) {
            int width = fixNumTextViewWidth(view, text);

            if (width <= maxWidth) {
                return text;
            }
            String tmpText = "";
            int left = 0;
            int right = text.length();
            while (left < right) {
                int start = (left + right) / 2;
                tmpText = text.substring(0, start);
                tmpText += "...";
                width = fixNumTextViewWidth(view, tmpText);

                if (width < maxWidth) {
                    left = start + 1;
                } else if (width > maxWidth) {
                    right = start - 1;
                } else {
                    break;
                }
            }
            return tmpText;

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.thumbnails_item, viewGroup, false);

            ViewHolder holder = new ViewHolder(view);
            // holder.tvThumb = (TextView) view.findViewById(R.id.tv_thumb_id);
            holder.mTvTitle = (TextView) view.findViewById(R.id.tv_thumb_title_id);
            holder.mThisView = view;
            holder.mIvThumb = (ImageView) view.findViewById(R.id.iv_thumb_id);
            return holder;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View view) {
                super(view);
            }

            public void setThumb(Drawable drawable) {
                // ivThumb.setImageDrawable(drawable);
                mIvThumb.setBackground(drawable);
            }

            TextView /* tvThumb, */mTvTitle;
            View mThisView;
            ImageView mIvThumb;
        }

        public OnClickListener mOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                if (position != null) {
                    Log.d(TAG, "[onClick]:position " + position);
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(null, v, position, getItemId(position));
                    }
                }
            }
        };

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
        }

        public void updateList(List<ListItem> list) {
            mItems = list;
            notifyDataSetChanged();
        }

        public ListItem getItem(int position) {
            if (position > mItems.size()) {
                Log.e(TAG, "[getItem]:position " + position + " bigger than list size !!!");
                return null;
            }
            return mItems.get(position);
        }
    }

    private Object[] mGetThumbnailThreadLock = new Object[0];

    class GetThumbnailsThread extends Thread {
        private boolean mExit = false;
        private Deque<ListItem> mItems;

        public GetThumbnailsThread() {
            mItems = new LinkedBlockingDeque<GridList.ListItem>();
        }

        public void clear() {
            mItems.clear();
        }

        public void put(ListItem item) {
            mItems.addFirst(item);
            try {
                synchronized (mGetThumbnailThreadLock) {
                    mGetThumbnailThreadLock.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void exit() {
            mExit = true;
        }

        @Override
        public void run() {
            while (!mExit) {

                if (mItems.isEmpty()) {
                    synchronized (mGetThumbnailThreadLock) {
                        try {
                            mGetThumbnailThreadLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }

                // Integer fileId = mItems.pop();
                ListItem item = mItems.pop();

                if (item == null){
                    continue;
                }
                Drawable drawable = getThumbnailFromCache(item.mFileId);
                if (drawable == null) {
                    drawable = decodeThumbnailsDrawable(item.mFullPath);// 通过回调，到control里面实现的
                    Log.d(TAG,
                            "[run(GetThumbnailsThread)]:cache maxSize " + mCaches.maxSize() + " size " + mCaches.size());
                    if (drawable == null){
                        drawable = DEFAULT_IMAGE_THUMB_DRAWABLE;
                    }
                    setToCahche(item.mFileId, drawable);
                    item.mDrawable = drawable;
                    mHandler.obtainMessage(MSG_SET_THUMB, item).sendToTarget();

                    // if(mCaches.maxSize() - mCaches.size() >
                    // measureOneItemSize(drawable)){
                    // setToCahche(item.fileId,drawable);
                    // }else{
                    // Log.d(TAG,
                    // "[run(GetThumbnailsThread)]:cache didn't has enough memory,remainder memory:"
                    // + (mCaches.maxSize() - mCaches.size()));
                    // mItems.clear();
                    // }

                }
            }
            mExit = true;
        }

        public boolean isExit() {
            return mExit;
        }

    }

    public void handleAsyncMessage(Message msg) {
        switch (msg.what) {
            case MSG_GET_THUMB:
                break;
            case MSG_GET_ONE_THUMBNAIL:
                break;
            default:
                break;
        }
    }

//    private Bitmap getVideoThumbnail(String videoPath, int width, int height) {
//        Bitmap bitmap = null;
//        // 获取视频的缩略图
//        // bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
//        // bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
//        // ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
//        MediaMetadataRetriever retriever = null;
//        try {
//            retriever = new MediaMetadataRetriever();
//            retriever.setDataSource(videoPath);
//            String timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//            int time = Integer.valueOf(timeString != null ? timeString : "0");
//            Trace.d(TAG, "[getVideoThumbnail]:video path " + videoPath + " time " + time / 2 + " time string "
//                    + timeString);
//            bitmap = retriever.getFrameAtTime(time / 2, MediaMetadataRetriever.OPTION_NEXT_SYNC);
//
//            // if(time > 1*1000*1000){//单位是微秒
//            // bitmap = retriever.getFrameAtTime(1*1000*1000,
//            // MediaMetadataRetriever.OPTION_NEXT_SYNC);
//            // }else{
//            // bitmap = retriever.getFrameAtTime();
//            // }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (retriever != null){
//                retriever.release();
//            }
//            retriever = null;
//        }
//
//        if (bitmap != null) {
//            float scaleWidth = (float) width / bitmap.getWidth();
//            float scaleHeight = (float) height / bitmap.getHeight();
//            Matrix matrix = new Matrix();
//            matrix.setScale(scaleWidth, scaleHeight);
//            Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            bitmap.recycle();
//            return bmp;
//        }
//        return bitmap;
//    }

    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高，注意此处的bitmap为null
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        // 计算缩放比
        int h = options.outHeight;
        int w = options.outWidth;

        int beWidth = w / width;
        int beHeight = h / height;

        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;

        try {
            // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
            bitmap = BitmapFactory.decodeFile(imagePath, options);
            // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (bitmap != null && (bitmap.getWidth() > width || bitmap.getHeight() > height)) {

            int newWidth = Math.min(bitmap.getWidth(), width);
            int newHeight = Math.min(bitmap.getHeight(), height);

            Bitmap newBmp = null;
            Log.e(TAG, "[getImageThumbnail]:imagePath " + imagePath + " has get thumbnail failed,new width " + newWidth
                    + " new height " + newHeight);
            newBmp = Bitmap.createBitmap(bitmap, 0, 0, newWidth, newHeight);
            bitmap.recycle();
            bitmap = newBmp;
        }
        return bitmap;
    }
}
