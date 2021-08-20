package com.qytech.qycamera.ui.detatil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.jzvd.Jzvd
import com.qytech.qycamera.databinding.VideoDetailFragmentBinding
import com.qytech.securitycheck.consts.ExtraConst

class VideoDetailFragment : Fragment() {

    companion object {
        fun newInstance(videoPath: String) = VideoDetailFragment().apply {
            val args = Bundle()
            args.putString(ExtraConst.FILE_PATH, videoPath)
            arguments = args
        }
    }

    private lateinit var viewModel: VideoDetailViewModel
    private lateinit var dataBinding: VideoDetailFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = VideoDetailFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(VideoDetailViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        val videoPath = arguments?.getString(ExtraConst.FILE_PATH) ?: ""
        viewModel.setImagePath(videoPath)
    }


    override fun onPause() {
        super.onPause()
        Jzvd.releaseAllVideos();
    }

}