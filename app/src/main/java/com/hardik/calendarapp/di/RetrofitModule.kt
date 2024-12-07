package com.hardik.calendarapp.di

import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.data.remote.api.ApiInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetryInterceptor


@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    // Provides the logging interceptor
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Set the logging level here
        }
    }

    // Provides the application interceptor
    @Provides
    @Singleton
    @ApplicationInterceptor
    fun provideApplicationInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    // Provides the retry interceptor
    @Provides
    @Singleton
    @RetryInterceptor
    fun provideRetryInterceptor(): Interceptor {
        return Interceptor { chain ->
            var response = chain.proceed(chain.request())
            var tryCount = 0
            while (!response.isSuccessful && tryCount < 3) {
                tryCount++
                response = chain.proceed(chain.request())
            }
            response
        }
    }

    // Provides the OkHttpClient, injecting the necessary interceptors
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor, // Injecting the logging interceptor here
        @ApplicationInterceptor applicationInterceptor: Interceptor,
        @RetryInterceptor retryInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add logging interceptor
            .addInterceptor(applicationInterceptor) // Add application interceptor
            .addInterceptor(retryInterceptor) // Add retry interceptor
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Provides the Retrofit instance
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Provides the ApiInterface
    @Provides
    @Singleton
    fun provideApiInterface(retrofit: Retrofit): ApiInterface {
        return retrofit.create(ApiInterface::class.java)
    }
}
