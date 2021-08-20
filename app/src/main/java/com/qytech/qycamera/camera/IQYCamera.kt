package com.qytech.qycamera.camera

interface IQYCamera {

    /**
     * 根据摄像头 ID 初始化摄像头
     * */
    fun initCamera(id: String)

    /**
     * 释放摄像头
     * */
    fun releaseCamera()

    /**
     * 拍照并返回 LiveData 存储了照片的路径
     * */
    fun takePicture(callback: (String?) -> Unit)

    /**
     * 开始录像
     * */
    fun startRecordVideo()

    /**
     * 结束录制并返回 LiveData 记录了录像的路径
     * */
    fun stopRecordVideo(callback: (String?) -> Unit)

    /**
     * 销毁对象释放资源
     * */
    fun destroy()

}