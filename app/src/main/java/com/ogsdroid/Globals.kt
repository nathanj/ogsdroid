package com.ogsdroid

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.ogs.OGS
import com.ogs.OgsOauthService
import com.ogs.OgsService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.atomic.AtomicInteger

object Globals {
    private val ogs: OGS = OGS("82ff83f2631a55273c31", "cd42d95fd978348d57dc909a9aecd68d36b17bd2")
    private val refCount = AtomicInteger()

    var accessToken = ""

    val ogsOauthService: OgsOauthService by lazy {
        Retrofit.Builder()
                .baseUrl("https://beta.online-go.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OgsOauthService::class.java)
    }

    val ogsService: OgsService by lazy {
        val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer ${Globals.accessToken}")
                            .build()

                    chain.proceed(request)
                }
                .build()

        Retrofit.Builder()
                .baseUrl("https://beta.online-go.com/api/v1/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OgsService::class.java)
    }

    // Returns the OGS object. The caller must putOGS() when finished with the
    // object.
    fun getOGS(): OGS {
	    refCount.incrementAndGet()
	    println("NJ getOGS: refCount = ${refCount.get()}")
	    return ogs
    }

    // Closes the OGS socket if this is the last reference.
    fun putOGS() {
	    println("NJ putOGS: refCount = ${refCount.get()}")
	    if (refCount.decrementAndGet() == 0) {
		    println("NJ putOGS: closing socket")
		    ogs.closeSocket()
	    }
    }
}

