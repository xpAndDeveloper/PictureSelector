package com.luck.picture.lib;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.luck.picture.lib.camera.view.CaptureLayoutMd;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.immersive.ImmersiveManage;
import com.luck.picture.lib.manager.UCropManager;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.thread.PictureThreadUtils;
import com.luck.picture.lib.tools.BitmapUtils;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.luck.picture.lib.tools.ToastUtils;
import com.luck.picture.lib.tools.ValueOf;
import com.luck.picture.lib.widget.PictureMdBottomBarView;
import com.yalantis.ucrop.UCrop;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2019-11-15 21:41
 * @describe：PictureSelectorCameraEmptyActivity
 */
public class PictureSelectorCameraEmptyFragment extends PictureBaseFragment {
    protected PictureMdBottomBarView mPictureBottomView;
    protected CaptureLayoutMd mCaptureLayout;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (config == null) {
            exit();
            return;
        }
        if (!config.isUseCustomCamera) {
            setActivitySize();
            if (savedInstanceState == null) {
                if (PermissionChecker
                        .checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        PermissionChecker
                                .checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                        if (config.chooseMode == PictureConfig.TYPE_VIDEO) {
                            PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_VIDEO);
                        } else {
                            PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_IMAGE);
                        }
                    } else {
                        onTakePhoto();
                    }
                } else {
                    PermissionChecker.requestPermissions(requireActivity(), new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
                }
            }
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void initWidgets(View root) {
        super.initWidgets(root);
        mPictureBottomView = root.findViewById(R.id.picture_md_view);
        mPictureBottomView.initView(config, numComplete, true);
        mPictureBottomView.dataChanged(new ArrayList<>());
        mPictureBottomView.setOnPictureListener(new PictureMdBottomBarView.OnPictureListener() {
            @Override
            public void onItemDragEnd(@NotNull List<? extends LocalMedia> data) {


            }

            @Override
            public void onButtonComplete() {
                onFinishNext(mPictureBottomView.getData());
            }

            @Override
            public void onItemRemove(@NotNull LocalMedia item) {
                if (mCaptureLayout != null) {
                    mCaptureLayout.isMaxMedia(mPictureBottomView.getData().size() >= config.maxSelectNum, config.maxSelectNum);
                }
            }
        });
    }

    /**
     * 设置个1像素的Activity
     */
    private void setActivitySize() {
        Window window = requireActivity().getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);
    }


    @Override
    public int getResourceId() {
        return R.layout.picture_empty;
    }


    /**
     * open camera
     */
    public void onTakePhoto() {
        if (PermissionChecker
                .checkSelfPermission(requireContext(), Manifest.permission.CAMERA)) {
            boolean isPermissionChecker = true;
            if (config != null && config.isUseCustomCamera) {
                isPermissionChecker = PermissionChecker.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
            }
            if (isPermissionChecker) {
//                startCamera();
            } else {
                PermissionChecker
                        .requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.RECORD_AUDIO}, PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE);
            }
        } else {
            PermissionChecker.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    singleCropHandleResult(data);
                    break;
                case PictureConfig.REQUEST_CAMERA:
                    dispatchHandleCamera(data);
                    break;
                default:
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onCancel();
            }
            // Delete this cameraPath when you cancel the camera
            if (requestCode == PictureConfig.REQUEST_CAMERA) {
                MediaUtils.deleteCamera(requireContext(), config.cameraPath);
            }
            exit();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data == null) {
                return;
            }
            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
            if (throwable != null) {
                ToastUtils.s(getContext(), throwable.getMessage());
            }
        }
    }

    /**
     * Single picture clipping callback
     *
     * @param data
     */
    protected void singleCropHandleResult(Intent data) {
        if (data == null) {
            return;
        }
        List<LocalMedia> medias = new ArrayList<>();
        Uri resultUri = UCrop.getOutput(data);
        if (resultUri == null) {
            return;
        }
        String cutPath = resultUri.getPath();
        boolean isCutEmpty = TextUtils.isEmpty(cutPath);
        LocalMedia media = new LocalMedia(config.cameraPath, 0, false,
                config.isCamera ? 1 : 0, 0, config.chooseMode);
        if (SdkVersionUtils.checkedAndroid_Q()) {
            int lastIndexOf = config.cameraPath.lastIndexOf("/") + 1;
            media.setId(lastIndexOf > 0 ? ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) : -1);
            media.setAndroidQToPath(cutPath);
            if (isCutEmpty) {
                if (PictureMimeType.isContent(config.cameraPath)) {
                    String path = PictureFileUtils.getPath(requireContext(), Uri.parse(config.cameraPath));
                    media.setSize(!TextUtils.isEmpty(path) ? new File(path).length() : 0);
                } else {
                    media.setSize(new File(config.cameraPath).length());
                }
            } else {
                media.setSize(new File(cutPath).length());
            }
        } else {
            // Taking a photo generates a temporary id
            media.setId(System.currentTimeMillis());
            media.setSize(new File(isCutEmpty ? media.getPath() : cutPath).length());
        }
        media.setCut(!isCutEmpty);
        media.setCutPath(cutPath);
        String mimeType = PictureMimeType.getImageMimeType(cutPath);
        media.setMimeType(mimeType);
        media.setOrientation(-1);
        if (PictureMimeType.isContent(media.getPath())) {
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(media.getPath()), media);
            } else if (PictureMimeType.isHasImage(media.getMimeType())) {
                int[] size = MediaUtils.getImageSizeForUri(getContext(), Uri.parse(media.getPath()));
                media.setWidth(size[0]);
                media.setHeight(size[1]);
            }
        } else {
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                int[] size = MediaUtils.getVideoSizeForUrl(media.getPath());
                media.setWidth(size[0]);
                media.setHeight(size[1]);
            } else if (PictureMimeType.isHasImage(media.getMimeType())) {
                int[] size = MediaUtils.getImageSizeForUrl(media.getPath());
                media.setWidth(size[0]);
                media.setHeight(size[1]);
            }
        }
        // The width and height of the image are reversed if there is rotation information
        MediaUtils.setOrientationAsynchronous(getContext(), media, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH,
                item -> {
                    medias.add(item);
                    handlerResult(medias);
                });
    }

    /**
     * dispatchHandleCamera
     *
     * @param intent
     */
    protected void dispatchHandleCamera(Intent intent) {
        boolean isAudio = config.chooseMode == PictureMimeType.ofAudio();
        config.cameraPath = isAudio ? getAudioPath(intent) : config.cameraPath;
        if (TextUtils.isEmpty(config.cameraPath)) {
            return;
        }
        showPleaseDialog();
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<LocalMedia>() {

            @Override
            public LocalMedia doInBackground() {
                LocalMedia media = new LocalMedia();
                String mimeType = isAudio ? PictureMimeType.MIME_TYPE_AUDIO : "";
                long duration = 0;
                if (!isAudio) {
                    if (PictureMimeType.isContent(config.cameraPath)) {
                        // content: Processing rules
                        String path = PictureFileUtils.getPath(getContext(), Uri.parse(config.cameraPath));
                        if (!TextUtils.isEmpty(path)) {
                            File cameraFile = new File(path);
                            mimeType = PictureMimeType.getMimeType(config.cameraMimeType);
                            media.setSize(cameraFile.length());
                        }
                        if (PictureMimeType.isHasImage(mimeType)) {
                            int[] newSize = MediaUtils.getImageSizeForUrlToAndroidQ(getContext(), config.cameraPath);
                            media.setWidth(newSize[0]);
                            media.setHeight(newSize[1]);
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(config.cameraPath), media);
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath);
                        }
                        int lastIndexOf = config.cameraPath.lastIndexOf("/") + 1;
                        media.setId(lastIndexOf > 0 ? ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) : -1);
                        media.setRealPath(path);
                        // Custom photo has been in the application sandbox into the file
                        String mediaPath = intent != null ? intent.getStringExtra(PictureConfig.EXTRA_MEDIA_PATH) : null;
                        media.setAndroidQToPath(mediaPath);
                    } else {
                        File cameraFile = new File(config.cameraPath);
                        mimeType = PictureMimeType.getMimeType(config.cameraMimeType);
                        media.setSize(cameraFile.length());
                        if (PictureMimeType.isHasImage(mimeType)) {
                            int degree = PictureFileUtils.readPictureDegree(getContext(), config.cameraPath);
                            BitmapUtils.rotateImage(degree, config.cameraPath);
                            int[] newSize = MediaUtils.getImageSizeForUrl(config.cameraPath);
                            media.setWidth(newSize[0]);
                            media.setHeight(newSize[1]);
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            int[] newSize = MediaUtils.getVideoSizeForUrl(config.cameraPath);
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath);
                            media.setWidth(newSize[0]);
                            media.setHeight(newSize[1]);
                        }
                        // Taking a photo generates a temporary id
                        media.setId(System.currentTimeMillis());
                    }
                    media.setPath(config.cameraPath);
                    media.setDuration(duration);
                    media.setMimeType(mimeType);
                    if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasVideo(media.getMimeType())) {
                        media.setParentFolderName(Environment.DIRECTORY_MOVIES);
                    } else {
                        media.setParentFolderName(PictureMimeType.CAMERA);
                    }
                    media.setChooseModel(config.chooseMode);
                    long bucketId = MediaUtils.getCameraFirstBucketId(getContext());
                    media.setBucketId(bucketId);
                    media.setDateAddedTime(Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(0, 10)));
                    // The width and height of the image are reversed if there is rotation information
                    MediaUtils.setOrientationSynchronous(getContext(), media, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH);
                }
                return media;
            }

            @Override
            public void onSuccess(LocalMedia result) {
                // Refresh the system library
                dismissDialog();
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    if (config.isFallbackVersion3) {
                        new PictureMediaScannerConnection(getContext(), config.cameraPath);
                    } else {
                        requireActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(config.cameraPath))));
                    }
                }
                dispatchCameraHandleResult(result);
                // Solve some phone using Camera, DCIM will produce repetitive problems
