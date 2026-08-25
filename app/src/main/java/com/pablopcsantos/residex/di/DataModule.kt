package com.pablopcsantos.residex.di

import android.content.Context
import androidx.room.Room
import com.pablopcsantos.residex.residency.data.local.ResidencyDatabase
import com.pablopcsantos.residex.residency.data.local.SelectionDao
import com.pablopcsantos.residex.residency.data.remote.ResidencyApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        okHttp: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideResidencyApiService(retrofit: Retrofit): ResidencyApiService =
        retrofit.create(ResidencyApiService::class.java)


    @Provides
    @Singleton
    fun provideResidencyDatabase(@ApplicationContext appContext: Context): ResidencyDatabase =
        Room.databaseBuilder(appContext, ResidencyDatabase::class.java, "residency_db")
            .addMigrations(ResidencyDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideSelectionDao(db: ResidencyDatabase): SelectionDao = db.selectionDao()

    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context
}
