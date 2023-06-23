package com.rtb.bemonetise

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rtb.beglobal.appopen.AppOpenAdManager
import com.rtb.beglobal.sdk.BeGlobal
import com.rtb.beglobal.sdk.FullScreenContentCallback

class ThisApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private lateinit var lifeCyclerHandler: ActivityLifecycleHandler
    private var adUnitId = "/6499/example/app-open"

    override fun onCreate() {
        super.onCreate()
        lifeCyclerHandler = ActivityLifecycleHandler()
        registerActivityLifecycleCallbacks(lifeCyclerHandler)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifeCyclerHandler)
        BeGlobal.initialize(this, true)
        appOpenAdManager = AppOpenAdManager(this, adUnitId)
        appOpenAdManager.fullScreenContentCallback = fullScreenContentCallback
    }

    fun showAdIfAvailable(activity: Activity) {
        appOpenAdManager.showAdIfAvailable(activity) {
            // Empty because the user will go back to the activity that shows the ad.
        }
    }

    private var fullScreenContentCallback = object : FullScreenContentCallback {
        override fun onAdClicked() {

        }

        override fun onAdDismissedFullScreenContent() {
        }

        override fun onAdFailedToShowFullScreenContent(error: String) {
        }

        override fun onAdImpression() {
        }

        override fun onAdShowedFullScreenContent() {
        }
    }

    private inner class ActivityLifecycleHandler : ActivityLifecycleCallbacks,
        LifecycleEventObserver {

        private var currentActivity: Activity? = null

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_START) {
                currentActivity?.let { showAdIfAvailable(it) }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (!appOpenAdManager.isShowingAd) {
                currentActivity = activity
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}