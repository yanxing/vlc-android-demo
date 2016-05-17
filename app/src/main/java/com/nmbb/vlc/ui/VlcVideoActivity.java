package com.nmbb.vlc.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nmbb.vlc.R;
import com.nmbb.vlc.util.CountDownTimer;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.vlc.util.VLCInstance;

public class VlcVideoActivity extends Activity implements SurfaceHolder.Callback, IVideoPlayer,View.OnClickListener {

	private final static String TAG = "[VlcVideoActivity]";

	private SurfaceView mSurfaceView;
	private LibVLC mMediaPlayer;
	private SurfaceHolder mSurfaceHolder;
    
    private View mLoadingView;
	//暂停、播放控制
	private ImageView mControl;
	//播放进度条
	private SeekBar mSeekBar;
	private LinearLayout mControlLayout;
	//播放进度时间
	private TextView mCurrentTime;
	private TextView mTotalTime;
	private TextView mTip;

	/**
	 * 播放控制是否正可见,true可见
	 */
	private Boolean mIsVisible=false;

	/**
	 * 是否已经开始拖动
	 */
	private Boolean mDrag=false;
	
	

	private int mVideoHeight;
	private int mVideoWidth;
	private int mVideoVisibleHeight;
	private int mVideoVisibleWidth;
	private int mSarNum;
	private int mSarDen;

	private static final int HANDLER_BUFFER_START = 1;
	private static final int HANDLER_BUFFER_END = 2;
	private static final int HANDLER_SURFACE_SIZE = 3;

	private static final int SURFACE_BEST_FIT = 0;
	private static final int SURFACE_FIT_HORIZONTAL = 1;
	private static final int SURFACE_FIT_VERTICAL = 2;
	private static final int SURFACE_FILL = 3;
	private static final int SURFACE_16_9 = 4;
	private static final int SURFACE_4_3 = 5;
	private static final int SURFACE_ORIGINAL = 6;
	private int mCurrentSize = SURFACE_BEST_FIT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_vlc);

		mSurfaceView = (SurfaceView) findViewById(R.id.video);
        mLoadingView = findViewById(R.id.video_loading);
		mControl= (ImageView) findViewById(R.id.control);
		mSeekBar= (SeekBar) findViewById(R.id.seekBar);
		mControlLayout= (LinearLayout) findViewById(R.id.control_layout);
		mCurrentTime= (TextView) findViewById(R.id.current_time);
		mTotalTime= (TextView) findViewById(R.id.totalTime);
		mTip= (TextView) findViewById(R.id.tip);
		mSurfaceView.setOnClickListener(this);
		mControl.setOnClickListener(this);
		mSeekBar.setEnabled(true);
		mSeekBar.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener());
		try {
			mMediaPlayer = VLCInstance.getLibVlcInstance(getApplicationContext());
		} catch (LibVlcException e) {
			e.printStackTrace();
		}

		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
		mSurfaceHolder.addCallback(this);

		mMediaPlayer.eventVideoPlayerActivityCreated(true);

		EventHandler em = EventHandler.getInstance();
		em.addHandler(mVlcHandler);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mSurfaceView.setKeepScreenOn(true);
		//		mMediaPlayer.setMediaList();
		//		mMediaPlayer.getMediaList().add(new Media(mMediaPlayer, "http://live.3gv.ifeng.com/zixun.m3u8"), false);
		//		mMediaPlayer.playIndex(0);
		mMediaPlayer.playMRL("http://192.168.2.249:81/files/downloads/zhengquedeshuayafangfa.flv");
		mMediaPlayer.setNetworkCaching(6000);
