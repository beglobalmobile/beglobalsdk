package com.rtb.beglobal.appopen

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.sdk.AdLoadCallback
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.OnShowAdCompleteListener
import com.rtb.beglobal.sdk.SDKConfig
import com.rtb.beglobal.sdk.log
import java.util.Date

class AppOpenAdManager(private val context: Context, adUnit: String?) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var sdkConfig: SDKConfig? = null
    private val storeService = BeGlobal.getStoreService(context)
    private var appOpenConfig = AppOpenConfig()
    private var shouldBeActive: Boolean = false
    private var firstLook: Boolean = true
    private var overridingUnit: String? = null
    private var loadingAdUnit: String? = adUnit
    var fullScreenContentCallback: com.rtb.beglobal.sdk.FullScreenContentCallback? = null
    var isShowingAd = false

    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    private fun load(context: Context, adLoadCallback: AdLoadCallback? = null) {
        if (isLoadingAd || isAdAvailable() || loadingAdUnit == null) {
            return
        }
        var adManagerAdRequest = createRequest().getAdRequest() ?: return
        shouldSetConfig {
            if (it) {
                setConfig()
                if (appOpenConfig.isNewUnit) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            context,
                            getAdUnitName(false, hijacked = false, newUnit = true),
                            adManagerAdRequest,
                            adLoadCallback
                        )
                    }
                } else if (appOpenConfig.hijack?.status == 1) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            context,
                            getAdUnitName(false, hijacked = true, newUnit = false),
                            adManagerAdRequest,
                            adLoadCallback
                        )
                    }
                } else {
                    loadAd(context, loadingAdUnit!!, adManagerAdRequest, adLoadCallback)
                }
            } else {
                loadAd(context, loadingAdUnit!!, adManagerAdRequest, adLoadCallback)
            }
        }
    }

    private fun loadAd(context: Context, adUnit: String, adRequest: AdManagerAdRequest, adLoadCallback: AdLoadCallback?) {
        isLoadingAd = true
        AppOpenAd.load(context, adUnit, adRequest, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                Logger.INFO.log(msg = "AppOpen ad loaded")
                appOpenAd = ad
                isLoadingAd = false
                loadTime = Date().time
                appOpenConfig.retryConfig = sdkConfig?.retryConfig.also { it?.fillAdUnits() }
                adLoadCallback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.ERROR.log(msg = loadAdError.message)
                isLoadingAd = false
                val tempStatus = firstLook
                if (firstLook) {
                    firstLook = false
                }
                try {
                    adFailedToLoad(context, tempStatus, adLoadCallback)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    adLoadCallback?.onAdFailedToLoad(loadAdError.message)
                }
            }
        })
    }

    private fun adFailedToLoad(context: Context, firstLook: Boolean, adLoadCallback: AdLoadCallback?) {
        fun requestAd() {
            createRequest().getAdRequest()?.let {
                loadAd(
                    context,
                    getAdUnitName(unfilled = true, hijacked = false, newUnit = false),
                    it,
                    adLoadCallback
                )
            }
        }
        if (appOpenConfig.unFilled?.status == 1) {
            if (firstLook) {
                requestAd()
            } else {
                adLoadCallback?.onAdFailedToLoad("")
                if ((appOpenConfig.retryConfig?.retries ?: 0) > 0) {
                    appOpenConfig.retryConfig?.retries = (appOpenConfig.retryConfig?.retries ?: 0) - 1
                    Handler(Looper.getMainLooper()).postDelayed({
                        appOpenConfig.retryConfig?.adUnits?.firstOrNull()?.let {
                            appOpenConfig.retryConfig?.adUnits?.removeAt(0)
                            overridingUnit = it
                            requestAd()
                        } ?: kotlin.run {
                            overridingUnit = null
                        }
                    }, (appOpenConfig.retryConfig?.retryInterval ?: 0).toLong() * 1000)
                } else {
                    overridingUnit = null
                }
            }
        } else {
            adLoadCallback?.onAdFailedToLoad("")
        }
    }


    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun shouldSetConfig(callback: (Boolean) -> Unit) {
        val workManager = BeGlobal.getWorkManager(context)
        val workers = workManager.getWorkInfosForUniqueWork(ConfigSetWorker::class.java.simpleName).get()
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
        if (sdkConfig?.getBlockList()?.contains(loadingAdUnit) == true) {
            shouldBeActive = false
            return
        }
        val validConfig = sdkConfig?.refreshConfig?.firstOrNull { config ->
            config.specific?.equals(
                loadingAdUnit,
                true
            ) == true || config.type == AdTypes.APPOPEN || config.type.equals("all", true)
        }
        if (validConfig == null) {
            shouldBeActive = false
            return
        }
        val networkName = if (sdkConfig?.networkCode.isNullOrEmpty()) sdkConfig?.networkId else String.format(
            "%s,%s",
            sdkConfig?.networkId,
            sdkConfig?.networkCode
        )
        appOpenConfig.apply {
            customUnitName = String.format(
                "/%s/%s-%s",
                networkName,
                sdkConfig?.affiliatedId.toString(),
                validConfig.nameType ?: ""
            )
            position = validConfig.position ?: 0
            isNewUnit = loadingAdUnit?.contains(sdkConfig?.networkId ?: "") ?: false
            retryConfig = sdkConfig?.retryConfig.also { it?.fillAdUnits() }
            newUnit = sdkConfig?.hijackConfig?.newUnit
            hijack = sdkConfig?.hijackConfig?.appOpen ?: sdkConfig?.hijackConfig?.other
            unFilled = sdkConfig?.unfilledConfig?.appOpen ?: sdkConfig?.unfilledConfig?.other
            expriry = validConfig.expiry ?: 0
        }
    }

    private fun isAdAvailable(): Boolean {
        return if (appOpenConfig.expriry != 0) {
            appOpenAd != null && wasLoadTimeLessThanNHoursAgo(appOpenConfig.expriry.toLong())
        } else {
            appOpenAd != null
        }
    }

    private fun getAdUnitName(unfilled: Boolean, hijacked: Boolean, newUnit: Boolean): String {
        return overridingUnit ?: String.format(
            "%s-%d",
            appOpenConfig.customUnitName,
            if (unfilled) appOpenConfig.unFilled?.number else if (newUnit) appOpenConfig.newUnit?.number else if (hijacked) appOpenConfig.hijack?.number else appOpenConfig.position
        )
    }

    private fun createRequest() = AdRequest().Builder().apply {
        addCustomTargeting("adunit", loadingAdUnit ?: "")
        addCustomTargeting("hb_format", "amp")
    }.build()

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }


    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        if (isShowingAd) {
            Logger.INFO.log(msg = "The app open ad is already showing.")
            return
        }
        if (!isAdAvailable()) {
            Logger.ERROR.log(msg = "The app open ad is not ready yet.")
            onShowAdCompleteListener.onShowAdComplete()
            load(activity)
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullScreenContentCallback?.onAdDismissedFullScreenContent()
                appOpenAd = null
                isShowingAd = false

                onShowAdCompleteListener.onShowAdComplete()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                fullScreenContentCallback?.onAdFailedToShowFullScreenContent(adError.message)
                Logger.ERROR.log(msg = adError.message)
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener.onShowAdComplete()
                load(activity)
            }

            override fun onAdShowedFullScreenContent() {
                fullScreenContentCallback?.onAdShowedFullScreenContent()
            }

            override fun onAdClicked() {
                fullScreenContentCallback?.onAdClicked()
            }

            override fun onAdImpression() {
                fullScreenContentCallback?.onAdImpression()
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

    fun prepareAd(adUnit: String?, adLoadCallback: AdLoadCallback) {
        adUnit?.let { loadingAdUnit = it }
        if (context is Activity) {
            load(context, adLoadCallback)
        } else {
            adLoadCallback.onAdFailedToLoad("")
        }
    }
}