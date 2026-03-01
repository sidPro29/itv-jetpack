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
            "pk_test_51T6DloKIOfsbn4UWmZy7H5PjxqgVtuyAerT2s84NupJ02BkhJP9AQjZeV1jOVmFdz3nAix97p5K51eDqU8C5x8YK00g2YxYqbs"
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
