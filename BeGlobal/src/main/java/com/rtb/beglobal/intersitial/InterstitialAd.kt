package com.rtb.beglobal.intersitial

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.rtb.beglobal.common.AdRequest
import com.rtb.beglobal.sdk.FullScreenContentCallback
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log

class InterstitialAd(private val context: Activity, private val adUnit: String) {
    private var interstitialAdManager = InterstitialAdManager(context, adUnit)
    private var mInterstitialAd: AdManagerInterstitialAd? = null

    fun load(adRequest: AdRequest, callBack: (loaded: Boolean) -> Unit) {
        interstitialAdManager.load(adRequest) {
            mInterstitialAd = it
            callBack(mInterstitialAd != null)
        }
    }

    fun show() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(context)
        } else {
            Logger.ERROR.log(msg = "The interstitial ad wasn't ready yet.")
        }
    }

    fun setContentCallback(callback: FullScreenContentCallback) {
        mInterstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                mInterstitialAd = null
                callback.onAdDismissedFullScreenContent()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                mInterstitialAd = null
                callback.onAdFailedToShowFullScreenContent(p0.toString())
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                callback.onAdShowedFullScreenContent()
            }
        }
    }
}