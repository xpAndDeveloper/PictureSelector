package com.luck.picture.lib;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.luck.picture.lib.camera.MDCustomCameraView;
import com.luck.picture.lib.camera.listener.CameraListener;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.dialog.PictureCustomDialog;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.camera.view.CaptureLayoutMd;
import com.luck.picture.lib.widget.PictureMdBottomBarView;

import java.io.File;
import java.util.ArrayList;

/**
 * @author：luck
 * @date：2020-01-04 14:05
 * @describe：Custom photos and videos
 */
public class PictureCustomCameraFragment extends PictureSelectorCameraEmptyFragment {
    private final static String TAG = PictureCustomCameraFragment.class.getSimpleName();

    private LinearLayout pictureEmpty;
    private MDCustomCameraView mCameraView;
    protected boolean isEnterSetting;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 验证存储权限
        boolean isExternalStorage = PermissionChecker
                .checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!isExternalStorage) {
            PermissionChecker.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
            return;
        }

        // 验证相机权限和麦克风权限
        if (PermissionChecker
                .checkSelfPermission(requireContext(), Manifest.permission.CAMERA)) {
            boolean isRecordAudio = PermissionChecker.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
            if (isRecordAudio) {
                createCameraView();
            } else {
                PermissionChecker.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.RECORD_AUDIO}, PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE);
            }
        } else {
            PermissionChecker.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * 创建CameraView
     */
    private void createCameraView() {
        if (mCameraView == null) {
            mCameraView = new MDCustomCameraView(getContext());
            pictureEmpty.addView(mCameraView);
            initView();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden){

        }else {
            onTakePhoto();
        }
    }

    @Override
    protected void initWidgets(View root) {
        super.initWidgets(root);
        pictureEmpty = root.findViewById(R.id.picture_empty);
    }

    @Override
    public int getResourceId() {
        return R.layout.picture_empty;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 这里只针对权限被手动拒绝后进入设置页面重新获取权限后的操作
        if (isEnterSetting) {
            boolean isExternalStorage = PermissionChecker
                    .checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (isExternalStorage) {
                boolean isCameraPermissionChecker = PermissionChecker
                        .checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
                if (isCameraPermissionChecker) {
                    boolean isRecordAudio = PermissionChecker
                            .checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
                    if (isRecordAudio) {
                        createCameraView();
                    } else {
                        showPermissionsDialog(false, getString(R.string.picture_audio));
                    }
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_camera));
                }
            } else {
                showPermissionsDialog(false, getString(R.string.picture_jurisdiction));
            }
            isEnterSetting = false;
        }
    }

    /**
     * 初始化控件
     */
    protected void initView() {
        mCameraView.setPictureSelectionConfig(config);
        // 视频最大拍摄时长
        if (config.recordVideoSecond > 0) {
            mCameraView.setRecordVideoMaxTime(config.recordVideoSecond);
        }
        // 视频最小拍摄时长
        if (config.recordVideoMinSecond > 0) {
            mCameraView.setRecordVideoMinTime(config.recordVideoMinSecond);
        }
        // 设置拍照时loading色值
        if (config.captureLoadingColor != 0) {
            mCameraView.setCaptureLoadingColor(config.captureLoadingColor);
        }
        // 获取CameraView
        if (config.isCameraAroundState) {
            mCameraView.toggleCamera();
        }
        // 获取录制按钮
        CaptureLayoutMd captureLayout = mCameraView.getCaptureLayout();
        if (captureLayout != null) {
            mCaptureLayout = captureLayout;
            captureLayout.setButtonFeatures(config.buttonFeatures);
        }
        // 拍照预览
        mCameraView.setImageCallbackListener((file, imageView) -> {
            if (config != null && PictureSelectionConfig.imageEngine != null && file != null) {
                PictureSelectionConfig.imageEngine.loadImage(getContext(), file.getAbsolutePath(), imageView);
            }
        });
        // 设置拍照或拍视频回调监听
        mCameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureSuccess(@NonNull File file) {
                config.cameraMimeType = PictureMimeType.ofImage();
                Intent intent = new Intent();
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.getAbsolutePath());
                intent.putExtra(PictureConfig.EXTRA_CONFIG, config);
                dispatchHandleCamera(intent);
//                if (config.camera) { // 自定义相机
//                    dispatchHandleCamera(intent);
//                } else {
//                    requireActivity().setResult(Activity.RESULT_OK, intent);
//                    onBackPressed();
//                }
            }

            @Override
            public void onRecordSuccess(@NonNull File file) {
                config.cameraMimeType = PictureMimeType.ofVideo();
                Intent intent = new Intent();
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.getAbsolutePath());
                intent.putExtra(PictureConfig.EXTRA_CONFIG, config);
                dispatchHandleCamera(intent);
//                if (config.camera) { // 自定义相机
//                    dispatchHandleCamera(intent);
//                } else {
//                    requireActivity().setResult(Activity.RESULT_OK, intent);
//                    onBackPressed();
//                }
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.i(TAG, "onError: " + message);
            }
        });

        //左边按钮点击事件
        mCameraView.setOnClickListener(() -> onBackPressed());
    }

    public void onBackPressed() {
        if (config != null && config.camera && PictureSelectionConfig.listener != null) {
            PictureSelectionConfig.listener.onCancel();
        }
        exit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // 存储权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionChecker.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
                } else {
                    showPermissionsDialog(true, getString(R.string.picture_jurisdiction));
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // 相机权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean isRecordAudio = PermissionChecker.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
                    if (isRecordAudio) {
                        createCameraView();
                    } else {
                        PermissionChecker.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.RECORD_AUDIO}, PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE);
                    }
                } else {
                    showPermissionsDialog(true, getString(R.string.picture_camera));
                }
                break;
            case PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE:
                // 录音权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createCameraView();
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_audio));
                }
                break;
        }
    }

    @Override
    protected void showPermissionsDialog(boolean isCamera, String errorMsg) {
        if (isHidden()) {
            return;
        }
        final PictureCustomDialog dialog =
                new PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
        Button btn_commit = dialog.findViewById(R.id.btn_commit);
        btn_commit.setText(getString(R.string.picture_go_setting));
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tv_content = dialog.findViewById(R.id.tv_content);
        tvTitle.setText(getString(R.string.picture_prompt));
        tv_content.setText(errorMsg);
        btn_cancel.setOnClickListener(v -> {
            if (!isHidden()) {
                dialog.dismiss();
            }
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onCancel();
            }
            exit();
        });
        btn_commit.setOnClickListener(v -> {
            if (!isHidden()) {
                dialog.dismiss();
            }
            PermissionChecker.launchAppDetailsSettings(getContext());
            isEnterSetting = true;
        });
        dialog.show();
    }

    public void setCameraPreviewIsVideo(boolean isVideo) {
        if (mCameraView != null) {
            mCameraView.setCameraPreviewIsVideo(isVideo);
        }
    }

    @Override
    public void onDestroyView() {
        if (mCameraView != null) {
            mCameraView.unbindCameraController();
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mCameraView != null) {
            mCameraView.unbindCameraController();
        }
        super.onDestroy();
    }
}
