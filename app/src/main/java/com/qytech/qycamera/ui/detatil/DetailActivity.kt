package com.qytech.qycamera.ui.detatil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.qytech.qycamera.R
import com.qytech.qycamera.utils.isMovies
import com.qytech.qycamera.utils.isPictures
import com.qytech.qycamera.utils.toast
import com.qytech.securitycheck.consts.ExtraConst
import timber.log.Timber
import java.io.File

class DetailActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context, path: String) {
            val starter = Intent(context, DetailActivity::class.java)
                .putExtra(ExtraConst.FILE_PATH, path)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//左侧添加一个默认的返回图标
        supportActionBar?.setHomeButtonEnabled(true) //设置返回键可用
        //        Timber.d("onCreate message:  ")
        if (savedInstanceState == null) {
            val file = File(intent.getStringExtra(ExtraConst.FILE_PATH) ?: "")
            if (!file.exists()) {
                toast(R.string.file_not_exists)
                finish()
            }
            Timber.d("onCreate message:  ${file.absolutePath}")
            if (file.isPictures) {
                commitFragment(ImageDetailFragment.newInstance(file.absolutePath))
            } else if (file.isMovies) {
                commitFragment(VideoDetailFragment.newInstance(file.absolutePath))
            }
        }
    }

    private fun commitFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commitNow()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}