package com.qytech.qycamera.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.qycamera.R
import com.qytech.qycamera.databinding.PreviewFragmentBinding
import com.qytech.qycamera.ui.detatil.DetailActivity
import com.qytech.qycamera.utils.toast
import com.qytech.securitycheck.consts.ExtraConst
import java.io.File

class PreviewFragment : Fragment() {

    companion object {
        fun newInstance(path: String) = PreviewFragment().apply {
            val bundle = Bundle()
            bundle.putString(ExtraConst.FILE_PATH, path)
            arguments = bundle
        }
    }

    private lateinit var viewModel: PreviewViewModel
    private lateinit var dataBinding: PreviewFragmentBinding
    private lateinit var adapter: PreviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = PreviewFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(PreviewViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        viewModel.fetchData(arguments?.getString(ExtraConst.FILE_PATH, "") ?: "")
        adapter = PreviewAdapter()
        adapter.onItemClickListener = object : PreviewAdapter.OnItemClickListener {
            override fun onItemClick(item: File?) {
                item?.let {
                    if (!it.exists()) {
                        toast(R.string.file_not_exists)
                        return
                    }
                    when {
                        it.isDirectory -> {
                            PreviewActivity.start(requireContext(), it.absolutePath)
                        }
                        it.isFile -> {
                            DetailActivity.start(requireContext(), it.absolutePath)
                        }
                    }
                }
            }
        }
        dataBinding.adapter = adapter
        viewModel.data.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

}