package com.xesc.asltv.di

import android.content.Context
import com.xesc.asltv.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    fun provideChannelListDao(db: AppDatabase) = db.channelListDao()

    @Provides
    fun provideEpgDao(db: AppDatabase) = db.epgDao()
}