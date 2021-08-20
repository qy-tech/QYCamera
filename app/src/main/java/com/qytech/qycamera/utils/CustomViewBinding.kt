package com.qytech.qycamera.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JzvdStd
import coil.decode.VideoFrameDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.load
import coil.size.Scale
import com.qytech.qycamera.R
import timber.log.Timber
import java.io.File

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */
@BindingAdapter(value = ["setAdapter"])
fun RecyclerView.bindRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>) {
    this.run {
        this.setHasFixedSize(true)
        this.adapter = adapter
    }
}

@BindingAdapter(value = ["setImageUrl"])
fun ImageView.bindImageUrl(url: String?) {
    if (url != null && url.isNotBlank()) {
        if (url.startsWith("/")) {
            this.load(File(url)) {
                placeholder(R.drawable.ic_baseline_folder)
                error(R.drawable.ic_baseline_folder)
                scale(Scale.FIT)
            }
        } else {
            this.load(url) {
                placeholder(R.drawable.ic_baseline_folder)
                error(R.drawable.ic_baseline_folder)
                scale(Scale.FIT)
            }
        }
    }
}

@BindingAdapter(value = ["setVideoUrl", "setTitle"])
fun JzvdStd.bindVideoUrl(url: String?, title: String?) {
    if (url != null && url.isNotBlank()) {
        Timber.d("bindVideoUrl message:  $url")
        setUp(url, title)
    }
}