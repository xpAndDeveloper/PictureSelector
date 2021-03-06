package com.luck.picture.lib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.luck.picture.lib.app.PictureAppMaster;
import com.luck.picture.lib.compress.Luban;
import com.luck.picture.lib.compress.OnCompressListener;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.dialog.PictureCustomDialog;
import com.luck.picture.lib.dialog.PictureLoadingDialog;
import com.luck.picture.lib.engine.PictureSelectorEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.immersive.NavBarUtils;
import com.luck.picture.lib.language.PictureLanguageUtils;
import com.luck.picture.lib.model.LocalMediaPageLoader;
import com.luck.picture.lib.model.MDLocalMediaPageLoader;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.style.PictureSelectorUIStyle;
import com.luck.picture.lib.thread.PictureThreadUtils;
import com.luck.picture.lib.tools.AndroidQTransformUtils;
import com.luck.picture.lib.tools.AttrsUtils;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.luck.picture.lib.tools.StringUtils;
import com.luck.picture.lib.tools.ToastUtils;
import com.luck.picture.lib.tools.VoiceUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author???luck
 * @data???2018/3/28 ??????1:00
 * @describe: BaseActivity
 */
public abstract class PictureBaseFragment extends Fragment {
    protected PictureSelectionConfig config;
    public int myConfig = 0;
    protected boolean openWhiteStatusBar, numComplete;
    protected int colorPrimary, colorPrimaryDark;
    protected PictureLoadingDialog mLoadingDialog;
    protected List<LocalMedia> selectionMedias = new ArrayList<>();
    protected Handler mHandler = new Handler(Looper.getMainLooper());
    protected View container;
    public MDLocalMediaPageLoader localMediaPageLoader;
    /**
     * if there more
     */
    protected boolean isHasMore = true;
    /**
     * page
     */
    protected int mPage = 1;
    /**
     * is onSaveInstanceState
     */
    protected boolean isOnSaveInstanceState;

    /**
     * Whether to change the screen direction
     *
     * @return
     */
    public boolean isRequestedOrientation() {
        return true;
    }

    /**
     * get Layout Resources Id
     *
     * @return
     */
    public abstract int getResourceId();

    /**
     * init Views
     */
    protected void initWidgets(View root) {

    }

    /**
     * init PictureSelector Style
     */
    protected void initPictureSelectorStyle() {

    }

    /**
     * Set CompleteText
     */
    protected void initCompleteText(int startCount) {

    }

    /**
     * Set CompleteText
     */
    protected void initCompleteText(List<LocalMedia> list) {

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        config = new PictureSelectionConfig();
        config.initMinDuValue();

        PictureSelectionConfig.uiStyle = PictureSelectorUIStyle.ofMinDuStyle();
        if (!config.isWeChatStyle) {
            config.isWeChatStyle = PictureSelectionConfig.uiStyle.isNewSelectStyle;
        }
        PictureLanguageUtils.setAppLanguage(getContext(), config.language);
//        setTheme(config.themeStyleId == 0 ? R.style.picture_default_style : config.themeStyleId);
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            config.chooseMode = bundle.getInt("chooseMode", 0);
            List<LocalMedia> selectionData = bundle.getParcelableArrayList("selectionData");
            if (selectionData != null && selectionData.size() > 0) {
                config.selectionMedias = selectionData;
            }
            config.maxSelectNum = bundle.getInt("maxSelectNum",9);
            config.selectedNum = bundle.getInt("selectedNum",0);
        }


        if (localMediaPageLoader == null) {
            localMediaPageLoader = new MDLocalMediaPageLoader(requireContext(), config);
        }
        newCreateEngine();
        newCreateResultCallbackListener();

        initConfig();

        if (PictureSelectionConfig.uiStyle != null) {
            if (PictureSelectionConfig.uiStyle.picture_navBarColor != 0) {
                NavBarUtils.setNavBarColor((AppCompatActivity) requireActivity(), PictureSelectionConfig.uiStyle.picture_navBarColor);
            }
        } else if (PictureSelectionConfig.style != null) {
            if (PictureSelectionConfig.style.pictureNavBarColor != 0) {
                NavBarUtils.setNavBarColor((AppCompatActivity) requireActivity(), PictureSelectionConfig.style.pictureNavBarColor);
            }
        }

