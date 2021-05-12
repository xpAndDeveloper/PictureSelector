package com.luck.picture.lib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
                                  , var data: MutableList<LocalMedia> = arrayListOf()
                                  , private val config: PictureSelectionConfig
                                  , private val onMDBottomPhotoSelectChangedListener: OnMDBottomPhotoSelectChangedListener<LocalMedia>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val isDataEmpty: Boolean
        get() = data.size == 0

    fun clear() {
        if (size > 0) {
            data.clear()
        }
    }

    val size: Int
        get() = data.size

    fun getItem(position: Int): LocalMedia? {
        return if (size > 0) data[position] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_md_image_grid_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val contentHolder = holder as ViewHolder
        val image = data[position]
        image.position = contentHolder.absoluteAdapterPosition
        val path = image.path
        val mimeType = image.mimeType
        if (config.isSingleDirectReturn) {
            contentHolder.btnCheck.visibility = View.GONE
        } else {
            contentHolder.btnCheck.visibility = View.VISIBLE
        }
        contentHolder.tvIsGif.visibility = if (PictureMimeType.isGif(mimeType)) View.VISIBLE else View.GONE
        if (PictureMimeType.isHasImage(image.mimeType)) {
            if (image.loadLongImageStatus == PictureConfig.NORMAL) {
                image.isLongImage = MediaUtils.isLongImg(image)
                image.loadLongImageStatus = PictureConfig.LOADED
            }
            contentHolder.tvLongChart.visibility = if (image.isLongImage) View.VISIBLE else View.GONE
        } else {
            image.loadLongImageStatus = PictureConfig.NORMAL
            contentHolder.tvLongChart.visibility = View.GONE
        }
        val isHasVideo = PictureMimeType.isHasVideo(mimeType)
        if (isHasVideo || PictureMimeType.isHasAudio(mimeType)) {
            contentHolder.tvDuration.visibility = View.VISIBLE
            contentHolder.tvDuration.text = DateUtils.formatDurationTime(image.duration)
            if (PictureSelectionConfig.uiStyle != null) {
                if (isHasVideo) {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable != 0) {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable,
                                0, 0, 0)
                    } else {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.picture_icon_video, 0, 0, 0)
                    }
                } else {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable != 0) {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable,
                                0, 0, 0)
                    } else {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.picture_icon_audio, 0, 0, 0)
                    }
                }
            } else {
                contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(if (isHasVideo) R.drawable.picture_icon_video else R.drawable.picture_icon_audio,
                        0, 0, 0)
            }
        } else {
            contentHolder.tvDuration.visibility = View.GONE
        }
        if (config.chooseMode == PictureMimeType.ofAudio()) {
            contentHolder.ivPicture.setImageResource(R.drawable.picture_audio_placeholder)
        } else {
            if (PictureSelectionConfig.imageEngine != null) {
                PictureSelectionConfig.imageEngine.loadBottomGridImage(context, path, contentHolder.ivPicture)
            }
        }
        holder.rlClose.setOnClickListener {
            val removeItem = data[position]
            data.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
            onMDBottomPhotoSelectChangedListener.remove(removeItem)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var ivPicture: ImageView
        var btnClose: TextView
        var tvDuration: TextView
        var tvIsGif: TextView
        var tvLongChart: TextView
        var btnCheck: View
        var rlClose: RelativeLayout

        init {
            rlClose = contentView.findViewById(R.id.rlClose)
            ivPicture = contentView.findViewById(R.id.ivPicture)
            btnClose = contentView.findViewById(R.id.btnClose)
            btnCheck = contentView.findViewById(R.id.btnCheck)
            tvDuration = contentView.findViewById(R.id.tv_duration)
            tvIsGif = contentView.findViewById(R.id.tv_isGif)
            tvLongChart = contentView.findViewById(R.id.tv_long_chart)
            if (PictureSelectionConfig.uiStyle != null) {
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textSize > 0) {
                    tvDuration.textSize = PictureSelectionConfig.uiStyle.picture_adapter_item_textSize.toFloat()
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textColor != 0) {
                    tvDuration.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_textColor)
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text != 0) {
                    tvIsGif.text = contentView.context.getString(PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text)
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_show) {
                    tvIsGif.visibility = View.VISIBLE
                } else {
                    tvIsGif.visibility = View.GONE
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background != 0) {
                    tvIsGif.setBackgroundResource(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background)
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor != 0) {
                    tvIsGif.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor)
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize != 0) {
                    tvIsGif.textSize = PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize.toFloat()
                }
            }
        }
    }
}