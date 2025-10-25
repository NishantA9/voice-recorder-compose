package com.twinmind.voicerecorder.di

import android.content.Context
import androidx.room.Room
import com.twinmind.voicerecorder.BuildConfig
import com.twinmind.voicerecorder.data.db.AppDatabase
import com.twinmind.voicerecorder.data.db.RecordingDao
import com.twinmind.voicerecorder.data.summaries.MockSummaryApi
import com.twinmind.voicerecorder.data.summaries.SummaryApiLike
import com.twinmind.voicerecorder.data.transcripts.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "recorder.db").build()

    @Provides fun dao(db: AppDatabase): RecordingDao = db.dao()

    @Provides @Singleton
    fun transcribeApiLike(): TranscribeApiLike {
        val key = BuildConfig.Google_Gemeni_Key.orEmpty()
        if (key.isBlank()) return MockTranscribeApi()

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
        val real = retrofit.create(TranscribeApi::class.java)
        return RealTranscribeApi(real)
    }

    @Provides @Singleton
    fun summaryApi(): SummaryApiLike = MockSummaryApi()
}
