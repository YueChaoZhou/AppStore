package com.bb.googleplaybb.ui.adapter.holder;

import android.text.format.Formatter;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bb.googleplaybb.R;
import com.bb.googleplaybb.domain.AppInfo;
import com.bb.googleplaybb.domain.DownloadInfo;
import com.bb.googleplaybb.manager.AppDownloadManager;
import com.bb.googleplaybb.manager.DownloadManager;
import com.bb.googleplaybb.net.NetHelper;
import com.bb.googleplaybb.ui.view.ProgressArc;
import com.bb.googleplaybb.utils.BitmapHelper;
import com.bb.googleplaybb.utils.UIUtils;
import com.lidroid.xutils.BitmapUtils;

import java.io.File;

/**
 * Created by Boby on 2018/7/13.
 */

public class HomeHolder extends BaseHolder<AppInfo> implements AppDownloadManager.DownloadObserver, View.OnClickListener {
    private TextView tvName, tvSize, tvDes;
    private ImageView ivIcon;
    private RatingBar rbStart;

    private BitmapUtils utils;
    private FrameLayout flProgress;
    private ProgressArc mProgressArc;

    private int mCurrentState;
    private float mProgress;
    private TextView tvDownload;
    private AppDownloadManager mDm;

    @Override
    public View initView() {
        View view = UIUtils.inflate(R.layout.list_item_home);
        tvName = view.findViewById(R.id.tv_name);
        tvSize = view.findViewById(R.id.tv_size);
        tvDes = view.findViewById(R.id.tv_des);
        ivIcon = view.findViewById(R.id.iv_icon);
        rbStart = view.findViewById(R.id.rb_start);
        tvDownload = view.findViewById(R.id.tv_download);
        flProgress = view.findViewById(R.id.fl_progress);

        utils = BitmapHelper.getBitmapUtils();

        flProgress.setOnClickListener(this);

        mProgressArc = new ProgressArc(UIUtils.getContext());
        //设置进度条直径
        mProgressArc.setArcDiameter(UIUtils.dip2px(31));
        //设置进度条颜色
        mProgressArc.setProgressColor(R.color.progress);
        mProgressArc.setBackgroundResource(R.drawable.ic_download);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        //将进度条添加到布局
        flProgress.addView(mProgressArc, params);

        mDm = AppDownloadManager.getDownloadManager();
        mDm.registeredObserver(this);

        return view;
    }

    @Override
    public void refreshView(AppInfo data) {
        tvName.setText(data.name);
        tvSize.setText(Formatter.formatFileSize(UIUtils.getContext(), data.size));
        tvDes.setText(data.des);
        rbStart.setRating(data.stars);

        utils.display(ivIcon, NetHelper.URL + data.iconUrl);

        DownloadInfo downloadInfo = DownloadInfo.copy(data);
        File file = new File(downloadInfo.getFilePath());

        downloadInfo = mDm.getDownloadInfo(data);
        if (downloadInfo == null) {
            if (!file.exists()) {
                mCurrentState = DownloadManager.STATE_UNDO;
                mProgress = 0;
            } else if (file.length() == data.size) {
                mCurrentState = DownloadManager.STATE_SUCCESS;
                mProgress = 1;
            } else {
                mCurrentState = DownloadManager.STATE_PAUSE;
                mProgress = file.length() / (float) data.size;
            }
        } else {
            mCurrentState = downloadInfo.mCurrentState;
            mProgress = downloadInfo.getProgress();
        }

        refreshUI(mCurrentState, mProgress, data.id);//第一次手动调用
    }

    private void refreshUI(int state, float progress, String id) {
        //由于listview的重用机制，刷新之前要确保是同一个应用
//        if (!getData().id.equals(id)) {
//            return;
//        }
        System.out.println("refreshUI：   state:" + state + ";progress:" + progress);
        mCurrentState = state;
        mProgress = progress;
        switch (state) {
            case DownloadManager.STATE_UNDO:
                mProgressArc.setBackgroundResource(R.drawable.ic_download);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_NO_PROGRESS);
                tvDownload.setText("下载");
                break;
            case DownloadManager.STATE_PAUSE:
                mProgressArc.setBackgroundResource(R.drawable.ic_resume);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_NO_PROGRESS);
                mProgressArc.setProgress(progress, false);
                tvDownload.setText("暂停");
                System.out.println("暂停..........");
                break;
            case DownloadManager.STATE_WAITING:
                mProgressArc.setBackgroundResource(R.drawable.ic_download);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_WAITING);
                tvDownload.setText("等待中");
                break;
            case DownloadManager.STATE_DOWNLOADING:
                mProgressArc.setBackgroundResource(R.drawable.ic_pause);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_DOWNLOADING);
                mProgressArc.setProgress(progress, false);
                tvDownload.setText((int) (progress * 100) + "%");
                break;
            case DownloadManager.STATE_SUCCESS:
                mProgressArc.setBackgroundResource(R.drawable.ic_install);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_NO_PROGRESS);
                tvDownload.setText("安装");
                break;
            case DownloadManager.STATE_ERROR:
                mProgressArc.setBackgroundResource(R.drawable.ic_redownload);
                mProgressArc.setStyle(ProgressArc.PROGRESS_STYLE_NO_PROGRESS);
                tvDownload.setText("下载失败");
                break;
        }
    }

    public void refreshUIonUIThread(final DownloadInfo downloadInfo) {
        if (getData().id.equals(downloadInfo.id)) {
            UIUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int state = downloadInfo.mCurrentState;
                    float progress = downloadInfo.getProgress();
                    refreshUI(state, progress, downloadInfo.id);
                }
            });
        }
    }

    //在主线程和子线程中都有调用
    @Override
    public void notifyDownloadStateChange(DownloadInfo downloadInfo) {
        refreshUIonUIThread(downloadInfo);
    }

    //在子线程中调用
    @Override
    public void notifyDownloadProgressChange(DownloadInfo downloadInfo) {
        if (downloadInfo.getProgress() != mProgress) {
            refreshUIonUIThread(downloadInfo);
        }
    }

    @Override
    public void onClick(View v) {
//        downloadInfo = mDm.getDownloadInfo(getData());
//        if (downloadInfo == null || downloadInfo.mCurrentState == DownloadManager.STATE_PAUSE || downloadInfo.mCurrentState == DownloadManager.STATE_ERROR) {
//            mDm.download(getData());
//        } else if (downloadInfo.mCurrentState == DownloadManager.STATE_DOWNLOADING || downloadInfo.mCurrentState == DownloadManager.STATE_WAITING) {
//            mDm.pause(getData());
//        } else {
//            mDm.install(getData());
//        }
        AppInfo data = getData();
        switch (v.getId()) {
//            case R.id.btn_download:
            case R.id.fl_progress:
                if (mCurrentState == DownloadManager.STATE_UNDO || mCurrentState == DownloadManager.STATE_PAUSE || mCurrentState == DownloadManager.STATE_ERROR) {
                    mDm.download(data);
                } else if (mCurrentState == DownloadManager.STATE_DOWNLOADING || mCurrentState == DownloadManager.STATE_WAITING) {
                    mDm.pause(data);
                } else if (mCurrentState == DownloadManager.STATE_SUCCESS) {
                    mDm.install(data);
                }
                break;
        }
    }
}