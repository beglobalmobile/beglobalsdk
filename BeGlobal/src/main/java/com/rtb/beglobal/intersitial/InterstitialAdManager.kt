package com.rtb.beglobal.intersitial

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.SDKConfig
import com.rtb.beglobal.sdk.log
import org.prebid.mobile.InterstitialAdUnit

internal class InterstitialAdManager(private val context: Activity, private val adUnit: String) {

    private var sdkConfig: SDKConfig? = null
    private var interstitialConfig: InterstitialConfig = InterstitialConfig()
    private var shouldBeActive: Boolean = false
    private val storeService = BeGlobal.getStoreService(context)
    private var firstLook: Boolean = true

    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    fun load(adRequest: AdRequest, callBack: (interstitialAd: AdManagerInterstitialAd?) -> Unit) {
        var adManagerAdRequest = adRequest.getAdRequest()
        if (adManagerAdRequest == null) {
            callBack(null)
            return
        }
        shouldSetConfig {
            if (it) {
                setConfig()
                if (interstitialConfig.isNewUnit) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            getAdUnitName(false, hijacked = false, newUnit = true),
                            request,
                            callBack
                        )
                    }
                } else if (interstitialConfig.hijack?.status == 1) {
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
        callBack: (interstitialAd: AdManagerInterstitialAd?) -> Unit
    ) {
        fetchDemand(adRequest) {
            AdManagerInterstitialAd.load(
                context,
                adUnit,
                adRequest,
                object : AdManagerInterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                        firstLook = false
                        callBack(interstitialAd)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        LogLevel.ERROR.log(adError.message)
                        if (firstLook) {
                            firstLook = false
                            val request = createRequest().getAdRequest()
                            if (interstitialConfig.unFilled?.status == 1 && request != null) {
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
            ) == true || config.type == AdTypes.INTERSTITIAL || config.type == "all"
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
        interstitialConfig.apply {
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
            hijack = sdkConfig?.hijackConfig?.inter ?: sdkConfig?.hijackConfig?.other
            unFilled = sdkConfig?.unfilledConfig?.inter ?: sdkConfig?.unfilledConfig?.other
        }
    }

    private fun getAdUnitName(unfilled: Boolean, hijacked: Boolean, newUnit: Boolean): String {
        return String.format(
            "%s-%d",
            interstitialConfig.customUnitName,
            if (unfilled) interstitialConfig.unFilled?.number else if (newUnit) interstitialConfig.newUnit?.number else if (hijacked) interstitialConfig.hijack?.number else interstitialConfig.position
        )
    }

    private fun createRequest() = AdRequest().Builder().apply {
        addCustomTargeting("adunit", adUnit)
        addCustomTargeting("hb_format", "amp")
    }.build()

    private fun fetchDemand(adRequest: AdManagerAdRequest, callback: () -> Unit) {
        if (sdkConfig?.prebid?.other != 1) {
            callback()
        } else {
            val adUnit =
                InterstitialAdUnit((interstitialConfig.placement?.other ?: 0).toString(), 50, 70)
            adUnit.fetchDemand(adRequest) { callback() }
        }
    }
}