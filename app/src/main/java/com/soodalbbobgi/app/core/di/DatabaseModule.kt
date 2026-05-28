package com.soodalbbobgi.app.core.di

import android.content.Context
import androidx.room.Room
import com.soodalbbobgi.app.data.local.db.SoodalDatabase
import com.soodalbbobgi.app.data.local.db.SwimLogDao
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
    fun provideDatabase(@ApplicationContext context: Context): SoodalDatabase {
        return Room.databaseBuilder(
            context,
            SoodalDatabase::class.java,
            "soodal_bbobgi.db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides fun provideSwimLogDao(db: SoodalDatabase): SwimLogDao = db.swimLogDao()
}
