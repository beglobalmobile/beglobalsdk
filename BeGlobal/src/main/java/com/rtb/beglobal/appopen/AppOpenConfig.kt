package com.rtb.beglobal.appopen

import com.rtb.beglobal.sdk.SDKConfig

internal data class AppOpenConfig(
    var customUnitName: String = "",
    var isNewUnit: Boolean = false,
    var position: Int = 0,
    var expriry: Int = 0,
    var retryConfig: SDKConfig.RetryConfig? = null,
    var newUnit: SDKConfig.LoadConfig? = null,
    var hijack: SDKConfig.LoadConfig? = null,
    var unFilled: SDKConfig.LoadConfig? = null,
)
