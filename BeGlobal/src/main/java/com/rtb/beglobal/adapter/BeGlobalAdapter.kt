package com.rtb.beglobal.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.mediation.*
import com.rtb.beglobal.BuildConfig
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.sdk.log


class BeGlobalAdapter : Adapter() {

    private lateinit var bannerLoader: BeGlobalBannerLoader
    private lateinit var interstitialLoader: BeGlobalInterstitialLoader
    private val TAG: String = this::class.java.simpleName

    companion object {

        @SuppressLint("VisibleForTests")
        fun createAdRequest(mediationAdConfiguration: MediationAdConfiguration): AdManagerAdRequest {
            return AdManagerAdRequest.Builder().addCustomTargeting("hb_format", "amp").build()
        }
    }

    override fun initialize(
        context: Context,
        initializationCompleteCallback: InitializationCompleteCallback,
        list: List<MediationConfiguration>
    ) {
        LogLevel.INFO.log(TAG, "initialize: ${this.javaClass.simpleName}")
        return
    }

    override fun getVersionInfo(): VersionInfo {
        val versionString = BuildConfig.ADAPTER_VERSION
        val splits =
            versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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
        return VersionInfo(
            versionString.majorVersion,
            versionString.minorVersion,
            versionString.microVersion
        )
    }

    override fun loadBannerAd(
        mediationBannerAdConfiguration: MediationBannerAdConfiguration,
        mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
    ) {
        LogLevel.INFO.log(TAG, "loadBannerAd")
        bannerLoader = BeGlobalBannerLoader(mediationBannerAdConfiguration, mediationAdLoadCallback)
        bannerLoader.loadAd()
    }

    override fun loadInterstitialAd(
        mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
        callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
    ) {
        LogLevel.INFO.log(TAG, "loadInterstitialAd:")
        interstitialLoader =
            BeGlobalInterstitialLoader(mediationInterstitialAdConfiguration, callback)
        interstitialLoader.loadAd()
    }
}