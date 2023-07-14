package com.rtb.beglobal.native

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.appharbr.sdk.engine.AdBlockReason
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AppHarbr
import com.appharbr.sdk.engine.adformat.AdFormat
import com.appharbr.sdk.engine.listeners.AHIncident
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.gson.Gson
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.intersitial.InterstitialConfig
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.SDKConfig
import com.rtb.beglobal.sdk.log
import org.prebid.mobile.NativeAdUnit
import org.prebid.mobile.NativeDataAsset
import org.prebid.mobile.NativeEventTracker
import org.prebid.mobile.NativeImageAsset
import org.prebid.mobile.NativeTitleAsset

class NativeAdManager(private val context: Activity, private val adUnit: String) {

    private var sdkConfig: SDKConfig? = null
    private var nativeConfig: InterstitialConfig = InterstitialConfig()
    private var shouldBeActive: Boolean = false
    private val storeService = BeGlobal.getStoreService(context)
    private var firstLook: Boolean = true
    private var overridingUnit: String? = null
    private var otherUnit = false
    private var adListener: AdListener? = null
    private var adOptions = NativeAdOptions.Builder().build()
    private var loadCount: Int = 0
    private lateinit var adLoader: AdLoader

    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    fun setAdListener(adListener: AdListener) {
        this.adListener = adListener
    }

    fun setNativeAdOptions(adOptions: NativeAdOptions) {
        this.adOptions = adOptions
    }

    fun setLoadCount(count: Int) {
        this.loadCount = count
    }

    fun isLoading(): Boolean {
        return this::adLoader.isInitialized && adLoader.isLoading
    }

