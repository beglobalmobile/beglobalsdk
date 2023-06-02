package com.rtb.beglobal.adapter

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.log

class RewardedInterstitialLoader(private val mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
                                 private val mediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>)
    : MediationRewardedAd, RewardedInterstitialAdLoadCallback() {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var rewardedInterstitialAdCallback: MediationRewardedAdCallback? = null
    private val TAG: String = this::class.java.simpleName

    fun loadAd() {
        LogLevel.INFO.log(TAG, "Begin loading rewarded interstitial ad.")
        val serverParameter = mediationRewardedAdConfiguration.serverParameters.getString("parameter")
        if (serverParameter.isNullOrEmpty()) {
            mediationAdLoadCallback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        LogLevel.INFO.log(TAG, "Received server parameter. $serverParameter")
        val context = mediationRewardedAdConfiguration.context
        val request = BeGlobalAdapter.createAdRequest(mediationRewardedAdConfiguration)
        RewardedInterstitialAd.load(context, serverParameter, request, this)
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        rewardedInterstitialAd = null
        mediationAdLoadCallback.onFailure(p0)
    }

    override fun onAdLoaded(p0: RewardedInterstitialAd) {
        rewardedInterstitialAd = p0
        rewardedInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this)
        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                super.onAdClicked()
                rewardedInterstitialAdCallback?.reportAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                rewardedInterstitialAdCallback?.onAdClosed()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                rewardedInterstitialAdCallback?.reportAdImpression()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                rewardedInterstitialAdCallback?.onVideoStart()
                rewardedInterstitialAdCallback?.onAdOpened()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                rewardedInterstitialAdCallback?.onAdFailedToShow(p0)
            }
        }
    }

    override fun showAd(context: Context) {
        if (context is Activity && rewardedInterstitialAd != null) {
            rewardedInterstitialAd?.show(context) {
                rewardedInterstitialAdCallback?.onVideoComplete()
                rewardedInterstitialAdCallback?.onUserEarnedReward(it)
            }
        }
    }
}