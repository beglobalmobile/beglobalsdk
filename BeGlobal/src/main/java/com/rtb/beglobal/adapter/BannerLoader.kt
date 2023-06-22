package com.rtb.beglobal.adapter

import android.content.res.Resources
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log
import kotlin.math.roundToInt

internal class BannerLoader(private val mediationBannerAdConfiguration: MediationBannerAdConfiguration,
                            private val mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>)
    : MediationBannerAd, AdListener() {


    private lateinit var adView: AdManagerAdView
    private lateinit var bannerAdCallback: MediationBannerAdCallback
    private val TAG: String = this::class.java.simpleName

    fun loadAd() {
        Logger.INFO.log(TAG, "Begin loading banner ad.")
        val serverParameter = mediationBannerAdConfiguration.serverParameters.getString("parameter")
        if (serverParameter.isNullOrEmpty()) {
            mediationAdLoadCallback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        Logger.INFO.log(TAG, "Received server parameter. $serverParameter")
        val context = mediationBannerAdConfiguration.context

        adView = AdManagerAdView(context)
        adView.adUnitId = serverParameter

        val size = mediationBannerAdConfiguration.adSize
        val widthInPixels = size.getWidthInPixels(context)
        val heightInPixels = size.getHeightInPixels(context)

        val displayMetrics = Resources.getSystem().displayMetrics
        val widthInDp = (widthInPixels / displayMetrics.density).roundToInt()
        val heightInDp = (heightInPixels / displayMetrics.density).roundToInt()
        val adSize = AdSize(widthInDp, heightInDp)
        adView.setAdSize(adSize)
        adView.adListener = this
        val request = BeGlobalAdapter.createAdRequest(mediationBannerAdConfiguration)
        Logger.INFO.log(TAG, "Start fetching banner ad.")
        adView.loadAd(request)
    }

    override fun getView(): View {
        return adView
    }

    override fun onAdClosed() {
        Logger.INFO.log(TAG, "The banner ad was closed.")
        bannerAdCallback.onAdClosed()
    }

    override fun onAdClicked() {
        Logger.INFO.log(TAG, "The banner ad was clicked.")
        bannerAdCallback.onAdOpened()
        bannerAdCallback.onAdLeftApplication()
        bannerAdCallback.reportAdClicked()
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        Logger.ERROR.log(TAG, "Failed to fetch the banner ad.")
        mediationAdLoadCallback.onFailure(p0)
    }

    override fun onAdLoaded() {
        Logger.INFO.log(TAG, "Received the banner ad.")
        bannerAdCallback = mediationAdLoadCallback.onSuccess(this)
        bannerAdCallback.reportAdImpression()
    }
}