//                if (!SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasImage(result.getMimeType())) {
//                    int lastImageId = MediaUtils.getDCIMLastImageId(getContext());
//                    if (lastImageId != -1) {
//                        Log.e("1111111111111111","删除前一张");
//                        MediaUtils.removeMedia(getContext(), lastImageId);
//                    }
//                }
            }
        });
    }

    /**
     * 底部弹窗点击下一步时
     * @param result
     */
    private void onFinishNext(List<LocalMedia> result) {
        if (result.isEmpty()) return;
        boolean isHasImage = PictureMimeType.isHasImage(result.get(0).getMimeType());
        if (config.isCompress && isHasImage && !config.isCheckOriginalImage) {
            compressImage(result);
        } else {
            onResult(result);
        }
    }

    /**
     * dispatchCameraHandleResult
     * 拍照或拍视频完成的时候
     * @param media
     */
    private void dispatchCameraHandleResult(LocalMedia media) {
        boolean isHasImage = PictureMimeType.isHasImage(media.getMimeType());
        if (isHasImage) {
            mPictureBottomView.dataAdd(media);
            if (mCaptureLayout != null) {
                mCaptureLayout.isMaxMedia(mPictureBottomView.getData().size() >= config.maxSelectNum, config.maxSelectNum);
            }
        } else {
            // 视频处理
//            if (mCaptureLayout != null) {
//                mCaptureLayout.isMaxMedia(mPictureBottomView.getData().size() >= config.maxVideoSelectNum, config.maxVideoSelectNum);
//            }
            List<LocalMedia> result = new ArrayList<>();
            result.add(media);
            onResult(result);
        }

        //todo 需要这些逻辑
//        boolean isHasImage = PictureMimeType.isHasImage(media.getMimeType());
//        if (config.enableCrop && isHasImage) {
//            config.originalPath = config.cameraPath;
//            UCropManager.ofCrop(requireActivity(), config.cameraPath, media.getMimeType());
//        } else if (config.isCompress && isHasImage && !config.isCheckOriginalImage) {
//            List<LocalMedia> result = new ArrayList<>();
//            result.add(media);
//            compressImage(result);
//        } else {
//            List<LocalMedia> result = new ArrayList<>();
//            result.add(media);
//            onResult(result);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // Store Permissions
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionChecker.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction));
                    exit();
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // Camera Permissions
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    exit();
                    ToastUtils.s(getContext(), getString(R.string.picture_camera));
                }
                break;
            case PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE:
                // Recording Permissions
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    exit();
                    ToastUtils.s(getContext(), getString(R.string.picture_audio));
                }
                break;
        }
    }

}
