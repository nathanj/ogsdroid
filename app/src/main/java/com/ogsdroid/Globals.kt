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
    private val refCount = AtomicInteger()

    var accessToken = ""
    var uiConfig: UiConfig? = null

    fun saveLoginInfo(context: Context, loginInfo: LoginInfo) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        println("saving loginInfo = ${loginInfo}")
        editor.putString("accessToken", loginInfo.access_token)
        editor.putString("refreshToken", loginInfo.refresh_token)
        editor.putLong("expiresAt", Date().time + loginInfo.expires_in * 1000)
        editor.apply()
        accessToken = loginInfo.access_token
    }

    fun refreshAccessToken(context: Context, refreshToken: String): Observable<String> {
        return ogsOauthService.refreshToken(refreshToken)
                .doOnNext { saveLoginInfo(context, it) }
                .map { it.access_token }
    }

    fun getAccessToken(context: Context): Observable<String> {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val accessToken = pref.getString("accessToken", "")
        val refreshToken = pref.getString("refreshToken", "")
        val expiresAt = pref.getLong("expiresAt", 0)

        val timeLeft = expiresAt - Date().time
        println("expiresAt   = ${expiresAt}")
        println("Date().time = ${Date().time}")
        println("timeLeft    = ${timeLeft}")
        if (accessToken.isNotEmpty() && expiresAt - 5 * 60 * 1000 > Date().time) {
            println("using access token")
            return Observable.just(accessToken)
        } else if (refreshToken.isNotEmpty()) {
            println("using refresh token")
            return refreshAccessToken(context, refreshToken)
        } else {
            println("whoops")
            return Observable.just("")
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
                .baseUrl("https://online-go.com/")
                //.client(httpClient)
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
                    println("${request.method()} ${request.url()} -> ${response.code()} ${response.message()}")
                    response
                }
                .build()

        Retrofit.Builder()
                .baseUrl("https://online-go.com/api/v1/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OgsService::class.java)
    }
}

