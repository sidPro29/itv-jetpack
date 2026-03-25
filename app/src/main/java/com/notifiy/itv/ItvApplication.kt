package com.notifiy.itv

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ItvApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Initialize Stripe
        com.stripe.android.PaymentConfiguration.init(
            applicationContext,
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
