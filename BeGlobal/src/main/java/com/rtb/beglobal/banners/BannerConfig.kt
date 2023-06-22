package com.rtb.beglobal.banners

import androidx.annotation.Keep
import com.google.android.gms.ads.AdSize
import com.google.gson.annotations.SerializedName
import com.rtb.beglobal.sdk.SDKConfig
import java.util.Date

@Keep
internal data class BannerConfig(
        @SerializedName("customUnitName")
        var customUnitName: String = "",
        @SerializedName("isNewUnit")
        var isNewUnit: Boolean = false,
        @SerializedName("publisherAdUnit")
        var publisherAdUnit: String = "",
        @SerializedName("adSizes")
        var adSizes: List<AdSize> = arrayListOf(),
        @SerializedName("position")
        var position: Int = 0,
        @SerializedName("retryConfig")
        var retryConfig: SDKConfig.RetryConfig? = null,
        @SerializedName("newUnit")
        var newUnit: SDKConfig.LoadConfig? = null,
        @SerializedName("hijack")
        var hijack: SDKConfig.LoadConfig? = null,
        @SerializedName("unFilled")
        var unFilled: SDKConfig.LoadConfig? = null,
        @SerializedName("placement")
        var placement: SDKConfig.Placement? = null,
        @SerializedName("difference")
        var difference: Int = 0,
        @SerializedName("activeRefreshInterval")
        var activeRefreshInterval: Int = 0,
        @SerializedName("passiveRefreshInterval")
        var passiveRefreshInterval: Int = 0,
        @SerializedName("factor")
        var factor: Int = 0,
        @SerializedName("minView")
        var minView: Int = 0,
        @SerializedName("minViewRtb")
        var minViewRtb: Int = 0,
        @SerializedName("refreshCount")
        var refreshCount: Int = 0,
        @SerializedName("isVisible")
        var isVisible: Boolean = false,
        @SerializedName("isVisibleFor")
        var isVisibleFor: Long = 0,
        @SerializedName("lastRefreshAt")
        var lastRefreshAt: Long = Date().time
)
