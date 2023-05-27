package com.rtb.beglobal.adapter

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.log

class BeGlobalInterstitialLoader(
    private val mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    private val mediationAdLoadCallback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
) : MediationInterstitialAd, AdManagerInterstitialAdLoadCallback() {

    private lateinit var interstitialAdCallback: MediationInterstitialAdCallback
    private var mAdManagerInterstitialAd: AdManagerInterstitialAd? = null
    private val TAG: String = this::class.java.simpleName

    fun loadAd() {
        LogLevel.INFO.log(TAG, "Begin loading interstitial ad.")
        val serverParameter =
            mediationInterstitialAdConfiguration.serverParameters.getString("parameter")
        if (serverParameter.isNullOrEmpty()) {
            mediationAdLoadCallback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        LogLevel.INFO.log(TAG, "Received server parameter. $serverParameter")
        val context = mediationInterstitialAdConfiguration.context
        val request = BeGlobalAdapter.createAdRequest(mediationInterstitialAdConfiguration)
        AdManagerInterstitialAd.load(context, serverParameter, request, this)
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        mAdManagerInterstitialAd = null
        mediationAdLoadCallback.onFailure(adError)
    }

    override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
        mAdManagerInterstitialAd = interstitialAd
        interstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
        mAdManagerInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdImpression() {
                super.onAdImpression()
                interstitialAdCallback.reportAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                interstitialAdCallback.onAdOpened()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                interstitialAdCallback.onAdClosed()
            }
        }
    }


    override fun showAd(context: Context) {
        if (context is Activity && mAdManagerInterstitialAd != null) {
            mAdManagerInterstitialAd?.show(context)
        }
    }
}