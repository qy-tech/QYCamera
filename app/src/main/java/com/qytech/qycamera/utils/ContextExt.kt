package com.qytech.qycamera.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(@StringRes res: Int) {
    Toast.makeText(this, resources.getString(res), Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(message: String) {
    requireContext().toast(message)
}

fun Fragment.toast(res: Int) {
    requireContext().toast(res)
}