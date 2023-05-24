package com.rtb.beglobal.common

class ServerSideVerificationOptions {
    private lateinit var options: com.google.android.gms.ads.rewarded.ServerSideVerificationOptions

    internal fun getOptions(): com.google.android.gms.ads.rewarded.ServerSideVerificationOptions? {
        return if (this::options.isInitialized) {
            options
        } else null
    }

    inner class Builder {
        private val optionBuilder =
            com.google.android.gms.ads.rewarded.ServerSideVerificationOptions.Builder()

        fun setCustomData(customData: String): Builder {
            optionBuilder.setCustomData(customData)
            return this
        }

        fun setUserId(userId: String): Builder {
            optionBuilder.setUserId(userId)
            return this
        }

        fun build(): ServerSideVerificationOptions {
            this@ServerSideVerificationOptions.options = optionBuilder.build()
            return this@ServerSideVerificationOptions
        }
    }
}