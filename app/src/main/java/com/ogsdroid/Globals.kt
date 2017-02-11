package com.ogsdroid

import android.content.Context
import android.preference.PreferenceManager
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.ogs.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object Globals {
    private val ogs: OGS = OGS("82ff83f2631a55273c31", "cd42d95fd978348d57dc909a9aecd68d36b17bd2")
    private val refCount = AtomicInteger()

    var accessToken = ""
    var me: Me? = null

    private fun saveLoginInfo(context: Context, loginInfo: LoginInfo) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putString("accessToken", loginInfo.access_token)
        editor.putString("refreshToken", loginInfo.refresh_token)
        editor.putLong("expiresAt", Date().time + loginInfo.expires_in)
        editor.apply()
    }

    fun refreshAccessToken(context: Context, refreshToken: String): Observable<String?> {
        return ogsOauthService.refreshToken(refreshToken)
                .doOnNext { saveLoginInfo(context, it) }
                .map { it.access_token }
    }

    fun getAccessToken(context: Context): Observable<String?> {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val accessToken = pref.getString("accessToken", "")
        val refreshToken = pref.getString("refreshToken", "")
        val expiresAt = pref.getLong("expiresAt", 0)

        val timeLeft = expiresAt - Date().time
        println("expiresAt   = ${expiresAt}")
        println("Date().time = ${Date().time}")
        println("timeLeft    = ${timeLeft}")
        if (accessToken.isNotEmpty() && expiresAt - 5 * 60 > Date().time) {
            println("using access token")
            return Observable.just(accessToken)
        } else if (refreshToken.isNotEmpty()) {
            println("using refresh token")
            return refreshAccessToken(context, refreshToken)
        } else {
            println("whoops")
            return Observable.just(null)
        }
    }

    val ogsOauthService: OgsOauthService by lazy {
        val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    println("XX ${request.method()} ${request.url()} -> ${response.code()}")
                    response
                }
                .build()
        Retrofit.Builder()
                .baseUrl("https://beta.online-go.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
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
                    println("request.headers() = ${request.headers()}")
                    val response = chain.proceed(request)
                    println("${request.method()} ${request.url()} -> ${response.code()}")
                    response
                }
                .build()

        Retrofit.Builder()
                .baseUrl("https://beta.online-go.com/api/v1/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
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

