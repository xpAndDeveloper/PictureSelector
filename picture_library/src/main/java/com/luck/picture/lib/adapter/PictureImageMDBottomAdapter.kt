package com.luck.picture.lib.adapter

import android.content.Context
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.luck.picture.lib.R
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnMDBottomPhotoSelectChangedListener
import com.luck.picture.lib.tools.DateUtils
import com.luck.picture.lib.tools.MediaUtils

/**
 * @author：xp
 * @date：20121-5-12 12:02
 * @describe：PictureImageMDBottomAdapter
 */
class PictureImageMDBottomAdapter(private val context: Context
                                  , localMedias: MutableList<LocalMedia> = arrayListOf()
                                  , private val config: PictureSelectionConfig
                                  , private val onMDBottomPhotoSelectChangedListener: OnMDBottomPhotoSelectChangedListener<LocalMedia>)
    : BaseItemDraggableAdapter<LocalMedia, BaseViewHolder>(R.layout.picture_md_bottom_image_grid_item, localMedias) {

    private fun onBindViewHolder(helper: BaseViewHolder, item: LocalMedia) {
        item.position = helper.absoluteAdapterPosition
        val path = item.path
        val mimeType = item.mimeType
        helper.setGone(R.id.btnCheck, !config.isSingleDirectReturn)
        helper.setGone(R.id.tv_isGif, PictureMimeType.isGif(mimeType))

        if (PictureMimeType.isHasImage(item.mimeType)) {
            if (item.loadLongImageStatus == PictureConfig.NORMAL) {
                item.isLongImage = MediaUtils.isLongImg(item)
                item.loadLongImageStatus = PictureConfig.LOADED
            }
            helper.setGone(R.id.tv_long_chart, item.isLongImage)
        } else {
            item.loadLongImageStatus = PictureConfig.NORMAL
            helper.setGone(R.id.tv_long_chart, false)
        }
        val isHasVideo = PictureMimeType.isHasVideo(mimeType)
        if (isHasVideo || PictureMimeType.isHasAudio(mimeType)) {
            helper.setGone(R.id.tv_duration, true)
            helper.setText(R.id.tv_duration, DateUtils.formatDurationTime(item.duration))
            if (PictureSelectionConfig.uiStyle != null) {
                if (isHasVideo) {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable != 0) {
                        helper.getView<TextView>(R.id.tv_duration).setCompoundDrawablesRelativeWithIntrinsicBounds(PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable,
                                0, 0, 0)
                    } else {
                        helper.getView<TextView>(R.id.tv_duration).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.picture_icon_video, 0, 0, 0)
                    }
                } else {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable != 0) {
                        helper.getView<TextView>(R.id.tv_duration).setCompoundDrawablesRelativeWithIntrinsicBounds(PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable,
                                0, 0, 0)
                    } else {
                        helper.getView<TextView>(R.id.tv_duration).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.picture_icon_audio, 0, 0, 0)
                    }
                }
            } else {
                helper.getView<TextView>(R.id.tv_duration).setCompoundDrawablesRelativeWithIntrinsicBounds(if (isHasVideo) R.drawable.picture_icon_video else R.drawable.picture_icon_audio,
                        0, 0, 0)
            }
        } else {
            helper.setGone(R.id.tv_duration, false)
        }
        if (config.chooseMode == PictureMimeType.ofAudio()) {
            helper.setImageResource(R.id.ivPicture, R.drawable.picture_audio_placeholder)
        } else {
            if (PictureSelectionConfig.imageEngine != null) {
                PictureSelectionConfig.imageEngine.loadBottomGridImage(context, path, helper.getView(R.id.ivPicture))
            }
        }
        helper.setOnClickListener(R.id.rlClose) {
            if (helper.absoluteAdapterPosition!=-1) {
                val removeItem = item
                remove(helper.absoluteAdapterPosition)
                onMDBottomPhotoSelectChangedListener.remove(removeItem)
            }
        }
    }

    private fun setLayout(helper: BaseViewHolder) {
        if (PictureSelectionConfig.uiStyle != null) {
            if (PictureSelectionConfig.uiStyle.picture_adapter_item_textSize > 0) {
                helper.getView<TextView>(R.id.tv_duration)
                helper.getView<TextView>(R.id.tv_duration).textSize = PictureSelectionConfig.uiStyle.picture_adapter_item_textSize.toFloat()
            }
            if (PictureSelectionConfig.uiStyle.picture_adapter_item_textColor != 0) {
                helper.getView<TextView>(R.id.tv_duration).setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_textColor)
            }
            if (PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text != 0) {
                helper.setText(R.id.tv_isGif, context.getString(PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text))
            }
            helper.setGone(R.id.tv_isGif, PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_show)

            if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background != 0) {
                helper.setBackgroundRes(R.id.tv_isGif, PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background)
            }
            if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor != 0) {
                helper.setTextColor(R.id.tv_isGif, PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor)
            }
            if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize != 0) {
                helper.getView<TextView>(R.id.tv_isGif).textSize = PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize.toFloat()
            }
        }
    }

    override fun convert(helper: BaseViewHolder, item: LocalMedia) {
        setLayout(helper)
        onBindViewHolder(helper, item)
    }
}