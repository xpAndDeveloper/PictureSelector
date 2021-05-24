package com.luck.picture.lib.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.video.OnVideoSavedCallback;
import androidx.camera.view.video.OutputFileOptions;
import androidx.camera.view.video.OutputFileResults;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.luck.picture.lib.PictureMediaScannerConnection;
import com.luck.picture.lib.R;
import com.luck.picture.lib.camera.controller.MdCameraController;
import com.luck.picture.lib.camera.listener.CameraListener;
import com.luck.picture.lib.camera.listener.CaptureListener;
import com.luck.picture.lib.camera.listener.ClickListener;
import com.luck.picture.lib.camera.listener.ImageCallbackListener;
import com.luck.picture.lib.camera.listener.LoadingListener;
import com.luck.picture.lib.camera.listener.TypeListener;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.thread.PictureThreadUtils;
import com.luck.picture.lib.tools.AndroidQTransformUtils;
import com.luck.picture.lib.tools.DateUtils;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.luck.picture.lib.tools.StringUtils;
import com.luck.picture.lib.camera.view.CaptureLayoutMd;
import com.luck.picture.lib.tools.ToastUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import VideoHandle.EpEditor;
import VideoHandle.EpVideo;
import VideoHandle.OnEditorListener;

/**
 * @author：luck
 * @date：2020-01-04 13:41
 * @describe：自定义相机View
 */
public class MDCustomCameraView extends RelativeLayout {
    /**
     * 默认最小录制时间
     */
    public static final int DEFAULT_MIN_RECORD_VIDEO = 1500;
    /**
     * 拍照模式
     */
    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;
    /**
     * 录像模式
     */
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;
    /**
     * 两者都可以
     */
//    public static final int BUTTON_STATE_BOTH = 0x103;
    /**
     * 闪关灯状态
     */
    private static final int TYPE_FLASH_AUTO = 0x021;
    private static final int TYPE_FLASH_ON = 0x022;
    private static final int TYPE_FLASH_OFF = 0x023;
    private int type_flash = TYPE_FLASH_OFF;
    private PictureSelectionConfig mConfig;
    private PreviewView mCameraPreviewView;
    private MdCameraController mCameraController;
    /**
     * 回调监听
     */
    private CameraListener mCameraListener;
    private ClickListener mOnClickListener;
    private ImageCallbackListener mImageCallbackListener;
    private LoadingListener mLoadingListener;
    private ImageView mImagePreview;
    private ImageView mSwitchCamera;
    private ImageView mFlashLamp;
    private CaptureLayoutMd mCaptureLayout;
    private MediaPlayer mMediaPlayer;
    private TextureView mTextureView;
    private long recordTime = 0;
    private File mOutMediaFile;
    private Context mContext;

    public MDCustomCameraView(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public MDCustomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
    }

