package com.luck.picture.lib.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.luck.picture.lib.R
import com.luck.picture.lib.adapter.PictureImageMDBottomAdapter
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.event.HideBottom
import com.luck.picture.lib.listener.OnMDBottomPhotoSelectChangedListener
import kotlinx.android.synthetic.main.picture_md_bottom_bar.view.*
import org.greenrobot.eventbus.EventBus

/**
 *Created by wanghai
 *Date : 2021/5/18
 *Describe :
 */
class PictureMdBottomBarView(context: Context, attr: AttributeSet?) : RelativeLayout(context, attr) {
    private val mContext = context
    private val mView: View = LayoutInflater.from(context).inflate(R.layout.picture_md_bottom_bar, this, true)
    private var mdBottomAdapter: PictureImageMDBottomAdapter? = null
    private lateinit var mTvPictureOk: TextView
    private lateinit var mTvPictureImgNum: TextView
    private lateinit var mBottomLayout: RelativeLayout
    private var config: PictureSelectionConfig? = null
    private var mAnimation: Animation? = null
    private var numComplete = false
    private var isStartAnimation = false
    var onPictureListener: OnPictureListener? = null


    private var onItemDragListener: OnItemDragListener = object : OnItemDragListener {
        override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
        override fun onItemDragMoving(source: RecyclerView.ViewHolder, from: Int, target: RecyclerView.ViewHolder, to: Int) {}
        override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {
            onPictureListener?.onItemDragEnd(mdBottomAdapter?.data ?: arrayListOf())
        }
    }

    init {
        mTvPictureOk = mView.findViewById(R.id.picture_tv_ok)
        mTvPictureImgNum = mView.findViewById(R.id.tv_media_num)
        mBottomLayout = mView.findViewById(R.id.select_bar_layout)
    }

    fun initView(config: PictureSelectionConfig, numComplete: Boolean = false, isStartAnimation: Boolean = false) {
        this.numComplete = numComplete
        this.isStartAnimation = isStartAnimation
        this.config = config
        mView.run {
            if (config.chooseMode == PictureMimeType.ofVideo()) {
                mView.findViewById<TextView>(R.id.bottom_desc).visibility = View.GONE
            }

            val dividerItemDecoration = DividerItemDecoration(mContext, RecyclerView.HORIZONTAL)
            dividerItemDecoration.setDrawable(ContextCompat.getDrawable(mContext, R.drawable.item_10)!!)
            recyclerView.addItemDecoration(dividerItemDecoration)
            recyclerView.layoutManager = LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)
            mdBottomAdapter = PictureImageMDBottomAdapter(context = mContext, config = config, onMDBottomPhotoSelectChangedListener = OnMDBottomPhotoSelectChangedListener { item ->
                item?.let {
                    // ?????? item ???????????????
                    changeImageNumber(mdBottomAdapter?.data?: arrayListOf())
                    onPictureListener?.onItemRemove(it)
                }
            })
            val itemDragAndSwipeCallback = ItemDragAndSwipeCallback(mdBottomAdapter)
            val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)

            mdBottomAdapter?.enableDragItem(itemTouchHelper, R.id.item, true)
            mdBottomAdapter?.setOnItemDragListener(onItemDragListener)
            recyclerView.adapter = mdBottomAdapter

