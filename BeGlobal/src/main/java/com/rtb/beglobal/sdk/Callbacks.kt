package com.rtb.beglobal.sdk

import com.google.android.gms.ads.AdSize
import com.rtb.beglobal.common.AdRequest

internal interface BannerManagerListener {
    fun attachAdView(adUnitId: String, adSizes: List<AdSize>)

    fun loadAd(request: AdRequest): Boolean
}

interface FullScreenContentCallback {
    fun onAdClicked()
    fun onAdDismissedFullScreenContent()
    fun onAdFailedToShowFullScreenContent(error: String)
    fun onAdImpression()
    fun onAdShowedFullScreenContent()
}

interface BannerAdListener {
    fun onAdClicked()
    fun onAdClosed()
    fun onAdFailedToLoad(error: String)
    fun onAdImpression()
    fun onAdLoaded()
    fun onAdOpened()
}

fun interface OnShowAdCompleteListener {
    fun onShowAdComplete()
}

interface AdLoadCallback {
    fun onAdLoaded()

    fun onAdFailedToLoad(error: String)
}