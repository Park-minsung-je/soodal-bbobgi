package kr.ilf.soodalbbobgi.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.ilf.soodalbbobgi.data.local.entity.SwimLogEntity

/**
 * 로컬 영속 DB. swim_logs 하나만 영속화한다.
 * 나머지 사용자/인벤토리/가챠/상점/프로필 카드 상태는 모두 메모리(AppState)에 보관.
 */
@Database(
    entities = [SwimLogEntity::class],
    version = 11,
    exportSchema = true,
)
abstract class SoodalDatabase : RoomDatabase() {
    abstract fun swimLogDao(): SwimLogDao
}