            isNumComplete(numComplete)
            if (!numComplete) {
                mAnimation = AnimationUtils.loadAnimation(context, R.anim.picture_anim_modal_in)
            }
            initListener()
        }
    }

    private fun initListener() {
        mView.run {
            ll_next.setOnClickListener {
                onPictureListener?.onButtonComplete()
            }
        }
    }

    fun dataAdd(data: LocalMedia) {
        if (mdBottomAdapter == null) return
        mdBottomAdapter?.addData(data)
        changeImageNumber(mdBottomAdapter!!.data)
    }


    fun dataChanged(selectData: List<LocalMedia>, check: Boolean, item: LocalMedia) {
        if (!check) {
            var removeIndex = -1
            mdBottomAdapter?.data?.forEachIndexed { index, media ->
                if (media.path == item.path || media.id == item.id) {
                    removeIndex = index
                    return@forEachIndexed
                }
            }
            if (removeIndex != -1) {
                mdBottomAdapter?.remove(removeIndex)
            }
        } else {
            mdBottomAdapter?.addData(item)
        }
        changeImageNumber(mdBottomAdapter?.data ?: arrayListOf())
    }

    fun dataChanged(selectData: List<LocalMedia>) {
        if (selectData.size>1){
            val newSelectData = arrayListOf<LocalMedia>()
            selectData.forEach {
                newSelectData.add(it)
            }
            mdBottomAdapter?.setNewData(newSelectData)
            changeImageNumber(newSelectData)
        }else {
            val newSelectData = arrayListOf<LocalMedia>()
            selectData.forEach {
                newSelectData.add(it)
            }
            mdBottomAdapter?.setNewData(newSelectData)
            changeImageNumber(newSelectData)
        }
    }

    /**
     * none number style
     */
    private fun isNumComplete(numComplete: Boolean) {
        if (numComplete) {
            initCompleteText(0)
        }
    }

    /**
     * change image selector state
     *
     * @param selectData
     */
    private fun changeImageNumber(selectData: List<LocalMedia?>) {
        mView.run {
            val enable = selectData.isNotEmpty()
            //??????HideBottom
            EventBus.getDefault().post(HideBottom(enable))
            mBottomLayout.visibility = if (enable) VISIBLE else GONE
            if (enable) {
                mTvPictureOk.isEnabled = true
                mTvPictureOk.isSelected = true
                if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.pictureCompleteTextColor != 0) {
                        mTvPictureOk.setTextColor(PictureSelectionConfig.style.pictureCompleteTextColor)
                    }
                }
                if (numComplete) {
                    initCompleteText(selectData.size)
                } else {
                    if (!isStartAnimation) {
                        mTvPictureImgNum.startAnimation(mAnimation)
                    }
                    mTvPictureImgNum.visibility = VISIBLE
                    when (config?.chooseMode) {
                        PictureConfig.TYPE_ALL, PictureConfig.TYPE_IMAGE -> mTvPictureImgNum.text = "(" + (selectData.size+(config?.selectedNum?:0)) + "/" + (config?.maxSelectNum?:0) +")"
                        PictureConfig.TYPE_VIDEO -> mTvPictureImgNum.text = "(" + selectData.size + "/" + config?.maxVideoSelectNum + ")"
                        else -> {
                        }
                    }
                    if (PictureSelectionConfig.uiStyle != null) {
                        if (PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText != 0) {
                            mTvPictureOk.text = mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText)
                        }
                    } else if (PictureSelectionConfig.style != null) {
                        if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureCompleteText)) {
                            mTvPictureOk.text = PictureSelectionConfig.style.pictureCompleteText
                        }
                    } else {
                        mTvPictureOk.text = mContext.getString(R.string.picture_please_next)
                    }
                    isStartAnimation = false
                }
            } else {
                mTvPictureOk.isEnabled = config?.returnEmpty ?: false
                mTvPictureOk.isSelected = false
                if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.pictureUnCompleteTextColor != 0) {
                        mTvPictureOk.setTextColor(PictureSelectionConfig.style.pictureUnCompleteTextColor)
                    }
                }
                if (numComplete) {
                    initCompleteText(selectData.size)
                } else {
                    mTvPictureImgNum.visibility = INVISIBLE
                    if (PictureSelectionConfig.uiStyle != null) {
                        if (PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText != 0) {
                            mTvPictureOk.text = mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText)
                        }
                    } else if (PictureSelectionConfig.style != null) {
                        if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureUnCompleteText)) {
                            mTvPictureOk.text = PictureSelectionConfig.style.pictureUnCompleteText
                        }
                    } else {
                        mTvPictureOk.text = mContext.getString(R.string.picture_please_next)
                    }
                }
            }
        }
    }

    /**
     * init Text
     */
    private fun initCompleteText(startCount: Int) {
        if (config?.selectionMode == PictureConfig.SINGLE) {
            if (startCount <= 0) {
                if (PictureSelectionConfig.uiStyle != null) {
                    if (PictureSelectionConfig.uiStyle.isCompleteReplaceNum) {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText != 0) String.format(mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText), startCount, 1) else mContext.getString(R.string.picture_please_next)
                    } else {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText != 0) mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText) else mContext.getString(R.string.picture_please_next)
                    }
                } else if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.isCompleteReplaceNum && !TextUtils.isEmpty(PictureSelectionConfig.style.pictureUnCompleteText)) {
                        mTvPictureOk.text = String.format(PictureSelectionConfig.style.pictureUnCompleteText, startCount, 1)
                    } else {
                        mTvPictureOk.text = if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureUnCompleteText)) PictureSelectionConfig.style.pictureUnCompleteText else mContext.getString(R.string.picture_done)
                    }
                }
            } else {
                if (PictureSelectionConfig.uiStyle != null) {
                    if (PictureSelectionConfig.uiStyle.isCompleteReplaceNum) {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText != 0) String.format(mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText), startCount, 1) else mContext.getString(R.string.picture_done)
                    } else {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText != 0) mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText) else mContext.getString(R.string.picture_done)
                    }
                } else if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.isCompleteReplaceNum && !TextUtils.isEmpty(PictureSelectionConfig.style.pictureCompleteText)) {
                        mTvPictureOk.text = String.format(PictureSelectionConfig.style.pictureCompleteText, startCount, 1)
                    } else {
                        mTvPictureOk.text = if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureCompleteText)) PictureSelectionConfig.style.pictureCompleteText else mContext.getString(R.string.picture_done)
                    }
                }
            }
        } else {
            if (startCount <= 0) {
                if (PictureSelectionConfig.uiStyle != null) {
                    if (PictureSelectionConfig.uiStyle.isCompleteReplaceNum) {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText != 0) String.format(mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText), startCount, config?.maxSelectNum) else mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                    } else {
                        mTvPictureOk.text = if (PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText != 0) mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeDefaultText) else mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                    }
                } else if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.isCompleteReplaceNum) {
                        mTvPictureOk.text = if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureUnCompleteText)) String.format(PictureSelectionConfig.style.pictureUnCompleteText, startCount, config?.maxSelectNum) else mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                    } else {
                        mTvPictureOk.text = if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureUnCompleteText)) PictureSelectionConfig.style.pictureUnCompleteText else mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                    }
                }
            } else {
                if (PictureSelectionConfig.uiStyle != null) {
                    if (PictureSelectionConfig.uiStyle.isCompleteReplaceNum) {
                        if (PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText != 0) {
                            mTvPictureOk.text = String.format(mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText), startCount, config?.maxSelectNum)
                        } else {
                            mTvPictureOk.text = mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                        }
                    } else {
                        if (PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText != 0) {
                            mTvPictureOk.text = mContext.getString(PictureSelectionConfig.uiStyle.picture_bottom_completeNormalText)
                        } else {
                            mTvPictureOk.text = mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                        }
                    }
                } else if (PictureSelectionConfig.style != null) {
                    if (PictureSelectionConfig.style.isCompleteReplaceNum) {
                        if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureCompleteText)) {
                            mTvPictureOk.text = String.format(PictureSelectionConfig.style.pictureCompleteText, startCount, config?.maxSelectNum)
                        } else {
                            mTvPictureOk.text = mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                        }
                    } else {
                        if (!TextUtils.isEmpty(PictureSelectionConfig.style.pictureCompleteText)) {
                            mTvPictureOk.text = PictureSelectionConfig.style.pictureCompleteText
                        } else {
                            mTvPictureOk.text = mContext.getString(R.string.picture_done_front_num, startCount, config?.maxSelectNum)
                        }
                    }
                }
            }
        }
    }

    fun getOkTextView(): TextView {
        return mTvPictureOk
    }

    fun getNumberTextView(): TextView {
        return mTvPictureImgNum
    }

    fun getBottomLayout(): RelativeLayout {
        return mBottomLayout
    }

    override fun onDetachedFromWindow() {
        mAnimation?.cancel()
        mAnimation = null
        super.onDetachedFromWindow()
    }

    fun updateIsStartAnimation(isStartAnimation: Boolean) {
        this.isStartAnimation = isStartAnimation
    }

    fun getData(): List<LocalMedia> {
        return mdBottomAdapter?.data ?: listOf()
    }

    interface OnPictureListener {
        fun onItemDragEnd(data: List<LocalMedia>)
        fun onButtonComplete()
        fun onItemRemove(item: LocalMedia)
    }
}