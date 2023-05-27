package com.rtb.beglobal.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.appharbr.sdk.configuration.AHSdkConfiguration
import com.appharbr.sdk.engine.AppHarbr
import com.appharbr.sdk.engine.InitializationFailureReason
import com.appharbr.sdk.engine.listeners.OnAppHarbrInitializationCompleteListener
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.rtb.beglobal.common.LogLevel
import com.rtb.beglobal.common.URLs.BASE_URL
import okhttp3.OkHttpClient
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.rendering.models.openrtb.bidRequests.Ext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import java.util.concurrent.TimeUnit

object BeGlobal {
    private var storeService: StoreService? = null
    private var configService: ConfigService? = null
    private var workManager: WorkManager? = null
    private var logEnabled = false

    fun initialize(context: Context, logsEnabled: Boolean = false) {
        this.logEnabled = logsEnabled
        fetchConfig(context)
    }

    internal fun logEnabled() = logEnabled

    internal fun getStoreService(context: Context): StoreService {
        @Synchronized
        if (storeService == null) {
            storeService = StoreService(
                context.getSharedPreferences(
                    this.toString().substringBefore("@"),
                    Context.MODE_PRIVATE
                )
            )
        }
        return storeService as StoreService
    }

    internal fun getConfigService(): ConfigService {
        @Synchronized
        if (configService == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS).hostnameVerifier { _, _ -> true }.build()
            configService = Retrofit.Builder().baseUrl(BASE_URL).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(ConfigService::class.java)
        }
        return configService as ConfigService
    }

    internal fun getWorkManager(context: Context): WorkManager {
        @Synchronized
        if (workManager == null) {
            workManager = WorkManager.getInstance(context)
        }
        return workManager as WorkManager
    }

    private fun fetchConfig(context: Context, delay: Long? = null) {
        if (delay != null && delay < 900) return
        try {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workerRequest: OneTimeWorkRequest = delay?.let {
                OneTimeWorkRequestBuilder<ConfigSetWorker>().setConstraints(constraints)
                    .setInitialDelay(it, TimeUnit.SECONDS).build()
            } ?: kotlin.run {
                OneTimeWorkRequestBuilder<ConfigSetWorker>().setConstraints(constraints).build()
            }
            val workManager = getWorkManager(context)
            val storeService = getStoreService(context)
            workManager.enqueueUniqueWork(
                ConfigSetWorker::class.java.simpleName,
                ExistingWorkPolicy.REPLACE,
                workerRequest
            )
            workManager.getWorkInfoByIdLiveData(workerRequest.id).observeForever {
                if (it?.state == WorkInfo.State.SUCCEEDED) {
                    SDKManager.initialize(context)
                    if (storeService.config != null && storeService.config?.refetch != null) {
                        fetchConfig(context, storeService.config?.refetch)
                    }
                }
            }
        } catch (e: Exception) {
            SDKManager.initialize(context)
        }
    }
}

internal class ConfigSetWorker(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {
    override fun doWork(): Result {
        val storeService = BeGlobal.getStoreService(context)
        return try {
            val configService = BeGlobal.getConfigService()
            val response =
                configService.getConfig(hashMapOf("name" to context.packageName)).execute()
            if (response.isSuccessful && response.body() != null) {
                storeService.config = response.body()
                Result.success()
            } else {
                storeService.config?.let {
                    Result.success()
                } ?: Result.failure()
            }
        } catch (e: Exception) {
            LogLevel.ERROR.log(msg = e.message ?: "")
            storeService.config?.let {
                Result.success()
            } ?: Result.failure()
        }
    }
}

internal object SDKManager {

    fun initialize(context: Context) {
        initializeGAM(context)
        val storeService = BeGlobal.getStoreService(context)
        val config = storeService.config ?: return
        if (config.switch != 1) return
        initializePrebid(context, config.prebid)
        initializeGeoEdge(context, config.geoEdge?.apiKey)
    }

    private fun initializePrebid(context: Context, prebid: SDKConfig.Prebid?) {
        PrebidMobile.setPbsDebug(prebid?.debug == 1)
        PrebidMobile.setPrebidServerHost(Host.createCustomHost(prebid?.host ?: ""))
        PrebidMobile.setPrebidServerAccountId(prebid?.accountId ?: "")
        PrebidMobile.setTimeoutMillis(prebid?.timeout?.toIntOrNull() ?: 1000)
        PrebidMobile.initializeSdk(context) { LogLevel.INFO.log(msg = "Prebid Initialization Completed") }
        prebid?.schain?.let {
            TargetingParams.setUserExt(Ext().apply {
                put("schain", it)
            })
        }
    }

    private fun initializeGAM(context: Context) {
        MobileAds.initialize(context) {
            LogLevel.INFO.log(msg = "GAM Initialization complete.")
        }
    }

    private fun initializeGeoEdge(context: Context, apiKey: String?) {
        if (apiKey.isNullOrEmpty()) return
        val configuration = AHSdkConfiguration.Builder(apiKey).build()
        AppHarbr.initialize(
            context,
            configuration,
            object : OnAppHarbrInitializationCompleteListener {
                override fun onSuccess() {
                    LogLevel.INFO.log(msg = "AppHarbr SDK Initialized Successfully")
                }

                override fun onFailure(reason: InitializationFailureReason) {
                    LogLevel.ERROR.log(msg = "AppHarbr SDK Initialization Failed: ${reason.readableHumanReason}")
                }

            })
    }
}

internal interface ConfigService {
    @GET("appconfig1.php")
    fun getConfig(@QueryMap params: HashMap<String, Any>): Call<SDKConfig>
}

internal class StoreService(private val prefs: SharedPreferences) {

    var config: SDKConfig?
        get() {
            val string = prefs.getString("CONFIG", "") ?: ""
            if (string.isEmpty()) return null
            return Gson().fromJson(string, SDKConfig::class.java)
        }
        set(value) = prefs.edit().apply {
            value?.let { putString("CONFIG", Gson().toJson(value)) }
                ?: kotlin.run { remove("CONFIG") }
        }.apply()
}