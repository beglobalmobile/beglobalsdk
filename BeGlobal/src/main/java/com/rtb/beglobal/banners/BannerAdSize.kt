package com.rtb.beglobal.banners

import android.content.Context
import com.google.android.gms.ads.AdSize

class BannerAdSize(var adSize: AdSize) {

    constructor(width: Int, height: Int) : this(AdSize(width, height))

    companion object {
        val BANNER = BannerAdSize(AdSize.BANNER)
        val FULL_BANNER = BannerAdSize(AdSize.FULL_BANNER)
        val LARGE_BANNER = BannerAdSize(AdSize.FULL_BANNER)
        val LEADERBOARD = BannerAdSize(AdSize.LEADERBOARD)
        val MEDIUM_RECTANGLE = BannerAdSize(AdSize.MEDIUM_RECTANGLE)

        fun getCurrentOrientationAnchoredAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    width
                )
            )
        }

        fun getCurrentOrientationInlineAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                    context,
                    width
                )
            )
        }

        fun getCurrentOrientationInterscrollerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getCurrentOrientationInterscrollerAdSize(context, width))
        }

        fun getInlineAdaptiveBannerAdSize(width: Int, maxHeight: Int): BannerAdSize {
            return BannerAdSize(AdSize.getInlineAdaptiveBannerAdSize(width, maxHeight))
        }

        fun getLandscapeAnchoredAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(context, width))
        }

        fun getLandscapeInlineAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getLandscapeInlineAdaptiveBannerAdSize(context, width))
        }

        fun getLandscapeInterscrollerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getLandscapeInterscrollerAdSize(context, width))
        }

        fun getPortraitAnchoredAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getPortraitAnchoredAdaptiveBannerAdSize(context, width))
        }

        fun getPortraitInlineAdaptiveBannerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getPortraitInlineAdaptiveBannerAdSize(context, width))
        }

        fun getPortraitInterscrollerAdSize(context: Context, width: Int): BannerAdSize {
            return BannerAdSize(AdSize.getPortraitInterscrollerAdSize(context, width))
        }
    }


}
