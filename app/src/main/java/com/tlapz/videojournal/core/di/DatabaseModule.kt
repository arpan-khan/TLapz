package com.tlapz.videojournal.core.di

import android.content.Context
import androidx.room.Room
import com.tlapz.videojournal.core.util.Constants
import com.tlapz.videojournal.data.database.AppDatabase
import com.tlapz.videojournal.data.database.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: AppDatabase): VideoDao = database.videoDao()
}
