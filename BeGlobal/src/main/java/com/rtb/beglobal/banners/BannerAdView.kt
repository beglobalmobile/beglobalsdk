package com.rtb.beglobal.banners

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.appharbr.sdk.engine.AdBlockReason
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AppHarbr
import com.appharbr.sdk.engine.adformat.AdFormat
import com.appharbr.sdk.engine.listeners.AHIncident
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.gson.Gson
import com.rtb.beglobal.R
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.common.AdTypes
import com.rtb.beglobal.databinding.BannerAdViewBinding
import com.rtb.beglobal.sdk.BannerAdListener
import com.rtb.beglobal.sdk.BannerManagerListener
import com.rtb.beglobal.sdk.log
import org.prebid.mobile.addendum.AdViewUtils
import org.prebid.mobile.addendum.PbFindSizeError
import java.util.Locale

class BannerAdView : LinearLayout, BannerManagerListener {

    private lateinit var mContext: Context
    private lateinit var binding: BannerAdViewBinding
    private lateinit var adView: AdManagerAdView
    private lateinit var bannerManager: BannerManager
    private var adType: String = AdTypes.BANNER
    private lateinit var currentAdUnit: String
    private lateinit var currentAdSizes: List<AdSize>
    private var firstLook = true
    private var bannerAdListener: BannerAdListener? = null
    private lateinit var viewState: Lifecycle.Event
    private var isRefreshLoaded = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        this.mContext = context
        this.firstLook = true
        attachLifecycle(mContext)
        bannerManager = try {
            BannerManager(context, this, this.apply {
                if (this.id == -1) {
                    this.id = (0..Int.MAX_VALUE).random()
                }
            })
        } catch (e: Throwable) {
            e.printStackTrace()
            BannerManager(context, this, null)
        }

