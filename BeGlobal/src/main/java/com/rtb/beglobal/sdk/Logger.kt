package com.rtb.beglobal.sdk

import android.util.Log
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.common.TAG


internal fun LogLevel.log(msg: String) {
    if (!BeGlobal.logEnabled()) return
    when (this) {
        LogLevel.INFO -> Log.i(TAG, msg)
        LogLevel.DEBUG -> Log.d(TAG, msg)
        LogLevel.ERROR -> Log.e(TAG, msg)
    }
}