//		mMediaPlayer.playMRL("http://live.3gv.ifeng.com/zixun.m3u8");
	}

	/**
	 * 拖动进度
	 */
	private class MyOnSeekBarChangeListener implements
			SeekBar.OnSeekBarChangeListener {

		// 触发操作，拖动
		public void onProgressChanged(SeekBar seekBar, int progress,
									  boolean fromUser) {
			if (mDrag){
				mMediaPlayer.setTime(progress*1000);
			}
		}

		// 表示进度条刚开始拖动，开始拖动时候触发的操作
		public void onStartTrackingTouch(SeekBar seekBar) {
			mDrag=true;

		}

		// 停止拖动时候
		public void onStopTrackingTouch(SeekBar seekBar) {
			mDrag=false;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.video:
				if (mIsVisible){
					mControlLayout.setVisibility(View.GONE);
					mIsVisible=false;
				}else {
					mIsVisible=true;
					mControlLayout.setVisibility(View.VISIBLE);
					calculateCurrentTime();
					calculateAllTime();
				}
				break;
			//暂停
			case R.id.control:
				if(mMediaPlayer.isPlaying()){
					mMediaPlayer.pause();
					mControl.setImageResource(R.drawable.bofang);
				}else {
					mMediaPlayer.play();
					mControl.setImageResource(R.drawable.zanting);
				}
				break;
		}
	}

	/**
	 * 计算视频总时间
	 */
	private void calculateAllTime(){
		int allTime= (int) (mMediaPlayer.getLength()/1000);
		if (allTime<60){
			mTotalTime.setText(""+allTime);
		}else if (allTime<3600){
			int m=allTime/60;
			if (allTime%60<10){
				mTotalTime.setText(m+":0"+allTime%60);
			}else {
				mTotalTime.setText(m+":"+allTime%60);
			}
		}else {
			int h=allTime/3600;
			int m=allTime%3600;
			if (m/60>=1){
				if (m/60>=10){
					mTotalTime.setText(h+":"+m/60+":"+m%60);
				}else {
					//如果分钟不足两位数补0
					mTotalTime.setText(h+":0"+m/60+":"+m%60);
				}
			}else {
				if (m>=10){
					mTotalTime.setText(h+":00"+":"+m);
				}else {
					//不足补0
					mTotalTime.setText(h+":00"+":0"+m);
				}
			}
		}
	}

	/**
	 * 计算当前视频已经播放了多长时间
	 */
	private void calculateCurrentTime(){
		final int allTime= (int) (mMediaPlayer.getLength()/1000);
		mSeekBar.setMax(allTime);
		Log.d(TAG,allTime+"");
		new CountDownTimer(mMediaPlayer.getLength(),1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if (isFinishing()){
					this.cancel();
				}
				//已暂停
				if (!mMediaPlayer.isPlaying()){
					return;
				}
				int currentTime= (int) (mMediaPlayer.getTime()/1000);
				Log.d(TAG,currentTime+"");
				Log.d(TAG,String.valueOf(mMediaPlayer.getPosition()));
				//当前播放大于1个小时了
				if (currentTime/3600>=1){
					int hour=currentTime/3600;
					int minute=currentTime%3600;
					if (minute/60>=1){
						if (minute/60>=10){
							if (minute%60>=10){
								mCurrentTime.setText(hour+":"+minute/60+":"+minute%60);
							}else {
								mCurrentTime.setText(hour+":"+minute/60+":0"+minute%60);
							}
						}else {
							//如果分钟不足两位数补0
							if (minute%60>=10){
								mCurrentTime.setText(hour+":0"+minute/60+":"+minute%60);
							}else {
								mCurrentTime.setText(hour+":0"+minute/60+":0"+minute%60);
							}
						}
					}else {
						if (minute>=10){
							mCurrentTime.setText(hour+":00"+":"+minute);
						}else {
							//不足补0
							mCurrentTime.setText(hour+":00"+":0"+minute);
						}
					}
				}else if (currentTime/60>=1){
					int minute=currentTime/60;
					if (minute>=10){
						//如果总时间大于1个小时，小时位补0
						if (allTime/3600>=1){
							if (currentTime%60>=10){
								mCurrentTime.setText("00:"+minute+":"+currentTime%60);
							}else {
								mCurrentTime.setText("00:"+minute+":0"+currentTime%60);
							}
						}else {
							if (currentTime%60>=10){
								mCurrentTime.setText(minute+":"+currentTime%60);
							}else {
								mCurrentTime.setText(minute+":0"+currentTime%60);
							}
						}
					}else {
						//如果总时间大于1个小时，小时位补0,分钟补0
						if (allTime/3600>=1){
							if (currentTime%60>=10){
								mCurrentTime.setText("00:0"+minute+":"+currentTime%60);
							}else {
								mCurrentTime.setText("00:0"+minute+":0"+currentTime%60);
							}
						}else {
							if (currentTime%60>=10){
								mCurrentTime.setText("0"+minute+":"+currentTime%60);
							}else {
								mCurrentTime.setText("0"+minute+":0"+currentTime%60);
							}
						}
					}
				}else {
					if (currentTime%60<10){
						if (allTime/3600>=1){
							mCurrentTime.setText("00:00:0"+currentTime);
						}else if (allTime/60>=1){
							mCurrentTime.setText("00:0"+currentTime);
						}else {
							mCurrentTime.setText("0"+currentTime);
						}
					}else {
						if (allTime/3600>=1){
							mCurrentTime.setText("00:00:"+currentTime);
						}else if (allTime/60>=1){
							mCurrentTime.setText("00:"+currentTime);
						}else {
							mCurrentTime.setText(""+currentTime);
						}
					}
				}
				mSeekBar.setProgress(currentTime);
			}

			@Override
			public void onFinish() {
				mControl.setVisibility(View.GONE);
				mTip.setVisibility(View.VISIBLE);
			}
		}.start();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mSurfaceView.setKeepScreenOn(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			mMediaPlayer.eventVideoPlayerActivityCreated(false);

			EventHandler em = EventHandler.getInstance();
			em.removeHandler(mVlcHandler);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mMediaPlayer != null) {
			mSurfaceHolder = holder;
			mMediaPlayer.attachSurface(holder.getSurface(), this);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mSurfaceHolder = holder;
		if (mMediaPlayer != null) {
			mMediaPlayer.attachSurface(holder.getSurface(), this);//, width, height
		}
		if (width > 0) {
			mVideoHeight = height;
			mVideoWidth = width;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mMediaPlayer != null) {
			mMediaPlayer.detachSurface();
		}
	}

	@Override
	public void setSurfaceSize(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {
		mVideoHeight = height;
		mVideoWidth = width;
		mVideoVisibleHeight = visible_height;
		mVideoVisibleWidth = visible_width;
		mSarNum = sar_num;
		mSarDen = sar_den;
		mHandler.removeMessages(HANDLER_SURFACE_SIZE);
		mHandler.sendEmptyMessage(HANDLER_SURFACE_SIZE);
	}


	private Handler mVlcHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg == null || msg.getData() == null)
				return;

			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerTimeChanged:
				break;
			case EventHandler.MediaPlayerPositionChanged:
				break;
			case EventHandler.MediaPlayerPlaying:
				mHandler.removeMessages(HANDLER_BUFFER_END);
				mHandler.sendEmptyMessage(HANDLER_BUFFER_END);
				break;
			case EventHandler.MediaPlayerBuffering:
				break;
			case EventHandler.MediaPlayerLengthChanged:
				break;
			case EventHandler.MediaPlayerEndReached:
				//播放完成
				break;
			}

		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_BUFFER_START:
                showLoading();
				break;
			case HANDLER_BUFFER_END:
                hideLoading();
				break;
			case HANDLER_SURFACE_SIZE:
				changeSurfaceSize();
				break;
			}
		}
	};

	private void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
	}

	private void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = getWindowManager().getDefaultDisplay().getWidth();
		int dh = getWindowManager().getDefaultDisplay().getHeight();

		// calculate aspect ratio
		double ar = (double) mVideoWidth / (double) mVideoHeight;
		// calculate display aspect ratio
		double dar = (double) dw / (double) dh;

		switch (mCurrentSize) {
		case SURFACE_BEST_FIT:
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_FIT_HORIZONTAL:
			dh = (int) (dw / ar);
			break;
		case SURFACE_FIT_VERTICAL:
			dw = (int) (dh * ar);
			break;
		case SURFACE_FILL:
			break;
		case SURFACE_16_9:
			ar = 16.0 / 9.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_4_3:
			ar = 4.0 / 3.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_ORIGINAL:
			dh = mVideoHeight;
			dw = mVideoWidth;
			break;
		}

		mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
		ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.width = dw;
		lp.height = dh;
		mSurfaceView.setLayoutParams(lp);
		mSurfaceView.invalidate();
	}
}
