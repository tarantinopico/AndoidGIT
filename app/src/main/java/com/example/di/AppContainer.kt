package com.example.di

import android.content.Context
import com.example.auth.GitHubAuthApi
import com.example.auth.SessionManager
import com.example.files.FileManager
import com.example.git.GitManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(private val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val fileManager: FileManager by lazy {
        FileManager()
    }

    val gitManager: GitManager by lazy {
        GitManager()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                )
            )
            .build()
    }

    val gitHubAuthApi: GitHubAuthApi by lazy {
        retrofit.create(GitHubAuthApi::class.java)
    }
}
