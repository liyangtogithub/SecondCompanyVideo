package com.adayo.videoplayer.fragments;

import com.adayo.videoplayer.R;
import com.adayo.videoplayer.Trace;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class VideoShowInfoFragment extends Fragment {

    private static final String TAG = VideoShowInfoFragment.class.getSimpleName();
    private static String KEY_INFO = "info";
    private static String KEY_SHOW_LOADING = "loading";

    public static VideoShowInfoFragment newInstance(String info, boolean showLoading) {
        VideoShowInfoFragment fragment = new VideoShowInfoFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INFO, info);
        bundle.putBoolean(KEY_SHOW_LOADING, showLoading);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Trace.d(TAG, "[onCreateView]");
        return inflater.inflate(R.layout.show_info_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Trace.d(TAG, "[onViewCreated]");
        ((TextView) getView().findViewById(R.id.tv_info_id)).setText(getArguments().getString(KEY_INFO));
        if (getArguments().getBoolean(KEY_SHOW_LOADING) == false) {
            ((ProgressBar) getView().findViewById(R.id.progress_view_id)).setVisibility(View.GONE);
            Drawable drawable = getActivity().getResources().getDrawable(R.drawable.nothing);
            TextView textView = (TextView) getView().findViewById(R.id.tv_info_id);
            textView.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            textView.setGravity(Gravity.CENTER);
        } else {
            ((ProgressBar) getView().findViewById(R.id.progress_view_id)).setVisibility(View.VISIBLE);
        }
    }
}
