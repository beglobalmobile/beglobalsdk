package com.rtb.beglobal.intersitial

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.rtb.beglobal.sdk.SDKConfig

@Keep
internal data class InterstitialConfig(
        @SerializedName("customUnitName")
        var customUnitName: String = "",
        @SerializedName("isNewUnit")
        var isNewUnit: Boolean = false,
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
)