        int layoutResID = getResourceId();
        View root = inflater.inflate(layoutResID, container, false);
        initWidgets(root);
        initPictureSelectorStyle();
        isOnSaveInstanceState = false;
        return root;
    }

    /**
     * Get the image loading engine again, provided that the user implements the IApp interface in the Application
     */
    private void newCreateEngine() {
        if (PictureSelectionConfig.imageEngine == null) {
            PictureSelectorEngine baseEngine = PictureAppMaster.getInstance().getPictureSelectorEngine();
            if (baseEngine != null) PictureSelectionConfig.imageEngine = baseEngine.createEngine();
        }
    }

    /**
     * Retrieve the result callback listener, provided that the user implements the IApp interface in the Application
     */
    private void newCreateResultCallbackListener() {
        if (config.isCallbackMode) {
            if (PictureSelectionConfig.listener == null) {
                PictureSelectorEngine baseEngine = PictureAppMaster.getInstance().getPictureSelectorEngine();
                if (baseEngine != null) {
                    PictureSelectionConfig.listener = baseEngine.getResultCallbackListener();
                }
            }
        }
    }

    /**
     * get Context
     *
     * @return this
     */
    public Context getContext() {
        return (AppCompatActivity) requireActivity();
    }

    /**
     * init Config
     */
    private void initConfig() {
        if (config.selectionMedias != null) {
            selectionMedias.clear();
            selectionMedias.addAll(config.selectionMedias);
        }
        if (PictureSelectionConfig.uiStyle != null) {
            openWhiteStatusBar = PictureSelectionConfig.uiStyle.picture_statusBarChangeTextColor;
            if (PictureSelectionConfig.uiStyle.picture_top_titleBarBackgroundColor != 0) {
                colorPrimary = PictureSelectionConfig.uiStyle.picture_top_titleBarBackgroundColor;
            }
            if (PictureSelectionConfig.uiStyle.picture_statusBarBackgroundColor != 0) {
                colorPrimaryDark = PictureSelectionConfig.uiStyle.picture_statusBarBackgroundColor;
            }
            numComplete = PictureSelectionConfig.uiStyle.picture_switchSelectTotalStyle;

            config.checkNumMode = PictureSelectionConfig.uiStyle.picture_switchSelectNumberStyle;

        } else if (PictureSelectionConfig.style != null) {
            openWhiteStatusBar = PictureSelectionConfig.style.isChangeStatusBarFontColor;
            if (PictureSelectionConfig.style.pictureTitleBarBackgroundColor != 0) {
                colorPrimary = PictureSelectionConfig.style.pictureTitleBarBackgroundColor;
            }
            if (PictureSelectionConfig.style.pictureStatusBarColor != 0) {
                colorPrimaryDark = PictureSelectionConfig.style.pictureStatusBarColor;
            }
            numComplete = PictureSelectionConfig.style.isOpenCompletedNumStyle;
            config.checkNumMode = PictureSelectionConfig.style.isOpenCheckNumStyle;
        } else {
            openWhiteStatusBar = config.isChangeStatusBarFontColor;
            if (!openWhiteStatusBar) {
                openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(getContext(), R.attr.picture_statusFontColor);
            }

            numComplete = config.isOpenStyleNumComplete;
            if (!numComplete) {
                numComplete = AttrsUtils.getTypeValueBoolean(getContext(), R.attr.picture_style_numComplete);
            }

            config.checkNumMode = config.isOpenStyleCheckNumMode;
            if (!config.checkNumMode) {
                config.checkNumMode = AttrsUtils.getTypeValueBoolean(getContext(), R.attr.picture_style_checkNumMode);
            }

            if (config.titleBarBackgroundColor != 0) {
                colorPrimary = config.titleBarBackgroundColor;
            } else {
                colorPrimary = AttrsUtils.getTypeValueColor(getContext(), R.attr.colorPrimary);
            }

            if (config.pictureStatusBarColor != 0) {
                colorPrimaryDark = config.pictureStatusBarColor;
            } else {
                colorPrimaryDark = AttrsUtils.getTypeValueColor(getContext(), R.attr.colorPrimaryDark);
            }
        }

        if (config.openClickSound) {
            VoiceUtils.getInstance().init(getContext());
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        isOnSaveInstanceState = true;
        outState.putParcelable(PictureConfig.EXTRA_CONFIG, config);
    }

    /**
     * loading dialog
     */
    protected void showPleaseDialog() {
        try {
            if (!isHidden()) {
                if (mLoadingDialog == null) {
                    mLoadingDialog = new PictureLoadingDialog(getContext());
                }
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
                mLoadingDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * dismiss dialog
     */
    protected void dismissDialog() {
        if (!isHidden()) {
            try {
                if (mLoadingDialog != null
                        && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            } catch (Exception e) {
                mLoadingDialog = null;
                e.printStackTrace();
            }
        }
    }


    /**
     * compressImage
     */
    protected void compressImage(final List<LocalMedia> result) {
        showPleaseDialog();
        compressToLuban(result);
    }

    /**
     * compress
     *
     * @param result
     */
    private void compressToLuban(List<LocalMedia> result) {
        if (config.synOrAsy) {
            PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<List<File>>() {

                @Override
                public List<File> doInBackground() throws Exception {
                    return Luban.with(getContext())
                            .loadMediaData(result)
                            .isCamera(config.camera)
                            .setTargetDir(config.compressSavePath)
                            .setCompressQuality(config.compressQuality)
                            .setFocusAlpha(config.focusAlpha)
                            .setNewCompressFileName(config.renameCompressFileName)
                            .ignoreBy(config.minimumCompressSize).get();
                }

                @Override
                public void onSuccess(List<File> files) {
                    if (files != null && files.size() > 0 && files.size() == result.size()) {
                        handleCompressCallBack(result, files);
                    } else {
                        onResult(result);
                    }
                }
            });
        } else {
            Luban.with(getContext())
                    .loadMediaData(result)
                    .ignoreBy(config.minimumCompressSize)
                    .isCamera(config.camera)
                    .setCompressQuality(config.compressQuality)
                    .setTargetDir(config.compressSavePath)
                    .setFocusAlpha(config.focusAlpha)
                    .setNewCompressFileName(config.renameCompressFileName)
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onSuccess(List<LocalMedia> list) {
                            onResult(list);
                        }

                        @Override
                        public void onError(Throwable e) {
                            onResult(result);
                        }
                    }).launch();
        }
    }

    /**
     * handleCompressCallBack
     *
     * @param images
     * @param files
     */
    private void handleCompressCallBack(List<LocalMedia> images, List<File> files) {
        if (images == null || files == null) {
            exit();
            return;
        }
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        int size = images.size();
        if (files.size() == size) {
            for (int i = 0, j = size; i < j; i++) {
                File file = files.get(i);
                if (file == null) {
                    continue;
                }
                String path = file.getAbsolutePath();
                LocalMedia image = images.get(i);
                boolean http = PictureMimeType.isHasHttp(path);
                boolean flag = !TextUtils.isEmpty(path) && http;
                boolean isHasVideo = PictureMimeType.isHasVideo(image.getMimeType());
                image.setCompressed(!isHasVideo && !flag);
                image.setCompressPath(isHasVideo || flag ? null : path);
                if (isAndroidQ) {
                    image.setAndroidQToPath(image.getCompressPath());
                }
            }
        }
        onResult(images);
    }


    /**
     * compress or callback
     *
     * @param result
     */
    protected void handlerResult(List<LocalMedia> result) {
        if (config.isCompress && !config.isCheckOriginalImage) {
            compressImage(result);
        } else {
            onResult(result);
        }
    }


    /**
     * If you don't have any albums, first create a camera film folder to come out
     *
     * @param folders
     */
    protected void createNewFolder(List<LocalMediaFolder> folders) {
        if (folders.size() == 0) {
            // ???????????? ?????????????????????????????????
            LocalMediaFolder newFolder = new LocalMediaFolder();
            String folderName = config.chooseMode == PictureMimeType.ofAudio() ?
                    getString(R.string.picture_all_audio) : getString(R.string.picture_camera_roll);
            newFolder.setName(folderName);
            newFolder.setFirstImagePath("");
            newFolder.setCameraFolder(true);
            newFolder.setBucketId(-1);
            newFolder.setChecked(true);
            folders.add(newFolder);
        }
    }

    /**
     * Insert the image into the camera folder
     *
     * @param path
     * @param imageFolders
     * @return
     */
    protected LocalMediaFolder getImageFolder(String path, String realPath, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(PictureMimeType.isContent(path) ? realPath : path);
        File folderFile = imageFile.getParentFile();
        for (LocalMediaFolder folder : imageFolders) {
            if (folderFile != null && folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile != null ? folderFile.getName() : "");
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * return image result
     *  ????????????????????????????????????
     * @param images
     */
    protected void onResult(List<LocalMedia> images) {
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        if (isAndroidQ && config.isAndroidQTransform) {
            showPleaseDialog();
            onResultToAndroidAsy(images);
        } else {
            dismissDialog();
            if (config.camera
                    && config.selectionMode == PictureConfig.MULTIPLE) {
                images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
            }
            if (config.isCheckOriginalImage) {
                int size = images.size();
                for (int i = 0; i < size; i++) {
                    LocalMedia media = images.get(i);
                    media.setOriginal(true);
                    media.setOriginalPath(media.getPath());
                }
            }
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onResult(images);
            } else {
                Intent intent = PictureSelector.putIntentResult(images);
                requireActivity().setResult(Activity.RESULT_OK, intent);
            }
            exit();
        }
    }

    /**
     * Android Q
     *
     * @param images
     */
    private void onResultToAndroidAsy(List<LocalMedia> images) {
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<List<LocalMedia>>() {
            @Override
            public List<LocalMedia> doInBackground() {
                int size = images.size();
                for (int i = 0; i < size; i++) {
                    LocalMedia media = images.get(i);
                    if (media == null || TextUtils.isEmpty(media.getPath())) {
                        continue;
                    }
                    boolean isCopyAndroidQToPath = !media.isCut()
                            && !media.isCompressed()
                            && TextUtils.isEmpty(media.getAndroidQToPath());
                    if (isCopyAndroidQToPath && PictureMimeType.isContent(media.getPath())) {
                        if (!PictureMimeType.isHasHttp(media.getPath())) {
                            String AndroidQToPath = AndroidQTransformUtils.copyPathToAndroidQ(getContext(),
                                    media.getPath(), media.getWidth(), media.getHeight(), media.getMimeType(), config.cameraFileName);
                            media.setAndroidQToPath(AndroidQToPath);
                        }
                    } else if (media.isCut() && media.isCompressed()) {
                        media.setAndroidQToPath(media.getCompressPath());
                    }
                    if (config.isCheckOriginalImage) {
                        media.setOriginal(true);
                        media.setOriginalPath(media.getAndroidQToPath());
                    }
                }
                return images;
            }

            @Override
            public void onSuccess(List<LocalMedia> images) {
                dismissDialog();
                if (images != null) {
                    if (config.camera
                            && config.selectionMode == PictureConfig.MULTIPLE) {
                        images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
                    }
                    if (PictureSelectionConfig.listener != null) {
                        PictureSelectionConfig.listener.onResult(images);
                    } else {
                        Intent intent = PictureSelector.putIntentResult(images);
                        requireActivity().setResult(Activity.RESULT_OK, intent);
                    }
                    exit();
                }
            }
        });
    }

    /**
     * Close Activity
     */
    protected void exit() {
        requireActivity().finish();
        if (config.camera) {
            requireActivity().overridePendingTransition(0, R.anim.picture_anim_fade_out);
            if (getContext() instanceof PictureSelectorCameraEmptyActivity
                    || getContext() instanceof PictureCustomCameraActivity) {
                releaseResultListener();
            }
        } else {
            requireActivity().overridePendingTransition(0,
                    PictureSelectionConfig.windowAnimationStyle.activityExitAnimation);
            if (getContext() instanceof PictureSelectorActivity) {
                releaseResultListener();
                if (config.openClickSound) {
                    VoiceUtils.getInstance().releaseSoundPool();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        super.onDestroyView();
    }


    /**
     * get audio path
     *
     * @param data
     */
    protected String getAudioPath(Intent data) {
        if (data != null && config.chooseMode == PictureMimeType.ofAudio()) {
            try {
                Uri uri = data.getData();
                if (uri != null) {
                    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? uri.getPath() : MediaUtils.getAudioFilePathFromUri(getContext(), uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * Release listener
     */
    private void releaseResultListener() {
        if (config != null) {
            PictureSelectionConfig.destroy();
            LocalMediaPageLoader.setInstanceNull();
            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                }
            } else {
                ToastUtils.s(getContext(), getString(R.string.picture_audio));
            }
        }
    }

    /**
     * showPermissionsDialog
     *
     * @param isCamera
     * @param errorMsg
     */
    protected void showPermissionsDialog(boolean isCamera, String errorMsg) {

    }

    /**
     * Dialog
     *
     * @param content
     */
    protected void showPromptDialog(String content) {
        if (!isHidden()) {
            PictureCustomDialog dialog = new PictureCustomDialog(getContext(), R.layout.picture_prompt_dialog);
            TextView btnOk = dialog.findViewById(R.id.btnOk);
            TextView tvContent = dialog.findViewById(R.id.tv_content);
            tvContent.setText(content);
            btnOk.setOnClickListener(v -> {
                if (!isHidden()) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }


    /**
     * sort
     *
     * @param imageFolders
     */
    protected void sortFolder(List<LocalMediaFolder> imageFolders) {
        Collections.sort(imageFolders, (lhs, rhs) -> {
            if (lhs.getData() == null || rhs.getData() == null) {
                return 0;
            }
            int lSize = lhs.getImageNum();
            int rSize = rhs.getImageNum();
            return Integer.compare(rSize, lSize);
        });
    }
}
