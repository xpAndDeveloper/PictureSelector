package com.luck.picture.lib.camera.controller

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FlashMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.SensorRotationListener
import androidx.camera.view.video.ExperimentalVideo
import androidx.camera.view.video.OnVideoSavedCallback
import androidx.camera.view.video.OutputFileOptions
import androidx.camera.view.video.OutputFileResults
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 *Created by wanghai
 *Date : 2021/5/20
 *Describe :
 */
class MdCameraController(context: Context, private var lifecycleOwner: LifecycleOwner?, private val viewFinder: PreviewView, cameraModel: CameraModel) {
    private val TAG = "CameraController"

    private var mContext: Context = context

    enum class CameraModel {
        IMAGE_MODEL, VIDEO_MODEL
    }

    private var mEnabledUseCases: CameraModel = cameraModel

    private var mCamera: Camera? = null
    private var preview: Preview? = null
    private var mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var mCameraProvider: ProcessCameraProvider? = null

    private var mImageCapture: ImageCapture? = null
    private var mVideoCapture: VideoCapture? = null

    private var mSensorRotationListener: SensorRotationListener? = null
    private var rotation: Int = 0

    private val mVideoIsRecording = AtomicBoolean(false)

    private var mSaveVideo = false


    private fun initListener() {
        mSensorRotationListener = object : SensorRotationListener(mContext) {
            override fun onRotationChanged(rotation: Int) {
                mImageCapture?.targetRotation = rotation
                mVideoCapture?.setTargetRotation(rotation)
            }
        }
    }

    init {
        viewFinder.post {
            rotation = viewFinder.display.rotation
            initPreview()
            mImageCapture = ImageCapture.Builder()
                    .setTargetRotation(rotation)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
            // 视频的输出配置
            mVideoCapture = VideoCapture.Builder()
                    .setTargetRotation(rotation)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
            initCamera()
            initListener()
        }
    }

    private fun initPreview() {
        if (mEnabledUseCases == CameraModel.IMAGE_MODEL) {
            // 图片预览的配置
            preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build().also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
        } else {
            // 视频预览的配置
            preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build().also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
        }
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)
        cameraProviderFuture.addListener(Runnable {
            // 等待相机初始化完成
            mCameraProvider = cameraProviderFuture.get()
            startCamera()
        }, ContextCompat.getMainExecutor(mContext))
    }

    private fun startCamera() {
        try {
            // Unbind use cases before rebinding
            mCameraProvider?.unbindAll()
            // Bind use cases to camera
            mCamera = mCameraProvider?.bindToLifecycle(
                    lifecycleOwner!!,
                    mCameraSelector,
                    preview,
                    if (mEnabledUseCases == CameraModel.VIDEO_MODEL) mVideoCapture else mImageCapture)
        } catch (exc: Exception) {
            Log.e("PhotoPicker", "bindCameraCase 失败", exc)
        }
    }

    /**
     * 切换拍视频还是拍照
     */
    fun setEnabledUseCases(model: CameraModel) {
        if (mEnabledUseCases == CameraModel.VIDEO_MODEL) {
            stopRecording()
        }
        mEnabledUseCases = model
        initPreview()
        startCamera()
    }

    /**
     * 停止录制
     * 是否需要保存视频
     */
    fun stopRecording(saveVideo: Boolean = false) {
        if (mVideoIsRecording.get()) {
            mSaveVideo = saveVideo
            mVideoCapture?.stopRecording()
        }
    }

    fun getCameraSelector(): CameraSelector {
//        Threads.checkMainThread()
        return mCameraSelector
    }

    @MainThread
    fun hasCamera(cameraSelector: CameraSelector): Boolean {
//        Threads.checkMainThread()
        checkNotNull(mCameraProvider) {
            ("Camera not initialized. Please wait for "
                    + "the initialization future to finish. See #getInitializationFuture().")
        }
        return try {
            mCameraProvider!!.hasCamera(cameraSelector)
        } catch (e: CameraInfoUnavailableException) {
            Log.w(TAG, "Failed to check camera availability", e)
            false
        }
    }

    @MainThread
    fun setImageCaptureFlashMode(@FlashMode flashMode: Int) {
//        Threads.checkMainThread()
        if (mEnabledUseCases == CameraModel.IMAGE_MODEL) {
            mImageCapture?.flashMode = flashMode
        }
    }

    @ExperimentalVideo
    @MainThread
    fun isRecording(): Boolean {
//        Threads.checkMainThread()
        return mVideoIsRecording.get()
    }

    // 是否是拍照
    @MainThread
    fun isImageCaptureEnabled(): Boolean {
        return mEnabledUseCases == CameraModel.IMAGE_MODEL
    }

    fun unbind() {
        mCamera = null
        if (mCameraProvider != null) {
            mCameraProvider!!.unbindAll()
            mCameraProvider = null
        }
        lifecycleOwner = null
    }

    @MainThread
    fun takePicture(
            outputFileOptions: ImageCapture.OutputFileOptions,
            executor: Executor,
            imageSavedCallback: ImageCapture.OnImageSavedCallback) {
//        Threads.checkMainThread()
//        Preconditions.checkState(isCameraInitialized(), CameraController.CAMERA_NOT_INITIALIZED)
//        Preconditions.checkState(isImageCaptureEnabled(), CameraController.IMAGE_CAPTURE_DISABLED)
        updateMirroringFlagInOutputFileOptions(outputFileOptions)
        mImageCapture?.takePicture(outputFileOptions, executor, imageSavedCallback)
    }

    @MainThread
    fun startRecording(outputFileOptions: OutputFileOptions,
                       executor: Executor, callback: OnVideoSavedCallback) {
//        Threads.checkMainThread()
//        Preconditions.checkState(isCameraInitialized(), CameraController.CAMERA_NOT_INITIALIZED)
//        Preconditions.checkState(isVideoCaptureEnabled(), CameraController.VIDEO_CAPTURE_DISABLED)
        mVideoCapture?.startRecording(outputFileOptions.toVideoCaptureOutputFileOptions(), executor,
                object : VideoCapture.OnVideoSavedCallback {
                    override fun onVideoSaved(
                            outputFileResults: VideoCapture.OutputFileResults) {
                        mVideoIsRecording.set(false)
                        if (mSaveVideo) {
                            callback.onVideoSaved(
                                    OutputFileResults.create(outputFileResults.savedUri))
                        }
                    }

                    override fun onError(videoCaptureError: Int, message: String,
                                         cause: Throwable?) {
                        mVideoIsRecording.set(false)
                        callback.onError(videoCaptureError, message, cause)
                    }
                })
        mVideoIsRecording.set(true)
    }

    private fun updateMirroringFlagInOutputFileOptions(
            outputFileOptions: ImageCapture.OutputFileOptions) {
        if (mCameraSelector.lensFacing != null
                && !outputFileOptions.metadata.isReversedHorizontalSet) {
            outputFileOptions.metadata.isReversedHorizontal = mCameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT
        }
    }

    fun setCameraSelector(cameraSelector: CameraSelector) {
        mCameraSelector = cameraSelector
        startCamera()
    }
}