    public MDCustomCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
    }

    public void initView() {
        inflate(getContext(), R.layout.picture_camera_view_md, this);
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.picture_color_black));
        mCameraPreviewView = findViewById(R.id.cameraPreviewView);
        mTextureView = findViewById(R.id.video_play_preview);
        mImagePreview = findViewById(R.id.image_preview);
        mSwitchCamera = findViewById(R.id.image_switch);
        mFlashLamp = findViewById(R.id.image_flash);
        mCaptureLayout = findViewById(R.id.capture_layout);
        mSwitchCamera.setImageResource(R.drawable.picture_ic_camera);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCameraController = new MdCameraController(getContext(), (LifecycleOwner) getContext(), mCameraPreviewView, MdCameraController.CameraModel.IMAGE_MODEL);
        }
        setFlashRes();
        mFlashLamp.setOnClickListener(v -> {
            type_flash++;
            if (type_flash > 0x023)
                type_flash = TYPE_FLASH_AUTO;
            setFlashRes();
        });
        mCaptureLayout.setDuration(15 * 1000);
        //切换摄像头
        mSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCamera();
            }
        });
        //拍照 录像
        mCaptureLayout.setCaptureListener(new CaptureListener() {
            @Override
            public void takePictures() {
                mOutMediaFile = createImageFile();
                mCaptureLayout.setButtonCaptureEnabled(false);
                mSwitchCamera.setVisibility(INVISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
//                mCameraController.setEnabledUseCases(MdCameraController.CameraModel.IMAGE_MODEL);
                ImageCapture.OutputFileOptions fileOptions =
                        new ImageCapture.OutputFileOptions.Builder(mOutMediaFile)
                                .build();
                mCameraController.takePicture(fileOptions, ContextCompat.getMainExecutor(getContext()),
                        new MyImageResultCallback(mOutMediaFile,
                                mImagePreview, mCaptureLayout, mImageCallbackListener, mCameraListener));
            }

            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void recordStart() {
                mOutMediaFile = createVideoFile();
                mSwitchCamera.setVisibility(INVISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
//                mCameraController.setEnabledUseCases(MdCameraController.CameraModel.VIDEO_MODEL);
                OutputFileOptions fileOptions = OutputFileOptions.builder(mOutMediaFile).build();
                mCameraController.startRecording(fileOptions, ContextCompat.getMainExecutor(getContext()), new OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull OutputFileResults outputFileResults) {
                        long minSecond = mConfig.recordVideoMinSecond <= 0 ? DEFAULT_MIN_RECORD_VIDEO : mConfig.recordVideoMinSecond * 1000;
                        if (recordTime < minSecond && mOutMediaFile.exists() && mOutMediaFile.delete()) {
                            return;
                        }
                        if (mCameraController.getCameraSelector() == CameraSelector.DEFAULT_FRONT_CAMERA){
                            mCaptureLayout.setButtonCaptureEnabled(false);
                            if (mLoadingListener != null) mLoadingListener.onLoadingShow();
                            File newFile = createVideoFile();
                            EpVideo epVideo = new EpVideo(mOutMediaFile.getAbsolutePath());
                            epVideo.rotation(0, true);
                            EpEditor.OutputOption outputOption = new EpEditor.OutputOption(newFile.getAbsolutePath());
                            EpEditor.exec(epVideo, outputOption, new OnEditorListener(){
                                @Override
                                public void onSuccess() {
                                    if (mContext instanceof Activity){
                                        ((Activity)mContext).runOnUiThread(() -> {
                                            if (mLoadingListener != null) mLoadingListener.onLoadingHide();
                                            mOutMediaFile = newFile;
                                            mCaptureLayout.startTypeBtnAnimator();
                                        });
                                    }
                                }

                                @Override
                                public void onFailure() {
                                    if (mContext instanceof Activity){
                                        ((Activity)mContext).runOnUiThread(() -> {
                                            if (mLoadingListener != null) mLoadingListener.onLoadingHide();
                                            ToastUtils.s(mContext, "保存视频失败");
                                        });
                                    }
                                }

                                @Override
                                public void onProgress(float progress) {

                                }
                            });
                        } else {
                            mCaptureLayout.startTypeBtnAnimator();
                        }

                        // todo 不要删除，现在是拍摄完视频就选中，没有预览
//                        if (mCameraController.getCameraSelector() == CameraSelector.DEFAULT_FRONT_CAMERA) {
//                            mTextureView.setRotationY(180); // 前置摄像头播放镜像旋转
//                        }
//                        mTextureView.setVisibility(View.VISIBLE);
//                        mCameraPreviewView.setVisibility(View.INVISIBLE);
//                        if (mTextureView.isAvailable()) {
//                            startVideoPlay(mOutMediaFile);
//                        } else {
//                            mTextureView.setSurfaceTextureListener(surfaceTextureListener);
//                        }
                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        if (mCameraListener != null) {
                            mCameraListener.onError(videoCaptureError, message, cause);
                        }
                    }
                });
            }

            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void recordShort(final long time) {
                recordTime = time;
                mSwitchCamera.setVisibility(VISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
                mCaptureLayout.resetCaptureLayout();
                mCaptureLayout.setTextWithAnimation(getContext().getString(R.string.picture_recording_time_is_short));
                mCameraController.stopRecording(false);
                ToastUtils.s(mContext, getContext().getString(R.string.picture_recording_time_is_short));
            }

            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void recordEnd(long time) {
                recordTime = time;
                mCameraController.stopRecording(true);
            }

            @Override
            public void recordZoom(float zoom) {

            }

            @Override
            public void recordError() {
                if (mCameraListener != null) {
                    mCameraListener.onError(0, "An unknown error", null);
                }
            }
        });
        //确认 取消
        mCaptureLayout.setTypeListener(new TypeListener() {
            @Override
            public void cancel() {
                stopVideoPlay();
                resetState();
            }

            @Override
            public void confirm() {
                if (mOutMediaFile == null || !mOutMediaFile.exists()) {
                    return;
                }
                // 拷贝一份至公共目录, 针对 Android Q 以上
                if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mConfig.cameraPath)) {
                    PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<Boolean>() {

                        @Override
                        public Boolean doInBackground() {
                            return AndroidQTransformUtils.copyPathToDCIM(getContext(), mOutMediaFile, Uri.parse(mConfig.cameraPath));
                        }

                        @Override
                        public void onSuccess(Boolean result) {
                            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool());
                            if (mCameraController.isImageCaptureEnabled()) {
                                mImagePreview.setVisibility(INVISIBLE);
                                if (mCameraListener != null) {
                                    mCameraListener.onPictureSuccess(mOutMediaFile);
                                }
                            } else {
                                stopVideoPlay();
                                if (mCameraListener != null || !mOutMediaFile.exists()) {
                                    mCameraListener.onRecordSuccess(mOutMediaFile);
                                }
                            }
                        }
                    });
                } else {
                    if (mCameraController.isImageCaptureEnabled()) {
                        mImagePreview.setVisibility(INVISIBLE);
                        if (mCameraListener != null) {
                            mCameraListener.onPictureSuccess(mOutMediaFile);
                        }
                    } else {
                        stopVideoPlay();
                        if (mCameraListener != null || !mOutMediaFile.exists()) {
                            mCameraListener.onRecordSuccess(mOutMediaFile);
                        }
                    }
                }
            }
        });
        mCaptureLayout.setLeftClickListener(() -> {
            if (mOnClickListener != null) {
                mOnClickListener.onClick();
            }
        });
    }

    /**
     * 拍照回调
     */
    private static class MyImageResultCallback implements ImageCapture.OnImageSavedCallback {
        private final WeakReference<File> mFileReference;
        private final WeakReference<ImageView> mImagePreviewReference;
        private final WeakReference<CaptureLayoutMd> mCaptureLayoutReference;
        private final WeakReference<ImageCallbackListener> mImageCallbackListenerReference;
        private final WeakReference<CameraListener> mCameraListenerReference;

        public MyImageResultCallback(
                File imageOutFile, ImageView imagePreview,
                CaptureLayoutMd captureLayout, ImageCallbackListener imageCallbackListener,
                CameraListener cameraListener) {
            super();
            this.mFileReference = new WeakReference<>(imageOutFile);
            this.mImagePreviewReference = new WeakReference<>(imagePreview);
            this.mCaptureLayoutReference = new WeakReference<>(captureLayout);
            this.mImageCallbackListenerReference = new WeakReference<>(imageCallbackListener);
            this.mCameraListenerReference = new WeakReference<>(cameraListener);
        }

        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
            if (mCaptureLayoutReference.get() != null) {
                mCaptureLayoutReference.get().setButtonCaptureEnabled(true);
            }
            if (mImageCallbackListenerReference.get() != null
                    && mFileReference.get() != null
                    && mImagePreviewReference.get() != null) {
                mImageCallbackListenerReference.get().onLoadImage(mFileReference.get(), mImagePreviewReference.get());
            }
            if (mImagePreviewReference.get() != null) {
                mImagePreviewReference.get().setVisibility(View.VISIBLE);
            }
            if (mCaptureLayoutReference.get() != null) {
                mCaptureLayoutReference.get().startTypeBtnAnimator();
            }
        }

        @Override
        public void onError(@NonNull ImageCaptureException exception) {
            if (mCaptureLayoutReference.get() != null) {
                mCaptureLayoutReference.get().setButtonCaptureEnabled(true);
            }
            if (mCameraListenerReference.get() != null) {
                mCameraListenerReference.get().onError(exception.getImageCaptureError(), exception.getMessage(), exception.getCause());
            }
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            startVideoPlay(mOutMediaFile);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    public File createImageFile() {
        if (SdkVersionUtils.checkedAndroid_Q()) {
            String diskCacheDir = PictureFileUtils.getDiskCacheDir(getContext());
            File rootDir = new File(diskCacheDir);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
            boolean isOutFileNameEmpty = TextUtils.isEmpty(mConfig.cameraFileName);
            String suffix;
            if (mConfig.suffixType.startsWith("image/")) {
                suffix = mConfig.suffixType.replaceAll("image/", ".");
            } else {
                suffix = PictureMimeType.JPEG;
            }
            String newFileImageName = isOutFileNameEmpty ? DateUtils.getCreateFileName("IMG_") + suffix : mConfig.cameraFileName;
            File cameraFile = new File(rootDir, newFileImageName);
            Uri outUri = getOutUri(PictureMimeType.ofImage());
            if (outUri != null) {
                mConfig.cameraPath = outUri.toString();
            }
            return cameraFile;
        } else {
            String cameraFileName = "";
            if (!TextUtils.isEmpty(mConfig.cameraFileName)) {
                boolean isSuffixOfImage = PictureMimeType.isSuffixOfImage(mConfig.cameraFileName);
                mConfig.cameraFileName = !isSuffixOfImage ? StringUtils.renameSuffix(mConfig.cameraFileName, PictureMimeType.JPEG) : mConfig.cameraFileName;
                cameraFileName = mConfig.camera ? mConfig.cameraFileName : StringUtils.rename(mConfig.cameraFileName);
            }
            File cameraFile = PictureFileUtils.createCameraFile(getContext(),
                    PictureMimeType.ofImage(), cameraFileName, mConfig.suffixType, mConfig.outPutCameraPath);
            mConfig.cameraPath = cameraFile.getAbsolutePath();
            return cameraFile;
        }
    }

    public File createVideoFile() {
        if (SdkVersionUtils.checkedAndroid_Q()) {
            String diskCacheDir = PictureFileUtils.getVideoDiskCacheDir(getContext());
            File rootDir = new File(diskCacheDir);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
            boolean isOutFileNameEmpty = TextUtils.isEmpty(mConfig.cameraFileName);
            String suffix;
            if (mConfig.suffixType.startsWith("video/")) {
                suffix = mConfig.suffixType.replaceAll("video/", ".");
            } else {
                suffix = PictureMimeType.MP4;
            }
            String newFileImageName = isOutFileNameEmpty ? DateUtils.getCreateFileName("VID_") + suffix : mConfig.cameraFileName;
            File cameraFile = new File(rootDir, newFileImageName);
            Uri outUri = getOutUri(PictureMimeType.ofVideo());
            if (outUri != null) {
                mConfig.cameraPath = outUri.toString();
            }
            return cameraFile;
        } else {
            String cameraFileName = "";
            if (!TextUtils.isEmpty(mConfig.cameraFileName)) {
                boolean isSuffixOfImage = PictureMimeType.isSuffixOfImage(mConfig.cameraFileName);
                mConfig.cameraFileName = !isSuffixOfImage ? StringUtils
                        .renameSuffix(mConfig.cameraFileName, PictureMimeType.MP4) : mConfig.cameraFileName;
                cameraFileName = mConfig.camera ? mConfig.cameraFileName : StringUtils.rename(mConfig.cameraFileName);
            }
            File cameraFile = PictureFileUtils.createCameraFile(getContext(),
                    PictureMimeType.ofVideo(), cameraFileName, mConfig.suffixType, mConfig.outPutCameraPath);
            mConfig.cameraPath = cameraFile.getAbsolutePath();
            return cameraFile;
        }
    }

    private Uri getOutUri(int type) {
        return type == PictureMimeType.ofVideo()
                ? MediaUtils.createVideoUri(getContext(), mConfig.cameraFileName, mConfig.suffixType) : MediaUtils.createImageUri(getContext(), mConfig.cameraFileName, mConfig.suffixType);
    }

    public void setCameraListener(CameraListener cameraListener) {
        this.mCameraListener = cameraListener;
    }

    public void setPictureSelectionConfig(PictureSelectionConfig config) {
        this.mConfig = config;
    }

    /**
     * 设置录制视频最大时长 秒
     */
    public void setRecordVideoMaxTime(int maxDurationTime) {
        mCaptureLayout.setDuration(maxDurationTime * 1000);
    }

    /**
     * 设置录制视频最小时长 秒
     */
    public void setRecordVideoMinTime(int minDurationTime) {
        mCaptureLayout.setMinDuration(minDurationTime * 1000);
    }

    /**
     * 设置拍照时loading色值
     *
     * @param color
     */
    public void setCaptureLoadingColor(int color) {
        mCaptureLayout.setCaptureLoadingColor(color);
    }

    /**
     * 切换前后摄像头
     */
    public void toggleCamera() {
        if (mCameraController.getCameraSelector() == CameraSelector.DEFAULT_BACK_CAMERA
                && mCameraController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            mCameraController.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA);
        } else if (mCameraController.getCameraSelector() == CameraSelector.DEFAULT_FRONT_CAMERA
                && mCameraController.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            mCameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        }
    }

    /**
     * 关闭相机界面按钮
     *
     * @param clickListener
     */
    public void setOnClickListener(ClickListener clickListener) {
        this.mOnClickListener = clickListener;
    }

    public void setImageCallbackListener(ImageCallbackListener mImageCallbackListener) {
        this.mImageCallbackListener = mImageCallbackListener;
    }

    public void setLoadingListener(LoadingListener loadingListener) {
        this.mLoadingListener = loadingListener;
    }

    private void setFlashRes() {
        switch (type_flash) {
            case TYPE_FLASH_AUTO:
                mFlashLamp.setImageResource(R.drawable.picture_ic_flash_auto);
                mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_AUTO);
                break;
            case TYPE_FLASH_ON:
                mFlashLamp.setImageResource(R.drawable.picture_ic_flash_on);
                mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_ON);
                break;
            case TYPE_FLASH_OFF:
                mFlashLamp.setImageResource(R.drawable.picture_ic_flash_off);
                mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_OFF);
                break;
        }
    }

    public CaptureLayoutMd getCaptureLayout() {
        return mCaptureLayout;
    }

    /**
     * 重置状态
     */
    @SuppressLint("UnsafeOptInUsageError")
    private void resetState() {
        if (mCameraController.isImageCaptureEnabled()) {
            mImagePreview.setVisibility(INVISIBLE);
        } else {
            if (mCameraController.isRecording()) {
                mCameraController.stopRecording(false);
            }
        }
        if (mOutMediaFile != null && mOutMediaFile.exists()) {
            mOutMediaFile.delete();
            if (!SdkVersionUtils.checkedAndroid_Q()) {
                new PictureMediaScannerConnection(getContext(), mOutMediaFile.getAbsolutePath());
            }
        }
        mSwitchCamera.setVisibility(VISIBLE);
        mFlashLamp.setVisibility(INVISIBLE);
        mCameraPreviewView.setVisibility(View.VISIBLE);
        mCaptureLayout.resetCaptureLayout();
    }

    /**
     * 开始循环播放视频
     *
     * @param videoFile
     */
    private void startVideoPlay(File videoFile) {
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }
            mMediaPlayer.setDataSource(videoFile.getAbsolutePath());
            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnPreparedListener(mp -> {
                mp.start();

                float ratio = mp.getVideoWidth() * 1f / mp.getVideoHeight();
                int width1 = mTextureView.getWidth();
                ViewGroup.LayoutParams layoutParams = mTextureView.getLayoutParams();
                layoutParams.height = (int) (width1 / ratio);
                mTextureView.setLayoutParams(layoutParams);
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            stopVideoPlay();
            mCameraPreviewView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 停止视频播放
     */
    private void stopVideoPlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mTextureView.setVisibility(View.GONE);
    }

    /**
     * fragemnt 显示或隐藏的时候
     * @param isVisibleToUser
     */
    public void fragmentUserVisibleHint(boolean isVisibleToUser){
        if (!isVisibleToUser) {
            stopVideoPlay();
            resetState();
        }
    }

    /**
     * 切换拍照和视频的时候
     * @param isVideo
     */
    public void setCameraPreviewIsVideo(boolean isVideo) {
        if (mCameraController == null) return;
        mCameraController.setEnabledUseCases(isVideo ? MdCameraController.CameraModel.VIDEO_MODEL : MdCameraController.CameraModel.IMAGE_MODEL);
        mCaptureLayout.setCameraType(isVideo ? BUTTON_STATE_ONLY_RECORDER : BUTTON_STATE_ONLY_CAPTURE);
        mCameraPreviewView.post(() -> {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mCameraPreviewView.getLayoutParams();
            layoutParams.dimensionRatio = isVideo ? "9:16" : "3:4";
        });
    }

    public void unbindCameraController() {
        if (mCameraController != null) {
            mCameraController.unbind();
        }
    }
}
