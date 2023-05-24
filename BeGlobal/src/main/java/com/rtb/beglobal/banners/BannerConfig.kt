package com.rtb.beglobal.banners

import com.google.android.gms.ads.AdSize
import com.rtb.beglobal.sdk.SDKConfig
import java.util.Date

internal data class BannerConfig(
    var customUnitName: String = "",
    var isNewUnit: Boolean = false,
    var publisherAdUnit: String = "",
    var adSizes: List<AdSize> = arrayListOf(),
    var position: Int = 0,
    var retryConfig: SDKConfig.RetryConfig? = null,
    var newUnit: SDKConfig.LoadConfig? = null,
    var hijack: SDKConfig.LoadConfig? = null,
    var unFilled: SDKConfig.LoadConfig? = null,
    var placement: SDKConfig.Placement? = null,
    var difference: Int = 0,
    var activeRefreshInterval: Int = 0,
    var passiveRefreshInterval: Int = 0,
    var factor: Int = 0,
    var minView: Int = 0,
    var minViewRtb: Int = 0,
    var refreshCount: Int = 0,
    var isVisible: Boolean = false,
    var isVisibleFor: Long = 0,
    var lastRefreshAt: Long = Date().time
)
