package com.soodalbbobgi.app.core.di

import android.content.Context
import androidx.room.Room
import com.soodalbbobgi.app.data.local.db.GachaBoxDao
import com.soodalbbobgi.app.data.local.db.GachaHistoryDao
import com.soodalbbobgi.app.data.local.db.InventoryDao
import com.soodalbbobgi.app.data.local.db.ProfileCardDao
import com.soodalbbobgi.app.data.local.db.SoodalDatabase
import com.soodalbbobgi.app.data.local.db.SwimLogDao
import com.soodalbbobgi.app.data.local.db.UserDao
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

    @Provides fun provideUserDao(db: SoodalDatabase): UserDao = db.userDao()
    @Provides fun provideSwimLogDao(db: SoodalDatabase): SwimLogDao = db.swimLogDao()
    @Provides fun provideInventoryDao(db: SoodalDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideProfileCardDao(db: SoodalDatabase): ProfileCardDao = db.profileCardDao()
    @Provides fun provideGachaHistoryDao(db: SoodalDatabase): GachaHistoryDao = db.gachaHistoryDao()
    @Provides fun provideGachaBoxDao(db: SoodalDatabase): GachaBoxDao = db.gachaBoxDao()
}
