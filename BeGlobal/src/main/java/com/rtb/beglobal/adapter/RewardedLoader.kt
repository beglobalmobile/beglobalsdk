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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log

class RewardedLoader(private val mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
                     private val mediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>)
    : MediationRewardedAd, RewardedAdLoadCallback() {

    private var mAdManagerRewardedAd: RewardedAd? = null
    private var rewardedAdCallback: MediationRewardedAdCallback? = null
    private val TAG: String = this::class.java.simpleName

    fun loadAd() {
        Logger.INFO.log(TAG, "Begin loading rewarded ad.")
        val serverParameter = mediationRewardedAdConfiguration.serverParameters.getString("parameter")
        if (serverParameter.isNullOrEmpty()) {
            mediationAdLoadCallback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        Logger.INFO.log(TAG, "Received server parameter. $serverParameter")
        val context = mediationRewardedAdConfiguration.context
        val request = BeGlobalAdapter.createAdRequest(mediationRewardedAdConfiguration)
        RewardedAd.load(context, serverParameter, request, this)
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        mAdManagerRewardedAd = null
        mediationAdLoadCallback.onFailure(p0)
    }

    override fun onAdLoaded(p0: RewardedAd) {
        mAdManagerRewardedAd = p0
        rewardedAdCallback = mediationAdLoadCallback.onSuccess(this)
        mAdManagerRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                super.onAdClicked()
                rewardedAdCallback?.reportAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                rewardedAdCallback?.onAdClosed()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                rewardedAdCallback?.reportAdImpression()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                rewardedAdCallback?.onVideoStart()
                rewardedAdCallback?.onAdOpened()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                rewardedAdCallback?.onAdFailedToShow(p0)
            }
        }
    }

    override fun showAd(context: Context) {
        if (context is Activity && mAdManagerRewardedAd != null) {
            mAdManagerRewardedAd?.show(context) {
                rewardedAdCallback?.onVideoComplete()
                rewardedAdCallback?.onUserEarnedReward(it)
            }
        }
    }
}