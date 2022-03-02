package com.qytech.qycamera.ui.main

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.qycamera.databinding.MainFragmentBinding
import com.qytech.qycamera.ui.preview.PreviewActivity
import com.qytech.qycamera.camera.QYCamera2
import com.qytech.qycamera.utils.toast
import com.qytech.qycamera.widget.RecordButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding

    private lateinit var qyCamera2: QYCamera2
    private var isRecoding = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = MainFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        qyCamera2 = QYCamera2(requireContext(), dataBinding.viewFinder)
        dataBinding.btnRecord.setOnRecordListener(onRecordList)

        //dataBinding.spinnerCameraList.onItemSelectedListener = onItemSelectedListener
        dataBinding.ivFileExplorer.setOnClickListener {
            PreviewActivity.start(
                requireContext(),
                requireContext().getExternalFilesDir(null)?.absolutePath ?: ""
            )
        }
//        ArrayAdapter<String>(
//            requireContext(),
//            R.layout.simple_spinner_item
//        ).also { adapter ->
//            dataBinding.spinnerCameraList.adapter = adapter
//            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
//            adapter.addAll(qyCamera2.cameraIdList.toMutableList())
//            adapter.notifyDataSetChanged()
//        }

//        val cameraId = qyCamera2.cameraIdList.last()
//            if (cameraId != qyCamera2.cameraId) {
//                qyCamera2.releaseCamera()
//                qyCamera2.initCamera(cameraId)
//            }
        Timber.d("onViewCreated")
    }

    override fun onDestroy() {
        super.onDestroy()
        qyCamera2.destroy()
    }

//    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//            val cameraId = qyCamera2.cameraIdList[position]
//            if (cameraId != qyCamera2.cameraId) {
//                qyCamera2.releaseCamera()
//                qyCamera2.initCamera(cameraId)
//            }
//        }
//
//        override fun onNothingSelected(parent: AdapterView<*>?) {
//        }
//    }

    private val onRecordList = object : RecordButton.RecordListener {
        override fun onTakePicture() {
            Timber.d("onTakePicture")
            qyCamera2.takePicture {
                Timber.d("stopRecordVideo save image path is $it ")
                if (it?.isNotEmpty() == true) {
                    toast("save image success")
                } else {
                    toast("save image fail")
                }
            }
        }

        override fun onRecording(duration: Int) {
            if (!isRecoding) {
                isRecoding = true
                qyCamera2.startRecordVideo()
            }
        }

        override fun onRecordCancel() {
            Timber.d("onRecordCancel")
            stopRecord()
        }

        override fun onRecordStop() {
            Timber.d("onRecordStop")
            stopRecord()
        }
    }

    private fun stopRecord() {
        if (isRecoding) {
            isRecoding = false
            qyCamera2.stopRecordVideo {
                Timber.d("stopRecordVideo save video path is $it ")
                if (it?.isNotEmpty() == true) {
                    toast("save video success")
                } else {
                    toast("save video fail")
                }
            }
        }
    }

}