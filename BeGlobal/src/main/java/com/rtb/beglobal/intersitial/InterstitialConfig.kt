package com.rtb.beglobal.intersitial

import com.rtb.beglobal.sdk.SDKConfig

internal data class InterstitialConfig(
    var customUnitName: String = "",
    var isNewUnit: Boolean = false,
    var position: Int = 0,
    var newUnit: SDKConfig.LoadConfig? = null,
    var hijack: SDKConfig.LoadConfig? = null,
    var unFilled: SDKConfig.LoadConfig? = null,
    var placement: SDKConfig.Placement? = null,
)
