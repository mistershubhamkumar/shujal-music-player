package com.example.ads

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AdMobManager {
    const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    val bannerUnitId: String
        get() = BuildConfig.ADMOB_BANNER_UNIT_ID.ifBlank { TEST_BANNER_UNIT_ID }

    val interstitialUnitId: String
        get() = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID.ifBlank { TEST_INTERSTITIAL_UNIT_ID }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    @Volatile
    private var isLoadingInterstitial = false

    @Volatile
    private var lastShownTimeMs: Long = 0L

    private const val INTERSTITIAL_COOLDOWN_MS = 180_000L

    fun initializeAsync(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                MobileAds.initialize(appContext) {
                    isInitialized = true
                    preloadInterstitial(appContext)
                }
            }
        }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        val appContext = context.applicationContext
        isLoadingInterstitial = true
        CoroutineScope(Dispatchers.Main).launch {
            runCatching {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    appContext,
                    interstitialUnitId,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isLoadingInterstitial = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                            isLoadingInterstitial = false
                        }
                    }
                )
            }.onFailure {
                isLoadingInterstitial = false
            }
        }
    }

    fun showInterstitialIfReady(activity: Activity?, onDone: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        if (activity == null || ad == null || (now - lastShownTimeMs) < INTERSTITIAL_COOLDOWN_MS) {
            if (activity != null && ad == null) {
                preloadInterstitial(activity)
            }
            onDone()
            return
        }

        lastShownTimeMs = now
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadInterstitial(activity)
                onDone()
            }
        }
        runCatching { ad.show(activity) }.onFailure { onDone() }
    }
}
