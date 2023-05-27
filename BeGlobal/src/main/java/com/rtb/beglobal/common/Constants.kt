package com.rtb.beglobal.common

internal const val TAG = "Ads"

internal object URLs {
     const val BASE_URL = "https://prebid.andbeyond.media/"
}

object AdTypes {
    const val BANNER = "BANNER"
    const val ADAPTIVE = "ADAPTIVE"
    const val INLINE = "INLINE"
    const val STICKY = "STICKY"
    const val INREAD = "INREAD"
    const val INTERSTITIAL = "INTERSTITIAL"
    const val REWARDEDINTERSTITIAL = "REWARDEDINTERSTITIAL"
    const val REWARDED = "REWARDED"
    const val APPOPEN = "APPOPEN"
    const val NATIVE = "NATIVE"
    const val OTHER = "OTHER"
}

internal enum class LogLevel {
    DEBUG, INFO, ERROR
}