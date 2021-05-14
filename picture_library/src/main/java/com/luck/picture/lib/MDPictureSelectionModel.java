package com.luck.picture.lib;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;

import com.luck.picture.lib.animators.AnimationType;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.engine.CacheResourcesEngine;
import com.luck.picture.lib.engine.ImageEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnCustomCameraInterfaceListener;
import com.luck.picture.lib.listener.OnCustomImagePreviewCallback;
import com.luck.picture.lib.listener.OnVideoSelectedPlayCallback;
import com.luck.picture.lib.style.PictureCropParameterStyle;
import com.luck.picture.lib.style.PictureParameterStyle;
import com.luck.picture.lib.style.PictureSelectorUIStyle;
import com.luck.picture.lib.style.PictureWindowAnimationStyle;
import com.luck.picture.lib.tools.DoubleUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.yalantis.ucrop.UCrop;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * @author：luck
 * @date：2017-5-24 21:30
 * @describe：PictureSelectionModel
 */

public class MDPictureSelectionModel {
    private final PictureSelector selector;
    public List<LocalMedia> selectionMedias = new ArrayList<>();
    public MDPictureSelectionModel(PictureSelector selector) {
        this.selector = selector;
    }

    /**
     * @param engine Image Load the engine
     * @return
     */
    public MDPictureSelectionModel imageEngine(ImageEngine engine) {
        if (PictureSelectionConfig.imageEngine != engine) {
            PictureSelectionConfig.imageEngine = engine;
        }
        return this;
    }
    /**
     * @param selectionData Select the selected picture set
     * @return Use {link .selectionData()}
     */
    public MDPictureSelectionModel selectionData(List<LocalMedia> selectionData) {
        selectionMedias = selectionData;
        return this;
    }
    /**
     * @param callback Provide video playback control，Users are free to customize the video display interface
     * @return
     */
    public MDPictureSelectionModel bindCustomPlayVideoCallback(OnVideoSelectedPlayCallback callback) {
        PictureSelectionConfig.customVideoPlayCallback = new WeakReference<>(callback).get();
        return this;
    }

    /**
     * @param callback Custom preview callback function
     * @return
     */
    public MDPictureSelectionModel bindCustomPreviewCallback(OnCustomImagePreviewCallback callback) {
        PictureSelectionConfig.onCustomImagePreviewCallback = new WeakReference<>(callback).get();
        return this;
    }

    /**
     * # The developer provides an additional callback interface to the user where the user can perform some custom actions
     * {link 如果是自定义相机则必须使用.startActivityForResult(this,PictureConfig.REQUEST_CAMERA);方式启动否则PictureSelector处理不了相机后的回调}
     *
     * @param listener
     * @return Use ${bindCustomCameraInterfaceListener}
     */
    @Deprecated
    public MDPictureSelectionModel bindPictureSelectorInterfaceListener(OnCustomCameraInterfaceListener listener) {
        PictureSelectionConfig.onCustomCameraInterfaceListener = new WeakReference<>(listener).get();
        return this;
    }

    /**
     * # The developer provides an additional callback interface to the user where the user can perform some custom actions
     * {link 如果是自定义相机则必须使用.startActivityForResult(this,PictureConfig.REQUEST_CAMERA);方式启动否则PictureSelector处理不了相机后的回调}
     *
     * @param listener
     * @return
     */
    public MDPictureSelectionModel bindCustomCameraInterfaceListener(OnCustomCameraInterfaceListener listener) {
        PictureSelectionConfig.onCustomCameraInterfaceListener = new WeakReference<>(listener).get();
        return this;
    }

    /**
     * 动态设置裁剪主题样式
     *
     * @param style 裁剪页主题
     *              <p>{@link PictureSelectorUIStyle}</>
     * @return
     */
    @Deprecated
    public MDPictureSelectionModel setPictureCropStyle(PictureCropParameterStyle style) {
        if (style != null) {
            PictureSelectionConfig.cropStyle = style;
        } else {
            PictureSelectionConfig.cropStyle = PictureCropParameterStyle.ofDefaultCropStyle();
        }
        return this;
    }

    /**
     * Dynamically set the album to start and exit the animation
     *
     * @return
     */
    public MDPictureSelectionModel setPictureWindowAnimationStyle(PictureWindowAnimationStyle windowAnimationStyle) {
        if (windowAnimationStyle != null) {
            PictureSelectionConfig.windowAnimationStyle = windowAnimationStyle;
        } else {
            PictureSelectionConfig.windowAnimationStyle = PictureWindowAnimationStyle.ofDefaultWindowAnimationStyle();
        }
        return this;
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    public void forResult(int requestCode) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Activity activity = selector.getActivity();
            if (activity == null ) {
                return;
            }
            Intent intent = new Intent(activity, MDPhotoFragmentActivity.class);
            intent.putParcelableArrayListExtra("selectionMedias", (ArrayList<? extends Parcelable>) selectionMedias);
            Fragment fragment = selector.getFragment();
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
            PictureWindowAnimationStyle windowAnimationStyle = PictureSelectionConfig.windowAnimationStyle;
            activity.overridePendingTransition(windowAnimationStyle.activityEnterAnimation, R.anim.picture_anim_fade_in);
        }
    }

    /**
     * # replace for setPictureWindowAnimationStyle();
     * Start to select media and wait for result.
     * <p>
     * # Use PictureWindowAnimationStyle to achieve animation effects
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    @Deprecated
    public void forResult(int requestCode, int enterAnim, int exitAnim) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Activity activity = selector.getActivity();
            if (activity == null) {
                return;
            }
            Intent intent = new Intent(activity, MDPhotoFragmentActivity.class);
            intent.putParcelableArrayListExtra("selectionMedias", (ArrayList<? extends Parcelable>) selectionMedias);
            Fragment fragment = selector.getFragment();
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
            activity.overridePendingTransition(enterAnim, exitAnim);
        }
    }


    /**
     * 提供外部预览图片方法
     *
     * @param position
     * @param medias
     */
    public void openExternalPreview(int position, List<LocalMedia> medias) {
        if (selector != null) {
            selector.externalPicturePreview(position, medias, PictureSelectionConfig.windowAnimationStyle.activityPreviewEnterAnimation);
        } else {
            throw new NullPointerException("This PictureSelector is Null");
        }
    }


    /**
     * 提供外部预览图片方法-带自定义下载保存路径
     * # 废弃 由于Android Q沙盒机制 此方法不在需要了
     *
     * @param position
     * @param medias
     */
    @Deprecated
    public void openExternalPreview(int position, String directory_path, List<LocalMedia> medias) {
        if (selector != null) {
            selector.externalPicturePreview(position, directory_path, medias,
                    PictureSelectionConfig.windowAnimationStyle.activityPreviewEnterAnimation);
        } else {
            throw new NullPointerException("This PictureSelector is Null");
        }
    }

    /**
     * set preview video
     *
     * @param path
     */
    public void externalPictureVideo(String path) {
        if (selector != null) {
            selector.externalPictureVideo(path);
        } else {
            throw new NullPointerException("This PictureSelector is Null");
        }
    }
}
