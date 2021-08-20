package com.qytech.qycamera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import com.qytech.qycamera.utils.AudioStream
import com.qytech.qycamera.utils.EXTENSIONS_MOVIES
import com.qytech.qycamera.utils.EXTENSIONS_PICTURES
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 针对 Camera2 封装的工具类
 * **/
@ExperimentalCoroutinesApi
class QYCamera2(
    private val context: Context,
    private val surfaceView: SurfaceView,
    override val coroutineContext: CoroutineContext = Dispatchers.Default
) : IQYCamera, CoroutineScope {
    companion object {
        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L
        private const val THREAD_NAME_CAMERA = "CameraThread"
        private const val THREAD_NAME_IMAGE_READER = "ImageReader"
        private const val IMAGE_BUFFER_SIZE = 1

        private const val HDMIIN_CAMERA_ID = "1"
    }

    //获取CameraManager
    private val cameraManger: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    //获取所有的可用摄像头 ID
    val cameraIdList: List<String> by lazy {
        cameraManger.cameraIdList.filter { id ->
            cameraManger.getCameraCharacteristics(id)
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                ?: false
        }
    }

    //当前使用的摄像头 ID
    lateinit var cameraId: String
        private set

    //默认使用前置摄像头
    var cameraFacing: Int = CameraCharacteristics.LENS_FACING_BACK
        private set

    private var imageFormat: Int = ImageFormat.JPEG

    //当前的摄像头特征信息
    private lateinit var cameraCharacteristics: CameraCharacteristics

    //当前摄像头
    private lateinit var camera: CameraDevice

    //摄像头会话
    private lateinit var session: CameraCaptureSession

    //拍照
    private lateinit var imageReader: ImageReader
    private var imageQueue: ArrayBlockingQueue<Image> =
        ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private var recordingStartMillis = 0L

    private lateinit var previewSize: Size
    private lateinit var imageSize: Size
    private lateinit var videoSize: Size

    //为拍照创建一个新的线程和 Handler
    private val imageReaderThread = HandlerThread(THREAD_NAME_IMAGE_READER).apply { start() }

    private val imageReaderHandler = Handler(imageReaderThread.looper)

    //为相机运行创建一个新的线程和 Handler
    private val cameraThread = HandlerThread(THREAD_NAME_CAMERA).apply { start() }

    private val cameraHandler = Handler(cameraThread.looper)


    private var imageOutput: String? = null
    private var videoOutput: String? = null
    private var isAvailable = false

    private val audioStream: AudioStream by lazy {
        AudioStream(context)
    }

    private val surfaceHolderCallBack = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Timber.d("surfaceCreated")
            if (cameraIdList.isNotEmpty()) {
                if (!::cameraId.isInitialized) {
                    initCamera(cameraIdList.first())
                } else {
                    initCamera(cameraId)
                }
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Timber.d("surfaceDestroyed")
            releaseCamera()
        }
    }

    init {
        surfaceView.holder.addCallback(surfaceHolderCallBack)
    }


    @ExperimentalCoroutinesApi
    override fun initCamera(id: String) {
        Timber.d("init camera isAvailable $isAvailable")
        launch {
            cameraId = id
            // 获取摄像头特征
            cameraCharacteristics = cameraManger.getCameraCharacteristics(id)
            // 获取前后置属性
            cameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!

            imageFormat = getSupportImageFormat()
            // 获取预览和拍照、录像的最大尺寸
            previewSize = getSupportPreviewSize()
            surfaceView.holder.setFixedSize(previewSize.width, previewSize.height)

            imageSize = getSupportImageSize()
            videoSize = getSupportVideoSize()

            Timber.d("support preview size is $previewSize")
            Timber.d("support image size is $imageSize")
            Timber.d("support video size is $videoSize")
            // 打开摄像头
            camera = openCamera()
            // 创建预览、拍照会话，并开始预览
            createImageSession()
            startAudioStream()
        }
    }

    /**
     * 关闭对应的流
     * */
    override fun releaseCamera() {
        runCatching {
            session.close()
            camera.close()
            imageReader.close()
            mediaRecorder.stop()
            stopAudioStream()
        }
    }

    /**
     * 拍照
     * */
    override fun takePicture(callback: (String?) -> Unit) {
        runCatching {
            session.capture(
                createImageRequest(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                        Timber.d("Capture result received: $resultTimestamp")
                        launch(Dispatchers.IO) {
                            val image = imageQueue.take()
                            imageOutput = saveResult(image, result)
                            scanFile(imageOutput)
                            launch(Dispatchers.Main) {
                                callback(imageOutput)
                            }
                            image.close()
                        }
                    }
                },
                cameraHandler
            )
        }.onFailure {
            callback(null)
        }

    }


    /**
     * 开始录像
     * */
    override fun startRecordVideo() {
        if (isRecording) {
            return
        }
        isRecording = true
        launch {
            runCatching {
                stopAudioStream()
                createVideoSession()
                recordingStartMillis = System.currentTimeMillis()
                mediaRecorder.start()
            }
        }
    }

    /**
     * 结束录像
     * */
    override fun stopRecordVideo(callback: (String?) -> Unit) {
        if (!isRecording) {
            return
        }
        isRecording = false
        launch(Dispatchers.IO) {
            runCatching {
                startAudioStream()
                val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
                if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
                    delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
                }
                mediaRecorder.stop()
                mediaRecorder.reset()
                scanFile(videoOutput)
                launch(Dispatchers.Main) {
                    callback(videoOutput)
                }
                createImageSession()
            }.onFailure {
                launch(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    /**
     * 释放资源
     * */
    override fun destroy() {
        runCatching {
            mediaRecorder.release()
            releaseThread()
        }
    }

    /**
     * 销毁线程释放资源
     * */
    private fun releaseThread() {
        runCatching {
            cameraThread.quitSafely()
            cameraThread.join()

            imageReaderThread.quitSafely()
            imageReaderThread.join()
        }
    }

    private suspend fun createImageSession() {
        runCatching {
            session.stopRepeating()
            session.close()
        }
        // 创建拍照
        imageReader = createImageReader()
        val targets = listOf(surfaceView.holder.surface, imageReader.surface)
        // 创建摄像头会话
        session = createCaptureSession(targets)
        // 开启预览
        session.setRepeatingRequest(createPreviewRequest(), null, cameraHandler)
    }

    private suspend fun createVideoSession() {
        runCatching {
            session.stopRepeating()
            session.close()
        }
        mediaRecorder = createRecorder()
        val targets = listOf(surfaceView.holder.surface, mediaRecorder.surface)
        session = createCaptureSession(targets)
        session.setRepeatingRequest(createVideoRequest(), null, cameraHandler)
    }

    /**
     * 判断是否有后置摄像头
     * */
    fun hasBackCamera(): Boolean = cameraIdList.any { id ->
        CameraCharacteristics.LENS_FACING_BACK == cameraManger.getCameraCharacteristics(id)
            .get(CameraCharacteristics.LENS_FACING)
    }

    /**
     * 判断是否有前置摄像头
     * */
    fun hasFrontCamera(): Boolean = cameraIdList.any { id ->
        CameraCharacteristics.LENS_FACING_FRONT == cameraManger.getCameraCharacteristics(id)
            .get(CameraCharacteristics.LENS_FACING)
    }

    /**
     * 根据摄像头方向获取摄像头 ID , 如果没有对应的方向则返回空
     * */
    fun getCameraIdByFacing(facing: Int): String? = cameraIdList.firstOrNull { id ->
        cameraManger.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == facing
    }

    private fun createPreviewRequest() =
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            .apply {
                addTarget(surfaceView.holder.surface)
            }
            .build()

    private fun createImageRequest() =
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            .apply {
                addTarget(imageReader.surface)
            }
            .build()

    private fun createVideoRequest() =
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview and recording surface targets
            addTarget(surfaceView.holder.surface)
            addTarget(mediaRecorder.surface)
            // Sets user requested FPS for all targets
            val fps = getVideoFrameRate()
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))
        }.build()

    private fun createImageReader(): ImageReader = ImageReader.newInstance(
        imageSize.width,
        imageSize.height,
        imageFormat,
        IMAGE_BUFFER_SIZE
    ).apply {
        setOnImageAvailableListener(onImageAvailableListener, imageReaderHandler)
    }

    private fun createRecorder(): MediaRecorder = MediaRecorder().apply {
        videoOutput = createFile(context, "mp4").absolutePath
        setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(videoOutput)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        if (getVideoFrameRate() > 0) setVideoFrameRate(getVideoFrameRate())
        setVideoSize(videoSize.width, videoSize.height)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setPreviewDisplay(surfaceView.holder.surface)
        runCatching {
            prepare()
        }
    }

    private fun getVideoFrameRate(): Int {
        val cameraConfig =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val secondsPerFrame: Double =
            cameraConfig?.getOutputMinFrameDuration(MediaRecorder::class.java, videoSize)
                ?.div((1_000_000_000.0))
                ?: Double.MIN_VALUE
        // Compute the frames per second to let user select a configuration
        return if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
    }

    private fun getSupportPreviewSize() =
        getSupportOutputSize(
            surfaceView.display,
            cameraCharacteristics,
            SurfaceHolder::class.java
        )


    private fun getSupportImageSize() =
        getSupportOutputSize(
            surfaceView.display,
            cameraCharacteristics,
            ImageReader::class.java,
            imageFormat
        )


    private fun getSupportVideoSize() =
        getSupportOutputSize(
            surfaceView.display,
            cameraCharacteristics,
            MediaRecorder::class.java
        )

    private suspend fun openCamera(): CameraDevice = suspendCancellableCoroutine { cont ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cont.resumeWithException(RuntimeException("Camera Permission is denied"))
        }
        cameraManger.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, cameraHandler)
    }

    private suspend fun createCaptureSession(targets: List<Surface>): CameraCaptureSession =
        suspendCoroutine { cont ->
            camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) =
                    cont.resume(session)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val exc = RuntimeException("Camera ${camera.id} session configuration failed");
                    cont.resumeWithException(exc)
                }
            }, cameraHandler)
        }

    @SuppressLint("InlinedApi")
    fun getSupportImageFormat(): Int {
        val capabilities =
            cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val outputFormats =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.outputFormats

        return when {
            (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true)
                    and (outputFormats?.contains(ImageFormat.RAW_SENSOR) == true) -> {
                ImageFormat.RAW_SENSOR
            }
            (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true)
                    and (outputFormats?.contains(ImageFormat.DEPTH_JPEG) == true) -> {
                ImageFormat.DEPTH_JPEG
            }
            else -> ImageFormat.JPEG
        }
    }

    /**
     * 拍照回调
     * 如果是 JPEG 这些格式数据也可以直接在这保存图片
     * */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireNextImage()
        Timber.d("onImageAvailable in queue ${image.timestamp}")
        imageQueue.add(image)
    }

    /**
     * 创建文件
     * */
    private fun createFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault())
        return when (".$extension".uppercase()) {
            in EXTENSIONS_PICTURES -> File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "IMG_${sdf.format(Date())}.$extension"
            )
            in EXTENSIONS_MOVIES -> File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "VIDEO_${sdf.format(Date())}.$extension"
            )
            else -> File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "${sdf.format(Date())}.$extension"
            )
        }
    }

    private fun scanFile(vararg paths: String?) {
        // Broadcasts the media file to the rest of the system
        MediaScannerConnection.scanFile(context, paths, null, null)
    }

    /**
     * 保存图片到文件
     * */
    private suspend fun saveResult(image: Image, metadata: CaptureResult): String =
        suspendCoroutine { cont ->
            when (imageReader.imageFormat) {
                // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
                ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                    runCatching {
                        val output = createFile(context, "jpg")
                        FileOutputStream(output).use { it.write(bytes) }
                        cont.resume(output.absolutePath)
                    }.onFailure {
                        cont.resumeWithException(it)
                    }
                }

                // When the format is RAW we use the DngCreator utility library
                ImageFormat.RAW_SENSOR -> {
                    val dngCreator = DngCreator(cameraCharacteristics, metadata)
                    runCatching {
                        val output = createFile(context, "dng")
                        FileOutputStream(output).use { dngCreator.writeImage(it, image) }
                        cont.resume(output.absolutePath)
                    }.onFailure {
                        cont.resumeWithException(it)
                    }
                }
                // No other formats are supported by this sample
                else -> {
                    val exc = RuntimeException("Unknown image format: ${image.format}")
                    cont.resumeWithException(exc)
                }
            }
        }

    private fun startAudioStream() {
        if (cameraId == HDMIIN_CAMERA_ID) {
            audioStream.start()
        }
    }

    private fun stopAudioStream() {
        if (cameraId == HDMIIN_CAMERA_ID) {
            audioStream.stop()
        }
    }
}