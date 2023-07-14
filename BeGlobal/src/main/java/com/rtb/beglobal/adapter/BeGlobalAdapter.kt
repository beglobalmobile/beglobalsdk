package com.rtb.beglobal.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.mediation.*
import com.rtb.beglobal.BuildConfig
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log


class BeGlobalAdapter : Adapter() {

    private lateinit var bannerLoader: BannerLoader
    private lateinit var interstitialLoader: InterstitialLoader
    private lateinit var rewardedLoaded: RewardedLoader
    private lateinit var rewardedInterstitialLoader: RewardedInterstitialLoader
    private lateinit var appOpenAdLoader: AppOpenAdLoader
    private lateinit var nativeAdLoaded: NativeAdLoader
    private val TAG: String = this::class.java.simpleName

    companion object {

        @SuppressLint("VisibleForTests")
        fun createAdRequest(mediationAdConfiguration: MediationAdConfiguration): AdManagerAdRequest {
            return AdManagerAdRequest.Builder().addCustomTargeting("hb_format", "amp").build()
        }
    }

    override fun initialize(context: Context, initializationCompleteCallback: InitializationCompleteCallback, list: List<MediationConfiguration>) {
        Logger.INFO.log(TAG, "initialize: $TAG")
        return
    }

    override fun getVersionInfo(): VersionInfo {
        val versionString = BuildConfig.ADAPTER_VERSION
        val splits = versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (splits.size >= 3) {
            val major = splits[0].toInt()
            val minor = splits[1].toInt()
            val micro = splits[2].toInt()
            return VersionInfo(major, minor, micro)
        }
        return VersionInfo(0, 0, 0)
    }

    override fun getSDKVersionInfo(): VersionInfo {
        val versionString = MobileAds.getVersion()
        return VersionInfo(versionString.majorVersion, versionString.minorVersion, versionString.microVersion)
    }

    override fun loadBannerAd(mediationBannerAdConfiguration: MediationBannerAdConfiguration, mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>) {
        Logger.INFO.log(TAG, "loadBannerAd")
        bannerLoader = BannerLoader(mediationBannerAdConfiguration, mediationAdLoadCallback)
        bannerLoader.loadAd()
    }

    override fun loadInterstitialAd(mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration, callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>) {
        Logger.INFO.log(TAG, "loadInterstitialAd:")
        interstitialLoader = InterstitialLoader(mediationInterstitialAdConfiguration, callback)
        interstitialLoader.loadAd()
    }

    override fun loadAppOpenAd(mediationAppOpenAdConfiguration: MediationAppOpenAdConfiguration, callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>) {
        Logger.INFO.log(TAG, "loadAppOpenAd:")
        appOpenAdLoader = AppOpenAdLoader(mediationAppOpenAdConfiguration, callback)
        appOpenAdLoader.loadAd()
    }

    override fun loadRewardedAd(mediationRewardedAdConfiguration: MediationRewardedAdConfiguration, callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>) {
        Logger.INFO.log(TAG, "loadRewardedAd:")
        rewardedLoaded = RewardedLoader(mediationRewardedAdConfiguration, callback)
        rewardedLoaded.loadAd()
    }

    override fun loadRewardedInterstitialAd(mediationRewardedAdConfiguration: MediationRewardedAdConfiguration, callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>) {
        Logger.INFO.log(TAG, "loadRewardedInterstitialAd:")
        rewardedInterstitialLoader = RewardedInterstitialLoader(mediationRewardedAdConfiguration, callback)
        rewardedInterstitialLoader.loadAd()
    }

    override fun loadNativeAd(mediationNativeAdConfiguration: MediationNativeAdConfiguration, callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>) {
        Logger.INFO.log(TAG, "loadNativeAd:")
        nativeAdLoaded = NativeAdLoader(mediationNativeAdConfiguration, callback)
        nativeAdLoaded.loadAd()
    }
}