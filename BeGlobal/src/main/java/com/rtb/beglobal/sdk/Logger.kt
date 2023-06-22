package com.rtb.beglobal.sdk

import android.util.Log
import android.view.View
import com.rtb.beglobal.common.TAG

internal enum class Logger {
    DEBUG, INFO, ERROR
}

internal fun Logger.log(tag: String = TAG, msg: String) {
    if (!BeGlobal.logEnabled) return
    when (this) {
        Logger.INFO -> Log.i(tag, msg)
        Logger.DEBUG -> Log.d(tag, msg)
        Logger.ERROR -> Log.e(tag, msg)
    }
}

internal fun log(getMessage: () -> String) {
    if (!BeGlobal.specialTag.isNullOrEmpty()) {
        try {
            Log.i(BeGlobal.specialTag, getMessage())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

internal fun View?.log(getMessage: () -> String) {
    if (!BeGlobal.specialTag.isNullOrEmpty()) {
        try {
            Log.i(BeGlobal.specialTag, String.format("%d-%s", this?.id ?: -1, getMessage()))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}