package com.twinmind.voicerecorder.data.transcripts

import kotlinx.coroutines.delay
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Header
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface TranscribeApiLike {
    suspend fun transcribe(bearer: String?, file: MultipartBody.Part): String
}

class RealTranscribeApi(private val api: TranscribeApi): TranscribeApiLike {
    override suspend fun transcribe(bearer: String?, file: MultipartBody.Part): String {
        val token = bearer ?: error("missing token")
        return api.transcribe(token, file).text
    }
}

class MockTranscribeApi: TranscribeApiLike {
    override suspend fun transcribe(bearer: String?, file: MultipartBody.Part): String {
        delay(400) // pretend work
        return "[mock transcript for ${file.body} …]"
    }
}

// Whisper-style endpoint (you can swap baseUrl to your proxy if needed)
interface TranscribeApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: String = "whisper-1"
    ): TranscriptionResponse

    data class TranscriptionResponse(val text: String)
}

object TranscribeApiFactory {
    fun create(baseUrl: String): TranscribeApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // ✅ tell Moshi how to handle Kotlin classes
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi)) // ✅ use our custom Moshi
            .client(client)
            .build()
            .create(TranscribeApi::class.java)
    }
}