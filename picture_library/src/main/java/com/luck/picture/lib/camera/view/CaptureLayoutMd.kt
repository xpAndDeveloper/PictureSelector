package com.luck.picture.lib.camera.view

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.luck.picture.lib.R
import com.luck.picture.lib.camera.CustomCameraView
import com.luck.picture.lib.camera.MDCustomCameraView
import com.luck.picture.lib.camera.listener.CaptureListener
import com.luck.picture.lib.camera.listener.ClickListener
import com.luck.picture.lib.camera.listener.TypeListener
import com.luck.picture.lib.dialog.PictureCustomDialog
import kotlinx.android.synthetic.main.picture_capture_layout_md.view.*

/**
 *Created by wanghai
 *Date : 2021/5/13
 *Describe :
 */
class CaptureLayoutMd(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var state = 0 //当前按钮状态

    val STATE_IDLE = 0x001 //空闲状态
    val STATE_PRESS = 0x002 //按下状态
    val STATE_LONG_PRESS = 0x003 //长按状态
    val STATE_RECORDERING = 0x004 //录制状态
    val STATE_BAN = 0x005 //禁止状态

    private var recorded_time = 0  //记录当前录制的时间
    private var timer: RecordCountDownTimer? = null  //计时器
    private val duration = 60 * 1000 //录制视频最大时间长度
    private var progress = 0f //录制视频的进度
    private val min_duration = CustomCameraView.DEFAULT_MIN_RECORD_VIDEO  //最短录制时间限制

    private var mCameraType = MDCustomCameraView.BUTTON_STATE_ONLY_CAPTURE

    private val mContext = context
    private val mView: View = LayoutInflater.from(context).inflate(R.layout.picture_capture_layout_md, this, true)

    private var captureListener: CaptureListener? = null //拍照按钮监听
    private var typeListener: TypeListener? = null //拍照或录制后接结果按钮监听
    private val leftClickListener: ClickListener? = null  //左边按钮监听
    private val rightClickListener: ClickListener? = null //右边按钮监听

    private var mIsMaxMedia = false
    private var mIsMaxMediaNumber = 9


    fun setTypeListener(typeListener: TypeListener?) {
        this.typeListener = typeListener
    }

    fun setCaptureListener(captureListener: CaptureListener?) {
        this.captureListener = captureListener
    }

    init {
        state = STATE_IDLE
        initView()
    }

    private fun initView() {
        mView.run {
            flTakePhoto.setOnClickListener {
                if (mIsMaxMedia) {
                    showPromptDialog(mContext.getString(R.string.picture_message_take_max_num, mIsMaxMediaNumber))
                } else {
                    captureListener?.takePictures()
                }
            }
            flVideoRecord.setOnClickListener {
                if (mIsMaxMedia) {
                    showPromptDialog(mContext.getString(R.string.picture_message_video_tak_max_num, mIsMaxMediaNumber))
                } else {
                    if (state == STATE_IDLE) {
                        viewRecordStart.animate().scaleX(0f).scaleY(0f).start()
                        viewRecording.animate().scaleX(1f).scaleY(1f).start()
                        timer = RecordCountDownTimer(duration.toLong(), (duration / 360).toLong()) //录制定时器
                        captureListener?.recordStart()
                        timer?.start()
                        state = STATE_RECORDERING
                    } else if (state == STATE_RECORDERING) {
                        recordEnd()
                    }
                }
            }
            tvSave.setOnClickListener {
                typeListener?.confirm()
            }
        }
    }

    private fun initCameraView(){
        if (mCameraType == MDCustomCameraView.BUTTON_STATE_ONLY_CAPTURE) {
            flTakePhoto.visibility = View.VISIBLE
            flVideoRecord.visibility = View.GONE
            mProgressBar.visibility = View.INVISIBLE
        } else {
            flTakePhoto.visibility = View.GONE
            flVideoRecord.visibility = View.VISIBLE
            mProgressBar.visibility = View.VISIBLE
        }
    }

    fun setCameraType(type: Int){
        mCameraType = type
        initCameraView()
        typeListener?.cancel()
    }


    fun setButtonCaptureEnabled(enabled: Boolean) {
        mView.flTakePhoto.isEnabled = enabled
    }

    // 拍照,视频完成保存后
    fun startTypeBtnAnimator() {
        if (mCameraType == MDCustomCameraView.BUTTON_STATE_ONLY_CAPTURE) {
            typeListener?.confirm()
        } else {
            tvSave.visibility = View.VISIBLE
        }
    }

    fun setCaptureLoadingColor(color: Int) {
    }

    fun resetCaptureLayout() {
    }


    fun startAlphaAnimation() {
    }

    fun setTextWithAnimation(tip: String?) {
    }

    fun setDuration(duration: Int) {
    }

    fun setMinDuration(duration: Int) {
    }

    fun setButtonFeatures(state: Int) {
    }

    fun setTip(tip: String?) {
    }

    fun showTip() {
    }

    fun setIconSrc(iconLeft: Int, iconRight: Int) {
    }

    fun setLeftClickListener(leftClickListener: ClickListener) {
    }

    fun setRightClickListener(rightClickListener: ClickListener) {
    }

    // 是否达到了最大上限
    fun isMaxMedia(isMaxMedia: Boolean, maxNumber: Int){
        this.mIsMaxMedia = isMaxMedia
        this.mIsMaxMediaNumber = maxNumber
    }

    //更新进度条
    private fun updateProgress(millisUntilFinished: Long) {
        recorded_time = (duration - millisUntilFinished).toInt()
        progress = 100f - millisUntilFinished / duration.toFloat() * 100f
        mProgressBar.progress = progress.toInt()
        invalidate()
    }

    private fun recordEnd() {
        if (state == STATE_RECORDERING) {
            viewRecordStart.animate().scaleX(1f).scaleY(1f).start()
            viewRecording.animate().scaleX(0f).scaleY(0f).start()
            timer?.cancel() //停止计时器
            if (recorded_time < min_duration) captureListener?.recordShort(recorded_time.toLong()) //回调录制时间过短
            else captureListener?.recordEnd(recorded_time.toLong()) //回调录制结束
            state = STATE_IDLE
        }
    }

    //录制视频计时器
    inner class RecordCountDownTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            updateProgress(millisUntilFinished)
        }

        override fun onFinish() {
            mProgressBar.progress = 100
            recordEnd()
        }
    }

    /**
     * Dialog
     *
     * @param content
     */
    private fun showPromptDialog(content: String?) {
        val dialog = PictureCustomDialog(mContext, R.layout.picture_prompt_dialog)
        val btnOk = dialog.findViewById<TextView>(R.id.btnOk)
        val tvContent = dialog.findViewById<TextView>(R.id.tv_content)
        tvContent.text = content
        btnOk.setOnClickListener { v: View? ->
            dialog.dismiss()
        }
        dialog.show()
    }
}