    fun load(adRequest: AdRequest, callBack: (nativeAd: NativeAd?) -> Unit) {
        var adManagerAdRequest = adRequest.getAdRequest()
        if (adManagerAdRequest == null) {
            callBack(null)
            return
        }
        shouldSetConfig {
            if (it) {
                setConfig()
                if (nativeConfig.isNewUnit && nativeConfig.newUnit?.status == 1) {
                    createRequest().getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(getAdUnitName(false, hijacked = false, newUnit = true), request, callBack)
                    }
                } else if (nativeConfig.hijack?.status == 1) {
                    createRequest(hijacked = true).getAdRequest()?.let { request ->
                        adManagerAdRequest = request
                        loadAd(getAdUnitName(false, hijacked = true, newUnit = false), request, callBack)
                    }
                } else {
                    loadAd(adUnit, adManagerAdRequest!!, callBack)
                }
            } else {
                loadAd(adUnit, adManagerAdRequest!!, callBack)
            }
        }
    }

    private fun loadAd(adUnit: String, adRequest: AdManagerAdRequest, callBack: (nativeAd: NativeAd?) -> Unit) {
        otherUnit = adUnit != this.adUnit
        fetchDemand(adRequest) {
            adLoader = AdLoader.Builder(context, adUnit)
                    .forNativeAd { nativeAd: NativeAd ->
                        nativeConfig.retryConfig = sdkConfig?.retryConfig.also { it?.fillAdUnits() }
                        addGeoEdge(nativeAd, otherUnit)
                        callBack(nativeAd)
                        firstLook = false
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdClicked() {
                            adListener?.onAdClicked()
                        }

                        override fun onAdClosed() {
                            adListener?.onAdClosed()
                        }

                        override fun onAdImpression() {
                            adListener?.onAdImpression()
                        }

                        override fun onAdLoaded() {
                            adListener?.onAdLoaded()
                        }

                        override fun onAdOpened() {
                            adListener?.onAdOpened()
                        }

                        override fun onAdSwipeGestureClicked() {
                            adListener?.onAdSwipeGestureClicked()
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Logger.ERROR.log(msg = adError.message)
                            val tempStatus = firstLook
                            if (firstLook) {
                                firstLook = false
                            }
                            try {
                                adFailedToLoad(tempStatus, callBack, adError)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                callBack(null)
                                adListener?.onAdFailedToLoad(adError)
                            }
                        }
                    })
                    .withNativeAdOptions(adOptions)
                    .build()
            if (loadCount == 0) {
                adLoader.loadAd(adRequest)
            } else {
                adLoader.loadAds(adRequest, loadCount)
            }
        }
    }

    private fun adFailedToLoad(firstLook: Boolean, callBack: (nativeAd: NativeAd?) -> Unit, adError: LoadAdError) {
        fun requestAd() {
            createRequest(unfilled = true).getAdRequest()?.let {
                loadAd(getAdUnitName(unfilled = true, hijacked = false, newUnit = false), it, callBack)
            }
        }
        if (nativeConfig.unFilled?.status == 1) {
            if (firstLook) {
                requestAd()
            } else {
                if ((nativeConfig.retryConfig?.retries ?: 0) > 0) {
                    nativeConfig.retryConfig?.retries = (nativeConfig.retryConfig?.retries ?: 0) - 1
                    Handler(Looper.getMainLooper()).postDelayed({
                        nativeConfig.retryConfig?.adUnits?.firstOrNull()?.let {
                            nativeConfig.retryConfig?.adUnits?.removeAt(0)
                            overridingUnit = it
                            requestAd()
                        } ?: kotlin.run {
                            overridingUnit = null
                            callBack(null)
                            adListener?.onAdFailedToLoad(adError)
                        }
                    }, (nativeConfig.retryConfig?.retryInterval ?: 0).toLong() * 1000)
                } else {
                    overridingUnit = null
                    callBack(null)
                    adListener?.onAdFailedToLoad(adError)
                }
            }
        } else {
            callBack(null)
            adListener?.onAdFailedToLoad(adError)
        }
    }

    private fun addGeoEdge(nativeAd: NativeAd, otherUnit: Boolean) {
        try {
            val number = (1..100).random()
            if ((!otherUnit && (number in 1..(sdkConfig?.geoEdge?.firstLook ?: 0))) ||
                    (otherUnit && (number in 1..(sdkConfig?.geoEdge?.other ?: 0)))) {
                AppHarbr.addInterstitial(AdSdk.GAM, nativeAd, object : AHIncident {
                    override fun onAdBlocked(p0: Any?, p1: String?, p2: AdFormat, reasons: Array<out AdBlockReason>) {
                        log { "Native : onAdBlocked : ${Gson().toJson(reasons.asList().map { it.reason })}" }
                    }

                    override fun onAdIncident(p0: Any?, p1: String?, p2: AdSdk?, p3: String?, p4: AdFormat, p5: Array<out AdBlockReason>, reportReasons: Array<out AdBlockReason>) {
                        log { "Native: onAdIncident : ${Gson().toJson(reportReasons.asList().map { it.reason })}" }
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        if (sdkConfig?.getBlockList()?.contains(adUnit) == true) {
            shouldBeActive = false
            return
        }
        val validConfig = sdkConfig?.refreshConfig?.firstOrNull { config -> config.specific?.equals(adUnit, true) == true || config.type == AdTypes.NATIVE || config.type.equals("all", true) }
        if (validConfig == null) {
            shouldBeActive = false
            return
        }
        val networkName = if (sdkConfig?.networkCode.isNullOrEmpty()) sdkConfig?.networkId else String.format("%s,%s", sdkConfig?.networkId, sdkConfig?.networkCode)
        nativeConfig.apply {
            customUnitName = String.format("/%s/%s-%s", networkName, sdkConfig?.affiliatedId.toString(), validConfig.nameType ?: "")
            position = validConfig.position ?: 0
            isNewUnit = adUnit.contains(sdkConfig?.networkId ?: "")
            placement = validConfig.placement
            newUnit = sdkConfig?.hijackConfig?.newUnit
            retryConfig = sdkConfig?.retryConfig.also { it?.fillAdUnits() }
            hijack = sdkConfig?.hijackConfig?.inter ?: sdkConfig?.hijackConfig?.other
            unFilled = sdkConfig?.unfilledConfig?.inter ?: sdkConfig?.unfilledConfig?.other
        }
    }

    private fun getAdUnitName(unfilled: Boolean, hijacked: Boolean, newUnit: Boolean): String {
        return overridingUnit ?: String.format("%s-%d", nativeConfig.customUnitName, if (unfilled) nativeConfig.unFilled?.number else if (newUnit) nativeConfig.newUnit?.number else if (hijacked) nativeConfig.hijack?.number else nativeConfig.position)
    }

    private fun createRequest(unfilled: Boolean = false, hijacked: Boolean = false) = AdRequest().Builder().apply {
        addCustomTargeting("adunit", adUnit)
        if (unfilled) addCustomTargeting("retry", "1")
        if (hijacked) addCustomTargeting("hijack", "1")
    }.build()


    private fun fetchDemand(adRequest: AdManagerAdRequest, callback: () -> Unit) {
        if ((!otherUnit && sdkConfig?.prebid?.firstLook == 1) || (otherUnit && sdkConfig?.prebid?.other == 1)) {
            val adUnit = NativeAdUnit((if (otherUnit) nativeConfig.placement?.other ?: 0 else nativeConfig.placement?.firstLook ?: 0).toString())
            adUnit.setContextType(NativeAdUnit.CONTEXT_TYPE.SOCIAL_CENTRIC)
            adUnit.setPlacementType(NativeAdUnit.PLACEMENTTYPE.CONTENT_FEED)
            adUnit.setContextSubType(NativeAdUnit.CONTEXTSUBTYPE.GENERAL_SOCIAL)
            addNativeAssets(adUnit)
            adUnit.fetchDemand(adRequest) { callback() }
        } else {
            callback()
        }
    }

    private fun addNativeAssets(adUnit: NativeAdUnit?) {
        // ADD NATIVE ASSETS

        val title = NativeTitleAsset()
        title.setLength(90)
        title.isRequired = true
        adUnit?.addAsset(title)

        val icon = NativeImageAsset(20, 20, 20, 20)
        icon.imageType = NativeImageAsset.IMAGE_TYPE.ICON
        icon.isRequired = true
        adUnit?.addAsset(icon)

        val image = NativeImageAsset(200, 200, 200, 200)
        image.imageType = NativeImageAsset.IMAGE_TYPE.MAIN
        image.isRequired = true
        adUnit?.addAsset(image)

        val data = NativeDataAsset()
        data.len = 90
        data.dataType = NativeDataAsset.DATA_TYPE.SPONSORED
        data.isRequired = true
        adUnit?.addAsset(data)

        val body = NativeDataAsset()
        body.isRequired = true
        body.dataType = NativeDataAsset.DATA_TYPE.DESC
        adUnit?.addAsset(body)

        val cta = NativeDataAsset()
        cta.isRequired = true
        cta.dataType = NativeDataAsset.DATA_TYPE.CTATEXT
        adUnit?.addAsset(cta)

        // ADD NATIVE EVENT TRACKERS
        val methods = ArrayList<NativeEventTracker.EVENT_TRACKING_METHOD>()
        methods.add(NativeEventTracker.EVENT_TRACKING_METHOD.IMAGE)
        methods.add(NativeEventTracker.EVENT_TRACKING_METHOD.JS)
        try {
            val tracker = NativeEventTracker(NativeEventTracker.EVENT_TYPE.IMPRESSION, methods)
            adUnit?.addEventTracker(tracker)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}