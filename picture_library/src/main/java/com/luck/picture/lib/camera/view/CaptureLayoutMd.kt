package com.luck.picture.lib.camera.view

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.luck.picture.lib.R
import com.luck.picture.lib.camera.CustomCameraView
import com.luck.picture.lib.camera.listener.CaptureListener
import com.luck.picture.lib.camera.listener.ClickListener
import com.luck.picture.lib.camera.listener.TypeListener
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
    private val duration = 10 * 1000 //录制视频最大时间长度
    private var progress = 0f //录制视频的进度
    private val min_duration = CustomCameraView.DEFAULT_MIN_RECORD_VIDEO  //最短录制时间限制


    private val mContext = context
    private val mView: View = LayoutInflater.from(context).inflate(R.layout.picture_capture_layout_md, this, true)

    private var captureListener: CaptureListener? = null //拍照按钮监听
    private var typeListener: TypeListener? = null //拍照或录制后接结果按钮监听
    private val leftClickListener: ClickListener? = null  //左边按钮监听
    private val rightClickListener: ClickListener? = null //右边按钮监听


    fun setTypeListener(typeListener: TypeListener?) {
        this.typeListener = typeListener
    }

    fun setCaptureListener(captureListener: CaptureListener?) {
        this.captureListener = captureListener
    }

    init {
        initView()
    }

    private fun initView() {
        state = STATE_IDLE
        mView.run {
            flTakePhoto.setOnClickListener {
                if (state == STATE_IDLE) {
                    timer = RecordCountDownTimer(duration.toLong(), (duration / 360).toLong()) //录制定时器
                    captureListener?.recordStart()
                    timer?.start()
                    state = STATE_RECORDERING
                } else if (state == STATE_RECORDERING) {
                    timer?.cancel() //停止计时器
                    if (recorded_time < min_duration) captureListener?.recordShort(recorded_time.toLong()) //回调录制时间过短
                    else captureListener?.recordEnd(recorded_time.toLong()) //回调录制结束
                    state = STATE_IDLE
                }
//                captureListener?.takePictures()
//                captureListener?.recordStart()
//                captureListener?.recordEnd()
            }

//            btn_capture.setCaptureListener(object : CaptureListener {
//                override fun takePictures() {
//                    if (captureListener != null) {
//                        captureListener!!.takePictures()
//                    }
//                    startAlphaAnimation()
//                }
//
//                override fun recordShort(time: Long) {
//                    if (captureListener != null) {
//                        captureListener!!.recordShort(time)
//                    }
//                }
//
//                override fun recordStart() {
//                    if (captureListener != null) {
//                        captureListener!!.recordStart()
//                    }
//                    startAlphaAnimation()
//                }
//
//                override fun recordEnd(time: Long) {
//                    if (captureListener != null) {
//                        captureListener!!.recordEnd(time)
//                    }
//                    startTypeBtnAnimator()
//                }
//
//                override fun recordZoom(zoom: Float) {
//                    if (captureListener != null) {
//                        captureListener!!.recordZoom(zoom)
//                    }
//                }
//
//                override fun recordError() {
//                    if (captureListener != null) {
//                        captureListener!!.recordError()
//                    }
//                }
//            })
        }
    }


    fun setButtonCaptureEnabled(enabled: Boolean) {
        mView.flTakePhoto.isEnabled = enabled
    }

    // 拍照,视频完成保存后
    fun startTypeBtnAnimator() {
//        typeListener?.confirm()
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

    //更新进度条
    private fun updateProgress(millisUntilFinished: Long) {
        recorded_time = (duration - millisUntilFinished).toInt()
        progress = 360f - millisUntilFinished / duration.toFloat() * 360f
        invalidate()
    }

    private fun recordEnd() {

    }

    //录制视频计时器
    inner class RecordCountDownTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            updateProgress(millisUntilFinished)
        }

        override fun onFinish() {
            recordEnd()
        }
    }
}