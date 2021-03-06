package com.doive.nameless.litter_hydra.widget.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_ERROR;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_IDLE;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_NULL;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_PAUSED;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_PLAYBACK_COMPLETED;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_PLAYING;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_PREPARED;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_PREPARING;
import static com.doive.nameless.litter_hydra.widget.video.VideoViewState.STATE_STOP;

/*
 *  @项目名：  Litter-Hydra2 
 *  @包名：    com.doive.nameless.litter_hydra.widget
 *  @文件名:   LiveVideoView
 *  @创建者:   zhong
 *  @创建时间:  2017/5/7 14:05
 *  @描述：
 *
 */
public class IjkVideoView
        extends FrameLayout
        implements IIjkOperation {
    private static final String TAG = "LiveVideoView";
    private Context       mContext;
    private SurfaceHolder mSurfaceHolder;
    @VideoViewState.State
    private int           mCurrentState, mTargetState;//当前状态,目标状态


    private IjkMediaPlayer      mIjkMediaPlayer;
    private String              mLivePath;//直播路径
    private Map<String, String> mLiveHeaders;//播直播请求头

    private boolean mCanSeekTo = true;
    private boolean mAutoPlayAfterPrepared;//自动播放
    private int     mCurrentBufferPercentage;//当前的缓存进度


    public void setStateListener(VideoViewState.onLiveStateListener stateListener) {
        mStateListener = stateListener;
    }

    private VideoViewState.onLiveStateListener mStateListener; //状态监听

    public IjkVideoView(Context context) {
        this(context, null, 0);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setBackgroundColor(Color.BLACK);
        initSurfaceView();
        notifyListenerCurrentStateChange();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    private void initSurfaceView() {
        SurfaceView surfaceView = new SurfaceView(mContext);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                               ViewGroup.LayoutParams.MATCH_PARENT);
        addView(surfaceView, 0, params);
        setLongClickable(true);
        setClickable(true);
        surfaceView.getHolder()
                   .addCallback(mSFHCallback);
        setFocusable(true);
        requestFocus();
    }

    private void openLive() {
        if (mLivePath == null || mSurfaceHolder == null) {
            return;
        }
        //释放相关资源
        release(false);
        try {
            mIjkMediaPlayer = new IjkMediaPlayer();
            //添加监听
            mIjkMediaPlayer.setOnErrorListener(mErrorListener);
            mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
            mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
            mIjkMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mIjkMediaPlayer.setOnInfoListener(mInfoListener);
            //设置地址
            mIjkMediaPlayer.setDataSource(mContext, Uri.parse(mLivePath), mLiveHeaders);
            //设置播放时候保持常亮
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).getWindow()
                                         .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } else {
                mIjkMediaPlayer.setScreenOnWhilePlaying(true);
            }
            //设置图像显示
            mIjkMediaPlayer.setDisplay(mSurfaceHolder);
            //异步加载
            mIjkMediaPlayer._prepareAsync();
            //设置状态
            mCurrentState = STATE_PREPARING;
            notifyListenerCurrentStateChange();
            mTargetState = STATE_PREPARING;
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();

            mErrorListener.onError(mIjkMediaPlayer, IjkMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    /**
     * 设置播放地址
     * @param path 播放路径
     */
    @Override
    public void setLivePath(String path) {
        setLiveUri(path, null);
    }

    /**
     * 设置播放地址以及请求头
     * @param path 播放路径
     * @param headers 请求头
     */
    @Override
    public void setLiveUri(String path, Map<String, String> headers) {
        this.mLivePath = path;
        this.mLiveHeaders = headers;
        openLive();
    }

    @Override
    public void setLivePathAndAutoPlay(String path) {
        setLivePath(path);
        this.mAutoPlayAfterPrepared = true;
    }

    /**
     * 播放
     */
    @Override
    public void play() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.start();
            //设置状态
            mCurrentState = VideoViewState.STATE_PLAYING;
            notifyListenerCurrentStateChange();
        }
        mTargetState = VideoViewState.STATE_PLAYING;
    }

    /**
     * 是否允许拖动进度
     * @param able 是否运行拖动
     */
    @Override
    public void enableSeekTo(boolean able) {
        this.mCanSeekTo = able;
    }

    private void notifyListenerCurrentStateChange() {
        if (mStateListener != null) {
            switch (mCurrentState) {
                case STATE_NULL:
                    mStateListener.onNull();
                    break;
                case STATE_ERROR:
                    mStateListener.onError();
                    break;
                case STATE_IDLE:
                    mStateListener.onIdle();
                    break;
                case STATE_PREPARING:
                    mStateListener.onPreparing();
                    break;
                case STATE_PREPARED:
                    mStateListener.onPrepared();
                    break;
                case STATE_PLAYING:
                    mStateListener.onPlaying();
                    break;
                case STATE_PAUSED:
                    mStateListener.onPause();
                    break;
                case STATE_PLAYBACK_COMPLETED:
                    mStateListener.onPlayCompleted();
                    break;
                case STATE_STOP:
                    mStateListener.onStop();
                    break;
            }
        }
    }

    /**
     * 获取当前播放的百分比
     * @return 返回当前播放百分比
     */
    public int getCurrentProgress() {
        if (mIjkMediaPlayer != null &&
                mCurrentState != VideoViewState.STATE_ERROR &&
                mIjkMediaPlayer.getDuration() != 0 &&
                mCurrentState != VideoViewState.STATE_IDLE)
        {
            return (int) (100f * mIjkMediaPlayer.getCurrentPosition() / mIjkMediaPlayer.getDuration());
        }
        return -1;
    }

    /**
     * 获取当前播放位置
     * @return 返回当前播放位置
     */
    public long getCurrentPosition() {
        if (mIjkMediaPlayer != null &&
                mCurrentState != VideoViewState.STATE_ERROR &&
                mIjkMediaPlayer.getDuration() != 0 &&
                mCurrentState != VideoViewState.STATE_IDLE)
        {
            return mIjkMediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    /**
     * 获取总时长
     * @return
     */
    public long getTotalDuration() {
        if (mIjkMediaPlayer != null &&
                mCurrentState != VideoViewState.STATE_ERROR &&
                mCurrentState != VideoViewState.STATE_IDLE)
        {
            return mIjkMediaPlayer.getDuration();
        }
        return -1;
    }

    /**
     * 拖动到某个部分
     * @param msec 毫秒
     */
    @Override
    public void seekTo(long msec) {
        seekTo(msec, true);
    }

    /**
     * 拖动到某个位置
     * @param msec
     * @param autoPlay 是否自动播放
     */
    @Override
    public void seekTo(long msec, boolean autoPlay) {
        if (isInPlaybackState() && mCanSeekTo) {
            mIjkMediaPlayer.seekTo(msec);
            if (autoPlay) {
                play();
            }
        }
    }

    @Override
    public void seekTo(int progress) {
        seekTo(getTotalDuration() * progress / 100L, true);
    }

    @Override
    public void seekTo(int progress, boolean autoPlay) {
        seekTo(getTotalDuration() * progress / 100L, autoPlay);
    }

    /**
     * 暂停
     */
    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mIjkMediaPlayer.isPlaying()) {
                mIjkMediaPlayer.pause();
            }
            mCurrentState = VideoViewState.STATE_PAUSED;
            notifyListenerCurrentStateChange();
        }
        mTargetState = VideoViewState.STATE_PAUSED;
    }

    /**
     * 恢复
     */
    @Override
    public void recovery() {
        if (isInPlaybackState()) {
            if (mCurrentState == VideoViewState.STATE_PAUSED) {
                mIjkMediaPlayer.start();
                mCurrentState = VideoViewState.STATE_PLAYING;
            }
        }
        mTargetState = VideoViewState.STATE_PLAYING;
    }

    /**
     * 停止
     */
    @Override
    public void stop() {
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.stop();
            mIjkMediaPlayer.release();
            mIjkMediaPlayer = null;
            mCurrentState = STATE_STOP;
            notifyListenerCurrentStateChange();
            mTargetState = STATE_STOP;
        }
    }

    @Override
    public int getBufferPercentage() {
        if (mIjkMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    /**
     * 释放资源
     */
    public void destroy() {
        stop();
        mIjkMediaPlayer = null;
        mStateListener = null;

    }

    /**
     * 判断是否处于可播放状态
     * @return
     */
    private boolean isInPlaybackState() {
        return (mIjkMediaPlayer != null &&
                mCurrentState != VideoViewState.STATE_ERROR &&
                mCurrentState != VideoViewState.STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    /**
     * 释放相关资源
     * @param clearTargetState 是否释放目标状态
     */
    private void release(boolean clearTargetState) {
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.reset();
            mIjkMediaPlayer.release();
            mIjkMediaPlayer = null;
            mCurrentState = VideoViewState.STATE_IDLE;
            notifyListenerCurrentStateChange();
            if (clearTargetState) {
                mTargetState = VideoViewState.STATE_IDLE;
            }
        }
    }

    /**
     * SurfaceHolder回调
     */
    private SurfaceHolder.Callback                   mSFHCallback             = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surfaceCreated: 创建了");
            mSurfaceHolder = holder;
            if (mCurrentState == STATE_NULL) {
                openLive();
            }
            if (mCurrentState == VideoViewState.STATE_PLAYING) {
                //如果是播放状态,设置图像
                mIjkMediaPlayer.setDisplay(holder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };
    /**
     * 错误监听
     */
    private IjkMediaPlayer.OnErrorListener           mErrorListener           = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int framework_err, int impl_err) {
            mCurrentState = VideoViewState.STATE_ERROR;
            notifyListenerCurrentStateChange();
            mTargetState = VideoViewState.STATE_ERROR;
            if (mOnErrorListener!=null){
                mOnErrorListener.onError(framework_err,impl_err);
            }
            return true;
        }
    };
    /**
     * 加载监听
     */
    private IjkMediaPlayer.OnPreparedListener        mPreparedListener        = new IjkMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            mCurrentState = STATE_PREPARED;
            notifyListenerCurrentStateChange();
            if (mTargetState == VideoViewState.STATE_PLAYING) {
                play();
            }
            if (

                    mAutoPlayAfterPrepared){
                play();
            }
        }
    };
    /**
     * 完成监听
     */
    private IjkMediaPlayer.OnCompletionListener      mCompletionListener      = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            notifyListenerCurrentStateChange();
        }
    };
    /**
     * 缓冲进度监听
     */
    private IjkMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
            mCurrentBufferPercentage = percent;
            if (mOnBufferingListener!=null){
                mOnBufferingListener.onBufferingUpdate(percent);
            }
        }

    };
    /**
     * 信息监听
     */
    private IjkMediaPlayer.OnInfoListener            mInfoListener            = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            if (mOnInfoListener!=null){
                mOnInfoListener.onInfo(i,i1);
            }
            switch (i) {
                case IIjkOperation.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                    //视频信息滞后
                    Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                    break;
                case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    //视频渲染开始
                    Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    //缓冲开始
                    if (mOnBufferingListener!=null){
                        mOnBufferingListener.onBufferingStart();
                    }
                    Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    //缓冲结束
                    if (mOnBufferingListener!=null){
                        mOnBufferingListener.onBufferingEnd();
                    }
                    Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                    break;
                case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                    //网络宽频 ,可获取下载速度 i1
                    Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + i1);
                    break;
                case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                    //坏交错?
                    Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                    break;
                case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                    //媒体不支持拖动
                    Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                    break;
                case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    //元数据开始更新
                    Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                    break;
                case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                    //不支持字幕
                    Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                    break;
                case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                    //媒体信息副标题超时
                    Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                    break;
                case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                    //旋转变化 i1
                    break;
                case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                    //音频媒体开始渲染
                    Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                    break;
            }
            return true;
        }
    };

    // TODO: 2017/5/30  添加一些对外回调
    public interface OnBufferingListener{
        void onBufferingStart();
        void onBufferingUpdate(int percent);
        void onBufferingEnd();
    }

    public void setOnBufferingListener(OnBufferingListener onBufferingListener) {
        mOnBufferingListener = onBufferingListener;
    }

    private OnBufferingListener mOnBufferingListener;

    //错误回调
    public interface OnErrorListener{
        void onError(int framework_err, int impl_err);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    private OnErrorListener mOnErrorListener;

    //info 监听

    public interface OnInfoListener{
         boolean onInfo(int arg , int arg1) ;
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        mOnInfoListener = onInfoListener;
    }

    private OnInfoListener mOnInfoListener;
}
