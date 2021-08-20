package com.qytech.qycamera.utils

import android.content.Context
import android.media.*
import android.os.SystemClock

import timber.log.Timber


enum class OutDevices(val value: String) {
    BYPASS("bypass"),
    HDMI("hdmi"),
    SPEAKER("speaker"),
    USB("usb"),
    BLUETOOTH("bluetooth"),
    ALL("hdmi,speaker,usb,bluetooth"),
    AUTO(""),
}

class AudioStream(context: Context) {
    companion object {
        const val TAG = "AudioStream"
        const val PROPERTIES_DEVICE_POLICY = "media.audio.device_policy"
        const val PROPERTIES_HDMIIN_ENABLE = "media.audio.hdmiin"
    }

    private var isRecording: Boolean = false
    private var currentOutput: OutDevices = OutDevices.AUTO
    private var recordThread: Thread? = null

    private fun switchAudioOutput(outDevices: OutDevices) {
        if (outDevices == currentOutput) {
            return
        }
        currentOutput = outDevices

        stop()

        start()
    }

    fun start(outDevices: OutDevices = OutDevices.AUTO) {
        runCatching {
            currentOutput = outDevices

            setDevicePolicy()

            isRecording = true

            recordThread = Thread(RecordSound()).apply {
                start()
            }
        }
    }

    fun stop() {
        runCatching {
            currentOutput = OutDevices.AUTO

            setDevicePolicy()

            isRecording = false

            recordThread?.join(300)
            recordThread = null
        }
    }

    private fun setDevicePolicy() {
        SystemPropertiesProxy.set(PROPERTIES_DEVICE_POLICY, currentOutput.value)
    }

    inner class RecordSound : Runnable {
        var audioRecord: AudioRecord? = null
        var audioTrack: AudioTrack? = null

        private fun rampVolume(inBytes: ByteArray, up: Boolean) {
            val inShorts = inBytes.toShortArray()
            val frameCount = inShorts.size / 2
            Timber.d("ramp volume count: $frameCount")
            var vl = if (up) 0.0f else 1.0f
            val vlInc = (if (up) 1.0f else -1.0f) / frameCount
            for (i in 0 until frameCount) {
                val a = vl * inShorts[i * 2].toFloat()
                inShorts[i * 2] = a.toInt().toShort()
                inShorts[i * 2 + 1] = a.toInt().toShort()
                vl += vlInc
            }
            inShorts.toByteArray(inBytes)
        }

        override fun run() {
            synchronized(this) {
                val frequence = 44100
                val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO
                val audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
                var outBuffSize: Int =
                    AudioTrack.getMinBufferSize(frequence, channelConfig, audioEncoding)
                if (outBuffSize < 8192) {
                    Timber.w("Track buffer=$outBuffSize, set to 8192")
                    outBuffSize = 8192
                }
                //int streamType, int sampleRateInHz, int channelConfig, int audioFormat,
                //            int bufferSizeInBytes, int mode
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC, frequence, channelConfig, audioEncoding,
                    outBuffSize, AudioTrack.MODE_STREAM
                )
                Timber.d("set media.audio.hdmiin 1")
                SystemPropertiesProxy.set(PROPERTIES_HDMIIN_ENABLE, "1")
                val inBuff: ByteArray
                val inBuffSize =
                    AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding)
                Timber.i("out min: $outBuffSize, in min: $inBuffSize")
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.CAMCORDER, frequence, channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT, inBuffSize
                )
                inBuff = ByteArray(inBuffSize)
                audioRecord?.startRecording()
                audioTrack?.play()
                var readBytes = 0

                // discard 500ms audio data
                var preReadCount = 1 + frequence * 2 * 2 / 2 / inBuffSize
                Timber.d("pre read count $preReadCount")
                while (isRecording && preReadCount-- >= 0) {
                    readBytes =
                        audioRecord?.read(inBuff, 0, inBuffSize) ?: 0
                }
                Timber.d("pre read end")
                if (!isRecording) {
                    Timber.d("exit hdmiin audio")
                    audioRecord?.release()
                    audioRecord = null
                    Timber.d("set media.audio.hdmiin 0")
                    SystemPropertiesProxy.set(PROPERTIES_HDMIIN_ENABLE, "0")
                    audioTrack?.release()
                    audioTrack = null
                    return
                }

                // ramp volume for begin
                rampVolume(inBuff, true)
                while (isRecording) {
                    if (readBytes > 0 && audioTrack != null) {
                        audioTrack?.write(inBuff, 0, readBytes)
                    }
                    readBytes = audioRecord?.read(inBuff, 0, inBuffSize) ?: 0
                }
            }
            Timber.d("exit hdmiin audio")
            audioRecord?.release()
            audioRecord = null
            Timber.d("set media.audio.hdmiin 0")
            SystemPropertiesProxy.set(PROPERTIES_HDMIIN_ENABLE, "0")

            // ramp volume for end
            Timber.d("AudioTrack setVolume 0")
            audioTrack?.setVolume(0.0f)
            Timber.d("AudioTrack pause")
            audioTrack?.pause()
            SystemClock.sleep(50)
            Timber.d("AudioTrack stop")
            audioTrack?.release()
            audioTrack = null
        }
    }
}