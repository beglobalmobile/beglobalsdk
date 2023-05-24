package com.rtb.beglobal.banners

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.sdk.BannerManagerListener
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.ConfigSetWorker
import com.rtb.beglobal.sdk.SDKConfig
import org.prebid.mobile.BannerAdUnit
import java.util.Date
import kotlin.math.ceil

internal class BannerManager(
    private val context: Context,
    private val bannerListener: BannerManagerListener
) {

    private var activeTimeCounter: CountDownTimer? = null
    private var passiveTimeCounter: CountDownTimer? = null
    private var unfilledRefreshCounter: CountDownTimer? = null
    private var bannerConfig = BannerConfig()
    private var sdkConfig: SDKConfig? = null
    private var shouldBeActive: Boolean = false
    private var wasFirstLook = true
    private val storeService = BeGlobal.getStoreService(context)
    private var isForegroundRefresh = 1
    private var overridingUnit: String? = null


    init {
        sdkConfig = storeService.config
        shouldBeActive = !(sdkConfig == null || sdkConfig?.switch != 1)
    }

    private fun shouldBeActive() = shouldBeActive

    fun convertStringSizesToAdSizes(adSizes: String): ArrayList<AdSize> {
        fun getAdSizeObj(adSize: String) = when (adSize) {
            "BANNER" -> AdSize.BANNER
            "LARGE_BANNER" -> AdSize.LARGE_BANNER
            "MEDIUM_RECTANGLE" -> AdSize.MEDIUM_RECTANGLE
            "FULL_BANNER" -> AdSize.FULL_BANNER
            "LEADERBOARD" -> AdSize.LEADERBOARD
            else -> {
                val w = adSize.replace(" ", "").substring(0, adSize.indexOf("x")).toIntOrNull() ?: 0
                val h = adSize.replace(" ", "").substring(adSize.indexOf("x") + 1, adSize.length)
                    .toIntOrNull() ?: 0
                AdSize(w, h)
            }
        }

        return ArrayList<AdSize>().apply {
            for (adSize in adSizes.replace(" ", "").split(",")) {
                add(getAdSizeObj(adSize))
            }
        }
    }

    fun convertVaragsToAdSizes(vararg adSizes: BannerAdSize): ArrayList<AdSize> {
        val adSizeList = arrayListOf<AdSize>()
        adSizes.toList().forEach {
            adSizeList.add(it.adSize)
        }
        return adSizeList
    }

    fun clearConfig() {
        storeService.config = null
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    fun shouldSetConfig(callback: (Boolean) -> Unit) {
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

    fun setConfig(pubAdUnit: String, adSizes: ArrayList<AdSize>, adType: String) {
        if (!shouldBeActive()) return
        if (sdkConfig?.getBlockList()?.any { pubAdUnit.contains(it) } == true) {
            shouldBeActive = false
            return
        }
        val validConfig = sdkConfig?.refreshConfig?.firstOrNull { config ->
            config.specific?.equals(
                pubAdUnit,
                true
            ) == true || config.type == adType || config.type == "all"
        }
        if (validConfig == null) {
            shouldBeActive = false
            return
        }
        bannerConfig.apply {
            customUnitName = String.format(
                "/%s/%s-%s",
                getNetworkName(),
                sdkConfig?.affiliatedId.toString(),
                validConfig.nameType ?: ""
            )
            isNewUnit = pubAdUnit.contains(sdkConfig?.networkId ?: "")
            publisherAdUnit = pubAdUnit
            position = validConfig.position ?: 0
            placement = validConfig.placement
            newUnit = sdkConfig?.hijackConfig?.newUnit
            retryConfig = sdkConfig?.retryConfig?.also { it.fillAdUnits() }
            hijack = getValidLoadConfig(adType, true)
            unFilled = getValidLoadConfig(adType, false)
            difference = sdkConfig?.difference ?: 0
            activeRefreshInterval = sdkConfig?.activeRefreshInterval ?: 0
            passiveRefreshInterval = sdkConfig?.passiveRefreshInterval ?: 0
            factor = sdkConfig?.factor ?: 0
            minView = sdkConfig?.minView ?: 0
            minViewRtb = sdkConfig?.minViewRtb ?: 0
            this.adSizes = if (validConfig.follow == 1 && !validConfig.sizes.isNullOrEmpty()) {
                getCustomSizes(adSizes, validConfig.sizes)
            } else {
                adSizes
            }
        }
    }

    private fun getNetworkName(): String? {
        return if (sdkConfig?.networkCode.isNullOrEmpty()) sdkConfig?.networkId else String.format(
            "%s,%s",
            sdkConfig?.networkId,
            sdkConfig?.networkCode
        )
    }

    private fun getValidLoadConfig(adType: String, forHijack: Boolean): SDKConfig.LoadConfig? {
        var validConfig = when {
            adType.equals(
                AdTypes.BANNER,
                true
            ) -> if (forHijack) sdkConfig?.hijackConfig?.banner else sdkConfig?.unfilledConfig?.banner

            adType.equals(
                AdTypes.INLINE,
                true
            ) -> if (forHijack) sdkConfig?.hijackConfig?.inline else sdkConfig?.unfilledConfig?.inline

            adType.equals(
                AdTypes.ADAPTIVE,
                true
            ) -> if (forHijack) sdkConfig?.hijackConfig?.adaptive else sdkConfig?.unfilledConfig?.adaptive

            adType.equals(
                AdTypes.INREAD,
                true
            ) -> if (forHijack) sdkConfig?.hijackConfig?.inread else sdkConfig?.unfilledConfig?.inread

            adType.equals(
                AdTypes.STICKY,
                true
            ) -> if (forHijack) sdkConfig?.hijackConfig?.sticky else sdkConfig?.unfilledConfig?.sticky

            else -> if (forHijack) sdkConfig?.hijackConfig?.other else sdkConfig?.unfilledConfig?.other
        }
        if (validConfig == null) {
            validConfig =
                if (forHijack) sdkConfig?.hijackConfig?.other else sdkConfig?.unfilledConfig?.other
        }
        return validConfig
    }

    private fun getCustomSizes(
        adSizes: ArrayList<AdSize>,
        sizeOptions: List<SDKConfig.Size>
    ): List<AdSize> {
        val sizes = ArrayList<AdSize>()
        adSizes.forEach {
            val lookingWidth = if (it.width != 0) it.width.toString() else "ALL"
            val lookingHeight = if (it.height != 0) it.height.toString() else "ALL"
            sizeOptions.firstOrNull { size -> size.height == lookingHeight && size.width == lookingWidth }?.sizes?.forEach { selectedSize ->
                if (selectedSize.width == "ALL" || selectedSize.height == "ALL") {
                    sizes.add(it)
                } else if (sizes.none { size ->
                        size.width == (selectedSize.width?.toIntOrNull()
                            ?: 0) && size.height == (selectedSize.height?.toIntOrNull() ?: 0)
                    }) {
                    sizes.add(
                        AdSize(
                            (selectedSize.width?.toIntOrNull() ?: 0),
                            (selectedSize.height?.toIntOrNull() ?: 0)
                        )
                    )
                }
            }
        }
        return sizes
    }

    fun saveVisibility(visible: Boolean) {
        if (visible == bannerConfig.isVisible) return
        bannerConfig.isVisible = visible
    }

    fun adFailedToLoad(isPublisherLoad: Boolean) {
        if (bannerConfig.unFilled?.status == 1) {
            startUnfilledRefreshCounter(sdkConfig?.passiveRefreshInterval?.toLong() ?: 0L)
            if (isPublisherLoad) {
                refresh(unfilled = true)
            } else {
                if ((bannerConfig.retryConfig?.retries ?: 0) > 0) {
                    bannerConfig.retryConfig?.retries = (bannerConfig.retryConfig?.retries ?: 0) - 1
                    Handler(Looper.getMainLooper()).postDelayed({
                        bannerConfig.retryConfig?.adUnits?.firstOrNull()?.let {
                            bannerConfig.retryConfig?.adUnits?.removeAt(0)
                            overridingUnit = it
                            refresh(unfilled = true)
                        } ?: kotlin.run {
                            overridingUnit = null
                        }
                    }, (bannerConfig.retryConfig?.retryInterval ?: 0).toLong() * 1000)
                } else {
                    overridingUnit = null
                }
            }
        }
    }

    fun adLoaded(firstLook: Boolean, loadedAdapter: AdapterResponseInfo?) {
        if (sdkConfig?.switch == 1) {
            overridingUnit = null
            bannerConfig.retryConfig = sdkConfig?.retryConfig
            unfilledRefreshCounter?.cancel()
            val blockedTerms = sdkConfig?.networkBlock?.replace(" ", "")?.split(",") ?: listOf()
            var isNetworkBlocked = false
            blockedTerms.forEach {
                if (it.isNotEmpty() && loadedAdapter?.adapterClassName?.contains(
                        it,
                        true
                    ) == true
                ) {
                    isNetworkBlocked = true
                }
            }
            if (!isNetworkBlocked
                && !(!loadedAdapter?.adSourceId.isNullOrEmpty() && blockedTerms.contains(
                    loadedAdapter?.adSourceId
                ))
                && !(!loadedAdapter?.adSourceName.isNullOrEmpty() && blockedTerms.contains(
                    loadedAdapter?.adSourceName
                ))
                && !(!loadedAdapter?.adSourceInstanceId.isNullOrEmpty() && blockedTerms.contains(
                    loadedAdapter?.adSourceInstanceId
                ))
                && !(!loadedAdapter?.adSourceInstanceName.isNullOrEmpty() && blockedTerms.contains(
                    loadedAdapter?.adSourceInstanceName
                ))
            ) {
                startRefreshing(resetVisibleTime = true, isPublisherLoad = firstLook)
            }
        }
    }

    private fun startRefreshing(
        resetVisibleTime: Boolean = false,
        isPublisherLoad: Boolean = false,
        timers: Int? = null
    ) {
        if (resetVisibleTime) {
            bannerConfig.isVisibleFor = 0
        }
        this.wasFirstLook = isPublisherLoad
        bannerConfig.let {
            timers?.let { active ->
                if (active == 1) startActiveCounter(it.activeRefreshInterval.toLong())
                else startPassiveCounter(it.passiveRefreshInterval.toLong())
            } ?: kotlin.run {
                startPassiveCounter(it.passiveRefreshInterval.toLong())
                startActiveCounter(it.activeRefreshInterval.toLong())
            }
        }
    }

    private fun startActiveCounter(seconds: Long) {
        activeTimeCounter?.cancel()
        if (seconds <= 0) return
        activeTimeCounter = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (bannerConfig.isVisible) {
                    bannerConfig.isVisibleFor++
                }
                bannerConfig.activeRefreshInterval--
            }

            override fun onFinish() {
                bannerConfig.activeRefreshInterval = sdkConfig?.activeRefreshInterval ?: 0
                refresh(1)
            }
        }
        activeTimeCounter?.start()
    }

    private fun startPassiveCounter(seconds: Long) {
        passiveTimeCounter?.cancel()
        if (seconds <= 0) return
        passiveTimeCounter = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                bannerConfig.passiveRefreshInterval--
            }

            override fun onFinish() {
                bannerConfig.passiveRefreshInterval = sdkConfig?.passiveRefreshInterval ?: 0
                refresh(0)
            }
        }
        passiveTimeCounter?.start()
    }

    private fun startUnfilledRefreshCounter(seconds: Long) {
        unfilledRefreshCounter?.cancel()
        if (seconds <= 0) return
        unfilledRefreshCounter = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                refresh(0, true)
            }
        }
        unfilledRefreshCounter?.start()
    }

    fun refresh(active: Int = 1, unfilled: Boolean = false) {
        val currentTimeStamp = Date().time
        val differenceOfLastRefresh =
            ceil((currentTimeStamp - bannerConfig.lastRefreshAt).toDouble() / 1000.00).toInt()
        val timers = if (active == 0 && unfilled) {
            null
        } else {
            active
        }

        fun refreshAd() {
            bannerConfig.lastRefreshAt = currentTimeStamp
            bannerListener.attachAdView(getAdUnitName(unfilled, false), bannerConfig.adSizes)
            loadAd(active, unfilled)
        }
        if (isForegroundRefresh == 0 && bannerConfig.factor < 0) {
            startRefreshing(timers = timers)
        } else {
            if (unfilled || ((bannerConfig.isVisible || (differenceOfLastRefresh >= (if (active == 1) bannerConfig.activeRefreshInterval else bannerConfig.passiveRefreshInterval) * bannerConfig.factor))
                        && differenceOfLastRefresh >= bannerConfig.difference && (bannerConfig.isVisibleFor >= (if (wasFirstLook || bannerConfig.isNewUnit) bannerConfig.minView else bannerConfig.minViewRtb)))
            ) {
                refreshAd()
            } else {
                startRefreshing(timers = timers)
            }
        }
    }

    private fun createRequest(active: Int, unfilled: Boolean = false) =
        AdRequest().Builder().apply {
            addCustomTargeting("adunit", bannerConfig.publisherAdUnit)
            addCustomTargeting("active", active.toString())
            addCustomTargeting("refresh", bannerConfig.refreshCount.toString())
            addCustomTargeting("hb_format", "amp")
            addCustomTargeting("visible", isForegroundRefresh.toString())
            addCustomTargeting(
                "min_view",
                (if (bannerConfig.isVisibleFor > 10) 10 else bannerConfig.isVisibleFor).toString()
            )
            addCustomTargeting("retry", (if (unfilled) 1 else 0).toString())
        }.build()

    private fun loadAd(active: Int, unfilled: Boolean) {
        if (bannerConfig.refreshCount < 10) {
            bannerConfig.refreshCount++
        } else {
            bannerConfig.refreshCount = 10
        }
        bannerListener.loadAd(createRequest(active, unfilled))
    }

    fun checkOverride(): AdManagerAdRequest? {
        if (bannerConfig.isNewUnit && bannerConfig.newUnit?.status == 1) {
            bannerListener.attachAdView(
                getAdUnitName(
                    unfilled = false,
                    hijacked = false,
                    newUnit = true
                ), bannerConfig.adSizes
            )
            return createRequest(1).getAdRequest()
        } else if (bannerConfig.hijack?.status == 1) {
            bannerListener.attachAdView(
                getAdUnitName(
                    unfilled = false,
                    hijacked = true,
                    newUnit = false
                ), bannerConfig.adSizes
            )
            return createRequest(1).getAdRequest()
        }
        return null
    }

    fun checkGeoEdge(firstLook: Boolean, callback: () -> Unit) {
        val number = (1..100).random()
        if ((firstLook && (number in 1..(sdkConfig?.geoEdge?.firstLook ?: 0))) ||
            (!firstLook && (number in 1..(sdkConfig?.geoEdge?.other ?: 0)))
        ) {
            callback()
        }
    }

    fun fetchDemand(firstLook: Boolean, adRequest: AdManagerAdRequest, callback: () -> Unit) {
        if ((firstLook && sdkConfig?.prebid?.firstLook == 1) || ((bannerConfig.isNewUnit || !firstLook) && sdkConfig?.prebid?.other == 1)) {
            bannerConfig.placement?.let {
                if (bannerConfig.adSizes.isNotEmpty()) {
                    val totalSizes = (bannerConfig.adSizes as ArrayList<AdSize>)
                    val firstAdSize = totalSizes[0]
                    val adUnit = BannerAdUnit(
                        if (firstLook) it.firstLook ?: "" else it.other ?: "",
                        firstAdSize.width,
                        firstAdSize.height
                    )
                    totalSizes.forEach { adSize ->
                        adUnit.addAdditionalSize(
                            adSize.width,
                            adSize.height
                        )
                    }
                    adUnit.fetchDemand(adRequest) { callback() }
                }
            } ?: callback()
        } else {
            callback()
        }
    }

    private fun getAdUnitName(
        unfilled: Boolean,
        hijacked: Boolean,
        newUnit: Boolean = false
    ): String {
        return overridingUnit ?: String.format(
            "%s-%d", bannerConfig.customUnitName,
            if (unfilled) bannerConfig.unFilled?.number else if (newUnit) bannerConfig.newUnit?.number else if (hijacked) bannerConfig.hijack?.number else bannerConfig.position
        )
    }

    fun adPaused() {
        isForegroundRefresh = 0
        activeTimeCounter?.cancel()
    }

    fun adResumed() {
        isForegroundRefresh = 1
        if (bannerConfig.adSizes.isNotEmpty()) {
            startActiveCounter(bannerConfig.activeRefreshInterval.toLong())
        }
    }

    fun adDestroyed() {
        activeTimeCounter?.cancel()
        passiveTimeCounter?.cancel()
        unfilledRefreshCounter?.cancel()
    }
}