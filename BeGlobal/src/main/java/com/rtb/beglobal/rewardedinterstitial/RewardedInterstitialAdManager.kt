package com.rtb.beglobal.rewardedinterstitial

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.intersitial.InterstitialConfig
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.SDKConfig
import com.rtb.beglobal.sdk.log
import org.prebid.mobile.InterstitialAdUnit
import org.prebid.mobile.api.data.AdUnitFormat
import java.util.EnumSet

internal class RewardedInterstitialAdManager(
    private val context: Activity,
    private val adUnit: String
) {

    private var sdkConfig: SDKConfig? = null
    private var config: InterstitialConfig = InterstitialConfig()
    private var shouldBeActive: Boolean = false
    private val storeService = BeGlobal.getStoreService(context)
    private var firstLook: Boolean = true

    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    fun load(adRequest: AdRequest, callBack: (interstitialAd: RewardedInterstitialAd?) -> Unit) {
        var adManagerAdRequest = adRequest.getAdRequest()
        if (adManagerAdRequest == null) {
            callBack(null)
            return
        }
        shouldSetConfig {
            if (it) {
                setConfig()
                if (config.isNewUnit) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            getAdUnitName(false, hijacked = false, newUnit = true),
                            request,
                            callBack
                        )
                    }
                } else if (config.hijack?.status == 1) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            getAdUnitName(false, hijacked = true, newUnit = false),
                            request,
                            callBack
                        )
                    }
                } else {
                    loadAd(adUnit, adManagerAdRequest!!, callBack)
                }
            } else {
                loadAd(adUnit, adManagerAdRequest!!, callBack)
            }
        }
    }

    private fun loadAd(
        adUnit: String,
        adRequest: AdManagerAdRequest,
        callBack: (interstitialAd: RewardedInterstitialAd?) -> Unit
    ) {
        fetchDemand(adRequest) {
            RewardedInterstitialAd.load(
                context,
                adUnit,
                adRequest,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        firstLook = false
                        callBack(ad)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        LogLevel.ERROR.log(adError.message)
                        if (firstLook) {
                            firstLook = false
                            val request = createRequest().getAdRequest()
                            if (config.unFilled?.status == 1 && request != null) {
                                loadAd(
                                    getAdUnitName(
                                        unfilled = true,
                                        hijacked = false,
                                        newUnit = false
                                    ), request, callBack
                                )
                            }
                        } else {
                            callBack(null)
                        }
                    }
                })
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun shouldSetConfig(callback: (Boolean) -> Unit) {
        val workManager = BeGlobal.getWorkManager(context)
        val workers =
            workManager.getWorkInfosForUniqueWork(ConfigSetWorker::class.java.simpleName).get()
        if (workers.isNullOrEmpty()) {
            callback(false)
        } else {
            try {
                val workerData = workManager.getWorkInfoByIdLiveData(workers[0].id)
                workerData?.observeForever(object : Observer<WorkInfo> {
                    @Suppress("UNNECESSARY_SAFE_CALL")
                    override fun onChanged(value: WorkInfo) {
                        if (value?.state != WorkInfo.State.RUNNING && value?.state != WorkInfo.State.ENQUEUED) {
                            workerData.removeObserver(this)
                            sdkConfig = storeService.config
                            shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
                            callback(shouldBeActive)
                        }
                    }
                })
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun setConfig() {
        if (!shouldBeActive) return
        if (sdkConfig?.getBlockList()?.contains(adUnit) == true) {
            shouldBeActive = false
            return
        }
        val validConfig = sdkConfig?.refreshConfig?.firstOrNull { config ->
            config.specific?.equals(
                adUnit,
                true
            ) == true || config.type == AdTypes.REWARDEDINTERSTITIAL || config.type == "all"
        }
        if (validConfig == null) {
            shouldBeActive = false
            return
        }
        val networkName =
            if (sdkConfig?.networkCode.isNullOrEmpty()) sdkConfig?.networkId else String.format(
                "%s,%s",
                sdkConfig?.networkId,
                sdkConfig?.networkCode
            )
        config.apply {
            customUnitName = String.format(
                "/%s/%s-%s",
                networkName,
                sdkConfig?.affiliatedId.toString(),
                validConfig.nameType ?: ""
            )
            position = validConfig.position ?: 0
            isNewUnit = adUnit.contains(sdkConfig?.networkId ?: "")
            placement = validConfig.placement
            newUnit = sdkConfig?.hijackConfig?.newUnit
            hijack = sdkConfig?.hijackConfig?.reward ?: sdkConfig?.hijackConfig?.other
            unFilled = sdkConfig?.unfilledConfig?.reward ?: sdkConfig?.unfilledConfig?.other
        }
    }

    private fun getAdUnitName(unfilled: Boolean, hijacked: Boolean, newUnit: Boolean): String {
        return String.format(
            "%s-%d",
            config.customUnitName,
            if (unfilled) config.unFilled?.number else if (newUnit) config.newUnit?.number else if (hijacked) config.hijack?.number else config.position
        )
    }

    private fun createRequest() = AdRequest().Builder().apply {
        addCustomTargeting("adunit", adUnit)
        addCustomTargeting("hb_format", "video")
    }.build()

    private fun fetchDemand(adRequest: AdManagerAdRequest, callback: () -> Unit) {
        if (sdkConfig?.prebid?.other != 1) {
            callback()
        } else {
            val adUnit = InterstitialAdUnit(
                (config.placement?.other ?: 0).toString(),
                EnumSet.of(AdUnitFormat.VIDEO)
            )
            adUnit.fetchDemand(adRequest) { callback() }
        }
    }
}