package kr.ilf.soodalbbobgi.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.ilf.soodalbbobgi.data.local.db.SoodalDatabase
import kr.ilf.soodalbbobgi.data.local.db.SwimLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /** v10 → v11: 수동 입력용 평균 심박 컬럼 추가 — 기존 기록(HR 시계열 등) 보존을 위한 명시적 마이그레이션. */
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE swim_logs ADD COLUMN avgHr INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SoodalDatabase {
        return Room.databaseBuilder(
            context,
            SoodalDatabase::class.java,
            "soodal_bbobgi.db"
        ).addMigrations(MIGRATION_10_11)
         .fallbackToDestructiveMigration()
         .build()
    }

    @Provides fun provideSwimLogDao(db: SoodalDatabase): SwimLogDao = db.swimLogDao()
}