        val view = inflate(context, R.layout.banner_ad_view, this)
        binding = BannerAdViewBinding.bind(view)
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.BannerAdView).apply {
                val adUnitId = getString(R.styleable.BannerAdView_adUnitId) ?: ""
                val adSize = getString(R.styleable.BannerAdView_adSize) ?: "BANNER"
                var adSizes = getString(R.styleable.BannerAdView_adSizes) ?: "BANNER"
                adType = getString(R.styleable.BannerAdView_adType) ?: AdTypes.BANNER
                if (!adSizes.contains(adSize)) {
                    adSizes = if (adSizes.isEmpty()) adSize
                    else String.format(Locale.ENGLISH, "%s,%s", adSizes, adSize)
                }
                if (adUnitId.isNotEmpty() && adSizes.isNotEmpty()) {
                    attachAdView(adUnitId, bannerManager.convertStringSizesToAdSizes(adSizes))
                }
            }
        }
    }


    override fun attachAdView(adUnitId: String, adSizes: List<AdSize>) {
        adView = AdManagerAdView(mContext)
        currentAdSizes = adSizes
        currentAdUnit = adUnitId
        adView.setAdSizes(*adSizes.toTypedArray())
        adView.adUnitId = adUnitId
        adView.adListener = adListener
        binding.root.removeAllViews()
        binding.root.addView(adView)
        log { "attachAdView : $adUnitId" }
    }

    fun setAdSize(adSize: BannerAdSize) = setAdSizes(adSize)

    fun setAdSizes(vararg adSizes: BannerAdSize) {
        this.currentAdSizes = bannerManager.convertVaragsToAdSizes(*adSizes)
        if (this::currentAdSizes.isInitialized && this::currentAdUnit.isInitialized) {
            attachAdView(adUnitId = currentAdUnit, adSizes = currentAdSizes)
        }
    }

    fun setAdUnitID(adUnitId: String) {
        this.currentAdUnit = adUnitId
        if (this::currentAdSizes.isInitialized && this::currentAdUnit.isInitialized) {
            attachAdView(adUnitId = currentAdUnit, adSizes = currentAdSizes)
        }
    }

    fun setAdType(adType: String) {
        this.adType = adType
        if (this::currentAdSizes.isInitialized && this::currentAdUnit.isInitialized) {
            attachAdView(adUnitId = currentAdUnit, adSizes = currentAdSizes)
        }
    }

    fun setAdListener(listener: BannerAdListener) {
        this.bannerAdListener = listener
    }

    override fun loadAd(request: AdRequest): Boolean {
        var adRequest = request.getAdRequest() ?: return false
        if (!this::currentAdUnit.isInitialized) return false
        fun load() {
            if (this::adView.isInitialized) {
                log { "loadAd&load : ${Gson().toJson(adRequest.customTargeting)}" }
                isRefreshLoaded = adRequest.customTargeting.containsKey("refresh") && adRequest.customTargeting["retry"] == "0"
                bannerManager.fetchDemand(firstLook, adRequest) { adView.loadAd(adRequest) }
            }
        }
        if (firstLook) {
            bannerManager.shouldSetConfig {
                if (it) {
                    bannerManager.setConfig(currentAdUnit, currentAdSizes as ArrayList<AdSize>, adType)
                    adRequest = bannerManager.checkOverride() ?: adRequest
                    bannerManager.checkGeoEdge(true) { addGeoEdge(true) }
                }
                load()
            }
        } else {
            bannerManager.checkGeoEdge(false) { addGeoEdge(false) }
            load()
        }
        return true
    }

    private fun addGeoEdge(firstLook: Boolean) {
        try {
            log { "addGeoEdge with first look : $firstLook" }
            AppHarbr.addBannerView(AdSdk.GAM, adView, object : AHIncident {
                override fun onAdBlocked(p0: Any?, p1: String?, p2: AdFormat, reasons: Array<out AdBlockReason>) {
                    log { "Banner: onAdBlocked : ${Gson().toJson(reasons.asList().map { it.reason })}" }
                }

                override fun onAdIncident(view: Any?, unitId: String?, adNetwork: AdSdk?, creativeId: String?, adFormat: AdFormat, blockReasons: Array<out AdBlockReason>, reportReasons: Array<out AdBlockReason>) {
                    log { "Banner: onAdIncident : ${Gson().toJson(reportReasons.asList().map { it.reason })}" }
                    if (firstLook) {
                        bannerManager.adReported(creativeId, reportReasons.asList().map { it.reason })
                    }
                }
            })
        } catch (e: Throwable) {
            log { "Adding GeoEdgeFailed with first look: $firstLook" }
            e.printStackTrace()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        bannerManager.saveVisibility(isVisible)
    }

    private val adListener = object : AdListener() {
        override fun onAdClicked() {
            super.onAdClicked()
            bannerAdListener?.onAdClicked()
        }

        override fun onAdClosed() {
            super.onAdClosed()
            bannerAdListener?.onAdClosed()
        }

        override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            log { "Ad Failed with error : $p0" }
            val tempStatus = firstLook
            if (firstLook) {
                firstLook = false
            }
            val retryStatus = try {
                bannerManager.adFailedToLoad(tempStatus)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            if (bannerManager.allowCallback(isRefreshLoaded)) {
                bannerAdListener?.onAdFailedToLoad(p0.toString(), retryStatus)
            }
        }

        override fun onAdImpression() {
            super.onAdImpression()
            bannerManager.adImpressed()
            if (bannerManager.allowCallback(isRefreshLoaded)) {
                bannerAdListener?.onAdImpression()
            }
        }

        override fun onAdLoaded() {
            super.onAdLoaded()
            log { "Ad loaded with unit : $currentAdUnit" }
            if (bannerManager.allowCallback(isRefreshLoaded)) {
                bannerAdListener?.onAdLoaded()
            }
            bannerManager.adLoaded(firstLook, adView.responseInfo?.loadedAdapterResponseInfo)
            if (firstLook) {
                firstLook = false
            }
            AdViewUtils.findPrebidCreativeSize(adView, object : AdViewUtils.PbFindSizeListener {
                override fun success(width: Int, height: Int) {
                    adView.setAdSizes(AdSize(width, height))
                }

                override fun failure(error: PbFindSizeError) {}
            })
        }

        override fun onAdOpened() {
            bannerAdListener?.onAdOpened()
            super.onAdOpened()
        }
    }

    private fun attachLifecycle(context: Context) {
        viewState = Lifecycle.Event.ON_CREATE
        try {
            var lifecycle: Lifecycle? = null
            if (context is LifecycleOwner) {
                lifecycle = context.lifecycle
            }
            if (lifecycle == null) {
                lifecycle = (mContext as? AppCompatActivity)?.lifecycle
            }

            lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        resumeAd()
                    }
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        pauseAd()
                    }
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        destroyAd()
                        lifecycle.removeObserver(this)
                    }
                }
            })
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun pauseAd() {
        if (this::adView.isInitialized && this::viewState.isInitialized && viewState != Lifecycle.Event.ON_PAUSE) {
            viewState = Lifecycle.Event.ON_PAUSE
            adView.pause()
            bannerManager.adPaused()
        }
    }

    fun resumeAd() {
        if (this::adView.isInitialized && this::viewState.isInitialized && viewState != Lifecycle.Event.ON_RESUME) {
            viewState = Lifecycle.Event.ON_RESUME
            adView.resume()
            bannerManager.adResumed()
        }
    }

    fun destroyAd() {
        if (this::adView.isInitialized && this::viewState.isInitialized && viewState != Lifecycle.Event.ON_DESTROY) {
            viewState = Lifecycle.Event.ON_DESTROY
            adView.destroy()
            bannerManager.adDestroyed()
        }
    }


}