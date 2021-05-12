package com.luck.picture.lib.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.dialog.PictureCustomDialog;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.DateUtils;
import com.luck.picture.lib.tools.MediaUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * @author：xp
 * @date：20121-5-12 12:02
 * @describe：PictureImageMDBottomAdapter
 */
public class PictureImageMDBottomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<LocalMedia> data = new ArrayList<>();
    private final PictureSelectionConfig config;

    public PictureImageMDBottomAdapter(Context context, PictureSelectionConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * 全量刷新
     *
     * @param data
     */
    public void bindData(List<LocalMedia> data) {
        this.data = data == null ? new ArrayList<>() : data;
        this.notifyDataSetChanged();
    }

    public List<LocalMedia> getData() {
        return data == null ? new ArrayList<>() : data;
    }

    public boolean isDataEmpty() {
        return data == null || data.size() == 0;
    }

    public void clear() {
        if (getSize() > 0) {
            data.clear();
        }
    }

    public int getSize() {
        return data == null ? 0 : data.size();
    }

    public LocalMedia getItem(int position) {
        return getSize() > 0 ? data.get(position) : null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.picture_md_image_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder holder, final int position) {

        final ViewHolder contentHolder = (ViewHolder) holder;
        final LocalMedia image = data.get(position);
        image.position = contentHolder.getAbsoluteAdapterPosition();
        final String path = image.getPath();
        final String mimeType = image.getMimeType();

        if (config.isSingleDirectReturn) {
            contentHolder.btnCheck.setVisibility(View.GONE);
        } else {
            contentHolder.btnCheck.setVisibility(View.VISIBLE);
        }
        contentHolder.tvIsGif.setVisibility(PictureMimeType.isGif(mimeType) ? View.VISIBLE : View.GONE);
        if (PictureMimeType.isHasImage(image.getMimeType())) {
            if (image.loadLongImageStatus == PictureConfig.NORMAL) {
                image.isLongImage = MediaUtils.isLongImg(image);
                image.loadLongImageStatus = PictureConfig.LOADED;
            }
            contentHolder.tvLongChart.setVisibility(image.isLongImage ? View.VISIBLE : View.GONE);
        } else {
            image.loadLongImageStatus = PictureConfig.NORMAL;
            contentHolder.tvLongChart.setVisibility(View.GONE);
        }
        boolean isHasVideo = PictureMimeType.isHasVideo(mimeType);
        if (isHasVideo || PictureMimeType.isHasAudio(mimeType)) {
            contentHolder.tvDuration.setVisibility(View.VISIBLE);
            contentHolder.tvDuration.setText(DateUtils.formatDurationTime(image.getDuration()));
            if (PictureSelectionConfig.uiStyle != null) {
                if (isHasVideo) {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable != 0) {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                (PictureSelectionConfig.uiStyle.picture_adapter_item_video_textLeftDrawable,
                                        0, 0, 0);
                    } else {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                (R.drawable.picture_icon_video, 0, 0, 0);
                    }
                } else {
                    if (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable != 0) {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                (PictureSelectionConfig.uiStyle.picture_adapter_item_audio_textLeftDrawable,
                                        0, 0, 0);
                    } else {
                        contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                                (R.drawable.picture_icon_audio, 0, 0, 0);
                    }
                }
            } else {
                contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                        (isHasVideo ? R.drawable.picture_icon_video : R.drawable.picture_icon_audio,
                                0, 0, 0);
            }
        } else {
            contentHolder.tvDuration.setVisibility(View.GONE);
        }
        if (config.chooseMode == PictureMimeType.ofAudio()) {
            contentHolder.ivPicture.setImageResource(R.drawable.picture_audio_placeholder);
        } else {
            if (PictureSelectionConfig.imageEngine != null) {
                PictureSelectionConfig.imageEngine.loadBottomGridImage(context, path, contentHolder.ivPicture);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPicture;
        TextView btnClose;
        TextView tvDuration, tvIsGif, tvLongChart;
        View contentView;
        View btnCheck;

        public ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            ivPicture = itemView.findViewById(R.id.ivPicture);
            btnClose = itemView.findViewById(R.id.btnClose);
            btnCheck = itemView.findViewById(R.id.btnCheck);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvIsGif = itemView.findViewById(R.id.tv_isGif);
            tvLongChart = itemView.findViewById(R.id.tv_long_chart);
            if (PictureSelectionConfig.uiStyle != null) {
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textSize > 0) {
                    tvDuration.setTextSize(PictureSelectionConfig.uiStyle.picture_adapter_item_textSize);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_textColor != 0) {
                    tvDuration.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_textColor);
                }

                if (PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text != 0) {
                    tvIsGif.setText(itemView.getContext().getString(PictureSelectionConfig.uiStyle.picture_adapter_item_tag_text));
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_show) {
                    tvIsGif.setVisibility(View.VISIBLE);
                } else {
                    tvIsGif.setVisibility(View.GONE);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background != 0) {
                    tvIsGif.setBackgroundResource(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_background);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor != 0) {
                    tvIsGif.setTextColor(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textColor);
                }
                if (PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize != 0) {
                    tvIsGif.setTextSize(PictureSelectionConfig.uiStyle.picture_adapter_item_gif_tag_textSize);
                }
            }
        }
    }

    /**
     * Tips
     */
    private void showPromptDialog(String content) {
        PictureCustomDialog dialog = new PictureCustomDialog(context, R.layout.picture_prompt_dialog);
        TextView btnOk = dialog.findViewById(R.id.btnOk);
        TextView tvContent = dialog.findViewById(R.id.tv_content);
        tvContent.setText(content);
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

}
