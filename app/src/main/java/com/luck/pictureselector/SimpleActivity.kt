package com.luck.pictureselector

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.luck.picture.lib.MDPhotoFragmentActivity
import com.luck.picture.lib.config.PictureSelectionConfig

class SimpleActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var btn_activity: Button
    private lateinit var btn_fragment: Button
    private lateinit var btn_mindu: Button
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
                startActivity(Intent(this@SimpleActivity, MDPhotoFragmentActivity::class.java))
                val windowAnimationStyle = PictureSelectionConfig.windowAnimationStyle
                overridePendingTransition(
                        windowAnimationStyle.activityEnterAnimation, R.anim.picture_anim_fade_in)
            }
            else -> {
            }
        }
    }
}