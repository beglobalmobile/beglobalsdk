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
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.OnShowAdCompleteListener
import com.rtb.beglobal.sdk.SDKConfig
import com.rtb.beglobal.sdk.log
import java.util.Date

class AppOpenAdManager(private val context: Context, private var adUnit: String) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var sdkConfig: SDKConfig? = null
    private val storeService = BeGlobal.getStoreService(context)
    private var appOpenCofig = AppOpenConfig()
    private var shouldBeActive: Boolean = false
    private var firstLook: Boolean = true
    private var overridingUnit: String? = null
    var fullScreenContentCallback: com.rtb.beglobal.sdk.FullScreenContentCallback? = null
    var isShowingAd = false

    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    private fun load(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }
        var adManagerAdRequest = createRequest().getAdRequest() ?: return
        shouldSetConfig {
            if (it) {
                setConfig()
                if (appOpenCofig.isNewUnit) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            context,
                            getAdUnitName(false, hijacked = false, newUnit = true),
                            adManagerAdRequest
                        )
                    }
                } else if (appOpenCofig.hijack?.status == 1) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(
                            context,
                            getAdUnitName(false, hijacked = true, newUnit = false),
                            adManagerAdRequest
                        )
                    }
                } else {
                    loadAd(context, adUnit, adManagerAdRequest)
                }
            } else {
                loadAd(context, adUnit, adManagerAdRequest)
            }
        }
    }

    private fun loadAd(context: Context, adUnit: String, adRequest: AdManagerAdRequest) {
        isLoadingAd = true
        AppOpenAd.load(context, adUnit, adRequest, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                LogLevel.INFO.log(msg = "AppOpen ad loaded")
                appOpenAd = ad
                isLoadingAd = false
                loadTime = Date().time
                appOpenCofig.retryConfig = sdkConfig?.retryConfig
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                LogLevel.ERROR.log(msg = loadAdError.message)
                isLoadingAd = false
                val tempStatus = firstLook
                if (firstLook) {
                    firstLook = false
                }
                try {
                    adFailedToLoad(context, tempStatus)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun adFailedToLoad(context: Context, firstLook: Boolean) {
        fun requestAd() {
            createRequest().getAdRequest()?.let {
                loadAd(
                    context,
                    getAdUnitName(unfilled = true, hijacked = false, newUnit = false),
                    it
                )
            }
        }
        if (appOpenCofig.unFilled?.status == 1) {
            if (firstLook) {
                requestAd()
            } else {
                if ((appOpenCofig.retryConfig?.retries ?: 0) > 0) {
                    appOpenCofig.retryConfig?.retries = (appOpenCofig.retryConfig?.retries ?: 0) - 1
                    Handler(Looper.getMainLooper()).postDelayed({
                        appOpenCofig.retryConfig?.adUnits?.firstOrNull()?.let {
                            appOpenCofig.retryConfig?.adUnits?.removeAt(0)
                            overridingUnit = it
                            requestAd()
                        } ?: kotlin.run {
                            overridingUnit = null
                        }
                    }, (appOpenCofig.retryConfig?.retryInterval ?: 0).toLong() * 1000)
                } else {
                    overridingUnit = null
                }
            }
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
            ) == true || config.type == AdTypes.APPOPEN || config.type == "all"
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
        appOpenCofig.apply {
            customUnitName = String.format(
                "/%s/%s-%s",
                networkName,
                sdkConfig?.affiliatedId.toString(),
                validConfig.nameType ?: ""
            )
            position = validConfig.position ?: 0
            isNewUnit = adUnit.contains(sdkConfig?.networkId ?: "")
            retryConfig = sdkConfig?.retryConfig.also { it?.fillAdUnits() }
            newUnit = sdkConfig?.hijackConfig?.newUnit
            hijack = sdkConfig?.hijackConfig?.appOpen ?: sdkConfig?.hijackConfig?.other
            unFilled = sdkConfig?.unfilledConfig?.appOpen ?: sdkConfig?.unfilledConfig?.other
            expriry = validConfig.expiry ?: 0
        }
    }

    private fun isAdAvailable(): Boolean {
        return if (appOpenCofig.expriry != 0) {
            appOpenAd != null && wasLoadTimeLessThanNHoursAgo(appOpenCofig.expriry.toLong())
        } else {
            appOpenAd != null
        }
    }

    private fun getAdUnitName(unfilled: Boolean, hijacked: Boolean, newUnit: Boolean): String {
        return overridingUnit ?: String.format(
            "%s-%d",
            appOpenCofig.customUnitName,
            if (unfilled) appOpenCofig.unFilled?.number else if (newUnit) appOpenCofig.newUnit?.number else if (hijacked) appOpenCofig.hijack?.number else appOpenCofig.position
        )
    }

    private fun createRequest() = AdRequest().Builder().apply {
        addCustomTargeting("adunit", adUnit)
        addCustomTargeting("hb_format", "amp")
    }.build()

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }


    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        if (isShowingAd) {
            LogLevel.INFO.log(msg = "The app open ad is already showing.")
            return
        }
        if (!isAdAvailable()) {
            LogLevel.ERROR.log(msg = "The app open ad is not ready yet.")
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
                LogLevel.ERROR.log(msg = adError.message)
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
}