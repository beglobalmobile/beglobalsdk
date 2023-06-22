package com.rtb.beglobal.sdk

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal data class SDKConfig(
        @SerializedName("aff")
        val affiliatedId: Long? = null,
        @SerializedName("refetch")
        val refetch: Long? = null,
        @SerializedName("retry_config")
        val retryConfig: RetryConfig? = null,
        @SerializedName("info")
        val infoConfig: InfoConfig? = null,
        @SerializedName("prebid")
        val prebid: Prebid? = null,
        @SerializedName("geoedge")
        val geoEdge: GeoEdge? = null,
        @SerializedName("network_block")
        val networkBlock: String? = null,
        @SerializedName("diff")
        val difference: Int? = null,
        @SerializedName("network")
        val networkId: String? = null,
        @SerializedName("networkcode")
        val networkCode: String? = null,
        @SerializedName("global")
        val switch: Int? = null,
        @SerializedName("active")
        var activeRefreshInterval: Int? = null,
        @SerializedName("passive")
        var passiveRefreshInterval: Int? = null,
        @SerializedName("factor")
        val factor: Int? = null,
        @SerializedName("min_view")
        val minView: Int? = null,
        @SerializedName("min_view_rtb")
        val minViewRtb: Int? = null,
        @SerializedName("config")
        val refreshConfig: List<RefreshConfig>? = null,
        @SerializedName("block")
        private val block: List<List<String>?>? = null,
        @SerializedName("hijack")
        val hijackConfig: LoadConfigs? = null,
        @SerializedName("unfilled")
        val unfilledConfig: LoadConfigs? = null
) {

    @Keep
    fun getBlockList() = arrayListOf<String>().apply {
        block?.forEach {
            it?.forEach { unit -> add(unit) }
        }
    }

    @Keep
    data class RetryConfig(
            @SerializedName("networks")
            val networks: String? = null,
            @SerializedName("retries")
            var retries: Int? = null,
            @SerializedName("retry_interval")
            val retryInterval: Int? = null,
            @SerializedName("adUnits")
            var adUnits: ArrayList<String> = arrayListOf()
    ) {

        @Keep
        fun fillAdUnits() {
            adUnits = arrayListOf<String>().apply {
                addAll(networks?.replace(" ", "")?.split(",") ?: arrayListOf())
            }
        }
    }

    @Keep
    data class InfoConfig(
            @SerializedName("normal_info")
            val normalInfo: Int? = null,
            @SerializedName("special_tag")
            val specialTag: String? = null,
            @SerializedName("refresh_callbacks")
            val refreshCallbacks: Int? = null
    )

    @Keep
    data class Prebid(
            @SerializedName("firstlook")
            val firstLook: Int? = null,
            @SerializedName("other")
            val other: Int? = null,
            @SerializedName("host")
            val host: String? = null,
            @SerializedName("accountid")
            val accountId: String? = null,
            @SerializedName("timeout")
            val timeout: String? = null,
            @SerializedName("debug")
            val debug: Int = 0,
            @SerializedName("schain")
            val schain: String? = null
    )

    @Keep
    data class GeoEdge(
            @SerializedName("firstlook")
            val firstLook: Int? = null,
            @SerializedName("other")
            val other: Int? = null,
            @SerializedName("api_key")
            val apiKey: String? = null,
            @SerializedName("creative_id")
            val creativeIds: String? = null,
            @SerializedName("reasons")
            val reasons: String? = null
    )

    @Keep
    data class RefreshConfig(
            @SerializedName("type")
            val type: String? = null,
            @SerializedName("name_type")
            val nameType: String? = null,
            @SerializedName("sizes")
            val sizes: List<Size>? = null,
            @SerializedName("follow")
            val follow: Int? = null,
            @SerializedName("pos")
            val position: Int? = null,
            @SerializedName("placement")
            val placement: Placement? = null,
            @SerializedName("specific")
            val specific: String? = null,
            @SerializedName("expiry")
            val expiry: Int? = null
    )

    @Keep
    data class Size(
            @SerializedName("width")
            val width: String? = null,
            @SerializedName("height")
            val height: String? = null,
            @SerializedName("sizes")
            val sizes: List<Size>? = null
    ) {
        @Keep
        fun toSizes(): String {
            return sizes?.joinToString(",") ?: ""
        }

        @Keep
        override fun toString(): String {
            return String.format("%s x %s", width, height)
        }
    }

    @Keep
    data class Placement(
            @SerializedName("firstlook")
            val firstLook: String? = null,
            @SerializedName("other")
            val other: String? = null
    )

    @Keep
    data class LoadConfigs(
            @SerializedName("INTERSTITIAL")
            val inter: LoadConfig? = null,
            @SerializedName("REWARDEDINTERSTITIAL")
            val reward: LoadConfig? = null,
            @SerializedName("REWARDED")
            val rewardVideos: LoadConfig? = null,
            @SerializedName("NATIVE")
            val native: LoadConfig? = null,
            @SerializedName("newunit")
            val newUnit: LoadConfig? = null,
            @SerializedName("BANNER")
            val banner: LoadConfig? = null,
            @SerializedName("ADAPTIVE")
            val adaptive: LoadConfig? = null,
            @SerializedName("INLINE")
            val inline: LoadConfig? = null,
            @SerializedName("INREAD")
            val inread: LoadConfig? = null,
            @SerializedName("STICKY")
            val sticky: LoadConfig? = null,
            @SerializedName("APPOPEN")
            val appOpen: LoadConfig? = null,
            @SerializedName("ALL", alternate = ["all"])
            val other: LoadConfig? = null
    )

    @Keep
    data class LoadConfig(
            @SerializedName("status")
            val status: Int? = null,
            @SerializedName("number")
            val number: Int? = null
    )
}