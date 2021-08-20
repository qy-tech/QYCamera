package com.qytech.qycamera.utils

inline fun ByteArray.toShortArray(): ShortArray {
    val count = this.size shr 1
    val dest = ShortArray(count)
    (0 until count).forEach { i ->
        dest[i] =
            (this[i * 2 + 1].toInt() shl 8 or this[2 * i + 0].toInt() and 0xff).toShort()
    }
    return dest
}

inline fun ShortArray.toByteArray(): ByteArray {
    val count = this.size
    val dest = ByteArray(count shl 1)
    (0 until count).forEach { i ->
        dest[i * 2 + 0] = (this[i].toInt() shr 0).toByte()
        dest[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
    }
    return dest
}

inline fun ShortArray.toByteArray(dest: ByteArray) {
    var count = this.size
    if (dest.size / 2 < count) count = dest.size / 2
    (0 until count).forEach { i ->
        dest[i * 2 + 0] = (this[i].toInt() shr 0).toByte()
        dest[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
    }
}