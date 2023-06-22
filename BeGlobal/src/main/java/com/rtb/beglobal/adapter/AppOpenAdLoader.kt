package com.rtb.beglobal.adapter

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log

class AppOpenAdLoader(private val mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration,
                      private val callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>)
    : MediationAppOpenAd, AppOpenAdLoadCallback() {

    private lateinit var appOpenAdCallback: MediationAppOpenAdCallback
    private var appOpenAd: AppOpenAd? = null
    private val TAG: String = this::class.java.simpleName

    fun loadAd() {
        Logger.INFO.log(TAG, "Begin loading appopen ad.")
        val serverParameter = mediationAppOpenAdConfiguration.serverParameters.getString("parameter")
        if (serverParameter.isNullOrEmpty()) {
            callback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        Logger.INFO.log(TAG, "Received server parameter. $serverParameter")
        val context = mediationAppOpenAdConfiguration.context
        val request = BeGlobalAdapter.createAdRequest(mediationAppOpenAdConfiguration)
        AppOpenAd.load(context, serverParameter, request, this)
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        appOpenAd = null
        callback.onFailure(p0)
    }

    override fun onAdLoaded(p0: AppOpenAd) {
        appOpenAd = p0
        appOpenAdCallback = callback.onSuccess(this)
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdClicked() {
                super.onAdClicked()
                appOpenAdCallback.reportAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                appOpenAdCallback.onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                appOpenAdCallback.onAdFailedToShow(p0)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                appOpenAdCallback.reportAdImpression()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                appOpenAdCallback.onAdOpened()
            }
        }
    }

    override fun showAd(context: Context) {
        if (context is Activity && appOpenAd != null) {
            appOpenAd?.show(context)
        }
    }
}