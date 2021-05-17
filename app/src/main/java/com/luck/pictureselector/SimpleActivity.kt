package com.luck.pictureselector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.entity.LocalMedia

class SimpleActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var btn_activity: Button
    private lateinit var btn_fragment: Button
    private lateinit var btn_mindu: Button
    private var selectionData = mutableListOf<LocalMedia>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)
        btn_activity = findViewById(R.id.btn_activity)
        btn_fragment = findViewById(R.id.btn_fragment)
        btn_mindu = findViewById(R.id.btn_mindu)
        btn_activity.setOnClickListener(this)
        btn_fragment.setOnClickListener(this)
        btn_mindu.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_activity -> startActivity(Intent(this@SimpleActivity, MainActivity::class.java))
            R.id.btn_fragment -> startActivity(Intent(this@SimpleActivity, PhotoFragmentActivity::class.java))
            R.id.btn_mindu -> {
                PictureSelector.create(this)
                        .openMdGallery() //全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                        .imageEngine(GlideEngine.createGlideEngine())
                        .selectionData(selectionData)
                        .forResult(PictureConfig.CHOOSE_REQUEST) //结果回调onActivityResult code
                val windowAnimationStyle = PictureSelectionConfig.windowAnimationStyle
                overridePendingTransition(
                        windowAnimationStyle.activityEnterAnimation, R.anim.picture_anim_fade_in)
            }
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    selectionData = selectList
                    Log.e("LocalMedia","图库返回："+ selectionData.size.toString())
                    // 例如 LocalMedia 里面返回五种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
                    // 4.media.getOriginalPath()); media.isOriginal());为true时此字段才有值
                    // 5.media.getAndroidQToPath();为Android Q版本特有返回的字段，此字段有值就用来做上传使用
                    // 如果同时开启裁剪和压缩，则取压缩路径为准因为是先裁剪后压缩
//                    for (media in selectList) {
//                        Log.i(TAG, "是否压缩:" + media.isCompressed)
//                        Log.i(TAG, "压缩:" + media.compressPath)
//                        Log.i(TAG, "原图:" + media.path)
//                        Log.i(TAG, "绝对路径:" + media.realPath)
//                        Log.i(TAG, "是否裁剪:" + media.isCut)
//                        Log.i(TAG, "裁剪:" + media.cutPath)
//                        Log.i(TAG, "是否开启原图:" + media.isOriginal)
//                        Log.i(TAG, "原图路径:" + media.originalPath)
//                        Log.i(TAG, "Android Q 特有Path:" + media.androidQToPath)
//                    }
                }
            }
        }
    }
}