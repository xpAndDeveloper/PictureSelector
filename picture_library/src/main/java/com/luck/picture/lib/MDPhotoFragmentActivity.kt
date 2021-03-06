package com.luck.picture.lib

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.css.fontfamilylib.FontType
import com.css.fontfamilylib.setFontFamily
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.event.HideBottom
import com.luck.picture.lib.immersive.ImmersiveManage
import com.luck.picture.lib.permissions.PermissionChecker
import com.luck.picture.lib.tools.ToastUtils
import com.luck.picture.lib.widget.NoScrollViewPager
import net.lucode.hackware.magicindicator.MagicIndicator
import net.lucode.hackware.magicindicator.abs.IPagerNavigator
import net.lucode.hackware.magicindicator.buildins.UIUtil
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MDPhotoFragmentActivity : AppCompatActivity() {
    private val titles = arrayOf("照片", "视频", "拍视频", "拍照")
    private val fragments = arrayListOf<Fragment>()
    private var maxSelectNum = 9
    private var selectedNum = 9
    private lateinit var viewPager: NoScrollViewPager
    private lateinit var tabLayout: MagicIndicator
    private lateinit var llTabLayout: LinearLayout
    private var selectionData = arrayListOf<LocalMedia>()

    private var noChangeNum = 5//前两次不处理
//    private var mCameraFragment: PictureCustomCameraFragment ? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun hideBottomEvent(event: HideBottom?) {
        if (event != null) {
            if (noChangeNum == 0) {
                tabLayout.visibility = if (event.isHide) View.INVISIBLE else View.VISIBLE
            } else {
                noChangeNum--
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectionData = intent.getParcelableArrayListExtra<LocalMedia>("selectionMedias") ?: arrayListOf()
        maxSelectNum = intent.getIntExtra("maxSelectNum",9)
        selectedNum = intent.getIntExtra("selectedNum",9)
        EventBus.getDefault().register(this)
        ImmersiveManage.immersiveAboveAPI23(this, Color.parseColor("#101010"), Color.parseColor("#101010"), false)
        setContentView(R.layout.activity_md_photo)
        Log.e("LocalMedia", "数据恢复：" + selectionData.size.toString())
        initView()
        tabLayout.visibility = if (selectionData.isNotEmpty()) View.INVISIBLE else View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun initView() {
        fragments.add(PictureSelectorFragment())
        fragments.add(PictureSelectorFragment())
        fragments.add(PictureCustomCameraFragment())
        fragments[0].arguments = Bundle().apply {
            putInt("chooseMode", PictureMimeType.ofImage())
            if (selectedNum != 0) {
                putInt("maxSelectNum",maxSelectNum)
                putInt("selectedNum",selectedNum)
            }
        }
        fragments[1].arguments = Bundle().apply {
            putInt("chooseMode", PictureMimeType.ofVideo())
        }
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tab)
        llTabLayout = findViewById(R.id.ll_tab)
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = initViewPagerAdapter()
        tabLayout.navigator = initCommonNavigator()
        (( tabLayout.navigator as CommonNavigator).getPagerTitleView(0) as SimplePagerTitleView).setFontFamily(FontType.Medium)
        if (selectedNum != 0){
            llTabLayout.visibility = View.GONE
            viewPager.currentItem = 0
        }else{
            llTabLayout.visibility = View.VISIBLE
        }
    }

    private fun initViewPagerAdapter(): PagerAdapter? {
        return object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return fragments.size
            }

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return titles[position]
            }
        }
    }

    private fun initCommonNavigator(): IPagerNavigator? {
        val commonNavigator = CommonNavigator(this)
        commonNavigator.isAdjustMode = true
        commonNavigator.adapter = object : CommonNavigatorAdapter() {
            override fun getTitleView(context: Context?, index: Int): IPagerTitleView {
                val simplePagerTitleView = SimplePagerTitleView(context)
                simplePagerTitleView.text = titles[index]
                simplePagerTitleView.width = UIUtil.dip2px(this@MDPhotoFragmentActivity, 80.0)
                simplePagerTitleView.textSize = 16f
                simplePagerTitleView.setFontFamily(FontType.Light)
                simplePagerTitleView.normalColor = Color.parseColor("#909090")
                simplePagerTitleView.selectedColor = Color.parseColor("#ffffff")
                simplePagerTitleView.setOnClickListener {
                    titles.forEachIndexed { index, s ->
                        (commonNavigator.getPagerTitleView(index) as SimplePagerTitleView).setFontFamily(FontType.Light)
                    }
                    (commonNavigator.getPagerTitleView(index) as SimplePagerTitleView).setFontFamily(FontType.Medium)
                    tabLayout.onPageSelected(index)
                    commonNavigator.pagerIndicator.onPageScrolled(index, 0f, 0)
                    viewPager.currentItem = if (index > 1) 2 else index
                    if (index > 1 && fragments[2] is PictureCustomCameraFragment) {
                        (fragments[2] as PictureCustomCameraFragment).setCameraPreviewIsVideo(index == 2)
                    }
                }
                return simplePagerTitleView
            }

            override fun getCount(): Int {
                return titles.size
            }

            override fun getIndicator(context: Context?): IPagerIndicator {
                val linePagerIndicator = LinePagerIndicator(context)
                linePagerIndicator.setColors(Color.parseColor("#ffffff"))
                linePagerIndicator.mode = LinePagerIndicator.MODE_EXACTLY
                linePagerIndicator.lineWidth = UIUtil.dip2px(context, 26.0).toFloat()
                linePagerIndicator.lineHeight = UIUtil.dip2px(context, 2.0).toFloat()

                return linePagerIndicator
            }
        }
        return commonNavigator
    }

    /**
     * 解决Fragment中的onActivityResult()方法无响应问题。
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /**
         * 1.使用getSupportFragmentManager().getFragments()获取到当前Activity中添加的Fragment集合
         * 2.遍历Fragment集合，手动调用在当前Activity中的Fragment中的onActivityResult()方法。
         */
        if (supportFragmentManager.fragments.size > 0) {
            val fragments = supportFragmentManager.fragments
            fragments[viewPager.currentItem].onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fragments.forEach {
            it.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}