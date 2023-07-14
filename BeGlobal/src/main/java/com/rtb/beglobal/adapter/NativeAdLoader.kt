@file:Suppress("UNNECESSARY_SAFE_CALL")

package com.rtb.beglobal.adapter

import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import androidx.core.os.bundleOf
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.NativeAd
import com.rtb.beglobal.sdk.BeGlobalError
import com.rtb.beglobal.sdk.ErrorCode
import com.rtb.beglobal.sdk.Logger
import com.rtb.beglobal.sdk.log

class NativeAdLoader(private val mediationNativeAdConfiguration: MediationNativeAdConfiguration,
                     private val mediationAdLoadCallback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>)
    : NativeAd.OnNativeAdLoadedListener, AdListener() {

    private var nativeAdCallback: MediationNativeAdCallback? = null
    private val TAG: String = this::class.java.simpleName


    @Suppress("UNNECESSARY_SAFE_CALL")
    fun loadAd() {
        Logger.INFO.log(TAG, "Begin loading native ad.")
        val serverParameter = mediationNativeAdConfiguration.serverParameters.getString("parameter")
        if (TextUtils.isEmpty(serverParameter)) {
            mediationAdLoadCallback.onFailure(BeGlobalError.createCustomEventNoAdIdError())
            return
        }
        Logger.INFO.log(TAG, "Received server parameter.")
        val request = BeGlobalAdapter.createAdRequest(mediationNativeAdConfiguration)
        val context = mediationNativeAdConfiguration.context
        val adLoaderBuilder = AdLoader.Builder(context, serverParameter ?: "")
                .forNativeAd(this)
                .withAdListener(this)
        mediationNativeAdConfiguration.nativeAdOptions?.let {
            adLoaderBuilder.withNativeAdOptions(it)
        }
        val adLoader = adLoaderBuilder.build()
        adLoader.loadAd(request)
    }

    override fun onNativeAdLoaded(ad: NativeAd) {
        Logger.INFO.log(TAG, "Received the native ad.")
        nativeAdCallback = mediationAdLoadCallback.onSuccess(CustomUnifiedNativeAdMapper(ad))
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        Logger.ERROR.log(TAG, "Failed to fetch the native ad.")
        mediationAdLoadCallback.onFailure(BeGlobalError.createSampleSdkError(ErrorCode.BAD_REQUEST))
    }

    private inner class CustomUnifiedNativeAdMapper(private val nativeAd: NativeAd) : UnifiedNativeAdMapper() {
        init {
            headline = nativeAd.headline ?: ""
            body = nativeAd.body ?: ""
            callToAction = nativeAd.callToAction ?: ""
            starRating = nativeAd.starRating ?: 0.0
            store = nativeAd.store ?: ""
            nativeAd.icon?.let {
                if (it.drawable != null) {
                    icon = SampleNativeMappedImage(it.drawable!!, it.uri ?: Uri.EMPTY, it.scale)
                }
            }
            val imagesList: ArrayList<com.google.android.gms.ads.formats.NativeAd.Image> = ArrayList()
            nativeAd.images?.forEach {
                if (it.drawable != null) {
                    imagesList.add(SampleNativeMappedImage(it.drawable!!, it.uri ?: Uri.EMPTY, it.scale))
                }
            }
            images = imagesList
            price = nativeAd.price ?: ""
            overrideClickHandling = false
            overrideImpressionRecording = false
            advertiser = nativeAd.advertiser ?: ""
            extras = nativeAd.extras
        }

        override fun recordImpression() {
            nativeAd.recordImpression(bundleOf())
        }
    }

    private inner class SampleNativeMappedImage(private val drawable: Drawable, private val imageUri: Uri, private val scale: Double) : com.google.android.gms.ads.formats.NativeAd.Image() {

        override fun getDrawable(): Drawable {
            return drawable
        }

        override fun getUri(): Uri {
            return imageUri
        }

        override fun getScale(): Double {
            return scale
        }
    }
}