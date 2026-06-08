package com.danipinion.z_talk.data.remote


import com.google.firebase.encoders.json.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // private const val BASE_URL = "http://localhost:3000"
    private const val BASE_URL = "https://ztalkapi.danipinion.my.id"

    private val client = OkHttpClient.Builder().apply {
        if (com.danipinion.z_talk.BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            addInterceptor(logging)
        }
    }.build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
