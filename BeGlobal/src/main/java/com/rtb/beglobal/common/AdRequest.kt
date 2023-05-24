package com.rtb.beglobal.common

import android.annotation.SuppressLint
import android.text.TextUtils
import com.google.android.gms.ads.admanager.AdManagerAdRequest

class AdRequest {

    private lateinit var adRequest: AdManagerAdRequest

    internal fun getAdRequest(): AdManagerAdRequest? {
        return if (this::adRequest.isInitialized) {
            adRequest
        } else {
            null
        }
    }

    @SuppressLint("VisibleForTests")
    inner class Builder {
        private val requestBuilder: AdManagerAdRequest.Builder = AdManagerAdRequest.Builder()

        fun addCategoryExclusion(categoryExclusion: String): AdRequest.Builder {
            requestBuilder.addCategoryExclusion(categoryExclusion)
            return this
        }


        fun addCustomTargeting(key: String, value: String): AdRequest.Builder {
            requestBuilder.addCustomTargeting(key, value)
            return this
        }

        fun addCustomTargeting(key: String, values: List<String>?): AdRequest.Builder {
            if (values != null) {
                addCustomTargeting(key, TextUtils.join(",", values))
            }
            return this
        }

        fun setPublisherProvidedId(publisherProvidedId: String): AdRequest.Builder {
            requestBuilder.setPublisherProvidedId(publisherProvidedId)
            return this
        }

        fun build(): AdRequest {
            adRequest = requestBuilder.build()
            return this@AdRequest
        }

        fun buildWithRequest(request: AdManagerAdRequest): AdRequest {
            adRequest = request
            return this@AdRequest
        }
    }
}