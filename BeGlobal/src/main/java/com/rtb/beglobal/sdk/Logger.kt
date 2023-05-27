package com.rtb.beglobal.sdk

import android.util.Log
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.common.TAG


internal fun LogLevel.log(tag: String = TAG, msg: String) {
    if (!BeGlobal.logEnabled()) return
    when (this) {
        LogLevel.INFO -> Log.i(tag, msg)
        LogLevel.DEBUG -> Log.d(tag, msg)
        LogLevel.ERROR -> Log.e(tag, msg)
    }
}