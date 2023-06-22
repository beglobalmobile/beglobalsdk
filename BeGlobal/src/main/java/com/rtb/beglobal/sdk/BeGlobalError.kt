package com.rtb.beglobal.sdk

import com.google.android.gms.ads.AdError

/** Convenience factory class to create AdError objects for custom events.  */
object BeGlobalError {
    const val SAMPLE_SDK_DOMAIN = "com.google.android.gms.ads"
    const val CUSTOM_EVENT_ERROR_DOMAIN = "com.rtb.beglobal.adapter"

    /** Error raised when the custom event adapter cannot obtain the ad unit id.  */
    const val ERROR_NO_AD_UNIT_ID = 101

    /**
     * Error raised when the custom event adapter does not have an ad available when trying to show
     * the ad.
     */
    const val ERROR_AD_NOT_AVAILABLE = 102

    /** Error raised when the custom event adapter cannot obtain the activity context.  */
    const val ERROR_NO_ACTIVITY_CONTEXT = 103
    fun createCustomEventNoAdIdError(): AdError {
        return AdError(ERROR_NO_AD_UNIT_ID, "Ad unit id is empty", CUSTOM_EVENT_ERROR_DOMAIN)
    }

    fun createCustomEventAdNotAvailableError(): AdError {
        return AdError(ERROR_AD_NOT_AVAILABLE, "No ads to show", CUSTOM_EVENT_ERROR_DOMAIN)
    }

    fun createCustomEventNoActivityContextError(): AdError {
        return AdError(
            ERROR_NO_ACTIVITY_CONTEXT,
            "An activity context is required to show the sample ad",
            CUSTOM_EVENT_ERROR_DOMAIN
        )
    }

    /**
     * Creates a custom event `AdError`. This error wraps the underlying error thrown by the
     * sample SDK.
     *
     * @param errorCode A `SampleErrorCode` to be reported.
     */
    fun createSampleSdkError(errorCode: ErrorCode): AdError {
        val message: String = errorCode.toString()
        return AdError(getMediationErrorCode(errorCode), message, SAMPLE_SDK_DOMAIN)
    }

    /**
     * Converts the SampleErrorCode to an integer in the range 0-99. This range is distinct from the
     * SampleCustomEventErrorCode's range which is 100-199.
     *
     * @param errorCode the error code returned by the sample SDK
     * @return an integer in the range 0-99
     */
    private fun getMediationErrorCode(errorCode: ErrorCode) =
            when (errorCode) {
                ErrorCode.UNKNOWN -> 0
                ErrorCode.BAD_REQUEST -> 1
                ErrorCode.NO_INVENTORY -> 2
                ErrorCode.NETWORK_ERROR -> 3
            }
}