package com.qytech.qycamera.ui.detatil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.qycamera.databinding.PhotoDetailFragmentBinding
import com.qytech.securitycheck.consts.ExtraConst

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */
class ImageDetailFragment : Fragment() {

    companion object {
        fun newInstance(imagePath: String): ImageDetailFragment {
            val args = Bundle()
            args.putString(ExtraConst.FILE_PATH, imagePath)
            return ImageDetailFragment().apply {
                arguments = args
            }
        }
    }

    private lateinit var dataBinding: PhotoDetailFragmentBinding
    private lateinit var viewModel: ImageDetailViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = PhotoDetailFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ImageDetailViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewMode = viewModel
        viewModel.setImagePath(arguments?.getString(ExtraConst.FILE_PATH, "") ?: "")

    }
}